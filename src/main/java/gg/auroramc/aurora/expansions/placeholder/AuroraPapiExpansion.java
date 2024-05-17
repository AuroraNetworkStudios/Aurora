package gg.auroramc.aurora.expansions.placeholder;

import gg.auroramc.aurora.api.placeholder.PlaceholderHandlerRegistry;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class AuroraPapiExpansion extends PlaceholderExpansion {
    @Override
    public @NotNull String getIdentifier() {
        return PlaceholderHandlerRegistry.getGlobalId();
    }

    @Override
    public @NotNull String getAuthor() {
        return "auroramc";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0";
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        return PlaceholderHandlerRegistry.fillPlaceholderRequest(player, params);
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @NotNull List<String> getPlaceholders() {
        return PlaceholderHandlerRegistry.getPatterns();
    }
}
