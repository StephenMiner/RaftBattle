package me.stephenminer.raftbattle.game.util;

import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class Items {

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
        ItemStack item = new ItemStack(Material.WOOL, 1, DyeColor.BLUE.getWoolData());
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.BLUE + "" + ChatColor.BOLD + "Click to join Team Blue");
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
        ItemStack item = new ItemStack(Material.WOOL, 1, DyeColor.RED.getWoolData());
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
}
