package games.twinhead.simplegames.game;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.*;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GameManager {

    private final ArrayList<Game> activeGames = new ArrayList<>();
    private final ArrayList<GameInvite> gameInvites = new ArrayList<>();

    public GameManager() {}

    public void addGame(GameInvite invite){
        Game game = new Game(invite);
        activeGames.add(game);
        game.openAll();
    }

    public void acceptGameInvite(UUID inviteId){
        for (GameInvite invite: gameInvites) {
            if(invite.getInviteId().equals(inviteId)) {
                invite.setState(InviteState.ACCEPTED);
                addGame(invite);
            }
        }
    }

    public void declineGameInvite(UUID inviteId){
        for (GameInvite invite: gameInvites) {
            if(invite.getInviteId().equals(inviteId)) {
                invite.setState(InviteState.DECLINED);
                addGame(invite);
            }
        }
    }

    public void sendGameInvite(GameType type, Player sender, Player receiver){
        gameInvites.add(new GameInvite(type, sender, receiver));
    }

    public void clearActiveGames(){
        for (Game g: getActiveGames()) {
            g.getScreen().getMenu().close();
        }
    }

    public List<Game> getActiveGames(){
        return activeGames;
    }

    public List<Game> getActiveGames(Player player){
        List<Game> games = new ArrayList<>();
        for (Game g: getActiveGames()) {
            games.add(g);
        }
        return games;
    }

    public Boolean gameExists(UUID uuid){
        for (Game g: getActiveGames()) {
            if(g.getGameId().equals(uuid)) return true;
        }
        return null;
    }

    public @Nullable Game getGame(UUID uuid){
        for (Game g: getActiveGames()) {
            if(g.getGameId().equals(uuid)) return g;
        }
        return null;
    }


    public void sendAcceptMessage(Player host, List<Player> challengers, GameType gameType) {
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

        for(Player player: challengers){
            player.spigot().sendMessage(spacer);
            player.spigot().sendMessage(content);
            player.spigot().sendMessage(component);
            player.spigot().sendMessage(spacer);
        }
    }

    /**
    public void waitForChallenger(Game game) {
        sendAcceptMessage(game.getPlayer(), game.getOpponents(game.getPlayer()), game.getGameType());

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
                    //TODO change this to handle a game with more than one player
                    game.getOpponents(game.getPlayer()).get(0).sendMessage("Your invite to " + game.getGameType().getDisplayName() + "from " + game.getPlayer().getDisplayName() + " has expired");
                    game.getPlayer().sendMessage("Your invite to play " + game.getGameType().getDisplayName() + " with " + game.getOpponents(game.getPlayer()).get(0).getDisplayName() + " has expired");
                    this.cancel();
                }
            }

        }.runTaskTimer(SimpleGames.getInstance(), 20 * 1, 20 * 1);
    }**/
}
