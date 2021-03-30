package com.colosas.skywars.commands;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ToWorld implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only a player can use this command!");
            return false;
        }
        if (args.length == 0) {
            sender.sendMessage("Please specify a world name!");
            return false;
        }
        String worldName = args[0];
        World world = WorldCreator.name(worldName).createWorld();

        assert world != null;

        Player player = (Player) sender;
        player.teleport(world.getSpawnLocation());
        return true;
    }
}
