# Game đua xe thi đấu đối kháng online (Java)

Bài tập lớn môn Lập trình mạng, lớp LTM-2026-2-N01. Client Java Swing, server TCP Socket, dữ liệu MySQL.

- Báo cáo thiết kế: [`report/LTM_Nhom_GameDuaXe_Java.docx`](report/)
- Kế hoạch chia việc: [`PLAN.md`](PLAN.md)

## Cấu trúc

| Module | Nội dung | Phụ trách |
|---|---|---|
| `common` | `Message`, `MessageType`, DTO, `GameConfig` dùng chung | Trần Việt Anh |
| `server` | `GameServer`, `net/ClientHandler`, `core/` (`SessionManager`, `InviteManager`, `Room`, `RoomManager`, `CarSim` trọng tài, `MatchService`, `AccountService`), `db/` (DAO, JDBC repository) | Phạm Thị Thu Phương (server), Trần Việt Anh (`db/` nền), Nguyễn Trần Mai Anh (lịch sử trận, sự kiện) |
| `client` | `net/NetworkClient`, `ui/` (`LoginFrame`, `RegisterDialog`, `LobbyFrame`, `InviteDialog`, `LeaderboardFrame`, `HistoryFrame`, `RaceLauncher`), `ui/race/` (màn hình đua) | Vũ Văn Hiếu (sảnh), Trần Việt Anh (đua), Nguyễn Trần Mai Anh (xếp hạng, đăng ký, lịch sử) |
| `db/` | `schema.sql`, `seed.sql`, `reset-db.sql`; mô tả bảng và truy vấn trong `docs/DB.md` | Trần Việt Anh (schema), Nguyễn Trần Mai Anh (DB.md, truy vấn) |
| `tools` | `ScriptedClient`: client dòng lệnh nói giao thức để test server và chạy kịch bản T10, T17 | Trần Việt Anh |
| `docs/` | `TEST-PLAN.md`: 19 kịch bản kiểm thử tích hợp; `DB.md`: cơ sở dữ liệu | Trần Việt Anh, Nguyễn Trần Mai Anh |

Trạng thái 24/09/2026: toàn bộ MVP và phần làm thêm (đăng ký, lịch sử trận, ghi diễn biến) đã có trên `main`, CI xanh. Việc còn lại của nhóm là chạy kiểm thử tích hợp theo `docs/TEST-PLAN.md` trên 2 máy, sửa lỗi phát hiện được và chuẩn bị demo.

## Yêu cầu máy

- JDK 17 trở lên (`java -version`). Không cần cài Maven, dùng `mvnw` / `mvnw.cmd` có sẵn.
- MySQL 8. Cách nhanh nhất là Docker: `docker compose up -d` (tự nạp schema và dữ liệu mẫu lần đầu).
  Không dùng Docker thì cài MySQL rồi chạy:
  ```
  mysql -u root -p < db/schema.sql
  mysql -u root -p racing < db/seed.sql
  ```

## Cấu hình kết nối DB

Sao chép `server/src/main/resources/db.properties.example` thành `db.properties` cùng thư mục và sửa user, mật khẩu.
File `db.properties` nằm trong `.gitignore`. Có thể thay bằng biến môi trường `RACING_DB_URL`, `RACING_DB_USER`, `RACING_DB_PASSWORD`.

Tài khoản mẫu: `alice`, `bob`, `carol`, `dave`, `erin`, `frank`, mật khẩu đều là `123456`.

## Build và chạy

```
./mvnw package                       # Windows: mvnw.cmd package
java -jar server/target/racing-server.jar            # cổng 5000, dùng MySQL theo db.properties
java -jar client/target/racing-client.jar            # hoặc: racing-client.jar <host> <port>
```

Server nhận tham số `[port]` và `--memory`. `--memory` chạy không cần MySQL: dữ liệu nằm trong bộ nhớ với 6 tài khoản mẫu, mất hết khi tắt server; chỉ dùng để thử nhanh hoặc demo khi chưa cài MySQL.

```
java -jar server/target/racing-server.jar --memory
```

Chạy hai client trên cùng máy (hai cửa sổ), đăng nhập `alice` và `bob`, thách đấu, chấp nhận, đua bằng W/S/A/D hoặc phím mũi tên.

Chạy nhanh khi đang phát triển:

```
./mvnw -pl server -am exec:java
./mvnw -pl client -am exec:java
```

Test tích hợp DAO chỉ chạy khi có DB:

```
RACING_TEST_DB_URL=jdbc:mysql://localhost:3306/racing ./mvnw test
```

## Chạy thử màn hình đua không cần server

```
mvnw.cmd -pl client -am exec:java -Dexec.mainClass=racing.client.ui.race.RaceDemo
```

`RaceDemo` giả lập server trong tiến trình: đếm ngược, đối thủ tự lái, va chạm, kết quả, hỏi thi đấu tiếp.
Lớp `RaceFrame` chỉ cần một hàm gửi `Message` và phương thức `handle(Message)` để `NetworkClient` gọi, xem chú thích đầu lớp.

## Kịch bản demo 5 phút

1. `docker compose up -d` (hoặc server `--memory`), chạy server, mở 2 client.
2. Đăng nhập `alice` và `bob`: sảnh hai bên thấy nhau, trạng thái Rảnh.
3. `alice` thách đấu `bob`: hộp thoại đếm ngược 30 s, bấm Chấp nhận.
4. Đếm ngược 3-2-1-GO, đua 2500 m, 3 làn, 48 xe cộ chạy cùng chiều giống nhau hai bên (mỗi làn một tốc độ); giữ W để tăng tốc tới 360 km/h, giữ S để phanh; đâm vào xe cộ thì nổ, xe khựng 1,5 s. Màn hình kiểu game đua xe cổ điển: camera cuộn, cảnh quan, HUD, minimap.
5. Về đích: hộp kết quả, điểm mới; hỏi thi đấu tiếp, một bên từ chối thì về sảnh.
6. Mở Bảng xếp hạng và Lịch sử trận để thấy điểm và trận vừa đấu (với MySQL thì dữ liệu còn sau khi tắt server).
7. Thoát trận giữa chừng (Esc) để thấy xử thua; đăng ký tài khoản mới ở màn hình đăng nhập.

## Test server không cần giao diện

```
java -jar tools/target/racing-tools.jar "login alice 123456; wait LOGIN_RESULT; leaderboard; wait LEADERBOARD; exit"
java -jar tools/target/racing-tools.jar --label B --auto-accept --script tools/scripts/t10-draw-b.txt
```

`--help` liệt kê đủ lệnh. Kịch bản mẫu trong `tools/scripts/`.

## Đóng gói bản chạy

```
scripts\build-dist.cmd        # hoặc scripts/build-dist.sh
```

Tạo thư mục `dist/` gồm 3 jar, SQL, `db.properties.example`, `run-server` và `run-client`.

## Tạo hash mật khẩu cho tài khoản mới

```
./mvnw -q -pl server -am compile exec:java -Dexec.mainClass=racing.server.db.PasswordHasher -Dexec.args="matkhau"
```

## Quy ước Git

Mỗi task một nhánh `feature/<mã-task>-<tên>`, mở Pull Request vào `main`, review chéo rồi merge. Chi tiết trong `PLAN.md`.
