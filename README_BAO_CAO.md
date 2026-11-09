# Tài liệu báo cáo: Game đua xe đối kháng online

## 1. Tóm tắt một câu

Đây là game đua xe 1-vs-1 viết bằng Java: hai chương trình client kết nối tới một
server trung tâm bằng **TCP Socket**, server xác thực người chơi, ghép phòng, làm
trọng tài cho trận đua và lưu tài khoản/kết quả vào **MySQL**.

Các ý cần nhớ khi thầy hỏi:

- **Giao thức mạng:** TCP.
- **Cổng mặc định:** `5000`.
- **Mô hình:** Client/Server, không phải P2P.
- **Định dạng thông điệp ứng dụng:** một đối tượng `Message` Java được truyền qua
  `ObjectOutputStream`/`ObjectInputStream`; đây là giao thức của ứng dụng nằm trên
  TCP, không phải một giao thức Internet mới.
- **Server là nguồn sự thật:** client gửi ý định điều khiển (làn, tốc độ), server
  tự tính quãng đường, va chạm và người thắng.
- **Cơ sở dữ liệu:** MySQL qua JDBC; có chế độ `--memory` để demo không cần MySQL.

## 2. Liên hệ với giáo trình Lập trình mạng

Giáo trình PDF được đối chiếu gồm các phần chính: mô hình OSI/TCP-IP, địa chỉ
IP/cổng/socket, mô hình client/server; lập trình TCP/IP với `Socket` và
`ServerSocket`; luồng vào-ra và lập trình đa luồng; lập trình UDP;
socket nâng cao/NIO; và WebSocket.

| Kiến thức trong giáo trình | Dự án áp dụng ở đâu | Cách giải thích khi báo cáo |
|---|---|---|
| Client/server | `server/GameServer.java`, `client/ClientMain.java` | Server lắng nghe, nhiều client chủ động kết nối và gửi yêu cầu. |
| TCP/IP | `GameServer` dùng `ServerSocket`; `NetworkClient` dùng `Socket` | TCP cung cấp kết nối tin cậy, đúng thứ tự, phù hợp login, lời mời và kết quả trận. |
| IP và port | `GameConfig.PORT = 5000`, `NetworkClient.connect(host, port)` | Socket endpoint được xác định bởi địa chỉ máy + cổng; máy khác chỉ cần IP LAN của server và cổng 5000. |
| Stream vào/ra | `ClientHandler`, `NetworkClient` | Mỗi phía có stream đọc/ghi trên TCP; `readObject()` nhận một `Message`. |
| Đa luồng | `GameServer`, `ClientHandler`, `RoomManager`, `NetworkClient` | Mỗi client có luồng đọc và luồng ghi; scheduler chạy tick phòng; client đọc mạng không khóa giao diện Swing. |
| Đồng bộ/trạng thái | `Room` dùng `synchronized`; `ConcurrentHashMap`, `BlockingQueue` | Phòng là máy trạng thái; các thao tác trận được tuần tự hóa theo từng phòng. |
| UDP | Không dùng | UDP không bảo đảm giao hàng/thứ tự; không phù hợp các lệnh login/kết quả. Trận nhỏ 1-vs-1 này ưu tiên TCP và server authoritative. |
| NIO/non-blocking | Không dùng | Dự án dùng blocking I/O dễ hiểu, kết hợp nhiều thread; chưa dùng `Selector`/`Channel`. |
| WebSocket | Không dùng | Đây là ứng dụng Java desktop, không chạy trong trình duyệt; dùng TCP Socket trực tiếp. |

## 3. Kiến trúc và vị trí code

```text
Client A (Swing) ── TCP :5000 ──┐
                                ├── GameServer
Client B (Swing) ── TCP :5000 ──┘      ├── Session/Invite
                                       ├── Room/CarSim/MatchService
                                       └── JDBC ── MySQL
```

### `common`: hợp đồng giữa client và server

- [`Message.java`](common/src/main/java/racing/common/net/Message.java): gói tin
  duy nhất đi qua socket, gồm `MessageType`, `payload`, timestamp.
- [`MessageType.java`](common/src/main/java/racing/common/net/MessageType.java):
  danh sách lệnh và sự kiện hai chiều.
- [`dto/`](common/src/main/java/racing/common/dto/): các payload có thể
  serialize được, ví dụ `LoginRequest`, `CarState`, `RaceState`, `MatchResult`.
- [`GameConfig.java`](common/src/main/java/racing/common/GameConfig.java): cổng,
  chu kỳ tick 50 ms, tốc độ tối đa 360 km/h, timeout và giới hạn trận.

### `server`: trung tâm xử lý

- [`GameServer.java`](server/src/main/java/racing/server/GameServer.java):
  mở `ServerSocket`, nhận kết nối, tạo `ClientHandler`, chạy scheduler.
- [`ClientHandler.java`](server/src/main/java/racing/server/net/ClientHandler.java):
  một kết nối TCP; đọc `Message`, phân loại theo `MessageType`, gọi service;
  có hàng đợi ghi riêng để luồng game không bị chặn bởi client chậm.
- [`SessionManager.java`](server/src/main/java/racing/server/core/SessionManager.java):
  đăng nhập, chống đăng nhập trùng, danh sách online và trạng thái người chơi.
- [`InviteManager.java`](server/src/main/java/racing/server/core/InviteManager.java):
  gửi lời mời, nhận/từ chối, timeout 30 giây.
- [`RoomManager.java`](server/src/main/java/racing/server/core/RoomManager.java):
  tạo phòng 2 người, countdown, tick tất cả phòng và đóng phòng.
- [`Room.java`](server/src/main/java/racing/server/core/Room.java):
  máy trạng thái `WAITING -> COUNTDOWN -> RACING -> AWAIT_REMATCH -> CLOSED`.
- [`CarSim.java`](server/src/main/java/racing/server/core/CarSim.java):
  mô phỏng xe phía server, giới hạn tốc độ, tính quãng đường và va chạm.
- [`MatchService.java`](server/src/main/java/racing/server/core/MatchService.java):
  tạo/chốt trận và đưa event vào hàng đợi ghi DB.

### `client`: giao diện và điều khiển

- [`NetworkClient.java`](client/src/main/java/racing/client/net/NetworkClient.java):
  kết nối TCP, luồng đọc, luồng ghi FIFO, heartbeat PING mỗi 5 giây và listener.
- [`LoginFrame.java`](client/src/main/java/racing/client/ui/LoginFrame.java),
  [`LobbyFrame.java`](client/src/main/java/racing/client/ui/LobbyFrame.java):
  đăng nhập/đăng ký, danh sách online và thách đấu.
- [`RaceLauncher.java`](client/src/main/java/racing/client/ui/RaceLauncher.java):
  nối các message trận vào màn hình đua.
- [`RaceFrame.java`](client/src/main/java/racing/client/ui/race/RaceFrame.java):
  nhận countdown/update/result, đọc phím, gửi `CAR_STATE` khoảng 20 lần/giây.
  Swing cập nhật trên Event Dispatch Thread.

### `db` và các module phụ

- [`schema.sql`](db/schema.sql): `players`, `matches`, `match_events`.
- [`MatchDAO.java`](server/src/main/java/racing/server/db/MatchDAO.java):
  JDBC `PreparedStatement`, transaction chốt kết quả và cộng điểm.
- [`DbConnection.java`](server/src/main/java/racing/server/db/DbConnection.java):
  lấy cấu hình từ biến môi trường hoặc `db.properties`.
- [`tools/`](tools/src/main/java/racing/tools/): client dòng lệnh để test giao
  thức và các kịch bản hòa/gian lận.

## 4. Giao thức ứng dụng trên TCP

TCP chỉ cung cấp byte stream tin cậy. Dự án định nghĩa lớp giao thức riêng bằng
`MessageType` và kiểu payload:

| Hướng | Message | Payload | Ý nghĩa |
|---|---|---|---|
| C -> S | `LOGIN` | `LoginRequest` | Đăng nhập |
| S -> C | `LOGIN_RESULT` | `LoginResult` | Kết quả đăng nhập |
| S -> C | `ONLINE_LIST` | danh sách `PlayerInfo` | Cập nhật người chơi online |
| C -> S | `INVITE` | username | Mời đối thủ |
| S -> C | `INVITE_RECEIVED` | `InviteInfo` | Hiển thị lời mời |
| C -> S | `INVITE_REPLY` | `InviteReply` | Chấp nhận/từ chối |
| S -> C | `MATCH_START` | `MatchStart` | Mở trận và gửi chướng ngại vật |
| S -> C | `COUNTDOWN` | số `3,2,1,0` | Đếm ngược, `0` là GO |
| C -> S | `CAR_STATE` | `CarState` | Làn và tốc độ hiện tại |
| S -> C | `RACE_UPDATE` | `RaceState` | Trạng thái hai xe do server tính |
| C -> S | `FINISH` | thời gian client | Gợi ý đã về đích; server không tin tuyệt đối |
| S -> C | `MATCH_RESULT` | `MatchResult` | Thắng/thua/hòa |
| C -> S | `QUIT_MATCH` | room id | Chủ động thoát |
| C <-> S | `PING`/`PONG` | rỗng | Heartbeat phát hiện mất kết nối |
| C -> S | `LEADERBOARD_REQ`, `MATCH_HISTORY_REQ` | rỗng | Yêu cầu dữ liệu |

Chi tiết đầy đủ của hợp đồng message nằm trong
[`MessageType.java`](common/src/main/java/racing/common/net/MessageType.java).
`ObjectOutputStream.reset()` sau mỗi lần ghi giúp stream không giữ lại object cũ
trong cache tham chiếu khi DTO được cập nhật nhiều lần.

## 5. Luồng hoạt động của một trận

1. Client tạo TCP `Socket` tới `host:5000`; server `accept()` và tạo
   `ClientHandler`.
2. Client gửi `LOGIN(LoginRequest)`. Server kiểm tra tài khoản, tạo `Session`,
   trả `LOGIN_RESULT` và `ONLINE_LIST`.
3. Người chơi A gửi `INVITE`; server kiểm tra B online/rảnh. B trả
   `INVITE_REPLY`.
4. `RoomManager.createRoom()` gán hai session vào phòng và `Room.startRound()`
   tạo bản ghi `matches`, sinh chướng ngại vật, gửi `MATCH_START`.
5. Scheduler gửi `COUNTDOWN` 3-2-1-0. Sau GO, server gọi `tickAll()` mỗi 50 ms.
6. Client điều khiển xe và gửi `CAR_STATE`. Server cắt tốc độ về tối đa, tự tích
   phân quãng đường, xét va chạm, rồi gửi `RACE_UPDATE` cho cả hai.
7. Xe chạm đích trước được server xác định. Cùng `finishTick` là hòa; client
   không thể tự đặt quãng đường hoặc tự quyết định người thắng.
8. Server gửi `MATCH_RESULT`, chốt DB trong transaction, rồi hỏi rematch.
   Hai bên đồng ý thì tạo ván mới cùng phòng; một bên từ chối/timeout thì đóng
   phòng và trả về sảnh.
9. Thoát trận hoặc mất heartbeat được xử lý thành `QUIT`/`DISCONNECT`, đối thủ
   được thắng theo luật.

## 6. Tính đồng thời và độ tin cậy

- `GameServer`: pool xử lý client, scheduler cho tick/countdown/timeout.
- `ClientHandler`: đọc socket ở một luồng, ghi socket ở luồng khác qua
  `BlockingQueue`; tránh việc tick bị kẹt vì client không đọc.
- `NetworkClient`: đọc mạng ở `net-reader`, ghi FIFO ở `net-writer`; callback UI
  được chuyển sang Swing EDT.
- TCP có `SO_TIMEOUT` 15 giây ở server; client gửi PING mỗi 5 giây.
- Hàng đợi gửi đầy hoặc đọc/ghi lỗi thì đóng kết nối và giải phóng session/phòng.
- `Room` được đồng bộ hóa; nhiều phòng có thể chạy song song nhưng message được
  kiểm tra đúng `roomId`.
- Ghi `match_events` chạy qua một event writer riêng; tạo trận và chốt điểm vẫn
  là thao tác chính xác cần xử lý lỗi.

## 7. MySQL và tính đúng dữ liệu

`players` lưu tài khoản, điểm và thống kê; `matches` lưu người chơi, người thắng,
trạng thái và lý do kết thúc; `match_events` lưu START/COLLISION/FINISH/QUIT/
DISCONNECT/REMATCH.

Trong [`MatchDAO.saveResult`](server/src/main/java/racing/server/db/MatchDAO.java),
server:

1. bắt đầu transaction;
2. cập nhật `matches`;
3. cập nhật điểm/thắng/thua/hòa của hai người;
4. `commit`, hoặc `rollback` nếu có lỗi.

Mật khẩu không lưu dạng rõ mà dùng dạng `sha256$<salt>$<hash>`. Truy vấn lịch sử
và bảng xếp hạng chỉ được thực hiện sau khi người chơi đã đăng nhập.

## 8. Các câu hỏi thầy có thể hỏi

**Vì sao dùng TCP thay UDP?**  
Game này cần login, lời mời, kết quả và trạng thái đúng thứ tự; TCP có kết nối,
đảm bảo giao hàng/thứ tự và đơn giản hơn. Nếu tối ưu game thời gian thực ở quy mô
lớn, UDP có thể giảm độ trễ nhưng phải tự xử lý mất/gửi lại/sắp xếp gói; dự án
hiện tại không cài cơ chế đó.

**Code giao thức nằm ở đâu?**  
Tầng truyền TCP ở `GameServer`, `ClientHandler`, `NetworkClient`; giao thức ứng
dụng ở `common/net/Message.java` và `MessageType.java`; payload ở `common/dto`.

**Client gửi quãng đường hay server tính?**  
Client chỉ gửi làn/tốc độ trong `CarState`. `CarSim.advance()` ở server tự tính
quãng đường theo tick và kiểm tra va chạm/về đích, nên gửi tốc độ 999 vẫn bị cắt
về 360.

**Nếu client bị treo hoặc rút mạng?**  
Heartbeat và timeout phát hiện tối đa khoảng 15 giây; `ClientHandler.cleanup()`
gọi xử lý disconnect, phòng chốt đối thủ thắng và cập nhật danh sách online.

**Có phải mỗi client kết nối một server riêng không?**  
Không. Một `GameServer` có một `ServerSocket` cổng 5000 và chấp nhận nhiều
connection; mỗi connection có handler/session riêng.

**Có dùng HTTP/WebSocket không?**  
Không. Đây là desktop Java Swing, kết nối TCP Socket trực tiếp. MySQL là kết nối
JDBC riêng giữa server và DB, không phải client kết nối thẳng vào DB.

## 9. Cách demo nhanh trước khi báo cáo

```powershell
docker compose up -d
mvnw.cmd package
java -jar server\target\racing-server.jar
java -jar client\target\racing-client.jar 127.0.0.1 5000
```

Mở hai client, đăng nhập `alice` và `bob` (mật khẩu `123456`), thách đấu, chấp
nhận và đua. Nếu chưa có MySQL, dùng:

```powershell
java -jar server\target\racing-server.jar --memory
```

Để chứng minh giao thức không phụ thuộc giao diện, chạy
`tools\target\racing-tools.jar` với các script trong
[`tools/scripts/`](tools/scripts/). Kế hoạch 19 kịch bản kiểm thử nằm ở
[`docs/TEST-PLAN.md`](docs/TEST-PLAN.md).

## 10. Phạm vi và hạn chế nên nói rõ

- Đây là TCP blocking I/O + thread pool, chưa phải NIO/non-blocking.
- Serialization Java giúp làm bài rõ ràng nhưng client/server phải dùng DTO
  tương thích; hệ thống thực tế có thể dùng JSON/protobuf với framing/versioning.
- Chưa có mã hóa TLS; chỉ nên demo trong mạng tin cậy, không coi đây là hệ thống
  production.
- Server authoritative chống gian lận ở mức mô phỏng tốc độ/quãng đường, không
  phải hệ thống chống gian lận hoàn chỉnh.
- Chế độ `--memory` chỉ để demo; tắt server sẽ mất lịch sử và điểm.
