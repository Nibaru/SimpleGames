package games.twinhead.monitorbridge;

import games.twinhead.monitorbridge.api.HttpApiServer;
import games.twinhead.monitorbridge.data.EventLogBuffer;
import games.twinhead.monitorbridge.data.LiveStatsService;
import games.twinhead.monitorbridge.listeners.MonitorListeners;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;

public final class MonitorBridgePlugin extends JavaPlugin {

    private static MonitorBridgePlugin instance;
    private EventLogBuffer eventBuffer;
    private LiveStatsService statsService;
    private HttpApiServer httpServer;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        int bufferSize = getConfig().getInt("events.buffer-size", 2000);
        eventBuffer = new EventLogBuffer(bufferSize);
        statsService = new LiveStatsService(this, eventBuffer);

        getServer().getPluginManager().registerEvents(
                new MonitorListeners(this, eventBuffer, getConfig()),
                this
        );

        if (getConfig().getBoolean("http.enabled", true)) {
            try {
                httpServer = new HttpApiServer(this, eventBuffer, statsService);
                httpServer.start();
                getLogger().info("Monitor bridge API listening on "
                        + getConfig().getString("http.host") + ":"
                        + getConfig().getInt("http.port"));
            } catch (IOException e) {
                getLogger().severe("Failed to start HTTP API: " + e.getMessage());
            }
        }

        statsService.logSystem("info", "MonitorBridge enabled");
        getLogger().info("MonitorBridge ready — connect your web monitor to port "
                + getConfig().getInt("http.port"));
    }

    @Override
    public void onDisable() {
        if (httpServer != null) {
            httpServer.stop();
        }
        if (eventBuffer != null) {
            eventBuffer.log("system", "info", "MonitorBridge disabled", null);
        }
        instance = null;
    }

    public static MonitorBridgePlugin getInstance() {
        return instance;
    }

    public EventLogBuffer getEventBuffer() {
        return eventBuffer;
    }

    public LiveStatsService getStatsService() {
        return statsService;
    }
}
