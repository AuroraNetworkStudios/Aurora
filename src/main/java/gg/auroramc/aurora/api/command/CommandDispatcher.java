package gg.auroramc.aurora.api.command;

import com.google.common.collect.Maps;
import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.api.dependency.Dep;
import gg.auroramc.aurora.api.dependency.DependencyManager;
import gg.auroramc.aurora.api.message.ActionBar;
import gg.auroramc.aurora.api.message.Placeholder;
import gg.auroramc.aurora.api.message.Text;
import gg.auroramc.aurora.expansions.gui.GuiExpansion;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Player;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class CommandDispatcher {
    private static final Map<String, BiConsumer<Player, String>> actions = Maps.newConcurrentMap();

    static {
        registerActionHandler("default", CommandDispatcher::runConsoleCommand);
        registerActionHandler("console", CommandDispatcher::runConsoleCommand);
        registerActionHandler("player", CommandDispatcher::runPlayerCommand);

        registerActionHandler("message", (player, message) -> player.sendMessage(Text.component(player, message)));
        registerActionHandler("actionbar", ActionBar::send);
        registerActionHandler("sound", CommandDispatcher::playSound);

        registerActionHandler("close", (player, command) -> player.getScheduler().run(Aurora.getInstance(), task -> player.closeInventory(), null));
        registerActionHandler("open-gui", (player, gui) -> Aurora.getExpansionManager().getExpansion(GuiExpansion.class).openGui(gui, player));

        registerActionHandler("placeholder", (player, placeholder) -> {
            if (DependencyManager.hasDep(Dep.PAPI)) PlaceholderAPI.setPlaceholders(player, placeholder);
        });
    }

    public static void registerActionHandler(String id, BiConsumer<Player, String> handler) {
        actions.put(id, handler);
    }

    public static Collection<String> getActions() {
        return actions.keySet();
    }

    private static Map.Entry<String, String> extractActionAndContent(String input) {
        if (input.charAt(0) == '[') {
            int end = input.indexOf(']');
            if (end != -1) {
                String action = input.substring(1, end);
                String content = removeFirstSpace(input.substring(end + 1));

                return new AbstractMap.SimpleEntry<>(action, content);
            }
        }
        return new AbstractMap.SimpleEntry<>("default", input);
    }

    public record MetaRecord(String action, String key, String value) {
    }

    public static void dispatch(Player player, String command) {
        if (command.startsWith("[meta")) {
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
            return;
        }

        var action = extractActionAndContent(command);
        var handler = actions.get(action.getKey());

        if (handler != null) {
            try {
                handler.accept(player, action.getValue());
            } catch (Exception e) {
                Aurora.logger().severe("Failed to execute action: " + action.getKey() + " with content: " + action.getValue() + " for player: " + player.getName() + " with error: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            Aurora.logger().warning("Invalid dispatcher action: " + action.getKey());
        }
    }

    public static void dispatch(Player player, String command, List<Placeholder<?>> placeholders) {
        dispatch(player, Placeholder.execute(command, placeholders));
    }

    public static void dispatch(Player player, String command, Placeholder<?>... placeholders) {
        dispatch(player, Placeholder.execute(command, placeholders));
    }

    private static void runConsoleCommand(Player player, String command) {
        Bukkit.getGlobalRegionScheduler().run(Aurora.getInstance(), (task) -> {
            var cmd = DependencyManager.hasDep(Dep.PAPI) ? PlaceholderAPI.setPlaceholders(player, command) : command;
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        });
    }

    private static void runPlayerCommand(Player player, String command) {
        player.getScheduler().run(Aurora.getInstance(), (task) -> {
            var cmd = DependencyManager.hasDep(Dep.PAPI) ? PlaceholderAPI.setPlaceholders(player, command) : command;
            player.performCommand(cmd);
        }, null);
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
