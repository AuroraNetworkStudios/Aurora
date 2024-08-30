package gg.auroramc.aurora.expansions.gui;

import org.bukkit.entity.Player;

public interface AuroraGui {
    void open(Player player);
    default void dispose() {

    }
}
