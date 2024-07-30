package me.stephenminer.raftbattle.listeners;

import me.stephenminer.raftbattle.RaftBattle;
import me.stephenminer.raftbattle.game.GameMap;
import me.stephenminer.raftbattle.game.util.OfflineProfile;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
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
        GameMap game = gameIn(uuid);
        if (game == null) return;
        game.removePlayer(player,true);
    }

    /**
     * Gets the GameMap the player is participating in, null if none
     * @param uuid
     * @return
     */
    private GameMap gameIn(UUID uuid){
        for (GameMap map : plugin.active.values()){
            if (map.players().contains(uuid)) return map;
        }
        return null;
    }

    @EventHandler
    public void rejoin(PlayerJoinEvent event){
        GameMap game = null;
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        for (GameMap map : plugin.active.values()){
            if (map.offlines().containsKey(uuid)){
                game = map;
                break;
            }
        }
        if (game == null) return;
        if (game.board().isTeam1(player))
            player.teleport(game.spawn1());
        if (game.board().isTeam2(player))
            player.teleport(game.spawn2());
        OfflineProfile profile = game.offlines().remove(uuid);
        profile.loadOntoPlayer(player);

    }


}
