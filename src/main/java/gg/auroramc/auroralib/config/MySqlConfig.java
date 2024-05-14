package gg.auroramc.auroralib.config;

import lombok.Getter;

@Getter
public class MySqlConfig {
    private String host = "127.0.0.1";
    private Integer port = 3306;
    private String database = "db name";
    private String username = "username";
    private String password = "password";
    private Boolean ssl = false;
    private Integer networkLatency = 500;
    private Integer poolSize = 10;
    private Integer syncRetryCount = 3;
}
