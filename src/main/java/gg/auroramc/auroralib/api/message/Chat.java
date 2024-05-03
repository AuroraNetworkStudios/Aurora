package gg.auroramc.auroralib.api.message;

import gg.auroramc.auroralib.AuroraLib;

import gg.auroramc.auroralib.api.util.TextUtil;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Chat {
    private static final String[] LEGACY_CODES = {
            "&0", "&1", "&2", "&3", "&4", "&5", "&6", "&7", "&8", "&9",
            "&a", "&b", "&c", "&d", "&e", "&f",
            "&k", "&l", "&m", "&n", "&o", "&r"
    };
    private static final String[] MINI_MESSAGE_CODES = {
            "<black>", "<dark_blue>", "<dark_green>", "<dark_aqua>", "<dark_red>", "<dark_purple>", "<gold>", "<gray>", "<dark_gray>", "<blue>",
            "<green>", "<aqua>", "<red>", "<light_purple>", "<yellow>", "<white>",
            "<obfuscated>", "<bold>", "<strikethrough>", "<underline>", "<italic>", "<reset>"
    };

    /**
     * Sends a translated color message to a player.
     *
     * @param player  The player to send the message to.
     * @param message The message to be sent, possibly containing color codes.
     */
    public static void sendMessage(Player player, String message, Placeholder<?>... placeholders) {
        message = Placeholder.execute(message, placeholders);
        if(AuroraLib.isPAPIEnabled())
            message = PlaceholderAPI.setPlaceholders(player, message);
        player.sendMessage(AuroraLib.getMiniMessage().deserialize(translateToMM(message)));
    }

    /**
     * Sends a translated color message to a command sender.
     *
     * @param sender  The command sender to send the message to.
     * @param message The message to be sent, possibly containing color codes.
     */
    public static void sendMessage(CommandSender sender, String message, Placeholder<?>... placeholders) {
        message = Placeholder.execute(message, placeholders);
        if(AuroraLib.isPAPIEnabled() && sender instanceof Player player)
            message = PlaceholderAPI.setPlaceholders(player, message);
        sender.sendMessage(AuroraLib.getMiniMessage().deserialize(translateToMM(message)));
    }

    /**
     * Sends a translated color message to a HumanEntity.
     *
     * @param entity       The HumanEntity to send the message to.
     * @param message The message to be sent, possibly containing color codes.
     */
    public static void sendMessage(HumanEntity entity, String message, Placeholder<?>... placeholders) {
        message = Placeholder.execute(message, placeholders);
        if(AuroraLib.isPAPIEnabled() && entity instanceof Player player)
            message = PlaceholderAPI.setPlaceholders(player, message);
        entity.sendMessage(AuroraLib.getMiniMessage().deserialize(translateToMM(message)));
    }

    public static String translateColorCodes(String text) {
        String message = text;

        Pattern hexPattern = Pattern.compile("&#([A-Fa-f0-9]{6})");
        Matcher matcher = hexPattern.matcher(message);

        while (matcher.find()) {
            String colorCode = matcher.group(1);
            StringBuilder replacement = new StringBuilder(ChatColor.COLOR_CHAR + "x");
            for (char c : colorCode.toCharArray()) {
                replacement.append(ChatColor.COLOR_CHAR).append(c);
            }
            message = message.replace("&#" + colorCode, replacement.toString());
        }

        Pattern bracketHexPattern = Pattern.compile("\\{#([A-Fa-f0-9]{6})\\}");
        matcher = bracketHexPattern.matcher(message);

        while (matcher.find()) {
            String colorCode = matcher.group(1);
            StringBuilder replacement = new StringBuilder(ChatColor.COLOR_CHAR + "x");
            for (char c : colorCode.toCharArray()) {
                replacement.append(ChatColor.COLOR_CHAR).append(c);
            }
            message = message.replace("{#" + colorCode + "}", replacement);
        }

        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public static String legacyCodesToMiniMessage(String input) {
        return TextUtil.replaceEach(input, LEGACY_CODES, MINI_MESSAGE_CODES, false, 0);
    }

    public static String translateToMM(String text) {
        String message = text;

        Pattern hexPattern = Pattern.compile("&#([A-Fa-f0-9]{6})");
        Matcher matcher = hexPattern.matcher(message);

        while (matcher.find()) {
            String colorCode = matcher.group(1);
            message = message.replace("&#" + colorCode, "<#" + colorCode + ">");
        }

        Pattern bracketHexPattern = Pattern.compile("\\{#([A-Fa-f0-9]{6})\\}");
        matcher = bracketHexPattern.matcher(message);

        while (matcher.find()) {
            String colorCode = matcher.group(1);
            message = message.replace("{#" + colorCode + "}", "<#" + colorCode + ">");
        }

        return legacyCodesToMiniMessage(message);
    }
}
