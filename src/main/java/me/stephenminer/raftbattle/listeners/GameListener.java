package me.stephenminer.raftbattle.listeners;

import me.stephenminer.raftbattle.RaftBattle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public class GameListener implements Listener {
    private final RaftBattle plugin;


    public GameListener(){
        this.plugin = JavaPlugin.getPlugin(RaftBattle.class);
    }










    /*

    Handling premature quitting (World Changes, Quiting)

     */

    @EventHandler
    public void onLeave(PlayerQuitEvent event){
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
    }
}
