package me.stephenminer.raftbattle.game.fishing;

import me.stephenminer.raftbattle.RaftBattle;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class FishingTable {

    private final RaftBattle plugin;
    private final String id;
    //Yeah it is a crazy setup, but it's a very useful one
    private final HashMap<Integer, List<ItemStack>> lootByWeight;
    private final List<LootPair> loot;
    private final Random random;
    private int weight;

    public FishingTable(String id, int weight, List<LootPair> loot){
        random = new Random();
        this.weight = weight;
        this.plugin = JavaPlugin.getPlugin(RaftBattle.class);
        this.id = id;
        this.loot = loot;
        loot.sort(Comparator.comparingInt(LootPair::weight));
        this.lootByWeight = groupByWeight();
    }

    public ItemStack makeRoll(){
        int sum = loot.stream().mapToInt(LootPair::weight).sum();
        int roll = random.nextInt(sum) + 1;
        List<ItemStack> validDrops = rolledItems(roll);
        return validDrops.get(random.nextInt(validDrops.size()));
    }

    private List<ItemStack> rolledItems(int roll){
        List<ItemStack> validDrops = null;
        for (LootPair lootPair : loot) {
            int weight = lootPair.weight();
            roll -= weight;
            if (roll < 1) {
                validDrops = lootByWeight.get(weight);
            }
        }
        return validDrops;
    }

    private HashMap<Integer, List<ItemStack>> groupByWeight(){
        HashMap<Integer, List<ItemStack>> byWeight = new HashMap<>();
        for (LootPair entry : loot){
            int weight = entry.weight();
            if (byWeight.containsKey(weight)) byWeight.get(weight).add(entry.item());
            else{
                List<ItemStack> grouping = new ArrayList<>();
                grouping.add(entry.item());
                byWeight.put(weight,grouping);
            }
        }
        return byWeight;
    }

    public int weight(){ return weight; }
}
