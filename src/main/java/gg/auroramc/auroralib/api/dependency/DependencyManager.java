package gg.auroramc.auroralib.api.dependency;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;


public class DependencyManager {
    public static boolean hasDep(Dep dep) {
        return Bukkit.getPluginManager().getPlugin(dep.getId()) != null;
    }

    public static boolean hasDep(String dep) {
        return Bukkit.getPluginManager().getPlugin(dep) != null;
    }

    public static boolean isEnabled(Dep dep) {
        return Bukkit.getPluginManager().isPluginEnabled(dep.getId());
    }

    public static boolean isEnabled(String dep) {
        return Bukkit.getPluginManager().isPluginEnabled(dep);
    }

    public static EssentialsAdapter getEssentials() {
        return EssentialsAdapter.getInstance();
    }

    public static WorldGuardPlugin getWorldGuard() {
        var wg = Bukkit.getPluginManager().getPlugin("WorldGuard");
        if(wg != null) {
            return (WorldGuardPlugin) wg;
        }
        return null;
    }

    public static <T extends JavaPlugin> T getPlugin(Class<T> clazz, String name) {
        var plugin = Bukkit.getPluginManager().getPlugin(name);
        if(plugin != null) {
            return clazz.cast(plugin);
        }
        return null;
    }

    public static boolean hasAnyDep(Dep... deps) {
        for(var dep : deps) {
            if(Bukkit.getPluginManager().getPlugin(dep.getId()) != null) return true;
        }
        return false;
    }

    public static boolean hasAnyDep(String... deps) {
        for(var dep : deps) {
            if(Bukkit.getPluginManager().getPlugin(dep) != null) return true;
        }
        return false;
    }

    public static boolean hasEveryDep(Dep... deps) {
        boolean result = true;

        for(var dep : deps) {
            if(Bukkit.getPluginManager().getPlugin(dep.getId()) == null) {
                result = false;
                break;
            }
        }

        return result;
    }

    public static boolean hasEveryDep(String... deps) {
        boolean result = true;

        for(var dep : deps) {
            if(Bukkit.getPluginManager().getPlugin(dep) == null) {
                result = false;
                break;
            }
        }

        return result;
    }
}
