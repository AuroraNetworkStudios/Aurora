package gg.auroramc.aurora.api.message;

import gg.auroramc.aurora.hooks.AuraSkillsHook;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public class ActionBar {
    public static void show(Player player, String msg, Placeholder<?>... placeholders) {
        send(player, msg, placeholders);
    }

    public static void send(Player player, String msg, Placeholder<?>... placeholders) {
        send(player, Text.component(player, msg, placeholders));
    }

    public static void send(Player player, Component component) {
        if (AuraSkillsHook.isEnabled()) {
            AuraSkillsHook.pauseActionBar(player);
        }
        player.sendActionBar(component);
    }
}
