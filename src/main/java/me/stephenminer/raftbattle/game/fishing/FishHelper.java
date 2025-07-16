package me.stephenminer.raftbattle.game.fishing;

import me.stephenminer.raftbattle.RaftBattle;
import me.stephenminer.raftbattle.game.GameMap;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class FishHelper {
    private final RaftBattle plugin;
    private final Random random;
    private List<FishingTable> tables;
    private GameMap host;

    public FishHelper(GameMap host){
        this.plugin = JavaPlugin.getPlugin(RaftBattle.class);
        this.host = host;
        this.random = new Random();
        loadTables();
    }

    /**
     * Rolls a drop based on the stored FishingTables
     * @return an ItemStack from one of the FishingTables
     */
    public ItemStack fish(){
        FishingTable table = findTable();
        if (table == null){
            return new ItemStack(Material.DEAD_BUSH);
        }
        return table.makeRoll();
    }

    /**
     * Loads all of the fishing loot tables from the loot.yml file
     * and stores them in a list of FishingTable objects sorted by their weights/chances to happen
     * The sorting is not really relevant anymore
     */
    private void loadTables(){
        tables = new ArrayList<>();
        Set<String> ids = plugin.loot.getConfig().getKeys(false);
        for (String id : ids){
            FishingTable table = new LootLoader(id).build();
            if (table != null) tables.add(table);
        }
        tables.sort(Comparator.comparingInt(FishingTable::weight));

    }

    private FishingTable findTable(){
        //stuff before is for when loot tables were weight baed
        /*
        int max = tables.stream().mapToInt(FishingTable::weight).sum();
        int roll = ThreadLocalRandom.current().nextInt(max) + 1;
        for (FishingTable table : tables){
            roll -= table.weight();
            if (roll <= 0) return table;
        }
        return null;

         */
        /*
        Old code to choose a random loot table that passed a roll check
        if (pool.isEmpty()) return null;
        else return pool.get(random.nextInt(pool.size()));
         */
        List<FishingTable> pool = rollTables();
        return mergeTables(pool);
    }

    /**
     * Randomly selects FishingTables from our total pool of FishingTables by
     * rolling a number 0 - 99 for each one. If that our roll is less than the table's "chance,"
     * then we add it to the ArrayList we return
     * @return A List containing randomly selected FishingTables
     */
    private List<FishingTable> rollTables(){
        List<FishingTable> tables = new ArrayList<>();
        for (FishingTable table : this.tables){
            int roll = random.nextInt(100);
            if (roll < table.weight()){
                tables.add(table);
            }
        }
        return tables;
    }

    /**
     * Merges all LootPairs in each of the provided FishingTables into a singular FishingTable object
     * @param tables a Collection of FishingTables to merge
     * @return A new FishingTable with a null id and -1 weight and all the LootPairs found in each of the provided FishingTables
     */
    private FishingTable mergeTables(Collection<FishingTable> tables){
        List<LootPair> loot = new ArrayList<>();
        for (FishingTable table : tables){
            loot.addAll(table.loot());
        }
        return new FishingTable(null, 0, loot);
    }

}
