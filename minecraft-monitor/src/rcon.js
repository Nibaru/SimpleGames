const { Rcon } = require('rcon-client');

class RconService {
  constructor({ host, port, password }) {
    this.host = host;
    this.port = port;
    this.password = password;
    this.client = null;
    this.connecting = null;
    this.cachedVersion = null;
    this.versionFetchedAt = 0;
  }

  async connect() {
    if (this.client?.authenticated) {
      return this.client;
    }

    if (this.connecting) {
      return this.connecting;
    }

    this.connecting = Rcon.connect({
      host: this.host,
      port: this.port,
      password: this.password,
    }).then((client) => {
      this.client = client;
      this.connecting = null;

      client.on('end', () => {
        this.client = null;
      });

      return client;
    }).catch((err) => {
      this.connecting = null;
      throw err;
    });

    return this.connecting;
  }

  async send(command) {
    const client = await this.connect();
    return client.send(command);
  }

  async getVersion() {
    const now = Date.now();
    if (this.cachedVersion && now - this.versionFetchedAt < 300_000) {
      return this.cachedVersion;
    }

    try {
      const output = await this.send('version');
      this.cachedVersion = parseVersion(output);
      this.versionFetchedAt = now;
      return this.cachedVersion;
    } catch {
      return null;
    }
  }

  async getStatus() {
    try {
      const [listOutput, tpsOutput, version] = await Promise.all([
        this.send('list'),
        this.send('tps').catch(() => null),
        this.getVersion(),
      ]);

      const players = parsePlayerList(listOutput);
      const tpsData = tpsOutput ? parseTps(tpsOutput) : { tps: null, mspt: null };

      return {
        online: true,
        players,
        tps: tpsData.tps,
        mspt: tpsData.mspt,
        version,
        raw: { list: listOutput, tps: tpsOutput },
      };
    } catch (err) {
      return {
        online: false,
        error: err.message,
        players: { count: 0, max: 0, names: [] },
        tps: null,
        mspt: null,
        version: this.cachedVersion,
      };
    }
  }

  async close() {
    if (this.client) {
      await this.client.end();
      this.client = null;
    }
  }
}

function parsePlayerList(output) {
  const match = output.match(/There are (\d+) of a max of (\d+) players online(?:: (.+))?/i);
  if (!match) {
    return { count: 0, max: 0, names: [] };
  }

  const names = match[3]
    ? match[3].split(',').map((name) => name.trim()).filter(Boolean)
    : [];

  return {
    count: parseInt(match[1], 10),
    max: parseInt(match[2], 10),
    names,
  };
}

function parseVersion(output) {
  const match = output.match(/This server is running (.+?) \(MC: ([\d.]+)\)/i)
    || output.match(/version[:\s]+(.+)/i);
  if (!match) return { raw: output.trim() };
  return {
    name: match[1]?.trim(),
    minecraft: match[2]?.trim(),
    raw: output.trim(),
  };
}

function parseTps(output) {
  const lines = output.split('\n').filter(Boolean);
  const values = [];
  let mspt = null;

  for (const line of lines) {
    const tpsMatch = line.match(/([\d.]+)\s*TPS/i) || line.match(/TPS from last [\d]+s: ([\d.]+)/i);
    if (tpsMatch) {
      values.push(parseFloat(tpsMatch[1]));
    }

    const msptMatch = line.match(/([\d.]+)\s*mspt/i) || line.match(/MSPT[:\s]+([\d.]+)/i);
    if (msptMatch) {
      mspt = parseFloat(msptMatch[1]);
    }
  }

  let tps = null;
  if (values.length > 0) {
    tps = values[0];
  } else {
    const simple = output.match(/([\d.]+)/);
    tps = simple ? parseFloat(simple[1]) : null;
  }

  return { tps, mspt };
}

module.exports = { RconService };
