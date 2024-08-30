package gg.auroramc.aurora.api.config.premade;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class PotionConfig {
    private String type = "WATER";

    private Boolean extended = false;

    private Boolean upgraded = false;

    public PotionConfig() {}

    public PotionConfig(PotionConfig other) {
        if(other == null) return;
        this.type = other.type;
        this.extended = other.extended;
        this.upgraded = other.upgraded;
    }
}
