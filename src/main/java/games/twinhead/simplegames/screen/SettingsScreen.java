package games.twinhead.simplegames.screen;

import games.twinhead.simplegames.SimpleGames;
import games.twinhead.simplegames.misc.Util;
import games.twinhead.simplegames.settings.PlayerSettings;
import games.twinhead.simplegames.settings.Setting;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class SettingsScreen extends Screen{

    private final PlayerSettings settings;

    public SettingsScreen(Player player){
        super("Settings");
        settings = SimpleGames.getInstance().getSettingsManager().getSettings(player.getUniqueId());

        drawSettings();
        display(player);
    }

    private void drawSettings(){
        int count = 9;
        for (Setting s: Setting.values()) {
            getMenu().getSlot(count).setItem(TokenSettingItem(s, settings.getString(s)));
            getMenu().getSlot(count++).setClickHandler((player, info) -> {
                PickTokenScreen screen = new PickTokenScreen(player, s, this);
                screen.display(player);
            });
        }
    }

    public ItemStack TokenSettingItem(Setting s, String value){
        ItemStack item = new ItemStack(Material.valueOf(value));
        ItemMeta meta = item.getItemMeta();
        List<String> lore = new ArrayList<>();

        assert meta != null;
        meta.setDisplayName(ChatColor.YELLOW + "Pick a new " + Util.formatString(s.toString()));
        lore.add(ChatColor.GRAY + " Current: " + Util.formatString(value));


        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
}
