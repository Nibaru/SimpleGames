package games.twinhead.simplegames.game;

import games.twinhead.simplegames.SimpleGames;
import games.twinhead.simplegames.events.WinEvent;
import games.twinhead.simplegames.screen.ConnectFourScreen;
import games.twinhead.simplegames.screen.RockPaperScissorsScreen;
import games.twinhead.simplegames.screen.Screen;
import games.twinhead.simplegames.screen.TicTacToeScreen;
import games.twinhead.simplegames.settings.Setting;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class Game {

    private final GameType gameType;

    private final UUID uuid;
    private GameState state;

    private final Player host;
    private final Player challenger;
    private Material hostMaterial;
    private Material challengerMaterial;

    private Player winner = null;

    private Player currentTurn;

    private final Screen screen;

    private Long turnTimer = 0L;

    public Game(GameType gameType, Player host, Player challenger) {
        this.gameType = gameType;
        this.host = host;
        this.challenger = challenger;
        this.uuid = UUID.randomUUID();
        this.screen = setScreen();
        setTokenMaterial(gameType);
        state = GameState.PENDING;
    }

    private void setTokenMaterial(GameType gameType){
        switch (gameType) {
            case TIC_TAC_TOE -> {
                hostMaterial = Material.valueOf(SimpleGames.getInstance().getSettingsManager().getSettings(host.getUniqueId()).getString(Setting.TIC_TAC_TOE_TOKEN));
                challengerMaterial = Material.valueOf(SimpleGames.getInstance().getSettingsManager().getSettings(challenger.getUniqueId()).getString(Setting.TIC_TAC_TOE_TOKEN));
            } case CONNECT_FOUR -> {
                hostMaterial = Material.valueOf(SimpleGames.getInstance().getSettingsManager().getSettings(host.getUniqueId()).getString(Setting.CONNECT_FOUR_TOKEN));
                challengerMaterial = Material.valueOf(SimpleGames.getInstance().getSettingsManager().getSettings(challenger.getUniqueId()).getString(Setting.CONNECT_FOUR_TOKEN));
            }
        }
    }


    public Player getOpponent(Player player){
        if(player == host) return challenger;
        else return host;
    }


    public void openAll(){
        getScreen().display(host);
        getScreen().display(challenger);
    }

    public void open(Player player){
        getScreen().display(player);
    }


    private Screen setScreen(){
        return switch (getGameType()){
            case TIC_TAC_TOE -> new TicTacToeScreen(this);
            case CONNECT_FOUR -> new ConnectFourScreen(this);
            case ROCK_PAPER_SCISSORS -> new RockPaperScissorsScreen(this);
        };
    }

    public Player getCurrentTurn(){
        if(currentTurn == null)
            if(ThreadLocalRandom.current().nextInt(0, 1 + 1) > 0) {
                currentTurn = host;
            } else {
                currentTurn = challenger;
            }

        return currentTurn;
    }

    public Long timeSinceLastTurn(){
        return System.currentTimeMillis() - turnTimer;
    }

    public String chatPrefix(){
        return ChatColor.BLUE + "[" + getGameType().getDisplayName() + "] " + ChatColor.RESET;
    }

    public void sendTurnReminder(){
        new BukkitRunnable() {
            @Override
            public void run() {
                if(getState().equals(GameState.PLAYING))
                    if(!getScreen().getViewers().contains(getCurrentTurn())){
                        getCurrentTurn().sendMessage(chatPrefix() + "It's your turn " + getOpponent(getCurrentTurn()).getDisplayName() +" is waiting.");
                        this.cancel();
                    }
            }
        }.runTaskLater(SimpleGames.getInstance(), 20 * 10);

    }

    public void changeTurn(){
        if(currentTurn == challenger){
            currentTurn = host;
        } else {
            currentTurn = challenger;
        }
        if(!getScreen().getViewers().contains(getCurrentTurn()))
            turnTimer = System.currentTimeMillis();
        sendTurnReminder();
    }

    public GameState getState(){
        return state;
    }

    public void setState(GameState state){
        if(this.state != GameState.COMPLETED && state == GameState.COMPLETED) {
            winEvent();
            this.currentTurn = null;
        }
        this.state = state;

    }

    public void winEvent(){
        Bukkit.getPluginManager().callEvent(new WinEvent(getCurrentTurn(), getOpponent(getCurrentTurn()), this));
    }

    public Screen getScreen(){
        return screen;
    }

    public Player getHost(){
        return host;
    }

    public Player getChallenger(){
        return challenger;
    }

    public void messagePlayers(String message) {
        getHost().sendMessage(message);
        getChallenger().sendMessage(message);
    }

    public GameType getGameType() {
        return gameType;
    }

    public UUID getUuid() {
        return uuid;
    }

    public Material getHostMaterial() {
        return hostMaterial;
    }

    public Material getChallengerMaterial() {
        return challengerMaterial;
    }

    public Player getWinner() {
        return winner;
    }

    public void setWinner(Player winner) {
        this.winner = winner;
    }
}
