package me.stephenminer.raftbattle.game.util;

import org.bukkit.ChatColor;
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
}
