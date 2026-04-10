package com.elevatorplugin.listeners;

import com.elevatorplugin.ElevatorPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;

public class ElevatorCraftListener implements Listener {

    private final ElevatorPlugin plugin;

    public ElevatorCraftListener(ElevatorPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof org.bukkit.entity.Player player)) return;

        // Check if this is the elevator recipe
        if (event.getRecipe().getResult().getType() != plugin.getConfigManager().getElevatorMaterial()) return;

        // Check crafting permission
        if (!player.hasPermission("elevator.craft")) {
            event.setCancelled(true);
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    "§cYou don't have permission to craft elevators!");
            return;
        }

        // Ensure the crafted item has the correct metadata (custom model data)
        // Replace the result with our properly tagged item
        ItemStack result = plugin.getElevatorManager().createElevatorItem(
                plugin.getConfigManager().getCraftResultAmount()
        );
        event.setCurrentItem(result);
    }
}
