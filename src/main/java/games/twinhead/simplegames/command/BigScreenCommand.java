package games.twinhead.simplegames.command;

import games.twinhead.simplegames.screen.BigScreen;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;


public class BigScreenCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if(!(sender instanceof Player)) return false;

        BigScreen screen = new BigScreen("Big Screen Test");
        screen.display(((Player) sender).getPlayer());

        return false;
    }
}
