package me.stephenminer.raftbattle.game.fishing;

import me.stephenminer.raftbattle.RaftBattle;
import me.stephenminer.raftbattle.game.GameMap;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class FishHelper {
    private final RaftBattle plugin;
    private List<FishingTable> tables;
    private GameMap host;

    public FishHelper(GameMap host){
        this.plugin = JavaPlugin.getPlugin(RaftBattle.class);
        this.host = host;
        loadTables();
    }

    public ItemStack fish(){
        FishingTable table = findTable();
        if (table == null){
            return new ItemStack(Material.DEAD_BUSH);
        }
        return table.makeRoll();
    }

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
        int max = tables.stream().mapToInt(FishingTable::weight).sum();
        int roll = ThreadLocalRandom.current().nextInt(max) + 1;
        for (FishingTable table : tables){
            roll -= table.weight();
            if (roll <= 0) return table;
        }
        return null;
    }

}
