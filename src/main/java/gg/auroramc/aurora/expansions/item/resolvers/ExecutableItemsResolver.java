package gg.auroramc.aurora.expansions.item.resolvers;

import com.ssomar.score.api.executableitems.ExecutableItemsAPI;
import gg.auroramc.aurora.api.item.ItemResolver;
import gg.auroramc.aurora.api.item.TypeId;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class ExecutableItemsResolver implements ItemResolver {
    @Override
    public boolean matches(ItemStack item) {
        return ExecutableItemsAPI.getExecutableItemsManager().getExecutableItem(item).isPresent();
    }

    @Override
    public TypeId resolveId(ItemStack item) {
        return new TypeId("ei", ExecutableItemsAPI.getExecutableItemsManager().getExecutableItem(item).get().getId());
    }

    @Override
    public ItemStack resolveItem(String id, @Nullable Player player) {
        return ExecutableItemsAPI.getExecutableItemsManager().getExecutableItem(id).get()
                .buildItem(1, player != null ? Optional.of(player) : Optional.empty());
    }
}
