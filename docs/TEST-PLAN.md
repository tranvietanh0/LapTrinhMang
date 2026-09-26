# Kế hoạch kiểm thử tích hợp (D6)

Chạy ở tuần 3 và 4, sau mốc M3. Mỗi kịch bản ghi kết quả PASS / FAIL, ngày chạy, người chạy và link Issue nếu FAIL. Cột "Kiểm tra DB" do Mai Anh xác nhận bằng câu SQL bên dưới sau mỗi kịch bản.

## Chuẩn bị

1. Hai máy trong cùng mạng LAN (hoặc một máy chạy 2 client). Mở cổng 5000 trong Windows Defender Firewall trên máy chạy server.
2. MySQL đã nạp `db/schema.sql` và `db/seed.sql`. Trước mỗi buổi test chạy `db/reset-db.sql` để điểm về 0.
3. Build mới nhất: `mvnw.cmd package`, lấy `racing-server.jar`, `racing-client.jar`, `racing-tools.jar`.
4. Tài khoản: `alice`, `bob`, `carol`, `dave`, `erin`, `frank`, mật khẩu `123456`.

Câu SQL dùng để kiểm tra sau mỗi trận:

```sql
SELECT username, points, wins, losses, draws FROM players ORDER BY points DESC, wins DESC;
SELECT match_id, room_code, player1_id, player2_id, winner_id, status, end_reason, started_at, ended_at
FROM matches ORDER BY match_id DESC LIMIT 5;
SELECT event_type, player_id, event_time, payload FROM match_events WHERE match_id = <id> ORDER BY event_time;
```

Công cụ dòng lệnh `racing-tools.jar` (xem `tools/`) dùng cho T10, T15, T17 và để test server khi chưa có giao diện:

```
java -jar tools/target/racing-tools.jar --help
java -jar tools/target/racing-tools.jar "login alice 123456; wait LOGIN_RESULT; leaderboard; wait LEADERBOARD; exit"
```

## Kịch bản

| # | Kịch bản | Các bước | Kết quả mong đợi | Kiểm tra DB | Người test | Kết quả | Ghi chú |
|---|---|---|---|---|---|---|---|
| T1 | Đăng nhập sai mật khẩu | Mở client, nhập `alice` / `sai` | Hộp thoại báo sai tài khoản hoặc mật khẩu, vẫn ở màn đăng nhập | không | Việt Anh | | |
| T2 | Đăng nhập cùng tài khoản ở 2 máy | Máy 1 đăng nhập `alice`; máy 2 đăng nhập `alice` | Máy 2 bị từ chối với thông báo "Tài khoản đang đăng nhập ở nơi khác", máy 1 không bị ảnh hưởng | không | Việt Anh | | Server đã cài theo cách này (SessionManagerTest) |
| T3 | 3 client online, 1 client thoát | `alice`, `bob`, `carol` đăng nhập; `carol` bấm Đăng xuất | Trong 1 s, hai client còn lại thấy `carol` biến mất khỏi danh sách | không | Hiếu | | |
| T4 | Mời người đang thi đấu | `alice` và `bob` đang đua; `carol` bấm Thách đấu `alice` | `carol` nhận thông báo BUSY; `alice` không thấy hộp thoại nào | không | Hiếu | | |
| T5 | Không trả lời lời mời 30 s | `alice` mời `bob`; `bob` không bấm gì | Sau 30 s hộp thoại ở `bob` tự đóng; `alice` nhận TIMEOUT; cả hai vẫn Rảnh trong danh sách | không | Hiếu | | |
| T6 | Từ chối lời mời | `alice` mời `bob`; `bob` bấm Từ chối | `alice` nhận REJECTED; cả hai vẫn Rảnh | không | Hiếu | | |
| T7 | Chấp nhận, đếm ngược, đua | `bob` bấm Chấp nhận | Cả hai chuyển sang màn hình đua, thấy 3-2-1-GO; bấm phím trước GO không có tác dụng; sau GO xe chạy | `matches` có dòng mới status PLAYING | Việt Anh | | |
| T8 | Va chạm xe cộ | Đuổi kịp và lái xe đâm vào đuôi một xe cộ đang chạy cùng làn | Tốc độ về 0 trong 1 s, xe cộ bị đâm nổ và biến mất, hiệu ứng hiện trên cả hai màn hình cùng lúc | `match_events` có COLLISION | Việt Anh | | |
| T9 | Về đích trước | `alice` về đích trước `bob` | Cả hai thấy kết quả; `alice` thắng | `alice` +1 điểm +1 thắng, `bob` +1 thua; `end_reason = FINISH`, `winner_id` = alice | Việt Anh | | |
| T10 | Về đích cùng tick | Chạy `t10-draw-b.txt` rồi `t10-draw-a.txt` bằng racing-tools (cùng tốc độ 200, cùng lúc GO) | Cả hai nhận MATCH_RESULT outcome DRAW | Mỗi người +1 điểm +1 hòa; `winner_id NULL`; `end_reason = DRAW` | Việt Anh | | Hai tiến trình gửi CAR_STATE độc lập nên có thể lệch 1 tick (50 ms) và ra thắng/thua: chạy lại vài lần. Luật "cùng tick = hòa" được kiểm chứng chắc chắn bằng test `RoomTest` trên server |
| T11 | Bấm Thoát trận giữa chừng | Đang đua, `alice` bấm Thoát trận và xác nhận | `bob` thấy thông báo đối thủ đã thoát và mình thắng; `alice` về sảnh | `alice` +1 thua, `bob` +1 điểm; `end_reason = QUIT` | Việt Anh | | |
| T12 | Rút mạng client đang đua | Đang đua, tắt Wi-Fi hoặc kill tiến trình client của `alice` | Trong ≤ 15 s `bob` nhận thắng và về sảnh; danh sách online không còn `alice` | `end_reason = DISCONNECT`, `winner_id` = bob | Việt Anh | | Ghi thời gian từ lúc rút mạng đến lúc `bob` nhận kết quả |
| T13 | Cả hai đồng ý thi đấu tiếp | Sau MATCH_RESULT cả hai bấm Đồng ý | Ván mới trong cùng phòng: xe cộ khác, quãng đường về 0, đếm ngược lại | `matches` có dòng mới với cùng `room_code` | Việt Anh | | |
| T14 | Một bên từ chối thi đấu tiếp | `alice` Đồng ý, `bob` Từ chối | Cả hai về sảnh, trạng thái Rảnh | không | Hiếu | | |
| T15 | 2 phòng đua cùng lúc | 4 client (`alice`+`bob`, `carol`+`dave`) đua song song; có thể dùng racing-tools cho 2 client | Không lẫn RACE_UPDATE giữa phòng; hai kết quả độc lập | 2 dòng `matches` với `room_code` khác nhau | Việt Anh | | |
| T16 | Tắt server khi client ở sảnh | Tắt server | Client báo mất kết nối, về màn đăng nhập, không treo | không | Hiếu | | |
| T17 | Gửi CAR_STATE tốc độ 999 | Chạy `t17-speed-cheat.txt` với đối thủ thật | RACE_UPDATE trả về `speed ≤ 200`; client gian lận không về đích nhanh hơn xe 200 km/h | `matches` không có kết quả bất thường | Việt Anh | | Đã chạy 24/09 với server `--memory`: server tự tính quãng đường từ tốc độ đã cắt về 200, client gian lận ngừng gửi sau khi "tự về đích" nên xe đứng lại ở ~211 m và thua |
| T18 | Đăng ký tài khoản | Bấm Đăng ký, tạo `test1`; đăng nhập `test1`; đăng ký lại `test1` | Vào sảnh được; lần hai báo trùng tên | `players` có dòng `test1`, `password_hash` bắt đầu bằng `sha256$` | Mai Anh | | |
| T19 | Lịch sử trận | Sau T9, T10, T11 mở Lịch sử của `alice` | 3 dòng, đúng đối thủ, kết quả và lý do | so với `SELECT ... FROM matches WHERE player1_id = ... OR player2_id = ...` | Mai Anh | | |

## Mẫu ghi Issue khi FAIL

```
Tiêu đề: [T12] Đối thủ không nhận kết quả sau khi rút mạng
Bước: ... (số kịch bản, thao tác)
Mong đợi: ...
Thực tế: ... (kèm ảnh chụp, log server, kết quả SQL)
Build: commit <sha>
Gán cho: <người sửa theo PLAN.md>
```
