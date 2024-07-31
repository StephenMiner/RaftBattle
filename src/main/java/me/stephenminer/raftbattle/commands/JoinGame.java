package me.stephenminer.raftbattle.commands;

import me.stephenminer.raftbattle.RaftBattle;
import me.stephenminer.raftbattle.game.GameMap;
import me.stephenminer.raftbattle.game.MapLoader;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JoinGame implements CommandExecutor, TabCompleter {
    private final RaftBattle plugin;

    public JoinGame(){
        this.plugin = JavaPlugin.getPlugin(RaftBattle.class);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
        if (!(sender instanceof Player)){
            sender.sendMessage(ChatColor.RED + "You need to be a player to use this command!");
            return false;
        }
        Player player = (Player) sender;
        if (args.length < 1){
            //TODO: Random selection or open menu
        }
        String id = ChatColor.stripColor(args[0]).toLowerCase();
        if (!validId(id)){
            player.sendMessage(ChatColor.RED + "The id " + id + " doesn't exist");
            return false;
        }
        if (gameStarted(id)){
            player.sendMessage(ChatColor.RED + "You cannot join a game that has started");
            return false;
        }
        GameMap game = plugin.active.getOrDefault(id, null);
        if (game == null){
            game = new MapLoader(id).build();
            plugin.active.put(id, game);
        }
        game.addPlayer(player);
        player.sendMessage(ChatColor.GREEN + "Sending you to the game");
        return true;
    }



    private boolean validId(String id){
        id = id.toLowerCase();
        if (!plugin.maps.getConfig().contains("maps")) return false;
        Set<String> ids = plugin.maps.getConfig().getConfigurationSection("maps").getKeys(false);
        return ids.contains(id);
    }
    private boolean gameStarted(String id){
        id = id.toLowerCase();
        return plugin.active.containsKey(id) && plugin.active.get(id).started();
    }




    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args){
        int size = args.length;
        if (size == 1) return joinableIds(args[0]);
        return null;
    }

    private List<String> joinableIds(String match){
        if (!plugin.maps.getConfig().contains("maps")) return null;
        Set<String> ids = new HashSet<>(plugin.maps.getConfig().getConfigurationSection("maps").getKeys(false));
        Collection<GameMap> active = plugin.active.values();
        for (GameMap map : active) if (map.started()) ids.remove(map.id());
        return plugin.filter(ids, match);
    }






}
