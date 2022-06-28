package games.twinhead.simplegames.events;

import games.twinhead.simplegames.game.Game;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class GameEndEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Game game;

    public GameEndEvent(Game game) {
        this.game = game;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    public Game getGame() {
        return game;
    }
}
