package me.stephenminer.raftbattle.game;

import me.stephenminer.raftbattle.RaftBattle;
import me.stephenminer.raftbattle.game.fishing.FishHelper;
import me.stephenminer.raftbattle.game.util.BoundingBox;
import me.stephenminer.raftbattle.game.util.OfflineProfile;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class GameMap {
    private final RaftBattle plugin;
    private final Location pos1, pos2;
    private final String id;
    private final BoundingBox bounds;
    private final Set<UUID> players;
    private final HashMap<Location, BlockState> savedStates;
    private final HashMap<UUID, OfflineProfile> offlines;

    private GameBoard board;
    private String name;
    private Location spawn1,spawn2,waiting;
    private FishHelper fishHelper;
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
        this.savedStates = new HashMap<>();
        players = new HashSet<>();
        offlines = new HashMap<>();
        this.name = name;
        board = new GameBoard(this);
    }


    public void start(){
        started = true;
        fishHelper = new FishHelper(this);
        board.fillTeams();
        for (OfflinePlayer offline : board.team1().getPlayers()){
            if (!offline.isOnline()) continue;
            Player player = offline.getPlayer();
            player.teleport(spawn1);
        }
        for (OfflinePlayer offline : board.team2().getPlayers()){
            if (!offline.isOnline()) continue;
            Player player = offline.getPlayer();
            player.teleport(spawn2);
        }
    }

    public void end(){
    }


    public void checkStart(){
        //Game is already starting
        if (starting || started) return;
        //Not enough players
        if (players.size() < plugin.readMinPlayers()) return;
        final int delay = plugin.readStartDelay();
        new BukkitRunnable(){
            int count = 0;
            @Override
            public void run(){
                if (count % 20 == 0) {
                    broadcastTitle("" + (delay-count) / 20,"");
                    broadcastSound(Sound.CAT_MEOW,1,1);
                }
                //Someone left
                if (players.size() < plugin.readMinPlayers()) {
                    starting = false;
                    broadcastMsg(ChatColor.RED + "Start interrupted: Not Enough players");
                    broadcastSound(Sound.CREEPER_DEATH,1,3);
                    this.cancel();
                    return;
                }
                //Game start (I dont know why, but I've always done a >= check instead of ==, I'm just paranoid like that )
                if (count >= delay){
                    start();
                    this.cancel();
                    return;
                }
                count++;
            }
        }.runTaskTimer(plugin,1,1);
    }

    public void checkEnd(){

    }









    /*

    Logic Methods

     */

    /**
     * Checks if the input block is within the map region
     * @param block
     * @return true if it is
     */
    public boolean isInMap(Block block){
        return bounds.overlaps(block);
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
        if (!starting) checkStart();
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
        player.getActivePotionEffects().clear();
        player.setScoreboard(null);
        if (teleport)
            player.teleport(plugin.reroute);
        board.clearPreferences(player.getUniqueId());
        if (started) {
            OfflineProfile offline = new OfflineProfile(player.getUniqueId(),player.getHealth(),player.getFoodLevel(),player.getSaturation(),player.getInventory().getContents());
            offlines.put(player.getUniqueId(),offline);
            checkEnd();
        }
        player.getInventory().clear();
    }


    public void broadcastMsg(String msg){
        for (UUID uuid : players){
            OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
            if (offline.isOnline()){
                Player player = offline.getPlayer();
                player.sendMessage(msg);
            }
        }
    }

    public void broadcastTitle(String title, String sub){
        for (UUID uuid : players){
            OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
            if (offline.isOnline()){
                Player player = offline.getPlayer();
                //May or may not work since method is deprecated
                player.sendTitle(title,sub);
            }
        }
    }

    public void broadcastSound(Sound sound, float volume, float pitch){
        for (UUID uuid : players){
            OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
            if (offline.isOnline()){
                Player player = offline.getPlayer();
                player.playSound(player.getLocation(),sound,volume, pitch);
            }
        }
    }

    /**
     * Attempts to save the input block state to the savedStates HashMap
     * It will only add the BlockState if the Map doesn't already contain the BlockState's Location as a key
     * @param state
     */
    public void trySaveBlockState(BlockState state){
        Location loc = state.getLocation();
        if (savedStates.containsKey(loc)) return;
        savedStates.put(loc,state);
    }


    /*

    Setters and Getters

     */
    public void setName(String name){
        this.name = name;
    }

    /**
     * Set the spawn for team 1
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
    public World world(){ return spawn1.getWorld(); }
    public FishHelper fishHelper(){ return fishHelper; }

    public String id(){ return id;}
    public String name(){ return name; }

    public Set<UUID> players(){ return players; }
    public HashMap<UUID, OfflineProfile> offlines(){ return offlines; }
    public GameBoard board(){ return board; }

    public HashMap<Location,BlockState> savedStates(){ return savedStates; }









}
