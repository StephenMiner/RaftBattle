package me.stephenminer.raftbattle.game;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.*;

public class GameBoard {
    private Scoreboard board;
    private Team team1,team2;
    private GameMap host;

    private final List<UUID> team1Prefer, team2Prefer;

    public GameBoard(GameMap host){
        board = Bukkit.getScoreboardManager().getMainScoreboard();
        team1 = board.registerNewTeam("team1");
        team1.setAllowFriendlyFire(false);
        team2 = board.registerNewTeam("team2");
        team2.setAllowFriendlyFire(false);
        team1Prefer = new ArrayList<>();
        team2Prefer = new ArrayList<>();
        this.host = host;
    }






    public Scoreboard board(){ return board; }

    public Team team1(){ return team1; }
    public Team team2(){ return team2; }




    /**
     * Mark a players preference for team 2
     * @param uuid
     * @return true if the players preference was marked, false otherwise. If false, the team is currently full
     */
    public boolean prefersTeam1(UUID uuid){
        int total = host.players().size();
        if ((total % 2 != 0 && team1Prefer.size() > total/2) || (total % 2 == 0 && team1Prefer.size() >= total / 2)) return false;
        team1Prefer.add(uuid);
        team2Prefer.remove(uuid);
        return true;
    }

    /**
     * Mark a players preference for team 2
     * @param uuid
     * @return true if the players preference was marked, false otherwise. If false, the team is currently full
     */
    public boolean prefersTeam2(UUID uuid){
        int total = host.players().size();
        if ((total % 2 != 0 && team2Prefer.size() > total/2) || (total % 2 == 0 && team2Prefer.size() >= total / 2)) return false;
        team2Prefer.add(uuid);
        team1Prefer.remove(uuid);
        return true;
    }

    /**
     * Mark a players preference for team 2
     * @param player
     * @return true if the players preference was marked, false otherwise. If false, the team is currently full
     */
    public boolean prefersTeam1(OfflinePlayer player){
        return prefersTeam1(player.getUniqueId());
    }

    /**
     * Mark a players preference for team 2
     * @param player
     * @return true if the players preference was marked, false otherwise. If false, the team is currently full
     */
    public boolean prefersTeam2(OfflinePlayer player){
        return prefersTeam2(player.getUniqueId());
    }

    /**
     * Will populate teams 1 and 2 from players found in GameMap#players()
     */
    public void fillTeams(){
        int i1 = team1Prefer.size()-1;
        while(teamOverflow(team1)){
            UUID uuid = team1Prefer.remove(i1);
            team2Prefer.add(uuid);
        }
        int i2 = team2Prefer.size()-1;
        while(teamOverflow(team2)){
            UUID uuid = team2Prefer.remove(i1);
            team1Prefer.add(uuid);
        }


        Set<UUID> copy = new HashSet<>(host.players());
        team1Prefer.forEach(copy::remove);
        team2Prefer.forEach(copy::remove);
        if (copy.isEmpty()) return;
        Random random = new Random();
        for (UUID uuid : copy){
            assignPreference(uuid, random);
        }

        team1Prefer.forEach(uuid->team1.addPlayer(Bukkit.getOfflinePlayer(uuid)));
        team2Prefer.forEach(uuid->team2.addPlayer(Bukkit.getOfflinePlayer(uuid)));
    }

    private void assignPreference(UUID uuid, Random random){
        if (random.nextBoolean()){
            if (!prefersTeam1(uuid)) prefersTeam2(uuid);
        }else{
            if (!prefersTeam2(uuid)) prefersTeam1(uuid);
        }
    }

    /**
     * Checks if there are too many players on a team or not
     * @param team
     * @return true if the team size <= 1 + half the total players
     */
    private boolean teamOverflow(Team team){
        int total = host.players().size();
        return  (total % 2 != 0 && team.getSize() > 1 + (total / 2)) ||(total % 2 == 0 && team.getSize() > (total / 2));
    }

}
