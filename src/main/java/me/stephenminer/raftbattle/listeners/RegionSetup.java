package me.stephenminer.raftbattle.listeners;

import me.stephenminer.raftbattle.RaftBattle;

import me.stephenminer.raftbattle.commands.MapRedefine;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class RegionSetup implements Listener {
    private final RaftBattle plugin;
    private HashMap<UUID, Location> mCorner1,mCorner2;
    private Set<UUID> regionName, pondName;

    public RegionSetup(){
        this.plugin = JavaPlugin.getPlugin(RaftBattle.class);
        mCorner1 = new HashMap<>();
        mCorner2 = new HashMap<>();
        regionName = new HashSet<>();
        pondName = new HashSet<>();
    }


    @EventHandler
    public void defineCorners(PlayerInteractEvent event){
        if (!event.hasItem()) return;
        ItemStack item = event.getItem();
        ItemMeta meta = item.getItemMeta();
        if (item.getType() == Material.AIR) return;
        boolean raft = plugin.checkLastLine(meta, "raft-battle-map");
        boolean pond = plugin.checkLastLine(meta, "raft-battle-pond");
        if (!raft && !pond) return;
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        UUID uuid = player.getUniqueId();
        switch (event.getAction()){
            case LEFT_CLICK_AIR:
                mCorner1.put(uuid,player.getLocation());
                player.sendMessage(ChatColor.YELLOW + "Position 1 Set");
                break;
            case LEFT_CLICK_BLOCK:
                mCorner1.put(uuid, block.getLocation());
                player.sendMessage(ChatColor.YELLOW + "Position 1 Set");
                break;
            case RIGHT_CLICK_AIR:
                mCorner2.put(uuid,player.getLocation());
                player.sendMessage(ChatColor.YELLOW + "Position 2 Set");
                break;
            case RIGHT_CLICK_BLOCK:
                mCorner2.put(uuid, block.getLocation());
                player.sendMessage(ChatColor.YELLOW + "Position 2 Set");
                break;
        }
        event.setCancelled(true);
        if (mCorner1.containsKey(uuid) && mCorner2.containsKey(uuid) && player.hasPermission("raftbattle.region.create")){
            if (MapRedefine.redefining.containsKey(uuid)){
                String mapId = MapRedefine.redefining.remove(uuid);
                Location pos1 = mCorner1.remove(uuid);
                Location pos2 = mCorner2.remove(uuid);
                saveMap(mapId, pos1, pos2);
                player.sendMessage(ChatColor.GREEN + "Redefined bounds for map " + mapId + "!");
                return;
            }
            player.sendMessage(ChatColor.GREEN + "Please type out the name of your region in chat!");
            if (raft)
                regionName.add(uuid);
            else if (pond)
                pondName.add(uuid);
            return;
        }
    }



    @EventHandler
    public void nameRegion(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        boolean raft = regionName.contains(uuid);
        boolean pond = pondName.contains(uuid);
        if (!raft && !pond) return;
        event.setCancelled(true);
        //erm what the sigma...
        int space = event.getMessage().indexOf(' ');
        String id = ChatColor.stripColor(event.getMessage().toLowerCase()).trim();
        if (space != -1) id = id.substring(space);
        if (idTaken(id, raft)) {
            player.sendMessage(ChatColor.RED + id + " is already taken, please choose a different id!");
            return;
        }
        pondName.remove(uuid);
        regionName.remove(uuid);
        Location loc1 = mCorner1.remove(uuid);
        Location loc2 = mCorner2.remove(uuid);
        if (raft) {
            saveMap(id, loc1, loc2);
            player.sendMessage(ChatColor.GREEN + "Created a new map region named " + id);
        } else{
            savePond(id, loc1, loc2);
            player.sendMessage(ChatColor.GREEN + "Created a new pond region named " + id);
        }

    }

    private boolean idTaken(String id, boolean raft){
        if (raft)
            return plugin.maps.getConfig().contains("maps." + id);
        else return plugin.ponds.getConfig().contains("ponds." + id);
    }

    private void saveMap(String id, Location loc1, Location loc2){
        loc1 = loc1.getBlock().getLocation();
        loc2 = loc2.getBlock().getLocation();
        String locData = plugin.fromBLoc(loc1) + "/" + plugin.fromBLoc(loc2);
        plugin.maps.getConfig().set("maps." + id + ".bounds", locData);
        plugin.maps.saveConfig();
    }

    private void savePond(String id, Location loc1, Location loc2){
        loc1 = loc1.getBlock().getLocation();
        loc2 = loc2.getBlock().getLocation();
        String locData = plugin.fromBLoc(loc1) + "/" + plugin.fromBLoc(loc2);
        plugin.ponds.getConfig().set("ponds." + id + ".bounds", locData);
        plugin.ponds.saveConfig();
    }
}
