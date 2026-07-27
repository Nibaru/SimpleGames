require('dotenv').config();

const http = require('http');
const path = require('path');
const fs = require('fs');
const express = require('express');
const { WebSocketServer } = require('ws');

const { RconService } = require('./src/rcon');
const { LogTailer } = require('./src/logTailer');
const { authMiddleware, login, authRequired } = require('./src/auth');
const { MetricsStore } = require('./src/metrics');
const { loadQuickCommands } = require('./src/quickCommands');
const { BridgeClient, mapBridgeStatus } = require('./src/bridgeClient');

const PORT = parseInt(process.env.PORT || '3000', 10);
const SERVER_DIR = process.env.SERVER_DIR || '';
const LOG_FILE = process.env.LOG_FILE || (SERVER_DIR ? path.join(SERVER_DIR, 'logs', 'latest.log') : '');
const RCON_HOST = process.env.RCON_HOST || '127.0.0.1';
const RCON_PORT = parseInt(process.env.RCON_PORT || '25575', 10);
const RCON_PASSWORD = process.env.RCON_PASSWORD || '';
const LOG_HISTORY_LINES = parseInt(process.env.LOG_HISTORY_LINES || '500', 10);
const DASHBOARD_PASSWORD = process.env.DASHBOARD_PASSWORD || '';
const BRIDGE_URL = process.env.BRIDGE_URL || 'http://127.0.0.1:8765';
const BRIDGE_API_KEY = process.env.BRIDGE_API_KEY || '';
const QUICK_COMMANDS_FILE = process.env.QUICK_COMMANDS_FILE
  || path.join(__dirname, 'quick-commands.json');

const QUICK_COMMANDS = loadQuickCommands(
  fs.existsSync(QUICK_COMMANDS_FILE) ? QUICK_COMMANDS_FILE : null,
);

const app = express();
app.use(express.json());
app.use(authMiddleware(DASHBOARD_PASSWORD));
app.use(express.static(path.join(__dirname, 'public')));

const rcon = new RconService({
  host: RCON_HOST,
  port: RCON_PORT,
  password: RCON_PASSWORD,
});

const metrics = new MetricsStore({ maxSamples: 120 });

const bridge = new BridgeClient({ url: BRIDGE_URL, apiKey: BRIDGE_API_KEY });
let bridgeAvailable = false;

async function fetchCombinedStatus() {
  if (bridge.isConfigured()) {
    try {
      const bridgeStatus = await bridge.getStatus();
      bridgeAvailable = true;
      const mapped = mapBridgeStatus(bridgeStatus, metrics.getHistory(bridgeStatus));
      metrics.recordStatus(mapped);
      return { ...mapped, bridgeConnected: true, source: 'bridge' };
    } catch {
      bridgeAvailable = false;
    }
  }

  const status = await rcon.getStatus();
  metrics.recordStatus(status);
  return { ...status, bridgeConnected: false, source: 'rcon' };
}

function broadcast(data) {
  const payload = JSON.stringify(data);
  for (const client of wss.clients) {
    if (client.readyState === client.OPEN) {
      client.send(payload);
    }
  }
}

function formatBridgeLogLine(event) {
  const time = event.timeFormatted || new Date(event.time).toTimeString().slice(0, 8);
  const level = (event.level || 'info').toUpperCase();
  const type = event.type || 'event';
  const message = event.message || '';
  return `[${time}] [MonitorBridge/${type}] [${level}] ${message}`;
}

let logTailer = null;

if (LOG_FILE) {
  logTailer = new LogTailer(LOG_FILE, { historyLines: LOG_HISTORY_LINES });
  logTailer.start().catch((err) => {
    console.error(`Failed to start log tailer: ${err.message}`);
  });
}

const {
  readWhitelist,
  readOps,
  readBannedPlayers,
  listPlugins,
  listDatapacks,
  readServerProperties,
  parseRconPluginList,
  parseRconDatapackList,
  mergePluginLoadState,
} = require('./src/serverData');

app.post('/api/login', (req, res) => {
  const result = login(DASHBOARD_PASSWORD, req.body?.password || '');
  if (result.error) {
    return res.status(401).json(result);
  }
  res.json(result);
});

app.get('/api/config', (_req, res) => {
  res.json({
    logFile: LOG_FILE || null,
    rcon: { host: RCON_HOST, port: RCON_PORT },
    authRequired: authRequired(DASHBOARD_PASSWORD),
    quickCommands: QUICK_COMMANDS.map(({ id, label, icon, confirm, command }) => ({
      id,
      label,
      icon,
      command,
      confirm: confirm || null,
    })),
    serverDir: SERVER_DIR || null,
    bridge: {
      url: bridge.isConfigured() ? BRIDGE_URL : null,
      configured: bridge.isConfigured(),
      connected: bridgeAvailable,
    },
  });
});

app.get('/api/bridge/health', async (_req, res) => {
  if (!bridge.isConfigured()) {
    return res.status(404).json({ error: 'BRIDGE_URL not configured' });
  }
  try {
    const health = await bridge.health();
    res.json({ connected: true, ...health });
  } catch (err) {
    res.status(502).json({ connected: false, error: err.message });
  }
});

app.get('/api/status', async (_req, res) => {
  const status = await fetchCombinedStatus();

  const props = SERVER_DIR ? readServerProperties(SERVER_DIR) : null;
  const metricsData = metrics.getHistory(status);

  res.json({
    ...status,
    logFile: LOG_FILE || null,
    logAvailable: Boolean(logTailer),
    bridgeConnected: bridgeAvailable,
    server: props ? {
      motd: status.server?.motd || props['motd'],
      maxPlayers: status.server?.maxPlayers || props['max-players'],
      gamemode: props['gamemode'],
      difficulty: props['difficulty'],
      pvp: props['pvp'],
      seed: props['level-seed'] || null,
      onlineMode: props['online-mode'],
      viewDistance: props['view-distance'],
    } : status.server || null,
    metrics: metricsData,
  });
});

app.get('/api/metrics', async (_req, res) => {
  const status = await fetchCombinedStatus().catch(() => ({ online: false }));
  res.json(metrics.getHistory(status));
});

app.post('/api/rcon', async (req, res) => {
  const { command } = req.body;

  if (!command || typeof command !== 'string') {
    return res.status(400).json({ error: 'Command is required' });
  }

  if (!RCON_PASSWORD) {
    return res.status(503).json({ error: 'RCON is not configured. Set RCON_PASSWORD in .env' });
  }

  try {
    const response = await rcon.send(command.trim());
    res.json({ command, response });
  } catch (err) {
    res.status(502).json({ error: err.message });
  }
});

app.get('/api/logs/history', async (_req, res) => {
  if (bridge.isConfigured() && bridgeAvailable) {
    try {
      const data = await bridge.getLogs(500);
      return res.json({ lines: data.lines || [], source: 'bridge', events: data.events });
    } catch {
      // fall through to file logs
    }
  }

  if (!logTailer) {
    return res.status(503).json({ error: 'Log file not configured. Set SERVER_DIR or LOG_FILE in .env' });
  }
  res.json({ lines: logTailer.getHistory(), source: 'file' });
});

app.get('/api/logs/download', (_req, res) => {
  if (!LOG_FILE || !fs.existsSync(LOG_FILE)) {
    return res.status(404).json({ error: 'Log file not available' });
  }
  res.download(LOG_FILE, path.basename(LOG_FILE));
});

app.get('/api/whitelist', (_req, res) => {
  if (!SERVER_DIR) return res.status(404).json({ error: 'SERVER_DIR not configured' });
  const names = readWhitelist(SERVER_DIR);
  if (names === null) {
    return res.status(404).json({ error: 'Whitelist file not found' });
  }
  res.json({ count: names.length, names });
});

app.get('/api/ops', (_req, res) => {
  if (!SERVER_DIR) return res.status(404).json({ error: 'SERVER_DIR not configured' });
  const names = readOps(SERVER_DIR);
  if (names === null) return res.status(404).json({ error: 'Ops file not found' });
  res.json({ count: names.length, names });
});

app.get('/api/banned', (_req, res) => {
  if (!SERVER_DIR) return res.status(404).json({ error: 'SERVER_DIR not configured' });
  const players = readBannedPlayers(SERVER_DIR);
  if (players === null) return res.status(404).json({ error: 'Banned players file not found' });
  res.json({ count: players.length, players });
});

app.get('/api/plugins', async (_req, res) => {
  if (bridge.isConfigured() && bridgeAvailable) {
    try {
      const data = await bridge.getPlugins();
      const plugins = (data.plugins || []).map((p) => ({
        id: p.name,
        name: p.name,
        file: p.name + '.jar',
        type: 'jar',
        version: p.version,
        description: p.description,
        authors: p.authors || [],
        enabled: p.enabled,
        loaded: p.enabled,
        sizeLabel: '—',
      }));
      return res.json({
        count: plugins.length,
        enabledCount: plugins.filter((p) => p.enabled).length,
        loadedCount: plugins.filter((p) => p.enabled).length,
        source: 'bridge',
        plugins,
      });
    } catch {
      // fall through
    }
  }

  if (!SERVER_DIR) return res.status(404).json({ error: 'SERVER_DIR not configured' });
  const plugins = listPlugins(SERVER_DIR);
  if (plugins === null) return res.status(404).json({ error: 'Plugins folder not found' });

  let loaded = [];
  if (RCON_PASSWORD) {
    try {
      const output = await rcon.send('plugins');
      loaded = parseRconPluginList(output);
    } catch {
      // RCON unavailable — file scan only
    }
  }

  const enriched = mergePluginLoadState(plugins, loaded);
  const enabledCount = enriched.filter((p) => p.enabled).length;
  const loadedCount = enriched.filter((p) => p.loaded).length;

  res.json({
    count: enriched.length,
    enabledCount,
    loadedCount,
    loaded,
    plugins: enriched,
  });
});

app.get('/api/datapacks', async (_req, res) => {
  if (!SERVER_DIR) return res.status(404).json({ error: 'SERVER_DIR not configured' });
  const result = listDatapacks(SERVER_DIR);
  if (result.datapacks === null) {
    return res.status(404).json({ error: `Datapacks folder not found for world "${result.world}"` });
  }

  let loaded = [];
  if (RCON_PASSWORD) {
    try {
      const output = await rcon.send('datapack list');
      loaded = parseRconDatapackList(output);
    } catch {
      // RCON unavailable
    }
  }

  const loadedNames = new Set(loaded.filter((d) => d.enabled).map((d) => d.name.toLowerCase()));
  const datapacks = result.datapacks.map((dp) => ({
    ...dp,
    loaded: loadedNames.has(dp.name.toLowerCase())
      || [...loadedNames].some((n) => n.includes(dp.name.toLowerCase()) || dp.name.toLowerCase().includes(n)),
  }));

  res.json({
    count: datapacks.length,
    loadedCount: datapacks.filter((d) => d.loaded).length,
    world: result.world,
    path: result.path,
    loaded,
    datapacks,
  });
});

app.get('/api/content', async (_req, res) => {
  if (!SERVER_DIR) return res.status(404).json({ error: 'SERVER_DIR not configured' });

  const plugins = listPlugins(SERVER_DIR) || [];
  const datapackResult = listDatapacks(SERVER_DIR);
  const datapacks = datapackResult.datapacks || [];

  let pluginLoaded = [];
  let datapackLoaded = [];
  if (RCON_PASSWORD) {
    try {
      const [pluginsOut, datapacksOut] = await Promise.all([
        rcon.send('plugins').catch(() => ''),
        rcon.send('datapack list').catch(() => ''),
      ]);
      pluginLoaded = parseRconPluginList(pluginsOut);
      datapackLoaded = parseRconDatapackList(datapacksOut);
    } catch { /* ignore */ }
  }

  const loadedDpNames = new Set(datapackLoaded.filter((d) => d.enabled).map((d) => d.name.toLowerCase()));

  res.json({
    plugins: {
      count: plugins.length,
      enabledCount: plugins.filter((p) => p.enabled).length,
      loadedCount: mergePluginLoadState(plugins, pluginLoaded).filter((p) => p.loaded).length,
      items: mergePluginLoadState(plugins, pluginLoaded),
    },
    datapacks: {
      count: datapacks.length,
      world: datapackResult.world,
      path: datapackResult.path,
      loadedCount: datapacks.filter((dp) =>
        loadedDpNames.has(dp.name.toLowerCase()),
      ).length,
      items: datapacks.map((dp) => ({
        ...dp,
        loaded: loadedDpNames.has(dp.name.toLowerCase())
          || [...loadedDpNames].some((n) => n.includes(dp.name.toLowerCase())),
      })),
    },
  });
});

const server = http.createServer(app);
const wss = new WebSocketServer({ server, path: '/ws' });

wss.on('connection', async (ws) => {
  let bridgeHistorySent = false;

  if (bridge.isConfigured() && bridgeAvailable) {
    try {
      const data = await bridge.getLogs(500);
      ws.send(JSON.stringify({ type: 'history', lines: data.lines || [], source: 'bridge' }));
      bridgeHistorySent = true;
    } catch { /* ignore */ }
  }

  if (!bridgeHistorySent && logTailer) {
    ws.send(JSON.stringify({ type: 'history', lines: logTailer.getHistory(), source: 'file' }));

    const unsubscribe = logTailer.subscribe((event) => {
      if (event.type === 'player_event') {
        metrics.recordPlayerEvent(event.event);
      }
      if (ws.readyState === ws.OPEN) {
        ws.send(JSON.stringify(event));
      }
    });

    ws.on('close', unsubscribe);
  } else if (!bridgeHistorySent) {
    ws.send(JSON.stringify({
      type: 'system',
      message: 'Log tailing unavailable — configure SERVER_DIR, LOG_FILE, or MonitorBridge plugin',
    }));
  }
});

bridge.on('event', (event) => {
  const line = formatBridgeLogLine(event);
  broadcast({ type: 'log', line, bridge: true, event });

  if (event.type === 'join' || event.type === 'quit') {
    metrics.recordPlayerEvent(event.type === 'join' ? 'join' : 'leave');
    broadcast({
      type: 'player_event',
      event: event.type === 'join' ? 'join' : 'leave',
      player: event.data?.player || event.message?.split(' ')[0] || 'Unknown',
    });
  }
});

bridge.on('connected', () => {
  bridgeAvailable = true;
  broadcast({ type: 'bridge_status', connected: true });
  console.log('MonitorBridge connected — using live plugin data');
});

bridge.on('disconnected', () => {
  bridgeAvailable = false;
  broadcast({ type: 'bridge_status', connected: false });
});

if (bridge.isConfigured()) {
  bridge.connectEvents();
  bridge.health().then(() => {
    bridgeAvailable = true;
    console.log(`MonitorBridge detected at ${BRIDGE_URL}`);
  }).catch(() => {
    console.warn(`MonitorBridge not reachable at ${BRIDGE_URL} — using RCON/file fallback`);
  });
}

setInterval(async () => {
  try {
    const status = await fetchCombinedStatus();
    const payload = JSON.stringify({
      type: 'status',
      ...status,
      bridgeConnected: bridgeAvailable,
      metrics: metrics.getHistory(status),
      server: SERVER_DIR ? (() => {
        const props = readServerProperties(SERVER_DIR);
        return props ? {
          motd: status.server?.motd || props['motd'],
          difficulty: props['difficulty'],
          gamemode: props['gamemode'],
        } : status.server || null;
      })() : status.server || null,
    });
    for (const client of wss.clients) {
      if (client.readyState === client.OPEN) {
        client.send(payload);
      }
    }
  } catch {
    // ignore background poll errors
  }
}, 5000);

server.listen(PORT, () => {
  console.log(`Minecraft Monitor running at http://localhost:${PORT}`);

  if (!RCON_PASSWORD) {
    console.warn('Warning: RCON_PASSWORD is not set. RCON commands will fail.');
  }

  if (authRequired(DASHBOARD_PASSWORD)) {
    console.log('Dashboard authentication is enabled.');
  }

  if (bridge.isConfigured()) {
    console.log(`MonitorBridge URL: ${BRIDGE_URL}`);
  }

  if (!LOG_FILE) {
    console.warn('Warning: SERVER_DIR / LOG_FILE not set. Log tailing disabled.');
  } else if (!fs.existsSync(LOG_FILE)) {
    console.warn(`Warning: Log file not found yet: ${LOG_FILE}`);
  } else {
    console.log(`Tailing log file: ${LOG_FILE}`);
  }
});

process.on('SIGINT', async () => {
  bridge.stop();
  if (logTailer) await logTailer.stop();
  await rcon.close();
  process.exit(0);
});
