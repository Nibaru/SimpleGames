package games.twinhead.simplegames.screen;

import games.twinhead.simplegames.game.Game;
import org.bukkit.entity.Player;
import org.ipvp.canvas.Menu;
import org.ipvp.canvas.type.ChestMenu;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class RockPaperScissorsScreen implements Screen{

    private final Menu menu;

    private final Game game;

    public RockPaperScissorsScreen(Game game){
        menu = ChestMenu.builder(6)
                .title(game.getGameType().getDisplayName())
                .build();

        this.game = game;
    }



    @Override
    public void display() {

        display(game.getHost());
        display(game.getChallenger());
    }

    @Override
    public void display(Player player) {
        menu.open(player);
    }

    @Override
    public @NotNull Menu getMenu() {
        return menu;
    }


    @Override
    public Collection<Player> getViewers(){
        return menu.getViewers();
    }
}
