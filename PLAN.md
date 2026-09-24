# Kế hoạch triển khai – Game đua xe thi đấu đối kháng online (Java)

Nhóm 4 người, 4 tuần, từ **thứ Hai 29/09/2026** đến **Chủ nhật 26/10/2026** (mốc thời gian là đề xuất, nhóm chỉnh theo lịch môn học). Kiến trúc, tên lớp và giao thức bám theo báo cáo `report/LTM_Nhom_GameDuaXe_Java.docx`.

| Thành viên | Vai trò | Luồng việc | Mã task |
|---|---|---|---|
| Phạm Thị Thu Phương | Server | Kết nối, phiên, lời mời, phòng đua, trọng tài | S1–S7 |
| Vũ Văn Hiếu | Client sảnh | Mạng phía client, đăng nhập, sảnh, lời mời, xếp hạng | C1–C7 |
| Nguyễn Trần Mai Anh | Client đua | Màn hình đua, điều khiển, đồng bộ, kết quả | R1–R6 |
| Trần Việt Anh | Dữ liệu + nền tảng | Maven, module `common`, MySQL, DAO, kiểm thử tích hợp, đóng gói | D1–D7 |

---

## 1. Phạm vi

**Bắt buộc (MVP, phải chạy được khi demo)**

1. Đăng nhập bằng tài khoản trong MySQL.
2. Sảnh: danh sách online (tên, điểm, trạng thái Rảnh / Đang thi đấu), tự cập nhật.
3. Thách đấu, chấp nhận / từ chối, tự hủy sau 30 s.
4. Phòng đua: đếm ngược 3-2-1, hai đường đua song song, cùng chướng ngại vật, đồng bộ vị trí hai xe.
5. Kết quả: thắng 1 điểm, thua 0, hòa mỗi người 1; lưu DB.
6. Thi đấu tiếp khi cả hai đồng ý; thoát trận bị xử thua; mất kết nối quá 15 s bị xử thua.
7. Bảng xếp hạng theo điểm giảm dần, số trận thắng giảm dần.

**Làm thêm nếu còn thời gian (tuần 4):** đăng ký tài khoản, âm thanh, lịch sử trận của từng người, xem lại diễn biến từ `match_events`.

---

## 2. Cấu trúc project và quy ước

### 2.1. Maven multi-module (JDK 17)

```
LapTrinhMang/
├── pom.xml                      # pom cha, quản lý version
├── common/                      # dùng chung client + server
│   └── src/main/java/racing/common/
│       ├── net/Message.java, MessageType.java
│       ├── dto/  LoginRequest, LoginResult, PlayerInfo, InviteInfo, InviteReply,
│       │         InviteResult, MatchStart, Obstacle, CarState, RaceState,
│       │         MatchResult, RematchReply, RankRow
│       └── GameConfig.java      # PORT=5000, TRACK_LENGTH=1000, LANES=3, TICK_MS=50,
│                                # INVITE_TIMEOUT_S=30, DISCONNECT_TIMEOUT_S=15, MAX_SPEED=200
├── server/
│   └── src/main/java/racing/server/
│       ├── GameServer.java
│       ├── net/ClientHandler.java
│       ├── core/SessionManager, InviteManager, RoomManager, Room, MatchService
│       └── db/DbConnection, PlayerDAO, MatchDAO
│   └── src/main/resources/db.properties.example
├── client/
│   └── src/main/java/racing/client/
│       ├── ClientMain.java
│       ├── net/NetworkClient.java
│       ├── model/ClientState, CarModel
│       ├── ui/LoginFrame, LobbyFrame, InviteDialog, LeaderboardFrame
│       └── ui/race/RaceFrame, RacePanel, ResultDialog, RematchDialog
├── db/schema.sql, db/seed.sql
└── report/
```

### 2.2. Git

- Nhánh `main` luôn build được. Mỗi task làm trên nhánh `feature/<mã-task>-<tên-ngắn>` (ví dụ `feature/S3-invite-manager`), xong thì mở Pull Request vào `main`.
- Review chéo trước khi merge: Phương ⇄ Việt Anh, Hiếu ⇄ Mai Anh. Người review chạy thử, không chỉ đọc.
- Commit message: `<phần>: <việc>` – ví dụ `server: add InviteManager 30s timeout`.
- Không commit `db.properties` thật, chỉ commit `db.properties.example`.

### 2.3. Hợp đồng chung phải chốt trước (đóng băng cuối tuần 1)

| Hợp đồng | Chủ trì | Người duyệt | Hạn |
|---|---|---|---|
| `MessageType` + toàn bộ DTO trong `common` | Việt Anh | Phương, Hiếu | Thứ Tư 01/10 |
| `GameConfig` (hằng số game) | Việt Anh | Mai Anh | Thứ Tư 01/10 |
| `db/schema.sql` | Việt Anh | Phương | Thứ Năm 02/10 |
| Luật va chạm và cách tính hòa (cùng tick) | Phương | Mai Anh | Thứ Sáu 03/10 |

Sau khi đóng băng, muốn đổi phải báo cả nhóm trên Zalo và cập nhật cả 3 module trong cùng một PR.

---

## 3. Phân công chi tiết

Ước lượng theo giờ làm thực tế. "Phụ thuộc" là task phải xong trước (có thể dùng stub để làm song song).

### 3.1. Phạm Thị Thu Phương – Server

| Mã | Tuần | Việc | File / lớp | Tiêu chí hoàn thành | Giờ | Phụ thuộc |
|---|---|---|---|---|---|---|
| S1 | 1 | Khung server: mở `ServerSocket` cổng 5000, `ExecutorService`, `ClientHandler` với vòng `readObject`, `send()` có `synchronized`, log ra console | `GameServer`, `net/ClientHandler` | 3 client kết nối cùng lúc, gửi PING nhận PONG, đóng client không làm server chết | 6 | D1 |
| S2 | 1 | `SessionManager`: đăng nhập qua `PlayerDAO` (stub trả true khi DAO chưa xong), map online, trạng thái FREE / IN_MATCH, phát `ONLINE_LIST` mỗi khi đổi, đăng xuất | `core/SessionManager` | 2 client đăng nhập, mỗi bên thấy bên kia; tắt 1 client thì bên còn lại thấy danh sách rút xuống | 5 | S1, D2 |
| S3 | 2 | `InviteManager`: `INVITE` → `INVITE_RECEIVED`; `INVITE_REPLY`; hẹn giờ 30 s bằng `ScheduledExecutorService`; trả `INVITE_RESULT` REJECTED / TIMEOUT / BUSY | `core/InviteManager` | Mời người đang đấu nhận BUSY; không trả lời 30 s nhận TIMEOUT; cả hai chuyển IN_MATCH khi chấp nhận | 5 | S2 |
| S4 | 2 | `Room` + `RoomManager`: tạo phòng, sinh chướng ngại vật theo seed (giống nhau 2 bên), gửi `MATCH_START`, `COUNTDOWN` 3-2-1-0, tick 50 ms nhận `CAR_STATE`, cắt ngưỡng tốc độ / quãng đường, tính va chạm, phát `RACE_UPDATE` | `core/Room`, `core/RoomManager` | 2 client thấy xe nhau chạy; gửi tốc độ 999 bị cắt về 200; va chạm làm tốc độ về 0 trong 1 s | 8 | S3, D2 |
| S5 | 3 | `MatchService`: xét về đích, hòa cùng tick, thoát, mất kết nối; gọi `MatchDAO.saveResult`; gửi `MATCH_RESULT`; luồng `REMATCH_ASK` / `REMATCH_REPLY`, chỉ tạo ván mới khi cả hai đồng ý | `core/MatchService` | Điểm trong DB đúng sau mỗi tình huống ở mục 5; rematch sinh lại chướng ngại vật và reset quãng đường | 6 | S4, D4 |
| S6 | 3 | Mất kết nối: `setSoTimeout(15000)`, xử lý `IOException` ở mọi trạng thái (trong sảnh, đang mời, đang đua), dọn phòng, cập nhật online | `ClientHandler`, `SessionManager`, `RoomManager` | Rút mạng client đang đua: sau ≤ 15 s đối thủ nhận thắng, DB ghi DISCONNECT | 4 | S5 |
| S7 | 4 | Chịu tải và đồng thời: 4 client, 2 phòng chạy song song; rà `synchronized`, `ConcurrentHashMap`; sửa lỗi từ kiểm thử tích hợp | toàn bộ server | 2 phòng đua cùng lúc không lẫn dữ liệu; không deadlock sau 10 trận liên tiếp | 4 | S6 |

### 3.2. Vũ Văn Hiếu – Client: mạng, đăng nhập, sảnh

| Mã | Tuần | Việc | File / lớp | Tiêu chí hoàn thành | Giờ | Phụ thuộc |
|---|---|---|---|---|---|---|
| C1 | 1 | `NetworkClient`: connect, luồng nhận `readObject`, `send()`, đăng ký listener theo `MessageType`, đẩy về Swing bằng `invokeLater`; PING mỗi 5 s | `net/NetworkClient` | Mai Anh gọi được `net.send()` và `net.on(RACE_UPDATE, ...)` mà không cần biết socket | 5 | D2 |
| C2 | 1 | `LoginFrame` + `ClientState`: nhập tài khoản, gửi `LOGIN`, hiện lỗi khi `LOGIN_RESULT` thất bại, chuyển sang sảnh khi thành công | `ui/LoginFrame`, `model/ClientState` | Đăng nhập sai báo lỗi, đúng thì mở sảnh | 4 | C1, S2 |
| C3 | 2 | `LobbyFrame`: `JTable` từ `ONLINE_LIST` (Tên, Điểm, Thắng, Trạng thái), nút Thách đấu chỉ với người Rảnh, trạng thái "đang chờ trả lời", xử lý `INVITE_RESULT` | `ui/LobbyFrame` | Danh sách tự cập nhật không cần bấm Làm mới; mời xong nút khóa đến khi có kết quả | 6 | C2, S3 |
| C4 | 2 | `InviteDialog`: `javax.swing.Timer` đếm 30 → 0, thanh tiến trình, nút Chấp nhận / Từ chối gửi `INVITE_REPLY`, tự đóng khi hết giờ hoặc server hủy | `ui/InviteDialog` | Không bấm gì 30 s thì hộp thoại tự đóng và người mời nhận TIMEOUT | 4 | C3 |
| C5 | 3 | Điều hướng màn hình: nhận `MATCH_START` mở `RaceFrame` của Mai Anh, về sảnh khi phòng đóng; `LeaderboardFrame` từ `LEADERBOARD` | `ui/LobbyFrame`, `ui/LeaderboardFrame` | Từ sảnh vào trận và quay về sảnh 3 lần liên tiếp không lỗi; bảng xếp hạng đúng thứ tự | 4 | C4, R3, D5 |
| C6 | 3 | Mất kết nối phía client: server tắt thì hiện thông báo và quay về `LoginFrame`; đăng xuất gửi `LOGOUT` | `net/NetworkClient`, `ui/*` | Tắt server khi đang ở sảnh: client báo lỗi rõ ràng, không treo | 3 | C5 |
| C7 | 4 | Hoàn thiện UX (thông báo lỗi, phím Enter đăng nhập, cỡ cửa sổ), sửa lỗi từ kiểm thử | `ui/*` | Không còn lỗi mức "chặn demo" trong danh sách kiểm thử | 4 | C6 |

### 3.3. Nguyễn Trần Mai Anh – Client: màn hình đua

| Mã | Tuần | Việc | File / lớp | Tiêu chí hoàn thành | Giờ | Phụ thuộc |
|---|---|---|---|---|---|---|
| R1 | 1 | `RacePanel` vẽ tĩnh bằng `paintComponent`: hai đường đua, 3 làn, vạch xuất phát / đích, chướng ngại vật từ `List<Obstacle>` (dữ liệu giả), quy đổi 1000 m → pixel | `ui/race/RacePanel` | Chạy độc lập bằng `main` thử với dữ liệu giả, cửa sổ 1200×800 hiển thị đúng như Hình 6 báo cáo | 6 | D2 (`Obstacle`) |
| R2 | 2 | `CarModel` + điều khiển: Key Bindings W/S/A/D và mũi tên, `Timer` 50 ms cập nhật quãng đường theo tốc độ, HUD tốc độ / quãng đường / thanh tiến trình | `model/CarModel`, `ui/race/RacePanel` | Xe chạy mượt, đổi làn tức thì, không vượt 200 km/h | 6 | R1 |
| R3 | 2 | Nối mạng: mỗi tick gửi `CAR_STATE` qua `NetworkClient`; overlay đếm ngược từ `COUNTDOWN`, khóa phím đến khi nhận 0 (GO) | `ui/race/RaceFrame` | Server log nhận 20 `CAR_STATE`/giây; không bấm được phím trước GO | 4 | R2, C1, S4 |
| R4 | 3 | Nhận `RACE_UPDATE`: hiệu chỉnh xe mình, cập nhật xe đối thủ; hiệu ứng va chạm (nháy đỏ 0,5 s); dừng `Timer` khi trận kết thúc | `ui/race/RacePanel` | Hai máy thấy vị trí như nhau (lệch < 1 tick); va chạm hiện đúng lúc | 5 | R3 |
| R5 | 3 | `ResultDialog` (thắng / thua / hòa + lý do + điểm mới), `RematchDialog` (Đồng ý / Từ chối gửi `REMATCH_REPLY`), nút Thoát trận có xác nhận gửi `QUIT_MATCH`, phím Esc | `ui/race/ResultDialog`, `RematchDialog` | Đủ 4 loại kết quả hiển thị đúng; rematch mở lại đường đua mới | 4 | R4, S5 |
| R6 | 4 | Hiệu năng và tinh chỉnh: double buffering, không giật khi 2 client cùng máy; tỉ lệ pixel / m; màu xe theo người chơi; sửa lỗi từ kiểm thử | `ui/race/*` | CPU client < 15 % khi đua; không còn lỗi hiển thị | 4 | R5 |

### 3.4. Trần Việt Anh – Nền tảng, dữ liệu, kiểm thử tích hợp

| Mã | Tuần | Việc | File / lớp | Tiêu chí hoàn thành | Giờ | Phụ thuộc |
|---|---|---|---|---|---|---|
| D1 | 1 (ngày 1–2) | Khởi tạo Maven multi-module, `.gitignore`, plugin chạy (`exec-maven-plugin`) và đóng gói (`maven-shade-plugin`), CI đơn giản (GitHub Actions `mvn package`) | `pom.xml` × 4, `.github/workflows/build.yml` | `mvn package` xanh trên máy cả 4 người và trên GitHub | 3 | – |
| D2 | 1 (hạn 01/10) | Module `common`: `MessageType`, `Message`, toàn bộ DTO (`Serializable`, có `serialVersionUID`), `GameConfig` | `common/**` | Được Phương và Hiếu duyệt; không đổi tên trường sau khi đóng băng | 6 | D1 |
| D3 | 1 | MySQL: `schema.sql` (3 bảng, chỉ mục `(points, wins)`), `seed.sql` 6 tài khoản thử (mật khẩu `123456`), `DbConnection` đọc `db.properties`, `PlayerDAO.findByUsername`, băm mật khẩu SHA-256 + salt | `db/*.sql`, `server/db/DbConnection`, `PlayerDAO` | Chạy `schema.sql` + `seed.sql` trên máy sạch thành công; test đăng nhập đúng / sai | 5 | D1 |
| D4 | 2 | `MatchDAO.createMatch`, `saveResult` (transaction: cập nhật `matches` + cộng điểm 2 người), `addEvent`; `PlayerDAO.getLeaderboard` | `server/db/MatchDAO`, `PlayerDAO` | Test JUnit trên DB thử: sau thắng / hòa / quit điểm đúng; rollback khi lỗi giữa chừng | 6 | D3 |
| D5 | 3 | Hỗ trợ tích hợp DAO vào `MatchService` cùng Phương; cung cấp `LEADERBOARD` cho Hiếu; script `reset-db.sql` | `server/db/*`, `db/reset-db.sql` | Bảng xếp hạng đúng thứ tự với dữ liệu 6 tài khoản seed | 3 | D4, S5 |
| D6 | 3–4 | Kiểm thử tích hợp toàn bộ kịch bản ở mục 5 trên 2 máy trong cùng mạng LAN, ghi bug lên GitHub Issues gán đúng người | GitHub Issues | Mỗi kịch bản có kết quả PASS / FAIL kèm ảnh chụp; bug chặn demo được sửa trước 24/10 | 6 | M3 |
| D7 | 4 | Đóng gói `server.jar`, `client.jar`; `README.md` hướng dẫn cài MySQL, cấu hình, chạy; kịch bản demo 5 phút | `README.md`, `dist/` | Người ngoài nhóm chạy được theo README trong 10 phút | 3 | D6 |

Tổng ước lượng: Phương 38 h, Hiếu 30 h, Mai Anh 29 h, Việt Anh 32 h (Việt Anh gánh thêm phần nền tảng và kiểm thử nên phần code ít hơn).

---

## 4. Mốc tích hợp

| Mốc | Ngày | Phải chạy được | Cách nghiệm thu |
|---|---|---|---|
| M1 – Nền tảng | CN 05/10 | `mvn package` xanh; server chạy; client đăng nhập bằng tài khoản seed; hai client thấy nhau trong sảnh; `RacePanel` vẽ được với dữ liệu giả | Demo trên Zalo call, quay màn hình 1 phút |
| M2 – Đua được | CN 12/10 | Mời → chấp nhận → phòng → đếm ngược → hai xe chạy và đồng bộ trên 2 máy | Hai máy khác nhau trong cùng LAN |
| M3 – Trọn vòng chơi | CN 19/10 | Kết quả thắng / hòa / thoát / mất kết nối, điểm lưu DB, rematch, bảng xếp hạng | Chạy đủ kịch bản mục 5 |
| M4 – Bàn giao | CN 26/10 | Kịch bản kiểm thử PASS, jar + README, báo cáo và slide demo | Chạy demo thử toàn bộ 1 lần từ máy sạch |

Ai chậm mốc quá 2 ngày phải báo nhóm ngay để chia lại việc, không tự gánh im lặng.

---

## 5. Kịch bản kiểm thử tích hợp (tuần 3–4)

| # | Kịch bản | Kết quả mong đợi | Người test | Người sửa nếu lỗi |
|---|---|---|---|---|
| T1 | Đăng nhập sai mật khẩu | Báo lỗi, không vào sảnh | Việt Anh | Hiếu / Việt Anh |
| T2 | Đăng nhập cùng tài khoản ở 2 máy | Máy sau bị từ chối (hoặc máy trước bị đá, nhóm chọn 1) | Việt Anh | Phương |
| T3 | 3 client online, 1 client thoát | 2 client còn lại thấy danh sách cập nhật trong 1 s | Hiếu | Phương |
| T4 | Mời người đang thi đấu | Nhận BUSY, không mở hộp thoại ở người kia | Hiếu | Phương |
| T5 | Không trả lời lời mời 30 s | Hộp thoại tự đóng, người mời nhận TIMEOUT, cả hai vẫn Rảnh | Hiếu | Phương / Hiếu |
| T6 | Từ chối lời mời | Người mời nhận REJECTED | Hiếu | Phương |
| T7 | Chấp nhận, đếm ngược, đua | Cả hai vào phòng, đếm 3-2-1, phím chỉ hoạt động sau GO | Mai Anh | Phương / Mai Anh |
| T8 | Va chạm chướng ngại vật | Tốc độ về 0 trong 1 s trên cả hai màn hình | Mai Anh | Phương / Mai Anh |
| T9 | Về đích trước | Người về trước +1, DB `end_reason = FINISH`, xếp hạng đổi | Việt Anh | Phương / Việt Anh |
| T10 | Về đích cùng tick (mô phỏng bằng script gửi cùng lúc) | Hòa, mỗi người +1, `winner_id NULL`, `end_reason = DRAW` | Việt Anh | Phương |
| T11 | Bấm Thoát trận giữa chừng | Người thoát 0 điểm, đối thủ +1, `QUIT` | Mai Anh | Phương / Mai Anh |
| T12 | Rút mạng client đang đua | Sau ≤ 15 s đối thủ nhận thắng, `DISCONNECT`, client kia về sảnh | Việt Anh | Phương / Hiếu |
| T13 | Cả hai đồng ý thi đấu tiếp | Ván mới trong cùng phòng, chướng ngại vật khác, quãng đường về 0 | Mai Anh | Phương / Mai Anh |
| T14 | Một bên từ chối thi đấu tiếp | Cả hai về sảnh, trạng thái Rảnh | Hiếu | Phương / Hiếu |
| T15 | 2 phòng đua cùng lúc (4 client) | Không lẫn dữ liệu giữa phòng | Việt Anh | Phương |
| T16 | Tắt server khi client đang ở sảnh | Client báo mất kết nối, về màn hình đăng nhập, không treo | Hiếu | Hiếu |
| T17 | Gửi `CAR_STATE` giả tốc độ 999 (test bằng client sửa) | Server cắt về 200, không thắng nhanh bất thường | Việt Anh | Phương |

---

## 6. Phối hợp

- **Họp nhanh 15 phút** tối thứ Hai, Tư, Sáu trên Zalo: mỗi người nói 3 ý (hôm qua làm gì, hôm nay làm gì, đang kẹt gì).
- **Demo cuối tuần** Chủ nhật 20:00: chạy đúng mốc M1–M4, ghi lại video 1–2 phút để đưa vào báo cáo.
- **Kênh:** Zalo nhóm cho trao đổi nhanh; GitHub Issues cho bug và việc còn tồn; PR cho review code.
- **Kẹt quá 2 giờ** vì phụ thuộc người khác thì dùng stub / dữ liệu giả để đi tiếp và ghi Issue, không chờ.

## 7. Định nghĩa "xong" cho một task

1. Code trên nhánh riêng, `mvn package` xanh.
2. Tiêu chí hoàn thành trong bảng đã tự chạy thử và ghi cách thử vào PR.
3. Có người review chéo chạy lại và approve.
4. Đã merge vào `main`, Issue liên quan đóng.
5. Nếu đổi hợp đồng chung (`common`, schema) thì cả 3 module cùng cập nhật trong PR đó.

## 8. Rủi ro và cách xử lý

| Rủi ro | Dấu hiệu | Cách xử lý | Ai theo dõi |
|---|---|---|---|
| Đổi `Message` / DTO muộn làm vỡ cả 3 module | PR sửa `common` sau tuần 1 | Đóng băng cuối tuần 1; đổi phải qua nhóm và sửa cả 3 module cùng PR | Việt Anh |
| `ObjectOutputStream` cache đối tượng cũ, client nhận dữ liệu không đổi | Xe đối thủ đứng yên dù server đã cập nhật | Tạo DTO mới mỗi lần gửi hoặc gọi `reset()` sau mỗi `writeObject` | Phương |
| Cập nhật Swing ngoài luồng EDT gây treo / vẽ lỗi | Giao diện nháy, `ConcurrentModificationException` | Mọi callback từ `NetworkClient` đều qua `invokeLater` (C1) | Hiếu |
| Hai thread cùng ghi một stream làm hỏng dữ liệu | `StreamCorruptedException` | `send()` có `synchronized` (S1); server không gửi từ luồng tick khi chưa khóa | Phương |
| Tính hòa không ổn định | Hai người về đích gần nhau lúc thắng lúc hòa | Chốt luật: xét trong cùng tick 50 ms tại server; test T10 bằng script | Phương / Việt Anh |
| Demo trên 2 máy không kết nối được | Firewall Windows chặn cổng 5000 | Mở cổng trong Windows Defender Firewall, thử trước ngày demo; dự phòng chạy 2 client trên 1 máy | Việt Anh |
| Một người bận thi / việc khác | Chậm mốc > 2 ngày | Báo sớm, người review chéo nhận bớt task; ưu tiên MVP, bỏ phần làm thêm | Cả nhóm |
