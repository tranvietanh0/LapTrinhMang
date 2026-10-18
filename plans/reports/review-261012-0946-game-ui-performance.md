# Giao diện game và độ mượt

## Kết quả
Đăng nhập, đăng ký, sảnh, bảng xếp hạng, lịch sử và lời mời dùng theme garage graphite/cam. Garage vẽ vector, cache theo kích thước, không có animation nền. Form có focus, hover, disabled, busy và error states; font tiếng Việt đã kiểm tra bằng ảnh Swing thật. Sảnh dùng dữ liệu MySQL thật, giữ chức năng thách đấu và đăng xuất.

## Bằng chứng hiệu năng
| Phép đo trên máy Windows này | Trước | Sau |
|---|---:|---:|
| Nhịp timer/yêu cầu vẽ, median | 30,85 ms (Swing Timer 16 ms) | 15,56 ms (fixed deadline) |
| Nhịp timer/yêu cầu vẽ, p95 | 31,81 ms | 16,69 ms |
| Nhịp yêu cầu vẽ trung bình sau sửa | — | 16,08 ms |
| Thời gian render offscreen 1200×800, median | 3,108 ms | 3,302 ms |
| Thời gian render offscreen, p95 | 4,552 ms | 4,876 ms |
| Khoảng cách gói race update LAN localhost, p95 | 62,87 ms | 63,06 ms |

Nhịp vẽ tốt hơn; chi phí render và thời gian nhận gói không cho thấy cải thiện đáng kể. Đây không phải FPS đo trên màn hình và không phải benchmark mạng hai máy. Trận GUI có p95 nhận gói 64,39 ms, max 163,56 ms khi chạy workload GUI/chụp ảnh. Không tuyên bố loại bỏ mọi giật do hệ điều hành hoặc mạng.

Đồng hồ xe cộ không còn nhảy ngược theo thời điểm gói đến; camera làm mượt bước 20 Hz. Xe người chơi dự đoán theo thời gian thực với pulse ngắn, vẫn gửi trạng thái khoảng 20 Hz. Gửi socket trên luồng riêng, TCP_NODELAY, handshake có timeout. Ghi match_events FIFO ngoài luồng tick, dừng bình thường chờ tối đa 10 giây; schema và transaction kết quả giữ nguyên.

## Kiểm tra
- Full Maven package đạt; 90 test trong các báo cáo Surefire, 0 lỗi/thất bại. Không thay đổi hay che test.
- Regression test gửi socket khi peer ngừng đọc từng thất bại trước sửa, hiện đạt; kiểm tra thứ tự thông điệp điều khiển.
- Regression test camera bước 20 Hz từng thất bại trước sửa, hiện đạt; test jitter không lùi xe cộ, giới hạn dự đoán 3 tick, reset ván và tốc độ đồng hồ ổn định.
- Test database writer bị chặn: caller vẫn trả về, sự kiện lưu đúng START/COLLISION/FINISH.
- Trận socket MySQL mới: 300 cập nhật, 3 va chạm phía carol, kết quả QUIT và 7 sự kiện lưu thành công. Trận GUI: 303 cập nhật, 2 va chạm, QUIT và 6 sự kiện lưu thành công. Từng trận có backup SQL trước khi ghi dữ liệu.
- UI capture kiểm tra nút ≥44 px và không cắt chữ; ảnh login, hover/error, register/error, lobby với hai tài khoản thật và leaderboard loading. Screenshot desktop ở [thư mục ảnh](../261012-0946-game-ui-performance/screenshots/). Kiểm tra hình bằng native Swing, không chạy checker HTML vì đây là ứng dụng desktop.
- Kiểm tra nguồn và diff inline theo ak-code-review; subagent chỉ simplify GameTheme/GaragePanel, giữ public API và hình ảnh. Đã sửa clipping sidebar, caption nút và timer chưa dừng khi dispose RaceFrame.

## Visual QA
Rubric desktop: 22/24. Fidelity/hierarchy/spacing/alignment/type/color/depth/states/copy/craft: 2 mỗi mục; responsiveness: 1 vì cửa sổ desktop có kích thước tối thiểu, không dành cho màn hình nhỏ; slop tells: 1 vì hình xe vector cố ý tối giản. Form và sidebar không bị cắt ở kích thước mặc định; hover và lỗi được chụp. Bảng xếp hạng/lịch sử populated và lời mời chưa được chụp riêng trong bộ ảnh này.

## Giới hạn và vận hành
Không thay luật chơi hoặc giao thức. Điểm/trận kiểm tra của carol và dave được giữ trong DB; backup private ở dist/private-backups. Jar cũ tại dist/previous-jars-20261012-ui. Người chơi máy khác cần chép lại dist/racing-client.jar. Chưa test hai máy thật hoặc trận hoàn chỉnh qua đích/rematch bằng GUI. Không commit.
