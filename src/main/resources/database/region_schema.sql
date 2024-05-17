CREATE TABLE IF NOT EXISTS regions
(
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    world_name TEXT,
    region_x   INTEGER,
    region_z   INTEGER
);

CREATE TABLE IF NOT EXISTS chunks
(
    id        INTEGER PRIMARY KEY AUTOINCREMENT,
    region_id INTEGER,
    chunk_x   INTEGER,
    chunk_z   INTEGER,
    FOREIGN KEY (region_id) REFERENCES regions (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS blocks
(
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    chunk_id    INTEGER,
    block_x     INTEGER,
    block_y     INTEGER,
    block_z     INTEGER,
    player_uuid TEXT,
    FOREIGN KEY (chunk_id) REFERENCES chunks (id) ON DELETE CASCADE
);