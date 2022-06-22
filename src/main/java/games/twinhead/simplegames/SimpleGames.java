package games.twinhead.simplegames;

import games.twinhead.simplegames.command.AcceptCommand;
import games.twinhead.simplegames.command.ListCommand;
import games.twinhead.simplegames.command.PlayCommand;
import games.twinhead.simplegames.misc.GameManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.ipvp.canvas.MenuFunctionListener;

public final class SimpleGames extends JavaPlugin {

    private static SimpleGames instance;

    private GameManager gameManager;

    @Override
    public void onEnable() {
        // Plugin startup logic
        instance = this;
        gameManager = new GameManager();

        Bukkit.getPluginManager().registerEvents(new MenuFunctionListener(), this);

        this.getCommand("play").setExecutor(new PlayCommand());
        this.getCommand("accept").setExecutor(new AcceptCommand());
        this.getCommand("listgames").setExecutor(new ListCommand());


    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }


    public static SimpleGames getInstance(){
        return instance;
    }

    public GameManager getGameManager() {
        return gameManager;
    }
}
