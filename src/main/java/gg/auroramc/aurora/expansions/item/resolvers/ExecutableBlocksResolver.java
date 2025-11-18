package gg.auroramc.aurora.expansions.item.resolvers;

import com.ssomar.executableblocks.api.ExecutableBlocksAPI;
import gg.auroramc.aurora.api.dependency.Dep;
import gg.auroramc.aurora.api.item.ItemResolver;
import gg.auroramc.aurora.api.item.TypeId;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class ExecutableBlocksResolver implements ItemResolver {
    @Override
    public boolean matches(ItemStack item) {
        return ExecutableBlocksAPI.getExecutableBlocksManager().getExecutableBlock(item).isPresent();
    }

    @Override
    public TypeId resolveId(ItemStack item) {
        return new TypeId("eb", ExecutableBlocksAPI.getExecutableBlocksManager().getExecutableBlock(item).get().getId());
    }

    @Override
    public ItemStack resolveItem(String id, @Nullable Player player) {
        return ExecutableBlocksAPI.getExecutableBlocksManager().getExecutableBlock(id).get()
                .buildItem(1, player != null ? Optional.of(player) : Optional.empty());
    }

    @Override
    public boolean isPluginEnabled() {
        return Bukkit.getPluginManager().isPluginEnabled(Dep.EXECUTABLE_BLOCKS.getId());
    }
}
