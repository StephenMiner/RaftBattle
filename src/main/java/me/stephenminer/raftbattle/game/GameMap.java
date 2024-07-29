package me.stephenminer.raftbattle.game;

import me.stephenminer.raftbattle.RaftBattle;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Team;

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


    /**
     * Attempts to add the player to the game
     * @param player
     * @return true if player was added, false otherwise.
     * A player can no longer join once the game has started or it is full
     */
    public boolean addPlayer(Player player){
       // if (players.size() > plugin.readMaxPlayers() || )
        return true;
    }

    public void removePlayer(Player player){

    }











}
