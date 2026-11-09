# Game đua xe thi đấu đối kháng online (Java)

Bài tập lớn môn Lập trình mạng, lớp LTM-2026-2-N01. Client Java Swing, server TCP Socket, dữ liệu MySQL.

- Báo cáo thiết kế: [`report/LTM_Nhom_GameDuaXe_Java.docx`](report/)
- Tài liệu thuyết trình theo giáo trình Lập trình mạng: [`README_BAO_CAO.md`](README_BAO_CAO.md)
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

Bản setup Windows trên máy hiện tại: [cách chạy/tắt và vị trí dữ liệu](docs/WINDOWS-LOCAL-SETUP.md).

```
./mvnw package                       # Windows: mvnw.cmd package
java -jar server/target/racing-server.jar            # cổng 5000, dùng MySQL theo db.properties
java -jar client/target/racing-client.jar            # hoặc: racing-client.jar <host> <port>
```

## Chạy trên 2 máy Windows cùng mạng LAN

Mô hình đúng là **một máy chạy server + MySQL**, máy còn lại chỉ chạy client.
Không chạy MySQL và không chạy `server.jar` trên máy client.

### Máy A: máy chủ

1. Cài JDK 17 trở lên, sau đó kiểm tra:

   ```powershell
   java -version
   ```

2. Lấy mã nguồn về máy A, mở PowerShell tại thư mục gốc dự án và build:

   ```powershell
   mvnw.cmd package
   ```

   Nếu không muốn cài Maven, dùng `mvnw.cmd` đã có sẵn trong project. Nếu chỉ
   chép bản đóng gói, chép cả `server\target\racing-server.jar` và thư mục cấu
   hình/DB cần thiết.

3. Chạy MySQL bằng Docker Desktop (khuyến nghị):

   ```powershell
   docker compose up -d
   docker compose ps
   ```

   Docker sẽ tạo database `racing`, user `racing`, mật khẩu `racing` và nạp
   `schema.sql`/`seed.sql` ở lần tạo volume đầu tiên. Nếu đã có volume cũ,
   script seed không chạy lại; dùng `db\reset-db.sql` khi chỉ cần xóa lịch sử
   và đưa điểm về 0.

4. Tạo cấu hình DB ở **thư mục hiện tại khi chạy server**:

   ```powershell
   Copy-Item server\src\main\resources\db.properties.example db.properties
   ```

   Với Docker, nội dung tối thiểu là:

   ```properties
   db.url=jdbc:mysql://127.0.0.1:3306/racing?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Ho_Chi_Minh
   db.user=racing
   db.password=racing
   ```

   Không commit hoặc gửi file `db.properties`.

5. Tìm địa chỉ IPv4 của máy A trong mạng LAN:

   ```powershell
   ipconfig
   ```

   Lấy dòng `IPv4 Address`, ví dụ `192.168.1.25`. Không dùng `127.0.0.1` cho
   máy B; địa chỉ đó chỉ trỏ về chính máy đang chạy client.

6. Mở TCP port 5000 trên Windows Firewall của máy A. Mở PowerShell bằng
   quyền Administrator và chạy một lần:

   ```powershell
   New-NetFirewallRule -DisplayName "Racing Game TCP 5000" -Direction Inbound -Protocol TCP -LocalPort 5000 -Action Allow
   ```

7. Khởi động server:

   ```powershell
   java -jar server\target\racing-server.jar
   ```

   Khi thành công phải thấy log server lắng nghe cổng `5000`. Kiểm tra cổng:

   ```powershell
   Get-NetTCPConnection -LocalPort 5000 -State Listen
   ```

   Chỉ mở port `5000` cho game. Máy B không cần truy cập port MySQL `3306`.

### Máy B: máy khách

1. Cài JDK 17 trở lên.
2. Chép `client\target\racing-client.jar` từ máy A sang máy B, hoặc chép cả
   project rồi build trên máy B. Máy B không cần Maven, MySQL, Docker hay
   `db.properties`.
3. Kiểm tra kết nối tới máy A trước khi mở game, thay IP bằng IP thật của máy A:

   ```powershell
   Test-NetConnection 192.168.1.25 -Port 5000
   ```

   Kết quả cần có `TcpTestSucceeded : True`. Nếu `False`, kiểm tra hai máy có
   cùng mạng không, IP có đúng không, server đã chạy chưa và firewall máy A.
4. Chạy client, truyền IP máy A và port:

   ```powershell
   java -jar client\target\racing-client.jar 192.168.1.25 5000
   ```

   Hoặc chạy không truyền tham số rồi nhập `192.168.1.25:5000` vào ô
   `SERVER / HOST:PORT` trên màn hình đăng nhập.
5. Máy A cũng có thể chạy client thứ nhất:

   ```powershell
   java -jar client\target\racing-client.jar 127.0.0.1 5000
   ```

   Đăng nhập hai client bằng hai tài khoản khác nhau, ví dụ `alice` và `bob`,
   sau đó thách đấu.

### Nếu không có Docker/MySQL: demo nhanh

Trên máy A chỉ cần JDK 17 và chạy:

```powershell
java -jar server\target\racing-server.jar --memory
```

Sau đó máy B vẫn kết nối tới `IP_MAY_A:5000` như bình thường. Dữ liệu tài khoản
mẫu (`alice`, `bob`, ...) có mật khẩu `123456`, nhưng điểm và lịch sử sẽ mất khi
server tắt. Cách này phù hợp để kiểm tra mạng và demo gameplay, không phù hợp
để lưu kết quả báo cáo.

### Lỗi thường gặp

| Hiện tượng | Cách xử lý |
|---|---|
| `Connection refused` | Server chưa chạy, sai port, hoặc đang dùng nhầm `localhost`. |
| `TcpTestSucceeded : False` | Mở firewall TCP 5000, kiểm tra IP LAN và cùng mạng. |
| Server báo không tìm thấy DB config | Tạo `db.properties` trong thư mục mà PowerShell đang đứng khi chạy server. |
| Client báo mất kết nối sau khi server chạy | Kiểm tra server có bị lỗi MySQL không; xem log ngay tại terminal server. |
| Cổng 5000 đã được sử dụng | Đóng server cũ hoặc chạy server port khác, ví dụ `java -jar server\target\racing-server.jar 5001`, rồi cho client dùng port đó. |

Không mở port 3306 ra Internet và không đưa mật khẩu MySQL vào Git. Chỉ client
kết nối tới TCP 5000 của game server; server mới kết nối tới MySQL trên máy A.

Server nhận tham số `[port]` và `--memory`. `--memory` chạy không cần MySQL: dữ liệu nằm trong bộ nhớ với 6 tài khoản mẫu, mất hết khi tắt server; chỉ dùng để thử nhanh hoặc demo khi chưa cài MySQL.

```
java -jar server/target/racing-server.jar --memory
```

Chạy hai client trên cùng máy (hai cửa sổ), đăng nhập `alice` và `bob`, thách đấu, chấp nhận, đua bằng W/S/A/D hoặc phím mũi tên.

Âm thanh (hiệu ứng, tiếng động cơ, nhạc nền sảnh và nhạc đua) được tổng hợp bằng code trong `client/src/main/java/racing/client/audio/`, không dùng file âm thanh ngoài. Nhấn **M** ở bất kỳ màn hình nào (trừ khi đang gõ chữ) để tắt / bật tiếng; lựa chọn được nhớ cho lần mở sau. Máy không có thiết bị âm thanh thì game chạy im lặng và in một dòng `[audio]` ra stderr.

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
