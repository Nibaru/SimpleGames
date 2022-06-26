package games.twinhead.simplegames.screen.game;

import games.twinhead.simplegames.game.Game;
import games.twinhead.simplegames.game.GameType;
import games.twinhead.simplegames.screen.Screen;

public class MinesweeperScreen extends Screen {

    public MinesweeperScreen(Game game) {
        super(GameType.MINESWEEPER.getDisplayName());
    }
}
