# MonitorBridge — PaperMC plugin for the web monitor

This plugin runs **on your Minecraft server** and exposes live data to the web dashboard over HTTP.

## What it provides

- **Accurate TPS / MSPT** from Paper (no RCON guessing)
- **Player list** with ping, gamemode, health, world
- **Structured event logs** — joins, quits, chat, deaths, commands, kicks, advancements
- **Live event stream** (SSE) to the web monitor
- **Plugin list** with versions from the running server

## Build

Requires Java 17+ and Maven:

```bash
cd bridge-plugin
mvn package
```

Copy `target/MonitorBridge-1.0.0.jar` to your server's `plugins/` folder.

## Configure the plugin

After first run, edit `plugins/MonitorBridge/config.yml`:

```yaml
http:
  enabled: true
  host: 127.0.0.1      # keep on localhost — only the monitor should connect
  port: 8765
  api-key: your-secret-key-here

events:
  buffer-size: 2000
  log-chat: true
  log-commands: true
  log-deaths: true
```

Restart the server after changing the config.

## Configure the web monitor

In `minecraft-monitor/.env`:

```env
SERVER_DIR=..
BRIDGE_URL=http://127.0.0.1:8765
BRIDGE_API_KEY=your-secret-key-here
```

The API key must match `http.api-key` in the plugin config.

## API endpoints (plugin)

| Endpoint | Description |
|----------|-------------|
| `GET /health` | Plugin health check |
| `GET /api/status` | TPS, MSPT, players, memory, worlds |
| `GET /api/players` | Online players with details |
| `GET /api/plugins` | Loaded plugins |
| `GET /api/logs?limit=200` | Structured event history |
| `GET /api/events` | SSE live event stream |

Auth header: `Authorization: Bearer your-api-key`

## Security

- Bind to `127.0.0.1` only (default)
- Set a strong `api-key`
- Do not expose port 8765 to the internet
