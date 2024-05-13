package gg.auroramc.auroralib.api.message;

import gg.auroramc.auroralib.AuroraLib;
import gg.auroramc.auroralib.api.util.Platform;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;

public class ActionBar {
    public static void show(Player player, String msg) {
        send(player, msg);
    }

    public static void send(Player player, String msg) {
        if(AuroraLib.isPAPIEnabled()) {
            Platform.sendActionBar(player, AuroraLib.getMiniMessage().deserialize(Chat.translateToMM(PlaceholderAPI.setPlaceholders(player, msg))));
        } else {
            Platform.sendActionBar(player, AuroraLib.getMiniMessage().deserialize(Chat.translateToMM(msg)));
        }
    }
}
