CREATE TABLE IF NOT EXISTS `%user_table%`
(
    id INT AUTO_INCREMENT PRIMARY KEY,
    player_uuid VARCHAR(36) NOT NULL,
    holder      VARCHAR(50) NOT NULL,
    data        MEDIUMTEXT,
    UNIQUE KEY unique_player_holder (player_uuid, holder),
    INDEX idx_player_uuid (player_uuid)
) CHARACTER SET utf8;

CREATE TABLE IF NOT EXISTS `%sync_table%`
(
    player_uuid VARCHAR(36) NOT NULL PRIMARY KEY,
    created     TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
) CHARACTER SET utf8;