package me.stephenminer.raftbattle.commands;

import me.stephenminer.raftbattle.RaftBattle;
import me.stephenminer.raftbattle.game.util.Items;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

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
        if (!sender.hasPermission("raftbattle.commands.map")){
            sender.sendMessage(ChatColor.RED + "No permission to use this command!");
            return false;
        }
        Player player = (Player) sender;
        int size = args.length;
        if (size == 1){
            if (args[0].equalsIgnoreCase("wand")){
                Items items = new Items();
                player.getInventory().addItem(items.mapWand());
                player.sendMessage(ChatColor.GREEN + "You got a wand");
                return true;
            }
            if (args[0].equalsIgnoreCase("reroute")){
                plugin.settings.reloadConfig();
                plugin.settings.getConfig().set("reroute",plugin.fromLoc(player.getLocation()));
                plugin.settings.saveConfig();
                plugin.reroute = player.getLocation();
                player.sendMessage(ChatColor.GREEN + "Set reroute point");
                return true;
            }
        }
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
                String subArg = args[2];
                switch (sub){
                    case "setname":
                        StringBuilder builder = new StringBuilder();
                        for (int i = 2; i < size; i++){
                            builder.append(args[i]).append(' ');
                        }
                        //Delete the last space
                        builder.deleteCharAt(builder.length()-1);
                        setName(id, builder.toString());
                        return true;
                    case "addpond":
                        //sub arg is pond id
                        if (!isPond(subArg)){
                            sender.sendMessage(ChatColor.RED + subArg + " is not a real pond!");
                            return false;
                        }
                        if (addPond(id, subArg)){
                            sender.sendMessage(ChatColor.GREEN + "Added " + subArg + " to the map " + id);
                            return true;
                        }else{
                            sender.sendMessage(ChatColor.YELLOW + subArg + " is already in the map " + id);
                            return false;
                        }
                    case "removepond":
                        //sub arg is pond id
                        if (!isPond(args[2])){
                            sender.sendMessage(ChatColor.RED + args[2] + " is not a real pond!");
                            return false;
                        }
                        if (removePond(id, args[2])){
                            sender.sendMessage(ChatColor.GREEN + "Removed pond " + subArg + " from the map " + id);
                            return true;
                        }else{
                            sender.sendMessage(ChatColor.YELLOW + subArg + "is already not in the map " + id);
                            return false;
                        }
                    case "bubbleinterval":
                        try{
                            int bubbleInterval = Integer.parseInt(subArg);
                            setBubbleInterval(id, bubbleInterval);
                            sender.sendMessage(ChatColor.GREEN + "Set the bubble interval for map " + id + " to " + bubbleInterval);
                            return true;
                        }catch (Exception e) {
                            sender.sendMessage(ChatColor.RED + subArg + " is not a whole number integer!");
                            return false;
                        }
                    case "bubblelife":
                        try{
                            int bubbleLife = Integer.parseInt(subArg);
                            setBubbleLifeSpan(id, bubbleLife);
                            sender.sendMessage(ChatColor.GREEN + "Set the bubble interval for map " + id + " to " + bubbleLife);
                            return true;
                        }catch (Exception e){
                            sender.sendMessage(ChatColor.RED + subArg + " is not a whole number integer!");
                            return false;
                        }
                    case "bubblechance":
                        try{
                            int bubbleChance = Integer.parseInt(subArg);
                            setBubbleChance(id, bubbleChance);
                            sender.sendMessage(ChatColor.GREEN + "Set the bubble interval for map " + id + " to " + bubbleChance);
                            return true;
                        }catch (Exception e){
                            sender.sendMessage(ChatColor.RED + subArg + " is not a whole number integer!");
                            return false;
                        }
                    case "maxbubblestreams":
                        try{
                            int maxStreams = Integer.parseInt(subArg);
                            setMaxBubbleStreams(id, maxStreams);
                            sender.sendMessage(ChatColor.GREEN + "Set the bubble interval for map " + id + " to " + maxStreams);
                            return true;
                        }catch (Exception e) {
                            sender.sendMessage(ChatColor.RED + subArg + " is not a whole number integer!");
                            return false;
                        }
                    case "setdecayrange":
                        try{
                            int decayRange = Integer.parseInt(subArg);
                            setDecayRange(id, decayRange);
                            sender.sendMessage(ChatColor.GREEN + "Set decay range for the map " + id + " to " + decayRange + "!");
                            return true;
                        }catch (Exception e){
                            sender.sendMessage(ChatColor.RED + subArg + " is not a whole number integer!");
                            return false;
                        }
                    case "setdecayticks":
                        try{
                            int decayTicks = Integer.parseInt(subArg);
                            setDecayTicks( id, decayTicks);
                            sender.sendMessage(ChatColor.GREEN + "Set decay ticks for the map " + id + " to " + decayTicks + "!");
                            return true;
                        }catch(Exception e){
                            sender.sendMessage(ChatColor.RED + subArg + " is not a whole number integer!");
                            return false;
                        }
                    case "restrictplacerange":
                        try{
                            int restrictPlaceRange = Integer.parseInt(subArg);
                            setRestrictPlaceRange(id, restrictPlaceRange);
                            sender.sendMessage(ChatColor.GREEN + "Set restrict place range for the map " + id + " to " + restrictPlaceRange + "!");
                            return true;
                        }catch (Exception e){
                            sender.sendMessage(ChatColor.RED + subArg + " is not a whole number integer!");
                            return false;
                        }
                }
            }
        }
        player.sendMessage(ChatColor.RED + "Most likely, you got here because you didnt input a sub command or use enough arguments for the inputted sub command");
        return false;
    }


    private boolean idExists(String id){
        return plugin.maps.getConfig().contains("maps." + id);
    }
    private boolean isPond(String pondId){
        return plugin.ponds.getConfig().contains("ponds." + pondId);
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

    private void setDecayRange(String id, int decayRange){
        plugin.maps.getConfig().set("maps." + id + ".decay-range", decayRange);
        plugin.maps.saveConfig();
    }

    private void setDecayTicks(String id, int decayTicks){
        plugin.maps.getConfig().set("maps." + id + ".decay-ticks", decayTicks);
        plugin.maps.saveConfig();
    }

    private boolean pondInMap(String mapId, String pondId){
        String ponds = plugin.maps.getConfig().getString("maps." + mapId + ".ponds");
        if (ponds == null || ponds.isEmpty()) return false;
        else return ponds.contains(pondId);
    }


    /**
     * Adds a pond id to the provided map id's pond entries in the maps.yml file
     * @param id the map id to add a pond to
     * @param pondId the pond id of the pond you wish to add to the map
     * @return true if the pond is added to the map, false if otherwise
     *          (mapId doesn't exist, pond doesn't exist, pond is already in the map)
     */
    private boolean addPond(String id, String pondId){
        if (!idExists(id) && !isPond(pondId)) return false;

        if (pondInMap(id, pondId)) return false;
        String path = "maps." + id + ".ponds";
        String currentPonds = plugin.maps.getConfig().getString(path);
        if (currentPonds == null) currentPonds = pondId;
        else currentPonds = currentPonds +  "," + pondId;
        plugin.maps.getConfig().set(path, currentPonds);
        plugin.maps.saveConfig();
        return true;
    }

    /**
     * Removes a pond id from the provided map id's pond entries in the maps.yml file
     * @param id the map id to add a pond to
     * @param pondId the pond id of the pond you wish to add to the map
     * @return true if the pond is removed from the map, false if otherwise
     *          (map isn't real, pond isn't real, pond isn't in the map)
     */
    private boolean removePond(String id, String pondId){
        if (!idExists(id) && !isPond(pondId)) return false;

        if (!pondInMap(id, pondId)) return false;
        String path = "maps." + id + ".ponds";
        String currentPonds = plugin.maps.getConfig().getString(path);
        //Break down string into individual pond ids, so we can reconstruct it without the pondId we want to remove
        String[] unbox = currentPonds.split(",");
        StringBuilder newPonds = new StringBuilder();
        for (String pond : unbox){
            if (pond.equalsIgnoreCase(pondId)) continue;
            newPonds.append(pond).append(',');
        }
        //This is to delete the extra comma we generate when placing any word into the string
        if (newPonds.length() > 0) newPonds.deleteCharAt(newPonds.length()-1);
        plugin.maps.getConfig().set(path, newPonds.toString());
        plugin.maps.saveConfig();
        return true;
    }


    private void setBubbleInterval(String mapId, int interval){
        plugin.maps.getConfig().set("maps." + mapId + ".bubble-spawn-period", interval);
        plugin.maps.saveConfig();
    }

    private void setBubbleLifeSpan(String mapId, int lifeSpan){
        plugin.maps.getConfig().set("maps." + mapId + ".bubble-life", lifeSpan);
        plugin.maps.saveConfig();
    }

    private void setBubbleChance(String mapId, int bubbleChance){
        plugin.maps.getConfig().set("maps." + mapId + ".bubble-spawn-chance",bubbleChance);
        plugin.maps.saveConfig();
    }

    private void setMaxBubbleStreams(String mapId, int maxStreams){
        plugin.maps.getConfig().set("maps." + mapId + ".max-bubble-streams", maxStreams);
        plugin.maps.saveConfig();
    }

    private void setRestrictPlaceRange(String mapId, int restrictPlaceRange){
        plugin.maps.getConfig().set("maps." + mapId + ".restrict-place-range", restrictPlaceRange);
        plugin.maps.saveConfig();
    }


    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args){
        int size = args.length;
        if (size == 1) return ids(args[0]);
        if (size == 2) return subs(args[1]);
        if (size == 3){
            String sub = args[1].toLowerCase();
            if (sub.equals("addpond"))
                return ponds(args[2]);
            if (sub.equals("removepond"))
                return pondsInMap(args[0], args[2]);
        }
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
        subs.add("addpond");
        subs.add("removepond");
        subs.add("bubbleInterval");
        subs.add("bubbleLife");
        subs.add("bubbleChance");
        subs.add("maxBubbleStreams");
        subs.add("setdecayrange");
        subs.add("setdecayticks");
        subs.add("setrestrictplacerange");
        return plugin.filter(subs, match);
    }

    private List<String> pondsInMap(String map, String match){
        if (!plugin.maps.getConfig().contains("maps." + map + ".ponds")) return null;
        String pondStr = plugin.maps.getConfig().getString("maps." + map + ".ponds");
        String[] itemizedStr = pondStr.split(",");
        return plugin.filter(Arrays.asList(itemizedStr), match);
    }

    private List<String> ponds(String match){
        if (!plugin.ponds.getConfig().contains("ponds")) return null;
        Collection<String> pondIds = plugin.ponds.getConfig().getConfigurationSection("ponds").getKeys(false);
        return plugin.filter(pondIds, match);
    }
}
