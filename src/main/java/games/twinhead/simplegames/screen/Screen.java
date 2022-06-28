package games.twinhead.simplegames.screen;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.ipvp.canvas.Menu;
import org.ipvp.canvas.mask.BinaryMask;
import org.ipvp.canvas.mask.Mask;
import org.ipvp.canvas.paginate.PaginatedMenuBuilder;
import org.ipvp.canvas.slot.SlotSettings;
import org.ipvp.canvas.template.ItemStackTemplate;
import org.ipvp.canvas.type.ChestMenu;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Screen {

    List<Menu> menu;
    private final String screenName;
    private Player owner;


    public Screen(String screenName){
        this.screenName = screenName;
        setMenu();
    }

    public Screen(String screenName, Player owner){
        this.screenName = screenName;
        this.owner = owner;
        setMenu();
    }



    public void setMenu(){
        menu = PaginatedMenuBuilder.builder(getMenuBuilder(screenName))
                .slots(getMask())
                .nextButton(getNextItem())
                .nextButtonEmpty(new ItemStack(Material.AIR)) // Icon when no next page available
                .nextButtonSlot(getNextButtonSlot())
                .previousButton(getPreviousItem())
                .previousButtonEmpty(new ItemStack(Material.AIR)) // Icon when no previous page available
                .previousButtonSlot(getPreviousButtonSlot())
                .addSlotSettings(getSlotSettings())
                .build();
    }

    public @Nullable Player getOwner(){
        return owner;
    }

    @NotNull
    public String getScreenName(){
        return this.screenName;
    }

    @NotNull

    public Menu.Builder<ChestMenu.Builder> getMenuBuilder(String screenName){
        return ChestMenu.builder(getRows()).title(screenName);
    }

    @NotNull
    public int getNextButtonSlot(){
        return getMenuBuilder(getScreenName()).getDimensions().getArea() - 1;
    }

    @NotNull
    public int getPreviousButtonSlot(){
        return getMenuBuilder(getScreenName()).getDimensions().getArea() - 9;
    }

    @NotNull
    public ItemStackTemplate getNextItem(){
        return player -> {
            ItemStack arrow = new ItemStack(Material.ARROW);
            ItemMeta meta = arrow.getItemMeta();
            assert meta != null;
            meta.setDisplayName(ChatColor.WHITE + "Next");
            arrow.setItemMeta(meta);
            return arrow;
        };
    }

    @NotNull
    public ItemStackTemplate getPreviousItem(){
        return player -> {
            ItemStack arrow = new ItemStack(Material.ARROW);
            ItemMeta meta = arrow.getItemMeta();
            assert meta != null;
            meta.setDisplayName(ChatColor.WHITE + "Previous");
            arrow.setItemMeta(meta);
            return arrow;
        };
    }

    @NotNull
    public Mask getMask(){
        BinaryMask.BinaryMaskBuilder builder = BinaryMask.builder(getMenuBuilder(getScreenName()).build());
        for (int i = 0; i < getRows()-1; i++) {
            builder.pattern("111111111");
        }
        builder.pattern("000000000");
        return builder.build();
    }

    public List<SlotSettings> getSlotSettings(){
        return new ArrayList<>();
    }

    public void display(Player player){
        getMenu().open(player);
    }


    public @NotNull List<Menu> getMenus(){
       return this.menu;
    }

    public @NotNull Menu getMenu(){
        return getMenus().get(0);
    }

    public Collection<Player> getViewers(){
        Collection<Player> players = new ArrayList<>();
        for (Menu m: getMenus()) {
            players.addAll(m.getViewers());
        }
        return players;
    }

    public void refreshMenu(){
        for (Player p: getViewers()) {
            display(p);
        }
    }

    public int getRows() {
        return 6;
    }
}
