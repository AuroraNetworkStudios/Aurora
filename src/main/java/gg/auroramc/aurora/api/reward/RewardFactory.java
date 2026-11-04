package gg.auroramc.aurora.api.reward;

import com.google.common.collect.Maps;
import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.api.util.NamespacedId;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public class RewardFactory {
    private final Map<NamespacedId, Supplier<? extends Reward>> rewardTypes = Maps.newConcurrentMap();

    /**
     * Register a reward type.
     * This method needs to be called in the onEnable method.
     * If it is called later, the reward for this type won't be constructed.
     *
     * @param id    id of the reward type
     * @param clazz implementation of the reward type
     */
    public void registerRewardType(NamespacedId id, Class<? extends Reward> clazz) {
        rewardTypes.put(id, () -> {
            try {
                return clazz.getDeclaredConstructor().newInstance();
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException(clazz.getSimpleName() + " needs a constructor without arguments");
            } catch (java.lang.reflect.InvocationTargetException e) {
                throw new IllegalStateException(clazz.getSimpleName() + " failed to create: " + e.getCause(), e.getCause());
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(clazz.getSimpleName() + " can't be created: " + e, e);
            }
        });
    }

    /**
     * Register a reward type created by the supplier, for rewards that take arguments (like the shared ones in
     * {@code api.reward.integration}, which take the plugin's own stat key).
     */
    public void registerRewardType(NamespacedId id, Supplier<? extends Reward> supplier) {
        rewardTypes.put(id, supplier);
    }

    public Optional<Reward> createReward(ConfigurationSection args) {
        if (args == null) return Optional.empty();
        var type = NamespacedId.fromDefault(args.getString("type", "command").toLowerCase(Locale.ROOT));
        Reward reward;

        try {
            var supplier = rewardTypes.get(type);
            if (supplier == null) {
                Aurora.logger().warning("Failed to create reward of type " + type + ": Reward type not found");
                return Optional.empty();
            }
            reward = supplier.get();
            reward.init(args);
            return Optional.of(reward);
        } catch (RuntimeException | LinkageError e) {
            // A bad reward is skipped, the others still load
            Aurora.logger().warning("Failed to create reward of type " + type + ": " + e);
            return Optional.empty();
        }
    }
}
