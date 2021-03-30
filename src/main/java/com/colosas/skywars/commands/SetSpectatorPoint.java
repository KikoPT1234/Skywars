package com.colosas.skywars.commands;

import com.colosas.skywars.Skywars;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SetSpectatorPoint implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("Only a player can execute this command!", NamedTextColor.RED));
            return false;
        }

        if (args.length < 1) {
            sender.sendMessage(Component.text("Please specify the name of the map!", NamedTextColor.RED));
            return false;
        }

        String map = args[0];

        Player player = (Player) sender;
        Location spectatorPoint = player.getLocation();

        FileConfiguration config = Skywars.getInstanceConfig();

        config.set("maps." + map + ".spectatorPoint.x", spectatorPoint.getBlockX());
        config.set("maps." + map + ".spectatorPoint.y", spectatorPoint.getBlockY());
        config.set("maps." + map + ".spectatorPoint.z", spectatorPoint.getBlockZ());

        Skywars.getInstance().saveConfig();

        return true;
    }
}
