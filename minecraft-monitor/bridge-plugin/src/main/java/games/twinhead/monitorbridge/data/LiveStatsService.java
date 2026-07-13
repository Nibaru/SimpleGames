package games.twinhead.monitorbridge.data;

import games.twinhead.monitorbridge.MonitorBridgePlugin;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class LiveStatsService {

    private final MonitorBridgePlugin plugin;
    private final EventLogBuffer buffer;
    private final long startedAt = System.currentTimeMillis();

    public LiveStatsService(MonitorBridgePlugin plugin, EventLogBuffer buffer) {
        this.plugin = plugin;
        this.buffer = buffer;
    }

    public void logSystem(String level, String message) {
        buffer.log("system", level, message, null);
    }

    public Map<String, Object> getStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("online", true);
        status.put("bridge", true);
        status.put("serverVersion", Bukkit.getVersion());
        status.put("bukkitVersion", Bukkit.getBukkitVersion());
        status.put("uptimeMs", System.currentTimeMillis() - startedAt);
        status.put("maxPlayers", Bukkit.getMaxPlayers());
        status.put("motd", Bukkit.getMotd());

        Map<String, Object> tpsData = getTpsData();
        status.put("tps", tpsData.get("tps"));
        status.put("mspt", tpsData.get("mspt"));
        status.put("tps1m", tpsData.get("tps1m"));
        status.put("tps5m", tpsData.get("tps5m"));
        status.put("tps15m", tpsData.get("tps15m"));

        Runtime runtime = Runtime.getRuntime();
        long used = runtime.totalMemory() - runtime.freeMemory();
        Map<String, Object> memory = new LinkedHashMap<>();
        memory.put("usedMb", used / (1024 * 1024));
        memory.put("maxMb", runtime.maxMemory() / (1024 * 1024));
        memory.put("heapUsedMb", ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed() / (1024 * 1024));
        status.put("memory", memory);

        status.put("players", getPlayers());
        status.put("worlds", getWorlds());
        status.put("plugins", getPlugins());

        return status;
    }

    public Map<String, Object> getPlayers() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("name", player.getName());
            p.put("uuid", player.getUniqueId().toString());
            p.put("ping", player.getPing());
            p.put("gamemode", player.getGameMode().name().toLowerCase());
            p.put("world", player.getWorld().getName());
            p.put("health", Math.round(player.getHealth() * 10) / 10.0);
            p.put("level", player.getLevel());
            p.put("op", player.isOp());
            list.add(p);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("count", list.size());
        result.put("max", Bukkit.getMaxPlayers());
        result.put("names", list.stream().map(p -> (String) p.get("name")).toList());
        result.put("details", list);
        return result;
    }

    public List<Map<String, Object>> getPlugins() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Plugin p : Bukkit.getPluginManager().getPlugins()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name", p.getName());
            entry.put("version", p.getPluginMeta().getVersion());
            entry.put("enabled", p.isEnabled());
            entry.put("authors", p.getPluginMeta().getAuthors());
            entry.put("description", p.getPluginMeta().getDescription());
            list.add(entry);
        }
        list.sort((a, b) -> ((String) a.get("name")).compareToIgnoreCase((String) b.get("name")));
        return list;
    }

    private List<Map<String, Object>> getWorlds() {
        List<Map<String, Object>> worlds = new ArrayList<>();
        for (World world : Bukkit.getWorlds()) {
            Map<String, Object> w = new LinkedHashMap<>();
            w.put("name", world.getName());
            w.put("players", world.getPlayers().size());
            w.put("entities", world.getEntities().size());
            w.put("time", world.getTime());
            w.put("difficulty", world.getDifficulty().name().toLowerCase());
            worlds.add(w);
        }
        return worlds;
    }

    private Map<String, Object> getTpsData() {
        Map<String, Object> data = new LinkedHashMap<>();
        try {
            double[] tps = Bukkit.getServer().getTPS();
            data.put("tps", round(tps[0]));
            data.put("tps1m", round(tps[0]));
            data.put("tps5m", round(tps[1]));
            data.put("tps15m", round(tps[2]));
        } catch (NoSuchMethodError | Exception e) {
            data.put("tps", null);
            data.put("tps1m", null);
            data.put("tps5m", null);
            data.put("tps15m", null);
        }

        try {
            double mspt = Bukkit.getServer().getAverageTickTime();
            data.put("mspt", round(mspt));
        } catch (NoSuchMethodError | Exception e) {
            data.put("mspt", null);
        }

        return data;
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
