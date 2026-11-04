package gg.auroramc.aurora.api.plugin;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.logging.Level;

/**
 * A plugin's integrations with other plugins. Each one is created only if one of its plugins is installed, and a
 * failing one is logged and skipped without affecting the others (an outdated integration shouldn't stop the plugin).
 */
public final class Hooks<P extends Plugin> {
    private record Entry<P extends Plugin>(Class<? extends Hook<P>> type, List<String> plugins) {
    }

    private final P plugin;
    private final List<Entry<P>> entries = new ArrayList<>();
    // Only changed in load and enable (onLoad, onEnable), read from any thread afterwards
    private volatile Map<Class<?>, Hook<P>> loaded = new LinkedHashMap<>();

    public Hooks(P plugin) {
        this.plugin = plugin;
    }

    /**
     * Adds an integration, used if any of the plugins is installed.
     */
    public Hooks<P> add(Class<? extends Hook<P>> type, String... anyOf) {
        entries.add(new Entry<>(type, List.of(anyOf)));
        return this;
    }

    /**
     * Creates the integrations whose plugins are installed and calls their {@link Hook#hookAtStartUp}. Call it in
     * the plugin's onLoad (or at the start of onEnable).
     */
    public void load() {
        var loaded = new LinkedHashMap<>(this.loaded);
        for (var entry : entries) {
            if (loaded.containsKey(entry.type())) continue;
            if (entry.plugins().stream().noneMatch(name -> Bukkit.getPluginManager().getPlugin(name) != null)) continue;
            try {
                var constructor = entry.type().getDeclaredConstructor();
                constructor.setAccessible(true);
                var hook = constructor.newInstance();
                hook.hookAtStartUp(plugin);
                loaded.put(entry.type(), hook);
            } catch (Throwable e) {
                plugin.getLogger().log(Level.WARNING, "Failed to hook into " + String.join(", ", entry.plugins()) + ", is it up to date? " + e, e);
            }
        }
        this.loaded = loaded;
    }

    /**
     * Enables the loaded integrations. Call it in the plugin's onEnable.
     */
    public void enable() {
        var loaded = new LinkedHashMap<>(this.loaded);
        for (var it = loaded.values().iterator(); it.hasNext(); ) {
            var hook = it.next();
            try {
                hook.hook(plugin);
                if (hook instanceof Listener listener) Bukkit.getPluginManager().registerEvents(listener, plugin);
            } catch (Throwable e) {
                plugin.getLogger().log(Level.WARNING, "Failed to enable the integration " + hook.getClass().getSimpleName() + ", is its plugin up to date? " + e, e);
                it.remove();
            }
        }
        this.loaded = Collections.unmodifiableMap(loaded);
    }

    @Nullable
    public <T extends Hook<P>> T get(Class<T> type) {
        return type.cast(loaded.get(type));
    }

    public boolean isEnabled(Class<? extends Hook<P>> type) {
        return loaded.containsKey(type);
    }
}
