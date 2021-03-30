package com.colosas.skywars.listeners;

import com.colosas.skywars.Skywars;
import com.colosas.skywars.objects.Match;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class PlayerDeath implements Listener {

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        event.setCancelled(true);
        event.deathMessage(null);
        Match match = Skywars.getInstance().getMatch();
        if (match.isState(Match.MatchState.ACTIVE) || match.isState(Match.MatchState.DEATHMATCH) || match.isState(Match.MatchState.PREPARATION)) match.handleDeath(player, player.getKiller());
        else {
            if (match.isState(Match.MatchState.ENDING)) player.teleport(match.getSpectatorPoint());
            else {
                Bukkit.getScheduler().scheduleSyncDelayedTask(Skywars.getInstance(), () -> player.teleport(match.getLobbyPoint()));
            }
        }
    }

}
