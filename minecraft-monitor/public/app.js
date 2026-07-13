const $ = (sel) => document.querySelector(sel);
const $$ = (sel) => document.querySelectorAll(sel);

const AUTH_KEY = 'mc-monitor-token';

const state = {
  logsPaused: false,
  logFilter: 'all',
  logSearch: '',
  commandHistory: [],
  historyIndex: -1,
  restartTimer: null,
  allLogLines: [],
  quickCommandMap: new Map(),
};

const elements = {
  app: $('#app'),
  loginOverlay: $('#loginOverlay'),
  loginForm: $('#loginForm'),
  loginPassword: $('#loginPassword'),
  loginError: $('#loginError'),
  statusBadge: $('#statusBadge'),
  statusText: $('#statusText'),
  playerCount: $('#playerCount'),
  tpsValue: $('#tpsValue'),
  msptValue: $('#msptValue'),
  peakPlayers: $('#peakPlayers'),
  serverVersion: $('#serverVersion'),
  playerList: $('#playerList'),
  quickCommands: $('#quickCommands'),
  logOutput: $('#logOutput'),
  rconOutput: $('#rconOutput'),
  rconForm: $('#rconForm'),
  rconInput: $('#rconInput'),
  autoScrollCheckbox: $('#autoScroll'),
  clearLogsBtn: $('#clearLogs'),
  pauseLogsBtn: $('#pauseLogs'),
  clearRconBtn: $('#clearRcon'),
  downloadLogsBtn: $('#downloadLogs'),
  logSearch: $('#logSearch'),
  logFilters: $('#logFilters'),
  broadcastForm: $('#broadcastForm'),
  broadcastInput: $('#broadcastInput'),
  whitelistList: $('#whitelistList'),
  whitelistMeta: $('#whitelistMeta'),
  refreshWhitelist: $('#refreshWhitelist'),
  scheduleRestart: $('#scheduleRestart'),
  cancelRestart: $('#cancelRestart'),
  restartDelay: $('#restartDelay'),
  stopServerBtn: $('#stopServerBtn'),
  toastContainer: $('#toastContainer'),
  tpsChart: $('#tpsChart'),
  commandSuggestions: $('#commandSuggestions'),
};

const COMMAND_SUGGESTIONS = [
  'list', 'tps', 'save-all', 'save-off', 'save-on', 'stop', 'kick', 'ban', 'pardon',
  'op', 'deop', 'whitelist add', 'whitelist remove', 'whitelist reload', 'say',
  'time set day', 'time set night', 'weather clear', 'weather rain', 'difficulty normal',
  'gamemode survival', 'gamemode creative', 'tp', 'give', 'effect', 'version', 'gc',
];

function getAuthHeaders() {
  const token = sessionStorage.getItem(AUTH_KEY);
  return token ? { Authorization: `Bearer ${token}` } : {};
}

async function apiFetch(url, options = {}) {
  const res = await fetch(url, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...getAuthHeaders(),
      ...options.headers,
    },
  });

  if (res.status === 401) {
    sessionStorage.removeItem(AUTH_KEY);
    showLogin();
    throw new Error('Unauthorized');
  }

  return res;
}

function showLogin() {
  elements.loginOverlay.classList.remove('hidden');
  elements.app.classList.add('hidden');
}

function showApp() {
  elements.loginOverlay.classList.add('hidden');
  elements.app.classList.remove('hidden');
}

function showToast(message, type = 'info') {
  const toast = document.createElement('div');
  toast.className = `toast toast-${type}`;
  toast.textContent = message;
  elements.toastContainer.appendChild(toast);
  requestAnimationFrame(() => toast.classList.add('show'));
  setTimeout(() => {
    toast.classList.remove('show');
    setTimeout(() => toast.remove(), 300);
  }, 4000);
}

function parseLogLine(line) {
  const match = line.match(/^(\[\d{2}:\d{2}:\d{2}\])\s(\[[^\]]+\/(\w+)\]):\s(.*)$/);
  if (!match) return { raw: line };

  return {
    time: match[1],
    thread: match[2],
    level: match[3].toUpperCase(),
    message: match[4],
    raw: line,
  };
}

function classifyLogLine(line, parsed) {
  const lower = line.toLowerCase();
  const level = parsed?.level?.toLowerCase();

  if (level === 'warn' || level === 'warning') return 'warn';
  if (level === 'error' || level === 'severe' || level === 'fatal') return 'error';
  if (lower.includes('joined the game') || lower.includes('left the game')) return 'player';
  if (lower.includes('error') || lower.includes('exception')) return 'error';
  if (lower.includes('warn')) return 'warn';
  if (lower.includes('done') || lower.includes('success')) return 'success';
  return 'info';
}

function lineMatchesFilter(line, type) {
  if (state.logSearch) {
    if (!line.toLowerCase().includes(state.logSearch.toLowerCase())) return false;
  }

  if (state.logFilter === 'all') return true;
  if (state.logFilter === 'player') return type === 'player';
  return type === state.logFilter;
}

function renderLogLine(line, type = '') {
  const parsed = parseLogLine(line);
  const logType = type || classifyLogLine(line, parsed);
  const el = document.createElement('div');
  el.className = `log-line ${logType}`;
  el.dataset.type = logType;
  el.dataset.text = line.toLowerCase();

  if (parsed.time) {
    el.innerHTML = `<span class="log-time">${parsed.time}</span> `
      + `<span class="log-thread">${parsed.thread}</span> `
      + `<span class="log-msg">${escapeHtml(parsed.message)}</span>`;
  } else {
    el.textContent = line;
  }

  if (!lineMatchesFilter(line, logType)) {
    el.classList.add('hidden');
  }

  return el;
}

function escapeHtml(text) {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;');
}

function appendLogLine(line, type = '') {
  state.allLogLines.push({ line, type });
  if (state.allLogLines.length > 2000) {
    state.allLogLines.shift();
    elements.logOutput.firstChild?.remove();
  }

  const el = renderLogLine(line, type);
  elements.logOutput.appendChild(el);

  if (elements.autoScrollCheckbox.checked && !state.logsPaused) {
    elements.logOutput.scrollTop = elements.logOutput.scrollHeight;
  }
}

function applyLogFilters() {
  for (const el of elements.logOutput.children) {
    const type = el.dataset.type || '';
    const text = el.dataset.text || el.textContent.toLowerCase();
    const matchesSearch = !state.logSearch || text.includes(state.logSearch.toLowerCase());
    let matchesFilter = state.logFilter === 'all';

    if (!matchesFilter) {
      if (state.logFilter === 'player') matchesFilter = type === 'player';
      else matchesFilter = type === state.logFilter;
    }

    el.classList.toggle('hidden', !(matchesSearch && matchesFilter));
  }
}

function appendRconEntry(command, response, isError = false) {
  const block = document.createElement('div');
  block.className = 'rcon-line';

  const cmdEl = document.createElement('div');
  cmdEl.className = 'rcon-command';
  cmdEl.textContent = command;
  block.appendChild(cmdEl);

  const respEl = document.createElement('div');
  respEl.className = isError ? 'rcon-error' : 'rcon-response';
  respEl.textContent = response;
  block.appendChild(respEl);

  elements.rconOutput.appendChild(block);
  elements.rconOutput.scrollTop = elements.rconOutput.scrollHeight;
}

async function sendRcon(command, { silent = false } = {}) {
  if (!command.trim()) return null;

  if (!state.commandHistory.length || state.commandHistory[state.commandHistory.length - 1] !== command) {
    state.commandHistory.push(command);
    if (state.commandHistory.length > 100) state.commandHistory.shift();
  }
  state.historyIndex = state.commandHistory.length;

  const submitBtn = elements.rconForm.querySelector('.btn-send');
  submitBtn.disabled = true;

  try {
    const res = await apiFetch('/api/rcon', {
      method: 'POST',
      body: JSON.stringify({ command }),
    });

    const data = await res.json();

    if (!res.ok) {
      if (!silent) appendRconEntry(command, data.error || 'Command failed', true);
      throw new Error(data.error || 'Command failed');
    }

    if (!silent) appendRconEntry(command, data.response || '(no output)');
    return data.response;
  } finally {
    submitBtn.disabled = false;
  }
}

function updateStatus(data) {
  const online = data.online;
  elements.statusBadge.classList.toggle('online', online);
  elements.statusBadge.classList.toggle('offline', !online);
  elements.statusText.textContent = online ? 'Online' : 'Offline';

  const { count, max, names } = data.players || { count: 0, max: 0, names: [] };
  elements.playerCount.textContent = `${count} / ${max}`;

  if (data.tps != null) {
    elements.tpsValue.textContent = data.tps.toFixed(1);
    elements.tpsValue.style.color = data.tps >= 19 ? 'var(--accent)' : data.tps >= 15 ? 'var(--warn)' : 'var(--danger)';
  } else {
    elements.tpsValue.textContent = '—';
    elements.tpsValue.style.color = '';
  }

  if (data.mspt != null) {
    elements.msptValue.textContent = data.mspt.toFixed(1);
    elements.msptValue.style.color = data.mspt <= 50 ? 'var(--accent)' : data.mspt <= 80 ? 'var(--warn)' : 'var(--danger)';
  } else {
    elements.msptValue.textContent = '—';
    elements.msptValue.style.color = '';
  }

  if (data.metrics?.peakPlayers != null) {
    elements.peakPlayers.textContent = String(data.metrics.peakPlayers);
  }

  if (data.version?.name) {
    elements.serverVersion.textContent = data.version.minecraft
      ? `${data.version.name} (MC ${data.version.minecraft})`
      : data.version.name;
  } else if (data.version?.raw) {
    elements.serverVersion.textContent = data.version.raw.slice(0, 60);
  }

  renderPlayerList(names);
  if (data.metrics?.tps) {
    drawTpsChart(data.metrics.tps);
  }
}

function renderPlayerList(names) {
  elements.playerList.innerHTML = '';

  if (!names.length) {
    const li = document.createElement('li');
    li.className = 'player-empty';
    li.textContent = 'No players online';
    elements.playerList.appendChild(li);
    return;
  }

  for (const name of names) {
    const li = document.createElement('li');
    li.className = 'player-row';

    const nameSpan = document.createElement('span');
    nameSpan.className = 'player-name';
    nameSpan.textContent = name;
    li.appendChild(nameSpan);

    const actions = document.createElement('div');
    actions.className = 'player-actions';

    const buttons = [
      { label: 'Msg', cmd: `tell ${name} `, input: true },
      { label: 'Kick', cmd: `kick ${name}`, confirm: `Kick ${name}?` },
      { label: 'Ban', cmd: `ban ${name}`, confirm: `Ban ${name}?` },
      { label: 'Op', cmd: `op ${name}` },
    ];

    for (const btn of buttons) {
      const button = document.createElement('button');
      button.className = 'player-action-btn';
      button.textContent = btn.label;
      button.title = btn.label;
      button.addEventListener('click', async () => {
        if (btn.confirm && !confirm(btn.confirm)) return;
        if (btn.input) {
          const msg = prompt(`Message to ${name}:`);
          if (!msg) return;
          await sendRcon(`tell ${name} ${msg}`);
        } else {
          await sendRcon(btn.cmd);
        }
        refreshStatus();
      });
      actions.appendChild(button);
    }

    li.appendChild(actions);
    elements.playerList.appendChild(li);
  }
}

function drawTpsChart(samples) {
  const canvas = elements.tpsChart;
  if (!canvas || !samples.length) return;

  const ctx = canvas.getContext('2d');
  const w = canvas.width;
  const h = canvas.height;
  ctx.clearRect(0, 0, w, h);

  const recent = samples.slice(-60);
  const minTps = 0;
  const maxTps = 20;

  ctx.strokeStyle = '#30363d';
  ctx.lineWidth = 1;
  ctx.beginPath();
  const targetY = h - ((20 - minTps) / (maxTps - minTps)) * h;
  ctx.moveTo(0, targetY);
  ctx.lineTo(w, targetY);
  ctx.stroke();

  ctx.beginPath();
  recent.forEach((sample, i) => {
    const x = (i / Math.max(recent.length - 1, 1)) * w;
    const y = h - ((Math.min(sample.tps, 20) - minTps) / (maxTps - minTps)) * h;
    if (i === 0) ctx.moveTo(x, y);
    else ctx.lineTo(x, y);
  });

  const lastTps = recent[recent.length - 1]?.tps ?? 20;
  ctx.strokeStyle = lastTps >= 19 ? '#3fb950' : lastTps >= 15 ? '#d29922' : '#f85149';
  ctx.lineWidth = 2;
  ctx.stroke();
}

async function refreshStatus() {
  try {
    const res = await apiFetch('/api/status');
    const data = await res.json();
    updateStatus(data);
  } catch (err) {
    if (err.message !== 'Unauthorized') {
      elements.statusBadge.classList.add('offline');
      elements.statusBadge.classList.remove('online');
      elements.statusText.textContent = 'Unreachable';
    }
  }
}

async function loadWhitelist() {
  try {
    const res = await apiFetch('/api/whitelist');
    const data = await res.json();
    if (!res.ok) throw new Error(data.error);

    elements.whitelistMeta.textContent = `${data.count} whitelisted`;
    elements.whitelistList.innerHTML = '';

    const preview = data.names.slice(0, 12);
    for (const name of preview) {
      const li = document.createElement('li');
      li.textContent = name;
      elements.whitelistList.appendChild(li);
    }

    if (data.names.length > 12) {
      const li = document.createElement('li');
      li.className = 'player-empty';
      li.textContent = `+${data.names.length - 12} more`;
      elements.whitelistList.appendChild(li);
    }
  } catch {
    elements.whitelistMeta.textContent = 'Not available';
    elements.whitelistList.innerHTML = '';
  }
}

function connectWebSocket() {
  const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:';
  const ws = new WebSocket(`${protocol}//${location.host}/ws`);

  ws.onmessage = (event) => {
    const data = JSON.parse(event.data);

    if (data.type === 'history') {
      elements.logOutput.innerHTML = '';
      state.allLogLines = [];
      for (const line of data.lines) {
        appendLogLine(line);
      }
    } else if (data.type === 'log' && !state.logsPaused) {
      appendLogLine(data.line);
    } else if (data.type === 'system' && !state.logsPaused) {
      appendLogLine(data.message, 'system');
    } else if (data.type === 'error' && !state.logsPaused) {
      appendLogLine(data.message, 'error');
    } else if (data.type === 'player_event') {
      const label = data.event === 'join' ? 'joined' : 'left';
      showToast(`${data.player} ${label} the server`, data.event === 'join' ? 'success' : 'info');
      refreshStatus();
    } else if (data.type === 'status') {
      updateStatus(data);
    }
  };

  ws.onclose = () => setTimeout(connectWebSocket, 3000);
}

function renderQuickCommands(commands) {
  elements.quickCommands.innerHTML = '';
  state.quickCommandMap.clear();

  for (const cmd of commands) {
    state.quickCommandMap.set(cmd.id, cmd);
    const btn = document.createElement('button');
    btn.className = 'cmd-btn';
    btn.dataset.commandId = cmd.id;
    btn.innerHTML = `<span>${cmd.icon || '▸'}</span> ${cmd.label}`;
    btn.addEventListener('click', () => runQuickCommand(cmd.id));
    elements.quickCommands.appendChild(btn);
  }
}

async function runQuickCommand(id) {
  const cmd = state.quickCommandMap.get(id);
  if (!cmd) return;
  const command = cmd.command || id;
  if (cmd.confirm && !confirm(cmd.confirm)) return;
  await sendRcon(command);
  refreshStatus();
}

function setupCommandSuggestions() {
  elements.commandSuggestions.innerHTML = '';
  for (const cmd of COMMAND_SUGGESTIONS) {
    const opt = document.createElement('option');
    opt.value = cmd;
    elements.commandSuggestions.appendChild(opt);
  }
}

function scheduleRestart() {
  const totalSeconds = parseInt(elements.restartDelay.value, 10);
  const warnAt = new Set([
    totalSeconds,
    Math.floor(totalSeconds / 2),
    60, 30, 10, 5, 4, 3, 2, 1,
  ].filter((v) => v > 0 && v <= totalSeconds));

  if (state.restartTimer) {
    clearInterval(state.restartTimer);
    state.restartTimer = null;
  }

  elements.cancelRestart.disabled = false;
  showToast(`Restart scheduled in ${totalSeconds}s`, 'warn');

  let remaining = totalSeconds;
  sendRcon(`say Server restarting in ${remaining} seconds!`);

  state.restartTimer = setInterval(async () => {
    remaining -= 1;

    if (warnAt.has(remaining)) {
      await sendRcon(`say Restarting in ${remaining} second${remaining === 1 ? '' : 's'}…`);
    }

    if (remaining <= 0) {
      clearInterval(state.restartTimer);
      state.restartTimer = null;
      elements.cancelRestart.disabled = true;
      await sendRcon('stop');
    }
  }, 1000);
}

function cancelRestart() {
  if (state.restartTimer) {
    clearInterval(state.restartTimer);
    state.restartTimer = null;
  }
  elements.cancelRestart.disabled = true;
  sendRcon('say Server restart cancelled.');
  showToast('Restart cancelled', 'info');
}

async function tryLogin(password) {
  const res = await fetch('/api/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ password }),
  });
  const data = await res.json();

  if (!res.ok) {
    elements.loginError.classList.remove('hidden');
    return false;
  }

  if (data.token) {
    sessionStorage.setItem(AUTH_KEY, data.token);
  }

  elements.loginError.classList.add('hidden');
  return true;
}

async function init() {
  setupCommandSuggestions();

  const configRes = await apiFetch('/api/config').catch(() => null);
  if (!configRes) {
    showLogin();
    return;
  }

  const config = await configRes.json();

  if (config.authRequired && !sessionStorage.getItem(AUTH_KEY)) {
    showLogin();
    return;
  }

  showApp();
  renderQuickCommands(config.quickCommands || []);
  connectWebSocket();
  await refreshStatus();
  await loadWhitelist();
  setInterval(refreshStatus, 15000);
}

elements.loginForm.addEventListener('submit', async (e) => {
  e.preventDefault();
  const ok = await tryLogin(elements.loginPassword.value);
  if (ok) await init();
});

elements.rconForm.addEventListener('submit', async (e) => {
  e.preventDefault();
  const command = elements.rconInput.value.trim();
  if (!command) return;
  elements.rconInput.value = '';
  await sendRcon(command);
  elements.rconInput.focus();
  refreshStatus();
});

elements.rconInput.addEventListener('keydown', (e) => {
  if (e.key === 'ArrowUp') {
    e.preventDefault();
    if (!state.commandHistory.length) return;
    state.historyIndex = Math.max(0, state.historyIndex - 1);
    elements.rconInput.value = state.commandHistory[state.historyIndex];
  } else if (e.key === 'ArrowDown') {
    e.preventDefault();
    if (!state.commandHistory.length) return;
    state.historyIndex = Math.min(state.commandHistory.length, state.historyIndex + 1);
    elements.rconInput.value = state.commandHistory[state.historyIndex] || '';
  }
});

elements.clearLogsBtn.addEventListener('click', () => {
  elements.logOutput.innerHTML = '';
  state.allLogLines = [];
});

elements.pauseLogsBtn.addEventListener('click', () => {
  state.logsPaused = !state.logsPaused;
  elements.pauseLogsBtn.textContent = state.logsPaused ? 'Resume' : 'Pause';
  elements.pauseLogsBtn.style.color = state.logsPaused ? 'var(--warn)' : '';
});

elements.clearRconBtn.addEventListener('click', () => {
  elements.rconOutput.innerHTML = '';
});

elements.downloadLogsBtn.addEventListener('click', async () => {
  try {
    const res = await apiFetch('/api/logs/download');
    if (!res.ok) throw new Error('Download failed');
    const blob = await res.blob();
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'latest.log';
    a.click();
    URL.revokeObjectURL(url);
  } catch {
    showToast('Log download failed', 'error');
  }
});

elements.logSearch.addEventListener('input', (e) => {
  state.logSearch = e.target.value.trim();
  applyLogFilters();
});

elements.logFilters.addEventListener('click', (e) => {
  const btn = e.target.closest('.filter-btn');
  if (!btn) return;
  $$('.filter-btn').forEach((b) => b.classList.remove('active'));
  btn.classList.add('active');
  state.logFilter = btn.dataset.filter;
  applyLogFilters();
});

elements.broadcastForm.addEventListener('submit', async (e) => {
  e.preventDefault();
  const message = elements.broadcastInput.value.trim();
  if (!message) return;
  await sendRcon(`say ${message}`);
  elements.broadcastInput.value = '';
  showToast('Broadcast sent', 'success');
});

elements.refreshWhitelist.addEventListener('click', loadWhitelist);
elements.scheduleRestart.addEventListener('click', scheduleRestart);
elements.cancelRestart.addEventListener('click', cancelRestart);

elements.stopServerBtn.addEventListener('click', async () => {
  if (!confirm('Stop the server? This will disconnect all players.')) return;
  await sendRcon('stop');
});

init();
