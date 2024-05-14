package gg.auroramc.auroralib.api.menu;

import gg.auroramc.auroralib.AuroraLib;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class MenuRefresher {
    private final Set<AuroraMenu> menus = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private ScheduledTask refreshTask = null;
    private final AuroraLib plugin;

    public MenuRefresher(AuroraLib plugin) {
        this.plugin = plugin;
    }

    public void add(AuroraMenu menu) {
        menus.add(menu);
        if(refreshTask == null || refreshTask.isCancelled()) {
            refreshTask = Bukkit.getAsyncScheduler().runAtFixedRate(plugin, (task) -> {
                for(AuroraMenu m : menus) {
                    m.refresh();
                }
            }, 1, 1, TimeUnit.SECONDS);
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
