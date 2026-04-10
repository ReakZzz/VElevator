package com.elevatorplugin;

import com.elevatorplugin.commands.ElevatorCommand;
import com.elevatorplugin.listeners.ElevatorCraftListener;
import com.elevatorplugin.listeners.ElevatorUseListener;
import com.elevatorplugin.managers.ConfigManager;
import com.elevatorplugin.managers.ElevatorDataManager;
import com.elevatorplugin.managers.ElevatorManager;
import org.bukkit.plugin.java.JavaPlugin;

public class ElevatorPlugin extends JavaPlugin {

    private static ElevatorPlugin instance;
    private ConfigManager configManager;
    private ElevatorManager elevatorManager;
    private ElevatorDataManager dataManager;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        configManager = new ConfigManager(this);
        dataManager   = new ElevatorDataManager(this);
        elevatorManager = new ElevatorManager(this);

        getServer().getPluginManager().registerEvents(new ElevatorUseListener(this), this);
        getServer().getPluginManager().registerEvents(new ElevatorCraftListener(this), this);

        ElevatorCommand elevatorCommand = new ElevatorCommand(this);
        getCommand("elevator").setExecutor(elevatorCommand);
        getCommand("elevator").setTabCompleter(elevatorCommand);

        elevatorManager.registerRecipe();

        getLogger().info("====================================");
        getLogger().info("  Simple Elevators Plugin Enabled!");
        getLogger().info("  Version: " + getDescription().getVersion());
        getLogger().info("====================================");
    }

    @Override
    public void onDisable() {
        getLogger().info("Simple Elevators Plugin Disabled.");
    }

    public void reload() {
        reloadConfig();
        configManager.reload();
        dataManager.reload();
        elevatorManager.reload();
    }

    public static ElevatorPlugin getInstance() { return instance; }
    public ConfigManager getConfigManager()    { return configManager; }
    public ElevatorManager getElevatorManager(){ return elevatorManager; }
    public ElevatorDataManager getDataManager() { return dataManager; }
}
