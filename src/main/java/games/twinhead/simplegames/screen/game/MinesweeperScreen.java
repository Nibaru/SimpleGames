package games.twinhead.simplegames.screen.game;

import games.twinhead.simplegames.game.Game;
import games.twinhead.simplegames.game.GameState;
import games.twinhead.simplegames.game.GameType;
import games.twinhead.simplegames.screen.Screen;
import games.twinhead.simplegames.screen.ScreenItems;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockDataMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.ipvp.canvas.slot.SlotSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MinesweeperScreen extends Screen {

    private int numberOfMines = 5;
    private final Material boardMaterial = Material.GRAY_STAINED_GLASS_PANE;
    private MineSlot[][] slots;
    private Boolean minesExploded = false;

    List<MineSlot> emptySlots = new ArrayList<>();

    private final Game game;

    public MinesweeperScreen(Game game) {
        super(GameType.MINESWEEPER.getDisplayName());
        this.game = game;
        init();
    }

    public MinesweeperScreen(Game game, int numberOfMines){
        super(GameType.MINESWEEPER.getDisplayName() + " - " + numberOfMines + " mines.");
        this.numberOfMines = numberOfMines;
        this.game = game;
        init();
    }

    private void init(){
        slots = new MineSlot[getMenu().getDimensions().getRows()][getMenu().getDimensions().getColumns()];

        for (int i = 0; i < getMenu().getDimensions().getRows(); i++) {
            for (int j = 0; j < getMenu().getDimensions().getColumns(); j++) {
                slots[i][j] = new MineSlot(i, j);
            }
        }
        populateMineSlots();
    }


    @Override
    public void display(Player player){
        setItemSettings();
        getMenu().open(player);
    }

    public Material getNumberMaterial(int i){
        return switch (i){
            case 0 -> Material.LIGHT_GRAY_STAINED_GLASS_PANE;
            case 1 -> Material.GREEN_STAINED_GLASS_PANE;
            case 2 -> Material.YELLOW_STAINED_GLASS_PANE;
            case 3 -> Material.ORANGE_STAINED_GLASS_PANE;
            case 4 -> Material.RED_STAINED_GLASS_PANE;
            default -> Material.PURPLE_STAINED_GLASS_PANE;
        };
    }

    public SlotSettings MineSlotSettings(MineSlot mine){
        return SlotSettings.builder().itemTemplate(viewer -> {
            ItemStack item = new ItemStack((mine.flagged ? Material.PAPER : (mine.clicked ? Material.ORANGE_STAINED_GLASS_PANE : boardMaterial)));
            if(minesExploded && mine.containsMine) item = new ItemStack(Material.TNT);

            if(item.getType() == Material.LIGHT){
                BlockDataMeta blockDataMeta = (BlockDataMeta) item.getItemMeta();
                BlockData blockData = item.getType().createBlockData();
                ((Levelled) blockData).setLevel(getNumberOfMines(mine));
                blockDataMeta.setBlockData(blockData);
                item.setItemMeta(blockDataMeta);
            }

            ItemMeta meta = item.getItemMeta();
            List<String> lore = new ArrayList<>();

            if(getNumberOfMines(mine) == 0 && mine.clicked) item.setAmount(0);

            assert meta != null;
            meta.setDisplayName((mine.flagged ? "Flagged" : (minesExploded ? "Mine" : (mine.clicked ? String.valueOf(getNumberOfMines(mine)) : " "))));

            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.values());
            item.setItemMeta(meta);
            return ScreenItems.addBorderToItem(item).getItem(viewer);
        }).clickHandler((playerWhoClicked, clickInformation) -> {
            switch (clickInformation.getClickType()){
                case LEFT -> {
                    if(!mine.flagged){
                        game.setState(GameState.PLAYING);
                        mine.clicked = true;
                        if(getNumberOfMines(mine) == 0)
                            clearEmptySlots(mine);
                        emptySlots.clear();
                        if(mine.containsMine){
                            this.minesExploded = true;
                            this.game.setState(GameState.COMPLETED);
                        }

                        if(allSlotsClicked()) {
                            game.setWinner(playerWhoClicked);
                            game.setState(GameState.COMPLETED);
                        }
                    }
                }
                case RIGHT -> flagMine(mine);
            }
            display(playerWhoClicked);
        }).build();
    }

    private int getNumberOfMines(MineSlot mine){
        int count = 0;
        for(MineSlot m :getSurroundingMines(mine)){
            if(m.containsMine) count ++;
        }
        return count;
    }

    private List<MineSlot> getSurroundingMines(MineSlot mine){
        List<MineSlot> s = new ArrayList<>();
        int mineRow = mine.row;
        int mineCol = mine.column;

        if(mineRow - 1 >= 0){
            s.add(slots[mine.row - 1][mine.column]); //TOP
            if(mineCol + 1 < 9){
                s.add(slots[mine.row - 1][mine.column + 1]); //TOP RIGHT
            }
            if(mineCol - 1 > 0){
                s.add(slots[mine.row - 1][mine.column - 1]); //TOP LEFT
            }
        }

        if(mineRow + 1 < 6){
            s.add(slots[mine.row + 1][mine.column]); // BOTTOM

            if(mineCol + 1 < 9){
                s.add(slots[mine.row + 1][mine.column + 1]); // BOTTOM RIGHT
            }
            if(mineCol > 0){
                s.add(slots[mine.row + 1][mine.column - 1]); // BOTTOM LEFT
            }
        }

        if(mineCol + 1 < 9){
            s.add(slots[mine.row][mine.column + 1]); // RIGHT
        }
        if(mineCol - 1 >= 0){
            s.add(slots[mine.row ][mine.column - 1]); // LEFT
        }
        return s;
    }

    private Boolean allSlotsClicked(){
        for(MineSlot[] row: slots)
            for(MineSlot m: row)
                if(!m.clicked && !m.containsMine) return false;

        return true;
    }

    private void clearEmptySlots(MineSlot mine){
        for(MineSlot m: getSurroundingMines(mine)){
            if(!m.containsMine){
                if(!emptySlots.contains(m)){
                    emptySlots.add(m);
                    if(getNumberOfMines(m) == 0)
                        clearEmptySlots(m);
                }
            }
        }
        if(emptySlots.size() > 4)
            for(MineSlot slot: emptySlots){
                if(!slot.containsMine){
                    slot.clicked = true;
                    slot.flagged = false;
                }
            }
    }

    private void setItemSettings(){
        for (MineSlot[] row: slots) {
            for(MineSlot m: row){
                getMenu().getSlot(m.row + 1, m.column + 1).setSettings(MineSlotSettings(m));
            }
        }
    }

    private void populateMineSlots(){
        List<int[]> mineSlots = new ArrayList<>();
        for (int i = 0; i < numberOfMines; i++) {
            int randomCol = new Random().nextInt(0, getMenu().getDimensions().getColumns());
            int randomRow = new Random().nextInt(0, getMenu().getDimensions().getRows());

            while(mineSlots.contains(new int[]{randomRow, randomCol})){
                randomCol = new Random().nextInt(0, getMenu().getDimensions().getColumns());
                randomRow = new Random().nextInt(0, getMenu().getDimensions().getRows());
            }
            mineSlots.add(new int[]{randomRow, randomCol});
        }

        for(int[] i: mineSlots){
            slots[i[0]][i[1]].containsMine = true;
        }
    }

    private void flagMine(MineSlot mine){
        for(MineSlot[] row: slots)
            for(MineSlot m: row)
                if(m == mine) m.flagged = !m.flagged;

    }

    class MineSlot {
        int column, row;
        boolean flagged;
        boolean containsMine;
        boolean clicked;

        public MineSlot(int row, int column){
            this.column = column;
            this.row = row;
            this.flagged = false;
        }
    }
}



