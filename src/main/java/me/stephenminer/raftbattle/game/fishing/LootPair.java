package me.stephenminer.raftbattle.game.fishing;

import org.bukkit.inventory.ItemStack;

public class LootPair {
    private final int weight;
    private final ItemStack item;

    public LootPair(ItemStack item, int weight){
        this.item = item;
        this.weight = weight;
    }

    public int weight(){ return weight; }
    public ItemStack item(){ return item; }
}
