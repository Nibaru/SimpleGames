package games.twinhead.simplegames.screen;

import org.bukkit.entity.Player;
import org.ipvp.canvas.Menu;
import org.jetbrains.annotations.NotNull;

public interface Screen {

    void display();

    void display(Player player);

    @NotNull
    Menu getMenu();
}
