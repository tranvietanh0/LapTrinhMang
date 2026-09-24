-- Xóa toàn bộ dữ liệu trận và đưa điểm về 0, giữ nguyên tài khoản.
-- Dùng trước mỗi buổi kiểm thử tích hợp: mysql -u root -p racing < db/reset-db.sql
USE racing;
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE match_events;
TRUNCATE TABLE matches;
SET FOREIGN_KEY_CHECKS = 1;
UPDATE players SET points = 0, wins = 0, losses = 0, draws = 0;
