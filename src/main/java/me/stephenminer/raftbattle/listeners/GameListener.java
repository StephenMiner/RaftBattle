package me.stephenminer.raftbattle.listeners;

import me.stephenminer.raftbattle.RaftBattle;
import me.stephenminer.raftbattle.game.GameMap;
import me.stephenminer.raftbattle.game.util.OfflineProfile;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import javax.swing.plaf.nimbus.State;
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
            GameMap map = gameIn(uuid);
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
                GameMap map = gameIn(player.getUniqueId());
                if (map == null) return;
                boolean respawned = map.respawnPlayer(player);
                if (respawned) player.sendMessage(ChatColor.GREEN + "You will respawn shortly");
                else player.sendMessage(ChatColor.RED + "Your team's sheep is dead and you cannot respawn");
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
                GameMap map = gameIn(player.getUniqueId());
                if (map == null) return;
                boolean respawned = map.respawnPlayer(player);
                if (respawned) player.sendMessage(ChatColor.GREEN + "You will respawn shortly");
                else player.sendMessage(ChatColor.RED + "Your team's sheep is dead and you cannot respawn!");
                map.broadcastMsg(generateDeathMessage(player,cause,null));

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



    /*

    Handling premature quitting (World Changes, Quiting)

     */

    @EventHandler
    public void onLeave(PlayerQuitEvent event){
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        GameMap game = gameIn(uuid);
        if (game == null) return;
        game.removePlayer(player,true);
    }

    @EventHandler
    public void worldChange(PlayerChangedWorldEvent event){
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        GameMap game = gameIn(uuid);
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

    }


}
