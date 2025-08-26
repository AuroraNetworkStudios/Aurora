package gg.auroramc.aurora.expansions.item.resolvers;

import com.shampaggon.crackshot.CSUtility;
import gg.auroramc.aurora.api.item.ItemResolver;
import gg.auroramc.aurora.api.item.TypeId;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class CrackShotItemResolver implements ItemResolver {

    private static final CSUtility crackshot = new CSUtility();


    @Override
    public boolean matches(ItemStack item) {
        if (item.getType() == Material.AIR) {
            return false;
        }
        return crackshot.getWeaponTitle(item) != null;
    }

    @Override
    public TypeId oneStepMatch(ItemStack item) {
        return resolveId(item);
    }

    @Override
    public TypeId resolveId(ItemStack item) {
        String crackshotId = crackshot.getWeaponTitle(item);
        return crackshotId == null ? null : new TypeId("crackshot", crackshotId);
    }

    @Override
    public ItemStack resolveItem(String id, Player player) {
        return crackshot.generateWeapon(id);
    }
}
