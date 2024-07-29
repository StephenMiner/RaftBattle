package me.stephenminer.raftbattle.game;

import org.bukkit.Bukkit;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class GameBoard {
    private Scoreboard board;
    private Team team1,team2;

    public GameBoard(){
        board = Bukkit.getScoreboardManager().getMainScoreboard();
        team1 = board.registerNewTeam("team1");
        team2 = board.registerNewTeam("team2");
    }






    public Scoreboard board(){ return board; }

    public Team team1(){ return team1; }
    public Team team2(){ return team2; }

}
