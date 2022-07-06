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
import org.ipvp.canvas.slot.SlotSettings;

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
        getMenu().getSlot(count++).setSettings(TokenSettingItem(Setting.TIC_TAC_TOE_TOKEN));
        getMenu().getSlot(count++).setSettings(TokenSettingItem(Setting.CONNECT_FOUR_TOKEN));
        getMenu().getSlot(count++).setSettings(BooleanSetting(Setting.SHOW_ITEM_BORDER));
        getMenu().getSlot(count++).setSettings(BooleanSetting(Setting.HIDE_IN_PLAYER_LIST));
    }

    public SlotSettings TokenSettingItem(Setting s){
        return SlotSettings.builder().itemTemplate(player -> {
            ItemStack item = new ItemStack(Material.valueOf(settings.getString(s)));
            ItemMeta meta = item.getItemMeta();
            List<String> lore = new ArrayList<>();

            assert meta != null;
            meta.setDisplayName(ChatColor.YELLOW + "Pick a new " + Util.formatString(s.toString()));
            lore.add(ChatColor.GRAY + " Current: " + Util.formatString(settings.getString(s)));


            meta.setLore(lore);
            item.setItemMeta(meta);
            return ScreenItems.addBorderToItem(item).getItem(player);
        }).clickHandler((player1, clickInformation) -> new PickTokenScreen(player1, s, this)).build();

    }

    private SlotSettings BooleanSetting(Setting setting){
        return SlotSettings.builder().itemTemplate(player -> {
            Boolean showBoarder = SimpleGames.getInstance().getSettingsManager().getSettings(player.getUniqueId()).getBoolean(setting);

            ItemStack item = (showBoarder ? new ItemStack(Material.OXIDIZED_COPPER) : new ItemStack(Material.COPPER_BLOCK));

            ItemMeta meta = item.getItemMeta();
            List<String> lore = new ArrayList<>();

            assert meta != null;
            meta.setDisplayName(ChatColor.YELLOW + Util.formatString(setting.toString()));
            lore.add(ChatColor.GRAY + " Current: " + (showBoarder ? ChatColor.GREEN:ChatColor.RED) +  "[ " + showBoarder + " ]");


            meta.setLore(lore);
            item.setItemMeta(meta);
            return ScreenItems.addBorderToItem(item).getItem(player);

        }).clickHandler((player1, clickInformation) -> {
            SimpleGames.getInstance().getSettingsManager().getSettings(player1.getUniqueId()).invertBoolSetting(setting);
            drawSettings();
        }).build();
    }
}
