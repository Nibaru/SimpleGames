package games.twinhead.simplegames.settings;

import games.twinhead.simplegames.SimpleGames;

import java.util.Arrays;
import java.util.List;

public enum Setting {
    TIC_TAC_TOE_TOKEN(Arrays.asList(
            "NETHER_STAR",
            "FIREWORK_STAR",
            "WHITE_CONCRETE",
            "STONE"
    )),
    CONNECT_FOUR_TOKEN(Arrays.asList(
            "CYAN_CONCRETE",
            "LIME_CONCRETE_POWDER"
    ));

    final List<String> options;

    Setting(List<String> options){
        this.options = options;
    }

    public String getDefaultSetting(){
        return SimpleGames.getInstance().getConfigManager().getOrDefaultString("settings.defaults." + this.toString());
    }

    public List<String> getOptions(){
        return this.options;
    }



}
