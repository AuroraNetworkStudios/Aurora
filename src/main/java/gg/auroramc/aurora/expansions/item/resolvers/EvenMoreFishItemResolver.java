package gg.auroramc.aurora.expansions.item.resolvers;

import com.oheers.fish.EvenMoreFish;
import com.oheers.fish.api.EMFAPI;
import com.oheers.fish.fishing.items.Fish;
import gg.auroramc.aurora.api.item.ItemResolver;
import gg.auroramc.aurora.api.item.TypeId;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class EvenMoreFishItemResolver implements ItemResolver {

    public static final String NAME = "emf";
    public static final String SEPARATOR = ":";

    @Override
    public boolean matches(ItemStack item) {
        EMFAPI emfapi = EvenMoreFish.getInstance().getApi();
        return emfapi.isFish(item);
    }

    @Override
    public TypeId resolveId(ItemStack item) {
        Fish fish = EvenMoreFish.getInstance().getApi().getFish(item);
        if (fish == null) return null;
        return new TypeId(NAME, fish.getRarity().getId() + SEPARATOR + fish.getName());
    }

    @Override
    public ItemStack resolveItem(String id, @Nullable Player player) {
        String[] split = id.split(SEPARATOR);
        if (split.length < 2) return null;

        // rarity is the first part of the id and fishname is the rest, no matter how many
        String rarity = split[0];
        String fishName = id.substring(rarity.length() + 1);

        Fish fish = EvenMoreFish.getInstance().getApi().getFish(rarity, fishName);
        if (fish == null) return null;

        try {
            return fish.getFactory().getItem(fish.getFactory().getMaterial().name());
        } catch (Exception e) {
            return null;
        }
    }
}