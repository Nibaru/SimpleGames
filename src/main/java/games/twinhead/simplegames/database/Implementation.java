package games.twinhead.simplegames.database;

import games.twinhead.simplegames.settings.PlayerSettings;

import java.util.UUID;

public interface Implementation {

    PlayerSettings getPlayerSettings(UUID uuid);
    
    
    void savePlayerSettings(UUID uuid, PlayerSettings playerSettings);

    void savePlayerSettingsAsync(UUID uuid, PlayerSettings playerSettings);

    void getPlayerSettingsAsync(UUID uuid, PlayerSettingsCallBack callBack);

    void getOnlinePlayerSettings();

    void saveAllPlayerSettingsSync();
}
