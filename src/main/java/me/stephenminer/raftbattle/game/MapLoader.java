package me.stephenminer.raftbattle.game;

import me.stephenminer.raftbattle.RaftBattle;
import me.stephenminer.raftbattle.game.util.Pond;
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
        return map;
    }


}
