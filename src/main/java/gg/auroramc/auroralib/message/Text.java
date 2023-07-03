package gg.auroramc.auroralib.message;

import gg.auroramc.auroralib.AuroraLib;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;

public class Text {

    public static String build(String text, Placeholder... placeholders) {
        return Chat.translateColorCodes(Placeholder.execute(text, placeholders));
    }

    public static String build(Player player, String text, Placeholder... placeholders) {
        var msg = Placeholder.execute(text, placeholders);
        if(AuroraLib.isPAPIEnabled()) {
            msg = PlaceholderAPI.setPlaceholders(player, msg);
        }
        return Chat.translateColorCodes(msg);
    }

}
