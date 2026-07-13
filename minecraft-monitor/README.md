# Minecraft Server Monitor

A web dashboard for monitoring **PaperMC** (and other Minecraft Java Edition) servers. View live server logs, run RCON commands from a terminal, and trigger common admin actions with one click.

![Node.js](https://img.shields.io/badge/node-%3E%3D18-brightgreen)

## Features

### Monitoring
- **Live log viewer** — tails `logs/latest.log` in real time via WebSocket
- **Log filtering** — search text and filter by info / warn / error / player events
- **Syntax highlighting** — parses Minecraft log timestamps and log levels
- **Download logs** — grab the current log file from the dashboard
- **Join/leave toasts** — notifications when players connect or disconnect

### Server Control
- **RCON terminal** — send any server command with command history (↑↓) and Tab suggestions
- **Quick command buttons** — save world, difficulty, time/weather, TPS, GC, and more
- **Custom commands** — add your own buttons via `quick-commands.json`
- **Broadcast** — send a `say` message to all players
- **Player actions** — message, kick, ban, or op players from the sidebar
- **Scheduled restart** — countdown with in-game warnings, then `stop`
- **Whitelist viewer** — reads `whitelist.json` from your server directory

### Status & Metrics
- **Live status** — online/offline, player count, TPS, MSPT
- **TPS chart** — sparkline of recent performance
- **Peak players** — session peak player count
- **Server version** — PaperMC / Minecraft version via RCON
- **Server properties** — MOTD, gamemode, difficulty from `server.properties`

### UI & UX
- **3 themes** — Dark, Light, and Minecraft-inspired
- **Tabbed navigation** — Overview, Players, Commands, Server panels
- **Health score ring** — computed from TPS and MSPT
- **TPS sparkline** — with gradient fill in header
- **Command palette** — Ctrl+K fuzzy search for commands
- **Favorite commands** — star quick commands to pin them
- **Player avatars** — Minecraft heads via Minotar
- **Resizable split pane** — drag to resize logs vs console
- **Workspace tabs** — Logs-only, Console-only, or Split view
- **Fullscreen logs** — expand log panel
- **Click-to-copy** log lines
- **Keyboard shortcuts** — `/` filter, `` ` `` console, `?` help
- **Settings panel** — font sizes, sounds, compact sidebar
- **Connection pills** — live Logs/RCON status indicators

### Security
- **Optional dashboard password** — session-based auth via `DASHBOARD_PASSWORD`

## Requirements

- Node.js 18+
- A running PaperMC server with **RCON enabled**
- The monitor must be able to read the server log file (run on the same machine, or mount the server directory)

## Setup

### 1. Enable RCON on your PaperMC server

Edit `server.properties`:

```properties
enable-rcon=true
rcon.port=25575
rcon.password=your-secure-password
```

Restart the server after changing these values.

### 2. Configure the monitor

```bash
cd minecraft-monitor
cp .env.example .env
```

Edit `.env`:

```env
PORT=3000
SERVER_DIR=/path/to/your/paper-server
RCON_HOST=127.0.0.1
RCON_PORT=25575
RCON_PASSWORD=your-secure-password
DASHBOARD_PASSWORD=your-dashboard-password   # optional but recommended
```

If your log file is in a non-standard location, set `LOG_FILE` directly instead of `SERVER_DIR`.

### 3. Custom quick commands (optional)

```bash
cp quick-commands.example.json quick-commands.json
# Edit quick-commands.json, then set in .env:
# QUICK_COMMANDS_FILE=./quick-commands.json
```

### 4. Install and run

```bash
npm install
npm start
```

Open **http://localhost:3000** in your browser.

For development with auto-restart on file changes:

```bash
npm run dev
```

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/login` | Authenticate (when `DASHBOARD_PASSWORD` is set) |
| GET | `/api/config` | Dashboard configuration |
| GET | `/api/status` | Server status, TPS, players, metrics |
| GET | `/api/metrics` | TPS history and peak players |
| POST | `/api/rcon` | Send an RCON command |
| GET | `/api/logs/history` | Recent log lines |
| GET | `/api/logs/download` | Download the log file |
| GET | `/api/whitelist` | Whitelist player names |
| GET | `/api/ops` | Operator list |
| GET | `/api/banned` | Banned players with reasons |
| GET | `/api/plugins` | Plugin JARs/folders with metadata + RCON load state |
| GET | `/api/datapacks` | World datapacks with pack.mcmeta + namespaces |
| GET | `/api/content` | Combined plugins + datapacks in one request |
| WS | `/ws` | Live log stream and status updates |

## Architecture

```
minecraft-monitor/
├── server.js              # Express + WebSocket server
├── src/
│   ├── auth.js            # Optional session auth
│   ├── metrics.js         # TPS history store
│   ├── quickCommands.js   # Default + custom commands
│   ├── rcon.js            # RCON client wrapper
│   └── logTailer.js       # Log file tailing + player events
└── public/
    ├── index.html
    ├── styles.css
    └── app.js
```

## Security Notes

This dashboard can stop your server and run arbitrary commands via RCON. **Do not expose it to the public internet** without authentication and a reverse proxy (e.g. nginx with basic auth or VPN-only access).

Recommended:

- Set `DASHBOARD_PASSWORD` in production
- Bind to localhost only, or put behind a reverse proxy with auth
- Use a strong RCON password
- Run the monitor on the same trusted network as your server

## License

MIT
