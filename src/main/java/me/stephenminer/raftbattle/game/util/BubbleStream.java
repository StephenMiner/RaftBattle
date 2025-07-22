package me.stephenminer.raftbattle.game.util;

import me.stephenminer.raftbattle.RaftBattle;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

public class BubbleStream {
    private final RaftBattle plugin;
    private final float minX, minZ, maxX, maxZ;
    private final float y; //just to spawn visuals
    private final World world;
    private final long birthday;


    public BubbleStream(World world, Location pos1, Location pos2){
        this(world, Math.min(pos1.getBlockX(), pos2.getBlockX()), Math.min(pos1.getBlockZ(),
                pos2.getBlockZ()),Math.max(pos1.getBlockX(), pos2.getBlockX()),  Math.max(pos1.getBlockZ(), pos2.getBlockZ()),
                Math.min(pos1.getBlockY(), pos2.getBlockY()));
    }

    public BubbleStream(World world, int centerX, int centerZ, int radius, int yLevel){
        this(world, centerX - radius, centerZ - radius, centerX + radius, centerZ + radius, yLevel);
    }

    public BubbleStream(World world, float minX, float minZ, float maxX, float maxZ, float y){
        this.world = world;
        this.plugin = JavaPlugin.getPlugin(RaftBattle.class);
        this.minX = (int) minX + 0.5f;
        this.minZ = (int) minZ + 0.5f;
        this.maxX = (int) maxX + 0.5f;
        this.maxZ = (int) maxZ + 0.5f;
        this.y = y;
        birthday = System.currentTimeMillis();
    }



    public boolean positionInStream(Location loc){
       int mx = loc.getBlockX() + 1;
       int mz = loc.getBlockZ() +1;
       return overlaps(loc.getBlockX(),loc.getBlockZ(), mx,mz);
    }

    public boolean overlaps(double minX, double minZ, double maxX, double maxZ){
        return this.minX < maxX && this.maxX > minX && this.minZ < maxZ && this.maxZ > minZ;
    }


    public long birthday(){ return birthday; }

    public float y(){ return y; }

    public Location center(){
        int mx = ((int) minX + (int) maxX) / 2;
        int mz = ((int) minZ + (int) maxZ) / 2;
        return new Location(world,mx,y,mz);
    }






}
