package me.stephenminer.raftbattle.game.fishing;

import me.stephenminer.raftbattle.RaftBattle;
import me.stephenminer.raftbattle.game.GameMap;
import me.stephenminer.raftbattle.game.util.Pond;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class FishHelper {
    private final RaftBattle plugin;
    private final Random random;
    private List<FishingTable> tables;
    private Map<String, List<FishingTable>> pondTables;
    private GameMap host;

    public FishHelper(GameMap host){
        this.plugin = JavaPlugin.getPlugin(RaftBattle.class);
        this.host = host;
        this.random = new Random();
        this.pondTables = new HashMap<>();
        loadTables();
    }

    /**
     * Rolls a drop based on the stored FishingTables
     * @return an ItemStack from one of the FishingTables
     */
    public ItemStack fish(Player roller){
        FishingTable table = findTable(roller);
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
        String mapId = host.id();

        for (String id : ids){
            boolean loaded = attemptLoad(mapId,id);
            if (loaded)
                plugin.getLogger().info("Loaded loot table " + id + " for map " + mapId);

        }
        tables.sort(Comparator.comparingInt(FishingTable::weight));

    }

    /**
     * Determines whether the provided loot table id should be loaded for the provided map
     * @param mapId the mapId to look out for
     * @param lootId the loot id we want to load
     * @return true if the loot id has no loading restrictions, or the mapId meets those descriptions,
     *          false otherwise.
     */
    private boolean attemptLoad(String mapId, String lootId){
        String mapStrs = plugin.loot.getConfig().getString(lootId + ".maps");
        String pondStrs = plugin.loot.getConfig().getString(lootId + ".ponds");

        //map has no loading restrictions
        if ((mapStrs == null ||  mapStrs.isEmpty()) && (pondStrs == null || pondStrs.isEmpty())) {
            FishingTable table = new LootLoader(lootId).build();
            if (table != null) tables.add(table);
            return true;
        }
        //map is designated as having this loottable
        if  (mapStrs != null && mapStrs.contains(mapId)){
            FishingTable table = new LootLoader(lootId).build();
            if (table != null)
                tables.add(table);
            return true;
        }

        //Now check to see if the map's ponds are tied to the loot table
        if (pondStrs != null){
            boolean pass = false;
            for (Pond pond : host.ponds()){
                String pondId = pond.id();
                if (pondStrs.contains(pondId)) {
                    pass = true;
                    FishingTable table = new LootLoader(lootId).build();
                    if (table != null) {
                        //If pondId is in the map, add to the existing list
                        if (pondTables.containsKey(pondId))
                            pondTables.get(pondId).add(table);
                        else{
                            //If pondId isnt in map then we need to make a new list to hold the tables
                            List<FishingTable> pondList = new ArrayList<>();
                            pondList.add(table);
                            pondTables.put(pondId, pondList);
                        }
                    }
                }
            }
            return pass;
        }
        //pretty sure this is unreachable... but eh
        return false;
    }

    private List<FishingTable> rollPool(Player roller){
        if (pondTables.isEmpty()) return tables;
        else if (roller != null && host.ponds().length > 0){
            //If the roller is in one of our map's ponds, we want to include those loot tables in our pool when making rolls
            List<FishingTable> amendedPool = new ArrayList<>(tables);
            for (Pond pond : host.ponds()){
                if (pond.isInPond(roller.getLocation())){
                    for (String pondId : pondTables.keySet())
                        amendedPool.addAll(pondTables.get(pondId));
                }
            }
            return amendedPool;
        }else return tables;
    }

    private FishingTable findTable(Player roller){
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
        List<FishingTable> pool = rollTables(roller);
        return mergeTables(pool);
    }

    /**
     * Randomly selects FishingTables from our total pool of FishingTables by
     * rolling a number 0 - 99 for each one. If that our roll is less than the table's "chance,"
     * then we add it to the ArrayList we return
     * @return A List containing randomly selected FishingTables
     */
    private List<FishingTable> rollTables(Player roller){
        List<FishingTable> tables = new ArrayList<>();
        List<FishingTable> rollPool = rollPool(roller);
        for (FishingTable item : rollPool){
            plugin.getLogger().info(item.id());
        }
        for (FishingTable table : rollPool){
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
