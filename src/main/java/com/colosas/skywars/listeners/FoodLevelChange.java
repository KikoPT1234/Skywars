package com.colosas.skywars.listeners;

import com.colosas.skywars.Skywars;
import com.colosas.skywars.objects.Match;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.FoodLevelChangeEvent;

public class FoodLevelChange implements Listener {

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();
        Match match = Skywars.getInstance().getMatch();

        if (match.isState(Match.MatchState.WAITING) || match.isState(Match.MatchState.STARTING) || match.isState(Match.MatchState.PREPARATION) || match.isState(Match.MatchState.ENDING)) {
            event.setFoodLevel(20);
            player.setSaturation(20);
        }
    }

}
