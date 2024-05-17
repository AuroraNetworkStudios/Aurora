package gg.auroramc.aurora.api.events.region;

import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class RegionBlockBreakEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    private final OfflinePlayer playerWhoPlaced;
    private final Player playerWhoBroke;
    private final Block block;

    private final boolean natural;

    public RegionBlockBreakEvent(Player playerWhoBroke, OfflinePlayer playerWhoPlaced, Block block, boolean natural) {
        this.playerWhoPlaced = playerWhoPlaced;
        this.playerWhoBroke = playerWhoBroke;
        this.block = block;
        this.natural = natural;
    }

    /**
     * Can be null if it's like sand fall or something passive.
     *
     * @return the player who broke the block
     */
    public Player getPlayerWhoBroke() {
        return playerWhoBroke;
    }


    /**
     * Can be null if it's a natural block break
     *
     * @return the player who originally placed the block
     */
    public OfflinePlayer getPlayerWhoPlaced() {
        return playerWhoPlaced;
    }

    public Block getBlock() {
        return block;
    }

    public boolean isNatural() {
        return natural;
    }
}
