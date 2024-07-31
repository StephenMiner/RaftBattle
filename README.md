
Commands:

/raftjoin [map-id]  
Sends you to the specified map  
/raftjoin  
Sends you to he most populated game
alias: /rjoin

/raftquit
aslias: /rquit

raftmap permission: raftbattle.commands.map
/raftmap wand  
Gives a wand to define map regions  
/raftmap reroute  
Defines the "reroute" position for players when they leave the game  
/raftmap [id] team1spawn  
Sets the spawn for team1  
/raftmap [id] team2spawn  
Sets the spawn for team2  
/raftmap [id] waitingarea  
Sets the waiting ara for the map  
/raftmap [id] setname [name]  
Sets the name for the map, names aren't used ATM  

/raftreload  
Permission: raftbattle.commands.reload  
Reloads all the config files, use if you edited any file manually

Before you can join a map, you must first have...
1. A reroute position set
2. A waiting area set
3. Team 1 spawn set
4. Team 2 spawn set


The fishing loottables are edited in the loot.yml file and are structured as so:  
```
loot-table-name:
    weight: 20
    loot:
      - DIRT,100
      - WOOL,50,1,4
```

For the "DIRT,100" or "WOOL,50,1,4" strings, the first item represents the Material name. 
Sometimes these can be weird (Repeaters are called DIODE??) so I guess let me know if you can't figure one out.

The next item represents the weight for the item to drop, and the next entry represents how much should be dropped. Finally the last number represents
the damage value on the item. This doesn't just control durability, but also the colors for things like WOOL.

Only required inputs are the material name and the weight

In this plugin, weight works like so:  
1. All weights from potential drops are added together and a number 1 - the sum is rolled
2. From least to greatest, the weights are subtracted from the rolled number until that number is <= 0
3. Then all items with the last weight that was subtracted and gathered and perfectly random one is chosen 
