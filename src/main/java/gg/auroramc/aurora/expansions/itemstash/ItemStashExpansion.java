package gg.auroramc.aurora.expansions.itemstash;

import com.google.common.collect.Maps;
import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.api.events.itemstash.StashItemAddEvent;
import gg.auroramc.aurora.api.events.user.AuroraUserLoadedEvent;
import gg.auroramc.aurora.api.expansions.AuroraExpansion;
import gg.auroramc.aurora.api.message.Chat;
import lombok.Getter;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.List;
import java.util.Map;
import java.util.Objects;
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

    @Override
    public void reload() {
        Config.saveDefault();
        config = new Config();
        config.load();
    }

    public void open(Player player) {
        if (menus.containsKey(player.getUniqueId())) return;
        menus.put(player.getUniqueId(), new StashMenu(player, config, menus::remove));
    }

    public void open(Player player, Player target) {
        if (menus.containsKey(target.getUniqueId())) {
            target.closeInventory();
        }

        player.getScheduler().run(Aurora.getInstance(), (t) -> {
            menus.put(target.getUniqueId(), new StashMenu(player, target, config, menus::remove));
        }, null);
    }

    /**
     * Opens the stash of a player who may be offline. If they aren't online on this server,
     * it shows their saved stash and is view only.
     */
    public void open(Player player, OfflinePlayer target) {
        if (target.getPlayer() != null) {
            open(player, target.getPlayer());
            return;
        }

        var name = Objects.requireNonNullElse(target.getName(), target.getUniqueId().toString());
        Aurora.getUserManager().loadUserFromStorage(target.getUniqueId()).thenAccept(user ->
                player.getScheduler().run(Aurora.getInstance(), (t) -> {
                    if (!user.isLoaded()) {
                        Chat.sendMessage(player, Aurora.getMsg(player).getCommandError());
                        return;
                    }
                    // Not in menus, that's for menus showing a live stash
                    new StashMenu(player, target.getUniqueId(), name, user.getStashData().getItems(), config, uuid -> {
                    });
                }, null));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onStashItemAdd(StashItemAddEvent event) {
        var menu = menus.get(event.getPlayerUniqueId());
        if (menu != null) {
            // The event is called before the item is added
            menu.refreshLater(config);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onUserLoaded(AuroraUserLoadedEvent event) {
        var messages = Aurora.getMsg(event.getUser().getPlayer());

        var user = event.getUser();
        if (config.getNotifyOnJoin() && user.getPlayer() != null && !user.getStashData().getItems().isEmpty()) {
            Chat.sendMessage(user.getPlayer(), messages.getStashAvailable());
        }
    }

    private String a(List<String> aliases) {
        return String.join("|", aliases);
    }
}
