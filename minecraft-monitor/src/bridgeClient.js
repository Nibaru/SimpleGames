const http = require('http');
const https = require('https');
const { EventEmitter } = require('events');

class BridgeClient extends EventEmitter {
  constructor({ url, apiKey } = {}) {
    super();
    this.baseUrl = (url || '').replace(/\/$/, '');
    this.apiKey = apiKey || '';
    this.connected = false;
    this.sseReq = null;
    this.reconnectTimer = null;
  }

  isConfigured() {
    return Boolean(this.baseUrl);
  }

  getHeaders() {
    const headers = { Accept: 'application/json' };
    if (this.apiKey) {
      headers.Authorization = `Bearer ${this.apiKey}`;
    }
    return headers;
  }

  request(path) {
    return new Promise((resolve, reject) => {
      if (!this.isConfigured()) {
        return reject(new Error('Bridge URL not configured'));
      }

      const url = new URL(path, this.baseUrl);
      const lib = url.protocol === 'https:' ? https : http;

      const req = lib.request(url, { headers: this.getHeaders(), timeout: 5000 }, (res) => {
        let body = '';
        res.on('data', (chunk) => { body += chunk; });
        res.on('end', () => {
          if (res.statusCode >= 400) {
            return reject(new Error(`Bridge ${res.statusCode}: ${body}`));
          }
          try {
            resolve(JSON.parse(body));
          } catch {
            reject(new Error('Invalid JSON from bridge'));
          }
        });
      });

      req.on('error', reject);
      req.on('timeout', () => {
        req.destroy();
        reject(new Error('Bridge request timeout'));
      });
      req.end();
    });
  }

  async health() {
    return this.request('/health');
  }

  async getStatus() {
    return this.request('/api/status');
  }

  async getLogs(limit = 200) {
    return this.request(`/api/logs?limit=${limit}`);
  }

  async getPlugins() {
    return this.request('/api/plugins');
  }

  connectEvents() {
    if (!this.isConfigured()) return;

    if (this.sseReq) {
      this.sseReq.destroy();
      this.sseReq = null;
    }

    const url = new URL('/api/events', this.baseUrl);
    const lib = url.protocol === 'https:' ? https : http;

    const req = lib.request(url, {
      headers: { ...this.getHeaders(), Accept: 'text/event-stream' },
      timeout: 0,
    }, (res) => {
      if (res.statusCode >= 400) {
        this.connected = false;
        this.emit('disconnected');
        this.scheduleReconnect();
        return;
      }

      this.connected = true;
      this.emit('connected');

      let buffer = '';
      res.on('data', (chunk) => {
        buffer += chunk.toString();
        const parts = buffer.split('\n\n');
        buffer = parts.pop() || '';

        for (const part of parts) {
          const line = part.split('\n').find((l) => l.startsWith('data: '));
          if (!line) continue;
          try {
            const event = JSON.parse(line.slice(6));
            this.emit('event', event);
          } catch {
            // ignore malformed
          }
        }
      });

      res.on('end', () => {
        this.connected = false;
        this.emit('disconnected');
        this.scheduleReconnect();
      });
    });

    req.on('error', () => {
      this.connected = false;
      this.emit('disconnected');
      this.scheduleReconnect();
    });

    req.end();
    this.sseReq = req;
  }

  scheduleReconnect() {
    if (this.reconnectTimer) clearTimeout(this.reconnectTimer);
    this.reconnectTimer = setTimeout(() => this.connectEvents(), 5000);
  }

  stop() {
    if (this.reconnectTimer) clearTimeout(this.reconnectTimer);
    if (this.sseReq) this.sseReq.destroy();
    this.connected = false;
  }
}

function mapBridgeStatus(bridgeStatus, metrics) {
  const players = bridgeStatus.players || {};
  return {
    online: true,
    bridge: true,
    players: {
      count: players.count ?? 0,
      max: players.max ?? 0,
      names: players.names || [],
      details: players.details || [],
    },
    tps: bridgeStatus.tps ?? null,
    mspt: bridgeStatus.mspt ?? null,
    tps1m: bridgeStatus.tps1m ?? null,
    tps5m: bridgeStatus.tps5m ?? null,
    tps15m: bridgeStatus.tps15m ?? null,
    version: {
      name: bridgeStatus.bukkitVersion || bridgeStatus.serverVersion,
      raw: bridgeStatus.serverVersion,
    },
    server: {
      motd: bridgeStatus.motd,
      maxPlayers: bridgeStatus.maxPlayers,
    },
    memory: bridgeStatus.memory,
    worlds: bridgeStatus.worlds,
    metrics,
  };
}

module.exports = { BridgeClient, mapBridgeStatus };
