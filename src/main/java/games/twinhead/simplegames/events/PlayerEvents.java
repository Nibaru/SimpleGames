package games.twinhead.simplegames.events;

import games.twinhead.simplegames.SimpleGames;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerEvents implements Listener {

    private final SimpleGames plugin;

    public PlayerEvents(SimpleGames plugin){
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event){
        plugin.getSettingsManager().loadSettings(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event){
        plugin.getSettingsManager().removeSettings(event.getPlayer().getUniqueId());
    }
}
