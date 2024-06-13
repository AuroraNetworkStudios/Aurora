package gg.auroramc.aurora.api.reward;

import com.google.common.collect.Maps;
import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.api.util.NamespacedId;
import org.bukkit.configuration.ConfigurationSection;

import java.lang.reflect.InvocationTargetException;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class RewardFactory {
    private final Map<NamespacedId, Class<? extends Reward>> rewardTypes = Maps.newConcurrentMap();

    /**
     * Register a reward type.
     * This method needs to be called in the onEnable method.
     * If it is called later, the reward for this type won't be constructed.
     *
     * @param id    id of the reward type
     * @param clazz implementation of the reward type
     */
    public void registerRewardType(NamespacedId id, Class<? extends Reward> clazz) {
        rewardTypes.put(id, clazz);
    }

    public Optional<Reward> createReward(ConfigurationSection args) {
        if (args == null) return Optional.empty();
        var type = NamespacedId.fromDefault(args.getString("type", "command").toLowerCase(Locale.ROOT));
        Reward reward;

        try {
            var clazz = rewardTypes.get(type);
            if (clazz == null) {
                Aurora.logger().warning("Failed to create reward of type " + type + ": Reward type not found");
                return Optional.empty();
            }
            reward = clazz.getDeclaredConstructor().newInstance();
            reward.init(args);
            return Optional.of(reward);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            Aurora.logger().warning("Failed to create reward of type " + type + ": " + e.getMessage());
            return Optional.empty();
        }
    }
}
