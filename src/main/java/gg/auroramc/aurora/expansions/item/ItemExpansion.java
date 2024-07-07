package gg.auroramc.aurora.expansions.item;

import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.api.dependency.Dep;
import gg.auroramc.aurora.api.dependency.DependencyManager;
import gg.auroramc.aurora.api.expansions.AuroraExpansion;
import gg.auroramc.aurora.api.item.ItemManager;
import gg.auroramc.aurora.expansions.item.resolvers.CustomFishingItemResolver;
import gg.auroramc.aurora.expansions.item.resolvers.MMOItemResolver;
import gg.auroramc.aurora.expansions.item.resolvers.MythicItemResolver;
import gg.auroramc.aurora.expansions.item.resolvers.OraxenItemResolver;
import lombok.Getter;

@Getter
public class ItemExpansion implements AuroraExpansion {
    private ItemManager itemManager;

    @Override
    public void hook() {
        itemManager = new ItemManager();

        if(DependencyManager.hasDep(Dep.CUSTOMFISHING)) {
            itemManager.registerResolver(Dep.CUSTOMFISHING, new CustomFishingItemResolver());
            Aurora.logger().debug("Hooked into CustomFishing for item resolvers.");
        }

        if(DependencyManager.hasDep(Dep.MMOITEMS)) {
            itemManager.registerResolver(Dep.MMOITEMS, new MMOItemResolver());
            Aurora.logger().debug("Hooked into MMOItems for item resolvers.");
        }

        if(DependencyManager.hasDep(Dep.MYTHICMOBS)) {
            itemManager.registerResolver(Dep.MYTHICMOBS, new MythicItemResolver());
            Aurora.logger().debug("Hooked into MythicMobs for item resolvers.");
        }

        if(DependencyManager.hasDep(Dep.ORAXEN)) {
            itemManager.registerResolver(Dep.ORAXEN, new OraxenItemResolver());
            Aurora.logger().debug("Hooked into Oraxen for item resolvers.");
        }
    }

    @Override
    public boolean canHook() {
        return true;
    }
}
