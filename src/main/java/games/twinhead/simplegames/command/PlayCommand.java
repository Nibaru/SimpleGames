package games.twinhead.simplegames.command;

import games.twinhead.simplegames.SimpleGames;
import games.twinhead.simplegames.game.Game;
import games.twinhead.simplegames.game.GameType;
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
                if(SimpleGames.getInstance().getGameManager().hasGameActive(player)){
                    if(SimpleGames.getInstance().getGameManager().getActiveGames(player).size() > 1){
                        for(Game g: SimpleGames.getInstance().getGameManager().getActiveGames(player)){
                            sendGameMessage(g, player);
                        }
                    } else {
                        SimpleGames.getInstance().getGameManager().getActiveGames(player).get(0).getScreen().display(player);
                    }
                    return true;
                }
            }
            case 1 -> {

                GameType type;
                try {
                    type = GameType.valueOf(args[0].toUpperCase());
                } catch (IllegalArgumentException e){
                    sender.sendMessage("Unable to find game type: " + args[0]);
                    return false;
                }

                if(SimpleGames.getInstance().getGameManager().hasGameOfThisTypeActive(player, type)) {
                    for(Game game: SimpleGames.getInstance().getGameManager().getActiveGames(player)){
                        if(game.getGameType().equals(type)){
                            game.open(player);
                        }
                    }
                    return true;
                } else {
                    player.sendMessage("You don't have a game of " + type.getDisplayName() + " active.");
                    return false;
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

                if(!SimpleGames.getInstance().getGameManager().hasGameOfThisTypeActive(player, type)){
                    Game game = new Game(type, player, SimpleGames.getInstance().getServer().getPlayer(args[1]));
                    SimpleGames.getInstance().getGameManager().addPending(game);
                    return true;
                }
            }
        }

        return false;
    }

    public void sendGameMessage(Game game, Player player){
        TextComponent content = new TextComponent("Game: " + game.getGameType().getDisplayName() + " vs. " + game.getOpponent(player).getDisplayName());
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
