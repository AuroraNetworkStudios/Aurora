package gg.auroramc.aurora.expansions.placeholder;

import gg.auroramc.aurora.api.message.Chat;
import gg.auroramc.aurora.api.message.Text;
import gg.auroramc.aurora.api.placeholder.PlaceholderHandler;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

import java.util.List;

public class ColorHandler implements PlaceholderHandler {
    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.builder()
            .hexColors().useUnusualXRepeatedCharacterHexFormat().build();

    @Override
    public String getIdentifier() {
        return "color";
    }

    @Override
    public String onPlaceholderRequest(Player player, String[] args) {
        var filled = PlaceholderAPI.setBracketPlaceholders(player, String.join("_", args));
        return serializer.serialize(Text.component(Chat.translateEverythingToMiniMessage(filled)));
    }

    @Override
    public List<String> getPatterns() {
        return List.of("<placeholders>");
    }
}
