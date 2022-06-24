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
import org.ipvp.canvas.Menu;
import org.ipvp.canvas.type.ChestMenu;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class SettingsScreen implements Screen{

    private final Player player;
    private final Menu menu;

    private final PlayerSettings settings;

    public SettingsScreen(Player player){
        this.player = player;

        settings = SimpleGames.getInstance().getSettingsManager().getSettings(player.getUniqueId());
        menu = ChestMenu.builder(6)
                .title("Tic Tac Toe")
                .build();
    }


    @Override
    public void display() {
        int count = 9;
        for (Setting s: Setting.values()) {
            getMenu().getSlot(count).setItem(TokenSettingItem(s, settings.getString(s)));
            clickHandler(count++, s);
        }

        getMenu().open(player);
    }

    @Override
    public void display(Player player) {

    }

    public void clickHandler(int slot, Setting setting){
        menu.getSlot(slot).setClickHandler((player, info) -> {
            PickTokenScreen screen = new PickTokenScreen(player, setting, this);
            screen.display();
        });
    }


    public ItemStack TokenSettingItem(Setting s, String value){
        ItemStack item = new ItemStack(Material.valueOf(value));
        ItemMeta meta = item.getItemMeta();
        List<String> lore = new ArrayList<>();

        meta.setDisplayName(ChatColor.YELLOW + "Pick a new " + Util.formatString(s.toString()));
        lore.add(ChatColor.GRAY + " Current: " + Util.formatString(value));


        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack SettingItem(Setting s, String value){
        ItemStack item = new ItemStack(Material.valueOf(value));
        ItemMeta meta = item.getItemMeta();
        List<String> lore = new ArrayList<>();

        meta.setDisplayName(Util.formatString(s.toString()));

        for (String option: s.getOptions()) {
            option = Util.formatString(option);
            if(Objects.equals(settings.getString(s), option)){
                lore.add(">" + option);
            } else {
                lore.add(option);
            }
        }


        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public @NotNull Menu getMenu() {
        return menu;
    }

    @Override
    public Collection<Player> getViewers() {
        return null;
    }
}
