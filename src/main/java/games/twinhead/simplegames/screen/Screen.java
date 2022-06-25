package games.twinhead.simplegames.screen;

import org.bukkit.entity.Player;
import org.ipvp.canvas.Menu;
import org.ipvp.canvas.type.ChestMenu;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public interface Screen {

    void display();

    void display(Player player);

    @NotNull
    Menu getMenu();

    Collection<Player> getViewers();
}
