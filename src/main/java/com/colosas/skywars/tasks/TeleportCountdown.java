package com.colosas.skywars.tasks;

import com.colosas.skywars.Skywars;
import com.colosas.skywars.objects.Match;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Instrument;
import org.bukkit.Note;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

public class TeleportCountdown extends BukkitRunnable {

    private final Match match;
    private int count;

    private final String broadcastMessage;
    private final String teleportedMessage;
    private final boolean broadcastActionBar;
    private final List<Integer> broadcastTitle;

    public TeleportCountdown(Match match) {
        this.match = match;
        this.match.setState(Match.MatchState.STARTING);
        count = Skywars.getInstanceConfig().getInt("game.teleportCountdown");
        if (count <= 0) {
            count = 20;
            Skywars.getInstance().getLogger().warning("Invalid teleport countdown, using default of 20 seconds.");
        }

        broadcastMessage = Skywars.getInstanceConfig().getString("messages.teleportCountdown.chat");
        teleportedMessage = Skywars.getInstanceConfig().getString("messages.teleportCountdown.teleported");
        broadcastActionBar = Skywars.getInstanceConfig().getBoolean("messages.teleportCountdown.actionBar");
        broadcastTitle = Skywars.getInstanceConfig().getIntegerList("messages.teleportCountdown.title");

//        broadcast();
    }

    @Override
    public void run() {
        if (count <= 0) {
            cancel();
            match.teleportPlayers();
            match.broadcast(LegacyComponentSerializer.legacy('&').deserialize(teleportedMessage));
            match.clearActionBars();
            match.setStartCountdown(new StartCountdown(match));
            match.getStartCountdown().runTaskTimer(Skywars.getInstance(), 0, 20);
        } else {
            broadcast();
            count--;
        }
    }

    public void broadcast() {
        if (broadcastMessage != null && !broadcastMessage.equals("")) match.broadcast(LegacyComponentSerializer.legacy('&').deserialize(broadcastMessage).replaceText(TextReplacementConfig.builder().match("%count%").replacement(Integer.toString(count)).build()));
        if (broadcastActionBar || broadcastTitle.size() > 0) {
            for (Player player : match.getPlayers()) {
                if (broadcastActionBar) player.sendActionBar(Component.text(count, NamedTextColor.RED));
                if (broadcastTitle.size() > 0 && broadcastTitle.contains(count)) {
                    player.playNote(player.getLocation(), Instrument.STICKS, Note.sharp(1, Note.Tone.F));
                    player.sendTitle(ChatColor.RED + Integer.toString(count), "", 5, 20, 5);
                }
            }
        }
    }
}
