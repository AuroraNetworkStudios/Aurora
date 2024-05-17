package gg.auroramc.auroralib.expansions.placeholder;

import gg.auroramc.auroralib.AuroraLib;
import gg.auroramc.auroralib.api.placeholder.PlaceholderHandler;
import org.bukkit.entity.Player;

import java.util.List;

public class MetaHandler implements PlaceholderHandler {
    @Override
    public String getIdentifier() {
        return "meta";
    }

    @Override
    public String onPlaceholderRequest(Player player, String[] args) {
        var user = AuroraLib.getUserManager().getUser(player);
        var meta = user.getMetaData();

        if (args.length < 2) return null;

        var key = args[0];
        var dataType = args[1];

        if (!user.isLoaded()) {
            // To ensure that placeholders can be properly used in expressions
            // even if the user isn't loaded yet.
            return dataType.equals("string") ? "" : "0";
        }

        if (dataType.equals("int")) {
            return String.valueOf(meta.getMeta(key, 0L));
        } else if (dataType.equals("double")) {
            return String.valueOf(meta.getMeta(key, 0.0));
        } else if (dataType.equals("string")) {
            return meta.getMeta(key, "");
        }

        return null;
    }

    @Override
    public List<String> getPatterns() {
        return List.of("<key>_int", "<key>_double", "<key>_string");
    }
}
