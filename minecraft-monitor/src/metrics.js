class MetricsStore {
  constructor({ maxSamples = 120 } = {}) {
    this.maxSamples = maxSamples;
    this.tpsHistory = [];
    this.lastOnline = null;
    this.lastOffline = null;
    this.peakPlayers = 0;
  }

  recordStatus(status) {
    const now = Date.now();

    if (status.online) {
      this.lastOnline = now;
      if (status.tps != null) {
        this.tpsHistory.push({ time: now, tps: status.tps, mspt: status.mspt ?? null });
        if (this.tpsHistory.length > this.maxSamples) {
          this.tpsHistory.shift();
        }
      }
      const count = status.players?.count ?? 0;
      if (count > this.peakPlayers) {
        this.peakPlayers = count;
      }
    } else {
      this.lastOffline = now;
    }
  }

  getHistory() {
    return {
      tps: [...this.tpsHistory],
      peakPlayers: this.peakPlayers,
      lastOnline: this.lastOnline,
      lastOffline: this.lastOffline,
    };
  }
}

module.exports = { MetricsStore };
