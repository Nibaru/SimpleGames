const fs = require('fs');
const path = require('path');

const DEFAULT_COMMANDS = [
  { id: 'list', label: 'List Players', command: 'list', icon: '👥' },
  { id: 'save-all', label: 'Save World', command: 'save-all', icon: '💾' },
  { id: 'save-off', label: 'Disable Saves', command: 'save-off', icon: '🔒' },
  { id: 'save-on', label: 'Enable Saves', command: 'save-on', icon: '🔓' },
  { id: 'whitelist-reload', label: 'Reload Whitelist', command: 'whitelist reload', icon: '📋' },
  { id: 'reload', label: 'Reload Config', command: 'reload confirm', icon: '🔄' },
  { id: 'clear', label: 'Clear Weather', command: 'weather clear', icon: '☀️' },
  { id: 'rain', label: 'Set Rain', command: 'weather rain', icon: '🌧️' },
  { id: 'thunder', label: 'Thunder', command: 'weather thunder', icon: '⛈️' },
  { id: 'day', label: 'Set Day', command: 'time set day', icon: '🌅' },
  { id: 'night', label: 'Set Night', command: 'time set night', icon: '🌙' },
  { id: 'noon', label: 'Set Noon', command: 'time set noon', icon: '🕛' },
  { id: 'peaceful', label: 'Peaceful', command: 'difficulty peaceful', icon: '🕊️' },
  { id: 'easy', label: 'Easy', command: 'difficulty easy', icon: '🌱' },
  { id: 'normal', label: 'Normal', command: 'difficulty normal', icon: '⚔️' },
  { id: 'hard', label: 'Hard', command: 'difficulty hard', icon: '💀' },
  { id: 'tps', label: 'Check TPS', command: 'tps', icon: '📊' },
  { id: 'gc', label: 'Run GC', command: 'gc', icon: '🗑️' },
];

function loadQuickCommands(configPath) {
  let custom = [];

  if (configPath && fs.existsSync(configPath)) {
    try {
      const parsed = JSON.parse(fs.readFileSync(configPath, 'utf8'));
      if (Array.isArray(parsed)) {
        custom = parsed;
      }
    } catch (err) {
      console.warn(`Failed to load quick commands from ${configPath}: ${err.message}`);
    }
  }

  const merged = [...DEFAULT_COMMANDS];
  const seen = new Set(DEFAULT_COMMANDS.map((cmd) => cmd.id));

  for (const cmd of custom) {
    if (!cmd.id || !cmd.command) continue;
    if (seen.has(cmd.id)) {
      const idx = merged.findIndex((c) => c.id === cmd.id);
      merged[idx] = { ...merged[idx], ...cmd };
    } else {
      merged.push(cmd);
      seen.add(cmd.id);
    }
  }

  return merged;
}

module.exports = { loadQuickCommands, DEFAULT_COMMANDS };
