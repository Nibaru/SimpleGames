package games.twinhead.simplegames.screen.game;

import games.twinhead.simplegames.game.Game;
import games.twinhead.simplegames.game.GameState;
import games.twinhead.simplegames.screen.Screen;
import games.twinhead.simplegames.screen.ScreenItems;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.ipvp.canvas.mask.Mask;
import org.ipvp.canvas.mask.RecipeMask;
import org.ipvp.canvas.slot.Slot;
import org.ipvp.canvas.template.ItemStackTemplate;

import java.util.ArrayList;
import java.util.List;

public class TicTacToeScreen extends Screen {

    private final Material boardMat = Material.YELLOW_STAINED_GLASS_PANE;

    private final Game game;

    private final Slot[] winningRow = new Slot[3];

    private final int[][] boardSlots = {
            {12, 13, 14},
            {21, 22, 23},
            {30, 31, 32}};

    private final Material[][] board = {
            {boardMat, boardMat, boardMat},
            {boardMat, boardMat, boardMat},
            {boardMat, boardMat, boardMat}};

    public TicTacToeScreen(Game game){
        super(game.getGameType().getDisplayName());
        this.game = game;
    }

    @Override
    public void display(Player player) {
        drawScreen();
        clickHandler();
        getMenu().open(player);
    }

    private void clickHandler(){
        for (int[] row: boardSlots) {
            for (int num: row) {
                getMenu().getSlot(num).setClickHandler(((player, clickInformation) -> {
                    if(checkForWinner()){
                        return;
                    }
                    if(game.getCurrentTurn() == player)
                        if(clickInformation.getClickedSlot().getItem(player).getType().equals(boardMat)) {

                            updateBoard(num, player);

                            if(checkForWinner()) {
                                game.setState(GameState.COMPLETED);
                                for (Slot slot: winningRow) {
                                    slot.setItem(ScreenItems.enchantItem(slot.getItem(player)));
                                }
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
        Mask mask = RecipeMask.builder(getMenu())
                .item('T', ScreenItems.turnIndicator(game.getPlayers().get(0), game))
                .item('0', ScreenItems.simpleItem(Material.AIR, " ", new ArrayList<>()))
                .item('H', ScreenItems.tokenDisplayItem(game.getPlayerMaterial(game.getPlayers().get(0)), game.getPlayers().get(0)))
                .item('h', ScreenItems.playerItem(game.getPlayers().get(0), ChatColor.AQUA + "Host - "))
                .item('C', ScreenItems.tokenDisplayItem(game.getPlayerMaterial(game.getPlayers().get(1)), game.getPlayers().get(1)))
                .item('c', ScreenItems.playerItem(game.getPlayers().get(1), ChatColor.AQUA + "Challenger - "))
                .pattern("000000000")
                .pattern("000000000")
                .pattern("000000000")
                .pattern("000000000")
                .pattern("h0000000c")
                .pattern("H0000000C").build();
        mask.apply(getMenu());

        drawBoard();
        drawTurnIndicator();
    }

    private void drawBoard(){
        for (int[] row: boardSlots) {
            for(int slot: row){
                if(getMenu().getSlot(slot).getItem(game.getPlayer(0)).getType().equals(boardMat) || getMenu().getSlot(slot).getItem(game.getPlayer(0)).getType().equals(Material.AIR))
                    getMenu().getSlot(slot).setItemTemplate(boardItem());
            }
        }
    }

    private ItemStackTemplate boardItem(){
        return player -> {
            ItemStack item = new ItemStack(boardMat);
            List<String> lore = new ArrayList<>();
            ItemMeta meta = item.getItemMeta();
            assert meta != null;

            if(game.getState().equals(GameState.COMPLETED)){
                meta.setDisplayName(ChatColor.GOLD + "Winner " + game.getWinner().getDisplayName() + "!");
            } else if (game.getState().equals(GameState.DRAW)) {
                meta.setDisplayName(ChatColor.GOLD + "Draw!");
            } else {
                if(player.equals(game.getCurrentTurn())){
                    meta.setDisplayName(ChatColor.GOLD + "Your Turn");
                    lore.add(ChatColor.GRAY + "Click to play here");
                } else {
                    meta.setDisplayName(ChatColor.GRAY +  game.getOpponents(player).get(0).getDisplayName() + "'s Turn");
                }
            }

            meta.setLore(lore);
            item.setItemMeta(meta);
            return ScreenItems.addBorderToItem(item).getItem(player);
        };
    }

    private void updateBoard(int num, Player player){
        Material mat = game.getPlayerMaterial(player);

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
        getMenu().getSlot(num).setItemTemplate(TokenItem(mat));
    }

    private void drawTurnIndicator(){
        for (int i = 1; i <= 4; i++) {
            getMenu().getSlot(i, 1).setItemTemplate(ScreenItems.turnIndicator(game.getPlayers().get(0), game));
            getMenu().getSlot(i, 9).setItemTemplate(ScreenItems.turnIndicator(game.getPlayers().get(1), game));
        }
    }

    private ItemStackTemplate TokenItem(Material material){
        return player -> {
            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            List<String> lore = new ArrayList<>();

            assert meta != null;

            if(material == game.getPlayerMaterial(game.getPlayer(0))){
                if(player == game.getPlayer(0)){
                    meta.setDisplayName(ChatColor.GRAY + player.getDisplayName() + " (You)");
                } else{
                    meta.setDisplayName(ChatColor.GRAY + player.getDisplayName());
                }
            } else {
                if(player != game.getPlayer(0)){
                    meta.setDisplayName(ChatColor.GRAY + game.getOpponents(game.getPlayer(0)).get(0).getDisplayName() + " (You)");
                } else{
                    meta.setDisplayName(ChatColor.GRAY + game.getOpponents(game.getPlayer(0)).get(0).getDisplayName());
                }
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
            return ScreenItems.addBorderToItem(item).getItem(player);
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
        for (int j = 0; j <= 1; j++) {
            for (int i = 0; i < 3; i++) {
                if(board[i][0] == game.getPlayerMaterial(game.getPlayer(j)) && board[i][1]  == game.getPlayerMaterial(game.getPlayer(j)) && board[i][2]  == game.getPlayerMaterial(game.getPlayer(j))){
                    winningRow[0] = getMenu().getSlot(boardSlots[i][0]);
                    winningRow[1] = getMenu().getSlot(boardSlots[i][1]);
                    winningRow[2] = getMenu().getSlot(boardSlots[i][2]);
                    return true;
                }

                if(board[0][i] == game.getPlayerMaterial(game.getPlayer(j)) && board[1][i]  == game.getPlayerMaterial(game.getPlayer(j)) && board[2][i]  == game.getPlayerMaterial(game.getPlayer(j))){
                    winningRow[0] = getMenu().getSlot(boardSlots[0][i]);
                    winningRow[1] = getMenu().getSlot(boardSlots[1][i]);
                    winningRow[2] = getMenu().getSlot(boardSlots[2][i]);
                    return true;
                }

            }
            if(board[0][0] == game.getPlayerMaterial(game.getPlayer(j)) && board[1][1]  == game.getPlayerMaterial(game.getPlayer(j)) && board[2][2]  == game.getPlayerMaterial(game.getPlayer(j))){
                winningRow[0] = getMenu().getSlot(boardSlots[0][0]);
                winningRow[1] = getMenu().getSlot(boardSlots[1][1]);
                winningRow[2] = getMenu().getSlot(boardSlots[2][2]);
                return true;
            }
            if(board[2][0] == game.getPlayerMaterial(game.getPlayer(j)) && board[1][1]  == game.getPlayerMaterial(game.getPlayer(j)) && board[0][2]  == game.getPlayerMaterial(game.getPlayer(j))){
                winningRow[0] = getMenu().getSlot(boardSlots[2][0]);
                winningRow[1] = getMenu().getSlot(boardSlots[1][1]);
                winningRow[2] = getMenu().getSlot(boardSlots[0][2]);
                return true;
            }
        }
        return false;
    }
}
