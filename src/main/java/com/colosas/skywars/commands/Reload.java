package com.colosas.skywars.commands;

import com.colosas.skywars.Skywars;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class Reload implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        sender.sendMessage(Component.text("Reloading...", NamedTextColor.GREEN));
        Skywars.getInstance().reload();
        sender.sendMessage(Component.text("Reloaded!", NamedTextColor.GREEN));
        return true;
    }
}
