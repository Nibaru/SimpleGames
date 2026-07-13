package games.twinhead.monitorbridge.data;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import games.twinhead.monitorbridge.MonitorBridgePlugin;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class ServerFilesService {

    private static final Gson GSON = new Gson();
    private final File serverRoot;

    public ServerFilesService(MonitorBridgePlugin plugin) {
        this.serverRoot = plugin.getDataFolder().getParentFile();
    }

    public File getServerRoot() {
        return serverRoot;
    }

    public List<String> readWhitelist() {
        return readJsonNames(new File(serverRoot, "whitelist.json"));
    }

    public List<String> readOps() {
        return readJsonNames(new File(serverRoot, "ops.json"));
    }

    public List<Map<String, Object>> readBanned() {
        File file = new File(serverRoot, "banned-players.json");
        if (!file.exists()) return null;
        try {
            JsonArray arr = JsonParser.parseString(Files.readString(file.toPath())).getAsJsonArray();
            List<Map<String, Object>> list = new ArrayList<>();
            for (JsonElement el : arr) {
                JsonObject obj = el.getAsJsonObject();
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("name", obj.get("name").getAsString());
                entry.put("reason", obj.has("reason") ? obj.get("reason").getAsString() : "");
                list.add(entry);
            }
            return list;
        } catch (Exception e) {
            return null;
        }
    }

    public List<Map<String, Object>> listFilePlugins() {
        File pluginsDir = new File(serverRoot, "plugins");
        if (!pluginsDir.exists()) return Collections.emptyList();
        List<Map<String, Object>> items = new ArrayList<>();
        File[] files = pluginsDir.listFiles();
        if (files == null) return items;

        for (File f : files) {
            if (f.getName().startsWith(".") || f.getName().equals("MonitorBridge")
                    || f.getName().equals("ServerMonitor")) continue;
            if (f.isDirectory() && !f.getName().equals("disabled")) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("name", f.getName());
                item.put("file", f.getName() + "/");
                item.put("type", "folder");
                items.add(item);
            } else if (f.getName().endsWith(".jar")) {
                Map<String, Object> item = new LinkedHashMap<>();
                String name = f.getName().replaceAll("\\.jar(\\.disabled)?$", "");
                item.put("name", name);
                item.put("file", f.getName());
                item.put("type", "jar");
                item.put("sizeLabel", formatBytes(f.length()));
                item.put("enabled", !f.getName().endsWith(".disabled"));
                items.add(item);
            }
        }
        return items;
    }

    public Map<String, Object> listDatapacks() {
        String worldName = readServerProperty("level-name", "world");
        File datapacksDir = new File(serverRoot, worldName + "/datapacks");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("world", worldName);
        result.put("path", datapacksDir.getAbsolutePath());

        if (!datapacksDir.exists()) {
            result.put("datapacks", null);
            return result;
        }

        List<Map<String, Object>> packs = new ArrayList<>();
        File[] files = datapacksDir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.getName().startsWith(".")) continue;
                Map<String, Object> dp = new LinkedHashMap<>();
                dp.put("name", f.getName().replaceAll("\\.zip$", ""));
                dp.put("file", f.isDirectory() ? f.getName() + "/" : f.getName());
                dp.put("type", f.isDirectory() ? "folder" : "zip");
                File meta = new File(f, "pack.mcmeta");
                if (meta.exists()) {
                    try {
                        JsonObject root = JsonParser.parseString(Files.readString(meta.toPath())).getAsJsonObject();
                        if (root.has("pack")) {
                            JsonObject pack = root.getAsJsonObject("pack");
                            if (pack.has("pack_format")) dp.put("packFormat", pack.get("pack_format").getAsInt());
                            if (pack.has("description")) {
                                dp.put("description", pack.get("description").isJsonPrimitive()
                                        ? pack.get("description").getAsString()
                                        : pack.get("description").toString());
                            }
                        }
                    } catch (Exception ignored) {
                    }
                }
                packs.add(dp);
            }
        }
        result.put("datapacks", packs);
        result.put("count", packs.size());
        return result;
    }

    public List<String> readLogTail(int lines) {
        File log = new File(serverRoot, "logs/latest.log");
        if (!log.exists()) return Collections.emptyList();
        try {
            List<String> all = Files.readAllLines(log.toPath(), StandardCharsets.UTF_8);
            int start = Math.max(0, all.size() - lines);
            return all.subList(start, all.size());
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    public File getLogFile() {
        return new File(serverRoot, "logs/latest.log");
    }

    public Map<String, String> readServerProperties() {
        File file = new File(serverRoot, "server.properties");
        Map<String, String> props = new LinkedHashMap<>();
        if (!file.exists()) return props;
        try {
            for (String line : Files.readAllLines(file.toPath())) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
                int idx = trimmed.indexOf('=');
                if (idx > 0) props.put(trimmed.substring(0, idx).trim(), trimmed.substring(idx + 1).trim());
            }
        } catch (IOException ignored) {
        }
        return props;
    }

    private String readServerProperty(String key, String def) {
        return readServerProperties().getOrDefault(key, def);
    }

    private List<String> readJsonNames(File file) {
        if (!file.exists()) return null;
        try {
            JsonArray arr = JsonParser.parseString(Files.readString(file.toPath())).getAsJsonArray();
            return StreamSupport.stream(arr.spliterator(), false)
                    .map(el -> el.getAsJsonObject().get("name").getAsString())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return null;
        }
    }

    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024));
    }
}
