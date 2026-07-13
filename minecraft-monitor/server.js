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

const PORT = parseInt(process.env.PORT || '3000', 10);
const SERVER_DIR = process.env.SERVER_DIR || '';
const LOG_FILE = process.env.LOG_FILE || (SERVER_DIR ? path.join(SERVER_DIR, 'logs', 'latest.log') : '');
const RCON_HOST = process.env.RCON_HOST || '127.0.0.1';
const RCON_PORT = parseInt(process.env.RCON_PORT || '25575', 10);
const RCON_PASSWORD = process.env.RCON_PASSWORD || '';
const LOG_HISTORY_LINES = parseInt(process.env.LOG_HISTORY_LINES || '500', 10);
const DASHBOARD_PASSWORD = process.env.DASHBOARD_PASSWORD || '';
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

let logTailer = null;

if (LOG_FILE) {
  logTailer = new LogTailer(LOG_FILE, { historyLines: LOG_HISTORY_LINES });
  logTailer.start().catch((err) => {
    console.error(`Failed to start log tailer: ${err.message}`);
  });
}

function readWhitelist() {
  if (!SERVER_DIR) return null;
  const file = path.join(SERVER_DIR, 'whitelist.json');
  if (!fs.existsSync(file)) return null;

  try {
    const data = JSON.parse(fs.readFileSync(file, 'utf8'));
    return Array.isArray(data) ? data.map((entry) => entry.name).filter(Boolean) : null;
  } catch {
    return null;
  }
}

function readServerProperties() {
  if (!SERVER_DIR) return null;
  const file = path.join(SERVER_DIR, 'server.properties');
  if (!fs.existsSync(file)) return null;

  try {
    const content = fs.readFileSync(file, 'utf8');
    const props = {};
    for (const line of content.split('\n')) {
      const trimmed = line.trim();
      if (!trimmed || trimmed.startsWith('#')) continue;
      const idx = trimmed.indexOf('=');
      if (idx === -1) continue;
      props[trimmed.slice(0, idx).trim()] = trimmed.slice(idx + 1).trim();
    }
    return props;
  } catch {
    return null;
  }
}

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
  });
});

app.get('/api/status', async (_req, res) => {
  const status = await rcon.getStatus();
  metrics.recordStatus(status);

  const props = readServerProperties();
  res.json({
    ...status,
    logFile: LOG_FILE || null,
    logAvailable: Boolean(logTailer),
    server: props ? {
      motd: props['motd'],
      maxPlayers: props['max-players'],
      gamemode: props['gamemode'],
      difficulty: props['difficulty'],
      pvp: props['pvp'],
      seed: props['level-seed'] || null,
    } : null,
    metrics: metrics.getHistory(),
  });
});

app.get('/api/metrics', (_req, res) => {
  res.json(metrics.getHistory());
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

app.get('/api/logs/history', (_req, res) => {
  if (!logTailer) {
    return res.status(503).json({ error: 'Log file not configured. Set SERVER_DIR or LOG_FILE in .env' });
  }
  res.json({ lines: logTailer.getHistory() });
});

app.get('/api/logs/download', (_req, res) => {
  if (!LOG_FILE || !fs.existsSync(LOG_FILE)) {
    return res.status(404).json({ error: 'Log file not available' });
  }
  res.download(LOG_FILE, path.basename(LOG_FILE));
});

app.get('/api/whitelist', (_req, res) => {
  const names = readWhitelist();
  if (names === null) {
    return res.status(404).json({ error: 'Whitelist file not found' });
  }
  res.json({ count: names.length, names });
});

const server = http.createServer(app);
const wss = new WebSocketServer({ server, path: '/ws' });

wss.on('connection', (ws) => {
  if (logTailer) {
    ws.send(JSON.stringify({ type: 'history', lines: logTailer.getHistory() }));

    const unsubscribe = logTailer.subscribe((event) => {
      if (ws.readyState === ws.OPEN) {
        ws.send(JSON.stringify(event));
      }
    });

    ws.on('close', unsubscribe);
  } else {
    ws.send(JSON.stringify({
      type: 'system',
      message: 'Log tailing unavailable — configure SERVER_DIR or LOG_FILE',
    }));
  }
});

setInterval(async () => {
  try {
    const status = await rcon.getStatus();
    metrics.recordStatus(status);
    const payload = JSON.stringify({ type: 'status', ...status, metrics: metrics.getHistory() });
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

  if (!LOG_FILE) {
    console.warn('Warning: SERVER_DIR / LOG_FILE not set. Log tailing disabled.');
  } else if (!fs.existsSync(LOG_FILE)) {
    console.warn(`Warning: Log file not found yet: ${LOG_FILE}`);
  } else {
    console.log(`Tailing log file: ${LOG_FILE}`);
  }
});

process.on('SIGINT', async () => {
  if (logTailer) await logTailer.stop();
  await rcon.close();
  process.exit(0);
});
