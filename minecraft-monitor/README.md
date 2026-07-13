# Minecraft Server Monitor

A web dashboard for **PaperMC** servers — live logs, server console, player list, plugins, datapacks, and admin quick actions.

## Recommended: single JAR deploy

**You only need one file:** `ServerMonitor-1.0.0.jar`

```bash
cd minecraft-monitor/bridge-plugin
mvn package
# Copy target/ServerMonitor-1.0.0.jar → your-server/plugins/
```

Start the server, then open **http://127.0.0.1:8765**.

Edit `plugins/ServerMonitor/config.yml` after first run:

```yaml
http:
  host: 127.0.0.1
  port: 8765
  api-key: your-secret-key
```

Full docs: [bridge-plugin/README.md](bridge-plugin/README.md)

### Why the plugin?

| | Plugin (recommended) | Node.js monitor (legacy) |
|--|----------------------|--------------------------|
| Deploy | One JAR in `plugins/` | Node 18+, `npm install`, separate process |
| Commands | In-process (Bukkit) | RCON required |
| TPS / players | Direct from Paper API | RCON or bridge polling |
| Logs | Structured events + `latest.log` | Tail `logs/latest.log` |

---

## Legacy: Node.js monitor

The Node app in this folder still works if you want to run the dashboard on a different machine or without the plugin. It requires RCON and optionally the plugin as a bridge.

### Requirements

- Node.js 18+
- RCON enabled on the server
- Read access to server files (same machine or mounted directory)

### Setup

```bash
cd minecraft-monitor
cp .env.example .env
# Edit .env — set SERVER_DIR, RCON_*, optional DASHBOARD_PASSWORD
npm install
npm start
```

Open **http://localhost:3000**.

### With MonitorBridge plugin (optional)

If you also install the plugin, set in `.env`:

```env
BRIDGE_URL=http://127.0.0.1:8765
BRIDGE_API_KEY=your-secret-key
```

The Node app will use the plugin for live stats and structured logs; RCON is still used for commands.

---

## Features

- **Live log viewer** with filtering, search, and download
- **Server console** — command history, suggestions, command palette (Ctrl+K)
- **Quick commands** — save, weather, time, TPS, whitelist, and custom buttons
- **Player sidebar** — kick, ban, op, message actions
- **TPS / MSPT chart** and health score
- **Plugins & datapacks** panels
- **Whitelist / ops / banned** viewers
- **Broadcast** and scheduled restart
- **3 themes** — dark, light, minecraft
- **Optional auth** — API key and/or dashboard password

## Security

This dashboard can stop your server and run arbitrary commands. Do not expose it to the public internet without authentication. Bind to localhost or use an SSH tunnel / reverse proxy with auth.

## License

MIT
