package me.stephenminer.raftbattle.game.gui;

import me.stephenminer.raftbattle.RaftBattle;
import me.stephenminer.raftbattle.game.GameMap;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class GameMenu {
    private final RaftBattle plugin;
    private Inventory inv;
    private ItemStack filler;

    public GameMenu(){
        this.plugin = JavaPlugin.getPlugin(RaftBattle.class);
        Set<String> ids = plugin.maps.getConfig().getConfigurationSection("maps").getKeys(false);
        int size = Math.min(54, (1 + (ids.size() / 9))  * 9);
        inv = Bukkit.createInventory(null,size,"Joinable Games");
        this.filler = filler();
    }

    public void updateInventory(Player viewer){
        viewer.openInventory(inv);
        new BukkitRunnable(){
            @Override
            public void run(){
                if (!viewer.getInventory().equals(inv)) {
                    this.cancel();
                    return;
                }
                Set<String> ids = plugin.maps.getConfig().getConfigurationSection("maps").getKeys(false);
                for (String id : ids){
                    ItemStack mapItem = mapItem(id);
                    inv.addItem(mapItem);
                }
            }
        }.runTaskTimer(plugin,1,20);
    }



    private ItemStack mapItem(String id){
        ItemStack item;
        ChatColor color;
        boolean active = plugin.active.containsKey(id);
        if (active && plugin.active.get(id).started()) {
            item = new ItemStack(Material.STAINED_CLAY, 1, DyeColor.RED.getDyeData());
            color = ChatColor.RED;
        }
        else if (active && plugin.active.get(id).isFull()) {
            item = new ItemStack(Material.STAINED_CLAY, 1, DyeColor.CYAN.getDyeData());
            color = ChatColor.DARK_AQUA;
        }
        else {
            item = new ItemStack(Material.STAINED_CLAY,1,DyeColor.GREEN.getDyeData());
            color = ChatColor.GREEN;
        }
        String name = plugin.maps.getConfig().getString("maps." + id + ".name");
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(color + name);
        GameMap map = plugin.active.getOrDefault(id, null);
        int max = plugin.readMaxPlayers();
        String playerCount;

        if (map == null) playerCount = "0 / "+ max + " players";
        else playerCount = map.players().size() + " / " + max + " players";
        List<String> lore = new ArrayList<>();
        lore.add(playerCount);

        String statusText = null;
        switch (color){
            case RED:
                statusText = "STARTED";
                break;
            case DARK_AQUA:
                statusText = "FULL";
                break;
            case GREEN:
                statusText = "You Can Join";
                break;
        }
        lore.add(color + statusText);
        lore.add(ChatColor.BLACK + "id");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private String playersString(GameMap map){
        int max = plugin.readMaxPlayers();
        if (map == null) return "0 / "+ max + " players";
        else return map.players().size() + " / " + max + " players";
    }

    private boolean checkItemEquivilency(ItemStack item, String id){
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) return false;
        List<String> lore = item.getItemMeta().getLore();
        String target = lore.get(0);
        return (playersString(plugin.active.getOrDefault(id, null)).equals(target));
    }








    private ItemStack filler(){
        ItemStack item = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 7);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(" ");
        item.setItemMeta(meta);
        return item;
    }


}
