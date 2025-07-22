package me.stephenminer.raftbattle.game;

import me.stephenminer.raftbattle.RaftBattle;
import me.stephenminer.raftbattle.game.util.Pond;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Used to for loading GameMap objects from the maps.yml file
 */
public class MapLoader {
    private final RaftBattle plugin;
    private final String id;

    /**
     *
     * @param id map ID, will be forced to be lowercase
     */
    public MapLoader(String id){
        this.plugin = JavaPlugin.getPlugin(RaftBattle.class);
        this.id = id.toLowerCase();
    }




    public Location[] unboxBounds(){
        String boxed = plugin.maps.getConfig().getString("maps." + id + ".bounds");
        if (boxed == null) return null;
        String[] unboxed = boxed.split("/");
        Location[] locs = new Location[2];
        locs[0] = plugin.fromString(unboxed[0]);
        locs[1] = plugin.fromString(unboxed[1]);
        return locs;
    }
    public Location loadTeamSpawn(boolean team1){
        String path = team1 ? "maps." + id + ".spawn1" : "maps." + id + ".spawn2";
        return plugin.fromString(plugin.maps.getConfig().getString(path));
    }

    public Location loadWaiting(){
        String loaded = plugin.maps.getConfig().getString("maps." + id + ".waiting-area");
        return plugin.fromString(loaded);
    }

    public String loadName(){
        String name = plugin.maps.getConfig().getString("maps." + id + ".name");
        if (name == null) return id;
        else return name;
    }


    private String[] loadPondIds(){
        String entry = plugin.maps.getConfig().getString("maps." + id + ".ponds");
        if (entry == null || entry.isEmpty()) return null;
        else return entry.split(",");
    }


    private Pond fromId(String id){
        String locStrs = plugin.ponds.getConfig().getString("ponds." + id + ".bounds");
        String[] splitLocs = locStrs.split("/");
        Location loc1 = plugin.fromString(splitLocs[0]);
        Location loc2 = plugin.fromString(splitLocs[1]);
        return new Pond(id, loc1, loc2);
    }

    /**\
     *
     * @return array containing config options for bubble streams.
     * Array order as follows: {bubble spawn period, bubble spawn chance, bubble life spawn, max bubble streams, bubble radius}
     */
    private int[] loadBubbleStreamConfig(){
        String base = "maps." + id;
        int bubbleSpawnPeriod = 1650;
        if (plugin.maps.getConfig().contains(base + ".bubble-spawn-period"))
            bubbleSpawnPeriod = plugin.maps.getConfig().getInt(base + ".bubble-spawn-period");

        int bubbleSpawnChance = 30;
        if (plugin.maps.getConfig().contains(base + ".bubble-spawn-chance"))
            bubbleSpawnChance = plugin.maps.getConfig().getInt(base + ".bubble-spawn-chance");

        int bubbleLifeSpan = 2400;
        if (plugin.maps.getConfig().contains(base + ".bubble-life"))
            bubbleLifeSpan = plugin.maps.getConfig().getInt(base + ".bubble-life");

        int maxStreams = 5;
        if (plugin.maps.getConfig().contains(base + ".max-bubble-streams"))
            maxStreams = plugin.maps.getConfig().getInt(base + ".max-bubble-streams");

        int streamRadius = 3;
        if (plugin.maps.getConfig().contains(base + ".bubble-radius"))
            streamRadius = plugin.maps.getConfig().getInt(base + ".bubble-radius");

        return new int[]{bubbleSpawnPeriod, bubbleSpawnChance, bubbleLifeSpan, maxStreams, streamRadius};
    }

    public Pond[] loadPonds(){
        String[] pondIds = loadPondIds();
        if (pondIds == null || pondIds.length == 0)
            return new Pond[0]; //makes my life easier when working with the object in the GameMap class I think
        Pond[] ponds = new Pond[pondIds.length];
        for (int i = 0; i < ponds.length; i++){
            ponds[i] = fromId(pondIds[i]);
        }
        return ponds;
    }

    public GameMap build(){
        Location[] locPair = unboxBounds();
        if (locPair == null){
            plugin.getLogger().warning("Attempted to build a GameMap, but the config where the bounds for the map is location is wrong!");
            return null;
        }
        String name = loadName();
        Pond[] ponds = loadPonds();
        GameMap map = new GameMap(id, name, locPair[0], locPair[1], ponds);
        map.setSpawn1(loadTeamSpawn(true));
        map.setSpawn2(loadTeamSpawn(false));
        map.setWaiting(loadWaiting());
        int[] bubbleConfig = loadBubbleStreamConfig();
        map.setBubbleSpawnPeriod(bubbleConfig[0]);
        map.setBubbleSpawnChance(bubbleConfig[1]);
        map.setBubbleLifeSpan(bubbleConfig[2]);
        map.setMaxStreams(bubbleConfig[3]);
        map.setStreamRadius(bubbleConfig[4]);
        return map;
    }





}
