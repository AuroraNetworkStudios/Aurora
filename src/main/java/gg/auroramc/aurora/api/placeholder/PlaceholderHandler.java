package gg.auroramc.aurora.api.placeholder;

import org.bukkit.entity.Player;

import java.util.List;

public interface PlaceholderHandler {
    String getIdentifier();
    String onPlaceholderRequest(Player player, String[] args);
    List<String> getPatterns();
}
