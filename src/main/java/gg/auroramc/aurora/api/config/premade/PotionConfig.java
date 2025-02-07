package gg.auroramc.aurora.api.config.premade;

import lombok.*;

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PotionConfig {
    @Builder.Default
    private String type = "WATER";
    @Builder.Default
    private Boolean extended = false;
    @Builder.Default
    private Boolean upgraded = false;

    public PotionConfig(PotionConfig other) {
        if(other == null) return;
        this.type = other.type;
        this.extended = other.extended;
        this.upgraded = other.upgraded;
    }
}
