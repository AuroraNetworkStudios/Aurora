package gg.auroramc.aurora.expansions.item;

import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.api.dependency.Dep;
import gg.auroramc.aurora.api.dependency.DependencyManager;
import gg.auroramc.aurora.api.expansions.AuroraExpansion;
import gg.auroramc.aurora.api.item.ItemManager;
import gg.auroramc.aurora.expansions.item.resolvers.*;
import gg.auroramc.aurora.expansions.item.resolvers.EcoItemsResolver;
import gg.auroramc.aurora.expansions.item.store.ItemStore;
import lombok.Getter;
import org.bukkit.Bukkit;

@Getter
public class ItemExpansion implements AuroraExpansion {
    private ItemManager itemManager;
    private ItemStore itemStore;

    @Override
    public void hook() {
        itemManager = new ItemManager();
        itemStore = new ItemStore("itemstore.yml");

        if (DependencyManager.hasDep(Dep.CUSTOMFISHING)) {
            itemManager.registerResolver(Dep.CUSTOMFISHING, new CustomFishingItemResolver());
            Aurora.logger().debug("Hooked into CustomFishing for item resolvers.");
        }

        if (DependencyManager.hasDep(Dep.EXECUTABLE_BLOCKS)) {
            // Use short name for plugin since it would be way too long
            itemManager.registerResolver("eb", new ExecutableBlocksResolver());
            Aurora.logger().debug("Hooked into ExecutableBlocks for item resolvers.");
        }

        if (DependencyManager.hasDep(Dep.EXECUTABLE_ITEMS)) {
            // Use short name for plugin since it would be way too long
            itemManager.registerResolver("ei", new ExecutableItemsResolver());
            Aurora.logger().debug("Hooked into ExecutableItems for item resolvers.");
        }

        if (DependencyManager.hasDep(Dep.MMOITEMS)) {
            itemManager.registerResolver(Dep.MMOITEMS, new MMOItemResolver());
            Aurora.logger().debug("Hooked into MMOItems for item resolvers.");
        }

        if (DependencyManager.hasDep(Dep.MYTHICMOBS)) {
            itemManager.registerResolver(Dep.MYTHICMOBS, new MythicItemResolver());
            Aurora.logger().debug("Hooked into MythicMobs for item resolvers.");
        }

        if (DependencyManager.hasDep(Dep.ECO)) {
            itemManager.registerResolver(Dep.ECO, new EcoItemsResolver());
            Aurora.logger().debug("Hooked into EcoItems for item resolvers.");
        }

        if (DependencyManager.hasDep(Dep.NEXO)) {
            itemManager.registerResolver(Dep.NEXO, new NexoItemResolver());
            Aurora.logger().debug("Hooked into Nexo for item resolvers.");
        }

        if (DependencyManager.hasDep(Dep.ORAXEN)) {
            itemManager.registerResolver(Dep.ORAXEN, new OraxenItemResolver());
            Aurora.logger().debug("Hooked into Oraxen for item resolvers.");
        }

        if (DependencyManager.hasDep("ItemsAdder")) {
            itemManager.registerResolver("ia", new ItemsAdderResolver());
            Aurora.logger().debug("Hooked into ItemsAdder for item resolvers.");
        }

        itemManager.registerResolver("aurora", new AuroraItemResolver(itemStore));

        if (DependencyManager.hasDep(Dep.HEAD_DATABASE)) {
            var hdbResolver = new HdbItemResolver();
            Bukkit.getPluginManager().registerEvents(hdbResolver, Aurora.getInstance());
            itemManager.registerResolver("hdb", hdbResolver);
            Aurora.logger().debug("Hooked into HeadDatabase for item resolvers.");
        }
    }

    @Override
    public boolean canHook() {
        return true;
    }
}
