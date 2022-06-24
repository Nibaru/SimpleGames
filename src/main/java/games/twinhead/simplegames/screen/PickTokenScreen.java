package games.twinhead.simplegames.screen;

import games.twinhead.simplegames.SimpleGames;
import games.twinhead.simplegames.settings.PlayerSettings;
import games.twinhead.simplegames.settings.Setting;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.checkerframework.checker.units.qual.A;
import org.ipvp.canvas.Menu;
import org.ipvp.canvas.mask.BinaryMask;
import org.ipvp.canvas.mask.Mask;
import org.ipvp.canvas.paginate.PaginatedMenuBuilder;
import org.ipvp.canvas.slot.SlotSettings;
import org.ipvp.canvas.type.ChestMenu;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PickTokenScreen implements Screen{


    private final Player player;

    private List<Menu> menu;

    private final PlayerSettings settings;
    private final SettingsScreen settingsScreen;

    private final Setting setting;

    public PickTokenScreen(Player player, Setting setting, SettingsScreen settingsScreen){
        this.player = player;
        this.settingsScreen = settingsScreen;
        this.setting = setting;

        settings = SimpleGames.getInstance().getSettingsManager().getSettings(player.getUniqueId());
    }


    @Override
    public void display() {
        Menu.Builder<ChestMenu.Builder> pageTemplate = ChestMenu.builder(6).title(ChatColor.LIGHT_PURPLE + "Pick a token");

        Mask itemSlots = BinaryMask.builder(pageTemplate.getDimensions())
                .pattern("011111110")
                .pattern("011111110")
                .pattern("011111110")
                .pattern("011111110")
                .pattern("011111110").build();

        menu = PaginatedMenuBuilder.builder(pageTemplate)
                .slots(itemSlots)
                .nextButton(new ItemStack(Material.ARROW))
                .nextButtonEmpty(new ItemStack(Material.AIR)) // Icon when no next page available
                .nextButtonSlot(53)
                .previousButton(new ItemStack(Material.ARROW))
                .previousButtonEmpty(new ItemStack(Material.AIR)) // Icon when no previous page available
                .previousButtonSlot(45)
                .addSlotSettings(slotSettings())
                .build();

        menu.get(0).open(player);
    }

    private List<SlotSettings> slotSettings(){
        List<SlotSettings> slotSettings = new ArrayList<>();
        for(ItemStack item: getItemList())
            slotSettings.add(SlotSettings.builder().item(item).clickHandler((player1, clickInformation) -> {
                settings.setSetting(setting, clickInformation.getClickedSlot().getItem(player1).getType().toString());
                settingsScreen.display();
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

    @Override
    public void display(Player player) {
        menu.get(0).open(player);
    }

    @Override
    public @NotNull Menu getMenu() {
        return menu.get(0);
    }

    @Override
    public Collection<Player> getViewers() {
        return null;
    }
}
