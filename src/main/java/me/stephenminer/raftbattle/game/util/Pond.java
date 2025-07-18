package me.stephenminer.raftbattle.game.util;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public class Pond {
    private final BoundingBox bounds;
    private final Location pos1, pos2;
    private final String id;

    public Pond(String id, Location pos1, Location pos2){
        this.id = id;
        this.pos1 = pos1.getBlock().getLocation().clone().add(0.5,0.5,0.5);
        this.pos2 = pos2.getBlock().getLocation().clone().add(0.5,0.5,0.5);
        this.bounds = new BoundingBox(pos1.toVector(), pos2.toVector());
    }


    public boolean isInPond(Location location){
        return bounds.overlaps(location.getBlock());
    }

    public String id(){ return id; }
}
