package com.elevatorplugin.listeners;

import com.elevatorplugin.ElevatorPlugin;
import com.elevatorplugin.managers.ConfigManager;
import com.elevatorplugin.managers.ElevatorDataManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ElevatorUseListener implements Listener {

    private final ElevatorPlugin plugin;

    // Cooldown — prevents double-teleport spam
    private final Set<UUID> cooldown = new HashSet<>();

    // Ground-state tracker — detects the jump moment (was grounded → now moving up)
    private final Set<UUID> wasOnGround = new HashSet<>();

    public ElevatorUseListener(ElevatorPlugin plugin) {
        this.plugin = plugin;
    }

    // -------------------------------------------------------
    // SPACE → go UP
    // Fires every move tick but only acts on the exact moment
    // the player transitions from grounded to moving upward.
    // -------------------------------------------------------
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (event.getTo() == null) return;

        boolean onGround = player.isOnGround();
        boolean movingUp = event.getTo().getY() > event.getFrom().getY();

        if (wasOnGround.contains(uuid) && movingUp && !cooldown.contains(uuid)) {
            Block standingBlock = getStandingBlock(player);
            // FIX 1: check both material AND that it is a registered elevator block
            if (standingBlock != null && isRegisteredElevator(standingBlock)) {
                plugin.getServer().getScheduler().runTask(plugin, () -> handleElevator(player, true));
            }
        }

        if (onGround) wasOnGround.add(uuid);
        else          wasOnGround.remove(uuid);
    }

    // -------------------------------------------------------
    // SHIFT → go DOWN
    // -------------------------------------------------------
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) return;
        Player player = event.getPlayer();
        if (cooldown.contains(player.getUniqueId())) return;
        handleElevator(player, false);
    }

    // -------------------------------------------------------
    // Core teleport logic
    // -------------------------------------------------------
    private void handleElevator(Player player, boolean goingUp) {
        ConfigManager cfg = plugin.getConfigManager();
        if (cfg.getBlockedWorlds().contains(player.getWorld().getName())) return;
        if (!player.hasPermission("elevator.use")) return;

        Block standingBlock = getStandingBlock(player);
        if (standingBlock == null) return;
        // FIX 1: only act if this specific block is a registered elevator
        if (!isRegisteredElevator(standingBlock)) return;

        Block targetElevator = findNextElevator(standingBlock, goingUp, cfg.getMaxFloorDistance());
        if (targetElevator == null) {
            if (cfg.isActionbarEnabled()) sendActionbar(player, cfg.getActionbarNoFloor());
            return;
        }

        Location destination = targetElevator.getLocation().add(0.5, 1.0, 0.5);
        destination.setYaw(player.getLocation().getYaw());
        destination.setPitch(player.getLocation().getPitch());

        addCooldown(player);
        if (cfg.isParticlesEnabled()) spawnParticles(player.getLocation());
        player.teleport(destination);
        if (cfg.isParticlesEnabled()) spawnParticles(destination);
        playSound(player, destination, cfg);
        sendElevatorMessages(player, goingUp, cfg);

        if (cfg.isDebug()) {
            plugin.getLogger().info("[Debug] " + player.getName() + " elevator " +
                    (goingUp ? "UP" : "DOWN") + " Y=" + standingBlock.getY() + " -> Y=" + targetElevator.getY());
        }
    }

    // -------------------------------------------------------
    // Register elevator block on place
    // -------------------------------------------------------
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.isCancelled()) return;
        Player player = event.getPlayer();
        ConfigManager cfg = plugin.getConfigManager();

        if (!plugin.getElevatorManager().isElevatorItem(event.getItemInHand())) return;

        if (cfg.getBlockedWorlds().contains(player.getWorld().getName())) {
            event.setCancelled(true);
            player.sendMessage(cfg.getPrefix() + "§cYou cannot place elevators in this world!");
            return;
        }

        plugin.getDataManager().addElevator(event.getBlockPlaced());
        if (cfg.isDebug())
            plugin.getLogger().info("[Debug] Elevator registered at " + plugin.getDataManager().toKey(event.getBlockPlaced()));
    }

    // -------------------------------------------------------
    // FIX 2 & 3: On break — handle WorldGuard/Plot protection correctly
    // We use MONITOR priority so we run AFTER WorldGuard/PlotSquared have
    // already decided whether to cancel the event.
    // If the event was cancelled (protected region), we do nothing — block stays.
    // If the event was NOT cancelled, we suppress vanilla drop and give elevator item.
    // -------------------------------------------------------
    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        ElevatorDataManager data = plugin.getDataManager();

        if (!data.isElevator(block)) return;

        // FIX 3: if WorldGuard / PlotSquared cancelled the event, respect it —
        // do NOT remove the block from our registry and do NOT drop anything
        if (event.isCancelled()) return;

        // Remove from persistent registry
        data.removeElevator(block);

        // Suppress the default plain wool drop
        event.setDropItems(false);

        // No drop in creative mode
        if (event.getPlayer().getGameMode() == org.bukkit.GameMode.CREATIVE) return;

        // Drop the properly tagged elevator item
        ItemStack elevatorItem = plugin.getElevatorManager().createElevatorItem(1);
        block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5), elevatorItem);

        if (plugin.getConfigManager().isDebug())
            plugin.getLogger().info("[Debug] Elevator broken at " + data.toKey(block) + " — dropped tagged item.");
    }

    // -------------------------------------------------------
    // FIX: Block pistons from pushing or pulling elevator blocks
    // -------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        for (Block block : event.getBlocks()) {
            if (plugin.getDataManager().isElevator(block)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        for (Block block : event.getBlocks()) {
            if (plugin.getDataManager().isElevator(block)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    // -------------------------------------------------------
    // Helpers
    // -------------------------------------------------------

    /**
     * Returns true only if the block is both the right material AND
     * is registered as an elevator in our data file.
     * FIX 1: This prevents any random wool block from acting as an elevator.
     */
    private boolean isRegisteredElevator(Block block) {
        if (block.getType() != plugin.getConfigManager().getElevatorMaterial()) return false;
        return plugin.getDataManager().isElevator(block);
    }

    /**
     * FIX 1: Only returns a floor block if it is a registered elevator block —
     * not just any wool block of the same material.
     */
    private Block findNextElevator(Block from, boolean up, int maxDistance) {
        World world = from.getWorld();
        int x = from.getX(), z = from.getZ(), startY = from.getY();
        Material mat = plugin.getConfigManager().getElevatorMaterial();

        if (up) {
            for (int y = startY + 1; y <= startY + maxDistance && y < world.getMaxHeight(); y++) {
                Block b = world.getBlockAt(x, y, z);
                if (b.getType() == mat && plugin.getDataManager().isElevator(b)) return b;
            }
        } else {
            for (int y = startY - 1; y >= startY - maxDistance && y > world.getMinHeight(); y--) {
                Block b = world.getBlockAt(x, y, z);
                if (b.getType() == mat && plugin.getDataManager().isElevator(b)) return b;
            }
        }
        return null;
    }

    private Block getStandingBlock(Player player) {
        Location loc = player.getLocation();
        World world = loc.getWorld();
        int x = loc.getBlockX(), z = loc.getBlockZ();
        for (int offset = 1; offset <= 2; offset++) {
            int blockY = (int) Math.floor(loc.getY()) - offset;
            Block block = world.getBlockAt(x, blockY, z);
            if (block.getType() != Material.AIR && !block.getType().isTransparent()) return block;
        }
        return null;
    }

    private void spawnParticles(Location loc) {
        ConfigManager cfg = plugin.getConfigManager();
        try {
            loc.getWorld().spawnParticle(cfg.getParticleType(), loc.clone().add(0, 1, 0),
                    cfg.getParticleCount(), cfg.getParticleOffsetX(), cfg.getParticleOffsetY(),
                    cfg.getParticleOffsetZ(), cfg.getParticleSpeed());
        } catch (Exception e) {
            if (cfg.isDebug()) plugin.getLogger().warning("[Debug] Particle error: " + e.getMessage());
        }
    }

    private void playSound(Player player, Location loc, ConfigManager cfg) {
        String soundName = cfg.getTeleportSound();
        if (soundName == null || soundName.isEmpty()) return;
        try {
            player.getWorld().playSound(loc, Sound.valueOf(soundName), cfg.getSoundVolume(), cfg.getSoundPitch());
        } catch (IllegalArgumentException e) {
            if (cfg.isDebug()) plugin.getLogger().warning("[Debug] Invalid sound: " + soundName);
        }
    }

    private void sendElevatorMessages(Player player, boolean goingUp, ConfigManager cfg) {
        if (cfg.isActionbarEnabled())
            sendActionbar(player, goingUp ? cfg.getActionbarUp() : cfg.getActionbarDown());
        if (cfg.isTitleEnabled()) {
            String title    = goingUp ? cfg.getTitleUp()         : cfg.getTitleDown();
            String subtitle = goingUp ? cfg.getTitleSubtitleUp() : cfg.getTitleSubtitleDown();
            player.showTitle(Title.title(
                    Component.text(stripColorCodes(title)).color(net.kyori.adventure.text.format.NamedTextColor.GREEN),
                    Component.text(stripColorCodes(subtitle)),
                    Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(1500), Duration.ofMillis(500))
            ));
        }
    }

    private void sendActionbar(Player player, String message) {
        player.sendActionBar(Component.text(message));
    }

    private String stripColorCodes(String text) {
        return text.replaceAll("§[0-9a-fk-orA-FK-OR]", "");
    }

    private void addCooldown(Player player) {
        cooldown.add(player.getUniqueId());
        plugin.getServer().getScheduler().runTaskLater(plugin,
                () -> cooldown.remove(player.getUniqueId()), 10L);
    }
}
