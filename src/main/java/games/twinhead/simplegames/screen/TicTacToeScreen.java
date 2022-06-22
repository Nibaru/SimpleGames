package games.twinhead.simplegames.screen;

import games.twinhead.simplegames.SimpleGames;
import games.twinhead.simplegames.misc.Game;
import games.twinhead.simplegames.misc.GameState;
import games.twinhead.simplegames.misc.ScreenItems;
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
import java.util.concurrent.ThreadLocalRandom;

public class TicTacToeScreen implements Screen{

    private final Menu menu;
    private final Player host;
    private final Player challenger;

    private final Material hostMaterial = Material.FIREWORK_STAR;
    private final Material challengerMaterial = Material.NETHER_STAR;
    private final Material boardMat = Material.YELLOW_STAINED_GLASS_PANE;

    private Player currentTurn;

    private final int[][] boardSlots = {
            {10, 11, 12},
            {19, 20, 21},
            {28, 29, 30}};

    private final Material[][] board = {
            {boardMat, boardMat, boardMat},
            {boardMat, boardMat, boardMat},
            {boardMat, boardMat, boardMat}};

    public TicTacToeScreen(Player host, Player challenger){
        menu = ChestMenu.builder(6)
                .title("Tic Tac Toe")
                .build();

        this.host = host;
        this.challenger = challenger;

        if(ThreadLocalRandom.current().nextInt(0, 1 + 1) > 0) {
            currentTurn = host;
        } else {
            currentTurn = challenger;
        }
    }

    private void drawScreen(){
        Mask mask = RecipeMask.builder(menu)
                .item('b', ScreenItems.simpleItem(Material.BLACK_STAINED_GLASS_PANE, " ", new ArrayList<>()))
                .item('y', boardItem())
                .item('0', ScreenItems.simpleItem(Material.AIR, " ", new ArrayList<>()))
                .pattern("bbbbbbbbb")
                .pattern("byyyb000b")
                .pattern("byyyb000b")
                .pattern("byyyb000b")
                .pattern("bbbbb000b")
                .pattern("bbbbbbbbb").build();
        mask.apply(menu);
    }


    @Override
    public void display() {
        drawScreen();

        menu.getSlot(2, 6).setItem(playerItem(host, ChatColor.AQUA + "Host - "));
        menu.getSlot(2, 8).setItem(playerItem(challenger, ChatColor.AQUA + "Challenger - "));

        menu.getSlot(5, 3).setItem(playerItem(currentTurn, ChatColor.AQUA + "Current Turn - "));

        menu.getSlot(3, 6).setItem(boardItem(hostMaterial));
        menu.getSlot(3, 8).setItem(boardItem(challengerMaterial));

        clickHandler();

        display(host);
        display(challenger);
    }

    private void clickHandler(){
        for (int[] row: boardSlots) {
            for (int num: row) {
                menu.getSlot(num).setClickHandler(((player, clickInformation) -> {
                    if(checkForWinner()) return;
                    if(currentTurn == player)
                        if(clickInformation.getClickedSlot().getItem(player).getType().equals(boardMat)) {
                            menu.getSlot(num).setItem(boardItem(getMaterial(player)));
                            changeBoard(num, getMaterial(player));
                            if(checkForWinner()) {
                                menu.getSlot(4, 7).setItem(ScreenItems.simpleItem(Material.DIAMOND, "Winner" + currentTurn.getDisplayName(), null).getItem(host));
                                SimpleGames.getInstance().getGameManager().getActiveGame(host).setState(GameState.COMPLETED);
                                SimpleGames.getInstance().getGameManager().removeGame(host.getUniqueId());
                            }
                            changeTurn();
                        }
                }));
            }
        }
    }

    private ItemStackTemplate boardItem(){
        return player -> {
            ItemStack item = new ItemStack(boardMat);
            ItemMeta meta = item.getItemMeta();
            if(player.equals(currentTurn)){
                meta.setDisplayName("Your Turn");
            } else {
                meta.setDisplayName("Opponents Turn");
            }

            item.setItemMeta(meta);
            return item;
        };
    }

    private void changeTurn(){
        if(currentTurn == challenger){
            currentTurn = host;
        } else {
            currentTurn = challenger;
        }

        menu.getSlot(5, 3).setItem(playerItem(currentTurn, ChatColor.AQUA + "Current Turn - "));

        for (int i = 1; i <= 9; i++) {
            menu.getSlot(6, i).setItemTemplate(turnIndicator());
        }

    }

    private Material getMaterial(Player player){
        if(player.equals(host)) return hostMaterial;
        if(player.equals(challenger)) return challengerMaterial;
        return boardMat;
    }

    private void changeBoard(int num, Material mat){
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
    }

    private ItemStack boardItem(Material material){
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if(material == hostMaterial){
            meta.setDisplayName(ChatColor.WHITE + "X");
        } else {
            meta.setDisplayName(ChatColor.WHITE + "O");
        }
        item.setItemMeta(meta);
        return item;

    }

    private boolean checkForWinner(){
        for (int i = 0; i < 3; i++) {
            if(board[i][0] == hostMaterial && board[i][1]  == hostMaterial && board[i][2]  == hostMaterial)return true;
            if(board[i][0] == challengerMaterial && board[i][1]  == challengerMaterial && board[i][2]  == challengerMaterial) return true;
            if(board[0][i] == hostMaterial && board[1][i]  == hostMaterial && board[2][i]  == hostMaterial)return true;
            if(board[0][i] == challengerMaterial && board[1][i]  == challengerMaterial && board[2][i]  == challengerMaterial)return true;
        }
        if(board[0][0] == hostMaterial && board[1][1]  == hostMaterial && board[2][2]  == hostMaterial)return true;
        if(board[2][0] == hostMaterial && board[1][1]  == hostMaterial && board[0][2]  == hostMaterial)return true;

        if(board[0][0] == challengerMaterial && board[1][1]  == challengerMaterial && board[2][2]  == challengerMaterial)return true;
        if(board[2][0] == challengerMaterial && board[1][1]  == challengerMaterial && board[0][2]  == challengerMaterial)return true;

        return false;
    }

    private ItemStackTemplate turnIndicator(){
        return player -> {
            ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            ItemMeta meta = item.getItemMeta();
            if(checkForWinner()){
                item = new ItemStack(Material.CYAN_STAINED_GLASS_PANE);
                meta.setDisplayName(currentTurn.getDisplayName());
            } else {
                if(player.equals(currentTurn)){
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

    public ItemStack playerItem(Player player, String prefix){
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwningPlayer(player);

        meta.setDisplayName(prefix + player.getDisplayName());

        item.setItemMeta(meta);
        return item;
    }


}
