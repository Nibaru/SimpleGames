package games.twinhead.simplegames.misc;

import games.twinhead.simplegames.screen.Screen;
import org.bukkit.entity.Player;

public class Game {

    private final GameType gameType;

    private GameState state;
    private final Player host;
    private final Player challenger;

    private final Screen screen;

    public Game(GameType gameType, Player host, Player challenger, Screen screen) {
        this.gameType = gameType;
        this.host = host;
        this.challenger = challenger;
        this.screen = screen;

        state = GameState.PENDING;
    }

    public GameState getState(){
        return state;
    }

    public void setState(GameState state){
        this.state = state;
    }

    public Screen getScreen(){
        return screen;
    }

    public Player getHost(){
        return host;
    }

    public Player getChallenger(){
        return challenger;
    }

    public void messagePlayers(String message) {
        getHost().sendMessage(message);
        getChallenger().sendMessage(message);
    }

    public GameType getGameType() {
        return gameType;
    }
}
