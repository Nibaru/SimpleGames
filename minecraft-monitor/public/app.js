const $ = (sel) => document.querySelector(sel);

const statusBadge = $('#statusBadge');
const statusText = $('#statusText');
const playerCount = $('#playerCount');
const tpsValue = $('#tpsValue');
const logPath = $('#logPath');
const playerList = $('#playerList');
const quickCommands = $('#quickCommands');
const logOutput = $('#logOutput');
const rconOutput = $('#rconOutput');
const rconForm = $('#rconForm');
const rconInput = $('#rconInput');
const autoScrollCheckbox = $('#autoScroll');
const clearLogsBtn = $('#clearLogs');
const pauseLogsBtn = $('#pauseLogs');
const clearRconBtn = $('#clearRcon');

let logsPaused = false;
let ws = null;
let statusInterval = null;

function classifyLogLine(line) {
  const lower = line.toLowerCase();
  if (lower.includes('error') || lower.includes('exception') || lower.includes('severe')) {
    return 'error';
  }
  if (lower.includes('warn')) {
    return 'warn';
  }
  if (lower.includes('done') || lower.includes('success') || lower.includes('enabled')) {
    return 'success';
  }
  if (lower.includes('info') || lower.includes('joined') || lower.includes('left')) {
    return 'info';
  }
  return '';
}

function appendLogLine(line, type = '') {
  const el = document.createElement('div');
  el.className = `log-line ${type || classifyLogLine(line)}`;
  el.textContent = line;
  logOutput.appendChild(el);

  if (autoScrollCheckbox.checked && !logsPaused) {
    logOutput.scrollTop = logOutput.scrollHeight;
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

  rconOutput.appendChild(block);
  rconOutput.scrollTop = rconOutput.scrollHeight;
}

async function sendRcon(command, { silent = false } = {}) {
  if (!command.trim()) return null;

  const submitBtn = rconForm.querySelector('.btn-send');
  submitBtn.disabled = true;

  try {
    const res = await fetch('/api/rcon', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
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
  statusBadge.classList.toggle('online', online);
  statusBadge.classList.toggle('offline', !online);
  statusText.textContent = online ? 'Online' : 'Offline';

  const { count, max, names } = data.players || { count: 0, max: 0, names: [] };
  playerCount.textContent = `${count} / ${max}`;

  if (data.tps != null) {
    const tps = data.tps;
    tpsValue.textContent = tps.toFixed(1);
    tpsValue.style.color = tps >= 19 ? 'var(--accent)' : tps >= 15 ? 'var(--warn)' : 'var(--danger)';
  } else {
    tpsValue.textContent = '—';
    tpsValue.style.color = '';
  }

  if (data.logFile) {
    const parts = data.logFile.split('/');
    logPath.textContent = parts.slice(-2).join('/');
    logPath.title = data.logFile;
  }

  playerList.innerHTML = '';
  if (names.length === 0) {
    const li = document.createElement('li');
    li.className = 'player-empty';
    li.textContent = 'No players online';
    playerList.appendChild(li);
  } else {
    for (const name of names) {
      const li = document.createElement('li');
      li.textContent = name;
      playerList.appendChild(li);
    }
  }
}

async function refreshStatus() {
  try {
    const res = await fetch('/api/status');
    const data = await res.json();
    updateStatus(data);
  } catch {
    statusBadge.classList.add('offline');
    statusBadge.classList.remove('online');
    statusText.textContent = 'Unreachable';
  }
}

function connectWebSocket() {
  const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:';
  ws = new WebSocket(`${protocol}//${location.host}/ws`);

  ws.onmessage = (event) => {
    if (logsPaused) return;

    const data = JSON.parse(event.data);

    if (data.type === 'history') {
      logOutput.innerHTML = '';
      for (const line of data.lines) {
        appendLogLine(line);
      }
    } else if (data.type === 'log') {
      appendLogLine(data.line);
    } else if (data.type === 'system') {
      appendLogLine(data.message, 'system');
    } else if (data.type === 'error') {
      appendLogLine(data.message, 'error');
    }
  };

  ws.onclose = () => {
    setTimeout(connectWebSocket, 3000);
  };
}

function renderQuickCommands(commands) {
  quickCommands.innerHTML = '';

  for (const cmd of commands) {
    const btn = document.createElement('button');
    btn.className = 'cmd-btn';
    btn.dataset.command = cmd.command || cmd.id;
    btn.innerHTML = `<span>${cmd.icon || '▸'}</span> ${cmd.label}`;
    btn.addEventListener('click', () => {
      sendRcon(btn.dataset.command);
    });
    quickCommands.appendChild(btn);
  }

  document.querySelectorAll('.cmd-btn[data-confirm]').forEach((btn) => {
    btn.addEventListener('click', async () => {
      const command = btn.dataset.command;
      const message = btn.dataset.confirm;
      if (!confirm(message)) return;
      await sendRcon(command);
    });
  });
}

async function init() {
  try {
    const res = await fetch('/api/config');
    const config = await res.json();
    renderQuickCommands(config.quickCommands);
  } catch {
    renderQuickCommands([]);
  }

  connectWebSocket();
  await refreshStatus();
  statusInterval = setInterval(refreshStatus, 10000);
}

rconForm.addEventListener('submit', async (e) => {
  e.preventDefault();
  const command = rconInput.value.trim();
  if (!command) return;
  rconInput.value = '';
  await sendRcon(command);
  rconInput.focus();
});

clearLogsBtn.addEventListener('click', () => {
  logOutput.innerHTML = '';
});

pauseLogsBtn.addEventListener('click', () => {
  logsPaused = !logsPaused;
  pauseLogsBtn.textContent = logsPaused ? 'Resume' : 'Pause';
  pauseLogsBtn.style.color = logsPaused ? 'var(--warn)' : '';
});

clearRconBtn.addEventListener('click', () => {
  rconOutput.innerHTML = '';
});

rconInput.addEventListener('keydown', (e) => {
  if (e.key === 'ArrowUp' || e.key === 'ArrowDown') {
    e.preventDefault();
  }
});

init();
