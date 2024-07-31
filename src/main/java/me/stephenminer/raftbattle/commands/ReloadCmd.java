package me.stephenminer.raftbattle.commands;

import me.stephenminer.raftbattle.RaftBattle;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class ReloadCmd implements CommandExecutor {
    private final RaftBattle plugin;

    public ReloadCmd(){
        this.plugin = JavaPlugin.getPlugin(RaftBattle.class);
    }


    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
        if (!sender.hasPermission("raftbattle.commands.reload")){
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command!");
            return false;
        }
        plugin.loot.reloadConfig();
        plugin.settings.reloadConfig();
        plugin.maps.reloadConfig();
        if (plugin.settings.getConfig().contains("reroute"))
            plugin.reroute = plugin.fromString(plugin.settings.getConfig().getString("reroute"));
        sender.sendMessage(ChatColor.GREEN + "Reloaded files");
        return true;
    }
}
