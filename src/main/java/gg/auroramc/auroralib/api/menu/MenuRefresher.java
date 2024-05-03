package gg.auroramc.auroralib.api.menu;

import gg.auroramc.auroralib.AuroraLib;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.Set;

public class MenuRefresher {
    private final Set<AuroraMenu> menus = new HashSet<>();
    private BukkitTask refreshTask = null;
    private final AuroraLib plugin;

    public MenuRefresher(AuroraLib plugin) {
        this.plugin = plugin;
    }

    public void add(AuroraMenu menu) {
        menus.add(menu);
        if(refreshTask == null || refreshTask.isCancelled()) {
            refreshTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                for(AuroraMenu m : menus) {
                    m.refresh();
                }
            }, 20L, 20L);
        }
    }

    public void remove(AuroraMenu menu) {
        menus.remove(menu);

        if(menus.isEmpty() && refreshTask != null) {
            refreshTask.cancel();
            refreshTask = null;
        }
    }
}
