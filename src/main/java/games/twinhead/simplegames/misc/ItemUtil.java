package games.twinhead.simplegames.misc;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ItemUtil {

    public ItemUtil(){}

    public static ItemStack addBorderToItem(ItemStack item){
        List<String> newList = new ArrayList<>();
        ItemMeta meta = item.getItemMeta();
        List<String> oldLore = (meta.getLore() == null || meta.getLore().isEmpty()) ? new ArrayList<>() : meta.getLore();

        if(!meta.getDisplayName().equals(" "))
            newList.add("   " + meta.getDisplayName() + "   ");

        meta.setDisplayName(" ");

        for (String string : oldLore) {
            newList.add((string.contains("   ") ? string : "   " + string + "   "));
        }

        newList.add("");

        meta.setLore(newList);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack addLoreToItem(ItemStack item, String newLore){
        ItemMeta meta = item.getItemMeta();
        List<String> lore = (meta.getLore() == null ? new ArrayList<>() : meta.getLore());
        lore.add(newLore);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public static boolean loreContains(List<String> list, String string){
        for(String s: list){
            if(s.contains(string)) return true;
        }
        return false;
    }

    public static ItemStack removeLastLineFromLore(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        List<String> lore = (meta.getLore() == null ? new ArrayList<>() : meta.getLore());
        lore.remove(lore.size()-1);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
}
