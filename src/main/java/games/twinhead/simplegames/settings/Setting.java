package games.twinhead.simplegames.settings;

import games.twinhead.simplegames.SimpleGames;

public enum Setting {
    TIC_TAC_TOE_TOKEN,
    CONNECT_FOUR_TOKEN,
    SHOW_ITEM_BORDER;

    public String getDefaultSetting(){
        return SimpleGames.getInstance().getConfigManager().getOrDefaultString("settings.defaults." + this.toString());
    }



}
