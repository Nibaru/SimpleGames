package games.twinhead.simplegames.screen;

import games.twinhead.simplegames.SimpleGames;
import games.twinhead.simplegames.misc.Util;
import games.twinhead.simplegames.settings.PlayerSettings;
import games.twinhead.simplegames.settings.Setting;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.ipvp.canvas.slot.SlotSettings;

import java.util.ArrayList;
import java.util.List;

public class PickTokenScreen extends Screen{

    private final PlayerSettings settings;
    private final SettingsScreen settingsScreen;
    private final Setting setting;

    public PickTokenScreen(Player player, Setting setting, SettingsScreen settingsScreen){
        super("Pick a " + Util.formatString(setting.toString()));

        this.settingsScreen = settingsScreen;
        this.setting = setting;
        settings = SimpleGames.getInstance().getSettingsManager().getSettings(player.getUniqueId());

        display(player);
    }

    @Override
    public List<SlotSettings> getSlotSettings(){
        List<SlotSettings> slotSettings = new ArrayList<>();
        for(ItemStack item: getItemList())
            slotSettings.add(SlotSettings.builder().item(item).clickHandler((player1, clickInformation) -> {
                settings.setSetting(setting, clickInformation.getClickedSlot().getItem(player1).getType().toString());
                settingsScreen.display(player1);
            }).build());
        return slotSettings;
    }

    private List<ItemStack> getItemList(){
        List<ItemStack> items = new ArrayList<>();
        for(Material mat: getMaterialList())
            items.add(item(mat));
        return items;
    }

    private ItemStack item(Material mat){
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();


        item.setItemMeta(meta);
        return item;
    }


    private List<Material> getMaterialList(){
        List<Material> mats = new ArrayList<>();
        mats.addAll(getColorList("CONCRETE_POWDER"));
        mats.addAll(getColorList("STAINED_GLASS"));
        mats.addAll(getColorList("WOOL"));
        mats.addAll(getColorList("TERRACOTTA"));

        return mats;
    }

    private List<Material> getColorList(String block){
        String[] color = {"white", "yellow", "black", "red", "purple", "pink", "orange", "magenta", "lime", "light_gray", "light_blue", "green", "gray", "cyan", "brown", "blue"};
        List<Material> colors = new ArrayList<>();

        for (String c: color) {
            colors.add(Material.valueOf(c.toUpperCase() + "_" + block.toUpperCase()));
        }
        return colors;
    }
}
