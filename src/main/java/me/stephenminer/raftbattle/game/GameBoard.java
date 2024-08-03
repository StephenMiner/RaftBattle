package me.stephenminer.raftbattle.game;

import me.stephenminer.raftbattle.RaftBattle;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.*;

public class GameBoard {
    private final RaftBattle plugin;
    private Scoreboard board;
    private Team team1,team2;
    private GameMap host;

    private final List<UUID> team1Prefer, team2Prefer;

    public GameBoard(GameMap host){
        board = Bukkit.getScoreboardManager().getNewScoreboard();
        team1 = board.registerNewTeam("team1");
        team1.setAllowFriendlyFire(false);
        team2 = board.registerNewTeam("team2");
        team2.setAllowFriendlyFire(false);
        team1Prefer = new ArrayList<>();
        team2Prefer = new ArrayList<>();
        this.host = host;
        this.plugin = JavaPlugin.getPlugin(RaftBattle.class);
        initBoard();
    }

    private void initBoard(){
        Objective obj = board.registerNewObjective("teams", "dummy");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        obj.setDisplayName("Raft Battles");

    }

    public void updateBoard(){
        Team count1 = board.registerNewTeam("team1count");
        count1.addEntry(ChatColor.BLUE + "" + ChatColor.BLACK);
        Team count2 = board.registerNewTeam("team2count");
        count2.addEntry(ChatColor.BLUE + "" + ChatColor.RED);
        Team sheep1 = board.registerNewTeam("sheep1hp");
        sheep1.addEntry(ChatColor.BLUE + "" + ChatColor.GREEN);
        //sheep1.addEntry("Sheep:");
        Team sheep2 = board.registerNewTeam("sheep2hp");
        sheep2.addEntry(ChatColor.BLUE + "" + ChatColor.YELLOW);
        //sheep2.addEntry("Sheep:");

        Objective obj = board.getObjective("teams");
        obj.getScore(ChatColor.BLUE + "" + ChatColor.GREEN).setScore(9);
        obj.getScore(ChatColor.BLUE + "" + ChatColor.BLACK).setScore(8);
        obj.getScore("----------").setScore(7);
        obj.getScore(ChatColor.BLUE + "" + ChatColor.YELLOW).setScore(6);
        obj.getScore(ChatColor.BLUE + "" + ChatColor.RED).setScore(5);

        new BukkitRunnable(){
            @Override
            public void run(){
                if (!host.started()) {
                    this.cancel();
                    return;
                }
                SheepCore core1 = host.core(true);
                sheep1.setPrefix(plugin.teamName(true) + " Sheep:");
                sheep1.setSuffix(ChatColor.GREEN + " " + shortenDecimal(core1.health()) + "/" + core1.maxHealth() + " HP");
                SheepCore core2 = host.core(false);
                sheep2.setPrefix(plugin.teamName(false) + " Sheep:");
                sheep2.setSuffix(ChatColor.GREEN + " " + shortenDecimal(core2.health()) + "/" + core2.maxHealth() + " HP");
                count1.setPrefix( plugin.teamName(true) +  ":");
                count1.setSuffix(ChatColor.WHITE + " " + alive(team1) + " Alive");
                count2.setPrefix(plugin.teamName(false) + ":");
                count2.setSuffix(ChatColor.WHITE + " " + alive(team2) + " Alive");
            }
        }.runTaskTimer(plugin,1, 10);
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
        if (team1Prefer.contains(uuid)) return true;
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
        if (team2Prefer.contains(uuid)) return true;
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
        while(teamOverflow(team1Prefer)){
            UUID uuid = team1Prefer.remove(i1);
            team2Prefer.add(uuid);
            i1--;
        }
        int i2 = team2Prefer.size()-1;
        while(teamOverflow(team2Prefer)){
            UUID uuid = team2Prefer.remove(i2);
            team1Prefer.add(uuid);
            i2--;
        }
        Set<UUID> copy = new HashSet<>(host.players());
        team1Prefer.forEach(copy::remove);
        team2Prefer.forEach(copy::remove);

        if (!copy.isEmpty()) {
            Random random = new Random();
            for (UUID uuid : copy) {
                assignPreference(uuid, random);
            }
        }
        team1Prefer.forEach(uuid->team1.addPlayer(Bukkit.getOfflinePlayer(uuid)));
        team2Prefer.forEach(uuid->team2.addPlayer(Bukkit.getOfflinePlayer(uuid)));


        team1Prefer.clear();
        team2Prefer.clear();
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
     * @param teamPrefer
     * @return true if the team size <= 1 + half the total players
     */
    private boolean teamOverflow(List<UUID> teamPrefer){
        int total = host.players().size();
        return  (total % 2 != 0 && teamPrefer.size() > 1 + (total / 2)) ||(total % 2 == 0 && teamPrefer.size() > (total / 2));
    }


    public void clearPreferences(UUID uuid){
        team1Prefer.remove(uuid);
        team2Prefer.remove(uuid);
    }

    public boolean isTeam1(Player player){
        return team1.hasPlayer(player);
    }
    public boolean isTeam2(Player player){
        return team2.hasPlayer(player);
    }

    public int alive(Team team){
        return (int) team.getPlayers().stream()
                .filter(OfflinePlayer::isOnline)
                .filter(player->player.getPlayer().getGameMode()== GameMode.SURVIVAL)
                .count();
    }

    public String shortenDecimal(double num){
        int convert = Math.max(0,(int) (10 * num));
        return "" + convert/10d;
    }
}
