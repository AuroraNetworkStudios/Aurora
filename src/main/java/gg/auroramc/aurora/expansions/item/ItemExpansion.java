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
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

@Getter
public class ItemExpansion implements AuroraExpansion {
    private ItemManager itemManager;
    private ItemStore itemStore;

    @Override
    public void hook() {
        itemManager = new ItemManager();
        itemStore = new ItemStore("itemstore.yml");

        var enabledMatchers = Aurora.getLibConfig().getItemMatchers();

        if (DependencyManager.hasDep(Dep.CUSTOMFISHING) && enabledMatchers.contains(Dep.CUSTOMFISHING.getId())) {
            itemManager.registerResolver(Dep.CUSTOMFISHING, new CustomFishingItemResolver());
        }

        if (DependencyManager.hasDep(Dep.EXECUTABLE_BLOCKS) && enabledMatchers.contains(Dep.EXECUTABLE_BLOCKS.getId())) {
            // Use short name for plugin since it would be way too long
            itemManager.registerResolver("eb", new ExecutableBlocksResolver());
        }

        if (DependencyManager.hasDep(Dep.EVEN_MORE_FISH) && enabledMatchers.contains(Dep.EVEN_MORE_FISH.getId())) {
            itemManager.registerResolver(EvenMoreFishItemResolver.NAME, new EvenMoreFishItemResolver());
        }

        if (DependencyManager.hasDep(Dep.EXECUTABLE_ITEMS) && enabledMatchers.contains(Dep.EXECUTABLE_ITEMS.getId())) {
            // Use short name for plugin since it would be way too long
            itemManager.registerResolver("ei", new ExecutableItemsResolver());
        }

        if (DependencyManager.hasDep(Dep.MMOITEMS) && enabledMatchers.contains(Dep.MMOITEMS.getId())) {
            itemManager.registerResolver(Dep.MMOITEMS, new MMOItemResolver());
        }

        if (DependencyManager.hasDep(Dep.MYTHICMOBS) && enabledMatchers.contains(Dep.MYTHICMOBS.getId())) {
            itemManager.registerResolver(Dep.MYTHICMOBS, new MythicItemResolver());
        }

        if (DependencyManager.hasDep(Dep.ECO) && enabledMatchers.contains(Dep.ECO.getId())) {
            itemManager.registerResolver(Dep.ECO, new EcoItemsResolver());
        }

        if (DependencyManager.hasDep(Dep.NEXO) && enabledMatchers.contains(Dep.NEXO.getId())) {
            itemManager.registerResolver(Dep.NEXO, new NexoItemResolver());
        }

        if (DependencyManager.hasDep(Dep.ORAXEN) && enabledMatchers.contains(Dep.ORAXEN.getId())) {
            itemManager.registerResolver(Dep.ORAXEN, new OraxenItemResolver());
        }

        if (DependencyManager.hasDep("ItemsAdder") && enabledMatchers.contains("ItemsAdder")) {
            itemManager.registerResolver("ia", new ItemsAdderResolver());
        }

        if (DependencyManager.hasDep(Dep.ITEM_EDIT) && enabledMatchers.contains(Dep.ITEM_EDIT.getId())) {
            itemManager.registerResolver(Dep.ITEM_EDIT, new ItemEditResolver());
        }

        if (DependencyManager.hasDep("KGenerators") && enabledMatchers.contains("KGenerators")) {
            itemManager.registerResolver(KGeneratorsResolver.PREFIX, new KGeneratorsResolver());
        }

        initAuroraItemResolver();

        if (DependencyManager.hasDep(Dep.HEAD_DATABASE) && enabledMatchers.contains(Dep.HEAD_DATABASE.getId())) {
            var hdbResolver = new HdbItemResolver();
            Bukkit.getPluginManager().registerEvents(hdbResolver, Aurora.getInstance());
            itemManager.registerResolver("hdb", hdbResolver);
        }
    }

    @Override
    public boolean canHook() {
        return true;
    }

    private void initAuroraItemResolver() {
        var itemConfig = Aurora.getLibConfig().getAuroraItems();
        itemManager.registerResolver("aurora", new AuroraItemResolver(itemStore, itemConfig.getEnableIdResolver()));
    }
}
