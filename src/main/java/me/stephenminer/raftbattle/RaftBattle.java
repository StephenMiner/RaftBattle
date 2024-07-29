package me.stephenminer.raftbattle;

import me.stephenminer.raftbattle.game.GameMap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;

public final class RaftBattle extends JavaPlugin {
    //The place players will get teleported to when their game ends
    public Location reroute;

    public ConfigFile settings;

    public HashMap<String, GameMap> active;

    @Override
    public void onEnable() {
        // Plugin startup logic
        this.settings = new ConfigFile(this,"settings");

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }


    /**
     *  Formats a location into a comma separated string
     * @param loc location to turn into a string
     * @return String formated as "world,x,y,z,yaw,pitch"
     */
    public String fromLoc(Location loc){
        return loc.getWorld().getName() + "," + loc.getX() + "," + loc.getY() + "," + loc.getZ() + "," + loc.getYaw() + "," + loc.getPitch();
    }

    /**
     * formats a location into a comma separated string, only taking the block location values and not taking yaw or pitch
     * @param loc  location to turn into a string
     * @param center Modify stored values to be centered on the block (add 0.5 to each block location value) if true
     * @return String formatted as "world,x,y,z"
     */
    public String fromBLoc(Location loc, boolean center){
        double x = center ? loc.getBlockX() + 0.5 : loc.getBlockX();
        double y = center ? loc.getBlockY() + 0.5 : loc.getBlockY();
        double z = center ? loc.getBlockZ() + 0.5 : loc.getBlockZ();
        return loc.getWorld().getName() + "," + x + "," + y + "," + z;
    }
    /**
     * formats a location into a comma separated string, only taking the block location values, centering them, and not taking yaw or pitch
     * @param loc  location to turn into a string
     *
     * @return String formatted as "world,x,y,z"
     */
    public String fromBLoc(Location loc){
        return fromBLoc(loc, true);
    }

    /**
     * Reads a location from a String, should work with strings from "fromLoc()" & "fromBLoc()"
     * @param str the string to parse, formated as either world,x,y,z or world,x,y,z,yaw,pitch
     * @return Location from the given string, null if no data can be parsed
     */
    public Location fromString(String str){
        String[] unbox = str.split(",");
        String name = unbox[0];
        World world = Bukkit.getWorld(name);
        //Load the world if it is unloaded !! This will create a new world if none really existed !!
        if (world == null) world = new WorldCreator(name).createWorld();
        double x = Double.parseDouble(unbox[1]);
        double y = Double.parseDouble(unbox[2]);
        double z = Double.parseDouble(unbox[3]);
        if (unbox.length >= 6){
            float yaw = Float.parseFloat(unbox[4]);
            float pitch = Float.parseFloat(unbox[5]);
            return new Location(world,x,y,z,yaw,pitch);
        }else return new Location(world,x,y,z);
    }

    public int readMinPlayers(){
        return this.settings.getConfig().getInt("min-start");
    }

    public int readMaxPlayers(){
        return this.settings.getConfig().getInt("max-players");
    }

    public int readStartDelay(){
        return this.settings.getConfig().getInt("start-delay");
    }

    public Location readReroute(){
        String entry = this.settings.getConfig().getString("reroute");
        if (entry == null) return null;
        try{
            return fromString(entry);
        }catch (Exception ignored){
            this.getLogger().warning("Attempted to read a Location from " + entry + ", but this string is in an invalid format!");
            return null;
        }
    }


}
