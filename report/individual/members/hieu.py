"""Báo cáo cá nhân: Vũ Quốc Hiếu – Client sảnh (C1–C7)."""
import shared

CL = "client/src/main/java/racing/client/"

CONTENT = {
    "name": "Vũ Quốc Hiếu",
    "slug": "VuQuocHieu",
    "part_intro": [
        "Trong nhóm, em phụ trách phía client trước khi vào trận: lớp mạng NetworkClient mà mọi màn hình khác dùng "
        "để trao đổi với server, màn hình đăng nhập, sảnh chờ hiển thị người chơi online, hộp thoại lời mời đếm ngược "
        "30 giây, điều hướng giữa sảnh và màn hình đua, và xử lý khi mất kết nối.",
        "Đây là phần người chơi nhìn thấy đầu tiên và là cầu nối giữa giao diện Swing với socket TCP. Em không viết "
        "màn hình đua và phần máy chủ; hai phần đó được nối vào qua NetworkClient và các thông điệp chung trong module "
        "common.",
    ],
    "tasks": [
        ["C1", "NetworkClient: kết nối, luồng đọc, luồng gửi FIFO, đăng ký listener theo MessageType, đẩy callback "
               "về luồng Swing, PING mỗi 5 giây", "net/NetworkClient"],
        ["C2", "LoginFrame và ClientState: nhập tài khoản và địa chỉ server, gửi LOGIN, báo lỗi, chuyển sang sảnh",
         "ui/LoginFrame, model/ClientState, ClientApp"],
        ["C3", "LobbyFrame: bảng người chơi online từ ONLINE_LIST, nút THÁCH ĐẤU chỉ với người Rảnh, trạng thái chờ "
               "trả lời, xử lý INVITE_RESULT", "ui/LobbyFrame, ui/OnlineTableModel"],
        ["C4", "InviteDialog: đếm 30 giây về 0, thanh tiến trình, Chấp nhận / Từ chối gửi INVITE_REPLY, tự đóng khi "
               "hết giờ", "ui/InviteDialog"],
        ["C5", "Điều hướng: MATCH_START mở màn hình đua, quay về sảnh khi phòng đóng; mở cửa sổ Bảng xếp hạng và "
               "Lịch sử từ sảnh", "ui/LobbyFrame, ui/RaceLauncher"],
        ["C6", "Mất kết nối phía client: báo lỗi và quay về màn hình đăng nhập; đăng xuất gửi LOGOUT",
         "net/NetworkClient, ClientApp"],
        ["C7", "Hoàn thiện giao diện: phím Enter gửi đăng nhập, nút khóa trong lúc chờ, thông báo rõ cho từng kết quả "
               "lời mời", "ui/*"],
    ],
    "part_analysis": [
        "Phần việc của em nằm giữa giao diện và mạng. Yêu cầu chính là: đăng nhập báo lỗi rõ ràng, danh sách online "
        "cập nhật tức thì, lời mời có hạn 30 giây được hai bên hiểu thống nhất, và giao diện không đơ khi chờ mạng "
        "(YC01, YC03, YC04, YC11).",
        "Vì Swing không an toàn đa luồng, mọi cập nhật giao diện phải chạy trên luồng EDT. Em thiết kế NetworkClient "
        "để luồng đọc chỉ đẩy việc gọi listener sang EDT, còn gửi tin đi qua một hàng đợi riêng; nhờ vậy việc gửi, nhận "
        "và vẽ giao diện không chặn nhau.",
    ],
    "part_reqs": [
        ["H1", "Mọi việc mạng (kết nối, gửi, nhận) chạy ngoài EDT; listener luôn được gọi trên EDT", "YC11"],
        ["H2", "Đăng nhập: báo lỗi khi sai tài khoản hoặc mật khẩu, đúng thì mở sảnh; kết nối chạy trong SwingWorker",
         "YC01"],
        ["H3", "Sảnh hiển thị người chơi online và tự cập nhật khi nhận ONLINE_LIST; chỉ cho thách đấu người Rảnh",
         "YC03"],
        ["H4", "Hộp thoại lời mời đếm 30 giây theo hạn do server gửi, chỉ gửi đúng một INVITE_REPLY", "YC04"],
        ["H5", "Khi mất kết nối với server, báo lỗi và quay về màn hình đăng nhập ngay, không treo; heartbeat 5 giây "
               "giữ kết nối khỏi bị server coi là rớt", "YC11"],
        ["H6", "Chạy trên hai máy Windows trong cùng mạng LAN; cửa sổ và nút luôn nằm trong vùng nhìn thấy", "YC12"],
    ],
    "uc": {
        "title": "Phần client sảnh (C1–C7)",
        "actors": [("P", "Người chơi", "left", 2.5)],
        "cases": [
            ("dn", "Đăng nhập", 0, 0), ("ol", "Xem danh sách online", 0, 1), ("td", "Thách đấu người chơi", 0, 2),
            ("tl", "Trả lời lời mời", 0, 3), ("vt", "Vào màn hình đua", 0, 4), ("dx", "Đăng xuất", 0, 5),
            ("kn", "Kết nối tới server", 1, 0), ("mk", "Báo mất kết nối,\nvề đăng nhập", 1, 1),
            ("chk", "Chỉ mời người Rảnh", 1, 2), ("dem", "Đếm ngược 30 giây", 1, 3),
            ("ve", "Về sảnh khi phòng đóng", 1, 4), ("lo", "Gửi LOGOUT", 1, 5),
        ],
        "links": [("P", c, "assoc") for c in ("dn", "ol", "td", "tl", "vt", "dx")]
        + [("dn", "kn", "include"), ("mk", "ol", "extend"), ("td", "chk", "include"), ("tl", "dem", "include"),
           ("ve", "vt", "extend"), ("dx", "lo", "include")],
    },
    "uc_text": (
        "Biểu đồ use case chi tiết gồm sáu chức năng do người chơi thực hiện trực tiếp và các bước con đi kèm. Người "
        "chơi là tác nhân duy nhất. Kết nối tới server là một phần của đăng nhập (include); việc báo mất kết nối là nhánh "
        "mở rộng của sảnh chờ (extend), xảy ra khi đang ở sảnh mà server ngắt kết nối."
    ),
    "uc_specs": [
        {
            "name": "Đăng nhập",
            "actor": "Người chơi",
            "pre": "Ứng dụng client đã mở màn hình đăng nhập; server đang chạy; tài khoản đã được đăng ký.",
            "main": [
                "Người chơi nhập tài khoản, mật khẩu và địa chỉ server dạng host:port (mặc định cổng 5000).",
                "Client kiểm tra hai ô tài khoản, mật khẩu không rỗng, rồi mở kết nối trong SwingWorker; trong lúc "
                "chờ nút đăng nhập bị khóa.",
                "Sau khi nối được, client gửi LOGIN; server trả LOGIN_RESULT.",
                "Đăng nhập thành công: màn hình đăng nhập đóng, sảnh chờ mở với điểm và số trận thắng của người chơi.",
            ],
            "alt": [
                "A1. Ô tài khoản hoặc mật khẩu trống: báo “Nhập tài khoản và mật khẩu.”, không kết nối.",
                "A2. Cổng sai hoặc không nối được: báo lỗi kết nối, người chơi có thể sửa địa chỉ và thử lại.",
                "A3. Sai tài khoản hoặc mật khẩu: báo lỗi tại màn hình đăng nhập, con trỏ chọn lại ô mật khẩu.",
            ],
            "post": "Client đã đăng nhập; NetworkClient giữ kết nối để dùng cho sảnh và các trận sau đó.",
        },
        {
            "name": "Thách đấu người chơi",
            "actor": "Người chơi",
            "pre": "Đã đăng nhập; người được chọn đang Rảnh; người chơi chưa có lời mời nào đang chờ.",
            "main": [
                "Người chơi chọn một dòng trong bảng người chơi online.",
                "Nút THÁCH ĐẤU chỉ bật khi người được chọn đang Rảnh và không phải chính mình (ClientState.canInvite).",
                "Người chơi bấm THÁCH ĐẤU; client gửi INVITE tới server và nút chuyển sang “Đang chờ trả lời…”.",
                "Khi nhận INVITE_RESULT, nút mở lại và sảnh thông báo kết quả: từ chối, hết giờ, đang bận hoặc đã thoát.",
            ],
            "alt": [
                "A1. Người được mời chấp nhận: server gửi MATCH_START, sảnh ẩn và màn hình đua mở (use case “Vào màn hình đua”).",
                "A2. Người được mời đã thoát giữa chừng: báo người đó đã thoát, nút được mở lại.",
            ],
            "post": "Lời mời đã có kết quả; không còn lời mời nào đang chờ ở phía người mời.",
        },
    ],
    "classes": {
        "classes": [
            ("NetworkClient", None,
             ["- socket: Socket", "- outbox: BlockingQueue<Message>", "- listeners: Map<MessageType, List>"],
             ["+connect(host: String, port: int): void", "+send(m: Message): void",
              "+on(type: MessageType, l: Consumer): Runnable", "+onDisconnect(l: Runnable): Runnable",
              "+close(): void"], 0, 0, "blue"),
            ("ClientApp", None,
             ["- state: ClientState", "- net: NetworkClient", "- loggingOut: boolean"],
             ["+ensureConnected(host: String, port: int): NetworkClient", "+onLoggedIn(me: PlayerInfo, w: Window): void",
              "+logout(): void"], 1, 0, "purple"),
            ("ClientState", None,
             ["- me: PlayerInfo", "- online: List<PlayerInfo>", "- pendingInviteTarget: String"],
             ["+setOnline(list: List<PlayerInfo>): void", "+canInvite(p: PlayerInfo): boolean",
              "+markInviteSent(target: String): void", "+clearPendingInvite(): void"], 2, 0, "green"),
            ("LoginFrame", None,
             ["- app: ClientApp", "- userField: JTextField", "- passField: JPasswordField"],
             ["+login(): void", "+connectAsync(connected: Consumer): void", "-onLoginResult(m: Message): void"],
             0, 1, "orange"),
            ("LobbyFrame", None,
             ["- net: NetworkClient", "- state: ClientState", "- model: OnlineTableModel"],
             ["-registerListeners(): void", "-onOnlineList(list: List<PlayerInfo>): void",
              "-onInviteResult(r: InviteResult): void", "-onMatchStart(start: MatchStart): void", "-invite(): void"],
             1, 1, "orange"),
            ("OnlineTableModel", None,
             ["- rows: List<PlayerInfo>", "- selfUsername: String"],
             ["+setRows(players: List<PlayerInfo>, self: String): void", "+playerAt(row: int): PlayerInfo",
              "+rowOf(username: String): int"], 2, 1, "green"),
            ("InviteDialog", None,
             ["- net: NetworkClient", "- invite: InviteInfo", "- replied: boolean"],
             ["-tick(): void", "-reply(accept: boolean): void"], 0, 2, "orange"),
            ("RaceLauncher", None,
             ["- net: NetworkClient", "- frame: RaceFrame"],
             ["+open(start: MatchStart): void", "+isOpen(): boolean"], 1, 2, "orange"),
        ],
        "relations": [
            ("ClientApp", "NetworkClient", "comp", ""),
            ("ClientApp", "ClientState", "comp", ""),
            ("LoginFrame", "ClientApp", "dep", "ensureConnected"),
            ("LoginFrame", "NetworkClient", "dep", "on, send"),
            ("LobbyFrame", "OnlineTableModel", "comp", ""),
            ("LobbyFrame", "ClientState", "assoc", ""),
            ("LobbyFrame", "InviteDialog", "dep", "tạo khi có lời mời"),
            ("LobbyFrame", "RaceLauncher", "comp", "mở trận"),
        ],
    },
    "class_files": {
        "NetworkClient": CL + "net/NetworkClient.java",
        "ClientApp": CL + "ClientApp.java",
        "ClientState": CL + "model/ClientState.java",
        "LoginFrame": CL + "ui/LoginFrame.java",
        "LobbyFrame": CL + "ui/LobbyFrame.java",
        "OnlineTableModel": CL + "ui/OnlineTableModel.java",
        "InviteDialog": CL + "ui/InviteDialog.java",
        "RaceLauncher": CL + "ui/RaceLauncher.java",
    },
    "class_text": (
        "Biểu đồ lớp tập trung vào tám lớp của phần client sảnh. NetworkClient là lớp mạng duy nhất, được ClientApp "
        "giữ và truyền cho các màn hình. ClientState giữ trạng thái người chơi và quy tắc mời. Các cửa sổ Swing "
        "(LoginFrame, LobbyFrame, InviteDialog) chỉ gọi NetworkClient và ClientState, không làm việc với socket."
    ),
    "class_rows": [
        ["NetworkClient", "Kết nối TCP, luồng đọc / ghi, heartbeat và đăng ký listener theo MessageType"],
        ["ClientApp", "Giữ kết nối và trạng thái, chuyển giữa đăng nhập và sảnh, xử lý đăng xuất và mất kết nối"],
        ["ClientState", "Người chơi hiện tại, danh sách online, lời mời đang chờ và quy tắc ai được mời"],
        ["LoginFrame", "Màn hình đăng nhập, kết nối trong SwingWorker, báo lỗi"],
        ["LobbyFrame", "Sảnh chờ: nhận các thông điệp của sảnh, nút THÁCH ĐẤU, điều hướng vào trận"],
        ["OnlineTableModel", "Mô hình bảng người chơi online: thứ tự hàng, tra cứu dòng theo tên"],
        ["InviteDialog", "Hộp thoại lời mời đếm ngược 30 giây, gửi INVITE_REPLY một lần"],
        ["RaceLauncher", "Mở màn hình đua khi có MATCH_START và chuyển tiếp thông điệp trận cho màn hình đó"],
    ],
    "sequences": [
        {
            "title": "Biểu đồ tuần tự: đăng nhập và nhận danh sách online",
            "text": (
                "Đăng nhập chạy trong SwingWorker để EDT không bị chặn khi chờ socket. Khi server trả LOGIN_RESULT, luồng "
                "net-reader chuyển việc gọi listener sang EDT, nên LoginFrame mới được cập nhật giao diện."
            ),
            "participants": [("U", "Người chơi", "actor"), ("LF", "LoginFrame", "object"),
                             ("CA", "ClientApp", "object"), ("NC", "NetworkClient", "object"),
                             ("SV", "Server", "object")],
            "messages": [
                ("U", "LF", "Bấm Đăng nhập", "sync"),
                ("LF", "CA", "ensureConnected(host, port)", "sync"),
                ("CA", "NC", "connect(host, port)", "sync"),
                ("NC", "SV", "Mở socket TCP :5000", "sync"),
                ("CA", "LF", "trả về NetworkClient", "return"),
                ("LF", "NC", "on(LOGIN_RESULT, ...)", "sync"),
                ("LF", "NC", "send(LOGIN)", "sync"),
                ("NC", "SV", "LOGIN(LoginRequest)", "sync"),
                ("SV", "NC", "LOGIN_RESULT", "return"),
                ("NC", "LF", "invokeLater: onLoginResult(m)", "return"),
                ("LF", "CA", "onLoggedIn(me): mở sảnh", "sync"),
                ("SV", "NC", "ONLINE_LIST", "return"),
            ],
            "gap": 1.9,
        },
        {
            "title": "Biểu đồ tuần tự: thách đấu và trả lời lời mời",
            "text": (
                "Lời mời đi từ server tới luồng net-reader của người được mời, rồi được chuyển sang EDT để mở "
                "InviteDialog. Hộp thoại tự đếm theo hạn chót server gửi kèm; nếu hết giờ thì đóng mà không gửi gì, "
                "và server tự báo TIMEOUT cho người mời."
            ),
            "participants": [("A", "Người mời", "actor"), ("LA", "LobbyFrame A", "object"),
                             ("SV", "Server", "object"), ("NB", "NetworkClient B", "object"),
                             ("LB", "LobbyFrame B", "object"), ("ID", "InviteDialog B", "object")],
            "messages": [
                ("A", "LA", "Bấm THÁCH ĐẤU", "sync"),
                ("LA", "SV", "INVITE(người được mời)", "sync"),
                ("SV", "NB", "INVITE_RECEIVED(InviteInfo)", "sync"),
                ("NB", "LB", "net-reader → invokeLater (EDT)", "sync"),
                ("LB", "ID", "new InviteDialog, setVisible", "sync"),
                ("ID", "ID", "đếm 30 s", "self"),
                ("ID", "NB", "Chấp nhận / Từ chối", "sync"),
                ("NB", "SV", "INVITE_REPLY(accept)", "sync"),
                ("SV", "LA", "INVITE_RESULT (từ chối, hết giờ, bận, đã thoát)", "return"),
                ("SV", "NB", "MATCH_START (khi chấp nhận)", "return"),
            ],
            "frames": [(5, 8, "alt: trả lời trong 30 giây")],
            "gap": 1.7,
        },
    ],
    "er": {
        "entities": [
            ("players", [("player_id", "PK", "INT"), ("username", "", "VARCHAR(50)"),
                         ("password_hash", "", "VARCHAR(255)"), ("points", "", "INT"), ("wins", "", "INT"),
                         ("losses", "", "INT"), ("draws", "", "INT"), ("created_at", "", "DATETIME")],
             0.3, 2.9, "blue"),
            ("matches", [("match_id", "PK", "INT"), ("room_code", "", "VARCHAR(20)"),
                         ("player1_id", "FK", "INT"), ("player2_id", "FK", "INT"), ("winner_id", "FK", "INT"),
                         ("status", "", "ENUM"), ("end_reason", "", "ENUM"), ("started_at", "", "DATETIME"),
                         ("ended_at", "", "DATETIME")],
             4.0, 2.9, "blue"),
        ],
        "relations": [("players", "matches", "1", "0..*", "tham gia", (0.0, 0.25))],
        "W": 6.6,
        "H": 3.4,
    },
    "er_text": [
        "Phần việc của em đọc hai bảng players và matches. Khi đăng nhập, bảng players được dùng để kiểm tra tài khoản "
        "(username, password_hash); danh sách online hiển thị điểm và số trận thắng (points, wins) của mỗi người. "
        "Mỗi trận trong matches gắn với hai người chơi qua player1_id và player2_id, và winner_id cho biết người thắng.",
        "Client không truy cập CSDL trực tiếp: mọi dữ liệu này đi qua server và chỉ đến màn hình sảnh dưới dạng "
        "PlayerInfo trong thông điệp ONLINE_LIST.",
    ],
    "screens": [
        (shared.SHOTS / "login.png", "Màn hình đăng nhập (LoginFrame)", 12),
        (shared.SHOTS / "lobby.png", "Sảnh chờ (LobbyFrame) với danh sách người chơi online", 14),
    ],
    "results": [
        ["Lớp mạng NetworkClient (C1, C6)", "Đã cài đặt kết nối, luồng đọc / ghi FIFO, heartbeat PING 5 giây, listener "
         "trên EDT và xử lý mất kết nối; có test tự động (NetworkClientTest)"],
        ["Đăng nhập và trạng thái (C2)", "Đăng nhập chạy trong SwingWorker; sai mật khẩu báo lỗi tại chỗ, đúng thì mở "
         "sảnh; ClientState quản lý người chơi và lời mời (ClientStateTest)"],
        ["Sảnh chờ và bảng online (C3, C5)", "Danh sách online tự cập nhật khi nhận ONLINE_LIST; nút THÁCH ĐẤU bị khóa "
         "khi đang chờ trả lời; vào đua và quay về sảnh (OnlineTableModelTest cho bảng)"],
        ["Hộp thoại lời mời (C4)", "Đếm ngược theo hạn do server gửi, 5 giây cuối chuyển sang màu đỏ, chỉ gửi một "
         "INVITE_REPLY"],
        ["Mất kết nối và đăng xuất (C6)", "Lỗi đọc / ghi chỉ được xử lý một lần; đăng xuất chủ động không báo mất kết nối"],
        ["Hoàn thiện giao diện (C7)", "Phím Enter gửi đăng nhập; nút bị khóa trong lúc chờ; mỗi kết quả lời mời có "
         "thông báo riêng"],
    ],
    "tests": [
        ["T1", "Đăng nhập sai mật khẩu", "Báo lỗi sai tài khoản hoặc mật khẩu, vẫn ở màn đăng nhập",
         "Chạy thủ công khi demo"],
        ["T3", "3 client online, 1 client thoát", "Danh sách hai client còn lại cập nhật trong 1 giây",
         "Chạy thủ công khi demo"],
        ["T4", "Mời người đang thi đấu", "Người mời nhận BUSY, người bị mời không thấy hộp thoại",
         "Chạy thủ công khi demo"],
        ["T5", "Không trả lời lời mời 30 giây", "Hộp thoại tự đóng, người mời nhận TIMEOUT",
         "Chạy thủ công khi demo"],
        ["T6", "Từ chối lời mời", "Người mời nhận REJECTED, cả hai vẫn Rảnh", "Chạy thủ công khi demo"],
        ["T12", "Rút mạng client đang đua", "Client còn lại về sảnh", "Chạy thủ công khi demo"],
        ["T14", "Một bên từ chối thi đấu tiếp", "Cả hai về sảnh, trạng thái Rảnh", "Chạy thủ công khi demo"],
        ["T16", "Tắt server khi client đang ở sảnh", "Báo mất kết nối, về màn hình đăng nhập, không treo",
         "Chạy thủ công khi demo"],
    ],
    "unit_tests": [
        ("client/src/test/java/racing/client/net/NetworkClientTest.java",
         "Gửi đúng thứ tự, listener nhận payload trên EDT, listener chậm không làm đơ giao diện, hủy đăng ký, báo "
         "mất kết nối khi server đóng socket, đóng chủ động không báo lỗi"),
        ("client/src/test/java/racing/client/model/ClientStateTest.java",
         "Làm mới bản thân khi nhận danh sách online, chỉ mời người Rảnh, không mời khi đang có lời mời"),
        ("client/src/test/java/racing/client/ui/OnlineTableModelTest.java",
         "Thứ tự hàng (mình đầu, sau đó theo tên), tra cứu dòng theo tên, nhãn tiếng Việt, kiểm tra dữ liệu đăng ký"),
    ],
    "conclusion": [
        "Em đã cài đặt mã nguồn cho đủ bảy task C1–C7 của phần client sảnh: lớp mạng, đăng nhập, sảnh chờ, hộp thoại "
        "lời mời, điều hướng, xử lý mất kết nối và hoàn thiện giao diện. Các lớp mạng và trạng thái có kiểm thử tự động "
        "JUnit. Kịch bản kiểm thử tích hợp theo docs/TEST-PLAN.md chưa có kết quả ghi lại trong báo cáo này nên được "
        "đánh dấu chạy thủ công khi demo.",
        "Qua phần việc này em hiểu rõ lập trình socket phía client, luồng vào ra đối tượng, cách tách luồng mạng khỏi "
        "luồng giao diện, mô hình listener và callback, và cách phát hiện mất kết nối bằng heartbeat.",
        "Hạn chế: client chưa tự kết nối lại khi rớt mạng giữa chừng; sau khi mất kết nối, người chơi phải đăng nhập lại.",
        "Hướng phát triển: tự động kết nối lại kèm đếm lùi, hiển thị chất lượng kết nối (độ trễ PING), và viết kiểm thử "
        "giao diện tự động cho sảnh chờ.",
    ],
}

