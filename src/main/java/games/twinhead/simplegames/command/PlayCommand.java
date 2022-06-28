package games.twinhead.simplegames.command;

import games.twinhead.simplegames.SimpleGames;
import games.twinhead.simplegames.game.Game;
import games.twinhead.simplegames.game.GameType;
import games.twinhead.simplegames.screen.MainScreen;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.*;
import net.md_5.bungee.api.chat.hover.content.Text;
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

        switch (args.length){
            case 0 -> {
                new MainScreen(player);
            }
            case 1 -> {
                switch (args[0].toUpperCase()){
                    case "MINESWEEPER" -> {
                        if(!SimpleGames.getInstance().getGameManager().startSinglePlayerGame(GameType.MINESWEEPER, player)){
                            sender.sendMessage("You already have an active " + GameType.MINESWEEPER.getDisplayName() + " game [/play] to see your active games");
                        }

                    }
                }
            }
            case 2 -> {
                //todo check if the player selected is a player

                GameType type;
                try {
                    type = GameType.valueOf(args[0].toUpperCase());
                } catch (IllegalArgumentException e){
                    sender.sendMessage("Unable to find game type: " + args[0]);
                    return false;
                }

                SimpleGames.getInstance().getGameManager().sendGameInvite(type, player, SimpleGames.getInstance().getServer().getPlayer(args[1]));
                return true;

            }
        }

        return false;
    }

    public void sendGameMessage(Game game, Player player){
        TextComponent content = new TextComponent("Game: " + game.getGameType().getDisplayName() + " vs. " + game.getOpponents(player).get(0).getDisplayName());
        TextComponent play = new TextComponent("[ /Play ]");
        TextComponent abandon = new TextComponent("[ /Abandon ]");

        play.setColor(ChatColor.BLUE);
        abandon.setColor(ChatColor.RED);

        play.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/play " + game.getGameType()));
        play.setHoverEvent( new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Click to Play")));
        abandon.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/abandon " + game.getGameType()));
        abandon.setHoverEvent( new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Click to Abandon")));

        BaseComponent[] component = new ComponentBuilder().append(content).append(" ").append(play).append(" ").append(abandon).create();

        player.spigot().sendMessage(component);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> list = new ArrayList<>();

        switch (args.length){
            case 0 -> {

            }
            case 1 -> {
                for(GameType g: GameType.values()){
                    list.add(g.toString().toLowerCase());
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
