package gg.auroramc.aurora.expansions.itemstash;

import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.api.menu.AuroraMenu;
import gg.auroramc.aurora.api.menu.ItemBuilder;
import gg.auroramc.aurora.api.message.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class StashMenu {
    private final Player player;
    private final Consumer<Player> onClose;
    private Config config;
    private AuroraMenu menu;
    private int page = 0;

    public StashMenu(Player player, Config config, Consumer<Player> onClose) {
        this.player = player;
        this.onClose = onClose;
        refresh(config);
    }

    public void refresh(Config config) {
        this.config = config;
        this.menu = new AuroraMenu(player, config.getMenu().getTitle(), 54, false);
        menu.onClose((m, e) -> onClose.accept(player));
        var mc = config.getMenu();

        if (mc.getFiller().getEnabled()) {
            menu.addFiller(ItemBuilder.of(mc.getFiller().getItem()).toItemStack(player));
        } else {
            menu.addFiller(ItemBuilder.filler(Material.AIR));
        }

        for (var customItem : mc.getCustomItems().values()) {
            menu.addItem(ItemBuilder.of(customItem).placeholder(Placeholder.of("{player}", player.getName())).build(player));
        }

        var stashData = Aurora.getUserManager().getUser(player).getStashData();

        var items = getPageItems();
        for (int i = 0; i < config.getStashArea().size(); i++) {
            var menuSlot = config.getStashArea().get(i);
            if (i >= items.size()) {
                menu.addItem(ItemBuilder.item(new ItemStack(Material.AIR)).slot(menuSlot).build(player));
                continue;
            }
            var item = items.get(i);
            menu.addItem(ItemBuilder.item(item).amount(item.getAmount()).slot(menuSlot).build(player), (e) -> {
                if (stashData.removeItem(item)) {
                    var failed = player.getInventory().addItem(item);
                    if (!failed.isEmpty()) {
                        Bukkit.getGlobalRegionScheduler().run(Aurora.getInstance(),
                                (task) -> failed.forEach((slot, fitem) -> player.getWorld().dropItem(player.getLocation(), fitem)));

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
            var itemsToRemove = new ArrayList<ItemStack>();

            for (var item : stashData.getItems()) {
                var success = stashData.removeItem(item);
                if (success) {
                    itemsToRemove.add(item);
                }
            }

            var failed = player.getInventory().addItem(itemsToRemove.toArray(new ItemStack[0]));
            if (!failed.isEmpty()) {
                Bukkit.getGlobalRegionScheduler().run(Aurora.getInstance(),
                        (task) -> failed.forEach((slot, fitem) -> player.getWorld().dropItem(player.getLocation(), fitem)));
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

        menu.open();
    }

    private List<ItemStack> getPageItems() {
        int pageSize = config.getStashArea().size();
        var items = Aurora.getUserManager().getUser(player).getStashData().getItems();
        return items.subList(page * pageSize, Math.min(items.size(), (page + 1) * pageSize));
    }

    private int getMaxPages() {
        int pageSize = config.getStashArea().size();
        return (int) Math.ceil((double) Aurora.getUserManager().getUser(player).getStashData().getItems().size() / pageSize);
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
