package games.twinhead.monitorbridge;

import games.twinhead.monitorbridge.api.AuthService;
import games.twinhead.monitorbridge.api.DashboardServer;
import games.twinhead.monitorbridge.data.EventLogBuffer;
import games.twinhead.monitorbridge.data.LiveStatsService;
import games.twinhead.monitorbridge.data.ServerFilesService;
import games.twinhead.monitorbridge.listeners.MonitorListeners;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;

public final class MonitorBridgePlugin extends JavaPlugin {

    private static MonitorBridgePlugin instance;
    private EventLogBuffer eventBuffer;
    private LiveStatsService statsService;
    private ServerFilesService filesService;
    private DashboardServer dashboard;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        int bufferSize = getConfig().getInt("events.buffer-size", 2000);
        eventBuffer = new EventLogBuffer(bufferSize);
        statsService = new LiveStatsService(this, eventBuffer);
        filesService = new ServerFilesService(this);

        getServer().getPluginManager().registerEvents(
                new MonitorListeners(this, eventBuffer, getConfig()),
                this
        );

        if (getConfig().getBoolean("http.enabled", true)) {
            AuthService auth = new AuthService(
                    getConfig().getString("http.api-key", ""),
                    getConfig().getString("dashboard.password", "")
            );
            try {
                dashboard = new DashboardServer(this, eventBuffer, statsService, filesService, auth);
                dashboard.start();
                int port = getConfig().getInt("http.port", 8765);
                getLogger().info("ServerMonitor dashboard: http://"
                        + getConfig().getString("http.host") + ":" + port);
            } catch (IOException e) {
                getLogger().severe("Failed to start dashboard: " + e.getMessage());
            }
        }

        statsService.logSystem("info", "ServerMonitor enabled");
    }

    @Override
    public void onDisable() {
        if (dashboard != null) dashboard.stop();
        if (eventBuffer != null) eventBuffer.log("system", "info", "ServerMonitor disabled", null);
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
