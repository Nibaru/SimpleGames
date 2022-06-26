package games.twinhead.simplegames.game;

import org.bukkit.ChatColor;

public enum GameType {
    TIC_TAC_TOE(ChatColor.YELLOW.toString() + ChatColor.BOLD.toString() +  "Tic Tac Toe"),
    CONNECT_FOUR(ChatColor.BLUE.toString() + ChatColor.BOLD.toString() + "Connect Four"),
    ROCK_PAPER_SCISSORS(ChatColor.DARK_GRAY.toString() + ChatColor.BOLD.toString() + "Rock Paper Scissors"),
    MINESWEEPER(ChatColor.DARK_GRAY.toString() + ChatColor.BOLD.toString() + "Minesweeper");

    final String displayName;

    GameType(String displayName){
        this.displayName = displayName;
    }

    public String getDisplayName(){
        return this.displayName;
    }

}
