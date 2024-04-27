package gg.auroramc.auroralib.api.menu;

import gg.auroramc.auroralib.AuroraLib;
import org.bukkit.Bukkit;

import java.util.HashSet;
import java.util.Set;

public class MenuRefresher {
    private final Set<AuroraMenu> menus = new HashSet<>();

    public MenuRefresher(AuroraLib plugin) {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for(AuroraMenu menu : menus) {
                menu.refresh();
            }
        }, 20L, 20L);
    }

    public void add(AuroraMenu menu) {
        menus.add(menu);
    }

    public void remove(AuroraMenu menu) {
        menus.remove(menu);
    }
}
