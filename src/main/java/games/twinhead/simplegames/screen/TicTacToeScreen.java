package games.twinhead.simplegames.screen;

import games.twinhead.simplegames.SimpleGames;
import games.twinhead.simplegames.game.Game;
import games.twinhead.simplegames.game.GameState;
import games.twinhead.simplegames.settings.Setting;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.ipvp.canvas.Menu;
import org.ipvp.canvas.mask.Mask;
import org.ipvp.canvas.mask.RecipeMask;
import org.ipvp.canvas.slot.Slot;
import org.ipvp.canvas.template.ItemStackTemplate;
import org.ipvp.canvas.type.ChestMenu;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TicTacToeScreen implements Screen{

    private final Menu menu;
    private final Player host;
    private final Player challenger;
    
    private final Material boardMat = Material.YELLOW_STAINED_GLASS_PANE;

    private final Game game;

    private Slot[] winningRow = new Slot[3];

    private final int[][] boardSlots = {
            {12, 13, 14},
            {21, 22, 23},
            {30, 31, 32}};

    private final Material[][] board = {
            {boardMat, boardMat, boardMat},
            {boardMat, boardMat, boardMat},
            {boardMat, boardMat, boardMat}};

    public TicTacToeScreen(Game game){
        menu = ChestMenu.builder(6)
                .title("Tic Tac Toe")
                .build();

        this.host = game.getHost();
        this.challenger = game.getChallenger();
        this.game = game;
    }
    



    @Override
    public void display() {
        drawScreen();

        clickHandler();

        display(host);
        display(challenger);
    }

    private void clickHandler(){
        for (int[] row: boardSlots) {
            for (int num: row) {
                menu.getSlot(num).setClickHandler(((player, clickInformation) -> {
                    if(checkForWinner()){
                        return;
                    }
                    if(game.getCurrentTurn() == player)
                        if(clickInformation.getClickedSlot().getItem(player).getType().equals(boardMat)) {

                            updateBoard(num, player);

                            if(checkForWinner()) {
                                game.setState(GameState.COMPLETED);
                                ScreenItems.enchantSlots(winningRow, game.getHost());
                                game.setWinner(game.getCurrentTurn());
                            } else if (checkForDraw()) {
                                game.setState(GameState.DRAW);

                            } else {
                                game.setState(GameState.PLAYING);
                            }
                            drawBoard();
                            drawTurnIndicator();
                        }
                }));
            }
        }
    }

    private void drawScreen(){
        Mask mask = RecipeMask.builder(menu)
                .item('T', ScreenItems.turnIndicator(host, game))
                .item('0', ScreenItems.simpleItem(Material.AIR, " ", new ArrayList<>()))
                .item('H', ScreenItems.tokenDisplayItem(game.getHostMaterial(), game.getHost()))
                .item('h', ScreenItems.playerItem(host, ChatColor.AQUA + "Host - "))
                .item('C', ScreenItems.tokenDisplayItem(game.getChallengerMaterial(), game.getChallenger()))
                .item('c', ScreenItems.playerItem(challenger, ChatColor.AQUA + "Challenger - "))
                .pattern("000000000")
                .pattern("000000000")
                .pattern("000000000")
                .pattern("000000000")
                .pattern("h0000000c")
                .pattern("H0000000C").build();
        mask.apply(menu);

        drawBoard();
        drawTurnIndicator();
    }

    private void drawBoard(){
        for (int[] row: boardSlots) {
            for(int slot: row){
                if(menu.getSlot(slot).getItem(host).getType().equals(boardMat) || menu.getSlot(slot).getItem(host).getType().equals(Material.AIR))
                    menu.getSlot(slot).setItemTemplate(boardItem());
            }
        }
    }

    private ItemStackTemplate boardItem(){
        return player -> {
            ItemStack item = new ItemStack(boardMat);
            List<String> lore = new ArrayList<>();

            if(game.getState().equals(GameState.COMPLETED)){
                lore.add(ChatColor.GOLD + "   Winner " + game.getWinner().getDisplayName() + "!   ");
            } else if (game.getState().equals(GameState.DRAW)) {
                lore.add(ChatColor.GOLD + "   Draw!   ");
            } else {
                if(player.equals(game.getCurrentTurn())){
                    lore.add(ChatColor.GOLD + "   Your Turn   ");
                    lore.add(ChatColor.GRAY + "   Click to play here   ");
                } else {
                    lore.add(ChatColor.GRAY + "   " + game.getOpponent(player).getDisplayName() + "'s Turn   ");
                }
            }

            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(" ");
            lore.add("");
            meta.setLore(lore);
            item.setItemMeta(meta);
            return item;
        };
    }

    private Material getMaterial(Player player){
        if(player.equals(host)) return game.getHostMaterial();
        if(player.equals(challenger)) return game.getChallengerMaterial();
        return boardMat;
    }

    private void updateBoard(int num, Player player){
        Material mat = getMaterial(player);

        switch (num){
            case 12 -> board[0][0] = mat;
            case 13 -> board[0][1] = mat;
            case 14 -> board[0][2] = mat;
            case 21 -> board[1][0] = mat;
            case 22 -> board[1][1] = mat;
            case 23 -> board[1][2] = mat;
            case 30 -> board[2][0] = mat;
            case 31 -> board[2][1] = mat;
            case 32 -> board[2][2] = mat;
        }

        if(!checkForWinner()) game.changeTurn();
        menu.getSlot(num).setItemTemplate(TokenItem(mat));
    }

    private void drawTurnIndicator(){
        for (int i = 1; i <= 4; i++) {
            menu.getSlot(i, 1).setItemTemplate(ScreenItems.turnIndicator(game.getHost(), game));
            menu.getSlot(i, 9).setItemTemplate(ScreenItems.turnIndicator(game.getChallenger(), game));
        }
    }

    private ItemStackTemplate TokenItem(Material material){
        return player -> {
            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            List<String> lore = new ArrayList<>();

            meta.setDisplayName(" ");

            if(material == game.getHostMaterial()){
                if(player == host){
                    lore.add(ChatColor.GRAY + "   " + host.getDisplayName() + " (You)   ");
                } else{
                    lore.add(ChatColor.GRAY + "   " +  host.getDisplayName()+ "   ");
                }
            } else {
                if(player != host){
                    lore.add(ChatColor.GRAY + "   " + challenger.getDisplayName() + " (You)   ");
                } else{
                    lore.add(ChatColor.GRAY + "   " +  challenger.getDisplayName()+ "   ");
                }
            }

            lore.add("");
            meta.setLore(lore);
            item.setItemMeta(meta);
            return item;
        };
    }


    
    private boolean checkForDraw(){
        if(!checkForWinner()){
            for (Material[] row: board) {
                for (Material mat : row) {
                    if(mat.equals(boardMat)) return false;
                }
            }
        }
        return true;
    }

    private boolean checkForWinner(){
        for (int i = 0; i < 3; i++) {
            if(board[i][0] == game.getHostMaterial() && board[i][1]  == game.getHostMaterial() && board[i][2]  == game.getHostMaterial()){
                winningRow[0] = getMenu().getSlot(boardSlots[i][0]);
                winningRow[1] = getMenu().getSlot(boardSlots[i][1]);
                winningRow[2] = getMenu().getSlot(boardSlots[i][2]);
                return true;
            }

            if(board[i][0] == game.getChallengerMaterial() && board[i][1]  == game.getChallengerMaterial() && board[i][2]  == game.getChallengerMaterial()){
                winningRow[0] = getMenu().getSlot(boardSlots[i][0]);
                winningRow[1] = getMenu().getSlot(boardSlots[i][1]);
                winningRow[2] = getMenu().getSlot(boardSlots[i][2]);
                return true;
            }

            if(board[0][i] == game.getHostMaterial() && board[1][i]  == game.getHostMaterial() && board[2][i]  == game.getHostMaterial()){
                winningRow[0] = getMenu().getSlot(boardSlots[0][i]);
                winningRow[1] = getMenu().getSlot(boardSlots[1][i]);
                winningRow[2] = getMenu().getSlot(boardSlots[2][i]);
                return true;
            }

            if(board[0][i] == game.getChallengerMaterial() && board[1][i]  == game.getChallengerMaterial() && board[2][i]  == game.getChallengerMaterial()){
                winningRow[0] = getMenu().getSlot(boardSlots[0][i]);
                winningRow[1] = getMenu().getSlot(boardSlots[1][i]);
                winningRow[2] = getMenu().getSlot(boardSlots[2][i]);
                return true;
            }

        }
        if(board[0][0] == game.getHostMaterial() && board[1][1]  == game.getHostMaterial() && board[2][2]  == game.getHostMaterial()){
            winningRow[0] = getMenu().getSlot(boardSlots[0][0]);
            winningRow[1] = getMenu().getSlot(boardSlots[1][1]);
            winningRow[2] = getMenu().getSlot(boardSlots[2][2]);
            return true;
        }
        if(board[2][0] == game.getHostMaterial() && board[1][1]  == game.getHostMaterial() && board[0][2]  == game.getHostMaterial()){
            winningRow[0] = getMenu().getSlot(boardSlots[2][0]);
            winningRow[1] = getMenu().getSlot(boardSlots[1][1]);
            winningRow[2] = getMenu().getSlot(boardSlots[0][2]);
            return true;
        }

        if(board[0][0] == game.getChallengerMaterial() && board[1][1]  == game.getChallengerMaterial() && board[2][2]  == game.getChallengerMaterial()){
            winningRow[0] = getMenu().getSlot(boardSlots[0][0]);
            winningRow[1] = getMenu().getSlot(boardSlots[1][1]);
            winningRow[2] = getMenu().getSlot(boardSlots[2][2]);
            return true;
        }

        if(board[2][0] == game.getChallengerMaterial() && board[1][1]  == game.getChallengerMaterial() && board[0][2]  == game.getChallengerMaterial()){
            winningRow[0] = getMenu().getSlot(boardSlots[2][0]);
            winningRow[1] = getMenu().getSlot(boardSlots[1][1]);
            winningRow[2] = getMenu().getSlot(boardSlots[0][2]);
            return true;
        }

        return false;
    }

    @Override
    public void display(Player player){
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
