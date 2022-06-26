package games.twinhead.simplegames.game;

import games.twinhead.simplegames.SimpleGames;
import games.twinhead.simplegames.screen.Screen;
import games.twinhead.simplegames.screen.game.ConnectFourScreen;
import games.twinhead.simplegames.screen.game.MinesweeperScreen;
import games.twinhead.simplegames.screen.game.RockPaperScissorsScreen;
import games.twinhead.simplegames.screen.game.TicTacToeScreen;
import games.twinhead.simplegames.settings.Setting;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.*;

public class Game {

    private final Screen screen;
    private final List<Player> players = new ArrayList<>();
    private Player currentTurnPlayer;
    private GameState state = GameState.PENDING;
    private final HashMap<Player, Material> materials = new HashMap<>();
    private final GameType gameType;
    private final UUID gameId;

    private Player winner = null;

    public Game(GameType gameType){
        this.gameType = gameType;
        this.screen = getScreen(gameType);
        this.gameId = UUID.randomUUID();
    }

    public Game(GameInvite invite){
        this.gameType = invite.getGameType();
        this.screen = getScreen(gameType);
        this.gameId = UUID.randomUUID();
        addPlayer(invite.getSender());
        addPlayer(invite.getReceiver());
    }

    public Screen getScreen(GameType gameType){
        return switch (gameType){
            case TIC_TAC_TOE -> new TicTacToeScreen(this);
            case CONNECT_FOUR -> new ConnectFourScreen(this);
            case ROCK_PAPER_SCISSORS -> new RockPaperScissorsScreen(this);
            case MINESWEEPER -> new MinesweeperScreen(this);
        };
    }

    public void messagePlayers(String message) {
        for(Player player: getPlayers()){
            player.sendMessage(message);
        }
    }

    public List<Player> getOpponents(Player player){
        List<Player> list = new ArrayList<>();
        for (Player p: getPlayers()) {
            if(!player.equals(p)) list.add(p);
        }
        return list;
    }
    public void open(Player player){
        getScreen().display(player);
    }

    public void openAll(){
        for(Player p: getPlayers()){
            open(p);
        }
    }

    public Screen getScreen() {
        return screen;
    }

    public List<Player> getPlayers(){
        return players;
    }

    public Player getPlayer(int index){
        return players.get(index);
    }

    public void setPlayerMaterial(Player player){
        String mat = switch (getGameType()){
            case TIC_TAC_TOE -> SimpleGames.getInstance().getSettingsManager().getSettings(player.getUniqueId()).getString(Setting.TIC_TAC_TOE_TOKEN);
            case CONNECT_FOUR -> SimpleGames.getInstance().getSettingsManager().getSettings(player.getUniqueId()).getString(Setting.CONNECT_FOUR_TOKEN);
            case ROCK_PAPER_SCISSORS, MINESWEEPER -> "AIR";
        };

        materials.put(player, Material.valueOf(mat));
    }

    public Material getPlayerMaterial(Player player){
        return materials.get(player);
    }

    public void removePlayer(Player p){
        players.remove(p);
    }

    public void addPlayer(Player p){
        players.add(p);
        setPlayerMaterial(p);
    }

    public void addPlayers(List<Player> p){
        players.addAll(p);
    }

    public void changeTurn(){
        setCurrentTurn(((getCurrentTurn() != null && getCurrentTurn().equals(getPlayer(0))) ? getPlayer(1) : getPlayer(0)));
    }

    public void setCurrentTurn(Player player){
        this.currentTurnPlayer = player;
    }

    public Player getCurrentTurn() {
        if(currentTurnPlayer == null) setCurrentTurn(getRandomPlayer());
        return currentTurnPlayer;
    }

    public Player getRandomPlayer(){
        return players.get(new Random().nextInt(0, players.size()));
    }

    public GameState getState() {
        return state;
    }

    public void setState(GameState state) {
        this.state = state;
    }

    public GameType getGameType() {
        return this.gameType;
    }

    public UUID getGameId() {
        return gameId;
    }

    public Player getWinner() {
        return winner;
    }

    public void setWinner(Player winner) {
        this.winner = winner;
    }
}
