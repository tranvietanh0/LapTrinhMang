# Chạy trên Windows với MySQL

Setup hiện tại ngày 12/10/2026 dùng JDK Temurin 17 và MySQL Server 8.4.11 cài bằng bộ cài Windows. Server game kết nối MySQL thật, không dùng `--memory`.

## Chạy lại

MySQL chạy dưới Windows Service `MySQL84`. Kiểm tra bằng `Get-Service MySQL84`; nếu đã dừng, mở PowerShell quyền quản trị và chạy `Start-Service MySQL84`.

Từ thư mục gốc dự án, chạy server trong một terminal:

```powershell
& "C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot\bin\java.exe" -jar dist/racing-server.jar
```

Server dùng TCP 5000. Không mở thêm server nếu cổng đã có tiến trình của dự án. Kiểm tra bằng `netstat -ano | Select-String ':5000'` và đối chiếu PID với dòng lệnh tiến trình.

Mở terminal khác để chạy client; mở hai client nếu demo trên cùng máy:

```powershell
& "C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot\bin\java.exe" -jar dist/racing-client.jar 127.0.0.1 5000
```

Đăng nhập `alice` và `bob`, mật khẩu mẫu đều là `123456`. Với hai máy, máy thứ hai chỉ cần Java 17 và `racing-client.jar`; nhập IP LAN của máy server và cổng 5000 vào màn hình đăng nhập. Cho phép TCP 5000 từ LAN trên firewall máy server. Máy thứ hai không cần MySQL.

Thoát client và bấm Ctrl+C tại terminal server để tắt. Không cần dừng MySQL khi thoát game. Server tự động mở trong phiên setup có PID lưu tại `dist/server-mysql.pid`; chỉ dừng PID đó sau khi kiểm tra nó vẫn là server của dự án.

Launcher `dist/run-game.cmd` và `dist/stop-game.cmd` cũ trỏ tới script không còn trên máy. Dùng các lệnh trên thay cho launcher cũ.

## Cấu hình, dữ liệu và log

- MySQL: `C:\Program Files\MySQL\MySQL Server 8.4`; cấu hình service tại `C:\ProgramData\MySQL\MySQL Server 8.4\my.ini`.
- Database game: `racing`, kết nối tại `127.0.0.1:3306`.
- JDBC: `db.properties` trong thư mục gốc dự án, được Git bỏ qua. Setup tạo tài khoản DB riêng với quyền SELECT/INSERT/UPDATE/DELETE trên `racing.*`; giữ mật khẩu riêng trên máy.
- Log server: `dist/server-mysql.log`, `dist/server-mysql-error.log`.
- Backup toàn bộ MySQL trước setup và bản sao cấu hình JDBC cũ: `dist/private-backups/`. Các file chứa dữ liệu tài khoản hoặc mật khẩu, không chia sẻ hay commit.
- Script cục bộ: `dist/setup-mysql-local.ps1`. Chỉ chạy khi cần thiết lập lại; nhập mật khẩu root trực tiếp trong cửa sổ, sao lưu trước khi nạp schema và chỉ thêm tài khoản mẫu chưa có, không ghi đè tài khoản game đã tồn tại.

Các file trong `dist/` được Git bỏ qua. `scripts/build-dist.cmd` tạo lại bản phân phối; giữ riêng backup trước khi đóng gói.

## Xem dữ liệu trong Workbench

Kết nối `127.0.0.1`, cổng `3306`, bằng tài khoản root bạn đặt lúc cài. Chạy:

```sql
SELECT player_id, username, points, wins, losses, draws FROM racing.players;
SELECT * FROM racing.matches ORDER BY match_id DESC LIMIT 100;
SELECT * FROM racing.match_events ORDER BY event_id DESC LIMIT 100;
```

Sau khi chơi trận mới, chạy lại truy vấn để cập nhật. Các trận chơi trước khi chuyển từ `--memory` không có trong MySQL.

## Kiểm tra đã thực hiện

Client đã có giao diện garage nền tối cho đăng nhập, đăng ký, sảnh, bảng xếp hạng và lịch sử. Màn đua dùng lịch vẽ theo thời hạn cố định để tránh timer Swing 16 ms bị làm tròn thành khoảng 31 ms trên Windows; điều khiển dự đoán theo thời gian thực, gói trạng thái vẫn gửi khoảng 20 lần/giây. Thông điệp gửi chạy trên luồng riêng; xe cộ và camera làm mượt giữa các gói cập nhật. Nếu mở bản cũ, đóng client rồi dùng jar mới trong `dist/`; máy khác cũng cần chép lại `racing-client.jar`.

Đã sao lưu MySQL trước setup và từng trận kiểm tra; xác nhận JDBC, đăng nhập, thách đấu, đếm ngược, chạy `RaceFrame` với điều khiển tăng tốc, va chạm, thoát trận và lưu kết quả/diễn biến bằng MySQL thật. Các trận kiểm tra giữa `carol` và `dave` được giữ trong DB; backup trước từng trận nằm tại `dist/private-backups/`. Chưa kiểm thử kết nối giữa hai máy; xem [kế hoạch kiểm thử](TEST-PLAN.md).
