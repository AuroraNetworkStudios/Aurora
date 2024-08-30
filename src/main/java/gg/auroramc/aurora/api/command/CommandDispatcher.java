package gg.auroramc.aurora.api.command;

import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.api.dependency.Dep;
import gg.auroramc.aurora.api.dependency.DependencyManager;
import gg.auroramc.aurora.api.message.ActionBar;
import gg.auroramc.aurora.api.message.Placeholder;
import gg.auroramc.aurora.api.message.Text;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandDispatcher {
    public record MetaRecord(String action, String key, String value) {
    }

    public static void dispatch(Player player, String command) {
        if (command.startsWith("[message]")) {
            var msg = Text.component(player, removeFirstSpace(command.replace("[message]", "")));
            player.sendMessage(msg);
        } else if (command.startsWith("[player]")) {
            player.getScheduler().run(Aurora.getInstance(), (task) -> {
                var cmd = removeFirstSpace(command.replace("[player]", ""));
                if (DependencyManager.hasDep(Dep.PAPI)) cmd = PlaceholderAPI.setPlaceholders(player, cmd);
                player.performCommand(cmd);
            }, null);
        } else if (command.startsWith("[console]")) {
            Bukkit.getGlobalRegionScheduler().run(Aurora.getInstance(), task -> {
                var cmd = removeFirstSpace(command.replace("[console]", ""));
                if (DependencyManager.hasDep(Dep.PAPI)) cmd = PlaceholderAPI.setPlaceholders(player, cmd);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            });
        } else if (command.startsWith("[close]")) {
            player.getScheduler().run(Aurora.getInstance(), task -> player.closeInventory(), null);
        } else if (command.startsWith("[meta")) {
            var data = Aurora.getUserManager().getUser(player).getMetaData();
            var meta = parseMetaString(command);

            if (meta.action == null) return;
            if (meta.key == null) return;

            switch (meta.action) {
                case "set" -> {
                    try {
                        var value = Double.parseDouble(meta.value);
                        data.setMeta(meta.key, value);
                    } catch (NumberFormatException e) {
                        data.setMeta(meta.key, meta.value);
                    }
                }
                case "remove" -> data.removeMeta(meta.key);
                case "increment" ->
                        data.incrementMeta(meta.key, meta.value == null ? 1 : Double.parseDouble(meta.value));
                case "decrement" ->
                        data.decrementMeta(meta.key, meta.value == null ? 1 : Double.parseDouble(meta.value));
            }
        } else if (command.startsWith("[placeholder]")) {
            if (DependencyManager.hasDep(Dep.PAPI)) PlaceholderAPI.setPlaceholders(player, command);
        } else if (command.startsWith("[sound]")) {
            playSound(player, removeFirstSpace(command.replace("[sound]", "")));
        } else if(command.startsWith("[actionbar]")) {
            var msg = removeFirstSpace(command.replace("[actionbar]", ""));
            ActionBar.send(player, msg);
        } else {
            Bukkit.getGlobalRegionScheduler().run(Aurora.getInstance(), (task) -> {
                var cmd = DependencyManager.hasDep(Dep.PAPI) ? PlaceholderAPI.setPlaceholders(player, command) : command;
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            });
        }
    }

    public static void dispatch(Player player, String command, List<Placeholder<?>> placeholders) {
        dispatch(player, Placeholder.execute(command, placeholders));
    }

    public static void dispatch(Player player, String command, Placeholder<?>... placeholders) {
        dispatch(player, Placeholder.execute(command, placeholders));
    }

    private static void playSound(Player player, String cmd) {
        String[] args = cmd.split(" ");
        if (args.length == 0) return;
        var sound = Registry.SOUNDS.get(NamespacedKey.fromString(args[0]));
        if (sound == null) {
            Aurora.logger().warning("Invalid sound: " + args[0]);
            return;
        }
        if (args.length == 1) {
            player.playSound(player, sound, 1, 1);
        } else if (args.length == 2) {
            player.playSound(player.getLocation(), args[0], Float.parseFloat(args[1]), 1);
        } else if (args.length == 3) {
            player.playSound(player.getLocation(), args[0], Float.parseFloat(args[1]), Float.parseFloat(args[2]));
        }
    }

    private static MetaRecord parseMetaString(String input) {
        Pattern pattern = Pattern.compile("\\[meta:(set|remove|increment|decrement):([a-zA-Z0-9_-]+)]\\s*(.*)");
        Matcher matcher = pattern.matcher(input);

        if (matcher.matches()) {
            String action = matcher.group(1);
            String key = matcher.group(2);
            String value = matcher.group(3).isEmpty() ? null : matcher.group(3);

            return new MetaRecord(action, key, value);
        } else {
            return new MetaRecord(null, null, null);
        }
    }

    private static String removeFirstSpace(String text) {
        if (text.startsWith(" ")) {
            return text.substring(1);
        }
        return text;
    }
}
