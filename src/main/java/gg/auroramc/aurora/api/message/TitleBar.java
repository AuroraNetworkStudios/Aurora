package gg.auroramc.aurora.api.message;

import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;

import java.util.List;

public class TitleBar {
    public static void send(Player player, String title, String subtitle, List<Placeholder<?>> placeholders) {
        player.showTitle(Title.title(Text.component(player, title, placeholders), Text.component(player, subtitle, placeholders)));
    }
}
