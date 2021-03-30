package com.colosas.skywars.listeners;

import com.colosas.skywars.Skywars;
import com.colosas.skywars.objects.Match;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoin implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Match match = Skywars.getInstance().getMatch();
        if (match.isState(Match.MatchState.ENDING) || match.isState(Match.MatchState.SHUTTING_DOWN)) {
            event.getPlayer().kick(Component.text("Match's over"));
            return;
        }
        event.joinMessage(null);
        match.handleJoin(event.getPlayer());
    }

}
