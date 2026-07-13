# Minecraft Server Monitor

A web dashboard for monitoring **PaperMC** (and other Minecraft Java Edition) servers. View live server logs, run RCON commands from a terminal, and trigger common admin actions with one click.

![Node.js](https://img.shields.io/badge/node-%3E%3D18-brightgreen)

## Features

- **Live log viewer** — tails `logs/latest.log` in real time via WebSocket
- **RCON terminal** — send any server command and see the response
- **Quick command buttons** — save world, change difficulty, set time/weather, reload configs, and more
- **Server status** — online/offline indicator, player count, and TPS (PaperMC)
- **Player list** — shows who is currently online

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
```

If your log file is in a non-standard location, set `LOG_FILE` directly instead of `SERVER_DIR`.

### 3. Install and run

```bash
npm install
npm start
```

Open **http://localhost:3000** in your browser.

For development with auto-restart on file changes:

```bash
npm run dev
```

## Quick Commands

| Button | Command |
|--------|---------|
| List Players | `list` |
| Save World | `save-all` |
| Reload Whitelist | `whitelist reload` |
| Reload Config | `reload confirm` |
| Clear Weather | `weather clear` |
| Set Day / Night | `time set day` / `time set night` |
| Difficulty | `difficulty peaceful/easy/normal/hard` |
| Check TPS | `tps` |
| Stop Server | `stop` (requires confirmation) |

## Architecture

```
minecraft-monitor/
├── server.js          # Express + WebSocket server
├── src/
│   ├── rcon.js        # RCON client wrapper
│   └── logTailer.js   # Log file tailing
└── public/
    ├── index.html
    ├── styles.css
    └── app.js
```

- **REST API**: `/api/status`, `/api/rcon`, `/api/config`, `/api/logs/history`
- **WebSocket**: `/ws` — streams new log lines to connected clients

## Security Notes

This dashboard can stop your server and run arbitrary commands via RCON. **Do not expose it to the public internet** without authentication and a reverse proxy (e.g. nginx with basic auth or VPN-only access).

Recommended:

- Bind to localhost only, or put behind a reverse proxy with auth
- Use a strong RCON password
- Run the monitor on the same trusted network as your server

## License

MIT
