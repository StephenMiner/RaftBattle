package me.stephenminer.raftbattle.commands;

import me.stephenminer.raftbattle.RaftBattle;
import me.stephenminer.raftbattle.game.util.Items;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class PondWand implements CommandExecutor{
    private final RaftBattle plugin;

    public PondWand(){
        this.plugin = JavaPlugin.getPlugin(RaftBattle.class);
    }


    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
        if (!sender.hasPermission("raftbattle.commands.pond")){
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command!");
            return false;
        }
        if (sender instanceof Player){
            Player player = (Player)  sender;
            Items items = new Items();
            player.getInventory().addItem(items.pondWand());
            player.sendMessage(ChatColor.GREEN + "You've been given a wand!");
            return true;
        }else{
            sender.sendMessage(ChatColor.RED + "You need to be a player to use this command!");
        }
        return false;
    }


}
