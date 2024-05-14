package gg.auroramc.auroralib.api.user.storage.sql;

public record DatabaseCredentials(String host, int port, String database, String username, String password, boolean ssl) {

}
