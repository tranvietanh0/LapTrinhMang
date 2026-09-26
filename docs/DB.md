# Cơ sở dữ liệu `racing`

MySQL 8, charset `utf8mb4`. Tạo bằng `db/schema.sql`, dữ liệu mẫu `db/seed.sql`, xóa sạch để test lại bằng `db/reset-db.sql`.
Ba bảng: `players` (tài khoản và điểm), `matches` (mỗi ván một dòng), `match_events` (diễn biến trong ván).

Nhanh nhất là Docker: `docker compose up -d` rồi kết nối `jdbc:mysql://localhost:3306/racing`, user `racing`, mật khẩu `racing`.

## Bảng `players`

| Cột | Kiểu | Ý nghĩa |
|---|---|---|
| `player_id` | INT, khóa chính, tự tăng | Mã người chơi, dùng làm khóa ngoại ở hai bảng còn lại |
| `username` | VARCHAR(50), UNIQUE | Tên đăng nhập, 3–50 ký tự chữ, số, `_` |
| `password_hash` | VARCHAR(255) | Mật khẩu đã băm dạng `sha256$<salt hex>$<hash hex>`, sinh bởi `PasswordHasher` |
| `points` | INT | Tổng điểm: thắng +1, hòa +1, thua +0 |
| `wins` / `losses` / `draws` | INT | Số trận thắng / thua / hòa |
| `created_at` | DATETIME | Thời điểm tạo tài khoản |

Chỉ mục `idx_rank (points DESC, wins DESC)` phục vụ bảng xếp hạng.

## Bảng `matches`

| Cột | Kiểu | Ý nghĩa |
|---|---|---|
| `match_id` | INT, khóa chính, tự tăng | Mã ván đấu |
| `room_code` | VARCHAR(20) | Mã phòng (`R1`, `R2`…); thi đấu tiếp trong cùng phòng tạo dòng mới cùng `room_code` |
| `player1_id` / `player2_id` | INT, FK → players | Hai người chơi (player1 là người mời) |
| `winner_id` | INT NULL, FK → players | Người thắng; NULL khi hòa hoặc hủy |
| `status` | ENUM PLAYING / FINISHED / ABORTED | Đang đua / đã có kết quả / bị hủy |
| `end_reason` | ENUM FINISH / DRAW / QUIT / DISCONNECT / ABORTED, NULL | Lý do kết thúc; NULL khi còn đang đua |
| `started_at` | DATETIME | Lúc tạo phòng (trước đếm ngược) |
| `ended_at` | DATETIME NULL | Lúc có kết quả |

Quy tắc cộng điểm (thực hiện trong một transaction ở `MatchDAO.saveResult`):

| `end_reason` | `winner_id` | Người thắng | Người thua |
|---|---|---|---|
| FINISH | người về đích trước | +1 điểm, +1 wins | +1 losses |
| DRAW | NULL | mỗi người +1 điểm, +1 draws | |
| QUIT | người còn lại | +1 điểm, +1 wins | người thoát +1 losses |
| DISCONNECT | người còn lại | +1 điểm, +1 wins | người rớt mạng +1 losses |
| ABORTED | NULL | không cộng gì | |

## Bảng `match_events`

| Cột | Kiểu | Ý nghĩa |
|---|---|---|
| `event_id` | BIGINT, khóa chính, tự tăng | |
| `match_id` | INT, FK → matches | Ván đấu |
| `player_id` | INT, FK → players | Người gây ra sự kiện |
| `event_type` | VARCHAR(30) | `START`, `COLLISION`, `FINISH`, `QUIT`, `DISCONNECT`, `REMATCH` |
| `event_time` | DATETIME(3) | Thời điểm, chính xác mili giây |
| `payload` | JSON NULL | Dữ liệu kèm theo, ví dụ `{"lane":0,"distance":312,"obstacle":300}` |

Payload theo loại: `START` `{"lane":1}` · `COLLISION` `{"lane","distance","obstacle"}` (`obstacle` = vị trí xe cộ lúc va chạm, vì xe cộ chạy cùng chiều) · `FINISH` `{"distance","elapsedMillis"}` · `QUIT`/`DISCONNECT` `{"distance"}` · `REMATCH` NULL.

## Truy vấn hay dùng

1. Bảng xếp hạng (đúng thứ tự client hiển thị):

```sql
SELECT RANK() OVER (ORDER BY points DESC, wins DESC, username) AS hang,
       username, points, wins, losses, draws
FROM players
ORDER BY hang
LIMIT 100;
```

2. Lịch sử 20 trận gần nhất của một người (ví dụ `alice`), kèm tên đối thủ và kết quả:

```sql
SELECT m.match_id, m.started_at, m.ended_at,
       IF(m.player1_id = me.player_id, p2.username, p1.username) AS doi_thu,
       CASE WHEN m.winner_id = me.player_id THEN 'THẮNG'
            WHEN m.winner_id IS NULL AND m.end_reason = 'DRAW' THEN 'HÒA'
            WHEN m.winner_id IS NULL THEN 'HỦY'
            ELSE 'THUA' END AS ket_qua,
       m.end_reason
FROM matches m
JOIN players me ON me.username = 'alice'
JOIN players p1 ON p1.player_id = m.player1_id
JOIN players p2 ON p2.player_id = m.player2_id
WHERE (m.player1_id = me.player_id OR m.player2_id = me.player_id)
  AND m.status <> 'PLAYING'
ORDER BY m.started_at DESC, m.match_id DESC
LIMIT 20;
```

3. Số trận theo ngày và số trận theo từng lý do kết thúc:

```sql
SELECT DATE(started_at) AS ngay, COUNT(*) AS so_tran,
       SUM(end_reason = 'FINISH') AS ve_dich, SUM(end_reason = 'DRAW') AS hoa,
       SUM(end_reason = 'QUIT') AS thoat, SUM(end_reason = 'DISCONNECT') AS rot_mang
FROM matches
WHERE status <> 'PLAYING'
GROUP BY DATE(started_at)
ORDER BY ngay DESC;
```

4. Xem lại diễn biến một trận theo thứ tự thời gian:

```sql
SELECT e.event_time, p.username, e.event_type,
       JSON_EXTRACT(e.payload, '$.lane') AS lane,
       JSON_EXTRACT(e.payload, '$.distance') AS distance
FROM match_events e
JOIN players p ON p.player_id = e.player_id
WHERE e.match_id = 1
ORDER BY e.event_time, e.event_id;
```

5. Số lần va chạm của mỗi người (ai lái ẩu nhất):

```sql
SELECT p.username, COUNT(*) AS va_cham
FROM match_events e JOIN players p ON p.player_id = e.player_id
WHERE e.event_type = 'COLLISION'
GROUP BY p.username
ORDER BY va_cham DESC;
```

6. Kiểm tra tính nhất quán: tổng điểm trong `players` phải khớp số trận thắng + hòa trong `matches`:

```sql
SELECT p.username, p.points,
       (SELECT COUNT(*) FROM matches m WHERE m.winner_id = p.player_id) +
       (SELECT COUNT(*) FROM matches m WHERE m.end_reason = 'DRAW'
          AND (m.player1_id = p.player_id OR m.player2_id = p.player_id)) AS diem_tinh_lai
FROM players p
HAVING p.points <> diem_tinh_lai;
```

Kết quả rỗng nghĩa là dữ liệu khớp (dùng ở bước kiểm tra DB của `docs/TEST-PLAN.md`).

## Dọn dữ liệu trước một buổi test

```sql
SOURCE db/reset-db.sql;
```

Lệnh xóa `match_events`, `matches` và đưa điểm mọi người về 0 nhưng giữ tài khoản.
