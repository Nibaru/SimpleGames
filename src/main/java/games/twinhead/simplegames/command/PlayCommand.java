package games.twinhead.simplegames.command;

import games.twinhead.simplegames.SimpleGames;
import games.twinhead.simplegames.misc.Game;
import games.twinhead.simplegames.misc.GameType;
import games.twinhead.simplegames.screen.Screen;
import games.twinhead.simplegames.screen.TicTacToeScreen;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class PlayCommand implements TabExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if(!(sender instanceof Player player)) return false;

        if(args.length > 0){
            if(SimpleGames.getInstance().getGameManager().hasGameActive(player)){
                player.sendMessage("You already have an active game use /play to play");
                return true;
            }

            if(args[0].equals("TIC_TAC_TOE")){
                Game game = new Game(GameType.TIC_TAC_TOE, (Player) sender, SimpleGames.getInstance().getServer().getPlayer(args[1]), new TicTacToeScreen((Player) sender, SimpleGames.getInstance().getServer().getPlayer(args[1])));
                SimpleGames.getInstance().getGameManager().addPending(game);
            }
        } else {
            if(SimpleGames.getInstance().getGameManager().hasGameActive(player)){
                SimpleGames.getInstance().getGameManager().getActiveGame(player).getScreen().display(player);
            }
        }



        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> list = new ArrayList<>();

        switch (args.length){
            case 0 -> {

            }
            case 1 -> {
                for(GameType g: GameType.values()){
                    list.add(g.toString());
                }
            }
            case 2 -> {
                for (Player p: SimpleGames.getInstance().getServer().getOnlinePlayers()) {
                    list.add(p.getDisplayName());
                }
            }
        }


        return list;
    }
}
