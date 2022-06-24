package games.twinhead.simplegames.game;

import games.twinhead.simplegames.SimpleGames;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.*;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

public class GameManager {

    private final ArrayList<Game> activeGames = new ArrayList<>();
    private final ArrayList<Game> pendingGames = new ArrayList<>();

    public GameManager() {
    }

    public Boolean hasGameActive(Player player) {
        for (Game game : activeGames) {
            if ((game.getHost() == player || game.getChallenger() == player) && game.getState() != GameState.COMPLETED)
                return true;
        }
        return false;
    }

    public Boolean hasGameOfThisTypeActive(Player player, GameType type) {
        for (Game game : getActiveGames()) {
            if(game.getHost().equals(player) || game.getChallenger().equals(player))
                if (game.getGameType().equals(type) && (game.getState().equals(GameState.STARTING) ||game.getState().equals(GameState.PLAYING) || game.getState().equals(GameState.PENDING))) return true;
        }
        return false;
    }

    public List<Game> getActiveGames(Player player) {
        List<Game> games = new ArrayList<>();
        for (Game game : activeGames) {
            if (game.getHost() == player || game.getChallenger() == player) games.add(game);
        }
        return games;
    }

    public Boolean acceptGame(Player player) {
        for (Game g : pendingGames) {
            if (g.getChallenger() == player && !g.getState().equals(GameState.DECLINED)) {
                g.setState(GameState.STARTING);
                return true;
            }
        }
        return false;
    }

    public Boolean declineGame(Player player) {
        for (Game g : pendingGames) {
            if (g.getChallenger() == player) {
                g.setState(GameState.DECLINED);
                removePending(g);
                return true;
            }

        }
        return false;
    }

    private void removeCompletedGames() {
        activeGames.removeIf(game -> game.getState().equals(GameState.COMPLETED) || game.getState().equals(GameState.DECLINED));
    }

    public void addPending(Game game) {
        pendingGames.add(game);
        waitForChallenger(game);
    }

    public void removePending(Game game) {
        pendingGames.remove(game);
    }

    public void addGame(Game game) {
        pendingGames.remove(game);
        activeGames.add(game);

        game.openAll();
    }

    public void removeGame(Game game) {
        activeGames.remove(game);
    }

    public ArrayList<Game> getActiveGames() {
        return activeGames;
    }

    public ArrayList<Game> getPendingGames(){
        return pendingGames;
    }

    public void clearActiveGames(){
        for(Game game: getActiveGames()){
            if(game.getScreen().getViewers().size() > 0){
                game.getScreen().getMenu().close();
            }
        }
    }


    public void sendAcceptMessage(Player host, Player challenger, GameType gameType) {
        TextComponent content = new TextComponent(host.getDisplayName() + " has challenged you to a game of " + gameType.getDisplayName());
        TextComponent accept = new TextComponent("[ /Accept ]");
        TextComponent decline = new TextComponent("[ /Decline ]");
        TextComponent spacer = new TextComponent(" ");

        accept.setColor(ChatColor.BLUE);
        decline.setColor(ChatColor.RED);

        accept.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/accept"));
        accept.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Click to Accept")));
        decline.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/decline"));
        decline.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Click to Decline")));

        BaseComponent[] component = new ComponentBuilder().append("     ").append(accept).append("     ").append(decline).create();

        challenger.spigot().sendMessage(spacer);
        challenger.spigot().sendMessage(content);
        challenger.spigot().sendMessage(component);
        challenger.spigot().sendMessage(spacer);
    }

    public void waitForChallenger(Game game) {
        sendAcceptMessage(game.getHost(), game.getChallenger(), game.getGameType());

        new BukkitRunnable() {
            Long startTime = System.currentTimeMillis();

            @Override
            public void run() {
                if (game.getState().equals(GameState.STARTING)) {
                    game.messagePlayers("Game Accepted!");
                    addGame(game);
                    this.cancel();

                } else if (game.getState().equals(GameState.DECLINED)) {
                    game.messagePlayers("Game Declined!");
                    removePending(game);

                    this.cancel();
                }

                if (System.currentTimeMillis() > startTime + 30000) {
                    game.getChallenger().sendMessage("Your invite to " + game.getGameType().getDisplayName() + "from " + game.getHost().getDisplayName() + " has expired");
                    game.getHost().sendMessage("Your invite to play " + game.getGameType().getDisplayName() + " with " + game.getChallenger().getDisplayName() + " has expired");
                    this.cancel();
                }
            }

        }.runTaskTimer(SimpleGames.getInstance(), 20 * 1, 20 * 1);
    }
}
