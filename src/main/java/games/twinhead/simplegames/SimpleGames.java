package games.twinhead.simplegames;

import games.twinhead.simplegames.command.*;
import games.twinhead.simplegames.database.Implementation;
import games.twinhead.simplegames.database.SqliteImplementation;
import games.twinhead.simplegames.events.OnGameEndEvent;
import games.twinhead.simplegames.events.PlayerEvents;
import games.twinhead.simplegames.misc.ConfigManager;
import games.twinhead.simplegames.game.GameManager;
import games.twinhead.simplegames.settings.SettingsManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.ipvp.canvas.MenuFunctionListener;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;


public final class SimpleGames extends JavaPlugin {

    private static SimpleGames instance;
    private GameManager gameManager;
    private ConfigManager config;
    private Implementation database;
    private SettingsManager settingsManager;

    public static final String prefix = "[SG] ";

    @Override
    public void onEnable() {
        // Plugin startup logic
        instance = this;
        config = new ConfigManager(this);
        gameManager = new GameManager();

        if(getConfigManager().getOrDefaultBool("settings.enabled")){
            settingsManager = new SettingsManager(this);
            database = initDatabase();
            database.getOnlinePlayerSettings();
        }


        Bukkit.getPluginManager().registerEvents(new MenuFunctionListener(), this);
        Bukkit.getPluginManager().registerEvents(new OnGameEndEvent(), this);
        Bukkit.getPluginManager().registerEvents(new PlayerEvents(this), this);

        this.getCommand("play").setExecutor(new PlayCommand());
        this.getCommand("accept").setExecutor(new AcceptCommand());
        this.getCommand("decline").setExecutor(new DeclineCommand());
        this.getCommand("listgames").setExecutor(new ListCommand());
        this.getCommand("reload").setExecutor(new ReloadCommand());
        this.getCommand("settings").setExecutor(new SettingsCommand());
        this.getCommand("bigscreen").setExecutor(new BigScreenCommand());
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getGameManager().clearActiveGames();
        getDatabase().saveAllPlayerSettingsSync();

    }

    private Implementation initDatabase(){
        //switch (config.getOrDefaultString("database.type")){
        //    case "sqlite" -> {
                try {
                    Files.createDirectories(Paths.get("plugins/SimpleGames/data/"));
                    return new SqliteImplementation(this);
                } catch (IOException e) {
                    e.printStackTrace();
                }
         //   }
         //   default -> {
         //       return null;
          //  }
        //}
        return null;
    }

    public static SimpleGames getInstance(){
        return instance;
    }

    public GameManager getGameManager() {
        return gameManager;
    }

    public ConfigManager getConfigManager() {
        return config;
    }


    public SettingsManager getSettingsManager() {
        return settingsManager;
    }

    public Implementation getDatabase() {
        return database;
    }
}
