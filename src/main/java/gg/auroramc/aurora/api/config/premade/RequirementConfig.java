package gg.auroramc.aurora.api.config.premade;

import lombok.Getter;

import java.util.List;

@Getter
public class RequirementConfig {
    private String requirement;
    private List<String> denyActions;
}
