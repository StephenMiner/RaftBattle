package me.stephenminer.raftbattle.listeners;

import me.stephenminer.raftbattle.RaftBattle;
import me.stephenminer.raftbattle.game.GameMap;
import me.stephenminer.raftbattle.game.SheepCore;
import me.stephenminer.raftbattle.game.util.OfflineProfile;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.UUID;

public class GameListener implements Listener {
    private final RaftBattle plugin;

    public GameListener(){
        this.plugin = JavaPlugin.getPlugin(RaftBattle.class);
    }


    /**
     * Replaces fished items with items from this game's loot tables if player is in a game
     * @param event
     */
    @EventHandler
    public void onFish(PlayerFishEvent event){
        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH && event.getCaught() instanceof Item){
            Player player = event.getPlayer();
            UUID uuid = player.getUniqueId();
            GameMap map = gameIn(player);
            if (map == null) return;
            Item item = (Item) event.getCaught();
            ItemStack replace = map.fishHelper().fish();
            item.setItemStack(replace);
        }
    }


    @EventHandler
    public void stopPvPDeath(EntityDamageByEntityEvent event){
        if (event.getEntity() instanceof Player){
            Player player = (Player) event.getEntity();
            if (player.getHealth() - event.getFinalDamage() <= 0){
                GameMap map = gameIn(player);
                if (map == null) return;
                if (!map.started()){
                    event.setCancelled(true);
                    player.teleport(map.waiting());
                    player.setHealth(20);
                    player.setFoodLevel(20);
                    player.setSaturation(1);
                    return;
                }
                boolean respawned = map.respawnPlayer(player);
                if (respawned) player.sendMessage(ChatColor.GREEN + "You will respawn shortly");
                else player.sendMessage(ChatColor.RED + "Your team's sheep is dead and you cannot respawn");
                event.setDamage(0);
            }
        }
    }



    @EventHandler
    public void stopOtherDeath(EntityDamageEvent event){
        EntityDamageEvent.DamageCause cause = event.getCause();
        if (cause == EntityDamageEvent.DamageCause.ENTITY_ATTACK || cause == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION ||
                cause == EntityDamageEvent.DamageCause.PROJECTILE) return;
        if (event.getEntity() instanceof Player){
            Player player = (Player) event.getEntity();
            if (player.getHealth() - event.getFinalDamage() <= 0){
                GameMap map = gameIn(player);
                if (map == null) return;
                if (!map.started()){
                    event.setCancelled(true);
                    player.teleport(map.waiting());
                    player.setHealth(20);
                    player.setFoodLevel(20);
                    player.setSaturation(1);
                    return;
                }

                boolean respawned = map.respawnPlayer(player);
                if (respawned) player.sendMessage(ChatColor.GREEN + "You will respawn shortly");
                else player.sendMessage(ChatColor.RED + "Your team's sheep is dead and you cannot respawn!");
                map.broadcastMsg(generateDeathMessage(player,cause,null));
                event.setDamage(0);
            }
        }
    }

    private String generateDeathMessage(Player dead, EntityDamageEvent.DamageCause cause, Entity killer){
        switch (cause){
            case FALLING_BLOCK:
                return dead.getName() + " was squished by a falling block";
            case FALL:
                return dead.getName() + " fell to their doom";
            case THORNS:
                return killer == null ? dead.getName() + " was pricked to death" : dead.getName() + " was pricked to death by " + killer.getName();
            case ENTITY_ATTACK:
                return dead.getName() + " was slain by " + killer.getName();
            case ENTITY_EXPLOSION:
                return dead.getName() + " was blown up by " + killer.getName();
            case LAVA:
                return dead.getName() + " tried to swim in lava";
            case FIRE_TICK:
                return dead.getName() + " burned to death";
            case PROJECTILE:
                return killer != null ? dead.getName() + " was shot by " + killer.getName() :  dead.getName() + " was shot";
            default:
                return dead.getName() + " lost track of their HP";
        }
    }


    @EventHandler
    public void queueTeam(PlayerInteractEvent event){
        if (!event.hasItem()) return;
        ItemStack item = event.getItem();

        Player player = event.getPlayer();
        GameMap map = gameIn(player);
        if (map == null) return;
        ItemMeta meta = item.getItemMeta();
        if (plugin.checkLastLine(meta, "team1selector")){
            if (map.board().prefersTeam1(player))
                player.sendMessage(ChatColor.GREEN + "Set preference for team 1");
            else player.sendMessage(ChatColor.RED + "Team is currently full");
        }else if (plugin.checkLastLine(meta,"team2selector")){
            if (map.board().prefersTeam2(player)) player.sendMessage(ChatColor.GREEN + "Set preference for team 2");
            else player.sendMessage(ChatColor.RED + "Team is currently full");

        }
    }

    @EventHandler (ignoreCancelled = false)
    public void syncSheepHealth(EntityDamageEvent event){

        EntityDamageEvent.DamageCause cause = event.getCause();
        if (cause == EntityDamageEvent.DamageCause.ENTITY_ATTACK || cause == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION
        || cause == EntityDamageEvent.DamageCause.PROJECTILE) return;


        if (event.getEntity() instanceof Sheep){
            Sheep sheep = (Sheep) event.getEntity();
            if (!sheep.hasMetadata("mapId")) return;
            String id = sheep.getMetadata("mapId").get(0).asString().toLowerCase();
            if (!plugin.active.containsKey(id)) return;
            GameMap map = plugin.active.get(id);
            SheepCore core = null;
            if (map.core(true).sheep().equals(sheep)){
                core = map.core(true);
            }
            if (map.core(false).sheep().equals(sheep)) core = map.core(false);
            core.setHealth(core.health() - event.getFinalDamage());
            event.setDamage(0);
            core.syncHealth();
        }
    }

    @EventHandler (priority = EventPriority.HIGH)
    public void stopFriendlyFire(EntityDamageByEntityEvent event){
        if (event.getEntity() instanceof Sheep){
            Sheep sheep = (Sheep) event.getEntity();
            if (!sheep.hasMetadata("mapId")) return;
            String id = sheep.getMetadata("mapId").get(0).asString().toLowerCase();
            if (!plugin.active.containsKey(id)) return;
            GameMap map = plugin.active.get(id);
            Player player = null;
            if (event.getDamager() instanceof Player)
                player = (Player) event.getDamager();
            else if (event.getDamager() instanceof Projectile) {
                Projectile proj = (Projectile) event.getDamager();
                if (proj.getShooter() instanceof Player) player = (Player) proj.getShooter();
            }
            if (player != null) {
                if (map.board().isTeam1(player) && sheep.equals(map.core(true).sheep())) event.setCancelled(true);
                if (map.board().isTeam2(player) && sheep.equals(map.core(false).sheep())) event.setCancelled(true);
            }

            if (event.isCancelled()) return;
            SheepCore core = null;
            if (map.core(true).sheep().equals(sheep)){
                core = map.core(true);
            }
            if (map.core(false).sheep().equals(sheep)) core = map.core(false);
            core.setHealth(core.health() - event.getFinalDamage());
            event.setDamage(0);
            core.syncHealth();
        }
    }

    @EventHandler
    public void stopOutsidePlacing(BlockPlaceEvent event){
        Player player = event.getPlayer();
        GameMap map = gameIn(player);
        if (map == null) return;
        if (!map.started()) event.setCancelled(true);
        if (!map.isInMap(event.getBlock())) event.setCancelled(true);
    }

    @EventHandler
    public void stopOutsideBreaking(BlockBreakEvent event){
        Player player = event.getPlayer();
        GameMap map = gameIn(player);
        if (map == null) return;
        if (!map.started()) event.setCancelled(true);
        if (!map.isInMap(event.getBlock())) event.setCancelled(true);
    }


    @EventHandler
    public void stopLeaveBounds(PlayerMoveEvent event){
        Location fLoc = event.getFrom();
        Location tLoc = event.getTo();
        if (fLoc.getBlockX() == tLoc.getBlockX() && fLoc.getBlockY() == tLoc.getBlockY() && fLoc.getBlockZ() == tLoc.getBlockZ()) return;
        Player player = event.getPlayer();
        GameMap map = gameIn(player);
        if (map == null) return;
        if (map.started() && !map.isInMap(tLoc.getBlock())) {
            tLoc.setX(fLoc.getX());
            tLoc.setZ(fLoc.getZ());
        }
        //if (!map.isInMap(tLoc.getBlock())) event.setCancelled(true);
    }

    /*

    Handling premature quitting (World Changes, Quiting)

     */

    @EventHandler
    public void onLeave(PlayerQuitEvent event){
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        GameMap game = gameIn(player);
        if (game == null) return;
        game.removePlayer(player,true, true);
    }

    @EventHandler
    public void worldChange(PlayerChangedWorldEvent event){
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        GameMap game = gameIn(player);
        if (game == null || event.getPlayer().getWorld().equals(game.world())) return;
        game.removePlayer(player,true);
    }

    /**
     * Gets the GameMap the player is participating in, null if none
     * @param uuid
     * @return
     */
    private GameMap gameIn(UUID uuid){
        for (GameMap map : plugin.active.values()){
            if (map.players().contains(uuid)) return map;
        }
        return null;
    }

    /**
     * Might be a slightly faster version of the other method, gets the GameMap the player is in, null if none
     * @param player
     * @return
     */
    private GameMap gameIn(Player player){
        if (!player.hasMetadata("mapId")) return null;
        String id = player.getMetadata("mapId").get(0).asString();
        return plugin.active.getOrDefault(id,null);
    }

    @EventHandler
    public void rejoin(PlayerJoinEvent event){
        GameMap game = null;
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        for (GameMap map : plugin.active.values()){
            if (map.offlines().containsKey(uuid)){
                game = map;
                break;
            }
        }
        if (game == null) return;
        if (game.board().isTeam1(player))
            player.teleport(game.spawn1());
        if (game.board().isTeam2(player))
            player.teleport(game.spawn2());
        OfflineProfile profile = game.offlines().remove(uuid);
        profile.loadOntoPlayer(player);
        player.setMetadata("mapId",new FixedMetadataValue(plugin,game.id()));
        game.players().add(uuid);
        player.setScoreboard(game.board().board());

    }


}
