package gg.auroramc.aurora.expansions.item.resolvers;

import gg.auroramc.aurora.api.dependency.Dep;
import gg.auroramc.aurora.api.item.ItemResolver;
import gg.auroramc.aurora.api.item.TypeId;
import me.kryniowesegryderiusz.kgenerators.Main;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class KGeneratorsResolver implements ItemResolver {
    public static final String PREFIX = "kgenerators";

    @Override
    public boolean matches(ItemStack item) {
        return Main.getGenerators().get(item) != null;
    }

    @Override
    public TypeId oneStepMatch(ItemStack item) {
        var id = Main.getGenerators().get(item);
        if (id == null) return null;

        return new TypeId(PREFIX, id.getId());
    }

    @Override
    public TypeId resolveId(ItemStack item) {
        return new TypeId(PREFIX, Main.getGenerators().get(item).getId());
    }

    @Override
    public ItemStack resolveItem(String id, @Nullable Player player) {
        return Main.getGenerators().get(id).getGeneratorItem();
    }

    @Override
    public boolean isPluginEnabled() {
        return Bukkit.getPluginManager().isPluginEnabled(Dep.K_GENERATORS.getId());
    }
}
