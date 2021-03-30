package com.colosas.skywars.listeners;

import com.colosas.skywars.Skywars;
import com.colosas.skywars.objects.Match;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class CraftItem implements Listener {

    @EventHandler
    public void onItemCraft(CraftItemEvent event) {
        ItemStack stack = event.getRecipe().getResult();
        Match match = Skywars.getInstance().getMatch();

        if (!match.isState(Match.MatchState.ACTIVE) && !match.isState(Match.MatchState.DEATHMATCH)) return;

        List<String> itemNames = Skywars.getInstanceConfig().getStringList("game.itemBlacklist");
        for (String itemName : itemNames) {
            Material material = Material.getMaterial(itemName.toUpperCase(Locale.ENGLISH));

            if (material == null) {
                Skywars.getInstance().getLogger().warning("Unable to resolve item with name '" + itemName + "'.");
                continue;
            }

            ItemStack item = new ItemStack(material);

            if (item.isSimilar(stack)) {
                event.setCancelled(true);
                event.getWhoClicked().sendMessage(Component.text("This item is not allowed!", NamedTextColor.RED));
                return;
            }
        }
    }

}
