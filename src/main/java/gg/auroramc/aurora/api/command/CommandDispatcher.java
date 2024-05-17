package gg.auroramc.aurora.api.command;

import gg.auroramc.aurora.api.dependency.Dep;
import gg.auroramc.aurora.api.dependency.DependencyManager;
import gg.auroramc.aurora.api.message.Text;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class CommandDispatcher {
    public static void dispatch(Player player, String command) {
        if(command.startsWith("[message]")) {
            var msg = Text.component(player, removeFirstSpace(command.replace("[message]", "")));
            player.sendMessage(msg);
        } else if(command.startsWith("[player]")) {
            var cmd = removeFirstSpace(command.replace("[player]", ""));
            if(DependencyManager.hasDep(Dep.PAPI)) cmd = PlaceholderAPI.setPlaceholders(player, cmd);
            player.performCommand(cmd);
        } else if(command.startsWith("[console]")) {
            var cmd = removeFirstSpace(command.replace("[console]", ""));
            if(DependencyManager.hasDep(Dep.PAPI)) cmd = PlaceholderAPI.setPlaceholders(player, cmd);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        } else if (command.startsWith("[close]")) {
            player.closeInventory();
        } else if (command.startsWith("[placeholder]")) {
            if(DependencyManager.hasDep(Dep.PAPI)) PlaceholderAPI.setPlaceholders(player, command);
        } else {
            var cmd = DependencyManager.hasDep(Dep.PAPI) ? PlaceholderAPI.setPlaceholders(player, command) : command;
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        }
    }

    private static String removeFirstSpace(String text) {
        if(text.startsWith(" ")) {
            return text.substring(1);
        }
        return text;
    }
}
