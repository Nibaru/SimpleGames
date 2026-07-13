const fs = require('fs');
const path = require('path');
const { execFileSync } = require('child_process');

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

function getWorldName(serverDir) {
  const props = readServerProperties(serverDir);
  return props?.['level-name'] || 'world';
}

function formatBytes(bytes) {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

function parseSimpleYaml(content) {
  const result = {};
  if (!content) return result;

  const nameMatch = content.match(/^name:\s*['"]?(.+?)['"]?\s*$/m);
  if (nameMatch) result.name = nameMatch[1].trim();

  const versionMatch = content.match(/^version:\s*['"]?(.+?)['"]?\s*$/m);
  if (versionMatch) result.version = versionMatch[1].trim();

  const apiMatch = content.match(/^api-version:\s*['"]?(.+?)['"]?\s*$/m);
  if (apiMatch) result.apiVersion = apiMatch[1].trim();

  const descMatch = content.match(/^description:\s*['"]?(.+?)['"]?\s*$/m);
  if (descMatch) result.description = descMatch[1].trim();

  const authorsMatch = content.match(/^authors:\s*\[(.+)\]/m);
  if (authorsMatch) {
    result.authors = authorsMatch[1].split(',').map((a) => a.trim().replace(/['"]/g, ''));
  } else {
    const authorMatch = content.match(/^author:\s*['"]?(.+?)['"]?\s*$/m);
    if (authorMatch) result.authors = [authorMatch[1].trim()];
  }

  const mainMatch = content.match(/^main:\s*['"]?(.+?)['"]?\s*$/m);
  if (mainMatch) result.main = mainMatch[1].trim();

  return result;
}

function readPluginYmlFromJar(jarPath) {
  try {
    const output = execFileSync('unzip', ['-p', jarPath, 'plugin.yml'], {
      encoding: 'utf8',
      stdio: ['pipe', 'pipe', 'ignore'],
      timeout: 3000,
    });
    return parseSimpleYaml(output);
  } catch {
    return {};
  }
}

function readPluginYmlFromFolder(folderPath) {
  const ymlPath = path.join(folderPath, 'plugin.yml');
  if (!fs.existsSync(ymlPath)) return {};
  try {
    return parseSimpleYaml(fs.readFileSync(ymlPath, 'utf8'));
  } catch {
    return {};
  }
}

function listPlugins(serverDir) {
  const pluginsDir = path.join(serverDir, 'plugins');
  if (!fs.existsSync(pluginsDir)) return null;

  const items = [];
  const seen = new Set();

  try {
    const entries = fs.readdirSync(pluginsDir, { withFileTypes: true });

    for (const entry of entries) {
      if (entry.name.startsWith('.')) continue;

      const fullPath = path.join(pluginsDir, entry.name);

      if (entry.isDirectory()) {
        if (entry.name === 'disabled') continue;
        const meta = readPluginYmlFromFolder(fullPath);
        const key = meta.name || entry.name;
        if (seen.has(key)) continue;
        seen.add(key);

        items.push({
          id: entry.name,
          name: meta.name || entry.name,
          file: entry.name + '/',
          type: 'folder',
          version: meta.version || null,
          apiVersion: meta.apiVersion || null,
          description: meta.description || null,
          authors: meta.authors || [],
          main: meta.main || null,
          enabled: true,
          size: null,
          sizeLabel: '—',
          modifiedAt: fs.statSync(fullPath).mtime.toISOString(),
        });
      } else if (entry.name.endsWith('.jar')) {
        const disabled = entry.name.endsWith('.jar.disabled');
        const baseName = entry.name.replace(/\.jar(\.disabled)?$/i, '');
        const stats = fs.statSync(fullPath);
        const meta = readPluginYmlFromJar(fullPath);
        const key = meta.name || baseName;
        if (seen.has(key)) continue;
        seen.add(key);

        items.push({
          id: entry.name,
          name: meta.name || baseName,
          file: entry.name,
          type: 'jar',
          version: meta.version || null,
          apiVersion: meta.apiVersion || null,
          description: meta.description || null,
          authors: meta.authors || [],
          main: meta.main || null,
          enabled: !disabled,
          size: stats.size,
          sizeLabel: formatBytes(stats.size),
          modifiedAt: stats.mtime.toISOString(),
        });
      }
    }

    const disabledDir = path.join(pluginsDir, 'disabled');
    if (fs.existsSync(disabledDir)) {
      for (const file of fs.readdirSync(disabledDir)) {
        if (!file.endsWith('.jar')) continue;
        const fullPath = path.join(disabledDir, file);
        const stats = fs.statSync(fullPath);
        const meta = readPluginYmlFromJar(fullPath);
        const baseName = file.replace(/\.jar$/i, '');

        items.push({
          id: `disabled/${file}`,
          name: meta.name || baseName,
          file: `disabled/${file}`,
          type: 'jar',
          version: meta.version || null,
          apiVersion: meta.apiVersion || null,
          description: meta.description || null,
          authors: meta.authors || [],
          main: meta.main || null,
          enabled: false,
          size: stats.size,
          sizeLabel: formatBytes(stats.size),
          modifiedAt: stats.mtime.toISOString(),
        });
      }
    }

    return items.sort((a, b) => a.name.localeCompare(b.name));
  } catch {
    return null;
  }
}

function readPackMcmeta(filePath) {
  try {
    const raw = JSON.parse(fs.readFileSync(filePath, 'utf8'));
    const pack = raw.pack || {};
    let description = pack.description;
    if (typeof description === 'object' && description !== null) {
      description = description.text || JSON.stringify(description);
    }
    return {
      packFormat: pack.pack_format ?? pack.min_format ?? null,
      supportedFormats: pack.supported_formats || null,
      description: description || null,
    };
  } catch {
    return { packFormat: null, supportedFormats: null, description: null };
  }
}

function readPackMcmetaFromZip(zipPath) {
  try {
    const output = execFileSync('unzip', ['-p', zipPath, 'pack.mcmeta'], {
      encoding: 'utf8',
      stdio: ['pipe', 'pipe', 'ignore'],
      timeout: 3000,
    });
    const raw = JSON.parse(output);
    const pack = raw.pack || {};
    let description = pack.description;
    if (typeof description === 'object' && description !== null) {
      description = description.text || JSON.stringify(description);
    }
    return {
      packFormat: pack.pack_format ?? pack.min_format ?? null,
      supportedFormats: pack.supported_formats || null,
      description: description || null,
    };
  } catch {
    return { packFormat: null, supportedFormats: null, description: null };
  }
}

function countDatapackNamespaces(targetPath, isZip) {
  if (isZip) {
    try {
      const listing = execFileSync('unzip', ['-l', targetPath], {
        encoding: 'utf8',
        stdio: ['pipe', 'pipe', 'ignore'],
        timeout: 5000,
      });
      const namespaces = new Set();
      for (const line of listing.split('\n')) {
        const match = line.match(/\s+data\/([^/]+)\//);
        if (match) namespaces.add(match[1]);
      }
      return [...namespaces];
    } catch {
      return [];
    }
  }

  const dataDir = path.join(targetPath, 'data');
  if (!fs.existsSync(dataDir)) return [];
  try {
    return fs.readdirSync(dataDir, { withFileTypes: true })
      .filter((e) => e.isDirectory())
      .map((e) => e.name);
  } catch {
    return [];
  }
}

function listDatapacks(serverDir) {
  const worldName = getWorldName(serverDir);
  const datapacksDir = path.join(serverDir, worldName, 'datapacks');
  if (!fs.existsSync(datapacksDir)) return { world: worldName, datapacks: null };

  const items = [];

  try {
    const entries = fs.readdirSync(datapacksDir, { withFileTypes: true });

    for (const entry of entries) {
      const fullPath = path.join(datapacksDir, entry.name);
      if (entry.name.startsWith('.')) continue;

      if (entry.isDirectory()) {
        const metaPath = path.join(fullPath, 'pack.mcmeta');
        const meta = fs.existsSync(metaPath)
          ? readPackMcmeta(metaPath)
          : { packFormat: null, description: null };
        const namespaces = countDatapackNamespaces(fullPath, false);
        const stats = fs.statSync(fullPath);

        items.push({
          id: entry.name,
          name: entry.name,
          file: entry.name + '/',
          type: 'folder',
          world: worldName,
          packFormat: meta.packFormat,
          supportedFormats: meta.supportedFormats,
          description: meta.description,
          namespaces,
          namespaceCount: namespaces.length,
          size: null,
          sizeLabel: '—',
          modifiedAt: stats.mtime.toISOString(),
        });
      } else if (entry.name.endsWith('.zip')) {
        const meta = readPackMcmetaFromZip(fullPath);
        const namespaces = countDatapackNamespaces(fullPath, true);
        const stats = fs.statSync(fullPath);

        items.push({
          id: entry.name,
          name: entry.name.replace(/\.zip$/i, ''),
          file: entry.name,
          type: 'zip',
          world: worldName,
          packFormat: meta.packFormat,
          supportedFormats: meta.supportedFormats,
          description: meta.description,
          namespaces,
          namespaceCount: namespaces.length,
          size: stats.size,
          sizeLabel: formatBytes(stats.size),
          modifiedAt: stats.mtime.toISOString(),
        });
      }
    }

    return {
      world: worldName,
      path: datapacksDir,
      datapacks: items.sort((a, b) => a.name.localeCompare(b.name)),
    };
  } catch {
    return { world: worldName, datapacks: null };
  }
}

function parseRconPluginList(output) {
  if (!output) return [];
  const names = [];
  for (const line of output.split('\n')) {
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith('§') || /plugins?\s*:/i.test(trimmed)) continue;
    const cleaned = trimmed
      .replace(/^[-•*]\s*/, '')
      .replace(/§./g, '')
      .replace(/\s+v[\d.]+.*$/i, '')
      .trim();
    if (cleaned && !/no plugins/i.test(cleaned)) {
      names.push(cleaned);
    }
  }
  return names;
}

function parseRconDatapackList(output) {
  if (!output) return [];
  const names = [];
  let section = null;

  for (const line of output.split('\n')) {
    const trimmed = line.replace(/§./g, '').trim();
    if (!trimmed) continue;

    if (/available/i.test(trimmed)) { section = 'available'; continue; }
    if (/enabled/i.test(trimmed)) { section = 'enabled'; continue; }

    const match = trimmed.match(/^\[([^\]]+)\]/);
    if (match) {
      names.push({ name: match[1], enabled: section === 'enabled' });
      continue;
    }

    if (section === 'enabled' && !trimmed.includes(':')) {
      names.push({ name: trimmed.replace(/^[-•*]\s*/, ''), enabled: true });
    }
  }

  return names;
}

function mergePluginLoadState(plugins, loadedNames) {
  if (!plugins || !loadedNames.length) return plugins;

  const normalizedLoaded = loadedNames.map((n) => n.toLowerCase());
  return plugins.map((plugin) => {
    const nameMatch = normalizedLoaded.some((n) =>
      n === plugin.name.toLowerCase()
      || n.includes(plugin.name.toLowerCase())
      || plugin.name.toLowerCase().includes(n),
    );
    return { ...plugin, loaded: nameMatch };
  });
}

module.exports = {
  readWhitelist,
  readOps,
  readBannedPlayers,
  listPlugins,
  listDatapacks,
  readServerProperties,
  getWorldName,
  parseRconPluginList,
  parseRconDatapackList,
  mergePluginLoadState,
};
