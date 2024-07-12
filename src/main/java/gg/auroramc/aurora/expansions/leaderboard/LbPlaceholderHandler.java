package gg.auroramc.aurora.expansions.leaderboard;

import gg.auroramc.aurora.api.AuroraAPI;
import gg.auroramc.aurora.api.message.Text;
import gg.auroramc.aurora.api.placeholder.PlaceholderHandler;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Stream;

public class LbPlaceholderHandler implements PlaceholderHandler {
    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.builder()
            .hexColors().useUnusualXRepeatedCharacterHexFormat().build();

    private final LeaderboardExpansion lbm;

    public LbPlaceholderHandler(LeaderboardExpansion leaderboardExpansion) {
        this.lbm = leaderboardExpansion;
    }

    @Override
    public String getIdentifier() {
        return "lb";
    }

    @Override
    public String onPlaceholderRequest(Player player, String[] args) {
        try {
            var index = Integer.parseInt(args[args.length - 1]) - 1;
            var type = args[args.length - 2];
            var boardName = String.join("_", List.of(args).subList(0, args.length - 2));

            var list = lbm.getBoard(boardName);

            if (list == null) return serializer.serialize(Text.component(player, lbm.getEmptyPlaceholder()));
            if (list.size() - 1 < index) return serializer.serialize(Text.component(player, lbm.getEmptyPlaceholder()));

            var entry = list.get(index);

            return switch (type) {
                case "name" -> entry.getName();
                case "value" -> String.valueOf(entry.getValue());
                case "fvalue" -> AuroraAPI.formatNumber(entry.getValue());
                case "cvalue" -> lbm.formatValue(entry);
                default -> "Invalid type: " + type;
            };
        } catch (Exception ignored) {
            return "Invalid format, try: %aurora_lb_[board]_[name|value|fvalue|cvalue]_[number]%";
        }
    }

    @Override
    public List<String> getPatterns() {
        return lbm.getBoards().stream().flatMap(board -> Stream.of(
                board + "_name_[number]%",
                board + "_value_[number]",
                board + "_fvalue_[number]",
                board + "_cvalue_[number]"
        )).toList();
    }
}
