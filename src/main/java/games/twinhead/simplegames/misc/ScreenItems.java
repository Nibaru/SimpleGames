package games.twinhead.simplegames.misc;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.ipvp.canvas.template.ItemStackTemplate;

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
            return item;
        };
    }


}
