package gg.auroramc.aurora.api.message;

import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.api.dependency.Dep;
import gg.auroramc.aurora.api.dependency.DependencyManager;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;

import java.util.List;

public class Text {

    private static Style noItalic = Style.style(TextDecoration.ITALIC.withState(false));

    public static Component removeItalic(Component component) {
        return Component.empty().style(noItalic).append(component);
    }

    public static String build(String text, Placeholder<?>... placeholders) {
        return Chat.translateColorCodes(Placeholder.execute(text, placeholders));
    }

    public static String build(Player player, String text, Placeholder<?>... placeholders) {
        var msg = Placeholder.execute(text, placeholders);
        if(DependencyManager.hasDep(Dep.PAPI)) {
            msg = PlaceholderAPI.setPlaceholders(player, msg);
        }
        return Chat.translateColorCodes(msg);
    }

    public static Component component(Player player, String text, Placeholder<?>... placeholders) {
        var msg = Placeholder.execute(text, placeholders);
        if(DependencyManager.hasDep(Dep.PAPI)) {
            msg = PlaceholderAPI.setPlaceholders(player, msg);
        }
        return removeItalic(Aurora.getMiniMessage().deserialize(Chat.translateToMM(msg)));
    }

    public static Component component(Player player, String text, List<Placeholder<?>> placeholders) {
        var msg = Placeholder.execute(text, placeholders);
        if(DependencyManager.hasDep(Dep.PAPI)) {
            msg = PlaceholderAPI.setPlaceholders(player, msg);
        }
        return removeItalic(Aurora.getMiniMessage().deserialize(Chat.translateToMM(msg)));
    }

    public static Component component(String text, Placeholder<?>... placeholders) {
        var msg = Placeholder.execute(text, placeholders);
        return removeItalic(Aurora.getMiniMessage().deserialize(Chat.translateToMM(msg)));
    }

    public static Component component(String text, List<Placeholder<?>> placeholders) {
        var msg = Placeholder.execute(text, placeholders);
        return removeItalic(Aurora.getMiniMessage().deserialize(Chat.translateToMM(msg)));
    }
}
