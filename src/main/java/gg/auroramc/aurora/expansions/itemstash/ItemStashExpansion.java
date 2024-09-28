package gg.auroramc.aurora.expansions.itemstash;

import com.google.common.collect.Maps;
import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.api.events.itemstash.StashItemAddEvent;
import gg.auroramc.aurora.api.events.user.AuroraUserLoadedEvent;
import gg.auroramc.aurora.api.expansions.AuroraExpansion;
import gg.auroramc.aurora.api.message.Chat;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.List;
import java.util.Map;
import java.util.UUID;


public class ItemStashExpansion implements AuroraExpansion, Listener {
    @Getter
    private Config config;
    private Map<UUID, StashMenu> menus = Maps.newConcurrentMap();

    @Override
    public void hook() {
        reload();
        var cm = Aurora.getInstance().getCommandManager().getPaperCommandManager();
        cm.getCommandReplacements().addReplacement("stashAlias", a(config.getCommandAliases()));
        cm.registerCommand(new StashCommand(this));
    }

    @Override
    public boolean canHook() {
        return true;
    }

    public void reload() {
        Config.saveDefault();
        config = new Config();
        config.load();
    }

    public void open(Player player) {
        if (menus.containsKey(player.getUniqueId())) return;
        menus.put(player.getUniqueId(), new StashMenu(player, config, p -> menus.remove(p.getUniqueId())));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onStashItemAdd(StashItemAddEvent event) {
        if (menus.containsKey(event.getPlayerUniqueId())) {
            menus.get(event.getPlayerUniqueId()).refresh(config);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onUserLoaded(AuroraUserLoadedEvent event) {
        var user = event.getUser();
        if (config.getNotifyOnJoin() && user.getPlayer() != null && !user.getStashData().getItems().isEmpty()) {
            Chat.sendMessage(user.getPlayer(), Aurora.getMessageConfig().getStashAvailable());
        }
    }

    private String a(List<String> aliases) {
        return String.join("|", aliases);
    }
}
