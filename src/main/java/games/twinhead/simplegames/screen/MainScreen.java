package games.twinhead.simplegames.screen;

import games.twinhead.simplegames.SimpleGames;
import games.twinhead.simplegames.game.Game;
import games.twinhead.simplegames.game.GameState;
import games.twinhead.simplegames.misc.ItemUtil;
import games.twinhead.simplegames.misc.Util;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.ipvp.canvas.mask.BinaryMask;
import org.ipvp.canvas.mask.Mask;
import org.ipvp.canvas.slot.SlotSettings;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MainScreen extends Screen{

    public MainScreen(Player player) {
        super("Simple Games", player);
        display(player);
    }

    @Override
    public void display(Player player){
        setMenu();
        getMenu().open(player);
    }

    public void refresh(Player player){
        getMenu().open(player);
    }

    public SlotSettings GameSlotSetting(Game game){
        return SlotSettings.builder().itemTemplate(viewer -> {
            ItemStack item = switch (game.getGameType()){
                case TIC_TAC_TOE -> new ItemStack(Material.MUSIC_DISC_WAIT);
                case CONNECT_FOUR -> new ItemStack(Material.MUSIC_DISC_MELLOHI);
                case ROCK_PAPER_SCISSORS -> new ItemStack(Material.MUSIC_DISC_STAL);
                case MINESWEEPER -> new ItemStack(Material.MUSIC_DISC_CHIRP);
            };

            ItemMeta meta = item.getItemMeta();
            List<String> lore = new ArrayList<>();

            assert meta != null;
            meta.setDisplayName(ChatColor.WHITE + "Game: " +  game.getGameType().getDisplayName());
            lore.add(ChatColor.GRAY + "");
            lore.add(ChatColor.GRAY + "   Vs. " + game.getOpponents(viewer).get(0).getDisplayName());
            lore.add(ChatColor.GRAY + "   State: " + (game.getState().equals(GameState.PLAYING) && viewer.equals(game.getCurrentTurn()) ? " Waiting for you." : Util.formatString(game.getState().toString())));
            lore.add("");
            lore.add(ChatColor.GREEN + "[ Click ] to Play!");
            lore.add(ChatColor.DARK_GRAY + "[ Drop ] to Abandon.");

            if(game.getState().equals(GameState.PLAYING) && viewer.equals(game.getCurrentTurn())){
                meta.addEnchant(Enchantment.LUCK, 1, false);
            }

            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.values());
            item.setItemMeta(meta);
            return ScreenItems.addBorderToItem(item).getItem(viewer);
        }).clickHandler((player1, clickInformation) -> {
            switch (clickInformation.getClickType()){
                case LEFT, RIGHT -> {
                    game.open(player1);
                }
                case SHIFT_LEFT, SHIFT_RIGHT -> {
                    ItemStack item = clickInformation.getClickedSlot().getItem(player1);
                    if(!ItemUtil.loreContains(item.getItemMeta().getLore(), "Game Id:")) {
                        item = ItemUtil.addLoreToItem(item.clone(), ChatColor.DARK_GRAY + "Game Id: " + game.getGameId());
                        item = ScreenItems.addBorderToItem(item).getItem(player1);
                        clickInformation.getClickedSlot().setItem(item);
                    } else {
                        clickInformation.getClickedSlot().setSettings(GameSlotSetting(game));
                    }
                    refresh(player1);
                }
                case DROP -> {
                    //TODO Remove game
                    //game.quit(player1);

                    refreshMenu();
                }
            }
        }).build();
    }

    @Override
    public List<SlotSettings> getSlotSettings(){
        List<SlotSettings> slotSettings = new ArrayList<>();
        for(Game game: SimpleGames.getInstance().getGameManager().getActiveGames(getOwner()))
            slotSettings.add(GameSlotSetting(game));
        return slotSettings;
    }

    @NotNull
    @Override
    public Mask getMask(){
        return BinaryMask.builder(getMenuBuilder(getScreenName()).build())
                .pattern("000000000")
                .pattern("000000000")
                .pattern("000000000")
                .pattern("000000000")
                .pattern("000000000")
                .pattern("011111110")
                .build();
    }

    @Override
    public int getNextButtonSlot(){
        return 53 - 9;
    }

    @Override
    public int getPreviousButtonSlot(){
        return 45;
    }
}
