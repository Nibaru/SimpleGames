class MetricsStore {
  constructor({ maxSamples = 120 } = {}) {
    this.maxSamples = maxSamples;
    this.tpsHistory = [];
    this.lastOnline = null;
    this.lastOffline = null;
    this.onlineSince = null;
    this.wasOnline = false;
    this.peakPlayers = 0;
    this.totalJoinEvents = 0;
    this.totalLeaveEvents = 0;
  }

  recordStatus(status) {
    const now = Date.now();

    if (status.online) {
      if (!this.wasOnline) {
        this.onlineSince = now;
      }
      this.wasOnline = true;
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
      if (this.wasOnline) {
        this.onlineSince = null;
      }
      this.wasOnline = false;
      this.lastOffline = now;
    }
  }

  recordPlayerEvent(event) {
    if (event === 'join') this.totalJoinEvents += 1;
    if (event === 'leave') this.totalLeaveEvents += 1;
  }

  computeHealth(status) {
    if (!status.online) return 0;

    let score = 100;
    const tps = status.tps ?? 20;
    const mspt = status.mspt ?? 0;

    if (tps < 20) score -= (20 - tps) * 8;
    if (tps < 15) score -= 10;
    if (tps < 10) score -= 20;
    if (mspt > 50) score -= Math.min(30, (mspt - 50) * 0.6);
    if (mspt > 80) score -= 15;

    return Math.max(0, Math.min(100, Math.round(score)));
  }

  getHistory(status = {}) {
    const uptimeMs = this.onlineSince ? Date.now() - this.onlineSince : 0;

    return {
      tps: [...this.tpsHistory],
      peakPlayers: this.peakPlayers,
      lastOnline: this.lastOnline,
      lastOffline: this.lastOffline,
      onlineSince: this.onlineSince,
      uptimeMs,
      health: this.computeHealth(status),
      joinEvents: this.totalJoinEvents,
      leaveEvents: this.totalLeaveEvents,
    };
  }
}

module.exports = { MetricsStore };
