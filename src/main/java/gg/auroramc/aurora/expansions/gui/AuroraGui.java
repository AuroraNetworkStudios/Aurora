package gg.auroramc.aurora.expansions.gui;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public interface AuroraGui {
    void open(Player player, @Nullable Map<String, String> args);

    default void open(Player player) {
        open(player, null);
    }

    default void dispose() {

    }
}
