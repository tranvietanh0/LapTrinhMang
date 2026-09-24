-- Dữ liệu thử: 6 tài khoản, mật khẩu đều là 123456.
-- Hash tạo bằng racing.server.db.PasswordHasher (sha256$<salt>$<hash>).
-- Chạy sau schema.sql: mysql -u root -p racing < db/seed.sql
USE racing;

INSERT INTO players (username, password_hash, points, wins, losses, draws) VALUES
  ('alice', 'sha256$f615b44b33fc7839436ddc8cb92cb6f1$d6e93e38440e52de8d6eaf53341b35444d9a4e65dc22b2c3a388002743024ab9', 12, 10, 3, 2),
  ('bob',   'sha256$04e03415a7f8b3968352b184ada617e6$81c80070fc93d7048fcca64c7cfa29d8d3b8b02200a9a8197ace8e4195be5cf8', 15, 12, 4, 3),
  ('carol', 'sha256$71096c95e78906b7680a4adbf3cd1bcc$3c8e45537a48f2d3993bfb10dd0581d8d9bb6fb16f7676e74049396bcb5cad6e',  9,  7, 6, 2),
  ('dave',  'sha256$3ec9bd67ba0f7694f2eca874cb4e6980$1a110a242d07399487ce4286f85a9aa9cc4a2727de2347e74e41e305adc104c2',  7,  6, 8, 1),
  ('erin',  'sha256$d1e0eb1731f71c4b4bc63509c0d26697$3e7f7a3892e4de53c4e8634577494089b54d554518c6d48edab301089f304674',  3,  2, 9, 1),
  ('frank', 'sha256$072fecc02d0103beeea95e04766d335f$479fb86e6675a0d083f349ce8b2d75ff267a2d8719898a6b185140b3a97ca28e',  0,  0, 0, 0)
ON DUPLICATE KEY UPDATE password_hash = VALUES(password_hash);
