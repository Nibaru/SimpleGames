package games.twinhead.simplegames.events;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class OnGameEndEvent implements Listener {


    @EventHandler
    public void onWin(GameEndEvent event){
        if(event.getGame().getGameType().isSinglePlayer()){
            Bukkit.broadcastMessage(event.getGame().getPlayer(0).getDisplayName() + " has " + (event.getGame().getWinner() == null ? "lost" : "won") +" a game of " + event.getGame().getGameType().getDisplayName());
        } else {
            StringBuilder s = new StringBuilder();
            for(Player player: event.getGame().getOpponents(event.getGame().getWinner())) s.append(player.getDisplayName());

            Bukkit.broadcastMessage(event.getGame().getWinner().getDisplayName() + " has won a game of " + event.getGame().getGameType().getDisplayName() + " vs. " + s.toString());
        }
    }
}
