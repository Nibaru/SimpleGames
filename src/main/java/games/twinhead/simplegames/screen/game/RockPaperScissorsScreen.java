package games.twinhead.simplegames.screen.game;

import games.twinhead.simplegames.SimpleGames;
import games.twinhead.simplegames.game.Game;
import games.twinhead.simplegames.game.GameState;
import games.twinhead.simplegames.game.GameType;
import games.twinhead.simplegames.misc.Util;
import games.twinhead.simplegames.screen.Screen;
import games.twinhead.simplegames.screen.ScreenItems;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.ipvp.canvas.mask.Mask;
import org.ipvp.canvas.mask.RecipeMask;
import org.ipvp.canvas.slot.SlotSettings;
import org.ipvp.canvas.template.ItemStackTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class RockPaperScissorsScreen extends Screen {

    private final Game game;
    private Option hostOption = Option.NONE;
    private Option challengerOption = Option.NONE;
    private Boolean revealed = false;
    private Boolean countDownStarted = false;

    public RockPaperScissorsScreen(Game game){
        super(GameType.ROCK_PAPER_SCISSORS.getDisplayName());
        this.game = game;
    }

    @Override
    public void display(Player player) {
        drawScreen();
        getMenu().open(player);
        changeState();
    }

    public void drawScreen(){
        Mask mask = RecipeMask.builder(getMenu())
                .item('0', new ItemStack(Material.GRAY_STAINED_GLASS_PANE, 0))
                .item('h', ScreenItems.playerItem(game.getPlayer(0), ChatColor.AQUA + "Host - "))
                .item('c', ScreenItems.playerItem(game.getPlayer(1), ChatColor.AQUA + "Challenger - "))
                .item('r', optionSlotSetting(Option.ROCK))
                .item('p', optionSlotSetting(Option.PAPER))
                .item('s', optionSlotSetting(Option.SCISSORS))
                .item('R', topOption(Option.ROCK))
                .item('P', topOption(Option.PAPER))
                .item('S', topOption(Option.SCISSORS))
                .pattern("0000c0000")
                .pattern("00R0P0S00")
                .pattern("000000000")
                .pattern("00r0p0s00")
                .pattern("000000000")
                .pattern("0000h0000").build();
        mask.apply(getMenu());
    }

    public SlotSettings optionSlotSetting(Option option){
        return SlotSettings.builder().itemTemplate(optionItem(option)).clickHandler((player, clickInformation) -> {
            if(countDownStarted) return;

            if(player.equals(game.getPlayer(0))){
                hostOption = option;
            } else if(player.equals(game.getPlayer(1))) {
                challengerOption = option;
            }

            if(!hostOption.equals(Option.NONE) && !challengerOption.equals(Option.NONE)){
                if(!countDownStarted) countDown();
            }

            refreshMenu();
        }).build();
    }

    private void countDown(){
        countDownStarted = true;
        new BukkitRunnable() {
            int count = 5;
            @Override
            public void run() {
                refreshMenu();
                if(count <= 0) {
                    revealed = true;
                    refreshMenu();
                    getMenu().getSlot(22).setItemTemplate(winIndicatorItem(getWinner()));
                    this.cancel();
                } else {
                    getMenu().getSlot(22).setItemTemplate(countDown(count));
                    count--;
                }
            }
        }.runTaskTimer(SimpleGames.getInstance(), 0, 20);
    }

    private ItemStackTemplate winIndicatorItem(Player winner){
        return player -> {
            ItemStack item = (game.getState().equals(GameState.DRAW) ? new ItemStack(Material.FIREWORK_STAR) : new ItemStack(Material.EMERALD));
            List<String> lore = new ArrayList<>();

            ItemMeta meta = item.getItemMeta();
            assert meta != null;
            meta.setDisplayName(ChatColor.YELLOW + (game.getState().equals(GameState.DRAW) ? "Draw!" : winner.getDisplayName() + " Wins!"));
            meta.setLore(lore);
            item.setItemMeta(meta);
            return ScreenItems.addBorderToItem(item).getItem(player);
        };
    }

    private ItemStackTemplate countDown(int seconds){
        return player -> {
            ItemStack item = new ItemStack(Material.CLOCK);
            List<String> lore = new ArrayList<>();
            ItemMeta meta = item.getItemMeta();
            assert meta != null;

            meta.setDisplayName(ChatColor.GRAY + "Revealing in " + ChatColor.GOLD + seconds +"s");
            meta.setLore(lore);
            item.setItemMeta(meta);
            return ScreenItems.addBorderToItem(item).getItem(player);
        };
    }

    public Player getWinner(){
        if(hostOption.winTo() == challengerOption)
            return game.getPlayer(0);
        return game.getPlayer(1);
    }

    private void changeState(){
        if(hostOption == challengerOption) {
            game.setState(GameState.DRAW);
        } else if(revealed){
            game.setState(GameState.COMPLETED);
        } else {
            game.setState(GameState.PLAYING);
        }
    }

    private Option getPlayerOption(Player player){
        if (player.equals(game.getPlayer(0))) return hostOption;
        if (player.equals(game.getPlayer(1))) return challengerOption;
        return null;
    }

    public ItemStackTemplate topOption(Option option){
        return player -> {
            ItemStack item = null;
            List<String> lore = new ArrayList<>();
            switch (option) {
                case ROCK -> item = new ItemStack(Material.STONE);
                case PAPER -> item = new ItemStack(Material.PAPER);
                case SCISSORS -> item = new ItemStack(Material.SHEARS);
            }

            assert item != null;
            ItemMeta meta = item.getItemMeta();
            Player opponent = game.getOpponents(player).get(0);

            if(!revealed){
                lore.add("");
                lore.add(ChatColor.GRAY +"Waiting for "+ ChatColor.YELLOW + (Objects.equals(getPlayerOption(opponent), Option.NONE) ? opponent.getDisplayName() : "you") + ".");
            } else {
                if(Objects.equals(getPlayerOption(opponent), option)){
                    lore.add("");
                    lore.add(ChatColor.GRAY + opponent.getDisplayName() + "'s selection");
                    assert meta != null;
                    meta.addEnchant(Enchantment.LUCK, 1, false);
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                } else {
                    item = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
                }
            }

            assert meta != null;
            meta.setDisplayName(ChatColor.WHITE + Util.formatString(option.toString()));
            meta.setLore(lore);
            item.setItemMeta(meta);
            return ScreenItems.addBorderToItem(item).getItem(player);
        };
    }

    public ItemStackTemplate optionItem(Option option){
        return player -> {
            ItemStack item = null;
            List<String> lore = new ArrayList<>();

            switch (option) {
                case ROCK -> item = new ItemStack(Material.STONE);
                case PAPER -> item = new ItemStack(Material.PAPER);
                case SCISSORS -> item = new ItemStack(Material.SHEARS);
            }

            assert item != null;
            ItemMeta meta = item.getItemMeta();
            assert meta != null;

            if(Objects.equals(getPlayerOption(player), option)){
                lore.add("");
                lore.add(ChatColor.GRAY + "Your Selection");
                meta.addEnchant(Enchantment.LUCK, 1, false);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            } else {
                if(revealed)
                    item = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
            }

            meta.setDisplayName(ChatColor.WHITE + Util.formatString(option.toString()));
            meta.setLore(lore);
            item.setItemMeta(meta);
            return ScreenItems.addBorderToItem(item).getItem(player);
        };
    }


}

enum Option{
    ROCK,
    PAPER,
    SCISSORS,
    NONE;

    public Option winTo(){
        return switch (this){
            case ROCK -> SCISSORS;
            case PAPER -> ROCK;
            case SCISSORS -> PAPER;
            case NONE -> NONE;
        };
    }
}
