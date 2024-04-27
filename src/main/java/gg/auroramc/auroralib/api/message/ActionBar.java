package gg.auroramc.auroralib.api.message;

import gg.auroramc.auroralib.AuroraLib;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;

public class ActionBar {
    public static void show(Player player, String msg) {
        send(player, msg);
    }

    public static void send(Player player, String msg) {
        if(AuroraLib.isPAPIEnabled()) {
            player.sendActionBar(AuroraLib.getMiniMessage().deserialize(Chat.translateColorCodes(PlaceholderAPI.setPlaceholders(player, msg))));
        } else {
            player.sendActionBar(AuroraLib.getMiniMessage().deserialize(Chat.translateColorCodes(msg)));
        }
    }
}
