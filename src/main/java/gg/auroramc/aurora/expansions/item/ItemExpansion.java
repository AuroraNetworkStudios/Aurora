package gg.auroramc.aurora.expansions.item;

import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.api.dependency.Dep;
import gg.auroramc.aurora.api.dependency.DependencyManager;
import gg.auroramc.aurora.api.expansions.AuroraExpansion;
import gg.auroramc.aurora.api.item.ItemManager;
import gg.auroramc.aurora.api.util.Version;
import gg.auroramc.aurora.expansions.item.resolvers.*;
import gg.auroramc.aurora.expansions.item.resolvers.EcoItemsResolver;
import gg.auroramc.aurora.expansions.item.store.ItemStore;
import gg.auroramc.aurora.api.item.ItemResolver;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.function.Supplier;

@Getter
public class ItemExpansion implements AuroraExpansion {
    private ItemManager itemManager;
    private ItemStore itemStore;

    @Override
    public void hook() {
        itemManager = new ItemManager();
        itemStore = new ItemStore("itemstore.yml");

        var enabledMatchers = Aurora.getLibConfig().getItemMatchers();

        if (DependencyManager.hasDep(Dep.CRACKSHOT) && enabledMatchers.contains(Dep.CRACKSHOT.getId())) {
            register(Dep.CRACKSHOT, () -> new CrackShotItemResolver());
        }

        if (DependencyManager.hasDep(Dep.CUSTOMFISHING) && enabledMatchers.contains(Dep.CUSTOMFISHING.getId())) {
            register(Dep.CUSTOMFISHING, () -> new CustomFishingItemResolver());
        }

        if (DependencyManager.hasDep(Dep.CRAFT_ENGINE) && enabledMatchers.contains(Dep.CRAFT_ENGINE.getId())) {
            register(Dep.CRAFT_ENGINE, () -> new CraftEngineItemResolver());
        }

        if (DependencyManager.hasDep(Dep.EXECUTABLE_BLOCKS) && enabledMatchers.contains(Dep.EXECUTABLE_BLOCKS.getId())) {
            // Use short name for plugin since it would be way too long
            register("eb", () -> new ExecutableBlocksResolver());
        }

        if (DependencyManager.hasDep(Dep.EVEN_MORE_FISH) && enabledMatchers.contains(Dep.EVEN_MORE_FISH.getId())) {
            register(EvenMoreFishItemResolver.NAME, () -> new EvenMoreFishItemResolver());
        }

        if (DependencyManager.hasDep(Dep.EXECUTABLE_ITEMS) && enabledMatchers.contains(Dep.EXECUTABLE_ITEMS.getId())) {
            // Use short name for plugin since it would be way too long
            register("ei", () -> new ExecutableItemsResolver());
        }

        if (DependencyManager.hasDep(Dep.MMOITEMS) && enabledMatchers.contains(Dep.MMOITEMS.getId())) {
            register(Dep.MMOITEMS, () -> new MMOItemResolver());
        }

        if (DependencyManager.hasDep(Dep.MYTHICMOBS) && enabledMatchers.contains(Dep.MYTHICMOBS.getId())) {
            register(Dep.MYTHICMOBS, () -> new MythicItemResolver());
        }

        if (DependencyManager.hasDep(Dep.ECO) && enabledMatchers.contains(Dep.ECO.getId())) {
            register(Dep.ECO, () -> new EcoItemsResolver());
        }

        if (DependencyManager.hasDep(Dep.NEXO) && enabledMatchers.contains(Dep.NEXO.getId())) {
            register(Dep.NEXO, () -> new NexoItemResolver());
        }

        if (DependencyManager.hasDep(Dep.ORAXEN) && enabledMatchers.contains(Dep.ORAXEN.getId())) {
            register(Dep.ORAXEN, () -> new OraxenItemResolver());
        }

        if (DependencyManager.hasDep(Dep.ITEMS_ADDER) && enabledMatchers.contains(Dep.ITEMS_ADDER.getId())) {
            register("ia", () -> new ItemsAdderResolver());
        }

        if (DependencyManager.hasDep(Dep.ITEM_EDIT) && enabledMatchers.contains(Dep.ITEM_EDIT.getId())) {
            register(Dep.ITEM_EDIT, () -> new ItemEditResolver());
        }

        if (DependencyManager.hasDep(Dep.K_GENERATORS) && enabledMatchers.contains(Dep.K_GENERATORS.getId())) {
            register(KGeneratorsResolver.PREFIX, () -> new KGeneratorsResolver());
        }

        initAuroraItemResolver();

        if (DependencyManager.hasDep(Dep.HEAD_DATABASE) && enabledMatchers.contains(Dep.HEAD_DATABASE.getId())) {
            register("hdb", () -> {
                var hdbResolver = new HdbItemResolver();
                Bukkit.getPluginManager().registerEvents(hdbResolver, Aurora.getInstance());
                return hdbResolver;
            });
        }

        Bukkit.getGlobalRegionScheduler().runDelayed(Aurora.getInstance(), (t) -> {
            Aurora.logger().info("Registered resolvers: " + String.join(", ", itemManager.getResolvers().stream().map(r -> r.plugin() + ":" + r.priority()).toList()));
        }, 20);
    }

    @Override
    public boolean canHook() {
        return true;
    }

    private void register(Dep plugin, Supplier<ItemResolver> resolver) {
        register(plugin.getId(), resolver);
    }

    // One outdated integration only disables its own resolver, not every item lookup
    private void register(String plugin, Supplier<ItemResolver> resolver) {
        try {
            itemManager.registerResolver(plugin, resolver.get());
        } catch (Exception | LinkageError e) {
            Aurora.logger().severe("Failed to hook item resolver: " + plugin + ", is it up to date? Error: " + e);
        }
    }

    private void initAuroraItemResolver() {
        var itemConfig = Aurora.getLibConfig().getAuroraItems();
        itemManager.registerResolver("aurora", new AuroraItemResolver(itemStore, itemConfig.getEnableIdResolver()));
    }
}
