const fs = require('fs');
const path = require('path');

function readJsonNames(filePath, nameKey = 'name') {
  if (!fs.existsSync(filePath)) return null;

  try {
    const data = JSON.parse(fs.readFileSync(filePath, 'utf8'));
    if (!Array.isArray(data)) return null;
    return data.map((entry) => entry[nameKey]).filter(Boolean);
  } catch {
    return null;
  }
}

function readWhitelist(serverDir) {
  return readJsonNames(path.join(serverDir, 'whitelist.json'));
}

function readOps(serverDir) {
  return readJsonNames(path.join(serverDir, 'ops.json'));
}

function readBannedPlayers(serverDir) {
  const file = path.join(serverDir, 'banned-players.json');
  if (!fs.existsSync(file)) return null;

  try {
    const data = JSON.parse(fs.readFileSync(file, 'utf8'));
    if (!Array.isArray(data)) return null;
    return data.map((entry) => ({
      name: entry.name,
      reason: entry.reason || '',
      source: entry.source || '',
    })).filter((entry) => entry.name);
  } catch {
    return null;
  }
}

function listPlugins(serverDir) {
  const pluginsDir = path.join(serverDir, 'plugins');
  if (!fs.existsSync(pluginsDir)) return null;

  try {
    return fs.readdirSync(pluginsDir)
      .filter((file) => file.endsWith('.jar'))
      .map((file) => ({
        name: file.replace(/\.jar$/i, ''),
        file,
      }))
      .sort((a, b) => a.name.localeCompare(b.name));
  } catch {
    return null;
  }
}

function readServerProperties(serverDir) {
  const file = path.join(serverDir, 'server.properties');
  if (!fs.existsSync(file)) return null;

  try {
    const content = fs.readFileSync(file, 'utf8');
    const props = {};
    for (const line of content.split('\n')) {
      const trimmed = line.trim();
      if (!trimmed || trimmed.startsWith('#')) continue;
      const idx = trimmed.indexOf('=');
      if (idx === -1) continue;
      props[trimmed.slice(0, idx).trim()] = trimmed.slice(idx + 1).trim();
    }
    return props;
  } catch {
    return null;
  }
}

module.exports = {
  readWhitelist,
  readOps,
  readBannedPlayers,
  listPlugins,
  readServerProperties,
};
