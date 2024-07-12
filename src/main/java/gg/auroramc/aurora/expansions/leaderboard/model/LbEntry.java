package gg.auroramc.aurora.expansions.leaderboard.model;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class LbEntry {
    private final UUID uuid;
    private String name;
    private final String board;
    private double value;
    private long position;


    public LbEntry(UUID uuid, String name, String board, double value, long position) {
        this.uuid = uuid;
        this.name = name;
        this.board = board;
        this.value = value;
        this.position = position;
    }
}
