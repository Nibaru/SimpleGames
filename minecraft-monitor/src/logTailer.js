const fs = require('fs');
const path = require('path');
const readline = require('readline');

class LogTailer {
  constructor(logFilePath, { historyLines = 500 } = {}) {
    this.logFilePath = logFilePath;
    this.historyLines = historyLines;
    this.watchers = new Set();
    this.watcher = null;
    this.position = 0;
    this.buffer = [];
    this.started = false;
  }

  subscribe(callback) {
    this.watchers.add(callback);
    return () => this.watchers.delete(callback);
  }

  async start() {
    if (this.started) return;
    this.started = true;

    await this.ensureFile();
    const history = await this.readHistory();
    this.buffer = history;
    this.position = fs.statSync(this.logFilePath).size;

    this.watcher = fs.watch(this.logFilePath, { persistent: true }, () => {
      this.readNewLines().catch((err) => {
        this.emit({ type: 'error', message: `Log read error: ${err.message}` });
      });
    });
  }

  async stop() {
    if (this.watcher) {
      this.watcher.close();
      this.watcher = null;
    }
    this.started = false;
  }

  getHistory() {
    return [...this.buffer];
  }

  async ensureFile() {
    const dir = path.dirname(this.logFilePath);
    if (!fs.existsSync(dir)) {
      throw new Error(`Log directory not found: ${dir}`);
    }

    if (!fs.existsSync(this.logFilePath)) {
      fs.writeFileSync(this.logFilePath, '');
    }
  }

  async readHistory() {
    const content = fs.readFileSync(this.logFilePath, 'utf8');
    const lines = content.split('\n').filter((line) => line.length > 0);
    return lines.slice(-this.historyLines);
  }

  async readNewLines() {
    const stats = fs.statSync(this.logFilePath);

    if (stats.size < this.position) {
      this.position = 0;
      this.emit({ type: 'system', message: '--- Log file rotated ---' });
    }

    if (stats.size === this.position) {
      return;
    }

    const stream = fs.createReadStream(this.logFilePath, {
      start: this.position,
      end: stats.size - 1,
      encoding: 'utf8',
    });

    const rl = readline.createInterface({ input: stream, crlfDelay: Infinity });
    const lines = [];

    for await (const line of rl) {
      if (line.length > 0) {
        lines.push(line);
      }
    }

    this.position = stats.size;

    for (const line of lines) {
      this.buffer.push(line);
      if (this.buffer.length > this.historyLines * 2) {
        this.buffer = this.buffer.slice(-this.historyLines);
      }
      this.emit({ type: 'log', line });
    }
  }

  emit(event) {
    for (const callback of this.watchers) {
      callback(event);
    }
  }
}

module.exports = { LogTailer };
