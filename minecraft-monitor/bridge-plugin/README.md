# ServerMonitor — single-file PaperMC dashboard

One JAR. Drop it in `plugins/`, start the server, open the dashboard. No Node.js, no RCON, no separate monitor process.

## Quick start

### 1. Build (or download from releases)

```bash
cd minecraft-monitor/bridge-plugin
mvn package
```

Copy `target/ServerMonitor-1.0.0.jar` to your Paper server's `plugins/` folder.

### 2. Start the server

On first run the plugin creates `plugins/ServerMonitor/config.yml`. Restart after editing.

### 3. Open the dashboard

Default URL: **http://127.0.0.1:8765**

If you set an API key or dashboard password in config, sign in with that value.

## Configure

`plugins/ServerMonitor/config.yml`:

```yaml
http:
  enabled: true
  host: 127.0.0.1    # keep localhost — use SSH tunnel for remote access
  port: 8765
  api-key: your-secret-key-here

dashboard:
  password: ""       # optional extra password for the web UI

events:
  buffer-size: 2000
  log-chat: true
  log-commands: true
  log-deaths: true
```

**Security:** bind to `127.0.0.1`, set a strong `api-key`, and do not expose port 8765 to the public internet.

### Remote access (SSH tunnel)

```bash
ssh -L 8765:127.0.0.1:8765 user@your-server
```

Then open http://localhost:8765 on your machine.

## What's included in the JAR

| Component | Description |
|-----------|-------------|
| Web dashboard | HTML/CSS/JS served from the plugin |
| Live stats | TPS, MSPT, players, memory, worlds |
| Event logs | Joins, quits, chat, deaths, commands, kicks |
| Server console | Commands run in-process via Bukkit (no RCON) |
| File readers | Whitelist, ops, banned, plugins, datapacks, `latest.log` |

## API endpoints

| Endpoint | Auth | Description |
|----------|------|-------------|
| `GET /` | No | Dashboard UI |
| `GET /api/config` | No | Auth flags and quick commands |
| `POST /api/login` | No | Sign in (API key or dashboard password) |
| `GET /health` | No | Health check |
| `GET /api/status` | Yes | TPS, MSPT, players, memory |
| `GET /api/events` | Yes | SSE live event stream |
| `POST /api/command` | Yes | Run a server command |
| `GET /api/plugins` | Yes | Loaded + file plugins |
| `GET /api/datapacks` | Yes | World datapacks |
| `GET /api/whitelist` | Yes | Whitelist names |
| `GET /api/ops` | Yes | Operator list |
| `GET /api/banned` | Yes | Banned players |
| `GET /api/logs` | Yes | Recent structured events |
| `GET /api/logs/download` | Yes | Download `latest.log` |

Auth: `Authorization: Bearer <api-key-or-session-token>`, `X-Api-Key` header, or `?apiKey=` query param (used by SSE).

## Requirements

- Paper 1.21+ (or compatible fork)
- Java 17+

## Legacy Node.js monitor

The `minecraft-monitor/` Node app is kept for development and optional external hosting. For production on your game server, use this plugin only — it replaces Node, RCON tailing, and the old MonitorBridge split setup.
