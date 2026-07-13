require('dotenv').config();

const http = require('http');
const path = require('path');
const fs = require('fs');
const express = require('express');
const { WebSocketServer } = require('ws');

const { RconService } = require('./src/rcon');
const { LogTailer } = require('./src/logTailer');

const PORT = parseInt(process.env.PORT || '3000', 10);
const SERVER_DIR = process.env.SERVER_DIR || '';
const LOG_FILE = process.env.LOG_FILE || (SERVER_DIR ? path.join(SERVER_DIR, 'logs', 'latest.log') : '');
const RCON_HOST = process.env.RCON_HOST || '127.0.0.1';
const RCON_PORT = parseInt(process.env.RCON_PORT || '25575', 10);
const RCON_PASSWORD = process.env.RCON_PASSWORD || '';
const LOG_HISTORY_LINES = parseInt(process.env.LOG_HISTORY_LINES || '500', 10);

const QUICK_COMMANDS = [
  { id: 'list', label: 'List Players', command: 'list', icon: '👥' },
  { id: 'save-all', label: 'Save World', command: 'save-all', icon: '💾' },
  { id: 'save-off', label: 'Disable Saves', command: 'save-off', icon: '🔒' },
  { id: 'save-on', label: 'Enable Saves', command: 'save-on', icon: '🔓' },
  { id: 'whitelist-reload', label: 'Reload Whitelist', command: 'whitelist reload', icon: '📋' },
  { id: 'reload', label: 'Reload Config', command: 'reload confirm', icon: '🔄' },
  { id: 'clear', label: 'Clear Weather', command: 'weather clear', icon: '☀️' },
  { id: 'day', label: 'Set Day', command: 'time set day', icon: '🌅' },
  { id: 'night', label: 'Set Night', command: 'time set night', icon: '🌙' },
  { id: 'peaceful', label: 'Peaceful', command: 'difficulty peaceful', icon: '🕊️' },
  { id: 'easy', label: 'Easy', command: 'difficulty easy', icon: '🌱' },
  { id: 'normal', label: 'Normal', command: 'difficulty normal', icon: '⚔️' },
  { id: 'hard', label: 'Hard', command: 'difficulty hard', icon: '💀' },
  { id: 'tps', label: 'Check TPS', command: 'tps', icon: '📊' },
];

const app = express();
app.use(express.json());
app.use(express.static(path.join(__dirname, 'public')));

const rcon = new RconService({
  host: RCON_HOST,
  port: RCON_PORT,
  password: RCON_PASSWORD,
});

let logTailer = null;

if (LOG_FILE) {
  logTailer = new LogTailer(LOG_FILE, { historyLines: LOG_HISTORY_LINES });
  logTailer.start().catch((err) => {
    console.error(`Failed to start log tailer: ${err.message}`);
  });
}

app.get('/api/config', (_req, res) => {
  res.json({
    logFile: LOG_FILE || null,
    rcon: { host: RCON_HOST, port: RCON_PORT },
    quickCommands: QUICK_COMMANDS.map(({ id, label, icon }) => ({ id, label, icon })),
  });
});

app.get('/api/status', async (_req, res) => {
  const status = await rcon.getStatus();
  res.json({
    ...status,
    logFile: LOG_FILE || null,
    logAvailable: Boolean(logTailer),
  });
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

server.listen(PORT, () => {
  console.log(`Minecraft Monitor running at http://localhost:${PORT}`);

  if (!RCON_PASSWORD) {
    console.warn('Warning: RCON_PASSWORD is not set. RCON commands will fail.');
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
