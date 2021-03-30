package com.colosas.skywars.listeners;

import com.colosas.skywars.Skywars;
import com.colosas.skywars.objects.Match;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class BlockBreak implements Listener {

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Match match = Skywars.getInstance().getMatch();
        Block block = event.getBlock();
        if (block.getType() == Material.CHEST && (match.isState(Match.MatchState.ACTIVE) || match.isState(Match.MatchState.DEATHMATCH))) event.setCancelled(true);
    }

}
