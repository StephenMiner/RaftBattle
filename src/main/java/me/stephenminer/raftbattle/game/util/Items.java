package me.stephenminer.raftbattle.game.util;

import me.stephenminer.raftbattle.RaftBattle;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class Items {
    private final RaftBattle plugin;
    public Items(){
        this.plugin = JavaPlugin.getPlugin(RaftBattle.class);
    }

    public ItemStack mapWand(){
        ItemStack item = new ItemStack(Material.GOLD_AXE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("Map Wand");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.YELLOW + "Left Click: Define 1st Corner");
        lore.add(ChatColor.YELLOW + "Right Click: Define 2nd Corner");
        lore.add(ChatColor.BLACK + "raft-battle-map");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack team1Selector(){
        ItemStack item = baseIcon(true);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "Click to join Team Blue");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.YELLOW + "Not a guarantee! But a good chance");
        lore.add(ChatColor.YELLOW + "You only lose a guaranteed spot if people not queued");
        lore.add(ChatColor.YELLOW + "for your team leave the game");
        lore.add(ChatColor.BLACK + "team1selector");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
    public ItemStack team2Selector(){
        ItemStack item = baseIcon(false);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "Click to join Team Red");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.YELLOW + "Not a guarantee! But a good chance");
        lore.add(ChatColor.YELLOW + "You only lose a guaranteed spot if people not queued");
        lore.add(ChatColor.YELLOW + "for your team leave the game");
        lore.add(ChatColor.BLACK + "team2selector");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack baseIcon(boolean team1){
        String path = team1 ? "team-1-item" : "team-2-item";
        String[] unbox = plugin.settings.getConfig().getString(path).split(",");
        Material mat = Material.matchMaterial(unbox[0]);
        short dmg = Short.parseShort(unbox[1]);
        return new ItemStack(mat, 1, dmg);
    }
}
