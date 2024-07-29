package me.stephenminer.raftbattle.game;

import me.stephenminer.raftbattle.RaftBattle;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

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
        return plugin.fromString(plugin.getConfig().getString(path));
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


    public GameMap build(){
        Location[] locPair = unboxBounds();
        if (locPair == null){
            plugin.getLogger().warning("Attempted to build a GameMap, but the config where the bounds for the map is location is wrong!");
            return null;
        }
        String name = loadName();
        GameMap map = new GameMap(id, name, locPair[0], locPair[1]);
        map.setSpawn1(loadTeamSpawn(true));
        map.setSpawn2(loadTeamSpawn(false));
        map.setWaiting(loadWaiting());
        return map;
    }


}
