package games.twinhead.monitorbridge.listeners;

import games.twinhead.monitorbridge.MonitorBridgePlugin;
import games.twinhead.monitorbridge.data.EventLogBuffer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.ServerLoadEvent;

import java.util.LinkedHashMap;
import java.util.Map;

public class MonitorListeners implements Listener {

    private final MonitorBridgePlugin plugin;
    private final EventLogBuffer buffer;
    private final boolean logChat;
    private final boolean logCommands;
    private final boolean logDeaths;

    public MonitorListeners(MonitorBridgePlugin plugin, EventLogBuffer buffer, FileConfiguration config) {
        this.plugin = plugin;
        this.buffer = buffer;
        this.logChat = config.getBoolean("events.log-chat", true);
        this.logCommands = config.getBoolean("events.log-commands", true);
        this.logDeaths = config.getBoolean("events.log-deaths", true);
    }

    @EventHandler
    public void onServerLoad(ServerLoadEvent event) {
        if (event.getType() == ServerLoadEvent.LoadType.STARTUP) {
            buffer.log("system", "info", "Server startup complete", null);
        } else {
            buffer.log("system", "info", "Server reload complete", null);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Map<String, Object> data = playerData(player);
        data.put("ip", player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "unknown");
        buffer.log("join", "info", player.getName() + " joined the game", data);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Map<String, Object> data = playerData(player);
        data.put("reason", event.getQuitReason() != null ? event.getQuitReason().toString() : "Disconnected");
        buffer.log("quit", "info", player.getName() + " left the game", data);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        if (!logChat) return;
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("player", event.getPlayer().getName());
        data.put("message", event.getMessage());
        buffer.log("chat", "info", "<" + event.getPlayer().getName() + "> " + event.getMessage(), data);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!logCommands) return;
        Map<String, Object> data = playerData(event.getPlayer());
        data.put("command", event.getMessage());
        buffer.log("command", "info", event.getPlayer().getName() + " issued command: " + event.getMessage(), data);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        if (!logDeaths) return;
        Player player = event.getEntity();
        Map<String, Object> data = playerData(player);
        data.put("cause", event.getEntity().getLastDamageCause() != null
                ? event.getEntity().getLastDamageCause().getCause().name() : "UNKNOWN");
        String msg = player.getName() + " died";
        if (event.deathMessage() != null) {
            msg = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                    .serialize(event.deathMessage());
        }
        buffer.log("death", "warn", msg, data);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onKick(PlayerKickEvent event) {
        Map<String, Object> data = playerData(event.getPlayer());
        data.put("reason", event.getReason() != null
                ? net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                    .serialize(event.getReason()) : "Kicked");
        buffer.log("kick", "warn", event.getPlayer().getName() + " was kicked", data);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onAdvancement(PlayerAdvancementDoneEvent event) {
        Map<String, Object> data = playerData(event.getPlayer());
        data.put("advancement", event.getAdvancement().getKey().toString());
        buffer.log("advancement", "info",
                event.getPlayer().getName() + " completed advancement " + event.getAdvancement().getKey(),
                data);
    }

    private Map<String, Object> playerData(Player player) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("player", player.getName());
        data.put("uuid", player.getUniqueId().toString());
        data.put("world", player.getWorld().getName());
        data.put("gamemode", player.getGameMode().name().toLowerCase());
        return data;
    }
}
