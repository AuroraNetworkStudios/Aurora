package gg.auroramc.auroralib.message;

import gg.auroramc.auroralib.AuroraLib;
import me.clip.placeholderapi.PlaceholderAPI;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

public class ActionBar {
    public static void show(Player player, String msg) {
        if(AuroraLib.isPAPIEnabled()) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(Chat.translateColorCodes(PlaceholderAPI.setPlaceholders(player, msg))));
        } else {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(Chat.translateColorCodes(msg)));
        }
    }
}
