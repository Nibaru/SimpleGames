package games.twinhead.simplegames.screen;

import games.twinhead.simplegames.game.Game;
import games.twinhead.simplegames.game.GameState;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.ipvp.canvas.Menu;
import org.ipvp.canvas.mask.Mask;
import org.ipvp.canvas.mask.RecipeMask;
import org.ipvp.canvas.slot.Slot;
import org.ipvp.canvas.template.ItemStackTemplate;
import org.ipvp.canvas.type.ChestMenu;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class ConnectFourScreen implements Screen{

    //TODO Enchant the items for the winning row
    private final Menu menu;
    private final Game game;

    private final Slot[] winningRow = new Slot[4];

    private int[][] board = {
            {0,0,0,0,0,0,0},
            {0,0,0,0,0,0,0},
            {0,0,0,0,0,0,0},
            {0,0,0,0,0,0,0},
            {0,0,0,0,0,0,0},
            {0,0,0,0,0,0,0},
    };



    public ConnectFourScreen(Game game){
        menu = ChestMenu.builder(6)
                .title(game.getGameType().getDisplayName())
                .build();

        this.game = game;
    }

    private void drawBoard(){
        Mask mask = RecipeMask.builder(menu)
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
        mask.apply(menu);

        for (int col = 0; col < 7; col++) {
            for (int row = 0; row < 6; row++) {
                menu.getSlot(row + 1 , col + 2).setItemTemplate(boardItem(row, col));
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
                    this.winningRow[0] = menu.getSlot(row, col);
                    this.winningRow[1] = menu.getSlot(row, col+1);
                    this.winningRow[2] = menu.getSlot(row, col+2);
                    this.winningRow[3] = menu.getSlot(row, col+3);
                    return true;
                }


        //Check Vertically
        for (int col = 0; col < 7; col++)
            for (int row = 0; row < 6-3; row++)
                if(this.board[row][col] == player && this.board[row+1][col] == player && this.board[row+2][col] == player && this.board[row+3][col] == player){
                    col++;
                    this.winningRow[0] = menu.getSlot(row, col);
                    this.winningRow[1] = menu.getSlot(row+1, col);
                    this.winningRow[2] = menu.getSlot(row+2, col);
                    this.winningRow[3] = menu.getSlot(row+3, col);
                    return true;
                }


        // ascendingDiagonalCheck
        for (int col=0; col< 7 - 3;  col++){
            for (int row=3; row < 6; row++){
                if (this.board[row][col] == player && this.board[row-1][col+1] == player && this.board[row-2][col+2] == player && this.board[row-3][col+3] == player){
                    col++;
                    this.winningRow[0] = menu.getSlot(row, col);
                    this.winningRow[1] = menu.getSlot(row-1, col+1);
                    this.winningRow[2] = menu.getSlot(row-2, col+2);
                    this.winningRow[3] = menu.getSlot(row-3, col+3);
                    return true;
                }
            }
        }
        // descendingDiagonalCheck
        for (int row=3; row< 6; row++){
            for (int col=3; col< 7; col++){
                if (this.board[row][col] == player && this.board[row-1][col-1] == player && this.board[row-2][col-2] == player && this.board[row-3][col-3] == player){
                    col++;
                    this.winningRow[0] = menu.getSlot(row, col);
                    this.winningRow[1] = menu.getSlot(row-1, col-1);
                    this.winningRow[2] = menu.getSlot(row-2, col-2);
                    this.winningRow[3] = menu.getSlot(row-3, col-3);
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


    private void handler(int row, int col){
        menu.getSlot(row + 1, col + 2).setClickHandler((player, info) ->{
            if(game.getCurrentTurn() != player) return;

            if (!checkForWinner(1) && !checkForWinner(2)) {
                game.setState(GameState.PLAYING);
                if(game.getHost() == player){
                    board[lowestRow(col)][col] = 1;
                } else if (game.getChallenger() == player) {
                    board[lowestRow(col)][col] = 2;
                }

            }

            if (winCheck()) {
                enchantWinningRow();
                drawBoard();
                return;
            }

            game.changeTurn();
            drawBoard();
        });
    }

    private void enchantWinningRow(){
        for (Slot slot: winningRow) {
            Bukkit.broadcastMessage(Arrays.toString(winningRow));

            ItemStack item = slot.getItem(game.getHost());
            ItemMeta meta = item.getItemMeta();
            meta.addEnchant(Enchantment.LUCK, 1, false);
            //meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
            slot.setItem(item);
        }
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
    public void display() {
        drawBoard();

        display(game.getHost());
        display(game.getChallenger());
    }

    @Override
    public void display(Player player) {
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
