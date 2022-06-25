package games.twinhead.simplegames.screen;

import games.twinhead.simplegames.game.Game;
import games.twinhead.simplegames.game.GameState;
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

public class ConnectFourScreen extends Screen{

    //TODO Enchant the items for the winning row
    private final Game game;

    private final Slot[] winningRow = new Slot[4];

    private final int[][] board = {
            {0,0,0,0,0,0,0},
            {0,0,0,0,0,0,0},
            {0,0,0,0,0,0,0},
            {0,0,0,0,0,0,0},
            {0,0,0,0,0,0,0},
            {0,0,0,0,0,0,0},
    };

    private final int[] lastSlotPlayed = new int[2];



    public ConnectFourScreen(Game game){
        super(game.getGameType().getDisplayName());
        this.game = game;
    }

    @SuppressWarnings("SpellCheckingInspection")
    private void drawBoard(){
        Mask mask = RecipeMask.builder(getMenu())
                .item('B', ScreenItems.turnIndicator(game.getHost(), game))
                .item('b', ScreenItems.turnIndicator(game.getChallenger(), game))
                .item('H', ScreenItems.playerItem(game.getHost(), "Host -"))
                .item('h', ScreenItems.tokenDisplayItem(game.getHostMaterial(), game.getHost()))
                .item('C', ScreenItems.playerItem(game.getChallenger(), "Challenger -"))
                .item('c', ScreenItems.tokenDisplayItem(game.getChallengerMaterial(), game.getChallenger()))
                .item('G', ScreenItems.simpleItem(Material.AIR, " ", new ArrayList<>()))

                .pattern("BGGGGGGGb")
                .pattern("BGGGGGGGb")
                .pattern("BGGGGGGGb")
                .pattern("BGGGGGGGb")
                .pattern("HGGGGGGGC")
                .pattern("hGGGGGGGc").build();
        mask.apply(getMenu());

        for (int col = 0; col < 7; col++) {
            for (int row = 0; row < 6; row++) {
                getMenu().getSlot(row + 1 , col + 2).setItemTemplate(boardItem(row, col));
                handler(row, col);
            }
        }
    }

    private Boolean checkForWinner(int player){
        //Check Horizontally
        for (int col = 0; col < 7-3; col++)
            for (int row = 0; row < 6; row++)
                if(this.board[row][col] == player && this.board[row][col+1] == player && this.board[row][col+2] == player && this.board[row][col+3] == player){
                    col++;
                    this.winningRow[0] = getMenu().getSlot(row+1, col + 1);
                    this.winningRow[1] = getMenu().getSlot(row+1, col+1 + 1);
                    this.winningRow[2] = getMenu().getSlot(row+1, col+2 + 1);
                    this.winningRow[3] = getMenu().getSlot(row+1, col+3 + 1);
                    return true;
                }


        //Check Vertically
        for (int col = 0; col < 7; col++)
            for (int row = 0; row < 6-3; row++)
                if(this.board[row][col] == player && this.board[row+1][col] == player && this.board[row+2][col] == player && this.board[row+3][col] == player){
                    col++;
                    this.winningRow[0] = getMenu().getSlot(row+1, col + 1);
                    this.winningRow[1] = getMenu().getSlot(row+1 + 1, col + 1);
                    this.winningRow[2] = getMenu().getSlot(row+2 + 1, col + 1);
                    this.winningRow[3] = getMenu().getSlot(row+3 + 1, col + 1);
                    return true;
                }


        // ascendingDiagonalCheck
        for (int col=0; col< 7 - 3;  col++){
            for (int row=3; row < 6; row++){
                if (this.board[row][col] == player && this.board[row-1][col+1] == player && this.board[row-2][col+2] == player && this.board[row-3][col+3] == player){
                    col++;
                    this.winningRow[0] = getMenu().getSlot(row + 1, col + 1);
                    this.winningRow[1] = getMenu().getSlot(row-1 + 1, col+1 + 1);
                    this.winningRow[2] = getMenu().getSlot(row-2 + 1, col+2 + 1);
                    this.winningRow[3] = getMenu().getSlot(row-3 + 1, col+3 + 1);
                    return true;
                }
            }
        }
        // descendingDiagonalCheck
        for (int row=3; row< 6; row++){
            for (int col=3; col< 7; col++){
                if (this.board[row][col] == player && this.board[row-1][col-1] == player && this.board[row-2][col-2] == player && this.board[row-3][col-3] == player){
                    col++;
                    this.winningRow[0] = getMenu().getSlot(row + 1, col + 1);
                    this.winningRow[1] = getMenu().getSlot(row-1 + 1, col-1 + 1);
                    this.winningRow[2] = getMenu().getSlot(row-2 + 1, col-2 + 1);
                    this.winningRow[3] = getMenu().getSlot(row-3 + 1, col-3 + 1);
                    return true;
                }
            }
        }
        return false;
    }

    private int lowestRow(int col){
        for (int row = 1; row < 6; row++) {
            if(board[row][col] == 1 || board[row][col] == 2){
                return row - 1;
            } else if(row == 5){
                return row;
            }
        }
        return 0;
    }

    private ItemStackTemplate boardItem(int row, int col){
        return player -> {
            ItemStack item = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
            ItemMeta meta = item.getItemMeta();
            List<String> lore = new ArrayList<>();

            assert meta != null;
            meta.setDisplayName(" ");

            if(board[row][col] == 0){
                item = new ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE);
                if(game.getCurrentTurn().equals(player)){
                    lore.add(ChatColor.GRAY + "   Click to play here.   ");
                } else {
                    lore.add(ChatColor.GRAY + "   "  + game.getCurrentTurn().getDisplayName() + "'s turn.   ");
                }

                lore.add("");
            } else if (board[row][col] == 1) {
                item = new ItemStack(game.getHostMaterial());
                meta.setDisplayName(ChatColor.GRAY + game.getHost().getDisplayName());
            } else if (board[row][col] == 2) {
                item = new ItemStack(game.getChallengerMaterial());
                meta.setDisplayName(ChatColor.GRAY + game.getChallenger().getDisplayName());
            }


            meta.setLore(lore);
            item.setItemMeta(meta);
            return item;
        };
    }

    private Boolean checkForDraw(){
        for(int[] row: board){
            for(int slot: row){
                if(slot == 0) return false;
            }
        }
        return !winCheck();
    }


    private void handler(int row, int col){
        getMenu().getSlot(row + 1, col + 2).setClickHandler((player, info) ->{
            if(game.getCurrentTurn() != player) return;
            if(game.getState().equals(GameState.DRAW) || game.getState().equals(GameState.COMPLETED)) return;

            game.setState(GameState.PLAYING);
            if(game.getHost() == player){
                board[lowestRow(col)][col] = 1;
            } else if (game.getChallenger() == player) {
                board[lowestRow(col)][col] = 2;
            }


            if (winCheck()) {
                ScreenItems.enchantSlots(winningRow, game.getHost());
            }else if(checkForDraw()){
                game.setState(GameState.DRAW);
                drawBoard();
            }else {
                game.changeTurn();
            }


            lastSlotPlayed[0] = lowestRow(col);
            lastSlotPlayed[1] = col + 2;

            drawBoard();
            enchantLastToken();

        });
    }

    private void enchantLastToken(){
        Slot slot = getMenu().getSlot(lastSlotPlayed[0], lastSlotPlayed[1]);
        ScreenItems.unEnchantSlot(slot, game.getHost());
        ScreenItems.enchantSlot(getMenu().getSlot(lastSlotPlayed[0] + 2, lastSlotPlayed[1]), game.getHost());
    }

    public Boolean winCheck(){
        if(checkForWinner(1)){
            game.setState(GameState.COMPLETED);
            game.setWinner(game.getHost());
            return true;
        } else if(checkForWinner(2)){
            game.setState(GameState.COMPLETED);
            game.setWinner(game.getChallenger());
            return true;
        }
        return false;
    }

    @Override
    public void display(Player player) {
        drawBoard();
        getMenu().open(player);
    }
}
