package games.twinhead.simplegames.screen;

import games.twinhead.simplegames.SimpleGames;
import games.twinhead.simplegames.game.Game;
import games.twinhead.simplegames.game.GameState;
import games.twinhead.simplegames.misc.ItemUtil;
import games.twinhead.simplegames.misc.Util;
import games.twinhead.simplegames.settings.Setting;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.ipvp.canvas.slot.Slot;
import org.ipvp.canvas.template.ItemStackTemplate;

import java.util.ArrayList;
import java.util.List;

public class ScreenItems {

    public static ItemStackTemplate simpleItem(Material material, String displayName, List<String> lore){
        return player -> {
            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            if(meta != null){
                meta.setDisplayName(displayName);
                meta.setLore(lore);
                item.setItemMeta(meta);

            }
            if(material.equals(Material.AIR)){
                return item;
            } else {
                return addBorderToItem(item).getItem(player);
            }

        };
    }
    public static ItemStackTemplate tokenDisplayItem(Material material, Player head){
        return player -> {
            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            List<String> lore = new ArrayList<>();

            if (meta != null) {
                meta.setDisplayName(ChatColor.GRAY + (head.equals(player) ? "Your Token" : head.getDisplayName() + "'s Token"));
                lore.add(ChatColor.GRAY + "Type: " + Util.formatString(material.toString()));

                if(head.equals(player)){
                    lore.add("");
                    lore.add(ChatColor.GRAY + "[ /Settings] to change your token");
                }

                meta.addEnchant(Enchantment.LUCK, 1, false);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

                meta.setLore(lore);
                item.setItemMeta(meta);
                return addBorderToItem(item).getItem(player);
            }
            return item;
        };
    }
    public static ItemStackTemplate playerItem(Player head, String prefix){
        return player -> {
            ItemStack item = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) item.getItemMeta();
            List<String> lore = new ArrayList<>();

            assert meta != null;
            meta.setOwningPlayer(head);
            if(player.equals(head)){
                meta.setDisplayName(ChatColor.WHITE + prefix + " " + head.getDisplayName() + " (you)");
            } else {
                meta.setDisplayName(ChatColor.WHITE + prefix + " " + head.getDisplayName());
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
            return addBorderToItem(item).getItem(player);
        };
    }

    public static ItemStackTemplate turnIndicator(Player side, Game game){
        return player -> {
            ItemStack item = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
            ItemMeta meta = item.getItemMeta();
            List<String> lore = new ArrayList<>();


            assert meta != null;
            if(game.getCurrentTurn() == player && side == player){
                item = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
                meta.setDisplayName(ChatColor.WHITE + "Your Turn");
            } else {
                if(game.getCurrentTurn() != null) meta.setDisplayName(ChatColor.GRAY + game.getCurrentTurn().getDisplayName() + "'s turn.");
            }

            if(game.getState().equals(GameState.COMPLETED) && !game.getState().equals(GameState.DRAW)){
                meta.setDisplayName(ChatColor.YELLOW + game.getWinner().getDisplayName() + " has won!");
                if(player == game.getWinner()){
                    item = new ItemStack(Material.CYAN_STAINED_GLASS_PANE);
                } else {
                    item = new ItemStack(Material.RED_STAINED_GLASS_PANE);
                }
            } else if(game.getState().equals(GameState.DRAW)){
                meta.setDisplayName(ChatColor.YELLOW + "Draw!");
                item = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
            }

            meta.setLore(lore);
            item.setItemMeta(meta);
            return addBorderToItem(item).getItem(player);
        };
    }

    public static ItemStackTemplate addBorderToItem(ItemStack item){
        return player -> {
            if(SimpleGames.getInstance().getSettingsManager().getSettings(player.getUniqueId()).getBoolean(Setting.SHOW_ITEM_BORDER)){
                return ItemUtil.addBorderToItem(item);
            }
            return item;
        };



    }

    public static ItemStack enchantItem(ItemStack item){
        ItemMeta meta = item.getItemMeta();
        assert meta != null;
        meta.addEnchant(Enchantment.LUCK, 1, false);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }

    public static void unEnchantSlot(Slot slot, Player player){
        ItemStack item = slot.getItem(player);
        ItemMeta meta = item.getItemMeta();
        assert meta != null;
        meta.removeEnchant(Enchantment.LUCK);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        slot.setItem(item);
    }


}
