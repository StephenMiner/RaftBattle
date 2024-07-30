package me.stephenminer.raftbattle.commands;

import me.stephenminer.raftbattle.RaftBattle;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class GameMapCmd implements CommandExecutor, TabCompleter {
    private final RaftBattle plugin;
    public GameMapCmd(){
        this.plugin = JavaPlugin.getPlugin(RaftBattle.class);
    }


    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
        if (!(sender instanceof Player)){
            sender.sendMessage(ChatColor.RED + "You need to be a player to use this command!");
            return false;
        }
        Player player = (Player) sender;
        int size = args.length;
        if (size >= 2){
            String id = args[0].toLowerCase();
            if (!idExists(id)){
                player.sendMessage(ChatColor.RED + id + " does not exist! Create the map first with a wand!");
                return false;
            }
            String sub = args[1].toLowerCase();
            switch (sub) {
                case "team1spawn":
                    setTeamSpawn(true, id, player.getLocation());
                    player.sendMessage(ChatColor.GREEN + "Set team 1 spawn");
                    return true;
                case "team2spawn":
                    setTeamSpawn(false, id, player.getLocation());
                    player.sendMessage(ChatColor.GREEN + "Set team 2 spawn");
                    return true;
                case "waitingarea":
                    setWaitingArea(id, player.getLocation());
                    player.sendMessage(ChatColor.GREEN + "Set waiting area");
                    return true;
                case "delete":
                    delete(id);
                    player.sendMessage(ChatColor.GREEN + "Deleted Map");
                    return true;
            }
            if (size >= 3){
                if (sub.equals("setname")){
                    StringBuilder builder = new StringBuilder();
                    for (int i = 2; i < size; i++){
                        builder.append(args[i]).append(' ');
                    }
                    //Delete the last space
                    builder.deleteCharAt(builder.length()-1);
                    setName(id, builder.toString());
                    return true;
                }
            }
        }
        player.sendMessage(ChatColor.RED + "Most likely, you got here because you didnt input a sub command or use enough arguments for the inputted sub command");
        return false;
    }


    private boolean idExists(String id){
        return plugin.maps.getConfig().contains("maps." + id);
    }
    private void setTeamSpawn(boolean team1, String id, Location loc){
        String path = team1 ?  "maps." + id + ".spawn1" : "maps." + id + ".spawn2";
        plugin.maps.getConfig().set(path, plugin.fromLoc(loc));
        plugin.maps.saveConfig();
    }
    private void setWaitingArea(String id, Location loc){
        plugin.maps.getConfig().set("maps." + id + ".waiting-area", plugin.fromLoc(loc));
        plugin.maps.saveConfig();;
    }
    private void setName(String id, String name){
        plugin.maps.getConfig().set("maps." + id + ".name", name);
    }

    private void delete(String id){
        plugin.maps.getConfig().set("maps." + id, null);
        plugin.maps.saveConfig();
    }
    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args){
        int size = args.length;
        if (size == 1) return ids(args[0]);
        if (size == 2) return subs(args[1]);
        return null;
    }


    private List<String> ids(String match){
        Set<String> ids = plugin.maps.getConfig().getConfigurationSection("maps").getKeys(false);
        return plugin.filter(ids, match);
    }

    private List<String> subs(String match){
        List<String> subs = new ArrayList<>();
        subs.add("team1spawn");
        subs.add("team2spawn");
        subs.add("waitingarea");
        subs.add("setname");
        subs.add("delete");
        return plugin.filter(subs, match);
    }
}
