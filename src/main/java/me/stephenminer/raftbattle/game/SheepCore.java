package me.stephenminer.raftbattle.game;

import me.stephenminer.raftbattle.RaftBattle;
import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class SheepCore {
    private final RaftBattle plugin;
    private final Sheep sheep;
    private final Location spawn;

    private double health, maxHealth;

    public ItemStack compass;

    public SheepCore(Location spawn, double health, String mapId){
        this.plugin = JavaPlugin.getPlugin(RaftBattle.class);
        this.health = health;
        this.maxHealth = health;
        this.spawn = spawn;
        this.sheep = (Sheep) spawn.getWorld().spawnEntity(spawn, EntityType.SHEEP);
        sheep.setMaxHealth(maxHealth);
        sheep.setHealth(health);
        sheep.setMetadata("mapId",new FixedMetadataValue(plugin,mapId));
        compass = compassItem();
    }

    public void startTracking(GameMap map, Collection<UUID> uuids){
        new BukkitRunnable(){
            @Override
            public void run(){
                if (!map.started() || isDead()){
                    this.cancel();
                    return;
                }
                for (UUID uuid : uuids){
                    OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
                    if (offline.isOnline() && offline.getPlayer().getGameMode() == GameMode.SURVIVAL)
                        offline.getPlayer().setCompassTarget(sheep.getLocation());
                }
            }
        }.runTaskTimer(plugin, 1,35);
    }

    public void syncHealth(){
        sheep.setHealth(Math.max(0,health));
    }


    public void setHealth(double health){
        this.health = health;
        syncHealth();
    }

    private ItemStack compassItem(){
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + "Sheep Finder 2000");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.ITALIC + "I hope this isn't pointing at MY sheep!");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isDead(){ return health <= 0; }

    public double health(){ return health; }
    public double maxHealth(){ return maxHealth; }

    public Sheep sheep(){ return sheep; }
}
