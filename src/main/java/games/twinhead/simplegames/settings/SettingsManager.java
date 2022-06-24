package games.twinhead.simplegames.settings;

import games.twinhead.simplegames.SimpleGames;

import java.util.HashMap;
import java.util.UUID;

public class SettingsManager {

    private final SimpleGames plugin;

    private final HashMap<UUID, PlayerSettings> playerSettings = new HashMap<>();

    public SettingsManager(SimpleGames plugin){
        this.plugin = plugin;
    }

    public boolean contains(UUID uuid){
       return playerSettings.containsKey(uuid);
    }

    public void addSettings(UUID uuid, PlayerSettings settings){
        playerSettings.put(uuid, settings);
    }

    public PlayerSettings getSettings(UUID uuid){
        return playerSettings.get(uuid);
    }

    public void removeSettings(UUID uuid){
        plugin.getDatabase().savePlayerSettingsAsync(uuid, getSettings(uuid));
        playerSettings.remove(uuid);
    }

    public void loadSettings(UUID uuid){
        if(!contains(uuid))
            plugin.getDatabase().getPlayerSettingsAsync(uuid, (settings)-> addSettings(uuid, settings));

    }
}
