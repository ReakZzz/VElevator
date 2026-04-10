package com.elevatorplugin.managers;

import com.elevatorplugin.ElevatorPlugin;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;
import java.util.logging.Level;

public class ConfigManager {

    private final ElevatorPlugin plugin;

    // Elevator Item
    private Material elevatorMaterial;
    private String elevatorDisplayName;
    private List<String> elevatorLore;

    // Crafting
    private boolean craftingEnabled;
    private int craftResultAmount;
    private List<String> craftShape;
    private Material ingredientW;
    private Material ingredientE;

    // Behavior
    private int maxFloorDistance;
    private String teleportSound;
    private float soundVolume;
    private float soundPitch;

    // Particles
    private boolean particlesEnabled;
    private Particle particleType;
    private int particleCount;
    private double particleOffsetX;
    private double particleOffsetY;
    private double particleOffsetZ;
    private double particleSpeed;

    // Messages
    private boolean actionbarEnabled;
    private String actionbarUp;
    private String actionbarDown;
    private String actionbarNoFloor;
    private boolean titleEnabled;
    private String titleUp;
    private String titleSubtitleUp;
    private String titleDown;
    private String titleSubtitleDown;
    private String prefix;
    private String reloadSuccess;
    private String giveSuccess;
    private String noPermission;
    private String playerNotFound;

    // Blocked Worlds
    private List<String> blockedWorlds;

    // Debug
    private boolean debug;

    public ConfigManager(ElevatorPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    public void reload() {
        load();
    }

    private void load() {
        FileConfiguration config = plugin.getConfig();

        // Elevator Item
        elevatorMaterial = parseMaterial(config.getString("elevator-item.material", "WHITE_WOOL"), Material.WHITE_WOOL);
        elevatorDisplayName = color(config.getString("elevator-item.display-name", "&bElevator"));
        elevatorLore = config.getStringList("elevator-item.lore");

        // Crafting
        craftingEnabled = config.getBoolean("crafting.enabled", true);
        craftResultAmount = config.getInt("crafting.result-amount", 1);
        craftShape = config.getStringList("crafting.shape");
        ingredientW = parseMaterial(config.getString("crafting.ingredients.W", "WHITE_WOOL"), Material.WHITE_WOOL);
        ingredientE = parseMaterial(config.getString("crafting.ingredients.E", "ENDER_PEARL"), Material.ENDER_PEARL);

        // Behavior
        maxFloorDistance = config.getInt("elevator.max-floor-distance", 50);
        teleportSound = config.getString("elevator.teleport-sound", "ENTITY_ENDERMAN_TELEPORT");
        soundVolume = (float) config.getDouble("elevator.sound-volume", 0.5);
        soundPitch = (float) config.getDouble("elevator.sound-pitch", 1.0);

        // Particles
        particlesEnabled = config.getBoolean("particles.enabled", true);
        particleType = parseParticle(config.getString("particles.type", "PORTAL"));
        particleCount = config.getInt("particles.count", 20);
        particleOffsetX = config.getDouble("particles.offset-x", 0.3);
        particleOffsetY = config.getDouble("particles.offset-y", 0.5);
        particleOffsetZ = config.getDouble("particles.offset-z", 0.3);
        particleSpeed = config.getDouble("particles.speed", 0.05);

        // Messages
        actionbarEnabled = config.getBoolean("messages.actionbar-enabled", true);
        actionbarUp = color(config.getString("messages.actionbar-up", "&a▲ Going Up ▲"));
        actionbarDown = color(config.getString("messages.actionbar-down", "&c▼ Going Down ▼"));
        actionbarNoFloor = color(config.getString("messages.actionbar-no-floor", "&eNo floor found in that direction!"));
        titleEnabled = config.getBoolean("messages.title-enabled", false);
        titleUp = color(config.getString("messages.title-up", "&aFloor Up"));
        titleSubtitleUp = color(config.getString("messages.title-subtitle-up", "&7Moving to the next floor..."));
        titleDown = color(config.getString("messages.title-down", "&cFloor Down"));
        titleSubtitleDown = color(config.getString("messages.title-subtitle-down", "&7Moving to the floor below..."));
        prefix = color(config.getString("messages.prefix", "&8[&bElevator&8] &r"));
        reloadSuccess = color(config.getString("messages.reload-success", "&aConfiguration reloaded successfully!"));
        giveSuccess = color(config.getString("messages.give-success", "&aGave &e{amount} &aElevator(s) to &e{player}&a."));
        noPermission = color(config.getString("messages.no-permission", "&cYou don't have permission to do that."));
        playerNotFound = color(config.getString("messages.player-not-found", "&cPlayer not found."));

        // Blocked Worlds
        blockedWorlds = config.getStringList("blocked-worlds");

        // Debug
        debug = config.getBoolean("debug", false);

        if (debug) {
            plugin.getLogger().info("[Debug] Config loaded. Blocked worlds: " + blockedWorlds);
        }
    }

    private Material parseMaterial(String name, Material fallback) {
        try {
            Material mat = Material.valueOf(name.toUpperCase());
            return mat;
        } catch (IllegalArgumentException e) {
            plugin.getLogger().log(Level.WARNING, "Invalid material '" + name + "', using fallback: " + fallback.name());
            return fallback;
        }
    }

    private Particle parseParticle(String name) {
        try {
            return Particle.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().log(Level.WARNING, "Invalid particle '" + name + "', using PORTAL.");
            return Particle.PORTAL;
        }
    }

    public String color(String text) {
        if (text == null) return "";
        return text.replace("&", "§");
    }

    // ---------- Getters ----------

    public Material getElevatorMaterial() { return elevatorMaterial; }
    public String getElevatorDisplayName() { return elevatorDisplayName; }
    public List<String> getElevatorLore() { return elevatorLore; }

    public boolean isCraftingEnabled() { return craftingEnabled; }
    public int getCraftResultAmount() { return craftResultAmount; }
    public List<String> getCraftShape() { return craftShape; }
    public Material getIngredientW() { return ingredientW; }
    public Material getIngredientE() { return ingredientE; }

    public int getMaxFloorDistance() { return maxFloorDistance; }
    public String getTeleportSound() { return teleportSound; }
    public float getSoundVolume() { return soundVolume; }
    public float getSoundPitch() { return soundPitch; }

    public boolean isParticlesEnabled() { return particlesEnabled; }
    public Particle getParticleType() { return particleType; }
    public int getParticleCount() { return particleCount; }
    public double getParticleOffsetX() { return particleOffsetX; }
    public double getParticleOffsetY() { return particleOffsetY; }
    public double getParticleOffsetZ() { return particleOffsetZ; }
    public double getParticleSpeed() { return particleSpeed; }

    public boolean isActionbarEnabled() { return actionbarEnabled; }
    public String getActionbarUp() { return actionbarUp; }
    public String getActionbarDown() { return actionbarDown; }
    public String getActionbarNoFloor() { return actionbarNoFloor; }
    public boolean isTitleEnabled() { return titleEnabled; }
    public String getTitleUp() { return titleUp; }
    public String getTitleSubtitleUp() { return titleSubtitleUp; }
    public String getTitleDown() { return titleDown; }
    public String getTitleSubtitleDown() { return titleSubtitleDown; }
    public String getPrefix() { return prefix; }
    public String getReloadSuccess() { return reloadSuccess; }
    public String getGiveSuccess() { return giveSuccess; }
    public String getNoPermission() { return noPermission; }
    public String getPlayerNotFound() { return playerNotFound; }

    public List<String> getBlockedWorlds() { return blockedWorlds; }
    public boolean isDebug() { return debug; }
}
