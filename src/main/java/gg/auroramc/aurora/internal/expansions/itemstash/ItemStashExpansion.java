package gg.auroramc.aurora.internal.expansions.itemstash;

import com.google.common.collect.Maps;
import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.api.events.itemstash.StashItemAddEvent;
import gg.auroramc.aurora.api.events.user.AuroraUserLoadedEvent;
import gg.auroramc.aurora.internal.expansions.AuroraExpansion;
import gg.auroramc.aurora.api.message.Chat;
import lombok.Getter;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;


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
        config = Config.load();
    }

    public void open(Player player) {
        if (menus.containsKey(player.getUniqueId())) return;
        // Menus don't open while sleeping, it would stay in the map and block opening the stash again
        if (player.isSleeping()) return;
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
     * Opens the stash of a player who may be offline. If they aren't online on this server, it shows their saved
     * stash, and items taken from it are taken from their saved data. Not for players online on another server.
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
                    // A copy, the saved data may be edited on another thread while the menu shows it
                    var stash = user.getStashData();
                    List<ItemStack> items;
                    synchronized (stash) {
                        items = stash.getItems().stream().map(ItemStack::clone).collect(Collectors.toCollection(ArrayList::new));
                    }
                    // Not in menus, that's for menus showing a live stash
                    new StashMenu(player, target.getUniqueId(), name, items, config, uuid -> {
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
