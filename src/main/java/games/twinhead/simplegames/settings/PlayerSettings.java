package games.twinhead.simplegames.settings;

import java.util.HashMap;
import java.util.Set;

public class PlayerSettings {

    private final HashMap<Setting, String> settings = new HashMap<>();

    public PlayerSettings(){}

    public HashMap<Setting, String> getSettings(){
        return settings;
    }

    public Set<Setting> getStoredSettings(){
        return settings.keySet();
    }

    private String getSetting(Setting key){
        if(!settings.containsKey(key)){
            return key.getDefaultSetting();
        } else {
            return settings.get(key);
        }
    }

    public Boolean getBoolean(Setting key){
        return Boolean.valueOf(getSetting(key));
    }

    public String getString(Setting key){
        return getSetting(key);
    }

    public Integer getInt(Setting key){
        return Integer.valueOf(getSetting(key));
    }

    public void invertBoolSetting(Setting key){
        setSetting(key, String.valueOf(!getBoolean(key)));
    }

    public void setSetting(Setting key, String value){
        if(value == null) {
            settings.put(key, key.getDefaultSetting());
        }else {
            settings.put(key, value);
        }
    }
}
