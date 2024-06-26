package gg.auroramc.aurora.api.command;

import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.api.message.Chat;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Stream;

public class AuroraCommand implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("aurora.core.admin")) {
            return true;
        }
        if (args.length == 0) {
            return true;
        }

        if (args[0].equalsIgnoreCase("meta")) {
            if (args.length < 4) {
                Chat.sendMessage(sender, "&cUsage: /aurora meta <get/set/remove/increment/decrement> <player> <key> [value]");
                return true;
            }

            var player = Bukkit.getPlayer(args[2]);

            if (player == null) {
                Chat.sendMessage(sender, "&cPlayer not found: " + args[2]);
                return true;
            }

            var meta = Aurora.getUserManager().getUser(player).getMetaData();
            var key = args[3];

            switch (args[1]) {
                case "get" -> {
                    // meta get <player> <key>
                    sender.sendMessage(meta.getMeta(key, "No value is set"));
                }
                case "set" -> {
                    // meta set <player> <key> <value>
                    if (args.length < 5) {
                        Chat.sendMessage(sender, "&cUsage: /aurora meta set <player> <key> <value>");
                        return true;
                    }
                    try {
                        var value = Double.parseDouble(args[4]);
                        meta.setMeta(key, value);
                        Chat.sendMessage(sender, "&aSuccessfully set meta key: " + key + " to " + value);
                    } catch (NumberFormatException e) {
                        var value = args[4];
                        meta.setMeta(key, value);
                        Chat.sendMessage(sender, "&aSuccessfully set meta key: " + key + " to " + value);
                    }
                }
                case "remove" -> {
                    // meta remove <player> <key>
                    Chat.sendMessage(sender, meta.removeMeta(key) ? "&aSuccessfully removed meta key: " + key : "&cFailed to remove meta key: " + key);
                }
                case "increment" -> {
                    // meta increment <player> <key> [number:1]
                    try {
                        var value = args.length > 4 ? Double.parseDouble(args[4]) : 1.0;
                        meta.incrementMeta(key, value);
                        Chat.sendMessage(sender, "&aSuccessfully increment meta key: " + key + " by " + value);
                    } catch (NumberFormatException e) {
                        Chat.sendMessage(sender, "&cInvalid value: " + args[4]);
                    }
                }
                case "decrement" -> {
                    // meta decrement <player> <key> [number:1]
                    try {
                        var value = args.length > 4 ? Double.parseDouble(args[4]) : 1.0;
                        meta.decrementMeta(key, value);
                        Chat.sendMessage(sender, "&aSuccessfully decrement meta key: " + key + " by " + value);
                    } catch (NumberFormatException e) {
                        Chat.sendMessage(sender, "&cInvalid value: " + args[4]);
                    }
                }
            }

            return true;
        }

        if (args[0].equalsIgnoreCase("dispatch")) {
            if (args.length < 2) {
                Chat.sendMessage(sender, "&cUsage: /aurora dispatch <player> <command>");
                return true;
            }

            var player = Bukkit.getPlayer(args[1]);

            if (player == null) {
                Chat.sendMessage(sender, "&cPlayer not found: " + args[1]);
                return true;
            }

            var commandString = String.join(" ", args).substring(args[0].length() + args[1].length() + 2);
            CommandDispatcher.dispatch(player, commandString);
            return true;
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("aurora.core.admin")) {
            return List.of();
        }
        if (args.length == 1) {
            return Stream.of("meta", "dispatch").filter(s -> s.startsWith(args[0])).toList();
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("meta")) {
                return Stream.of("get", "set", "remove", "increment", "decrement").filter(s -> s.startsWith(args[1])).toList();
            }
            if (args[0].equalsIgnoreCase("dispatch")) {
                return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
            }
        }

        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("meta")) {
                return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
            }
        }

        if (args.length == 4) {
            if (args[0].equalsIgnoreCase("meta")) {
                return List.of("<key>");
            }
        }

        if (args.length == 5) {
            if (args[0].equalsIgnoreCase("meta") && (args[1].equalsIgnoreCase("set")
                    || args[1].equalsIgnoreCase("increment")
                    || args[1].equalsIgnoreCase("decrement"))) {
                return List.of("<value>");
            }
        }

        return List.of();
    }
}
