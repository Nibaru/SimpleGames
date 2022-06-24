package games.twinhead.simplegames.events;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class OnWinEvent implements Listener {


    @EventHandler
    public void onWin(WinEvent event){
        Bukkit.broadcastMessage(event.getWinner().getDisplayName() + " has won a game of " + event.getGame().getGameType().getDisplayName() + " vs. " + event.getLoser().getDisplayName());
    }
}
