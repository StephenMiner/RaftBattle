package me.stephenminer.raftbattle.commands;

import me.stephenminer.raftbattle.RaftBattle;
import me.stephenminer.raftbattle.game.util.Items;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class MapRedefine implements CommandExecutor, TabCompleter {
    public static final Map<UUID, String> redefining = new HashMap<>();

    private final RaftBattle plugin;

    public MapRedefine(){
        this.plugin = JavaPlugin.getPlugin(RaftBattle.class);
    }



    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
        if (!sender.hasPermission("raftbattle.commands.redefine")){
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command!");
            return false;
        }
        int size = args.length;
        if (size < 1){
            sender.sendMessage(ChatColor.RED + "You need to specify the map region you wish to redefine the bounds of!");
            return false;
        }
        String mapId = args[0];
        if (!isMapId(mapId)){
            sender.sendMessage(ChatColor.RED + mapId + " is not an existing map id!");
            return false;
        }
        if (sender instanceof Player){
            Player player = (Player) sender;
            UUID uuid = player.getUniqueId();
            Items items = new Items();
            MapRedefine.redefining.put(uuid, mapId);
            player.getInventory().addItem(items.mapWand());
            player.sendMessage(ChatColor.GREEN + "Please use the map wand you have been provided to redefine the corners of the region " + mapId + "!");
            return true;
        }else sender.sendMessage(ChatColor.RED + "Only players can use this command!");

        return false;
    }

    private boolean isMapId(String mapId){
        return plugin.maps.getConfig().contains("maps." + mapId);
    }


    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args){
        int size = args.length;
        if (size == 1) return mapIds(args[0]);
        return null;
    }

    private List<String> mapIds(String match){
        Set<String> mapIds = plugin.maps.getConfig().getConfigurationSection("maps").getKeys(false);
        return plugin.filter(mapIds, match);
    }



}
