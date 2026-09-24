-- Lược đồ CSDL cho game đua xe thi đấu đối kháng online.
-- Chạy: mysql -u root -p < db/schema.sql

CREATE DATABASE IF NOT EXISTS racing
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
USE racing;

-- datastore-class: aggregate
CREATE TABLE IF NOT EXISTS players (
    player_id     INT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,            -- dạng sha256$<salt hex>$<hash hex>
    points        INT NOT NULL DEFAULT 0,
    wins          INT NOT NULL DEFAULT 0,
    losses        INT NOT NULL DEFAULT 0,
    draws         INT NOT NULL DEFAULT 0,
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_rank (points DESC, wins DESC)
) ENGINE=InnoDB;

-- datastore-class: aggregate
CREATE TABLE IF NOT EXISTS matches (
    match_id    INT AUTO_INCREMENT PRIMARY KEY,
    room_code   VARCHAR(20) NOT NULL,
    player1_id  INT NOT NULL,
    player2_id  INT NOT NULL,
    winner_id   INT NULL,                           -- NULL khi hòa hoặc hủy
    status      ENUM('PLAYING','FINISHED','ABORTED') NOT NULL DEFAULT 'PLAYING',
    end_reason  ENUM('FINISH','DRAW','QUIT','DISCONNECT','ABORTED') NULL,
    started_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ended_at    DATETIME NULL,
    CONSTRAINT fk_matches_p1     FOREIGN KEY (player1_id) REFERENCES players(player_id),
    CONSTRAINT fk_matches_p2     FOREIGN KEY (player2_id) REFERENCES players(player_id),
    CONSTRAINT fk_matches_winner FOREIGN KEY (winner_id)  REFERENCES players(player_id),
    INDEX idx_matches_p1 (player1_id),
    INDEX idx_matches_p2 (player2_id)
) ENGINE=InnoDB;

-- datastore-class: raw reason="diễn biến trận (va chạm, thoát, mất kết nối) để xem lại; ghi vài chục dòng mỗi trận"
CREATE TABLE IF NOT EXISTS match_events (
    event_id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    match_id   INT NOT NULL,
    player_id  INT NOT NULL,
    event_type VARCHAR(30) NOT NULL,                -- START, LANE_CHANGE, COLLISION, FINISH, QUIT, DISCONNECT, REMATCH
    event_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    payload    JSON NULL,
    CONSTRAINT fk_events_match  FOREIGN KEY (match_id)  REFERENCES matches(match_id),
    CONSTRAINT fk_events_player FOREIGN KEY (player_id) REFERENCES players(player_id),
    INDEX idx_events_match (match_id, event_time)
) ENGINE=InnoDB;
