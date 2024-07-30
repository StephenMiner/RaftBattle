package me.stephenminer.raftbattle.game;

import org.bukkit.block.Block;
import org.bukkit.util.Vector;

/**
 * Used for deciding whether something is inside of something else!
 */
public class BoundingBox {
    private double minx, miny, minz;
    private double maxx,maxy,maxz;


    public BoundingBox(double minx, double miny, double minz, double maxx, double maxy, double maxz){
        this.minx = minx;
        this.maxx = maxx;
        this.miny = miny;
        this.maxy = maxy;
        this.minz = minz;
        this.maxz = maxz;
    }

    public BoundingBox(Block block){
        this(block.getX(), block.getY(), block.getZ(),block.getX() + 1, block.getY() + 1, block.getZ() + 1);
    }

    public BoundingBox(Vector corner1, Vector corner2){
        this(Math.min(corner1.getX(),corner2.getX()), Math.min(corner1.getY(),corner2.getY()), Math.min(corner1.getZ(),corner2.getZ()),
                Math.max(corner1.getX(),corner2.getX()), Math.max(corner1.getY(),corner2.getY()), Math.max(corner1.getZ(),corner2.getZ()));
    }


    public boolean overlaps(double minx, double miny, double minz, double maxx, double maxy, double maxz){
        return this.minx < maxx && this.maxx > minx && this.miny < maxy && this.maxy > miny && this.minz < maxz && this.maxz > minz;
    }

    public boolean overlaps(Block block){
        int mx = block.getX() + 1;
        int my = block.getY() + 1;
        int mz = block.getZ() + 1;
        return this.overlaps(block.getX(),block.getY(), block.getZ(), mx,my,mz );
    }


    /**
     * Check whether a point is within the bounding box
     * @param x x coord
     * @param y y coord
     * @param z z coord
     * @return true if the given (x,y,z) point is contained within the bounding box.
     * Note that bounding boxes are rightwards exclusionary, but leftwards inclusionary. (A point on the rightmost edge is not considered to be in the box)
     */
    public boolean contains(double x, double y, double z){
        return x >= minx && x < maxx && y >= miny && y < maxy && z >= minz && z < maxz;
    }



    public double minX(){ return minx; }
    public double minY(){ return miny; }
    public double minZ(){ return minz; }

    public double maxX(){ return maxx; }
    public double maxY(){ return maxy; }
    public double maxZ(){ return maxz; }
}
