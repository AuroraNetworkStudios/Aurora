package gg.auroramc.aurora.api.config.premade;

import lombok.Getter;
import lombok.Setter;

public class PotionConfig {
    @Getter
    @Setter
    private String type = "WATER";

    @Getter
    @Setter
    private Boolean extended = false;

    @Getter
    @Setter
    private Boolean upgraded = false;

    public PotionConfig() {}

    public PotionConfig(PotionConfig other) {
        if(other == null) return;
        this.type = other.type;
        this.extended = other.extended;
        this.upgraded = other.upgraded;
    }
}
