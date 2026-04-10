package com.elevatorplugin.managers;

import com.elevatorplugin.ElevatorPlugin;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import java.util.logging.Level;

/**
 * Persists elevator block locations to elevators.yml so they survive server restarts.
 */
public class ElevatorDataManager {

    private final ElevatorPlugin plugin;
    private final File dataFile;
    private YamlConfiguration dataConfig;

    // In-memory set of "world:x:y:z" keys for fast lookup
    private final Set<String> elevatorBlocks = Collections.newSetFromMap(new HashMap<>());

    public ElevatorDataManager(ElevatorPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "elevators.yml");
        load();
    }

    // -------------------------------------------------------
    // Public API
    // -------------------------------------------------------

    public boolean isElevator(Block block) {
        return elevatorBlocks.contains(toKey(block));
    }

    public boolean isElevator(String key) {
        return elevatorBlocks.contains(key);
    }

    public void addElevator(Block block) {
        String key = toKey(block);
        elevatorBlocks.add(key);
        save();
    }

    public void removeElevator(Block block) {
        String key = toKey(block);
        elevatorBlocks.remove(key);
        save();
    }

    public void removeElevator(String key) {
        elevatorBlocks.remove(key);
        save();
    }

    public String toKey(Block block) {
        return block.getWorld().getName() + ":" + block.getX() + ":" + block.getY() + ":" + block.getZ();
    }

    // -------------------------------------------------------
    // Persistence
    // -------------------------------------------------------

    private void load() {
        if (!dataFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create elevators.yml!", e);
            }
        }

        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        elevatorBlocks.clear();

        java.util.List<String> keys = dataConfig.getStringList("elevators");
        elevatorBlocks.addAll(keys);

        plugin.getLogger().info("Loaded " + elevatorBlocks.size() + " elevator block(s) from disk.");
    }

    private void save() {
        dataConfig.set("elevators", new java.util.ArrayList<>(elevatorBlocks));
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save elevators.yml!", e);
        }
    }

    public void reload() {
        load();
    }
}
