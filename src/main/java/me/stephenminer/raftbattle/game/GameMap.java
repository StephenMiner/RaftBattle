package me.stephenminer.raftbattle.game;

import me.stephenminer.raftbattle.RaftBattle;
import me.stephenminer.raftbattle.game.fishing.FishHelper;
import me.stephenminer.raftbattle.game.util.BoundingBox;
import me.stephenminer.raftbattle.game.util.Items;
import me.stephenminer.raftbattle.game.util.OfflineProfile;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Team;

import java.util.*;
import java.util.stream.Collectors;

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

    private SheepCore sheep1,sheep2;
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

    /**
     * Starts the game, spawning the sheep, starting the scoreboard, and outfitting the players
     */
    public void start(){
        if (started) return;
        sheep1 = new SheepCore(spawn1,10, id);
        sheep2 = new SheepCore(spawn2, 10, id);
        started = true;
        board.updateBoard();
        fishHelper = new FishHelper(this);
        board.fillTeams();
        for (OfflinePlayer offline : board.team1().getPlayers()){
            if (!offline.isOnline()) continue;
            Player player = offline.getPlayer();
            player.teleport(spawn1);
            player.getInventory().clear();
            player.setHealth(20);
            player.setFoodLevel(20);
            player.setSaturation(1);
            player.setScoreboard(board.board());
            player.setFallDistance(0);
            outfitPlayer(player);
        }
        sheep2.startTracking(this, board.team1().getPlayers().stream().map(OfflinePlayer::getUniqueId).collect(Collectors.toSet()));
        for (OfflinePlayer offline : board.team2().getPlayers()){
            if (!offline.isOnline()) continue;
            Player player = offline.getPlayer();
            player.getInventory().clear();
            player.setHealth(20);
            player.setFoodLevel(20);
            player.setSaturation(1);
            player.teleport(spawn2);
            player.setScoreboard(board.board());
            player.setFallDistance(0);
            outfitPlayer(player);
        }
        sheep1.startTracking(this, board.team2().getPlayers().stream().map(OfflinePlayer::getUniqueId).collect(Collectors.toSet()));
        runWaterCheck();
    }

    /**
     * Ends the game, resets the map, and removes players
     */
    public void end(){
        for (BlockState state : savedStates.values())
            state.update(true);
        Player[] online = new Player[players.size()];
        int index = 0;
        for (UUID uuid : players){
            online[index] = Bukkit.getPlayer(uuid);
            index++;
        }
        for (Entity entity : world().getEntities()){
            if (entity instanceof Player) continue;
            Location loc = entity.getLocation();
            if (bounds.contains(loc.getX(),loc.getY(),loc.getZ()))
                entity.remove();
        }
        for (Player p : online){
            removePlayer(p,true);
        }
        started = false;
        players.clear();
        offlines.clear();
        savedStates.clear();
        plugin.active.remove(id);
    }

    /**
     * Checks if the game should start or not. If it should, a timer will begin counting down until starting for realsies
     */
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
                    broadcastTitle("Start in " + (delay-count) / 20,"Seconds");
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
                    this.cancel();
                    start();
                    return;
                }
                count++;
            }
        }.runTaskTimer(plugin,1,1);
    }

    /**
     * Checks if the game should end or not. If it should, a counter will be started where the game will end when it is done
     */
    public void checkEnd(){
        if (!started || ending) return;
        if (players.isEmpty()) {
            ending = true;
            end();
            return;
        }
        int alive1 = board.alive(board.team1());
        boolean team1win = false;
        if (board.team1().getSize() == 0 || (alive1 == 0 && sheep1.isDead()))
            ending = true;
        int alive2 = board.alive(board.team2());
        if (board.team2().getSize() == 0 || (alive2 == 0 && sheep2.isDead())) {
            ending = true;
            team1win = true;
        }
        if (!ending) return;
        broadcastMsg("--------------------");
        broadcastMsg(team1win ? plugin.teamName(true) + " has won the game" : plugin.teamName(false) + " has won the game");
        Set<OfflinePlayer> oplayers = team1win ? board.team1().getPlayers() : board.team2().getPlayers();
        broadcastMsg("Winning Members: ");
        for (OfflinePlayer offline : oplayers) {
            broadcastMsg("- " + offline.getName());
        }
        broadcastMsg("--------------------");
        broadcastSound(Sound.FIREWORK_LAUNCH,2,1);
        for (UUID uuid : players){
            Player player = Bukkit.getPlayer(uuid);
            player.getInventory().clear();
        }
        Bukkit.getScheduler().runTaskLater(plugin, this::end, 100);
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
        if (isFull() || started) {
            player.sendMessage(started ? ChatColor.RED + "Game is already started" : ChatColor.RED + "Game is full");
            return false;
        }
        UUID uuid = player.getUniqueId();
        player.setGameMode(GameMode.SURVIVAL);
        clearPlayer(player);
        player.teleport(waiting);
        players.add(uuid);
        Items items = new Items();
        player.getInventory().addItem(items.team1Selector(),items.team2Selector());
        player.setMetadata("mapId",new FixedMetadataValue(plugin,id));
        broadcastMsg(ChatColor.GOLD + player.getName() + " has joined (" + players.size() + "/" + plugin.readMaxPlayers() + ")");
        if (!starting) checkStart();
        return true;
    }

    /**
     * Removes the player from this game
     * @param player
     * @param teleport if true, player will be teleported to the reroute location
     * @param reconnect if true, player's data will be saved and they will be allowed to reconnect
     */
    public void removePlayer(Player player, boolean teleport, boolean reconnect){
        if (plugin.reroute == null) {
            player.sendMessage(ChatColor.RED + "No reroute location set!");
            return;
        }
        players.remove(player.getUniqueId());

        player.setGameMode(GameMode.SURVIVAL);
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        if (teleport)
            player.teleport(plugin.reroute);
        board.clearPreferences(player.getUniqueId());
        if (started && reconnect) {
            Team team = board.isTeam1(player) ? board.team1() : board.team2();
            OfflineProfile offline = new OfflineProfile(player.getUniqueId(),player.getHealth(),player.getFoodLevel(),player.getSaturation(),player.getInventory().getContents(), team);
            offlines.put(player.getUniqueId(),offline);
            broadcastMsg(ChatColor.GOLD + player.getName() + " has left, but can reconnect!");
        }else broadcastMsg(ChatColor.GOLD + player.getName() + " has quit!");
        board.team1().removePlayer(player);
        board.team2().removePlayer(player);
        checkEnd();
        player.removeMetadata("mapId",plugin);

        clearPlayer(player);
    }

    public void removePlayer(Player player, boolean teleport){
        removePlayer(player,teleport,false);
    }

    /**
     * Begins the respawning process. Items are cleared and health is reset.
     * @param player
     * @return true if the player can be respawned (is on a team)
     */
    public boolean respawnPlayer(Player player){
        player.setGameMode(GameMode.SPECTATOR);
        player.getActivePotionEffects().clear();
        ItemStack[] content = player.getInventory().getContents();
        World world = player.getWorld();
        for (ItemStack item : content){
            if (item == null) continue;
            Material type = item.getType();
            if (type == Material.COMPASS || type == Material.FISHING_ROD) continue;
            world.dropItemNaturally(player.getLocation(), item);
        }
        player.setHealth(20);
        player.setFoodLevel(20);
        player.setSaturation(1);
        checkEnd();
        clearPlayer(player);
        if (board.isTeam2(player) && !sheep2.isDead()){
            respawnTimer(player,false);
            return true;
        }
        if (board.isTeam1(player) && !sheep1.isDead()){
            respawnTimer(player,true);
            return true;
        }
        return false;
    }

    /**
     * A timer that when finished will finish respawning the player, putting them in survival at their spawn
     * @param player
     * @param team1
     */
    private void respawnTimer(Player player, boolean team1){
        Location spawn = team1 ? spawn1 : spawn2;
        new BukkitRunnable(){
            int count = 0;
            int target = plugin.respawnTime();
            @Override
            public void run(){
                if (!started){
                    this.cancel();
                    return;
                }
                if (count % 20 == 0){
                    player.sendTitle("Respawning in" , ((target - count) / 20) +" seconds");
                    player.sendMessage(ChatColor.GREEN + "Respawning in " + ((target-count) / 20) + " seconds");
                }
                if (count >= target){
                    player.teleport(spawn);
                    outfitPlayer(player);
                    player.setGameMode(GameMode.SURVIVAL);
                    this.cancel();
                    return;
                }
                count++;
            }
        }.runTaskTimer(plugin,1,1);
    }

    /**
     * A constant checker to see if the player is in water and needs to take damage
     */
    public void runWaterCheck(){
        new BukkitRunnable(){
            @Override
            public void run(){
                if (!started){
                    this.cancel();
                    return;
                }
                for (UUID uuid : players){
                    Player player = Bukkit.getPlayer(uuid);
                    if (player==null || player.getGameMode() == GameMode.SPECTATOR) continue;
                    Material mat = player.getLocation().getBlock().getType();
                    if (mat == Material.WATER || mat == Material.STATIONARY_WATER){
                       if (player.getHealth() - 1 <= 0){
                           boolean respawned = respawnPlayer(player);
                           if (respawned) player.sendMessage(ChatColor.GREEN + "You will respawn shortly");
                           else player.sendMessage(ChatColor.RED + "Your team's sheep is dead and you cannot respawn!");
                           broadcastMsg(player.getName() + " melted in the water");
                       }
                       player.setHealth(player.getHealth()-1);
                       player.damage(0);
                    }
                }
            }
        }.runTaskTimer(plugin,1,20);
    }


    /*
    Helper Methods
     */

    /**
     * Clears the player inventory and potion effects
     * @param player
     */
    private void clearPlayer(Player player){
        player.getActivePotionEffects().clear();
        player.getInventory().clear();
        player.getEquipment().clear();
        player.getInventory().setArmorContents(null);
    }

    /**
     * Equips the player
     * @param player
     */
    public void outfitPlayer(Player player){
        ItemStack rod = new ItemStack(Material.FISHING_ROD);
        ItemMeta meta = rod.getItemMeta();
        meta.addEnchant(Enchantment.LURE,5,true);
        meta.spigot().setUnbreakable(true);
        rod.setItemMeta(meta);
        player.getInventory().addItem(rod);
        player.getInventory().addItem(sheep1.compass);

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
     *
     * @return whether map is full, if players.size() >= max players
     */
    public boolean isFull(){
        return players.size() >= plugin.readMaxPlayers();
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

    public SheepCore core(boolean core1){ return core1 ? sheep1 : sheep2; }

    public String id(){ return id;}
    public String name(){ return name; }

    public Set<UUID> players(){ return players; }
    public HashMap<UUID, OfflineProfile> offlines(){ return offlines; }
    public GameBoard board(){ return board; }

    public HashMap<Location,BlockState> savedStates(){ return savedStates; }









}
