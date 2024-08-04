package me.stephenminer.raftbattle.game.fishing;

import me.stephenminer.raftbattle.RaftBattle;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class LootLoader {
    private final String id;
    private final RaftBattle plugin;

    public LootLoader(String id){
        this.id = id;
        this.plugin = JavaPlugin.getPlugin(RaftBattle.class);
    }

    /**
     *
     * @param str formated as MATERIAL,weight,(optional)weight,(optional) damage, (optional) data?
     * @return
     */
    private LootPair readItem(String str){
        String[] unbox = str.split(",");
        Material mat = Material.matchMaterial(unbox[0]);
        if (mat == null){
            System.out.println(unbox[0]);
            return null;

        }
        int weight = Integer.parseInt(unbox[1]);
        int amount = unbox.length > 2 ? Integer.parseInt(unbox[2]) : 1;
        ItemStack item;
        if (unbox.length > 3){
            short damage = Short.parseShort(unbox[3]);
            item = new ItemStack(mat, amount, damage);
        }else item = new ItemStack(mat, amount);
        return new LootPair(item, weight);
    }

    private List<LootPair> loadLoot(){
        if (plugin.loot.getConfig().contains(id + ".loot")){
            List<String> entries = plugin.loot.getConfig().getStringList(id + ".loot");
            List<LootPair> loot = new ArrayList<>();
            entries.forEach(entry -> loot.add(readItem(entry)));
            return loot;
        }
        return null;
    }

    private int loadWeight(){
        return plugin.loot.getConfig().getInt(id + ".weight");
    }


    public FishingTable build(){
        int weight = loadWeight();
        List<LootPair> loot = loadLoot();
        if (loot == null) return null;
        return new FishingTable(id, weight, loot);
    }




}
