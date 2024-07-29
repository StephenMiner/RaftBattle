package me.stephenminer.raftbattle.game;

import me.stephenminer.raftbattle.RaftBattle;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class GameMap {
    private final RaftBattle plugin;
    private final Location pos1, pos2;
    private final String id;
    private final BoundingBox bounds;
    private final Set<UUID> players;
    private String name;
    private Location spawn1,spawn2,waiting;
    private boolean started, starting, ending;
    /**
     *
     * @param id Identifier for the map
     * @param name Name for the map (what is shown)
     * @param pos1 Location that is centered on a block (corner 1 of the bounding box)
     * @param pos2 Location that is centered on a block  (corner 2 of the bounding box)
     */
    public GameMap(String id, String name, Location pos1, Location pos2){
        this.plugin = JavaPlugin.getPlugin(RaftBattle.class);
        this.id = id;
        this.pos1 = pos1;
        this.pos2 = pos2;
        this.bounds = new BoundingBox(pos1.toVector(),pos2.toVector());
        players = new HashSet<>();
        this.name = name;
    }



    public void checkStart(){
        if (starting || started) return;
    }

    public void checkEnd(){

    }














    /**
     * Attempts to add the player to the game
     * @param player A player who is NOT already in a game, should be checked externally
     * @return true if player was added, false otherwise.
     * A player can no longer join once the game has started or it is full
     */
    public boolean addPlayer(Player player){
        if (players.size() > plugin.readMaxPlayers() || started) {
            player.sendMessage(started ? ChatColor.RED + "Game is already started" : ChatColor.RED + "Game is full");
            return false;
        }
        UUID uuid = player.getUniqueId();
        players.add(uuid);
        player.teleport(waiting);
        return true;
    }

    /**
     * Removes the player from this game
     * @param player
     * @param teleport if true, player will be teleported to the reroute location
     */
    public void removePlayer(Player player, boolean teleport){
        if (plugin.reroute == null) {
            player.sendMessage(ChatColor.RED + "No reroute location set!");
            return;
        }
        player.getInventory().clear();
        player.getActivePotionEffects().clear();
        player.setScoreboard(null);
        if (teleport)
            player.teleport(plugin.reroute);
    }

    /**
     * Set the spawn for team 3
     */
    public void setSpawn1(Location spawn1){ this.spawn1 = spawn1; }

    /**
     * Set the spawn for team 2
     */
    public void setSpawn2(Location spawn2){ this.spawn2 = spawn2; }

    /**
     * Set the waiting area for the map
     */
    public void setWaiting(Location waiting){ this.waiting = waiting; }


    public boolean started(){ return started; }
    public boolean starting(){ return starting; }
    public boolean ending(){ return ending; }

    public Location spawn1(){ return spawn1; }
    public Location spawn2(){ return spawn2; }
    public Location waiting(){ return waiting;}








}
