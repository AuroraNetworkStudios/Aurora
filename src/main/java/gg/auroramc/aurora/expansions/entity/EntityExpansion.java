package gg.auroramc.aurora.expansions.entity;

import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.api.dependency.Dep;
import gg.auroramc.aurora.api.dependency.DependencyManager;
import gg.auroramc.aurora.api.entity.EntityManager;
import gg.auroramc.aurora.api.expansions.AuroraExpansion;
import gg.auroramc.aurora.expansions.entity.resolvers.ecomobs.EcoMobsEntityResolver;
import gg.auroramc.aurora.expansions.entity.resolvers.elitemobs.EliteMobsResolver;
import gg.auroramc.aurora.expansions.entity.resolvers.mythicmobs.MythicEntityResolver;
import lombok.Getter;

@Getter
public class EntityExpansion implements AuroraExpansion {
    private EntityManager entityManager;

    @Override
    public void hook() {
        entityManager = new EntityManager();

        if (DependencyManager.hasDep(Dep.MYTHICMOBS)) {
            entityManager.registerResolver(Dep.MYTHICMOBS, new MythicEntityResolver());
            Aurora.logger().debug("Hooked into MythicMobs for entity resolvers.");
        }

        if (DependencyManager.hasEveryDep("Eco", "EcoMobs")) {
            entityManager.registerResolver("ecomobs", new EcoMobsEntityResolver());
            Aurora.logger().debug("Hooked into EcoMobs for entity resolvers.");
        }

        if (DependencyManager.hasDep(Dep.ELITEMOBS)) {
            entityManager.registerResolver(Dep.ELITEMOBS, new EliteMobsResolver());
            Aurora.logger().debug("Hooked into EliteMobs for entity resolvers.");
        }
    }

    @Override
    public boolean canHook() {
        return true;
    }
}
