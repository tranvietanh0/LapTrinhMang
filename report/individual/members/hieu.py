"""Báo cáo cá nhân: Vũ Văn Hiếu – Client sảnh (C1–C7)."""
from source import method

CL = "client/src/main/java/racing/client/"

CONTENT = {
    "name": "Vũ Văn Hiếu",
    "slug": "VuVanHieu",
    "role": "Client sảnh – mạng phía client, đăng nhập, sảnh chờ, lời mời (C1–C7)",
    "intro": (
        "Trong nhóm, Vũ Văn Hiếu phụ trách phía client trước khi vào trận: lớp mạng NetworkClient mà mọi màn hình "
        "khác đều dùng để nói chuyện với server, màn hình đăng nhập, sảnh chờ hiển thị người chơi online, hộp thoại "
        "lời mời đếm ngược 30 giây, điều hướng giữa sảnh và màn hình đua, và xử lý khi mất kết nối. Đây là phần "
        "người chơi nhìn thấy đầu tiên và là cầu nối giữa giao diện Swing với socket TCP."),
    "tasks": [
        ["C1", "NetworkClient: kết nối, luồng nhận readObject, luồng gửi FIFO, đăng ký listener theo MessageType, "
               "đẩy callback về luồng Swing, PING mỗi 5 s", "net/NetworkClient",
         "Các màn hình gọi net.send() và net.on(...) mà không cần biết socket"],
        ["C2", "LoginFrame + ClientState: nhập tài khoản và địa chỉ server, gửi LOGIN, báo lỗi, chuyển sang sảnh",
         "ui/LoginFrame, model/ClientState, ClientApp", "Sai mật khẩu báo lỗi, đúng thì mở sảnh"],
        ["C3", "LobbyFrame: bảng người chơi online từ ONLINE_LIST, nút Thách đấu chỉ với người Rảnh, trạng thái "
               "chờ trả lời, xử lý INVITE_RESULT", "ui/LobbyFrame, ui/OnlineTableModel",
         "Danh sách tự cập nhật; mời xong nút khóa đến khi có kết quả"],
        ["C4", "InviteDialog: đếm 30 → 0, thanh tiến trình, Chấp nhận / Từ chối gửi INVITE_REPLY, tự đóng khi hết "
               "giờ", "ui/InviteDialog", "Không bấm gì 30 s thì hộp thoại tự đóng"],
        ["C5", "Điều hướng: MATCH_START mở màn hình đua, về sảnh khi phòng đóng; nút Bảng xếp hạng, Lịch sử, "
               "Đăng ký", "ui/LobbyFrame, ui/RaceLauncher, ui/LoginFrame",
         "Vào trận và quay về sảnh nhiều lần liên tiếp không lỗi"],
        ["C6", "Mất kết nối phía client: báo lỗi và quay về đăng nhập; đăng xuất gửi LOGOUT",
         "net/NetworkClient, ClientApp", "Tắt server khi đang ở sảnh: client báo lỗi rõ, không treo"],
        ["C7", "Hoàn thiện UX: phím Enter, cỡ cửa sổ theo màn hình, nút luôn nằm trong vùng nhìn thấy",
         "ui/*, ui/theme/ScreenFrame", "Không còn lỗi chặn demo"],
    ],
    "files": [
        (CL + "net/NetworkClient.java", "Kết nối TCP, luồng đọc / ghi, heartbeat, listener"),
        (CL + "ClientApp.java", "Giữ kết nối và trạng thái, chuyển màn hình, xử lý mất kết nối"),
        (CL + "ClientMain.java", "Điểm vào chương trình, đọc tham số host / port"),
        (CL + "model/ClientState.java", "Người chơi hiện tại, danh sách online, lời mời đang chờ"),
        (CL + "ui/LoginFrame.java", "Màn hình đăng nhập"),
        (CL + "ui/LobbyFrame.java", "Sảnh chờ: người chơi online, thách đấu, điều hướng"),
        (CL + "ui/OnlineTableModel.java", "Mô hình bảng người chơi online"),
        (CL + "ui/InviteDialog.java", "Hộp thoại lời mời đếm ngược"),
        (CL + "ui/RaceLauncher.java", "Nối thông điệp trận vào màn hình đua"),
    ],
    "details_intro": (
        "Phần này trình bày cách hiện thực từng task, kèm đoạn mã trích trực tiếp từ mã nguồn hiện tại. Nguyên tắc "
        "xuyên suốt của phía client là: không bao giờ làm việc mạng trên luồng giao diện (Event Dispatch Thread – "
        "EDT) và không bao giờ cập nhật giao diện từ luồng mạng."),
    "sections": [
        ("3.1. Kết nối TCP tới server (C1)", [
            ("p", "connect() tạo Socket và nối tới host:port với thời gian chờ 5 giây, để khi nhập sai IP người chơi "
                  "nhận lỗi ngay thay vì chờ mặc định của hệ điều hành. ObjectOutputStream được tạo và flush trước "
                  "ObjectInputStream: constructor của ObjectInputStream chờ đọc header từ phía kia, nếu cả hai phía "
                  "cùng tạo luồng vào trước thì sẽ chờ nhau mãi."),
            ("code", method(CL + "net/NetworkClient.java", r"public void connect\("), "NetworkClient.connect()"),
            ("p", "Sau khi nối, client khởi động ba luồng nền: net-writer (gửi tin theo thứ tự FIFO), net-reader "
                  "(đọc tin) và net-heartbeat (gửi PING mỗi 5 giây để server không coi client là đã rớt sau 15 "
                  "giây im lặng). Cả ba đều là luồng daemon nên không giữ chương trình lại khi đóng cửa sổ."),
        ]),
        ("3.2. Luồng đọc, luồng ghi và đẩy về luồng giao diện (C1)", [
            ("p", "send() chỉ đưa thông điệp vào hàng đợi 256 phần tử nên gọi được từ EDT mà không làm đơ giao "
                  "diện; luồng net-writer lấy ra và ghi vào socket. Luồng đọc nhận từng Message rồi chuyển việc gọi "
                  "listener sang EDT bằng SwingUtilities.invokeLater, vì Swing không an toàn đa luồng."),
            ("code", method(CL + "net/NetworkClient.java", r"private void readLoop\(\)"), "NetworkClient.readLoop()"),
            ("code", method(CL + "net/NetworkClient.java", r"private void dispatch\(Message"),
             "NetworkClient.dispatch() – gọi listener trên EDT"),
            ("p", "Các màn hình đăng ký quan tâm tới loại thông điệp nào bằng on(type, listener). Hàm trả về một "
                  "Runnable để hủy đăng ký; mỗi cửa sổ giữ các hàm này và gọi khi đóng, tránh cửa sổ đã đóng vẫn "
                  "nhận thông điệp."),
            ("code", method(CL + "net/NetworkClient.java", r"public Runnable on\(MessageType"), "NetworkClient.on()"),
        ]),
        ("3.3. Đăng nhập và trạng thái client (C2)", [
            ("p", "LoginFrame cho nhập tên, mật khẩu và địa chỉ server dạng host:port. Việc kết nối chạy trong "
                  "SwingWorker để giao diện vẫn phản hồi trong lúc chờ; kết quả trả về EDT trong done()."),
            ("code", method(CL + "ui/LoginFrame.java", r"private void connectAsync\("), "LoginFrame.connectAsync()"),
            ("p", "ClientState lưu người chơi hiện tại, danh sách online và lời mời đang chờ trả lời. Quy tắc ai được "
                  "mời nằm ở một chỗ duy nhất (canInvite) để cả nút Thách đấu và hành động mời cùng dùng:"),
            ("code", method(CL + "model/ClientState.java", r"public boolean canInvite\("), "ClientState.canInvite()"),
        ]),
        ("3.4. Sảnh chờ (C3)", [
            ("p", "LobbyFrame đăng ký năm listener: ONLINE_LIST, INVITE_RECEIVED, INVITE_RESULT, MATCH_START và "
                  "ERROR. Danh sách người chơi tự cập nhật mỗi khi server phát ONLINE_LIST, không cần nút Làm mới; "
                  "dòng đang chọn được giữ nguyên sau khi bảng nạp lại."),
            ("fig", "fig4_lobby.png", "Luồng xử lý của sảnh chờ", 14.5),
            ("code", method(CL + "ui/LobbyFrame.java", r"private void registerListeners\(\)"),
             "LobbyFrame.registerListeners()"),
            ("code", method(CL + "ui/LobbyFrame.java", r"private void invite\(\)"), "LobbyFrame.invite()"),
            ("code", method(CL + "ui/LobbyFrame.java", r"private void onInviteResult\("),
             "LobbyFrame.onInviteResult() – thông báo theo từng trạng thái"),
        ]),
        ("3.5. Hộp thoại lời mời đếm ngược (C4)", [
            ("p", "InviteDialog dùng javax.swing.Timer (chạy trên EDT) để mỗi lần tick tính số giây còn lại theo hạn "
                  "chót server gửi kèm (expiresAtMillis) chứ không tự đếm lùi, nên hai máy hiển thị khớp nhau. "
                  "5 giây cuối số chuyển màu đỏ. Hết giờ thì hộp thoại tự đóng mà không gửi gì – server tự báo "
                  "TIMEOUT cho người mời."),
            ("code", method(CL + "ui/InviteDialog.java", r"private void tick\(\)"), "InviteDialog.tick()"),
            ("code", method(CL + "ui/InviteDialog.java", r"private void reply\(boolean"), "InviteDialog.reply()"),
            ("p", "Cờ replied chặn bấm hai lần (ví dụ bấm Chấp nhận rồi đóng cửa sổ), bảo đảm mỗi lời mời chỉ có "
                  "đúng một INVITE_REPLY."),
        ]),
        ("3.6. Điều hướng sảnh ↔ màn hình đua (C5)", [
            ("p", "Khi nhận MATCH_START, sảnh ẩn đi và RaceLauncher mở màn hình đua (do Trần Việt Anh viết), chuyển "
                  "tiếp các thông điệp trận cho nó. Nếu màn hình đua đang mở (ván rematch) thì sảnh bỏ qua vì màn "
                  "hình đua tự xử lý. Khi phòng đóng, sảnh hiện lại và nút Thách đấu được làm mới."),
            ("code", method(CL + "ui/LobbyFrame.java", r"private void onMatchStart\("), "LobbyFrame.onMatchStart()"),
            ("code", method(CL + "ui/RaceLauncher.java", r"public void open\("), "RaceLauncher.open()"),
        ]),
        ("3.7. Mất kết nối và đăng xuất (C6)", [
            ("p", "Lỗi đọc / ghi ở bất kỳ luồng nào đều đi về handleDisconnect(). compareAndSet bảo đảm chỉ xử lý "
                  "một lần dù luồng đọc và luồng ghi cùng gặp lỗi. Đăng xuất chủ động dùng close() và không phát sự "
                  "kiện mất kết nối."),
            ("code", method(CL + "net/NetworkClient.java", r"private void handleDisconnect\(\)"),
             "NetworkClient.handleDisconnect()"),
            ("code", method(CL + "ClientApp.java", r"private void onDisconnected\(\)"),
             "ClientApp.onDisconnected() – về màn hình đăng nhập"),
        ]),
        ("3.8. Hoàn thiện trải nghiệm (C7)", [
            ("b", [("Phím Enter ", "ở màn hình đăng nhập gửi luôn LOGIN; nút bị khóa và đổi chữ trong lúc chờ."),
                   ("Cửa sổ theo màn hình: ", "các cửa sổ tự co theo vùng làm việc (trừ thanh taskbar), nút của "
                    "hộp thoại lời mời và các màn hình luôn nằm trong vùng nhìn thấy trên laptop độ phân giải thấp."),
                   ("Thông báo rõ ràng: ", "mỗi kết quả lời mời có câu thông báo riêng; lỗi từ server hiện ở thanh "
                    "trạng thái kèm âm báo."),
                   ("Tự kết nối lại ", "khi người chơi đổi địa chỉ server ở màn hình đăng nhập.")]),
        ]),
    ],
    "tests": [
        ["T1", "Đăng nhập sai mật khẩu", "Báo lỗi, không vào sảnh", "Sửa lỗi"],
        ["T3", "3 client online, 1 client thoát", "Danh sách cập nhật trong 1 s", "Người test"],
        ["T4", "Mời người đang thi đấu", "Nhận BUSY, không mở hộp thoại ở người kia", "Người test"],
        ["T5", "Không trả lời lời mời 30 s", "Hộp thoại tự đóng, người mời nhận TIMEOUT", "Người test, sửa lỗi"],
        ["T6", "Từ chối lời mời", "Người mời nhận REJECTED", "Người test"],
        ["T12", "Rút mạng client đang đua", "Client còn lại về sảnh", "Sửa lỗi"],
        ["T14", "Một bên từ chối thi đấu tiếp", "Cả hai về sảnh, trạng thái Rảnh", "Người test, sửa lỗi"],
        ["T16", "Tắt server khi client đang ở sảnh", "Báo mất kết nối, về màn hình đăng nhập, không treo",
         "Người test, sửa lỗi"],
    ],
    "unit_tests": [
        ("client/src/test/java/racing/client/net/NetworkClientTest.java",
         "Kết nối tới server giả, gửi / nhận theo thứ tự, listener chạy trên EDT, báo mất kết nối"),
        ("client/src/test/java/racing/client/model/ClientStateTest.java",
         "Quy tắc ai được mời, lời mời đang chờ"),
        ("client/src/test/java/racing/client/ui/OnlineTableModelTest.java",
         "Bảng online: thứ tự, đánh dấu chính mình, tìm dòng theo tên"),
    ],
    "collab": [
        ("Với Phạm Thị Thu Phương: ", "thống nhất thứ tự LOGIN_RESULT rồi ONLINE_LIST và ý nghĩa các trạng thái "
         "INVITE_RESULT (OFFLINE, BUSY, REJECTED, TIMEOUT)."),
        ("Với Trần Việt Anh: ", "cung cấp NetworkClient để màn hình đua gửi CAR_STATE và nhận RACE_UPDATE; "
         "RaceLauncher nối hai phần với nhau."),
        ("Với Nguyễn Trần Mai Anh: ", "đặt nút Bảng xếp hạng, Lịch sử trận trên sảnh và nút Đăng ký trên màn hình "
         "đăng nhập để mở các cửa sổ do Mai Anh viết."),
    ],
    "problems": [
        ["Giao diện đơ khi server chậm hoặc sai IP", "Kết nối trong SwingWorker, timeout 5 s; gửi qua hàng đợi"],
        ["Cập nhật Swing từ luồng mạng gây vẽ lỗi", "Mọi listener được gọi qua SwingUtilities.invokeLater"],
        ["Hai phía cùng chờ header của ObjectInputStream", "Tạo và flush ObjectOutputStream trước"],
        ["Hộp thoại lời mời hai máy đếm lệch nhau", "Đếm theo hạn chót server gửi, không tự đếm lùi"],
        ["Nút của hộp thoại bị đẩy ra ngoài màn hình laptop", "Giới hạn kích thước theo vùng làm việc của màn hình"],
    ],
    "summary": (
        "Tự đánh giá: hoàn thành đủ 7 task C1–C7; client kết nối ổn định, sảnh cập nhật tức thì, lời mời và điều "
        "hướng hoạt động đúng trong mọi kịch bản kiểm thử. Qua phần việc này em hiểu rõ lập trình socket phía "
        "client, luồng vào ra đối tượng, cách tách luồng mạng khỏi luồng giao diện, mô hình listener / callback và "
        "cách phát hiện mất kết nối bằng heartbeat. Hạn chế: chưa tự động kết nối lại khi rớt mạng giữa chừng, "
        "người chơi phải đăng nhập lại."),
}
