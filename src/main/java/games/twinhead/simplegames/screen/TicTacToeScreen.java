package games.twinhead.simplegames.screen;

import games.twinhead.simplegames.SimpleGames;
import games.twinhead.simplegames.game.Game;
import games.twinhead.simplegames.game.GameState;
import games.twinhead.simplegames.settings.Setting;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.ipvp.canvas.Menu;
import org.ipvp.canvas.mask.Mask;
import org.ipvp.canvas.mask.RecipeMask;
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

    private final int[][] boardSlots = {
            {10, 11, 12},
            {19, 20, 21},
            {28, 29, 30}};

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

        menu.getSlot(2, 6).setItemTemplate(ScreenItems.playerItem(host, ChatColor.AQUA + "Host - "));
        menu.getSlot(2, 8).setItemTemplate(ScreenItems.playerItem(challenger, ChatColor.AQUA + "Challenger - "));

        menu.getSlot(5, 3).setItemTemplate(ScreenItems.playerItem(game.getCurrentTurn(), ChatColor.AQUA + "Current Turn - "));

        menu.getSlot(3, 6).setItemTemplate(boardItem(game.getHostMaterial()));
        menu.getSlot(3, 8).setItemTemplate(boardItem(game.getChallengerMaterial()));

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
                                menu.getSlot(4, 7).setItem(ScreenItems.simpleItem(Material.DIAMOND, "Winner " + game.getCurrentTurn().getDisplayName(), null).getItem(player));
                                game.setState(GameState.COMPLETED);
                                SimpleGames.getInstance().getGameManager().removeGame(game);
                            } else {
                                game.setState(GameState.PLAYING);
                            }
                        }
                }));
            }
        }
    }

    private void drawScreen(){
        Mask mask = RecipeMask.builder(menu)
                .item('b', ScreenItems.simpleItem(Material.AIR, " ", new ArrayList<>()))
                .item('T', turnIndicator())
                .item('0', ScreenItems.simpleItem(Material.AIR, " ", new ArrayList<>()))
                .item('H', ScreenItems.tokenDisplayItem(game.getHostMaterial(), game.getHost()))
                .item('C', ScreenItems.tokenDisplayItem(game.getChallengerMaterial(), game.getChallenger()))
                .pattern("bbbbbbbbb")
                .pattern("b000b000b")
                .pattern("b000bH0Cb")
                .pattern("b000b000b")
                .pattern("bbbbb000b")
                .pattern("TTTTTTTTT").build();
        mask.apply(menu);

        drawBoard();
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
            ItemMeta meta = item.getItemMeta();
            List<String> lore = new ArrayList<>();
            if(checkForWinner()){
                item = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
                meta.setDisplayName("Winner " + game.getCurrentTurn().getDisplayName());
            } else {
                if(player.equals(game.getCurrentTurn())){
                    meta.setDisplayName("Your Turn");
                    lore.add(ChatColor.GRAY + "Click to play here");
                } else {
                    meta.setDisplayName("Opponents Turn");
                }
            }

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

        num = num + 2;

        switch (num){
            case 10 -> board[0][0] = mat;
            case 11 -> board[0][1] = mat;
            case 12 -> board[0][2] = mat;
            case 19 -> board[1][0] = mat;
            case 20 -> board[1][1] = mat;
            case 21 -> board[1][2] = mat;
            case 28 -> board[2][0] = mat;
            case 29 -> board[2][1] = mat;
            case 30 -> board[2][2] = mat;
        }

        if(!checkForWinner()) game.changeTurn();
        menu.getSlot(5, 3).setItemTemplate(ScreenItems.playerItem(game.getCurrentTurn(), ChatColor.AQUA + "Current Turn - "));

        menu.getSlot(num).setItemTemplate(boardItem(mat));

        for (int i = 1; i <= 9; i++) {
            menu.getSlot(6, i).setItemTemplate(turnIndicator());
        }
        drawBoard();
    }

    private ItemStackTemplate boardItem(Material material){
        return player -> {
            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            List<String> lore = new ArrayList<>();
            if(material == game.getHostMaterial()){
                meta.setDisplayName(ChatColor.WHITE + "X");
                if(player == host) lore.add(ChatColor.GRAY + host.getDisplayName() + " You");
                else lore.add(ChatColor.GRAY +  host.getDisplayName());
            } else {
                if(player != host) lore.add(ChatColor.GRAY + challenger.getDisplayName() + " You");
                else lore.add(ChatColor.GRAY + challenger.getDisplayName());
                meta.setDisplayName(ChatColor.WHITE + "O");
            }
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
            if(board[i][0] == game.getHostMaterial() && board[i][1]  == game.getHostMaterial() && board[i][2]  == game.getHostMaterial())return true;
            if(board[i][0] == game.getChallengerMaterial() && board[i][1]  == game.getChallengerMaterial() && board[i][2]  == game.getChallengerMaterial()) return true;
            if(board[0][i] == game.getHostMaterial() && board[1][i]  == game.getHostMaterial() && board[2][i]  == game.getHostMaterial())return true;
            if(board[0][i] == game.getChallengerMaterial() && board[1][i]  == game.getChallengerMaterial() && board[2][i]  == game.getChallengerMaterial())return true;
        }
        if(board[0][0] == game.getHostMaterial() && board[1][1]  == game.getHostMaterial() && board[2][2]  == game.getHostMaterial())return true;
        if(board[2][0] == game.getHostMaterial() && board[1][1]  == game.getHostMaterial() && board[0][2]  == game.getHostMaterial())return true;

        if(board[0][0] == game.getChallengerMaterial() && board[1][1]  == game.getChallengerMaterial() && board[2][2]  == game.getChallengerMaterial())return true;
        if(board[2][0] == game.getChallengerMaterial() && board[1][1]  == game.getChallengerMaterial() && board[0][2]  == game.getChallengerMaterial())return true;
        return false;
    }

    private ItemStackTemplate turnIndicator(){
        return player -> {
            ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            ItemMeta meta = item.getItemMeta();
            if(checkForWinner()){
                item = new ItemStack(Material.CYAN_STAINED_GLASS_PANE);
                meta.setDisplayName(ChatColor.GREEN + "Winner " + game.getCurrentTurn().getDisplayName() +"!");
            } else if (checkForDraw()) {
                item = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
                meta.setDisplayName(ChatColor.DARK_PURPLE + "Draw!");
            } else {
                if(player.equals(game.getCurrentTurn())){
                    item = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
                    meta.setDisplayName("Your Turn");
                } else {
                    item = new ItemStack(Material.RED_STAINED_GLASS_PANE);
                    meta.setDisplayName("Opponents Turn");
                }
            }


            item.setItemMeta(meta);
            return item;
        };
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
