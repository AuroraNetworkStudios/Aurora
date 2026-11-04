package gg.auroramc.aurora.api.plugin;

import org.bukkit.plugin.Plugin;

/**
 * An integration with another plugin, enabled by {@link Hooks} only when that plugin is installed. A hook that is
 * also a Listener gets registered.
 */
public interface Hook<P extends Plugin> {
    /**
     * When the plugin enables.
     */
    void hook(P plugin);

    /**
     * When the plugin loads, before {@link #hook}, for what has to be registered early.
     */
    default void hookAtStartUp(P plugin) {
    }
}
