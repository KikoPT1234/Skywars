package com.colosas.skywars.listeners;

import com.colosas.skywars.Skywars;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerLeave implements Listener {

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        Skywars.getInstance().getMatch().handleLeave(event.getPlayer());
    }

}
