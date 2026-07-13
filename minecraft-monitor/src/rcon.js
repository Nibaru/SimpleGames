const { Rcon } = require('rcon-client');

class RconService {
  constructor({ host, port, password }) {
    this.host = host;
    this.port = port;
    this.password = password;
    this.client = null;
    this.connecting = null;
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

  async getStatus() {
    try {
      const [listOutput, tpsOutput] = await Promise.all([
        this.send('list'),
        this.send('tps').catch(() => null),
      ]);

      const players = parsePlayerList(listOutput);
      const tps = tpsOutput ? parseTps(tpsOutput) : null;

      return {
        online: true,
        players,
        tps,
        raw: { list: listOutput, tps: tpsOutput },
      };
    } catch (err) {
      return {
        online: false,
        error: err.message,
        players: { count: 0, max: 0, names: [] },
        tps: null,
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

function parseTps(output) {
  const lines = output.split('\n').filter(Boolean);
  const values = [];

  for (const line of lines) {
    const match = line.match(/([\d.]+)\s*TPS/i) || line.match(/TPS from last [\d]+s: ([\d.]+)/i);
    if (match) {
      values.push(parseFloat(match[1]));
    }
  }

  if (values.length === 0) {
    const simple = output.match(/([\d.]+)/);
    return simple ? parseFloat(simple[1]) : null;
  }

  return values[0];
}

module.exports = { RconService };
