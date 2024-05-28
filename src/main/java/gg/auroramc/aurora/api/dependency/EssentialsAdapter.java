package gg.auroramc.aurora.api.dependency;

import com.earth2me.essentials.IEssentials;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.math.BigDecimal;

public class EssentialsAdapter {
    private static EssentialsAdapter instance;

    private final IEssentials ess;

    public EssentialsAdapter(IEssentials ess) {
        this.ess = ess;
    }

    public static EssentialsAdapter getInstance() {
        if(instance != null) return instance;
        var ess = Bukkit.getPluginManager().getPlugin(Dep.ESSENTIALS.getId());
        if(ess != null) {
            instance = new EssentialsAdapter((IEssentials) ess);
        }
        return instance;
    }

    public boolean isIgnoredPlayer(Player source, Player target) {
        return ess.getUser(source).isIgnoredPlayer(ess.getUser(target));
    }

    public void depositMoney(Player source, double amount) {
        try {
            ess.getUser(source).giveMoney(new BigDecimal(amount));
        } catch (Exception ignored) {}
    }

    public void withdrawMoney(Player source, double amount) {
        ess.getUser(source).takeMoney(new BigDecimal(amount));
    }

    public double getBalance(Player source) {
        return ess.getUser(source).getMoney().doubleValue();
    }

    public boolean isEconomyEnabled() {
        return !ess.getSettings().isEcoDisabled();
    }

}
