package com.kitsune.upgradablespawners.commands;

import com.kitsune.upgradablespawners.UpgradableSpawners;
import com.kitsune.upgradablespawners.data.SpawnerData;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SpawnerCommand implements CommandExecutor {

    private final UpgradableSpawners plugin;

    public SpawnerCommand(UpgradableSpawners plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // reload works from console too, so handle it before the player-only check.
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("upgradablespawners.admin")) {
                sender.sendMessage(ChatColor.RED + "No permission.");
                return true;
            }
            plugin.reloadConfig();
            plugin.reloadSettings();
            sender.sendMessage(ChatColor.GREEN + "UpgradableSpawners config reloaded. "
                    + ChatColor.GRAY + "(payment mode: " + plugin.getEconomyMode() + ")");
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command is for players only.");
            return true;
        }

        if (!player.hasPermission("upgradablespawners.admin")) {
            player.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }

        Block target = player.getTargetBlockExact(5);
        if (target == null || target.getType() != Material.SPAWNER) {
            player.sendMessage(ChatColor.YELLOW + "Look at a spawner to check its status.");
            return true;
        }

        if (target.getState() instanceof CreatureSpawner spawner) {
            SpawnerData data = plugin.getDataManager().getSpawnerData(target.getLocation());

            player.sendMessage(ChatColor.GOLD + "--- Spawner Status ---");
            player.sendMessage(ChatColor.AQUA + "Radius Level: " + ChatColor.WHITE + data.getRadiusLevel());
            player.sendMessage(ChatColor.AQUA + "Speed Level: " + ChatColor.WHITE + data.getSpeedLevel());
            player.sendMessage(ChatColor.AQUA + "Quantity Level: " + ChatColor.WHITE + data.getQuantityLevel());
            player.sendMessage("");
            player.sendMessage(
                    ChatColor.GREEN + "Required Player Range: " + ChatColor.WHITE + spawner.getRequiredPlayerRange());
            player.sendMessage(ChatColor.GREEN + "Spawn Range: " + ChatColor.WHITE + spawner.getSpawnRange());
            player.sendMessage(ChatColor.GREEN + "Min Spawn Delay: " + ChatColor.WHITE + spawner.getMinSpawnDelay());
            player.sendMessage(ChatColor.GREEN + "Max Spawn Delay: " + ChatColor.WHITE + spawner.getMaxSpawnDelay());
            player.sendMessage(ChatColor.GOLD + "----------------------");

            // Re-sync the physical spawner in case its settings drifted from stored data
            plugin.getDataManager().updateSpawnerSettings(target, data);
        }

        return true;
    }
}
