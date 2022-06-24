package games.twinhead.simplegames.command;

import games.twinhead.simplegames.SimpleGames;
import games.twinhead.simplegames.game.Game;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ListCommand implements TabExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        sender.sendMessage("Active Games:");
        for (Game game: SimpleGames.getInstance().getGameManager().getActiveGames()) {
            sender.sendMessage("    Game: " + game.getGameType() + " " +  game.getHost().getDisplayName() + " Vs. " + game.getChallenger().getDisplayName() + " State: " + game.getState());
        }
        sender.sendMessage("Pending Games:");
        for (Game game: SimpleGames.getInstance().getGameManager().getPendingGames()) {
            sender.sendMessage("    Game: " + game.getGameType() + " " +  game.getHost().getDisplayName() + " Vs. " + game.getChallenger().getDisplayName() + " State: " + game.getState());
        }
        return false;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return null;
    }
}
