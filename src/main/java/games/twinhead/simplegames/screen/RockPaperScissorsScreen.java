package games.twinhead.simplegames.screen;

import games.twinhead.simplegames.game.Game;

public class RockPaperScissorsScreen extends Screen{

    private final Game game;

    public RockPaperScissorsScreen(Game game){
        super(game.getGameType().getDisplayName());
        this.game = game;


    }
}
