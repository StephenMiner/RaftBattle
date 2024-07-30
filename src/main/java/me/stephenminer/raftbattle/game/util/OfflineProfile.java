package me.stephenminer.raftbattle.game.util;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class OfflineProfile {
    private final UUID uuid;
    private final ItemStack[] items;
    private final double health;
    private final float saturation;
    private final int food;

    public OfflineProfile(UUID uuid, double health, int food, float saturation, ItemStack[] items){
        this.uuid = uuid;
        this.items = items;
        this.health = health;
        this.food = food;
        this.saturation = saturation;

    }

    public void loadOntoPlayer(Player player){
        player.getInventory().setContents(items);
        player.setHealth(health);
        player.setSaturation(saturation);
        player.setFoodLevel(food);
    }
    public UUID uuid(){ return uuid; }
    public ItemStack[] items(){ return items; }
    public double health(){ return health; }
    public int food(){ return food; }
    public float saturation(){ return saturation; }
}
