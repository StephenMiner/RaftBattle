package me.stephenminer.raftbattle.game;

import me.stephenminer.raftbattle.RaftBattle;
import me.stephenminer.raftbattle.game.fishing.FishHelper;
import me.stephenminer.raftbattle.game.util.*;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.ContainerBlock;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Team;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class GameMap {
    private final RaftBattle plugin;
    private final Location pos1, pos2;
    private final String id;
    private final BoundingBox bounds;
    private final Set<UUID> players;
    private final HashMap<Location, BlockState> savedStates;
    private final HashMap<Location, ItemStack[]> savedContainers;
    private final HashMap<UUID, OfflineProfile> offlines;


    private final Pond[] ponds;

    private BubbleStream[] activeStreams;


    private GameBoard board;
    private String name;
    private Location spawn1,spawn2,waiting;
    private FishHelper fishHelper;
    private boolean started, starting, ending;

    private int maxStreams, streamCount;
    private int safeY;
    private int bubbleSpawnPeriod, bubbleSpawnChance, bubbleLifeSpan, streamRadius;
    private int decayRange, decayTicks, restrictPlaceRange;

    private SheepCore sheep1,sheep2;
    /**
     *
     * @param id Identifier for the map
     * @param name Name for the map (what is shown)
     * @param pos1 Location that is centered on a block (corner 1 of the bounding box)
     * @param pos2 Location that is centered on a block  (corner 2 of the bounding box)
     */
    public GameMap(String id, String name, Location pos1, Location pos2, Pond[] ponds){
        this.plugin = JavaPlugin.getPlugin(RaftBattle.class);
        this.id = id;
        this.pos1 = pos1;
        this.pos2 = pos2;
        this.bounds = new BoundingBox(pos1.toVector(),pos2.toVector());
        this.savedStates = new HashMap<>();
        this.savedContainers = new HashMap<>();
        players = new HashSet<>();
        offlines = new HashMap<>();
        this.name = name;
        board = new GameBoard(this);
        this.ponds = ponds;
        activeStreams = null;
        this.decayTicks = 60;
    }


    /**
     * Starts the game, spawning the sheep, starting the scoreboard, and outfitting the players
     */
    public void start(){
        if (started) return;
        sheep1 = new SheepCore(spawn1,300, id);
        sheep2 = new SheepCore(spawn2, 300, id);
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
        manageBubbleStreams();
    }

    /**
     * Ends the game, resets the map, and removes players
     */
    public void end(){
        ending = true;
        started = false;
        for (BlockState state : savedStates.values()) {
            Chunk chunk = state.getChunk();
            //Maybe this fixes the blockstates not loading properly?
            if (!chunk.isLoaded())
                chunk.load();
            state.update(true);
            state = state.getBlock().getState();
            if (state instanceof ContainerBlock && savedContainers.containsKey(state.getLocation())) {
                ContainerBlock container = (ContainerBlock) state;
                try {
                    System.out.println();
                    ((ContainerBlock) state).getInventory().setContents(savedContainers.get(state.getLocation()));
                    state.update(true);
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to update the state of the block " + state.toString() + " at loc " + plugin.fromBLoc(state.getLocation()));
                    plugin.getLogger().warning(container.getInventory().getSize() + "," + savedContainers.get(state.getLocation()).length);
                }
            }


        }
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
        starting = true;
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
            OfflineProfile offline = new OfflineProfile(player.getUniqueId(),player.getHealth(),player.getFoodLevel(),player.getSaturation(),player.getInventory().getContents(), player.getInventory().getArmorContents(),team);
            offlines.put(player.getUniqueId(),offline);
            broadcastMsg(ChatColor.GOLD + player.getName() + " has left, but can reconnect!");
        }else broadcastMsg(ChatColor.GOLD + player.getName() + " has quit!");
        board.team1().removePlayer(player);
        board.team2().removePlayer(player);
        checkEnd();
        player.removeMetadata("mapId",plugin);
        player.removeMetadata("raft-invulnerable", plugin);
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
            if (type == Material.AIR || type == Material.COMPASS || type == Material.FISHING_ROD) continue;
            world.dropItemNaturally(player.getLocation(), item);
        }
        ItemStack[] armor = player.getEquipment().getArmorContents();
        for (ItemStack item : armor){
            if (item == null || item.getType() == Material.AIR) continue;
            world.dropItemNaturally(player.getLocation(), item);
        }
        player.setHealth(20);
        player.setFoodLevel(20);
        player.setSaturation(1);
        player.setFireTicks(0);
        //clearPotEffects(player);
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


    private void clearPotEffects(Player player){
        for (PotionEffectType type : PotionEffectType.values()){
            player.removePotionEffect(type);
        }
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
                    player.removeMetadata("raft-invulnerable", plugin);
                    int seconds = (plugin.getInvincibilityTicks() / 20);
                    player.setMetadata("raft-invulnerable", new FixedMetadataValue(plugin, System.currentTimeMillis() + (seconds * 1000L)));
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

    public void manageBubbleStreams(){
        new BukkitRunnable(){
            int spawnTick = 0;
            int frame = 0;
            @Override
            public void run(){
                if (!started || ending){
                    this.cancel();
                    return;
                }
                if (frame > 32)
                    frame = 0;
                else frame++;
                for (BubbleStream stream : activeStreams){
                    if (stream != null) playAnimation(stream.center(), frame, streamRadius,1);
                }
                clearOldStreams();
                if (spawnTick > bubbleSpawnPeriod ){
                    spawnTick = 0;
                    if (streamCount >= maxStreams) return;
                    int roll = ThreadLocalRandom.current().nextInt();
                    if (roll < bubbleSpawnChance){
                        streamCount++;
                        int[] spawnPos = findStreamLocation(streamRadius, 0);
                        if (spawnPos.length == 0) return;
                        BubbleStream stream = new BubbleStream(world(),spawnPos[0], spawnPos[2], streamRadius, spawnPos[1]);
                       // System.out.println("created new stream");
                    //    System.out.println(spawnPos[0] + "," + spawnPos[1] + "," + spawnPos[2]);
                        for (int i = 0; i < activeStreams.length; i++)
                            if (activeStreams[i] == null) {
                                activeStreams[i] = stream;
                                break;
                            }
                    }
                }
                spawnTick++;
            }
        }.runTaskTimer(plugin,1,1);
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
        meta.addEnchant(Enchantment.LURE,  5,true);
        meta.spigot().setUnbreakable(true);
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.ITALIC + "There's something fishy here alright...");
        meta.setLore(lore);
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

    private int[] findStreamLocation(int streamRadius, int attempt){
        if (attempt >= 1000) return new int[0];
        int x = ThreadLocalRandom.current().nextInt((int) bounds.minX(), (int) bounds.maxX());
        int z = ThreadLocalRandom.current().nextInt((int) bounds.minZ(), (int) bounds.maxZ());
        int heightClearance = 2;
        final World world = world();
        int posY = 0; // Only changed when valid is true
        boolean valid = false;
        //First loop upwards starting at our safeY position (our lowest bounds if safeY is out of bounds)
        for (int y = Math.max((int) bounds.minY(), safeY); y < (int) bounds.maxY(); y++){
            valid = validArea(world,x,y,z,streamRadius, heightClearance);
            if (valid) {
                posY = y;
                break;
            }
        }
        //If a position wasn't found do the same thing but downwards
        if (!valid) {
            for (int y = Math.min((int) bounds.maxY(), safeY - 1); y >= bounds.minY(); y--) {
                valid = validArea(world,x,y,z,streamRadius,heightClearance);
                if (valid) {
                    posY = y;
                    break;
                }
            }
        }
        //If no position is found still, reroll and retry, else return the found position
       // System.out.println(attempt);
        if (!valid) return findStreamLocation(streamRadius, attempt + 1);
        else return new int[]{x, posY, z};
    }

    private boolean validArea(World world, int posX, int posY, int posZ, int radius, int heightClearance){
        Block origin = world.getBlockAt(posX,posY,posZ);
        //Hopefully serve as a way to have a faster check
        if (origin.getType() != Material.WATER && origin.getType() != Material.STATIONARY_WATER)
            return false;
        for (int x = posX - radius; x <= posX + radius; x ++){
            for (int z = posZ - radius; z <= posZ + radius; z++){
                Block first = world.getBlockAt(x,posY,z);
                if (first.getType() != Material.WATER && first.getType() != Material.STATIONARY_WATER) return false;
                boolean yPass = true;
                for (int y = posY+1; y <= posY + heightClearance; y++){
                    Block block = world.getBlockAt(x,y,z);
                    if (block.isLiquid() || block.getType().isSolid()) {
                        yPass = false;
                        break;
                    }
                }
                if (!yPass) return false;
            }

        }
        return true;
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
        //Change to BlockContainer if no worky
        if (state instanceof InventoryHolder) {
            ItemStack[] items = ((InventoryHolder) state).getInventory().getContents();
            int len = items.length;
            ItemStack[] copy = new ItemStack[len];
            for (int i = 0; i < len; i++){
                if (items[i] == null || items[i].getType() == Material.AIR) continue;
                ItemStack itemCopy = new ItemStack(items[i]);
                copy[i] = itemCopy;
            }
            savedContainers.put(loc, copy);
        }
        savedStates.put(loc,state);
    }


    private void clearOldStreams(){
        for (int i = 0; i < activeStreams.length; i++){
            if (activeStreams[i] == null) continue;
            BubbleStream stream = activeStreams[i];
            long aliveDuration = System.currentTimeMillis() - stream.birthday();
            if (aliveDuration >= bubbleLifeSpan) {
                activeStreams[i] = null;
                streamCount--;
            }

        }
    }


    private void playAnimation(Location pos, int frame, double radius, int circleHeight){
        World world = pos.getWorld();
        double var = frame * (Math.PI / 16);
        Location base = pos.clone();
        Location first = base.clone().add(radius * Math.cos(var), radius * Math.sin(var) + 1, radius * Math.sin(var));
        Location second = base.clone().add(radius * Math.cos(var + Math.PI), radius * Math.sin(var) + 1, radius * Math.sin(var + Math.PI));
        Effect effect = Effect.INSTANT_SPELL;
        Effect spiralEffect = Effect.WATERDRIP;
        world.playEffect(first,spiralEffect, 2);
        world.playEffect(second, spiralEffect, 2);
        if (frame % 2 == 0) {
            double y = (circleHeight * Math.sin(var) + 1);
            for (double theta = 0; theta <= 2 * Math.PI; theta += Math.PI / 20) {
                double x = radius * Math.cos(theta);
                double z = radius * Math.sin(theta);
                base.add(x, y, z);
                world.playEffect(base, effect, 1);
                base.subtract(x, y, z);
            }
        }
    }

    public boolean isInBubbleStream(Entity entity){
        for (BubbleStream stream : activeStreams){
            if (stream == null) continue;
            if (stream.positionInStream(entity.getLocation())) return true;
        }
        return false;
    }

    public boolean shouldStopBlockInteraction(Location loc){
        Block block = loc.getBlock();
        Location bLoc = block.getLocation();
        int y = bLoc.getBlockY();
        return isInMap(block) && (int) bounds.maxY() - restrictPlaceRange < y;
    }

    public boolean shouldDecay(Location loc){
        Block block = loc.getBlock();
        Location bLoc = block.getLocation();
        int y = bLoc.getBlockY();
        return isInMap(block) && (int) bounds.maxY() - restrictPlaceRange - decayRange <= y;
    }

    public boolean playDecayAnimation(Block block, int tick){
        World world = block.getWorld();
        if (tick >= decayTicks) {
            Material mat = block.getType();
            block.setType(Material.AIR);
            ItemStack item = new ItemStack(mat);
            world.dropItemNaturally(block.getLocation(),item);

            return true;
        }
        else if (tick >= decayTicks / 2 && tick % 10 == 0){
            world.playEffect(block.getLocation().clone().add(0,1,0), Effect.VILLAGER_THUNDERCLOUD, 1);
        }
        return false;
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

    /**
     * Sets the max number of bubble streams that can exist at once.
     * Also creates a new array to store bubble streams deleting an old one if it existed already
     * @param maxStreams
     */
    public void setMaxStreams(int maxStreams) {
        this.maxStreams = maxStreams;
        activeStreams = new BubbleStream[maxStreams];
    }

    public void setBubbleSpawnPeriod(int bubbleSpawnPeriod){ this.bubbleSpawnPeriod = bubbleSpawnPeriod; }
    public void setBubbleSpawnChance(int bubbleSpawnChance){ this.bubbleSpawnChance = bubbleSpawnChance; }

    /**
     * Note function converts the input ticks into milliseconds
     * @param bubbleLifeSpan
     */
    public void setBubbleLifeSpan(int bubbleLifeSpan){ this.bubbleLifeSpan = bubbleLifeSpan * 50; }
    public void setStreamRadius(int streamRadius){ this.streamRadius = streamRadius; }

    public void setDecayRange(int decayRange){ this.decayRange = decayRange; }
    public void setDecayTicks(int decayTicks){ this.decayTicks = decayTicks; }
    public void setRestrictPlaceRange(int restrictPlaceRange){ this.restrictPlaceRange = restrictPlaceRange; }


    public boolean started(){ return started; }
    public boolean starting(){ return starting; }
    public boolean ending(){ return ending; }

    public int maxStreams(){ return maxStreams; }

    public Location pos1(){ return pos1; }
    public Location pos2(){ return pos2; }
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

    public Pond[] ponds(){ return ponds; }

    public int decayRange(){ return decayRange; }
    public int decayTicks(){ return decayTicks; }

    public int restrictPlaceRange(){ return restrictPlaceRange; }








}
