package gg.auroramc.aurora.api.message;

import gg.auroramc.aurora.Aurora;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public class ActionBar {
    public static void show(Player player, String msg, Placeholder<?>... placeholders) {
        send(player, msg, placeholders);
    }

    public static void send(Player player, String msg, Placeholder<?>... placeholders) {
        player.getScheduler().run(Aurora.getInstance(),
                (task) -> player.sendActionBar(Text.component(player, msg, placeholders)), null);
    }

    public static void send(Player player, Component component) {
        player.getScheduler().run(Aurora.getInstance(),
                (task) -> player.sendActionBar(component), null);
    }
}
