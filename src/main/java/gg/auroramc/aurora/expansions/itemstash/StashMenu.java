package gg.auroramc.aurora.expansions.itemstash;

import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.api.menu.AuroraMenu;
import gg.auroramc.aurora.api.menu.ItemBuilder;
import gg.auroramc.aurora.api.message.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class StashMenu {
    private final UUID targetId;
    private final String targetName;
    private final Player openFor;
    private final Consumer<UUID> onClose;
    // Stash of a player who isn't online on this server, as it was when the menu was opened. The menu is view only then.
    @Nullable
    private final List<ItemStack> offlineItems;
    private final AtomicBoolean refreshQueued = new AtomicBoolean(false);
    private Config config;
    private AuroraMenu menu;
    private int page = 0;

    public StashMenu(Player openFor, Player target, Config config, Consumer<UUID> onClose) {
        this(openFor, target.getUniqueId(), target.getName(), null, config, onClose);
    }

    public StashMenu(Player target, Config config, Consumer<UUID> onClose) {
        this(target, target, config, onClose);
    }

    public StashMenu(Player openFor, UUID targetId, String targetName, @Nullable List<ItemStack> offlineItems, Config config, Consumer<UUID> onClose) {
        this.openFor = openFor;
        this.targetId = targetId;
        this.targetName = targetName;
        this.offlineItems = offlineItems;
        this.onClose = onClose;
        refresh(config);
    }

    /**
     * Refreshes the menu on the next tick, so changes made right after this call (like the item of a
     * {@link gg.auroramc.aurora.api.events.itemstash.StashItemAddEvent}) are shown too.
     */
    public void refreshLater(Config config) {
        if (refreshQueued.getAndSet(true)) return;
        openFor.getScheduler().run(Aurora.getInstance(), (task) -> {
            refreshQueued.set(false);
            refresh(config);
        }, null);
    }

    public void refresh(Config config) {
        this.config = config;
        // Placeholders are filled for the target if they are online here, like before
        var player = Objects.requireNonNullElse(Bukkit.getPlayer(targetId), openFor);
        this.menu = new AuroraMenu(player, config.getMenu().getTitle(), 54, false, Aurora.getLocalizationProvider());
        // Refreshing opens a new menu which closes the old one, only closing the current one counts
        menu.onClose((m, e) -> {
            if (m == this.menu) onClose.accept(targetId);
        });
        var mc = config.getMenu();

        if (mc.getFiller().getEnabled()) {
            menu.addFiller(ItemBuilder.of(mc.getFiller().getItem()).toItemStack(player));
        } else {
            menu.addFiller(ItemBuilder.filler(Material.AIR));
        }

        for (var customItem : mc.getCustomItems().values()) {
            menu.addItem(ItemBuilder.of(customItem).placeholder(Placeholder.of("{player}", targetName)).build(player));
        }

        // Null if they aren't online here, the menu is view only then
        var stashData = offlineItems == null ? Aurora.getUserManager().getUser(targetId).getStashData() : null;

        var items = getPageItems();
        for (int i = 0; i < config.getStashArea().size(); i++) {
            var menuSlot = config.getStashArea().get(i);
            if (i >= items.size()) {
                menu.addItem(ItemBuilder.item(new ItemStack(Material.AIR)).slot(menuSlot).build(player));
                continue;
            }
            var item = items.get(i);
            menu.addItem(ItemBuilder.item(item).amount(item.getAmount()).slot(menuSlot).build(player), (e) -> {
                if (stashData == null) return;
                if (stashData.removeItem(item)) {
                    var failed = openFor.getInventory().addItem(item);
                    if (!failed.isEmpty()) {
                        openFor.getScheduler().run(Aurora.getInstance(),
                                (task) -> failed.forEach((slot, fitem) -> openFor.getWorld().dropItem(openFor.getLocation(), fitem)), null);

                    }
                    if (getMaxPages() <= page) {
                        page = Math.max(getMaxPages() - 1, 0);
                    }
                    refresh(config);
                }
            });

        }

        // Collect all
        menu.addItem(ItemBuilder.of(mc.getItems().getCollectAll()).build(player), (e) -> {
            if (stashData == null) return;
            var itemsToRemove = new ArrayList<ItemStack>();

            for (var item : stashData.getItems()) {
                var success = stashData.removeItem(item);
                if (success) {
                    itemsToRemove.add(item);
                }
            }

            var failed = openFor.getInventory().addItem(itemsToRemove.toArray(new ItemStack[0]));
            if (!failed.isEmpty()) {
                openFor.getScheduler().run(Aurora.getInstance(),
                        (task) -> failed.forEach((slot, fitem) -> openFor.getWorld().dropItem(openFor.getLocation(), fitem)), null);
            }
            if (getMaxPages() <= page) {
                page = Math.max(getMaxPages() - 1, 0);
            }
            refresh(config);
        });

        // Pagination
        if (getMaxPages() > 1) {
            menu.addItem(ItemBuilder.of(mc.getItems().getPrevPage()).build(player), (e) -> {
                prevPage();
            });

            menu.addItem(ItemBuilder.of(mc.getItems().getCurrentPage())
                    .placeholder(Placeholder.of("{current}", page + 1))
                    .placeholder(Placeholder.of("{max}", getMaxPages()))
                    .build(player));

            menu.addItem(ItemBuilder.of(mc.getItems().getNextPage()).build(player), (e) -> {
                nextPage();
            });
        }

        menu.open(openFor);
    }

    private List<ItemStack> getItems() {
        if (offlineItems != null) return offlineItems;
        return Aurora.getUserManager().getUser(targetId).getStashData().getItems();
    }

    private List<ItemStack> getPageItems() {
        int pageSize = config.getStashArea().size();
        var items = getItems();
        return items.subList(page * pageSize, Math.min(items.size(), (page + 1) * pageSize));
    }

    private int getMaxPages() {
        int pageSize = config.getStashArea().size();
        return (int) Math.ceil((double) getItems().size() / pageSize);
    }

    private void nextPage() {
        if (page >= getMaxPages() - 1) return;
        page++;
        refresh(config);
    }

    private void prevPage() {
        if (page <= 0) return;
        page--;
        refresh(config);
    }
}
