package bench;

import gg.auroramc.aurora.api.user.AuroraUser;
import org.bukkit.Material;

// Like an economy withdraw: takes this many diamonds from the stash, or nothing if there aren't enough
public final class BenchTakeDiamonds {
    private BenchTakeDiamonds() {
    }

    public static boolean take(AuroraUser user, int amount) {
        var stash = user.getStashData();
        synchronized (stash) {
            var items = stash.getItems();
            if (BenchPlugin.diamonds(items) < amount) return false;
            int remaining = amount;
            for (int i = items.size() - 1; i >= 0 && remaining > 0; i--) {
                var item = items.get(i);
                if (item.getType() != Material.DIAMOND) continue;
                int take = Math.min(remaining, item.getAmount());
                remaining -= take;
                if (take == item.getAmount()) items.remove(i);
                else item.setAmount(item.getAmount() - take);
            }
            stash.setDirty(true);
            return true;
        }
    }
}
