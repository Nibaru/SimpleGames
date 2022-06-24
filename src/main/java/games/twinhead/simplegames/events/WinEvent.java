package games.twinhead.simplegames.events;

import games.twinhead.simplegames.game.Game;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class WinEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player winner, loser;
    private final Game game;


    public WinEvent(Player winner, Player loser, Game game){
        this.game = game;
        this.winner = winner;
        this.loser = loser;
    }


    @NotNull
    @Override
    public HandlerList getHandlers() {return HANDLERS;}
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    public Player getWinner() {
        return winner;
    }

    public Player getLoser() {
        return loser;
    }

    public Game getGame() {
        return game;
    }
}
