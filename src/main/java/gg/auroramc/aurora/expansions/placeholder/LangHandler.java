package gg.auroramc.aurora.expansions.placeholder;

import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.api.placeholder.PlaceholderHandler;
import org.bukkit.entity.Player;

import java.util.List;

public class LangHandler implements PlaceholderHandler {
    @Override
    public String getIdentifier() {
        return "lang";
    }

    @Override
    public String onPlaceholderRequest(Player player, String[] args) {
        return Aurora.getLocalizationProvider().fillVariables(player, "{{" + String.join("_", args) + "}}");
    }

    @Override
    public List<String> getPatterns() {
        return List.of();
    }
}
