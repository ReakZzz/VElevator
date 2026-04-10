package com.elevatorplugin.commands;

import com.elevatorplugin.ElevatorPlugin;
import com.elevatorplugin.managers.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ElevatorCommand implements CommandExecutor, TabCompleter {

    private final ElevatorPlugin plugin;

    public ElevatorCommand(ElevatorPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        ConfigManager cfg = plugin.getConfigManager();

        if (!sender.hasPermission("elevator.admin")) {
            sender.sendMessage(cfg.getPrefix() + cfg.getNoPermission());
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {

            case "reload" -> {
                plugin.reload();
                sender.sendMessage(cfg.getPrefix() + cfg.getReloadSuccess());
            }

            case "give" -> {
                handleGive(sender, args, cfg);
            }

            default -> sendHelp(sender);
        }

        return true;
    }

    private void handleGive(CommandSender sender, String[] args, ConfigManager cfg) {
        // /elevator give [player] [amount]
        Player target;
        int amount = 1;

        if (args.length >= 2) {
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(cfg.getPrefix() + cfg.getPlayerNotFound());
                return;
            }
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            sender.sendMessage(cfg.getPrefix() + "§cConsole must specify a player: /elevator give <player> [amount]");
            return;
        }

        if (args.length >= 3) {
            try {
                amount = Integer.parseInt(args[2]);
                if (amount < 1) amount = 1;
                if (amount > 64) amount = 64;
            } catch (NumberFormatException e) {
                sender.sendMessage(cfg.getPrefix() + "§cInvalid amount. Must be a number between 1 and 64.");
                return;
            }
        }

        ItemStack elevatorItem = plugin.getElevatorManager().createElevatorItem(amount);
        target.getInventory().addItem(elevatorItem);

        String msg = cfg.getGiveSuccess()
                .replace("{amount}", String.valueOf(amount))
                .replace("{player}", target.getName());
        sender.sendMessage(cfg.getPrefix() + msg);

        if (!sender.getName().equals(target.getName())) {
            target.sendMessage(cfg.getPrefix() + "§aYou received §e" + amount + " §aElevator block(s)!");
        }
    }

    private void sendHelp(CommandSender sender) {
        String prefix = plugin.getConfigManager().getPrefix();
        sender.sendMessage(prefix + "§e§lSimple Elevators - Commands");
        sender.sendMessage(prefix + "§7/elevator reload §8- §fReload the config");
        sender.sendMessage(prefix + "§7/elevator give [player] [amount] §8- §fGive elevator blocks");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("elevator.admin")) return new ArrayList<>();

        if (args.length == 1) {
            return filterStartsWith(Arrays.asList("reload", "give"), args[0]);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            List<String> players = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                players.add(p.getName());
            }
            return filterStartsWith(players, args[1]);
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            return Arrays.asList("1", "16", "32", "64");
        }

        return new ArrayList<>();
    }

    private List<String> filterStartsWith(List<String> list, String prefix) {
        List<String> result = new ArrayList<>();
        for (String s : list) {
            if (s.toLowerCase().startsWith(prefix.toLowerCase())) {
                result.add(s);
            }
        }
        return result;
    }
}
