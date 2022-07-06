package games.twinhead.simplegames.screen.game;

import games.twinhead.simplegames.SimpleGames;
import games.twinhead.simplegames.game.Game;
import games.twinhead.simplegames.game.GameType;
import games.twinhead.simplegames.screen.Screen;
import games.twinhead.simplegames.screen.ScreenItems;
import games.twinhead.simplegames.settings.Setting;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.ipvp.canvas.slot.SlotSettings;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class PickPlayerScreen extends Screen {

    private Player player;
    private PickPlayerCallBack callBack;
    private GameType gameType;

    public PickPlayerScreen(Player player, PickPlayerCallBack callBack) {
        super("Pick a player to play with!");
        this.player = player;
        this.callBack = callBack;
        display(player);
    }

    public PickPlayerScreen(Player player, PickPlayerCallBack callBack, GameType gameType) {
        super("Pick a player to play with!");
        this.player = player;
        this.callBack = callBack;
        this.gameType = gameType;
        display(player);
    }

    @Override
    public void display(Player player){
        getMenu().open(player);
    }

    public SlotSettings playerHead(Player player){
        return SlotSettings.builder().itemTemplate(viewer -> {
                    ItemStack item = new ItemStack(Material.PLAYER_HEAD);
                    SkullMeta meta = (SkullMeta) item.getItemMeta();
                    List<String> lore = new ArrayList<>();

                    meta.setOwningPlayer(player);
                    meta.setDisplayName(ChatColor.WHITE + player.getDisplayName());

                    lore.add("Click to Invite");

                    meta.setLore(lore);
                    meta.addItemFlags(ItemFlag.values());
                    item.setItemMeta(meta);
                    return ScreenItems.addBorderToItem(item).getItem(viewer);
                }).clickHandler((playerWhoClicked, clickInformation) -> {
                    callBack.PlayerPicked(player, gameType);
                }).build();
    }

    @Override
    public List<SlotSettings> getSlotSettings(){
        List<SlotSettings> slotSettings = new ArrayList<>();
        for(Player player: SimpleGames.getInstance().getServer().getOnlinePlayers())
            if(!SimpleGames.getInstance().getSettingsManager().getSettings(player.getUniqueId()).getBoolean(Setting.HIDE_IN_PLAYER_LIST) && !player.equals(this.player))
                slotSettings.add(playerHead(player));
        return slotSettings;
    }
}
