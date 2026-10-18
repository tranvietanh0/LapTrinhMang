# Chuyển server game sang MySQL — 12/10/2026

Theo yêu cầu người dùng, đã chuyển server từ --memory sang MySQL84 8.4.11 do người dùng cài bằng MSI. Cấu hình DB cũ bị từ chối đăng nhập; tạo tài khoản ứng dụng riêng với quyền đọc/ghi trên racing, giữ mật khẩu trong db.properties được Git bỏ qua. Người dùng nhập mật khẩu root trực tiếp trong cửa sổ thiết lập, không gửi qua chat.

Backup toàn bộ MySQL và cấu hình cũ lưu trong dist/private-backups trước khi nạp schema. Chỉ thêm tài khoản mẫu chưa tồn tại, giữ dữ liệu cũ. JDBC xác nhận 6 tài khoản; smoke test đăng nhập alice và bảng xếp hạng thành công. Server PID 13148 chạy TCP 5000 không có --memory, log và PID lưu trong dist. Mở lại hai client PID 4892 và 22464. Đóng cửa sổ thiết lập sau khi thành công; giữ server/client phục vụ yêu cầu chạy game. Chưa xác nhận một trận đấu hoặc kết nối hai máy.

Cập nhật docs/WINDOWS-LOCAL-SETUP.md theo service và cấu hình hiện tại. Launcher cũ còn trỏ tới runtime không tồn tại; tài liệu hướng dẫn lệnh Java trực tiếp. Không commit.