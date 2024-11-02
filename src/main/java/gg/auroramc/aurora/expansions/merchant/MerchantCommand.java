package gg.auroramc.aurora.expansions.merchant;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import gg.auroramc.aurora.api.message.Chat;
import gg.auroramc.aurora.api.message.Placeholder;
import org.bukkit.entity.Player;

@CommandAlias("%merchantAlias")
public class MerchantCommand extends BaseCommand {
    private MerchantExpansion expansion;

    public MerchantCommand(MerchantExpansion expansion) {
        this.expansion = expansion;
    }

    @Default
    @CommandCompletion("@merchants @nothing")
    @CommandPermission("aurora.core.user.merchant.open")
    public void onOpen(Player player, String merchantId) {
        var merchant = expansion.getConfig().getMerchants().get(merchantId);

        if (merchant == null || !merchant.getEnabled()) {
            Chat.sendMessage(player, expansion.getConfig().getMessages().getNotFound(), Placeholder.of("{merchant}", merchantId));
            return;
        }
        if (merchant.getPermission() != null && !player.hasPermission(merchant.getPermission())) {
            Chat.sendMessage(player, expansion.getConfig().getMessages().getNoPermission());
            return;
        }

        MerchantShop.merchantShop(player, merchant).open();
    }
}
