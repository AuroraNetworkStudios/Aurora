package gg.auroramc.auroralib.api.message;

import org.bukkit.entity.Player;

public class ActionBar {
    public static void show(Player player, String msg, Placeholder<?>... placeholders) {
        send(player, msg, placeholders);
    }

    public static void send(Player player, String msg, Placeholder<?>... placeholders) {
        player.sendActionBar(Text.component(player, msg, placeholders));
    }
}
