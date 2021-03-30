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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddSpawnPoint implements CommandExecutor {

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
        Location spawnPoint = player.getLocation();

        FileConfiguration config = Skywars.getInstanceConfig();

        List<Map<?, ?>> spawnPoints = config.getMapList("maps." + map + ".spawnPoints");

        Map<String, Integer> spawnPointMap = new HashMap<>();
        spawnPointMap.put("x", spawnPoint.getBlockX());
        spawnPointMap.put("y", spawnPoint.getBlockY());
        spawnPointMap.put("z", spawnPoint.getBlockZ());

        spawnPoints.add(spawnPointMap);

        config.set("maps." + map + ".spawnPoints", spawnPoints);

        Skywars.getInstance().saveConfig();

        return true;
    }
}
