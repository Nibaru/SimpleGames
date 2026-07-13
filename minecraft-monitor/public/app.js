const $ = (sel) => document.querySelector(sel);
const $$ = (sel) => document.querySelectorAll(sel);

const AUTH_KEY = 'mc-monitor-token';
const PREFS_KEY = 'mc-monitor-prefs';
const FAV_KEY = 'mc-monitor-favorites';

const COMMAND_SUGGESTIONS = [
  'list', 'tps', 'save-all', 'save-off', 'save-on', 'stop', 'kick', 'ban', 'pardon',
  'op', 'deop', 'whitelist add', 'whitelist remove', 'whitelist reload', 'say',
  'time set day', 'time set night', 'weather clear', 'weather rain', 'difficulty normal',
  'gamemode survival', 'gamemode creative', 'tp', 'give', 'effect', 'version', 'gc',
];

const PALETTE_ACTIONS = [
  { label: 'List players', command: 'list', group: 'Server' },
  { label: 'Check TPS', command: 'tps', group: 'Server' },
  { label: 'Save world', command: 'save-all', group: 'Server' },
  { label: 'Clear weather', command: 'weather clear', group: 'World' },
  { label: 'Set day', command: 'time set day', group: 'World' },
  { label: 'Set night', command: 'time set night', group: 'World' },
  { label: 'Reload whitelist', command: 'whitelist reload', group: 'Admin' },
  { label: 'Run garbage collection', command: 'gc', group: 'Admin' },
  { label: 'Reload config', command: 'reload confirm', group: 'Admin' },
];

const state = {
  logsPaused: false,
  logFilter: 'all',
  logSearch: '',
  commandHistory: [],
  historyIndex: -1,
  restartTimer: null,
  allLogLines: [],
  quickCommandMap: new Map(),
  plugins: [],
  datapacks: [],
  contentWorld: '',
  favorites: new Set(JSON.parse(localStorage.getItem(FAV_KEY) || '[]')),
  prefs: { theme: 'dark', logFontSize: 13, consoleFontSize: 13, soundEnabled: false, compactSidebar: false, ...JSON.parse(localStorage.getItem(PREFS_KEY) || '{}') },
  wsConnected: false,
  paletteIndex: 0,
  paletteItems: [],
};

const elements = {};

function cacheElements() {
  const ids = [
    'app', 'loginOverlay', 'loginForm', 'loginPassword', 'loginError',
    'statusBadge', 'statusText', 'playerCount', 'tpsValue', 'msptValue', 'peakPlayers',
    'uptimeValue', 'serverMotd', 'healthValue', 'healthRing', 'playerList', 'onlineBadge',
    'quickCommands', 'favoriteCommands', 'logOutput', 'rconOutput', 'rconForm', 'rconInput',
    'autoScroll', 'clearLogs', 'pauseLogs', 'clearRcon', 'downloadLogs', 'logSearch',
    'logFilters', 'broadcastForm', 'broadcastInput', 'whitelistList', 'whitelistMeta',
    'refreshWhitelist', 'refreshOps', 'refreshBanned', 'refreshPlugins', 'opsList', 'bannedList',
    'pluginList', 'pluginsMeta', 'pluginSearch', 'datapackList', 'datapacksMeta',
    'datapackSearch', 'refreshDatapacks', 'reloadDatapacks', 'listDatapacksRcon',
    'scheduleRestart', 'cancelRestart', 'restartDelay',
    'stopServerBtn', 'toastContainer', 'tpsChart', 'commandSuggestions', 'cmdSearch',
    'infoVersion', 'infoDifficulty', 'infoGamemode', 'logStats', 'wsPill', 'rconPill', 'bridgePill',
    'themeToggle', 'settingsBtn', 'shortcutsBtn', 'settingsModal', 'shortcutsModal',
    'commandPalette', 'paletteInput', 'paletteResults', 'openPalette', 'toggleSidebar',
    'sidePanel', 'paneContainer', 'splitter', 'fullscreenLogs', 'logPane', 'consolePane', 'logFontSize', 'consoleFontSize',
    'soundEnabled', 'compactSidebar', 'themeSelect', 'titleBroadcast',
  ];
  for (const id of ids) elements[id] = $(`#${id}`);
}

function savePrefs() {
  localStorage.setItem(PREFS_KEY, JSON.stringify(state.prefs));
}

function saveFavorites() {
  localStorage.setItem(FAV_KEY, JSON.stringify([...state.favorites]));
}

function applyPrefs() {
  document.documentElement.dataset.theme = state.prefs.theme;
  document.documentElement.style.setProperty('--log-font-size', `${state.prefs.logFontSize}px`);
  document.documentElement.style.setProperty('--console-font-size', `${state.prefs.consoleFontSize}px`);
  if (elements.themeSelect) elements.themeSelect.value = state.prefs.theme;
  if (elements.logFontSize) elements.logFontSize.value = state.prefs.logFontSize;
  if (elements.consoleFontSize) elements.consoleFontSize.value = state.prefs.consoleFontSize;
  if (elements.soundEnabled) elements.soundEnabled.checked = state.prefs.soundEnabled;
  if (elements.compactSidebar) elements.compactSidebar.checked = state.prefs.compactSidebar;
  if (elements.sidePanel) elements.sidePanel.classList.toggle('compact', state.prefs.compactSidebar);
  if (elements.themeToggle) {
    elements.themeToggle.textContent = state.prefs.theme === 'light' ? '☀️' : state.prefs.theme === 'minecraft' ? '🟩' : '🌙';
  }
}

function getAuthHeaders() {
  const token = sessionStorage.getItem(AUTH_KEY);
  return token ? { Authorization: `Bearer ${token}` } : {};
}

async function apiFetch(url, options = {}) {
  const res = await fetch(url, {
    ...options,
    headers: { 'Content-Type': 'application/json', ...getAuthHeaders(), ...options.headers },
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

function formatUptime(ms) {
  if (!ms) return '—';
  const s = Math.floor(ms / 1000);
  const h = Math.floor(s / 3600);
  const m = Math.floor((s % 3600) / 60);
  const sec = s % 60;
  if (h > 0) return `${h}h ${m}m`;
  if (m > 0) return `${m}m ${sec}s`;
  return `${sec}s`;
}

function playSound(type) {
  if (!state.prefs.soundEnabled) return;
  try {
    const ctx = new (window.AudioContext || window.webkitAudioContext)();
    const osc = ctx.createOscillator();
    const gain = ctx.createGain();
    osc.connect(gain);
    gain.connect(ctx.destination);
    osc.frequency.value = type === 'join' ? 880 : 440;
    gain.gain.value = 0.05;
    osc.start();
    gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.15);
    osc.stop(ctx.currentTime + 0.15);
  } catch { /* ignore */ }
}

function showToast(message, type = 'info') {
  const toast = document.createElement('div');
  toast.className = `toast toast-${type}`;
  toast.textContent = message;
  elements.toastContainer.appendChild(toast);
  requestAnimationFrame(() => toast.classList.add('show'));
  setTimeout(() => { toast.classList.remove('show'); setTimeout(() => toast.remove(), 300); }, 4000);
}

function updateHealth(score) {
  const circumference = 113;
  const offset = circumference - (score / 100) * circumference;
  elements.healthRing.style.strokeDashoffset = offset;
  elements.healthValue.textContent = score || '—';
  elements.healthRing.style.stroke = score >= 80 ? 'var(--accent)' : score >= 50 ? 'var(--warn)' : 'var(--danger)';
}

function parseLogLine(line) {
  const match = line.match(/^(\[\d{2}:\d{2}:\d{2}\])\s(\[[^\]]+\/(\w+)\]):\s(.*)$/);
  if (!match) return { raw: line };
  return { time: match[1], thread: match[2], level: match[3].toUpperCase(), message: match[4], raw: line };
}

function classifyLogLine(line, parsed) {
  const lower = line.toLowerCase();
  const level = parsed?.level?.toLowerCase();
  if (level === 'warn' || level === 'warning') return 'warn';
  if (level === 'error' || level === 'severe' || level === 'fatal') return 'error';
  if (lower.includes('joined the game') || lower.includes('left the game')) return 'player';
  if (lower.includes('monitorbridge/chat') || lower.includes('] chat:')) return 'chat';
  if (lower.includes('monitorbridge/death') || lower.includes('died')) return 'warn';
  if (lower.includes('monitorbridge/command')) return 'info';
  if (lower.includes('monitorbridge/kick')) return 'error';
  if (lower.includes('error') || lower.includes('exception')) return 'error';
  if (lower.includes('warn')) return 'warn';
  if (lower.includes('done') || lower.includes('success')) return 'success';
  return 'info';
}

function escapeHtml(text) {
  return text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}

function lineMatchesFilter(line, type) {
  if (state.logSearch && !line.toLowerCase().includes(state.logSearch.toLowerCase())) return false;
  if (state.logFilter === 'all') return true;
  if (state.logFilter === 'player') return type === 'player';
  if (state.logFilter === 'chat') return type === 'chat';
  return type === state.logFilter;
}

function renderLogLine(line, type = '') {
  const parsed = parseLogLine(line);
  const logType = type || classifyLogLine(line, parsed);
  const el = document.createElement('div');
  el.className = `log-line ${logType}`;
  el.dataset.type = logType;
  el.dataset.text = line.toLowerCase();
  el.title = 'Click to copy';

  if (parsed.time) {
    el.innerHTML = `<span class="log-time">${parsed.time}</span> <span class="log-thread">${parsed.thread}</span> <span class="log-msg">${escapeHtml(parsed.message)}</span>`;
  } else {
    el.textContent = line;
  }

  el.addEventListener('click', () => {
    navigator.clipboard.writeText(line).then(() => showToast('Log line copied', 'info'));
  });

  if (!lineMatchesFilter(line, logType)) el.classList.add('hidden');
  return el;
}

function updateLogStats() {
  const visible = [...elements.logOutput.children].filter((el) => !el.classList.contains('hidden')).length;
  elements.logStats.textContent = `${visible} / ${state.allLogLines.length} lines`;
}

function appendLogLine(line, type = '') {
  state.allLogLines.push({ line, type });
  if (state.allLogLines.length > 3000) {
    state.allLogLines.shift();
    elements.logOutput.firstChild?.remove();
  }
  elements.logOutput.appendChild(renderLogLine(line, type));
  updateLogStats();
  if (elements.autoScroll.checked && !state.logsPaused) {
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
      else if (state.logFilter === 'chat') matchesFilter = type === 'chat';
      else matchesFilter = type === state.logFilter;
    }
    el.classList.toggle('hidden', !(matchesSearch && matchesFilter));
  }
  updateLogStats();
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
  if (!state.commandHistory.length || state.commandHistory.at(-1) !== command) {
    state.commandHistory.push(command);
    if (state.commandHistory.length > 100) state.commandHistory.shift();
  }
  state.historyIndex = state.commandHistory.length;
  const submitBtn = elements.rconForm.querySelector('.btn-send');
  submitBtn.disabled = true;
  try {
    const res = await apiFetch('/api/rcon', { method: 'POST', body: JSON.stringify({ command }) });
    const data = await res.json();
    if (!res.ok) {
      if (!silent) appendRconEntry(command, data.error || 'Command failed', true);
      elements.rconPill.classList.remove('connected');
      throw new Error(data.error);
    }
    elements.rconPill.classList.add('connected');
    if (!silent) appendRconEntry(command, data.response || '(no output)');
    return data.response;
  } catch (err) {
    elements.rconPill.classList.remove('connected');
    throw err;
  } finally {
    submitBtn.disabled = false;
  }
}

function updateStatus(data) {
  const online = data.online;
  elements.statusBadge.classList.toggle('online', online);
  elements.statusBadge.classList.toggle('offline', !online);
  elements.statusText.textContent = online ? (data.bridgeConnected ? 'Online (Bridge)' : 'Online') : 'Offline';
  if (data.bridgeConnected) elements.bridgePill?.classList.add('connected');
  else elements.bridgePill?.classList.remove('connected');
  if (online) elements.rconPill.classList.add('connected');
  else elements.rconPill.classList.remove('connected');

  const { count, max, names } = data.players || { count: 0, max: 0, names: [] };
  elements.playerCount.textContent = `${count}/${max}`;
  elements.onlineBadge.textContent = count;

  if (data.tps != null) {
    elements.tpsValue.textContent = data.tps.toFixed(1);
    elements.tpsValue.style.color = data.tps >= 19 ? 'var(--accent)' : data.tps >= 15 ? 'var(--warn)' : 'var(--danger)';
  } else elements.tpsValue.textContent = '—';

  if (data.mspt != null) {
    elements.msptValue.textContent = data.mspt.toFixed(1);
    elements.msptValue.style.color = data.mspt <= 50 ? 'var(--accent)' : data.mspt <= 80 ? 'var(--warn)' : 'var(--danger)';
  } else elements.msptValue.textContent = '—';

  if (data.metrics?.peakPlayers != null) elements.peakPlayers.textContent = String(data.metrics.peakPlayers);
  if (data.metrics?.uptimeMs != null) elements.uptimeValue.textContent = formatUptime(data.metrics.uptimeMs);
  if (data.metrics?.health != null) updateHealth(data.metrics.health);

  if (data.server?.motd) {
    elements.serverMotd.textContent = data.server.motd.replace(/§./g, '');
  }
  if (data.version?.name) {
    const ver = data.version.minecraft ? `${data.version.name} · MC ${data.version.minecraft}` : data.version.name;
    elements.infoVersion.textContent = ver;
  }
  if (data.server) {
    elements.infoDifficulty.textContent = data.server.difficulty || '—';
    elements.infoGamemode.textContent = data.server.gamemode || '—';
  }

  renderPlayerList(names, data.players?.details);
  if (data.metrics?.tps) drawTpsChart(data.metrics.tps);
}

function avatarUrl(name) {
  return `https://minotar.net/avatar/${encodeURIComponent(name)}/24`;
}

function renderPlayerList(names, details = []) {
  elements.playerList.innerHTML = '';
  const detailMap = new Map((details || []).map((d) => [d.name, d]));
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
    const img = document.createElement('img');
    img.className = 'player-avatar';
    img.src = avatarUrl(name);
    img.alt = name;
    img.loading = 'lazy';
    li.appendChild(img);
    const nameSpan = document.createElement('span');
    nameSpan.className = 'player-name';
    nameSpan.textContent = detailMap.has(name)
      ? `${name} · ${detailMap.get(name).ping}ms`
      : name;
    li.appendChild(nameSpan);
    const actions = document.createElement('div');
    actions.className = 'player-actions';
    for (const btn of [
      { label: 'Msg', input: true },
      { label: 'Kick', cmd: `kick ${name}`, confirm: `Kick ${name}?` },
      { label: 'Ban', cmd: `ban ${name}`, confirm: `Ban ${name}?` },
      { label: 'Op', cmd: `op ${name}` },
    ]) {
      const button = document.createElement('button');
      button.className = 'player-action-btn';
      button.textContent = btn.label;
      button.addEventListener('click', async () => {
        if (btn.confirm && !confirm(btn.confirm)) return;
        if (btn.input) {
          const msg = prompt(`Message to ${name}:`);
          if (!msg) return;
          await sendRcon(`tell ${name} ${msg}`);
        } else await sendRcon(btn.cmd);
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
  ctx.strokeStyle = getComputedStyle(document.documentElement).getPropertyValue('--border').trim();
  ctx.lineWidth = 1;
  ctx.beginPath();
  const targetY = h - (20 / 20) * h;
  ctx.moveTo(0, targetY);
  ctx.lineTo(w, targetY);
  ctx.stroke();
  ctx.beginPath();
  recent.forEach((sample, i) => {
    const x = (i / Math.max(recent.length - 1, 1)) * w;
    const y = h - (Math.min(sample.tps, 20) / 20) * h;
    if (i === 0) ctx.moveTo(x, y); else ctx.lineTo(x, y);
  });
  const lastTps = recent.at(-1)?.tps ?? 20;
  ctx.strokeStyle = lastTps >= 19 ? '#3ddc84' : lastTps >= 15 ? '#f0b429' : '#ff6b6b';
  ctx.lineWidth = 2;
  ctx.stroke();
  ctx.lineTo(w, h);
  ctx.lineTo(0, h);
  ctx.closePath();
  ctx.fillStyle = lastTps >= 19 ? 'rgba(61,220,132,0.08)' : lastTps >= 15 ? 'rgba(240,180,41,0.08)' : 'rgba(255,107,107,0.08)';
  ctx.fill();
}

async function refreshStatus() {
  try {
    const res = await apiFetch('/api/status');
    updateStatus(await res.json());
  } catch (err) {
    if (err.message !== 'Unauthorized') {
      elements.statusBadge.classList.add('offline');
      elements.statusText.textContent = 'Unreachable';
    }
  }
}

async function loadList(endpoint, listEl, metaEl, renderItem) {
  try {
    const res = await apiFetch(endpoint);
    const data = await res.json();
    if (!res.ok) throw new Error(data.error);
    if (metaEl) metaEl.textContent = `${data.count} entries`;
    listEl.innerHTML = '';
    const items = data.names || data.players || [];
    const preview = items.slice(0, 15);
    for (const item of preview) {
      const li = document.createElement('li');
      renderItem(li, item);
      listEl.appendChild(li);
    }
    if (items.length > 15) {
      const li = document.createElement('li');
      li.className = 'player-empty';
      li.textContent = `+${items.length - 15} more`;
      listEl.appendChild(li);
    }
  } catch {
    if (metaEl) metaEl.textContent = 'Not available';
    listEl.innerHTML = '';
  }
}

function formatDate(iso) {
  if (!iso) return '—';
  const d = new Date(iso);
  return d.toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' });
}

function renderPluginCard(plugin) {
  const card = document.createElement('div');
  card.className = 'content-card';

  const badges = [];
  if (plugin.loaded) badges.push('<span class="badge-pill loaded">Running</span>');
  else if (plugin.enabled) badges.push('<span class="badge-pill">Installed</span>');
  if (!plugin.enabled) badges.push('<span class="badge-pill disabled">Disabled</span>');
  badges.push(`<span class="badge-pill type">${plugin.type}</span>`);

  const authors = plugin.authors?.length ? plugin.authors.join(', ') : null;
  const meta = [
    plugin.version ? `v${plugin.version}` : null,
    plugin.apiVersion ? `API ${plugin.apiVersion}` : null,
    plugin.sizeLabel,
    formatDate(plugin.modifiedAt),
  ].filter(Boolean);

  card.innerHTML = `
    <div class="content-card-header">
      <span class="content-card-title">${escapeHtml(plugin.name)}</span>
      <div class="content-card-badges">${badges.join('')}</div>
    </div>
    ${plugin.description ? `<p class="content-card-desc">${escapeHtml(plugin.description)}</p>` : ''}
    <div class="content-card-meta">
      ${meta.map((m) => `<span>${escapeHtml(m)}</span>`).join('')}
      ${authors ? `<span>${escapeHtml(authors)}</span>` : ''}
    </div>
    <div class="content-card-meta"><span>${escapeHtml(plugin.file)}</span></div>
  `;

  return card;
}

function renderDatapackCard(dp) {
  const card = document.createElement('div');
  card.className = 'content-card';

  const badges = [];
  if (dp.loaded) badges.push('<span class="badge-pill loaded">Loaded</span>');
  badges.push(`<span class="badge-pill type">${dp.type}</span>`);
  if (dp.packFormat != null) badges.push(`<span class="badge-pill">fmt ${dp.packFormat}</span>`);

  const meta = [
    dp.namespaceCount ? `${dp.namespaceCount} namespaces` : null,
    dp.sizeLabel,
    formatDate(dp.modifiedAt),
  ].filter(Boolean);

  const nsPreview = (dp.namespaces || []).slice(0, 6);
  const nsMore = (dp.namespaces || []).length - nsPreview.length;

  card.innerHTML = `
    <div class="content-card-header">
      <span class="content-card-title">${escapeHtml(dp.name)}</span>
      <div class="content-card-badges">${badges.join('')}</div>
    </div>
    ${dp.description ? `<p class="content-card-desc">${escapeHtml(String(dp.description).replace(/§./g, ''))}</p>` : ''}
    <div class="content-card-meta">${meta.map((m) => `<span>${escapeHtml(m)}</span>`).join('')}</div>
    <div class="content-card-meta"><span>${escapeHtml(dp.file)}</span></div>
    ${nsPreview.length ? `
      <div class="content-card-namespaces">
        ${nsPreview.map((ns) => `<span class="ns-tag">${escapeHtml(ns)}</span>`).join('')}
        ${nsMore > 0 ? `<span class="ns-tag">+${nsMore}</span>` : ''}
      </div>
    ` : ''}
  `;

  return card;
}

function renderPlugins(filter = '') {
  const q = filter.toLowerCase();
  const list = state.plugins.filter((p) =>
    !q
    || p.name.toLowerCase().includes(q)
    || p.file.toLowerCase().includes(q)
    || p.description?.toLowerCase().includes(q),
  );

  elements.pluginList.innerHTML = '';
  if (!list.length) {
    elements.pluginList.innerHTML = '<p class="content-empty">No plugins found</p>';
    return;
  }
  for (const plugin of list) {
    elements.pluginList.appendChild(renderPluginCard(plugin));
  }
}

function renderDatapacks(filter = '') {
  const q = filter.toLowerCase();
  const list = state.datapacks.filter((dp) =>
    !q
    || dp.name.toLowerCase().includes(q)
    || dp.file.toLowerCase().includes(q)
    || dp.description?.toLowerCase().includes(q)
    || dp.namespaces?.some((ns) => ns.toLowerCase().includes(q)),
  );

  elements.datapackList.innerHTML = '';
  if (!list.length) {
    elements.datapackList.innerHTML = '<p class="content-empty">No datapacks found</p>';
    return;
  }
  for (const dp of list) {
    elements.datapackList.appendChild(renderDatapackCard(dp));
  }
}

async function loadPlugins() {
  try {
    const res = await apiFetch('/api/plugins');
    const data = await res.json();
    if (!res.ok) throw new Error(data.error);

    state.plugins = data.plugins || [];
    const loaded = data.loadedCount ?? state.plugins.filter((p) => p.loaded).length;
    const enabled = data.enabledCount ?? state.plugins.filter((p) => p.enabled).length;
    elements.pluginsMeta.textContent = `${data.count} total · ${enabled} enabled · ${loaded} running`;
    renderPlugins(elements.pluginSearch?.value || '');
  } catch {
    state.plugins = [];
    elements.pluginsMeta.textContent = 'Not available — check SERVER_DIR';
    elements.pluginList.innerHTML = '<p class="content-empty">Could not load plugins</p>';
  }
}

async function loadDatapacks() {
  try {
    const res = await apiFetch('/api/datapacks');
    const data = await res.json();
    if (!res.ok) throw new Error(data.error);

    state.datapacks = data.datapacks || [];
    state.contentWorld = data.world || '';
    const loaded = data.loadedCount ?? state.datapacks.filter((d) => d.loaded).length;
    elements.datapacksMeta.textContent = `${data.count} in world "${data.world}" · ${loaded} loaded`;
    renderDatapacks(elements.datapackSearch?.value || '');
  } catch {
    state.datapacks = [];
    elements.datapacksMeta.textContent = 'Not available — check world folder';
    elements.datapackList.innerHTML = '<p class="content-empty">Could not load datapacks</p>';
  }
}

function setupContentTabs() {
  $$('.content-tab').forEach((tab) => {
    tab.addEventListener('click', () => {
      $$('.content-tab').forEach((t) => t.classList.remove('active'));
      $$('.content-view').forEach((v) => v.classList.remove('active'));
      tab.classList.add('active');
      $(`.content-view[data-content-view="${tab.dataset.contentTab}"]`)?.classList.add('active');
      if (tab.dataset.contentTab === 'datapacks' && !state.datapacks.length) loadDatapacks();
    });
  });
}

function connectWebSocket() {
  const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:';
  const ws = new WebSocket(`${protocol}//${location.host}/ws`);
  ws.onopen = () => { state.wsConnected = true; elements.wsPill.classList.add('connected'); };
  ws.onclose = () => { state.wsConnected = false; elements.wsPill.classList.remove('connected'); setTimeout(connectWebSocket, 3000); };
  ws.onmessage = (event) => {
    const data = JSON.parse(event.data);
    if (data.type === 'history') {
      elements.logOutput.innerHTML = '';
      state.allLogLines = [];
      for (const line of data.lines) appendLogLine(line);
    } else if (data.type === 'log' && !state.logsPaused) {
      const bridgeType = data.event?.type;
      const type = bridgeType === 'chat' ? 'chat' : bridgeType === 'death' || bridgeType === 'kick' ? 'warn' : bridgeType === 'join' || bridgeType === 'quit' ? 'player' : '';
      appendLogLine(data.line, type);
    else if (data.type === 'system' && !state.logsPaused) appendLogLine(data.message, 'system');
    else if (data.type === 'error' && !state.logsPaused) appendLogLine(data.message, 'error');
    else if (data.type === 'player_event') {
      const label = data.event === 'join' ? 'joined' : 'left';
      showToast(`${data.player} ${label}`, data.event === 'join' ? 'success' : 'info');
      playSound(data.event);
      refreshStatus();
    } else if (data.type === 'status') updateStatus(data);
    else if (data.type === 'bridge_status') {
      elements.bridgePill?.classList.toggle('connected', data.connected);
    }
  };
}

function renderQuickCommands(commands, filter = '') {
  elements.quickCommands.innerHTML = '';
  state.quickCommandMap.clear();
  const q = filter.toLowerCase();
  for (const cmd of commands) {
    if (q && !cmd.label.toLowerCase().includes(q) && !cmd.command?.toLowerCase().includes(q)) continue;
    state.quickCommandMap.set(cmd.id, cmd);
    const btn = document.createElement('button');
    btn.className = 'cmd-btn';
    btn.innerHTML = `<span>${cmd.icon || '▸'}</span> ${cmd.label}<span class="star ${state.favorites.has(cmd.id) ? 'active' : ''}">★</span>`;
    btn.querySelector('.star').addEventListener('click', (e) => {
      e.stopPropagation();
      toggleFavorite(cmd.id);
    });
    btn.addEventListener('click', () => runQuickCommand(cmd.id));
    elements.quickCommands.appendChild(btn);
  }
  renderFavorites(commands);
}

function renderFavorites(commands) {
  elements.favoriteCommands.innerHTML = '';
  const favs = commands.filter((c) => state.favorites.has(c.id));
  if (!favs.length) {
    elements.favoriteCommands.innerHTML = '<p class="hint">No favorites yet</p>';
    return;
  }
  for (const cmd of favs) {
    const btn = document.createElement('button');
    btn.className = 'cmd-btn';
    btn.innerHTML = `<span>${cmd.icon || '▸'}</span> ${cmd.label}`;
    btn.addEventListener('click', () => runQuickCommand(cmd.id));
    elements.favoriteCommands.appendChild(btn);
  }
}

function toggleFavorite(id) {
  if (state.favorites.has(id)) state.favorites.delete(id);
  else state.favorites.add(id);
  saveFavorites();
  renderQuickCommands(allCommands, elements.cmdSearch?.value || '');
}

async function runQuickCommand(id) {
  const cmd = state.quickCommandMap.get(id);
  if (!cmd) return;
  if (cmd.confirm && !confirm(cmd.confirm)) return;
  await sendRcon(cmd.command || id);
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

function openModal(modal) {
  modal.classList.remove('hidden');
  const input = modal.querySelector('input');
  if (input) { input.value = ''; input.focus(); }
}

function closeModals() {
  $$('.modal').forEach((m) => m.classList.add('hidden'));
}

function buildPaletteItems(query = '') {
  const q = query.toLowerCase();
  const cmds = [...state.quickCommandMap.values()].map((c) => ({ label: c.label, command: c.command, group: 'Quick' }));
  const all = [...PALETTE_ACTIONS, ...cmds, ...state.commandHistory.slice(-10).reverse().map((c) => ({ label: 'History', command: c, group: 'Recent' }))];
  return all.filter((item) => !q || item.label.toLowerCase().includes(q) || item.command.toLowerCase().includes(q));
}

function renderPalette(query = '') {
  state.paletteItems = buildPaletteItems(query);
  state.paletteIndex = 0;
  elements.paletteResults.innerHTML = '';
  for (const [i, item] of state.paletteItems.entries()) {
    const li = document.createElement('li');
    li.className = i === 0 ? 'active' : '';
    li.innerHTML = `<span class="pal-label">${item.label}</span><span class="pal-cmd">${item.command}</span>`;
    li.addEventListener('click', () => { closeModals(); sendRcon(item.command); });
    elements.paletteResults.appendChild(li);
  }
}

function setupSplitter() {
  let dragging = false;
  elements.splitter.addEventListener('mousedown', () => { dragging = true; elements.splitter.classList.add('dragging'); });
  document.addEventListener('mousemove', (e) => {
    if (!dragging) return;
    const container = elements.paneContainer.getBoundingClientRect();
    const ratio = (e.clientY - container.top) / container.height;
    elements.logPane.style.flex = ratio * 5;
    elements.consolePane.style.flex = (1 - ratio) * 5;
  });
  document.addEventListener('mouseup', () => { dragging = false; elements.splitter.classList.remove('dragging'); });
}

function setupNav() {
  $$('.nav-btn').forEach((btn) => {
    btn.addEventListener('click', () => {
      $$('.nav-btn').forEach((b) => b.classList.remove('active'));
      btn.classList.add('active');
      const panel = btn.dataset.panel;
      $$('.panel-view').forEach((v) => v.classList.toggle('active', v.dataset.view === panel));
      elements.sidePanel.classList.add('open');
    });
  });
}

function setupWorkspaceTabs() {
  $$('.workspace-tabs .tab').forEach((tab) => {
    tab.addEventListener('click', () => {
      $$('.workspace-tabs .tab').forEach((t) => t.classList.remove('active'));
      tab.classList.add('active');
      elements.paneContainer.className = 'pane-container';
      if (tab.dataset.tab === 'logs') elements.paneContainer.classList.add('logs-only');
      else if (tab.dataset.tab === 'console') elements.paneContainer.classList.add('console-only');
      else elements.paneContainer.classList.add('split-mode');
    });
  });
}

function scheduleRestart() {
  const totalSeconds = parseInt(elements.restartDelay.value, 10);
  const warnAt = new Set([totalSeconds, Math.floor(totalSeconds / 2), 60, 30, 10, 5, 4, 3, 2, 1].filter((v) => v > 0 && v <= totalSeconds));
  if (state.restartTimer) clearInterval(state.restartTimer);
  elements.cancelRestart.disabled = false;
  showToast(`Restart in ${totalSeconds}s`, 'warn');
  let remaining = totalSeconds;
  sendRcon(`say Server restarting in ${remaining} seconds!`);
  state.restartTimer = setInterval(async () => {
    remaining -= 1;
    if (warnAt.has(remaining)) await sendRcon(`say Restarting in ${remaining}s…`);
    if (remaining <= 0) {
      clearInterval(state.restartTimer);
      state.restartTimer = null;
      elements.cancelRestart.disabled = true;
      await sendRcon('stop');
    }
  }, 1000);
}

async function tryLogin(password) {
  const res = await fetch('/api/login', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ password }) });
  const data = await res.json();
  if (!res.ok) { elements.loginError.classList.remove('hidden'); return false; }
  if (data.token) sessionStorage.setItem(AUTH_KEY, data.token);
  elements.loginError.classList.add('hidden');
  return true;
}

let allCommands = [];

async function init() {
  applyPrefs();
  setupCommandSuggestions();
  setupSplitter();
  setupNav();
  setupWorkspaceTabs();
  setupContentTabs();

  const configRes = await apiFetch('/api/config').catch(() => null);
  if (!configRes) { showLogin(); return; }
  const config = await configRes.json();
  if (config.authRequired && !sessionStorage.getItem(AUTH_KEY)) { showLogin(); return; }

  showApp();
  allCommands = config.quickCommands || [];
  elements.quickCommands.dataset.allCommands = JSON.stringify(allCommands);
  renderQuickCommands(allCommands);
  connectWebSocket();
  await refreshStatus();
  await loadList('/api/whitelist', elements.whitelistList, elements.whitelistMeta, (li, name) => { li.textContent = name; });
  await loadList('/api/ops', elements.opsList, null, (li, name) => { li.textContent = name; });
  await loadList('/api/banned', elements.bannedList, null, (li, p) => { li.textContent = p.reason ? `${p.name} — ${p.reason}` : p.name; });
  await loadPlugins();
  setInterval(refreshStatus, 15000);
}

function bindEvents() {
  elements.loginForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    if (await tryLogin(elements.loginPassword.value)) await init();
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
      state.historyIndex = Math.min(state.commandHistory.length, state.historyIndex + 1);
      elements.rconInput.value = state.commandHistory[state.historyIndex] || '';
    }
  });

  elements.clearLogs.addEventListener('click', () => { elements.logOutput.innerHTML = ''; state.allLogLines = []; updateLogStats(); });
  elements.pauseLogs.addEventListener('click', () => {
    state.logsPaused = !state.logsPaused;
    elements.pauseLogs.textContent = state.logsPaused ? 'Resume' : 'Pause';
  });
  elements.clearRcon.addEventListener('click', () => { elements.rconOutput.innerHTML = ''; });
  elements.downloadLogs.addEventListener('click', async () => {
    try {
      const res = await apiFetch('/api/logs/download');
      if (!res.ok) throw new Error();
      const blob = await res.blob();
      const a = document.createElement('a');
      a.href = URL.createObjectURL(blob);
      a.download = 'latest.log';
      a.click();
    } catch { showToast('Download failed', 'error'); }
  });

  elements.logSearch.addEventListener('input', (e) => { state.logSearch = e.target.value.trim(); applyLogFilters(); });
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

  elements.titleBroadcast.addEventListener('click', async () => {
    const message = elements.broadcastInput.value.trim();
    if (!message) return;
    await sendRcon(`title @a title {"text":"${message.replace(/"/g, '\\"')}"}`);
    showToast('Title sent', 'success');
  });

  elements.refreshWhitelist.addEventListener('click', () => loadList('/api/whitelist', elements.whitelistList, elements.whitelistMeta, (li, n) => { li.textContent = n; }));
  elements.refreshOps.addEventListener('click', () => loadList('/api/ops', elements.opsList, null, (li, n) => { li.textContent = n; }));
  elements.refreshBanned.addEventListener('click', () => loadList('/api/banned', elements.bannedList, null, (li, p) => { li.textContent = p.name; }));
  elements.refreshPlugins.addEventListener('click', loadPlugins);
  elements.refreshDatapacks.addEventListener('click', loadDatapacks);
  elements.pluginSearch?.addEventListener('input', (e) => renderPlugins(e.target.value));
  elements.datapackSearch?.addEventListener('input', (e) => renderDatapacks(e.target.value));

  elements.reloadDatapacks?.addEventListener('click', async () => {
    if (!confirm('Reload server data (datapacks, functions, loot tables)? This may cause lag.')) return;
    await sendRcon('reload confirm');
    showToast('Reload initiated', 'warn');
    setTimeout(loadDatapacks, 3000);
  });

  elements.listDatapacksRcon?.addEventListener('click', async () => {
    const response = await sendRcon('datapack list');
    if (response) showToast('Datapack list sent to console', 'info');
  });

  elements.scheduleRestart.addEventListener('click', scheduleRestart);
  elements.cancelRestart.addEventListener('click', () => {
    if (state.restartTimer) clearInterval(state.restartTimer);
    state.restartTimer = null;
    elements.cancelRestart.disabled = true;
    sendRcon('say Restart cancelled.');
    showToast('Restart cancelled', 'info');
  });
  elements.stopServerBtn.addEventListener('click', async () => {
    if (!confirm('Stop the server?')) return;
    await sendRcon('stop');
  });

  elements.cmdSearch?.addEventListener('input', (e) => renderQuickCommands(allCommands, e.target.value));

  elements.themeToggle.addEventListener('click', () => {
    const themes = ['dark', 'light', 'minecraft'];
    state.prefs.theme = themes[(themes.indexOf(state.prefs.theme) + 1) % themes.length];
    savePrefs();
    applyPrefs();
  });

  elements.settingsBtn.addEventListener('click', () => openModal(elements.settingsModal));
  elements.shortcutsBtn.addEventListener('click', () => openModal(elements.shortcutsModal));
  elements.openPalette.addEventListener('click', () => { openModal(elements.commandPalette); renderPalette(); });

  elements.logFontSize.addEventListener('input', (e) => { state.prefs.logFontSize = +e.target.value; document.documentElement.style.setProperty('--log-font-size', `${e.target.value}px`); savePrefs(); });
  elements.consoleFontSize.addEventListener('input', (e) => { state.prefs.consoleFontSize = +e.target.value; document.documentElement.style.setProperty('--console-font-size', `${e.target.value}px`); savePrefs(); });
  elements.soundEnabled.addEventListener('change', (e) => { state.prefs.soundEnabled = e.target.checked; savePrefs(); });
  elements.compactSidebar.addEventListener('change', (e) => { state.prefs.compactSidebar = e.target.checked; elements.sidePanel.classList.toggle('compact', e.target.checked); savePrefs(); });
  elements.themeSelect.addEventListener('change', (e) => { state.prefs.theme = e.target.value; savePrefs(); applyPrefs(); });

  $$('[data-close]').forEach((el) => el.addEventListener('click', closeModals));
  elements.paletteInput.addEventListener('input', (e) => renderPalette(e.target.value));
  elements.paletteInput.addEventListener('keydown', (e) => {
    if (e.key === 'ArrowDown') { e.preventDefault(); state.paletteIndex = Math.min(state.paletteIndex + 1, state.paletteItems.length - 1); highlightPalette(); }
    else if (e.key === 'ArrowUp') { e.preventDefault(); state.paletteIndex = Math.max(state.paletteIndex - 1, 0); highlightPalette(); }
    else if (e.key === 'Enter' && state.paletteItems[state.paletteIndex]) { closeModals(); sendRcon(state.paletteItems[state.paletteIndex].command); }
    else if (e.key === 'Escape') closeModals();
  });

  elements.toggleSidebar?.addEventListener('click', () => elements.sidePanel.classList.toggle('open'));
  elements.fullscreenLogs.addEventListener('click', () => elements.paneContainer.classList.toggle('fullscreen-logs'));

  document.addEventListener('keydown', (e) => {
    if (e.ctrlKey && e.key === 'k') { e.preventDefault(); openModal(elements.commandPalette); renderPalette(); }
    if (e.key === '/' && document.activeElement.tagName !== 'INPUT' && document.activeElement.tagName !== 'TEXTAREA') { e.preventDefault(); elements.logSearch.focus(); }
    if (e.key === '`') { e.preventDefault(); elements.rconInput.focus(); }
    if (e.key === '?' && !e.ctrlKey) openModal(elements.shortcutsModal);
    if (e.key === 'Escape') closeModals();
  });
}

function highlightPalette() {
  $$('#paletteResults li').forEach((li, i) => li.classList.toggle('active', i === state.paletteIndex));
}

cacheElements();
bindEvents();
init();
