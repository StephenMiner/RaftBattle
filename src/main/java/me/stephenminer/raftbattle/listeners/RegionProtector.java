package me.stephenminer.raftbattle.listeners;

import me.stephenminer.raftbattle.RaftBattle;
import me.stephenminer.raftbattle.game.GameMap;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collection;
import java.util.List;

public class RegionProtector implements Listener {
    private final RaftBattle plugin;
    public RegionProtector(){
        this.plugin = JavaPlugin.getPlugin(RaftBattle.class);
    }
    @EventHandler
    public void handleBreaking(BlockBreakEvent event){
        Block block = event.getBlock();
        GameMap map = regionIn(block);
        if (map == null) return;
        Player player = event.getPlayer();
        if (!map.started()) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You cannot break blocks yet");
        }
        else{
            chainBlocks(block,map);
            BlockState state = block.getState();
            map.trySaveBlockState(state);
        }
    }

    @EventHandler
    public void handleDecay(BlockFadeEvent event){
        Block block = event.getBlock();
        Material mat = block.getType();
        if (mat == Material.FIRE) return;
        GameMap map = regionIn(block);
        if (map == null) return;
        if (map.started())
            event.setCancelled(true);
        else map.trySaveBlockState(block.getState());
    }

    @EventHandler
    public void handlePlacement(BlockPlaceEvent event){
        Block block = event.getBlockPlaced();
        GameMap map = regionIn(block);
        if (map == null) return;
        Player player = event.getPlayer();
        if (!map.started()){
            player.sendMessage(ChatColor.RED + "You cannot place blocks right now!");
            event.setCancelled(true);
        }else{
            popPlace(block, map);
            map.trySaveBlockState(event.getBlockReplacedState());

        }
    }

    @EventHandler
    public void handleFallingBlocks(EntitySpawnEvent event){
        if (event.getEntityType() != EntityType.FALLING_BLOCK) return;
        FallingBlock falling = (FallingBlock) event.getEntity();
        Block position = falling.getLocation().getBlock();
        GameMap map = regionIn(position);
        if (map == null) return;
        chainBlocks(position, map);
        new BukkitRunnable(){
            @Override
            public void run(){
                if (falling.isDead()) this.cancel();
                else if (falling.isOnGround()){
                    Block current = falling.getLocation().getBlock();
                    if (map.isInMap(current)) map.trySaveBlockState(current.getState());
                    this.cancel();
                    return;
                }
            }
        }.runTaskTimer(plugin,0,1);
        map.trySaveBlockState(position.getState());
    }

    @EventHandler
    public void handleBurning(BlockBurnEvent event){
        Block block = event.getBlock();
        GameMap map = regionIn(block);
        if (map == null) return;
        chainBlocks(block,map);
        map.trySaveBlockState(block.getState());
    }

    @EventHandler
    public void handleExplosions(EntityExplodeEvent event){
        List<Block> affected = event.blockList();
        for (int i = affected.size()-1; i >= 0; i--){
            Block block = affected.get(i);
            //technically O(n^2) but the math check is simple and there probably wont be that many active games at once
            GameMap map = regionIn(block);
            if (map == null){
                map = regionIn(event.getLocation().getBlock());
                if (map != null) affected.remove(i);
                continue;
            }
            if (!map.started()) affected.remove(i);
            else {
                chainBlocks(block, map);
                map.trySaveBlockState(block.getState());
            }
        }
    }

    @EventHandler
    public void handleWaterDamage(BlockFromToEvent event){
        Block to = event.getToBlock();
        GameMap map = regionIn(to);
        if (map == null) return;
        chainBlocks(to,map);
        map.trySaveBlockState(to.getState());
    }







    public void chainBlocksUp(Block block, GameMap map){
        Material mat = block.getType();
        if (popStack(mat)){
            Location loc = block.getLocation().clone();
            int y = block.getY();
            Material m = loc.getBlock().getType();
            while (y < 256 && popStack(mat)){
                if (m == mat){
                    map.trySaveBlockState(block.getState());
                }
                loc = loc.clone().add(0,1,0);
                m = loc.getBlock().getType();
                y++;
            }
        }else if(popDirUp(mat)){
            map.trySaveBlockState(block.getState());
        }
    }
    public void checkSides(Block block, GameMap map){
        BlockFace[] sides = new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};
        for (BlockFace side : sides) {
            Block b = block.getRelative(side);
            if (popSides(b.getType()) && map.isInMap(b)){
                map.trySaveBlockState(b.getState());
            }
        }
    }

    public void chainBlocksDown(Block block, GameMap map){
        Material mat = block.getType();
        if (popDown(mat)){
            Location loc = block.getLocation().clone();
            int y = block.getY();
            Material m = loc.getBlock().getType();
            while (y > 0 && popDown(mat)){
                if (m == mat){
                    map.trySaveBlockState(block.getState());
                }
                loc = loc.clone().add(0,-1,0);
                m = loc.getBlock().getType();
                y--;
            }
        }
    }

    private boolean popDirUp(Material mat){
        switch (mat){
            case WOOD_PLATE:
            case STONE_PLATE:
            case GOLD_PLATE:
            case IRON_PLATE:
            case STONE_BUTTON:
            case WOOD_BUTTON:
            case ACACIA_DOOR:
            case SPRUCE_DOOR:
            case DARK_OAK_DOOR:
            case BIRCH_DOOR:
            case WOOD_DOOR:
            case FLOWER_POT:
            case IRON_DOOR_BLOCK:
            case YELLOW_FLOWER:
            case RED_ROSE:
            case DOUBLE_PLANT:
            case CARROT:
            case POTATO:
            case RAILS:
            case ACTIVATOR_RAIL:
            case POWERED_RAIL:
            case DETECTOR_RAIL:
            case LEVER:
            case GRASS:
            case LONG_GRASS:
            case CARPET:
            case TORCH:
            case REDSTONE_TORCH_OFF:
            case REDSTONE_TORCH_ON:
            case REDSTONE_WIRE:
            case REDSTONE_COMPARATOR:
            case REDSTONE_COMPARATOR_OFF:
            case REDSTONE_COMPARATOR_ON:
            case DIODE_BLOCK_OFF:
            case DIODE_BLOCK_ON:
            case BROWN_MUSHROOM:
            case RED_MUSHROOM:
            case CACTUS:
            case SUGAR_CANE_BLOCK:
                return true;
            default:
                return false;
        }
    }

    private boolean popSides(Material mat){
        switch (mat){
            case STONE_BUTTON:
            case WOOD_BUTTON:
            case LEVER:
            case WALL_BANNER:
            case WALL_SIGN:
            case TORCH:
            case COCOA:
            case TRIPWIRE_HOOK:
                return true;
            default: return false;
        }
    }

    private boolean popDown(Material mat){
        return mat == Material.VINE;
    }

    private boolean popStack(Material mat){
        switch (mat){
            case CARPET:
            case FLOWER_POT:
            case CACTUS:
            case SUGAR_CANE_BLOCK:
                return true;
            default:
                return false;
        }
    }

    public void popPlace(Block block, GameMap map){
        BlockFace[] sides = new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};
        for (BlockFace face : sides){
            Block b = block.getRelative(face);
            if (block.getType() != Material.AIR && b.getType() == Material.CACTUS){
                if (map.isInMap(block)){
                    map.trySaveBlockState(block.getState());
                }
            }
        }
    }


    public void chainBlocks(Block block, GameMap map){
        chainBlocksDown(block.getRelative(BlockFace.DOWN), map);
        chainBlocksUp(block.getRelative(BlockFace.UP), map);
        checkSides(block, map);
    }


    private GameMap regionIn(Block block){
        Collection<GameMap> active = plugin.active.values();
        for (GameMap map : active){
            if (map.isInMap(block)) return map;
        }
        return null;
    }
}
