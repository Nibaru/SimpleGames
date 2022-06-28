package games.twinhead.simplegames.screen;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.ipvp.canvas.slot.SlotSettings;

public class BigScreen extends Screen{

    private final int[] gridIndex = new int[]
            {0, 0};

    private final char[][] grid = new char[][]{
            {'1','1','1','1','1','1','1','1','1','1','1','1','1','1','1','1','1'},
            {'1','1','0','0','0','0','0','0','1','0','0','0','0','0','0','1','1'},
            {'1','0','1','0','0','0','0','0','1','0','0','0','0','0','1','0','1'},
            {'1','0','0','1','0','0','0','0','1','0','0','0','0','1','0','0','1'},
            {'1','0','0','0','1','0','0','0','1','0','0','0','1','0','0','0','1'},
            {'1','0','0','0','0','1','0','0','1','0','0','1','0','0','0','0','1'},
            {'1','0','0','0','0','0','1','0','1','0','1','0','0','0','0','0','1'},
            {'1','0','0','0','0','0','0','1','1','1','0','0','0','0','0','0','1'},
            {'1','0','0','0','0','0','0','0','1','0','0','0','0','0','0','0','1'},
            {'1','0','0','0','0','0','0','0','1','0','0','0','0','0','0','0','1'},
            {'1','1','1','1','1','1','1','1','1','1','1','1','1','1','1','1','1'}
    };

    public BigScreen(String screenName) {
        super(screenName);
    }

    public void moveScreen(Direction d){
        switch (d){
            case LEFT -> {
                if(gridIndex[1] < grid[0].length - 9) gridIndex[1]++;
            }
            case RIGHT -> {
                if(gridIndex[1] > 0) gridIndex[1]--;
            }
            case UP -> {
                if(gridIndex[0] > 0) gridIndex[0]--;
            }
            case DOWN -> {
                if(gridIndex[0] > grid[0].length - 6) gridIndex[0]++;
            }
        }
    }

    public String gridToPattern(int row){
        StringBuilder s = new StringBuilder();

        for(char c: grid[row]){
            s.append(c);
        }
        Bukkit.broadcastMessage(s.toString());
        return s.toString();
    }

    @Override
    public void display(Player player){
        drawGrid();
        getMenu().getSlot(53).setSettings(controllerItem());
        getMenu().open(player);
    }

    private void drawGrid(){
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 9; j++) {
                getMenu().getSlot(i + 1, j + 1).setItem(charToItem(grid[i + gridIndex[0]][j + gridIndex[1]]));
            }
        }
    }

    private ItemStack charToItem(char c){
        return switch (c){
            case '0' -> new ItemStack(Material.AIR);
            case '1' -> new ItemStack(Material.STONE);
            default -> new ItemStack(Material.BEDROCK);
        };
    }

    private SlotSettings controllerItem(){
        return SlotSettings.builder().item(new ItemStack(Material.COMPARATOR)).clickHandler((playerWhoClicked, clickInformation) -> {
            switch (clickInformation.getClickType()){
                case LEFT -> moveScreen(Direction.LEFT);
                case SHIFT_LEFT -> {

                }
                case RIGHT -> moveScreen(Direction.RIGHT);

                case SHIFT_RIGHT -> {

                }
                case MIDDLE -> {

                }
                case NUMBER_KEY -> {

                }
                case DOUBLE_CLICK -> {

                }
                case DROP -> moveScreen(Direction.UP);
                case CONTROL_DROP -> moveScreen(Direction.DOWN);
                case SWAP_OFFHAND -> {

                }
            }
            display(playerWhoClicked);
        }).build();
    }

    private enum Direction{
        LEFT,
        RIGHT,
        UP,
        DOWN
    }
}
