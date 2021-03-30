package com.colosas.skywars.listeners;

import com.colosas.skywars.Skywars;
import com.colosas.skywars.objects.Match;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class ChestOpen implements Listener {

    @EventHandler
    public void onChestOpen(PlayerInteractEvent event) {

        Map<ItemStack, Integer> items = Skywars.getInstance().getItems();

        Player player = event.getPlayer();
        Match match = Skywars.getInstance().getMatch();

        if (!match.hasPlayer(player)) return;
        if (!match.isState(Match.MatchState.ACTIVE) && !match.isState(Match.MatchState.DEATHMATCH)) return;

        if (!event.hasBlock() || event.getClickedBlock() == null || event.getClickedBlock().getType() != Material.CHEST) return;

        Chest chest = (Chest) event.getClickedBlock().getState();
        if (match.getOpenedChests().contains(chest)) return;
        match.getOpenedChests().add(chest);

        Inventory inventory = chest.getBlockInventory();

        Random random = new Random();

        Set<ItemStack> usedItems = new HashSet<>();

        for (int i = 0; i < inventory.getSize(); i++) {
            boolean isAir = random.nextInt(3) > 0;
            if (isAir) continue;
            if (usedItems.size() == items.size()) break;

            ItemStack stack = getStack(items);

            usedItems.add(stack);

            inventory.setItem(i, stack);

            if (usedItems.size() == items.size()) break;
        }
    }

    public ItemStack getStack(Map<ItemStack, Integer> items) {
        Set<ItemStack> itemStacks = items.keySet();

        Random random = new Random();

        int count = 0;
        int randomCount = random.nextInt(itemStacks.size());

        for (ItemStack itemStack : itemStacks) {
            int probability = items.get(itemStack);
            if (count == randomCount) {
                if (probability != -1) {
                    int itemSelected = random.nextInt(100) + 1;
                    if (itemSelected > probability) {
                        return getStack(items);
                    }
                }
                return itemStack;
            }
            count++;
        }
        return null;
    }

}
