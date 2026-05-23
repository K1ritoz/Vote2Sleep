package com.github.k1ritoz.vote2Sleep.commands;

import com.github.k1ritoz.vote2Sleep.Vote2Sleep;
import com.github.k1ritoz.vote2Sleep.utils.PluginMetadata;
import com.github.k1ritoz.vote2Sleep.utils.WorldIdentity;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SleepCommand implements CommandExecutor, TabCompleter {

    private final Vote2Sleep plugin;

    public SleepCommand(Vote2Sleep plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (args.length == 0) {
            if (sender instanceof Player) {
                handleVoteCommand(sender);
            } else {
                sendHelpMessage(sender);
            }
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "vote":
            case "v":
                return handleVoteCommand(sender);

            case "status":
            case "s":
                return handleStatusCommand(sender);

            case "enable":
                return handleEnableCommand(sender);

            case "disable":
                return handleDisableCommand(sender);

            case "reload":
                return handleReloadCommand(sender);

            case "stats":
                return handleStatsCommand(sender);

            case "help":
            case "?":
                sendHelpMessage(sender);
                return true;

            case "cancel":
                return handleCancelCommand(sender);

            case "force":
                return handleForceCommand(sender);

            case "config":
                return handleConfigCommand(sender, args);

            case "language":
            case "lang":
                return handleLanguageCommand(sender, args);

            default:
                sendHelpMessage(sender);
                return true;
        }
    }

    private boolean handleVoteCommand(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }

        plugin.getVoteManager().startSleepVote(player);
        return true;
    }

    private Player requirePlayer(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players!");
            return null;
        }

        return (Player) sender;
    }

    private void sendLocalizedMessage(CommandSender sender, String key) {
        if (sender instanceof Player) {
            plugin.getMessageManager().sendMessage((Player) sender, key);
        } else {
            sender.sendMessage(plugin.getMessageManager().getMessage(key));
        }
    }

    private boolean handleLanguageCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("vote2sleep.admin")) {
            sendLocalizedMessage(sender, "no-permission");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§7Current language: §e" + plugin.getMessageManager().getCurrentLanguage());
            sender.sendMessage("§7Available languages: §aen§7, §apt_br");
            sender.sendMessage("§7Usage: §e/" + args[0] + " language <en|pt_br>");
            return true;
        }

        String newLanguage = args[1].toLowerCase();

        if (!newLanguage.equals("en") && !newLanguage.equals("pt_br")) {
            sender.sendMessage("§cInvalid language! Available: en, pt_br");
            return true;
        }

        if (plugin.getMessageManager().changeLanguage(newLanguage)) {
            sender.sendMessage("§aLanguage changed to: " + newLanguage);
            sender.sendMessage("§7Note: This change is temporary. Update config.yml to make it permanent.");
        } else {
            sender.sendMessage("§cFailed to change language to: " + newLanguage);
        }

        return true;
    }

    private boolean handleStatusCommand(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }

        World world = player.getWorld();

        if (!plugin.getConfigManager().isWorldEnabled(world)) {
            plugin.getMessageManager().sendMessage(player, "world-not-enabled");
            return true;
        }

        int currentVotes = plugin.getApi().getCurrentVotes(world);
        int requiredVotes = plugin.getApi().getRequiredVotes(world);
        boolean hasVoted = plugin.getApi().hasPlayerVoted(player);
        boolean canSleep = plugin.getApi().canSleep(world);
        double progress = plugin.getApi().getVoteProgress(world);

        player.sendMessage(plugin.getMessageManager().getMessage("status.header"));
        player.sendMessage(plugin.getMessageManager().getMessage("status.world",
                Map.of("world", WorldIdentity.key(world))));
        player.sendMessage(plugin.getMessageManager().getMessage("status.current-votes",
                Map.of("current", String.valueOf(currentVotes), "required", String.valueOf(requiredVotes))));
        player.sendMessage(plugin.getMessageManager().getMessage("status.you-voted",
                Map.of("status", hasVoted ? plugin.getMessageManager().getMessage("status-yes") :
                        plugin.getMessageManager().getMessage("status-no"))));
        player.sendMessage(plugin.getMessageManager().getMessage("status.can-sleep",
                Map.of("status", canSleep ? plugin.getMessageManager().getMessage("status-yes") :
                        plugin.getMessageManager().getMessage("status-no"))));
        player.sendMessage(plugin.getMessageManager().getMessage("status.progress",
                Map.of("progress", String.valueOf(Math.round(progress * 100)))));

        return true;
    }

    private boolean handleEnableCommand(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }

        if (!sender.hasPermission("vote2sleep.enable")) {
            sendLocalizedMessage(sender, "no-permission");
            return true;
        }

        World world = player.getWorld();

        if (plugin.getConfigManager().isWorldEnabled(world)) {
            plugin.getMessageManager().sendMessage(player, "already-enabled");
        } else {
            plugin.getConfigManager().enableWorld(world);
            plugin.getMessageManager().sendMessage(player, "world-enabled",
                    Map.of("world", WorldIdentity.key(world)));
        }

        return true;
    }

    private boolean handleDisableCommand(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }

        if (!sender.hasPermission("vote2sleep.disable")) {
            sendLocalizedMessage(sender, "no-permission");
            return true;
        }

        World world = player.getWorld();

        if (!plugin.getConfigManager().isWorldEnabled(world)) {
            plugin.getMessageManager().sendMessage(player, "already-disabled");
        } else {
            plugin.getConfigManager().disableWorld(world);
            // Clear any existing votes
            plugin.getVoteManager().clearVotes(world);
            plugin.getMessageManager().sendMessage(player, "world-disabled",
                    Map.of("world", WorldIdentity.key(world)));
        }

        return true;
    }

    private boolean handleReloadCommand(CommandSender sender) {
        if (!sender.hasPermission("vote2sleep.reload")) {
            sendLocalizedMessage(sender, "no-permission");
            return true;
        }

        plugin.reloadPlugin();
        plugin.getVoteManager().synchronizeBossBarSettings();

        sendLocalizedMessage(sender, "reload-success");
        return true;
    }

    private boolean handleStatsCommand(CommandSender sender) {
        if (!sender.hasPermission("vote2sleep.stats")) {
            sendLocalizedMessage(sender, "no-permission");
            return true;
        }

        // Display plugin statistics
        sender.sendMessage(plugin.getMessageManager().getMessage("stats.header"));
        sender.sendMessage(plugin.getMessageManager().getMessage("stats.plugin-version",
                Map.of("version", PluginMetadata.getVersion(plugin))));
        sender.sendMessage(plugin.getMessageManager().getMessage("stats.platform",
                Map.of("platform", plugin.getPlatformAdapter().getPlatformName())));
        sender.sendMessage(plugin.getMessageManager().getMessage("stats.enabled-worlds",
                Map.of("count", String.valueOf(plugin.getConfigManager().getEnabledWorlds().size()))));

        double[] tps = plugin.getPlatformAdapter().getTPS();
        sender.sendMessage(plugin.getMessageManager().getMessage("stats.server-tps",
                Map.of("tps", String.format("%.2f", tps[0]))));

        String dbStatus = plugin.getConfigManager().isDatabaseEnabled() ?
                plugin.getMessageManager().getMessage("status-yes") :
                plugin.getMessageManager().getMessage("status-no");
        sender.sendMessage(plugin.getMessageManager().getMessage("stats.database-status",
                Map.of("status", dbStatus)));

        return true;
    }

    private boolean handleCancelCommand(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }

        if (!sender.hasPermission("vote2sleep.cancel")) {
            sendLocalizedMessage(sender, "no-permission");
            return true;
        }

        World world = player.getWorld();

        plugin.getVoteManager().clearVotes(world);
        plugin.getMessageManager().sendWorldMessage(world, "votes-cancelled-by-admin",
                Map.of("admin", player.getName()));

        return true;
    }

    private boolean handleForceCommand(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }

        if (!sender.hasPermission("vote2sleep.force")) {
            sendLocalizedMessage(sender, "no-permission");
            return true;
        }

        World world = player.getWorld();

        if (!plugin.getConfigManager().isWorldEnabled(world)) {
            plugin.getMessageManager().sendMessage(player, "world-not-enabled");
            return true;
        }

        // Force skip night/storm
        plugin.getVoteManager().forceSkip(world, player);

        return true;
    }

    private boolean handleConfigCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("vote2sleep.config")) {
            sendLocalizedMessage(sender, "no-permission");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§cUsage: /" + args[0] + " config <setting> [value]");
            sender.sendMessage("§7Available settings: vote-percentage, min-players, max-players");
            return true;
        }

        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }

        World world = player.getWorld();
        String setting = args[1].toLowerCase();

        if (args.length == 2) {
            // Show current value
            switch (setting) {
                case "vote-percentage":
                    double percentage = plugin.getConfigManager().getVotePercentage(world);
                    sender.sendMessage("§7Vote percentage for §e" + WorldIdentity.key(world) + "§7: §a" + (percentage * 100) + "%");
                    break;
                case "min-players":
                    int min = plugin.getConfigManager().getMinimumPlayers();
                    sender.sendMessage("§7Minimum players: §a" + min);
                    break;
                case "max-players":
                    int max = plugin.getConfigManager().getMaximumPlayers();
                    sender.sendMessage("§7Maximum players: §a" + (max == -1 ? "unlimited" : max));
                    break;
                default:
                    sender.sendMessage("§cUnknown setting: " + setting);
                    break;
            }
        } else {
            // Set value
            String value = args[2];
            switch (setting) {
                case "vote-percentage":
                    try {
                        double percentage = Double.parseDouble(value);
                        if (percentage < 0.1 || percentage > 1.0) {
                            sender.sendMessage("§cVote percentage must be between 0.1 and 1.0");
                            return true;
                        }
                        plugin.getConfigManager().setVotePercentage(world, percentage);
                        sender.sendMessage("§aVote percentage for §e" + WorldIdentity.key(world) + "§a set to §e" + (percentage * 100) + "%");
                    } catch (NumberFormatException e) {
                        sender.sendMessage("§cInvalid number: " + value);
                    }
                    break;
                default:
                    sender.sendMessage("§cSetting §e" + setting + "§c cannot be modified in-game");
                    break;
            }
        }

        return true;
    }

    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(plugin.getMessageManager().getMessage("help.header"));
        sender.sendMessage(plugin.getMessageManager().getMessage("help.vote"));
        sender.sendMessage(plugin.getMessageManager().getMessage("help.status"));

        if (sender.hasPermission("vote2sleep.admin")) {
            sender.sendMessage(plugin.getMessageManager().getMessage("help.enable"));
            sender.sendMessage(plugin.getMessageManager().getMessage("help.disable"));
            sender.sendMessage(plugin.getMessageManager().getMessage("help.reload"));
        }

        if (sender.hasPermission("vote2sleep.stats")) {
            sender.sendMessage(plugin.getMessageManager().getMessage("help.stats"));
        }

        if (sender.hasPermission("vote2sleep.cancel")) {
            sender.sendMessage(plugin.getMessageManager().getMessage("help.cancel"));
        }

        if (sender.hasPermission("vote2sleep.force")) {
            sender.sendMessage(plugin.getMessageManager().getMessage("help.force"));
        }

        if (sender.hasPermission("vote2sleep.admin")) {
            sender.sendMessage("§e/sleep language <en|pt_br> §7- Change language");
        }

        sender.sendMessage(plugin.getMessageManager().getMessage("help.help"));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("vote", "status", "help");

            if (sender.hasPermission("vote2sleep.admin")) {
                subCommands = new ArrayList<>(subCommands);
                subCommands.addAll(Arrays.asList("enable", "disable", "reload"));
            }

            if (sender.hasPermission("vote2sleep.stats")) {
                subCommands = new ArrayList<>(subCommands);
                subCommands.add("stats");
            }

            if (sender.hasPermission("vote2sleep.cancel")) {
                subCommands = new ArrayList<>(subCommands);
                subCommands.add("cancel");
            }

            if (sender.hasPermission("vote2sleep.force")) {
                subCommands = new ArrayList<>(subCommands);
                subCommands.add("force");
            }

            if (sender.hasPermission("vote2sleep.config")) {
                subCommands = new ArrayList<>(subCommands);
                subCommands.add("config");
                subCommands.add("language");
            }

            return subCommands.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("config")) {
            return Stream.of("vote-percentage", "min-players", "max-players")
                    .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("language")) {
            return Stream.of("en", "pt_br")
                    .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return completions;
    }
}