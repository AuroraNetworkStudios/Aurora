package gg.auroramc.auroralib.api.command;

import gg.auroramc.auroralib.AuroraLib;
import gg.auroramc.auroralib.api.message.Text;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class CommandDispatcher {
    public static void dispatch(Player player, String command) {
        if(command.startsWith("[message]")) {
            var msg = Text.build(removeFirstSpace(command.replace("[message]", "")));
            if(AuroraLib.isPAPIEnabled()) msg = PlaceholderAPI.setPlaceholders(player, msg);
            player.sendMessage(msg);
        } else if(command.startsWith("[player]")) {
            var cmd = removeFirstSpace(command.replace("[player]", ""));
            if(AuroraLib.isPAPIEnabled()) cmd = PlaceholderAPI.setPlaceholders(player, cmd);
            player.performCommand(cmd);
        } else if(command.startsWith("[console]")) {
            var cmd = removeFirstSpace(command.replace("[console]", ""));
            if(AuroraLib.isPAPIEnabled()) cmd = PlaceholderAPI.setPlaceholders(player, cmd);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        } else if (command.startsWith("[close]")) {
            player.closeInventory();
        } else {
            var cmd = AuroraLib.isPAPIEnabled() ? PlaceholderAPI.setPlaceholders(player, command) : command;
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
