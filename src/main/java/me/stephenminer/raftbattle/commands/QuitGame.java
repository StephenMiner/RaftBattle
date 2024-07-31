package me.stephenminer.raftbattle.commands;

import me.stephenminer.raftbattle.RaftBattle;
import me.stephenminer.raftbattle.game.GameMap;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.UUID;

public class QuitGame implements CommandExecutor {
    private final RaftBattle plugin;

    public QuitGame(){
        this.plugin = JavaPlugin.getPlugin(RaftBattle.class);
    }


    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return false;
        }
        Player player = (Player) sender;
        GameMap game = gameIn(player);
        if (game != null){
            game.removePlayer(player,true);
            player.sendMessage(ChatColor.GREEN + "Removing you from your game");
            return true;
        }else player.sendMessage(ChatColor.RED + "You are not playing a game right now!");
        return false;
    }

    private GameMap gameIn(Player player){
        UUID uuid = player.getUniqueId();
        Collection<GameMap> active = plugin.active.values();
        for (GameMap map : active){
            if (map.players().contains(uuid)) return map;
        }
        return null;
    }
}
