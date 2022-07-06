package games.twinhead.simplegames.screen.game;

import games.twinhead.simplegames.SimpleGames;
import games.twinhead.simplegames.game.GameType;
import games.twinhead.simplegames.screen.Screen;
import games.twinhead.simplegames.screen.ScreenItems;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.ipvp.canvas.slot.SlotSettings;

import java.util.ArrayList;
import java.util.List;

public class PlayGameScreen extends Screen implements PickPlayerCallBack{

    private Player player;

    public PlayGameScreen(Player player) {
        super("Pick a game to play!");
        this.player = player;
        display(player);
    }

    @Override
    public void display(Player player){
        int slot = 10;
        for (GameType type: GameType.values()) {
            getMenu().getSlot(slot++).setSettings(GameItem(type));
            slot++;
        }
        getMenu().open(player);
    }

    public void playGame(GameType type){

    }

    public SlotSettings GameItem(GameType type){
        return SlotSettings.builder().itemTemplate(viewer -> {
            ItemStack item = switch (type){
                case TIC_TAC_TOE -> new ItemStack(Material.MUSIC_DISC_WAIT);
                case CONNECT_FOUR -> new ItemStack(Material.MUSIC_DISC_MELLOHI);
                case ROCK_PAPER_SCISSORS -> new ItemStack(Material.MUSIC_DISC_STAL);
                case MINESWEEPER -> new ItemStack(Material.MUSIC_DISC_CHIRP);
            };

            ItemMeta meta = item.getItemMeta();
            List<String> lore = new ArrayList<>();

            meta.setDisplayName(type.getDisplayName());

            lore.add("");
            lore.add(ChatColor.GRAY + "Click to play!");


            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.values());
            item.setItemMeta(meta);
            return ScreenItems.addBorderToItem(item).getItem(viewer);
        }).clickHandler((player1, clickInformation) -> {
            if(type.isSinglePlayer()){
                playGame(type);
            } else {
                new PickPlayerScreen(player1, this, type);
            }
        }).build();
    }


    @Override
    public void PlayerPicked(Player player) {

    }

    @Override
    public void PlayerPicked(Player player, GameType gameType) {
        SimpleGames.getInstance().getGameManager().sendGameInvite(gameType, this.player, player);
        getMenu().close();
    }
}
