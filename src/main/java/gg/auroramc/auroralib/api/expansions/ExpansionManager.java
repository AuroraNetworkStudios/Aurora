package gg.auroramc.auroralib.api.expansions;

import gg.auroramc.auroralib.AuroraLib;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public class ExpansionManager {
    private final Map<Class<? extends AuroraExpansion>, AuroraExpansion> expansions = new HashMap<>();

    public <T extends AuroraExpansion> void loadExpansion(Class<T> clazz) {
        try {
            var expansion = clazz.getDeclaredConstructor().newInstance();

            if(expansion.canHook()) {
                expansions.put(clazz, expansion);
                expansion.hook();
                if(expansion instanceof Listener) {
                    Bukkit.getPluginManager().registerEvents((Listener) expansion, AuroraLib.getInstance());
                }
            }
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            AuroraLib.logger().severe("Failed to load expansion: " + clazz.getName());
        }
    }

    public <T extends AuroraExpansion> T getExpansion(Class<T> clazz) {
        var expansion = expansions.get(clazz);
        if(clazz.isInstance(expansion)) {
            return clazz.cast(expansion);
        }
        return null;
    }
}
