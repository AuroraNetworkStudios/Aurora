package gg.auroramc.aurora.expansions.placeholder;

import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.api.placeholder.PlaceholderHandler;
import org.bukkit.entity.Player;

import java.util.List;

public class MetaHandler implements PlaceholderHandler {
    @Override
    public String getIdentifier() {
        return "meta";
    }

    @Override
    public String onPlaceholderRequest(Player player, String[] args) {
        var user = Aurora.getUserManager().getUser(player);
        var meta = user.getMetaData();

        if (args.length < 2) return null;

        var key = args[0];
        var dataType = args[1];

        if (!user.isLoaded()) {
            // To ensure that placeholders can be properly used in expressions
            // even if the user isn't loaded yet.
            return dataType.equals("string") ? "" : "0";
        }

        return switch (dataType) {
            case "int" -> String.valueOf(meta.getMeta(key, 0L));
            case "double" -> String.valueOf(meta.getMeta(key, 0.0));
            case "string" -> meta.getMeta(key, "");
            default -> null;
        };

    }

    @Override
    public List<String> getPatterns() {
        return List.of("<key>_int", "<key>_double", "<key>_string");
    }
}
