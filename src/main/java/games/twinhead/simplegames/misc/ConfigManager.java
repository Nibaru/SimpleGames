package games.twinhead.simplegames.misc;

import games.twinhead.simplegames.SimpleGames;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class ConfigManager {

    private final SimpleGames simpleGames;
    private FileConfiguration config, defaultConfig;

    public ConfigManager(SimpleGames simpleGames){
        this.simpleGames = simpleGames;
        reloadConfig();
        loadDefaultConfig();
    }

    public FileConfiguration getConfig(){
        return config;
    }

    public Boolean getOrDefaultBool(String path){
        return config.getBoolean(path);
    }

    public String getOrDefaultString(String path){
        return config.getString(path);
    }

    public int getOrDefaultInt(String path){
        return config.getInt(path);
    }

    public void reloadConfig(){
        createConfig();
    }


    private void loadDefaultConfig(){
        Reader defaultStream = null;
        defaultStream = new InputStreamReader(simpleGames.getResource("config.yml"), StandardCharsets.UTF_8);

        if (defaultStream != null) {
            this.defaultConfig = YamlConfiguration.loadConfiguration(defaultStream);
        }
    }

    public void createConfig(){
        File configFile = new File(simpleGames.getDataFolder(), "config.yml");
        if(!configFile.exists()){
            configFile.getParentFile().mkdirs();
            simpleGames.saveResource("config.yml", false);
        }

        config = new YamlConfiguration();
        config = YamlConfiguration.loadConfiguration(configFile);
    }



}
