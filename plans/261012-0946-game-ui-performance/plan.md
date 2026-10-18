# Giao diện game và độ mượt

## Outcome
Làm lại đăng nhập, đăng ký và sảnh chờ thành giao diện game đua xe chuyên nghiệp; chẩn đoán và sửa các nguyên nhân lag có bằng chứng. Giữ Java Swing, giao thức hiện tại, luật chơi và dữ liệu MySQL. Không thêm gameplay, không đổi schema.

## Design brief
Register: Product.
Scene: Người chơi trên laptop Windows, vào game và thách đấu trực tiếp qua LAN.
Direction: Garage đua xe, chữ lớn gọn, mặt tối và vạch cam lấy từ đường đua.
Color: Committed; nền graphite #101318, chữ #F3F4F6, accent #FF7842.
Type: Bahnschrift display + Segoe UI body; dự phòng SansSerif.
Signature: Hình xe và đường đua vẽ vector trong panel giới thiệu, không tạo tải animation.
Dials: variance 6, motion 2, density 4.

## Phases
- [x] Đo baseline: render, gửi socket trên EDT, và thời gian mở kết nối JDBC.
- [x] Theme chung; đăng nhập/đăng ký, sảnh, các dialog phụ liên quan.
- [x] Sửa nguyên nhân lag đã chứng minh, thêm regression tests cho bất biến liên quan.
- [x] Test, build, kiểm tra ảnh render Swing và luồng giao diện với server thật.
- [x] Cập nhật docs có ảnh hưởng; mở bản client mới dùng MySQL.

## Delivery
Full Maven package và client package cuối đạt. Server PID 11696 dùng MySQL; client mới PID 10484 và 19484 đã mở. Jar cũ và backup SQL được giữ riêng trong dist. Bằng chứng, phép đo và giới hạn tại [báo cáo](../reports/review-261012-0946-game-ui-performance.md).

## Acceptance
Form có focus, busy/error/disabled states và thao tác bàn phím; sảnh hiển thị dữ liệu thật và đủ thách đấu, xếp hạng, lịch sử, đăng xuất. Không chặn EDT khi ghi socket; không đổi thứ tự thông điệp điều khiển. So sánh baseline/sau sửa, không suy đoán FPS. Build/test phù hợp đạt, không mất dữ liệu.

## Risks / rollback
Không thay DB. Giữ jar cũ riêng trước khi thay bản chạy. Chỉ dừng tiến trình dự án đã xác định chủ sở hữu; giữ MySQL84. Native Swing QA bằng ảnh offscreen và test UI; không dùng checker HTML cho ứng dụng desktop.
