package me.stephenminer.raftbattle.game;

import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Sheep;

public class SheepCore {
    private final Sheep sheep;
    private final Location spawn;

    public int health, maxHealth;

    public SheepCore(Location spawn, int health){
        this.health = health;
        this.maxHealth = health;
        this.spawn = spawn;
        this.sheep = (Sheep) spawn.getWorld().spawnEntity(spawn, EntityType.SHEEP);
        sheep.setMaxHealth(maxHealth);
        sheep.setHealth(health);
    }

    public void syncHealth(){
        sheep.setHealth(health);
    }


    public void setHealth(int health){
        this.health = health;
        syncHealth();
    }

    public boolean isDead(){ return health <= 0; }
}
