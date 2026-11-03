package gg.auroramc.aurora.internal.user.storage.sql;

public record DatabaseCredentials(String host, int port, String database, String username, String password, boolean ssl) {

}
