package com.elevatorplugin.managers;

import com.elevatorplugin.ElevatorPlugin;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ElevatorManager {

    private final ElevatorPlugin plugin;
    private NamespacedKey recipeKey;

    public ElevatorManager(ElevatorPlugin plugin) {
        this.plugin = plugin;
        this.recipeKey = new NamespacedKey(plugin, "elevator_block");
    }

    public void reload() {
        // Remove old recipe and re-register
        plugin.getServer().removeRecipe(recipeKey);
        registerRecipe();
    }

    public void registerRecipe() {
        ConfigManager cfg = plugin.getConfigManager();

        if (!cfg.isCraftingEnabled()) {
            plugin.getLogger().info("Crafting recipe is disabled in config.");
            return;
        }

        ItemStack elevatorItem = createElevatorItem(cfg.getCraftResultAmount());

        ShapedRecipe recipe = new ShapedRecipe(recipeKey, elevatorItem);

        List<String> shape = cfg.getCraftShape();
        if (shape.size() >= 3) {
            recipe.shape(shape.get(0), shape.get(1), shape.get(2));
        } else {
            // Fallback shape
            recipe.shape("WWW", "WEW", "WWW");
        }

        recipe.setIngredient('W', cfg.getIngredientW());
        recipe.setIngredient('E', cfg.getIngredientE());

        plugin.getServer().addRecipe(recipe);

        if (cfg.isDebug()) {
            plugin.getLogger().info("[Debug] Elevator recipe registered.");
        }
    }

    public ItemStack createElevatorItem(int amount) {
        ConfigManager cfg = plugin.getConfigManager();

        ItemStack item = new ItemStack(cfg.getElevatorMaterial(), amount);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(cfg.getElevatorDisplayName());

            List<String> lore = new ArrayList<>();
            for (String line : cfg.getElevatorLore()) {
                lore.add(cfg.color(line));
            }
            meta.setLore(lore);

            // Custom model data tag to identify elevator items
            meta.setCustomModelData(777999);
            item.setItemMeta(meta);
        }

        return item;
    }

    public boolean isElevatorBlock(Material material) {
        return material == plugin.getConfigManager().getElevatorMaterial();
    }

    public boolean isElevatorItem(ItemStack item) {
        if (item == null || item.getType() != plugin.getConfigManager().getElevatorMaterial()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.hasCustomModelData() && meta.getCustomModelData() == 777999;
    }

    public NamespacedKey getRecipeKey() {
        return recipeKey;
    }
}
