# Schellmonitor — single-file PaperMC dashboard

One JAR. Drop it in `plugins/`, start the server, open the dashboard. No Node.js, no RCON, no separate monitor process.

## Quick start

### Build and deploy (Windows — Algoma cluster server)

```bat
cd minecraft-monitor\bridge-plugin
build.bat
```

This runs `mvn package` and copies `Schellmonitor-1.0.0.jar` to `M:\AlgomaClusterServer\plugins`.

### Build manually

```bash
cd minecraft-monitor/bridge-plugin
mvn package
```

Default deploy path: `M:/AlgomaClusterServer/plugins` (override with `-Ddeploy.dir=...`).

### Open the dashboard

Default URL: **http://127.0.0.1:8765**

On first run the plugin creates `plugins/Schellmonitor/config.yml`. Restart after editing.

## Configure

`plugins/Schellmonitor/config.yml`:

```yaml
http:
  enabled: true
  host: 127.0.0.1
  port: 8765
  api-key: your-secret-key-here

dashboard:
  password: ""

events:
  buffer-size: 2000
  log-chat: true
  log-commands: true
  log-deaths: true
```

## Remote access (SSH tunnel)

```bash
ssh -L 8765:127.0.0.1:8765 user@your-server
```

## Requirements

- Paper 1.21+ (or compatible fork)
- Java 17+
- Maven (for building)
