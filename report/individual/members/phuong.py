"""Báo cáo cá nhân: Phạm Thị Thu Phương – Server (S1–S7)."""
from source import lines_of, method

SRV = "server/src/main/java/racing/server/"

CONTENT = {
    "name": "Phạm Thị Thu Phương",
    "slug": "PhamThiThuPhuong",
    "role": "Server – kết nối, phiên, lời mời, phòng đua, trọng tài (S1–S7)",
    "intro": (
        "Trong nhóm, Phạm Thị Thu Phương phụ trách toàn bộ phía server: nhận kết nối TCP, quản lý phiên đăng nhập, "
        "chuyển lời mời thách đấu, tạo và điều khiển phòng đua, làm trọng tài tính quãng đường – va chạm – người "
        "thắng, xử lý thoát trận và mất kết nối, bảo đảm nhiều phòng chạy song song không lẫn dữ liệu. Server là "
        "trung tâm của hệ thống: mọi thông điệp từ client đều đi qua phần việc này trước khi tới cơ sở dữ liệu "
        "hoặc tới người chơi còn lại."),
    "tasks": [
        ["S1", "Khung server: mở ServerSocket cổng 5000, pool luồng, ClientHandler với vòng readObject, gửi tin "
               "không chặn, log ra console", "GameServer, net/ClientHandler",
         "Nhiều client kết nối cùng lúc, PING nhận PONG; một client đóng không làm server dừng"],
        ["S2", "SessionManager: đăng nhập qua PlayerRepository, danh sách online, trạng thái FREE / IN_MATCH, phát "
               "ONLINE_LIST, đăng xuất", "core/SessionManager, core/Session",
         "Chống đăng nhập trùng; danh sách online cập nhật ngay khi có người vào / ra"],
        ["S3", "InviteManager: INVITE → INVITE_RECEIVED, INVITE_REPLY, hết hạn 30 s, trả BUSY / OFFLINE / "
               "REJECTED / TIMEOUT", "core/InviteManager",
         "Mời người đang đấu nhận BUSY; không trả lời 30 s nhận TIMEOUT"],
        ["S4", "Room + RoomManager + CarSim: tạo phòng, sinh 48 xe cộ theo seed, MATCH_START, đếm ngược 3-2-1-0, "
               "tick 50 ms, cắt ngưỡng tốc độ, xét va chạm, phát RACE_UPDATE",
         "core/Room, RoomManager, CarSim, ObstacleGenerator",
         "Gửi tốc độ 999 bị cắt về 360; đâm xe cộ choáng 1,5 s"],
        ["S5", "MatchService: về đích, hòa cùng tick, thoát, mất kết nối; lưu kết quả; MATCH_RESULT; rematch",
         "core/MatchService, Room.endRound", "Điểm trong DB đúng mọi tình huống; rematch tạo ván mới"],
        ["S6", "Mất kết nối: SO_TIMEOUT 15 s, xử lý IOException ở mọi trạng thái, dọn phòng, cập nhật online",
         "ClientHandler, SessionManager, RoomManager", "Rút mạng khi đua: ≤ 15 s đối thủ thắng, DB ghi DISCONNECT"],
        ["S7", "Đồng thời và chịu tải: hàng đợi gửi riêng mỗi client, synchronized theo phòng, nhiều phòng song "
               "song", "toàn bộ server", "2 phòng đua cùng lúc không lẫn dữ liệu, không deadlock"],
    ],
    "files": [
        (SRV + "GameServer.java", "Mở cổng, vòng accept, scheduler tick 50 ms và quét hết hạn 1 s"),
        (SRV + "net/ClientHandler.java", "Một kết nối TCP: luồng đọc, luồng ghi, phân loại thông điệp"),
        (SRV + "core/SessionManager.java", "Đăng nhập, danh sách online, trạng thái người chơi"),
        (SRV + "core/Session.java", "Phiên của một người chơi đã đăng nhập"),
        (SRV + "core/InviteManager.java", "Lời mời thách đấu và hẹn giờ 30 s"),
        (SRV + "core/RoomManager.java", "Tạo / đóng phòng, tick tất cả phòng, định tuyến thông điệp trận"),
        (SRV + "core/Room.java", "Máy trạng thái phòng đua, kết thúc ván, rematch"),
        (SRV + "core/CarSim.java", "Mô phỏng xe phía server (trọng tài)"),
        (SRV + "core/ObstacleGenerator.java", "Sinh dòng xe cộ theo seed, giống nhau cho hai người"),
        (SRV + "core/MatchService.java", "Tạo trận, chốt kết quả, ghi sự kiện"),
    ],
    "details_intro": (
        "Phần này trình bày cách hiện thực từng task, kèm đoạn mã trích trực tiếp từ mã nguồn hiện tại của repo và "
        "giải thích các quyết định thiết kế. Thứ tự đi theo đường đi của một thông điệp: từ lúc client kết nối, "
        "đăng nhập, mời nhau, vào phòng đua cho tới khi trận kết thúc hoặc một bên rớt mạng."),
    "sections": [
        ("3.1. Khung server và vòng nhận kết nối (S1)", [
            ("p", "GameServer mở một ServerSocket duy nhất ở cổng 5000. Luồng acceptor chạy vòng accept(): mỗi "
                  "socket mới được bọc trong một ClientHandler và giao cho pool luồng, nên server phục vụ nhiều "
                  "client cùng lúc mà không client nào chặn client khác. Cùng lúc đó một ScheduledExecutorService "
                  "chạy hai công việc định kỳ: tick tất cả phòng đua mỗi 50 ms và quét lời mời / rematch hết hạn mỗi "
                  "giây."),
            ("code", method(SRV + "GameServer.java", r"public void start\(\)"), "GameServer.start()"),
            ("code", method(SRV + "GameServer.java", r"private void acceptLoop\(\)"), "GameServer.acceptLoop()"),
            ("p", "Server có hai chế độ khởi động: mặc định dùng MySQL (withMysql) và --memory dùng kho dữ liệu "
                  "trong bộ nhớ với 6 tài khoản mẫu, để demo được khi máy chưa cài MySQL. Hai chế độ dùng chung "
                  "toàn bộ phần lõi vì lõi chỉ phụ thuộc giao diện PlayerRepository / MatchRepository."),
            ("fig", "fig3_server.png", "Các thành phần phía server", 14.5),
        ]),
        ("3.2. ClientHandler: tách luồng đọc và luồng ghi (S1, S6, S7)", [
            ("p", "Mỗi kết nối có một ClientHandler. Hàm khởi tạo đặt SO_TIMEOUT 15 giây (client gửi PING mỗi 5 "
                  "giây nên 15 giây im lặng nghĩa là đã rớt), bật TCP_NODELAY để gói nhỏ như CAR_STATE đi ngay, và "
                  "tạo ObjectOutputStream trước ObjectInputStream rồi flush để hai phía không chờ header của nhau."),
            ("code", method(SRV + "net/ClientHandler.java", r"public void run\(\)"), "ClientHandler.run() – luồng đọc"),
            ("p", "Các nhánh catch phân biệt rõ ba kiểu kết thúc: quá 15 giây không có dữ liệu "
                  "(SocketTimeoutException), client tự đóng (EOFException) và lỗi mạng khác. Dù kết thúc kiểu nào, "
                  "khối finally luôn gọi cleanup() để dọn phòng, lời mời và phiên."),
            ("p", "Gửi tin được tách sang luồng ghi riêng qua một hàng đợi BlockingQueue 256 phần tử. Lý do: luồng "
                  "tick của phòng gửi RACE_UPDATE cho cả hai người; nếu ghi thẳng vào socket thì một client bị treo "
                  "(không đọc) sẽ làm writeObject chặn và đứng cả phòng. Với hàng đợi, send() không bao giờ chặn; "
                  "hàng đợi đầy nghĩa là client không theo kịp và bị ngắt kết nối."),
            ("code", method(SRV + "net/ClientHandler.java", r"public void send\(Message"), "ClientHandler.send()"),
            ("code", method(SRV + "net/ClientHandler.java", r"private void writeLoop\(\)"),
             "ClientHandler.writeLoop() – luồng ghi"),
            ("p", "out.reset() sau mỗi lần ghi là bắt buộc: ObjectOutputStream nhớ các đối tượng đã gửi và lần sau "
                  "chỉ gửi tham chiếu, nên nếu không reset thì client sẽ nhận lại trạng thái cũ dù server đã cập nhật."),
        ]),
        ("3.3. Phân loại thông điệp theo trạng thái đăng nhập", [
            ("p", "dispatch() xử lý các thông điệp không cần đăng nhập (PING, LOGIN, REGISTER, LOGOUT). Mọi loại "
                  "khác chỉ được chuyển cho dịch vụ tương ứng khi đã có phiên; chưa đăng nhập thì trả ERROR. Nhờ vậy "
                  "client chưa xác thực không thể mời, đua hay đọc lịch sử."),
            ("code", method(SRV + "net/ClientHandler.java", r"private void dispatchLoggedIn\("),
             "ClientHandler.dispatchLoggedIn()"),
            ("table", ["Thông điệp", "Dịch vụ xử lý", "Ghi chú"], [
                ["INVITE, INVITE_REPLY", "InviteManager", "Kiểm tra bận / offline, hẹn giờ 30 s"],
                ["CAR_STATE, FINISH", "RoomManager → Room → CarSim", "Chỉ nhận làn và tốc độ, server tự tính quãng đường"],
                ["QUIT_MATCH, REMATCH_REPLY", "RoomManager → Room", "Kết thúc ván / tạo ván mới"],
                ["LEADERBOARD_REQ, MATCH_HISTORY_REQ", "AccountService", "Truy vấn DB, lỗi SQL trả ERROR"],
            ], [5.0, 5.0, 6.0], "Định tuyến thông điệp sau khi đăng nhập"),
        ]),
        ("3.4. Quản lý phiên đăng nhập (S2)", [
            ("p", "SessionManager giữ map online (ConcurrentHashMap tên → Session). Hàm login() được đánh dấu "
                  "synchronized để hai máy đăng nhập cùng tài khoản cùng lúc không thể cùng lọt qua bước kiểm tra "
                  "trùng. Sau khi thêm phiên, server phát ONLINE_LIST cho mọi người."),
            ("code", method(SRV + "core/SessionManager.java", r"public synchronized LoginResult login\("),
             "SessionManager.login()"),
            ("p", "Khi kiểm thử phát hiện người đăng nhập sau cùng thấy sảnh trống: ONLINE_LIST phát trong login() "
                  "tới client trước LOGIN_RESULT, lúc client chưa mở sảnh nên bị bỏ qua. Cách sửa là gửi lại "
                  "ONLINE_LIST riêng cho người đó ngay sau LOGIN_RESULT (ClientHandler.login)."),
        ]),
        ("3.5. Lời mời thách đấu (S3)", [
            ("p", "InviteManager lưu lời mời đang chờ theo id và theo tên người chơi (byUser) để biết ai đang bận "
                  "với một lời mời. Mọi hàm public đều synchronized vì lời mời được đọc / ghi từ nhiều luồng: luồng "
                  "của người mời, luồng của người được mời và luồng scheduler quét hết hạn."),
            ("code", method(SRV + "core/InviteManager.java", r"public synchronized void invite\(", 36),
             "InviteManager.invite()"),
            ("table", ["Trạng thái", "Khi nào", "Người mời nhận"], [
                ["OFFLINE", "Người được mời không online hoặc thoát giữa chừng", "INVITE_RESULT OFFLINE"],
                ["BUSY", "Người được mời đang đấu hoặc đang có lời mời khác", "INVITE_RESULT BUSY"],
                ["REJECTED", "Người được mời bấm Từ chối", "INVITE_RESULT REJECTED"],
                ["TIMEOUT", "Quá 30 giây không trả lời (sweep() mỗi giây)", "INVITE_RESULT TIMEOUT"],
                ["Chấp nhận", "Người được mời bấm Chấp nhận", "MATCH_START (tạo phòng)"],
            ], [2.6, 8.4, 5.0], "Các kết quả của một lời mời"),
            ("code", method(SRV + "core/InviteManager.java", r"public synchronized void sweep\(\)"),
             "InviteManager.sweep() – báo hết hạn"),
        ]),
        ("3.6. Phòng đua và máy trạng thái (S4)", [
            ("p", "Room là máy trạng thái WAITING → COUNTDOWN → RACING → AWAIT_REMATCH → CLOSED. Mọi hàm public "
                  "của Room đều synchronized nên các thao tác trong một phòng được tuần tự hóa, nhưng các phòng khác "
                  "nhau chạy độc lập. startRound() tạo bản ghi trận, sinh dòng xe cộ theo seed (cả hai người thấy "
                  "giống hệt nhau) và gửi MATCH_START cho từng người với góc nhìn riêng (me / opponent)."),
            ("fig", "fig7_states.png", "Máy trạng thái của phòng đua", 14.0),
            ("code", method(SRV + "core/Room.java", r"public synchronized void tick\(\)"), "Room.tick() – một tick trọng tài"),
            ("p", "Mỗi tick 50 ms, server tiến cả hai xe, ghi sự kiện va chạm, gửi RACE_UPDATE cho từng người rồi "
                  "xét về đích. Luật hòa được chốt ở server: hai xe về đích trong cùng một tick là hòa; chỉ một xe về "
                  "đích thì người đó thắng. Client gửi FINISH chỉ mang tính gợi ý."),
        ]),
        ("3.7. CarSim – trọng tài chống gian lận (S4)", [
            ("p", "Điểm mấu chốt về an toàn: client chỉ được chọn làn và tốc độ, mọi giá trị đều bị cắt về khoảng "
                  "hợp lệ; quãng đường client gửi lên bị bỏ qua hoàn toàn. Server tự tích phân quãng đường từ tốc "
                  "độ đã cắt, nên một client sửa đổi gửi tốc độ 999 km/h vẫn chỉ chạy tối đa 360 km/h."),
            ("code", method(SRV + "core/CarSim.java", r"public void applyClient\("), "CarSim.applyClient()"),
            ("code", method(SRV + "core/CarSim.java", r"public Obstacle advance\("), "CarSim.advance()"),
            ("p", "Va chạm được xét theo vị trí xe cộ đã di chuyển tới tick hiện tại (positionAt(tick)), vì xe cộ "
                  "chạy cùng chiều với tốc độ 110 / 160 / 210 km/h. Mỗi xe cộ chỉ tính va chạm một lần; khi đâm, "
                  "tốc độ về 0 và bị choáng 1,5 giây, trong thời gian này CAR_STATE của client không có tác dụng."),
        ]),
        ("3.8. Kết thúc ván, lưu kết quả và thi đấu tiếp (S5)", [
            ("p", "endRound() là điểm kết thúc chung cho mọi lý do (FINISH, DRAW, QUIT, DISCONNECT): lưu kết quả "
                  "qua MatchService, gửi MATCH_RESULT theo góc nhìn từng người (WIN / LOSE / DRAW) kèm điểm mới, "
                  "rồi hỏi REMATCH_ASK. Hai bên cùng đồng ý thì startRound() tạo ván mới trong cùng phòng với dòng "
                  "xe cộ mới; một bên từ chối hoặc quá 30 giây thì phòng đóng và cả hai về sảnh."),
            ("code", method(SRV + "core/Room.java", r"private void endRound\("), "Room.endRound()"),
            ("code", method(SRV + "core/Room.java", r"private void leave\("), "Room.leave() – thoát / mất kết nối"),
            ("p", "Khi một người thoát hoặc rớt mạng giữa trận, người còn lại được xử thắng với lý do QUIT hoặc "
                  "DISCONNECT và không được hỏi thi đấu tiếp. MatchService.saveResult() cập nhật DB rồi nạp lại "
                  "điểm của hai Session để sảnh hiển thị điểm mới ngay."),
        ]),
        ("3.9. Mất kết nối và đồng thời (S6, S7)", [
            ("p", "Dọn dẹp khi một client rời đi phải theo đúng thứ tự: phòng trước (để đối thủ nhận kết quả), lời "
                  "mời sau, cuối cùng mới xóa phiên và phát danh sách online mới."),
            ("code", method(SRV + "net/ClientHandler.java", r"private void leaveEverything\(\)"),
             "ClientHandler.leaveEverything()"),
            ("b", [("Một phòng – một khóa: ", "Room dùng synchronized trên chính nó; luồng tick và luồng đọc của "
                    "hai client không thể sửa phòng cùng lúc."),
                   ("Cấu trúc dùng chung: ", "danh sách online, phòng, lời mời dùng ConcurrentHashMap; RoomManager "
                    "bọc mỗi phòng trong safe() để lỗi của một phòng không dừng tick của phòng khác."),
                   ("Không chặn khi gửi: ", "mọi send() chỉ đưa vào hàng đợi, nên luồng tick giữ khóa phòng không "
                    "bao giờ phải chờ mạng – tránh deadlock giữa khóa phòng và I/O."),
                   ("Ghi DB ngoài luồng tick: ", "match_events được ghi qua một executor riêng, tick 50 ms không bị "
                    "chậm bởi truy vấn INSERT.")]),
        ]),
    ],
    "tests": [
        ["T2", "Đăng nhập cùng tài khoản ở 2 máy", "Máy sau bị từ chối: \"Tài khoản đang đăng nhập ở nơi khác\"", "Sửa lỗi"],
        ["T3", "3 client online, 1 client thoát", "2 client còn lại thấy danh sách cập nhật trong 1 s", "Sửa lỗi"],
        ["T4", "Mời người đang thi đấu", "Nhận BUSY, người kia không hiện hộp thoại", "Sửa lỗi"],
        ["T5", "Không trả lời lời mời 30 s", "Người mời nhận TIMEOUT, cả hai vẫn Rảnh", "Sửa lỗi"],
        ["T6", "Từ chối lời mời", "Người mời nhận REJECTED", "Sửa lỗi"],
        ["T10", "Về đích cùng tick (script t10-draw)", "Hòa, mỗi người +1, winner_id NULL", "Sửa lỗi"],
        ["T12", "Rút mạng client đang đua", "≤ 15 s đối thủ thắng, end_reason = DISCONNECT", "Sửa lỗi"],
        ["T15", "2 phòng đua cùng lúc (4 client)", "Không lẫn dữ liệu giữa phòng", "Sửa lỗi"],
        ["T17", "Gửi CAR_STATE tốc độ 999 (script)", "Server cắt về 360, không thắng nhanh bất thường", "Sửa lỗi"],
    ],
    "unit_tests": [
        ("server/src/test/java/racing/server/core/RoomTest.java",
         "Đếm ngược, về đích, hòa cùng tick, thoát, mất kết nối, rematch đồng ý / từ chối / hết hạn"),
        ("server/src/test/java/racing/server/core/InviteManagerTest.java",
         "Tự mời, BUSY, OFFLINE, REJECTED, TIMEOUT bằng đồng hồ giả, người mời rớt mạng"),
        ("server/src/test/java/racing/server/core/SessionManagerTest.java",
         "Đăng nhập đúng / sai, chống đăng nhập trùng, đăng xuất, danh sách online"),
        ("server/src/test/java/racing/server/core/CarSimTest.java",
         "Cắt tốc độ, tích phân quãng đường, va chạm với xe cộ đang chạy, choáng 1,5 s"),
        ("server/src/test/java/racing/server/core/SocketSmokeTest.java", "Kết nối socket thật tới server, PING/PONG"),
        ("server/src/test/java/racing/server/net/ClientHandlerTest.java", "Hàng đợi gửi đầy thì ngắt client chậm"),
        ("server/src/test/java/racing/server/ServerIntegrationTest.java", "Trọn vòng đăng nhập – mời – đua với MySQL"),
    ],
    "collab": [
        ("Với Trần Việt Anh: ", "dùng hợp đồng common (Message, MessageType, DTO) và DAO nền; thống nhất luật hòa "
         "cùng tick và cách lưu kết quả trong transaction (task D5)."),
        ("Với Vũ Văn Hiếu: ", "thống nhất thứ tự LOGIN_RESULT / ONLINE_LIST và các trạng thái INVITE_RESULT để sảnh "
         "hiển thị đúng thông báo."),
        ("Với Nguyễn Trần Mai Anh: ", "đưa mẫu case trong ClientHandler để thêm LEADERBOARD_REQ, REGISTER, "
         "MATCH_HISTORY_REQ; cùng đặt các điểm gọi ghi sự kiện START, COLLISION, FINISH, QUIT, DISCONNECT."),
    ],
    "problems": [
        ["Một client không đọc làm đứng tick của cả phòng (writeObject bị chặn khi bộ đệm TCP đầy)",
         "Hàng đợi gửi 256 tin và luồng ghi riêng cho từng client; hàng đợi đầy thì ngắt client đó"],
        ["Client nhận RACE_UPDATE không đổi dù server đã cập nhật", "Gọi out.reset() sau mỗi writeObject"],
        ["Người đăng nhập sau cùng thấy sảnh trống", "Gửi lại ONLINE_LIST ngay sau LOGIN_RESULT"],
        ["Luật hòa khó tái hiện bằng 2 người thật", "Chốt hòa theo cùng tick tại server, kiểm thử bằng script T10"],
        ["Rớt mạng không phát hiện được (socket vẫn mở)", "SO_TIMEOUT 15 s kết hợp heartbeat PING 5 s của client"],
    ],
    "summary": (
        "Tự đánh giá: hoàn thành đủ 7 task S1–S7; server chạy ổn định với nhiều phòng song song, chống được gian "
        "lận tốc độ / quãng đường và xử lý đúng mọi cách kết thúc trận. Qua phần việc này em nắm được cách lập "
        "trình TCP Socket với ServerSocket / Socket, luồng vào ra đối tượng, lập trình đa luồng với "
        "ExecutorService, ScheduledExecutorService, BlockingQueue và đồng bộ hóa bằng synchronized / "
        "ConcurrentHashMap. Hạn chế: server dùng I/O chặn (mỗi client hai luồng), chưa dùng NIO, chưa mã hóa TLS."),
}

# Dùng để kiểm tra nhanh khi build: đoạn SQL / khai báo phải còn trong mã nguồn.
_ = lines_of(SRV + "core/Room.java", r"public synchronized void startRound", 1)
