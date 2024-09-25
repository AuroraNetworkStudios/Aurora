package gg.auroramc.aurora.expansions.gui;

import com.google.common.collect.Maps;
import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.api.expansions.AuroraExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Pattern;

public class GuiExpansion implements AuroraExpansion {
    private Map<String, Gui> guiMap = Maps.newConcurrentMap();
    private Map<String, Function<Player, AuroraGui>> customGuiMap = Maps.newConcurrentMap();

    @Override
    public void hook() {
        reload();
    }

    public void registerCustomGui(String id, Function<Player, AuroraGui> supplier) {
        customGuiMap.put(id, supplier);
    }

    public void openGui(String id, Player player, @Nullable Map<String, String> args) {
        var gui = guiMap.get(id);
        if(gui == null) {
            var customGui = customGuiMap.get(id);
            if(customGui != null) {
                customGui.apply(player).open(player, args);
            }
        } else {
            gui.open(player, args);
        }
    }

    public void refreshPlayerGuis(UUID uuid) {
        guiMap.forEach((id, gui) -> gui.refreshForPlayer(uuid));
    }

    public void refreshPlayerGuis(Player player) {
        refreshPlayerGuis(player.getUniqueId());
    }

    public Collection<String> getGuiIds() {
        return guiMap.keySet();
    }

    public void reload() {
        guiMap.values().forEach(Gui::dispose);
        guiMap.clear();

        var dir = new File(Aurora.getInstance().getDataFolder(), "gui/menus");
        if (!dir.exists()) {
            dir.mkdirs();
        }

        loadFolder(dir);
    }

    private void loadFolder(File dir) {
        var files = dir.listFiles();
        if (files != null) {
            for (var file : files) {
                if (file.getName().endsWith(".yml")) {
                    var config = new GuiConfig(file);
                    config.load();
                    var id = file.getName().replace(".yml", "");
                    if (!Pattern.compile("^[a-z0-9_]+$").matcher(id).matches()) {
                        Aurora.getInstance().getLogger().warning("Invalid gui id: " + id + ", Only lowercase letters, numbers, and underscores are allowed.");
                        continue;
                    }
                    if (guiMap.containsKey(id)) {
                        Aurora.getInstance().getLogger().warning("Duplicate gui id: " + id + ", Skipping...");
                        continue;
                    }
                    guiMap.put(id, new Gui(config, id));
                    Aurora.logger().debug("Loaded gui: " + id);
                } else if (file.isDirectory()) {
                    loadFolder(file);
                }
            }
        }
    }

    @Override
    public boolean canHook() {
        return true;
    }
}
