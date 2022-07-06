package games.twinhead.simplegames.screen.game;

import games.twinhead.simplegames.game.GameType;
import org.bukkit.entity.Player;

public interface PickPlayerCallBack {
    void PlayerPicked(Player player);
    void PlayerPicked(Player player, GameType gameType);
}
