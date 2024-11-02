package gg.auroramc.aurora.expansions.merchant;

import gg.auroramc.aurora.api.AuroraAPI;
import gg.auroramc.aurora.api.item.TypeId;
import gg.auroramc.aurora.api.message.Text;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;

import java.util.ArrayList;

public class MerchantShop {
    private final Player player;
    private final Config.MerchantConfig config;

    public static MerchantShop merchantShop(Player player, Config.MerchantConfig config) {
        return new MerchantShop(player, config);
    }

    public MerchantShop(Player player, Config.MerchantConfig config) {
        this.player = player;
        this.config = config;
    }

    public void open() {
        var merchant = Bukkit.createMerchant(Text.component(player, config.getName()));

        var trades = new ArrayList<MerchantRecipe>(config.getOffers().size());

        for (var offerConfig : config.getOffers()) {
            var recipe = new MerchantRecipe(getItemStack(offerConfig.getResult()), Integer.MAX_VALUE);
            recipe.setIgnoreDiscounts(true);

            for (var ingredient : offerConfig.getIngredients()) {
                var item = getItemStack(ingredient);
                if (item == null) continue;
                recipe.addIngredient(item);
            }

            trades.add(recipe);
        }

        merchant.setRecipes(trades);

        player.openMerchant(merchant, false);
    }

    private ItemStack getItemStack(String id) {
        if (id.equals("air")) {
            return null;
        }
        var split = id.split("/");
        var item = AuroraAPI.getItemManager().resolveItem(TypeId.fromDefault(split[0]));
        var amount = Math.min(Integer.parseInt(split[1]), item.getMaxStackSize());
        item.setAmount(amount);
        return item;
    }
}
