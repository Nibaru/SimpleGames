package games.twinhead.simplegames.game;

public enum GameType {
    TIC_TAC_TOE("Tic Tac Toe"),
    CONNECT_FOUR("Connect Four"),
    ROCK_PAPER_SCISSORS("Rock Paper Scissors");


    final String displayName;

    GameType(String displayName){
        this.displayName = displayName;
    }

    public String getDisplayName(){
        return this.displayName;
    }
}
