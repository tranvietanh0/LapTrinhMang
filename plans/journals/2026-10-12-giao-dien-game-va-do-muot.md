# Giao diện game và độ mượt — 12/10/2026

Theo yêu cầu người dùng, làm lại đăng nhập, đăng ký và sảnh theo phong cách garage graphite/cam, đồng bộ các màn bảng xếp hạng, lịch sử và lời mời. Artwork vector cache theo kích thước. Giữ Swing, giao thức, luật chơi và MySQL.

Người dùng làm rõ xe chướng ngại vật chưa mượt. Chứng minh Swing Timer 16 ms chạy khoảng 31 ms trên Windows này; thay lịch vẽ fixed deadline, đo median 15,56 ms và mean 16,08 ms. Làm mượt clock xe cộ/camera, dự đoán theo elapsed time, không thay tần suất gói mục tiêu 20 Hz. Ghi socket trên worker FIFO, TCP_NODELAY, timeout handshake; ghi sự kiện DB trên worker riêng. Có regression tests trước/sau cho chặn EDT và camera bước 20 Hz; timer simulation dừng khi dispose.

Full package và package client cuối đạt; 90 tests trong báo cáo Surefire không thất bại. Chụp native Swing cho login/hover/error, register/error, sảnh hai tài khoản thật và footer race; kiểm tra kích thước/cắt chữ. Subagent simplify chỉ sở hữu hai helper UI, giữ hành vi; controller review inline và chạy build/render.

Chạy ba trận kiểm tra socket/GUI với carol và dave trên MySQL, backup trước từng trận. Giữ kết quả kiểm tra trong DB và backup tại dist/private-backups. Không xóa dữ liệu. Server mới PID 11696 trên TCP 5000; client mới PID 10484 và 19484 để người dùng thử. Không còn process probe; giữ MySQL84. Jar trước sửa trong dist/previous-jars-20261012-ui. Không commit.

Daemon tool bị khởi động lại giữa phiên; tiếp tục bằng kiểm tra log và tiến trình, không lặp thao tác đã hoàn tất. Chi phí render và thời gian nhận gói không cho thấy cải thiện đáng kể; cải thiện đo được là nhịp yêu cầu vẽ và xử lý jitter. Chưa xác nhận FPS màn hình hay mạng hai máy. Xem [báo cáo](../reports/review-261012-0946-game-ui-performance.md).
