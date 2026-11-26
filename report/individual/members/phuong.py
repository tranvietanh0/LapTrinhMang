"""Báo cáo cá nhân: Phạm Thị Thu Phương – Server (S1–S7)."""

CONTENT = {
    "name": "Phạm Thị Thu Phương",
    "slug": "PhamThiThuPhuong",
    "part_intro": [
        "Trong nhóm, em phụ trách toàn bộ phía server (task S1–S7). Server nhận kết nối TCP từ các client, quản lý "
        "phiên đăng nhập và danh sách người chơi đang online, chuyển lời mời thách đấu, tạo và điều khiển các phòng "
        "đua, làm trọng tài tính quãng đường, va chạm và người thắng, xử lý thoát trận và mất kết nối, rồi ghi kết "
        "quả xuống cơ sở dữ liệu MySQL.",
        "Mọi thông điệp từ client đều đi qua phần việc này trước khi tới CSDL hoặc tới người chơi còn lại. Vì vậy em "
        "chịu trách nhiệm chính về tính đúng đắn của luật chơi và sự ổn định khi nhiều phòng chạy song song.",
    ],
    "tasks": [
        ["S1", "Khung server: mở ServerSocket cổng 5000, luồng acceptor và pool luồng, ClientHandler đọc thông điệp "
               "bằng readObject, gửi tin không chặn",
         "GameServer, net/ClientHandler"],
        ["S2", "Quản lý phiên: đăng nhập, chống đăng nhập trùng, danh sách online, trạng thái Rảnh / Đang đấu, phát "
               "ONLINE_LIST",
         "core/SessionManager, core/Session"],
        ["S3", "Lời mời thách đấu: INVITE, INVITE_RECEIVED, INVITE_REPLY, kết quả OFFLINE / BUSY / REJECTED / TIMEOUT, "
               "hạn 30 giây",
         "core/InviteManager"],
        ["S4", "Phòng đua và trọng tài: tạo phòng, đếm ngược 3-2-1, tick 50 ms, RACE_UPDATE; CarSim tính quãng đường "
               "và va chạm",
         "core/Room, core/RoomManager, core/CarSim, core/ObstacleGenerator"],
        ["S5", "Kết thúc ván: xác định thắng / thua / hòa, MATCH_RESULT, thi đấu tiếp (REMATCH), đóng phòng",
         "core/Room (endRound), core/MatchService"],
        ["S6", "Thoát trận và mất kết nối: QUIT_MATCH, SO_TIMEOUT 15 s, dọn phòng, lời mời và phiên theo đúng thứ tự",
         "net/ClientHandler, core/RoomManager"],
        ["S7", "Đồng thời: hàng đợi gửi riêng cho mỗi client, khóa theo phòng, ghi diễn biến trận ngoài luồng tick",
         "toàn bộ phần server"],
    ],
    "part_analysis": [
        "Phần server có ba yêu cầu khó. Thứ nhất, luật chơi phải do server quyết định để không ai gian lận được bằng "
        "cách sửa client (YC06). Thứ hai, các thao tác trên cùng một phòng phải tuần tự, còn các phòng khác nhau phải "
        "độc lập (YC10). Thứ ba, phải phát hiện mất kết nối trong thời gian ngắn mà không làm đơ luồng tick (YC11). "
        "Mỗi yêu cầu được hiện thực bằng một cơ chế cụ thể, trình bày ở mục 2.2.",
        "Các chức năng nghiệp vụ còn lại (đăng nhập, lời mời, thi đấu tiếp, ghi kết quả) được đặc tả bằng use case và "
        "biểu đồ tuần tự ở mục 2.2. Lõi nghiệp vụ không phụ thuộc socket nên có thể kiểm thử bằng JUnit với đồng hồ "
        "giả, không cần mở mạng.",
    ],
    "part_reqs": [
        ["P1", "ServerSocket cổng 5000, mỗi client một ClientHandler và hai luồng; client treo không làm chặn client "
               "khác", "YC10"],
        ["P2", "Đăng nhập không trùng tài khoản; danh sách online cập nhật ngay khi có người vào hoặc ra", "YC01, YC03"],
        ["P3", "Lời mời có trạng thái OFFLINE, BUSY, REJECTED, TIMEOUT; hết 30 giây không trả lời thì báo hết hạn",
         "YC04"],
        ["P4", "Tạo phòng, đếm ngược 3-2-1-GO, tick 50 ms, gửi RACE_UPDATE riêng cho từng người chơi", "YC05"],
        ["P5", "Trọng tài: cắt tốc độ tối đa 360 km/h, va chạm làm xe choáng 1,5 s, xác định thắng / thua / hòa",
         "YC06"],
        ["P6", "Thoát trận hoặc mất kết nối (≤ 15 giây) thì đối thủ thắng và trận được ghi lý do", "YC07, YC11"],
        ["P7", "Thi đấu tiếp khi cả hai đồng ý; phòng đóng khi một bên từ chối hoặc hết hạn", "YC08"],
        ["P8", "Chốt kết quả trong một transaction, cộng điểm, ghi diễn biến vào match_events", "YC09"],
    ],
    "uc": {
        "title": "Phần server: đăng nhập, lời mời, phòng đua",
        "actors": [("P", "Người chơi", "left", 2.5)],
        "cases": [
            ("dn", "Đăng nhập", 0, 0), ("dx", "Đăng xuất", 0, 1), ("ol", "Xem danh sách\nngười chơi online", 0, 2),
            ("td", "Thách đấu\nngười chơi", 0, 3), ("tl", "Trả lời lời mời", 0, 4),
            ("dua", "Điều khiển xe\ntrong trận", 0, 5),
            ("kiem", "Kiểm tra tài khoản\nđang online", 1, 0), ("hh", "Hết hạn 30 giây\n(không trả lời)", 1, 4),
            ("kq", "Ghi kết quả\nvà điểm", 1, 5), ("tt", "Thi đấu tiếp", 1, 6.1),
            ("thoat", "Thoát trận /\nmất kết nối", 1, 7.2),
        ],
        "links": [("P", c, "assoc") for c in ("dn", "dx", "ol", "td", "tl", "dua")]
        + [("dn", "kiem", "include"), ("tl", "hh", "extend"), ("dua", "kq", "include"),
           ("tt", "dua", "extend"), ("thoat", "dua", "extend")],
        "row_h": 0.7,
    },
    "uc_text": (
        "Sơ đồ chỉ gồm các ca sử dụng do server xử lý, tác nhân là người chơi qua client. Cột trái là chức năng người "
        "chơi gọi trực tiếp. Cột phải là ca bao hàm (include) hoặc mở rộng (extend): kiểm tra tài khoản khi đăng "
        "nhập, hết hạn lời mời, ghi kết quả khi trận kết thúc, thi đấu tiếp và thoát trận."
    ),
    "uc_specs": [
        {
            "name": "Mời thách đấu và trả lời lời mời",
            "actor": "Người chơi (người mời, người được mời)",
            "pre": "Cả hai đã đăng nhập; người mời không đang trong trận; người được mời đang Rảnh.",
            "main": [
                "Người mời chọn một người chơi và gửi INVITE.",
                "Server kiểm tra: không tự mời mình, người mời chưa có lời mời khác, người được mời không đang trong "
                "trận và không có lời mời đang chờ.",
                "Server gửi INVITE_RECEIVED tới người được mời kèm thời hạn 30 giây.",
                "Người được mời bấm Chấp nhận; server tạo phòng đua (RoomManager.createRoom) và gửi MATCH_START cho cả "
                "hai.",
            ],
            "alt": [
                "3a. Người được mời không online hoặc đang bận: server trả INVITE_RESULT OFFLINE hoặc BUSY cho người mời.",
                "4a. Người được mời bấm Từ chối: người mời nhận INVITE_RESULT REJECTED.",
                "4b. Quá 30 giây không trả lời: InviteManager.sweep() gửi INVITE_RESULT TIMEOUT cho người mời.",
            ],
            "post": "Nếu chấp nhận, hai người ở trạng thái Đang đấu trong một phòng mới. Nếu không, cả hai vẫn Rảnh và "
                    "lời mời đã được xóa khỏi bộ nhớ.",
        },
        {
            "name": "Điều khiển xe và thi đấu tiếp",
            "actor": "Người chơi",
            "pre": "Hai người đã vào phòng và đã qua đếm ngược (GO); trận ở trạng thái đua.",
            "main": [
                "Client gửi CAR_STATE (làn, tốc độ) khi người chơi đổi làn hoặc bấm ga / phanh.",
                "Mỗi tick 50 ms, Room cập nhật hai xe bằng CarSim, xét va chạm với xe cộ và phát RACE_UPDATE cho từng "
                "người.",
                "Khi có xe về đích, server ghi kết quả (FINISH hoặc DRAW), gửi MATCH_RESULT rồi hỏi REMATCH_ASK.",
                "Cả hai bấm Đồng ý; server tạo ván mới trong cùng phòng với xe cộ mới.",
            ],
            "alt": [
                "2a. Va chạm xe cộ: xe choáng 1,5 giây, tốc độ về 0 và không tăng được cho tới khi hết choáng.",
                "3a. Một bên thoát hoặc mất kết nối: đối thủ thắng với lý do QUIT hoặc DISCONNECT, phòng đóng và không "
                "hỏi thi đấu tiếp.",
                "4a. Một bên từ chối hoặc quá 30 giây: phòng đóng, cả hai về sảnh.",
            ],
            "post": "Kết quả và điểm mới được chốt trong CSDL và nạp lại vào phiên; hai người về trạng thái Rảnh khi "
                    "phòng đóng.",
        },
    ],
    "classes": {
        "classes": [
            ("GameServer", None, ["-services: ServerServices"],
             ["+start(): void", "+stop(): void", "+withMysql(port): GameServer"], 0, 0, "blue"),
            ("ServerServices", "record", ["sessions: SessionManager", "invites: InviteManager", "rooms: RoomManager"],
             [], 1, 0, "orange"),
            ("SessionManager", None, ["-online: Map<String, Session>"],
             ["+login(req, conn): LoginResult", "+logout(s): void", "+onlineList(): List"], 2, 0, "orange"),
            ("ClientHandler", None, ["-session: Session", "-outbox: BlockingQueue"],
             ["+run(): void", "+send(message): void"], 0, 1, "blue"),
            ("InviteManager", None, ["-pending: Map<Long, Invite>", "-byUser: Map<String, Long>"],
             ["+invite(from, target): void", "+reply(replier, reply): void", "+sweep(): void"], 1, 1, "orange"),
            ("RoomManager", None, ["-rooms: Map<Integer, Room>"],
             ["+createRoom(a, b): Room", "+tickAll(): void", "+onQuit(s, roomId): void"], 2, 1, "purple"),
            ("Session", None, ["-playerId: int", "-status: PlayerStatus"],
             ["+send(message): void", "+info(): PlayerInfo"], 0, 2, "grey"),
            ("MatchService", None, ["-matches: MatchRepository"],
             ["+createMatch(room, a, b): int", "+saveResult(id, a, b, w, why): void", "+event(id, s, type, json): void"],
             1, 2, "green"),
            ("Room", None, ["-state: State", "-matchId: int"],
             ["+startRound(): void", "+countdown(value): void", "+tick(): void", "+onQuit(s): void"], 2, 2, "purple"),
            ("CarSim", None, ["-distance: double", "-lane: int", "-speed: double"],
             ["+applyClient(cs, now): void", "+advance(now, tick, obs): Obstacle"], 2, 3, "purple"),
        ],
        "relations": [
            ("GameServer", "ServerServices", "comp", "services"),
            ("GameServer", "ClientHandler", "dep", "tạo mỗi kết nối"),
            ("ClientHandler", "ServerServices", "dep", "gọi dịch vụ"),
            ("ClientHandler", "Session", "comp", "phiên"),
            ("ServerServices", "SessionManager", "agg", ""),
            ("ServerServices", "InviteManager", "agg", ""),
            ("ServerServices", "RoomManager", "agg", ""),
            ("InviteManager", "RoomManager", "dep", "tạo phòng"),
            ("RoomManager", "Room", "comp", "rooms"),
            ("Room", "MatchService", "dep", ""),
            ("Room", "CarSim", "comp", "2 xe"),
        ],
        "col_w": 2.7,
        "width": 16.0,
    },
    "class_files": {
        "GameServer": "server/src/main/java/racing/server/GameServer.java",
        "ServerServices": "server/src/main/java/racing/server/core/ServerServices.java",
        "SessionManager": "server/src/main/java/racing/server/core/SessionManager.java",
        "ClientHandler": "server/src/main/java/racing/server/net/ClientHandler.java",
        "InviteManager": "server/src/main/java/racing/server/core/InviteManager.java",
        "RoomManager": "server/src/main/java/racing/server/core/RoomManager.java",
        "Session": "server/src/main/java/racing/server/core/Session.java",
        "MatchService": "server/src/main/java/racing/server/core/MatchService.java",
        "Room": "server/src/main/java/racing/server/core/Room.java",
        "CarSim": "server/src/main/java/racing/server/core/CarSim.java",
    },
    "class_text": (
        "Biểu đồ lớp gồm 10 lớp chính, tô màu theo vai trò: xanh dương là lớp mạng và điểm vào, cam là dịch vụ phiên "
        "và lời mời, tím là phòng đua và trọng tài, xanh lá là ghi dữ liệu, xám là phiên người chơi. GameServer tạo một "
        "ServerServices dùng chung cho mọi ClientHandler. Phương thức trên hình đều có trong mã nguồn."
    ),
    "class_rows": [
        ["GameServer", "Mở cổng 5000, vòng accept, scheduler tick 50 ms và quét lời mời / thi đấu tiếp mỗi giây"],
        ["ClientHandler", "Một kết nối TCP: vòng đọc, hàng đợi gửi riêng, phân loại thông điệp theo trạng thái đăng nhập"],
        ["ServerServices", "Bó các dịch vụ lõi (phiên, lời mời, phòng) để mọi ClientHandler dùng chung"],
        ["SessionManager", "Đăng nhập không trùng, đăng xuất, danh sách online, phát ONLINE_LIST"],
        ["Session", "Một người chơi đã đăng nhập: mã, điểm, trạng thái, phòng hiện tại và kênh gửi"],
        ["InviteManager", "Lời mời 1–1, hẹn giờ 30 giây, kết quả OFFLINE / BUSY / REJECTED / TIMEOUT"],
        ["RoomManager", "Tạo và đóng phòng, tick toàn bộ phòng, định tuyến thông điệp trong trận"],
        ["Room", "Máy trạng thái WAITING – COUNTDOWN – RACING – AWAIT_REMATCH – CLOSED, luật thắng / hòa"],
        ["CarSim", "Trọng tài một xe: cắt tốc độ, tích phân quãng đường, va chạm và choáng"],
        ["MatchService", "Tạo bản ghi trận, chốt kết quả và nạp lại điểm, ghi diễn biến không chặn luồng tick"],
    ],
    "sequences": [
        {
            "title": "Mời thách đấu, chấp nhận và bắt đầu trận",
            "text": "Hình dưới mô tả một lời mời được chấp nhận. InviteManager chuyển lời mời, RoomManager tạo phòng, Room "
                    "bắt đầu ván và gửi MATCH_START cho từng người. Đếm ngược 3-2-1-GO do RoomManager lên lịch theo "
                    "từng giây.",
            "participants": [("A", "Người mời", "actor"), ("IM", "InviteManager", "object"),
                             ("B", "Người được mời", "actor"), ("RM", "RoomManager", "object"),
                             ("R", "Room", "object")],
            "messages": [
                ("A", "IM", "INVITE(bob)", "sync"),
                ("IM", "B", "INVITE_RECEIVED", "sync"),
                ("B", "IM", "INVITE_REPLY(accept)", "sync"),
                ("IM", "RM", "createRoom(a, b)", "sync"),
                ("RM", "R", "new Room; startRound()", "sync"),
                ("R", "A", "MATCH_START (góc nhìn A)", "return"),
                ("R", "B", "MATCH_START (góc nhìn B)", "return"),
                ("RM", "RM", "lên lịch đếm ngược", "self"),
                ("R", "A", "COUNTDOWN 3 → 2 → 1 → 0", "return"),
                ("R", "B", "COUNTDOWN 3 → 2 → 1 → 0", "return"),
            ],
            "gap": 1.35,
            "width": 16.0,
        },
        {
            "title": "Vòng trọng tài trong trận và kết thúc ván",
            "text": "Client gửi CAR_STATE khi đổi làn hoặc ga / phanh, server cập nhật CarSim. Mỗi tick 50 ms, Room tiến "
                    "hai xe và gửi RACE_UPDATE cho từng người. Khi có xe về đích, Room gọi MatchService.saveResult() để "
                    "chốt trận trong một transaction rồi gửi MATCH_RESULT.",
            "participants": [("A", "Người chơi", "actor"), ("CH", "ClientHandler", "object"),
                             ("R", "Room", "object"), ("CS", "CarSim", "object"),
                             ("MS", "MatchService", "object")],
            "messages": [
                ("A", "CH", "CAR_STATE(lane, speed)", "sync"),
                ("CH", "R", "onCarState(s, cs)", "sync"),
                ("R", "CS", "applyClient(cs, now)", "sync"),
                ("R", "CS", "advance(now, tick, obs)", "sync"),
                ("CS", "R", "Obstacle hoặc null", "return"),
                ("R", "A", "RACE_UPDATE", "return"),
                ("note", "R", "nếu có xe về đích: endRound()"),
                ("R", "MS", "saveResult(matchId, ...)", "sync"),
                ("R", "A", "MATCH_RESULT, REMATCH_ASK", "return"),
            ],
            "frames": [(3, 5, "mỗi 50 ms")],
            "gap": 1.35,
            "width": 16.0,
        },
    ],
    "er": {
        "entities": [
            ("players", [("player_id", "PK", "INT"), ("username", "", "VARCHAR(50)"),
                         ("password_hash", "", "VARCHAR(255)"), ("points", "", "INT"), ("wins", "", "INT"),
                         ("losses", "", "INT"), ("draws", "", "INT"), ("created_at", "", "DATETIME")],
             0.3, 5.0, "blue"),
            ("matches", [("match_id", "PK", "INT"), ("room_code", "", "VARCHAR(20)"),
                         ("player1_id", "FK", "INT"), ("player2_id", "FK", "INT"), ("winner_id", "FK", "INT"),
                         ("status", "", "ENUM"), ("end_reason", "", "ENUM"), ("started_at", "", "DATETIME"),
                         ("ended_at", "", "DATETIME")],
             4.2, 5.0, "orange"),
            ("match_events", [("event_id", "PK", "BIGINT"), ("match_id", "FK", "INT"), ("player_id", "FK", "INT"),
                              ("event_type", "", "VARCHAR(30)"), ("event_time", "", "DATETIME(3)"),
                              ("payload", "", "JSON")],
             4.2, 1.9, "green"),
        ],
        "relations": [
            ("players", "matches", "1", "N", "đấu", (0.0, 0.0)),
            ("matches", "match_events", "1", "N", "ghi nhận", (0.0, 0.0)),
            ("players", "match_events", "1", "N", "tạo sự kiện", (0.0, 0.0)),
        ],
        "W": 7.2,
        "H": 5.3,
        "width": 13.5,
    },
    "er_text": [
        "Server đọc và ghi ba bảng. Bảng players: đọc khi đăng nhập và khi nạp lại điểm sau trận (findById), ghi các "
        "cột points, wins, losses, draws khi chốt trận. Bảng matches: INSERT khi bắt đầu ván (room_code, player1_id, "
        "player2_id) và UPDATE khi kết thúc (winner_id, status, end_reason, ended_at).",
        "Bảng match_events chỉ được INSERT. Mỗi va chạm, thoát, mất kết nối, về đích hoặc thi đấu tiếp là một dòng, ghi "
        "qua một executor riêng để không làm chậm tick. Cột payload kiểu JSON lưu thông tin kèm theo như làn, quãng "
        "đường và thời gian. Khóa chính và event_time do CSDL tự sinh, không lấy từ client.",
    ],
    "screens": [],
    "results": [
        ["Khung server và luồng kết nối (S1)",
         "Đạt (SocketSmokeTest, ClientHandlerTest): đăng nhập, mời, thoát và bảng xếp hạng qua socket thật"],
        ["Quản lý phiên và danh sách online (S2)",
         "Đạt (SessionManagerTest): chống đăng nhập trùng, đăng xuất, danh sách online gửi tới mọi người"],
        ["Lời mời thách đấu (S3)",
         "Đạt (InviteManagerTest): OFFLINE, BUSY, REJECTED, TIMEOUT; không tự mời mình; người mời rớt mạng"],
        ["Phòng đua và trọng tài (S4)",
         "Đạt (RoomTest, CarSimTest): đếm ngược, cắt tốc độ, tích phân quãng đường, va chạm choáng 1,5 giây"],
        ["Kết thúc ván, lưu kết quả, thi đấu tiếp (S5)",
         "Đạt (RoomTest, MatchServiceTest): thắng, hòa cùng tick, thi đấu tiếp, ghi diễn biến không chặn"],
        ["Thoát trận và mất kết nối (S6)",
         "Đạt (RoomTest) về luật thoát và mất kết nối; thời gian 15 giây kiểm tra thủ công khi demo"],
        ["Nhiều phòng song song (S7)",
         "Đạt (RoomTest.twoRoomsDoNotMixUpdates); kịch bản T15 chạy thủ công khi demo"],
    ],
    "tests": [
        ["T2", "Đăng nhập cùng tài khoản ở 2 máy",
         "Máy sau bị từ chối với thông báo “Tài khoản đang đăng nhập ở nơi khác”", "Đạt (SessionManagerTest)"],
        ["T3", "3 client online, 1 client đăng xuất",
         "Hai client còn lại nhận danh sách online đã bớt người", "Đạt (SessionManagerTest)"],
        ["T4", "Mời người đang thi đấu", "Người mời nhận BUSY; người được mời không nhận lời mời",
         "Đạt (InviteManagerTest)"],
        ["T5", "Không trả lời lời mời 30 giây", "Người mời nhận TIMEOUT; cả hai vẫn Rảnh",
         "Đạt (InviteManagerTest)"],
        ["T6", "Từ chối lời mời", "Người mời nhận REJECTED; cả hai vẫn Rảnh", "Đạt (InviteManagerTest)"],
        ["T8", "Va chạm xe cộ", "Tốc độ về 0 trong 1,5 giây; va chạm được ghi vào diễn biến",
         "Đạt (RoomTest, CarSimTest)"],
        ["T9", "Về đích trước", "Người về đích thắng, điểm cộng đúng", "Đạt (RoomTest)"],
        ["T10", "Về đích cùng tick", "Hòa, mỗi người +1 điểm, không có người thắng",
         "Đạt (racing-tools; RoomTest)"],
        ["T11", "Bấm Thoát trận giữa chừng", "Đối thủ thắng với lý do QUIT, phòng đóng",
         "Đạt (racing-tools; RoomTest)"],
        ["T12", "Rút mạng client đang đua", "Đối thủ thắng với lý do DISCONNECT trong ≤ 15 giây",
         "Đạt (RoomTest); thời gian 15 giây chạy thủ công khi demo"],
        ["T13", "Cả hai đồng ý thi đấu tiếp", "Ván mới trong cùng phòng, xe cộ mới, đếm ngược lại",
         "Đạt (RoomTest)"],
        ["T14", "Một bên từ chối thi đấu tiếp", "Phòng đóng, cả hai về sảnh", "Đạt (RoomTest)"],
        ["T15", "2 phòng đua cùng lúc", "Không lẫn RACE_UPDATE giữa hai phòng", "Đạt (RoomTest)"],
        ["T17", "Gửi CAR_STATE tốc độ 999", "Server cắt tốc độ về mức tối đa, không về đích nhanh bất thường",
         "Đạt (racing-tools)"],
    ],
    "unit_tests": [
        ("server/src/test/java/racing/server/core/RoomTest.java",
         "Đếm ngược, về đích, hòa cùng tick, thoát, mất kết nối, thi đấu tiếp đồng ý / từ chối / hết hạn, hai phòng song song"),
        ("server/src/test/java/racing/server/core/InviteManagerTest.java",
         "OFFLINE, BUSY, REJECTED, TIMEOUT bằng đồng hồ giả; tự mời mình; người mời hoặc người được mời rớt mạng"),
        ("server/src/test/java/racing/server/core/SessionManagerTest.java",
         "Sai mật khẩu, chống đăng nhập trùng, đăng xuất, danh sách online gửi tới mọi người"),
        ("server/src/test/java/racing/server/core/CarSimTest.java",
         "Cắt tốc độ, quãng đường do server tích phân, va chạm với xe cộ đang chạy, choáng 1,5 giây"),
        ("server/src/test/java/racing/server/core/MatchServiceTest.java",
         "Ghi diễn biến không chặn luồng gọi và giữ đúng thứ tự"),
        ("server/src/test/java/racing/server/net/ClientHandlerTest.java",
         "send() không chặn khi client ngừng đọc"),
        ("server/src/test/java/racing/server/core/SocketSmokeTest.java",
         "Đăng nhập, mời, thoát và bảng xếp hạng qua socket thật"),
        ("server/src/test/java/racing/server/ServerIntegrationTest.java",
         "Trọn vòng đăng nhập – mời – đua qua socket với MySQL (chạy trên CI có dịch vụ MySQL)"),
    ],
    "conclusion": [
        "Về kết quả, em hoàn thành đủ bảy task S1–S7 của phần server. Server phục vụ nhiều phòng đua song song mà không "
        "lẫn dữ liệu giữa các phòng. Luật thắng, hòa, thoát và mất kết nối đều do server quyết định, nên sửa client "
        "không làm thay đổi kết quả trận. Một số lỗi phát hiện khi kiểm thử đã được sửa: ONLINE_LIST bị bỏ qua lúc "
        "đăng nhập, và hàng đợi gửi chặn luồng tick khi một client không đọc.",
        "Qua phần việc này, em nắm được cách lập trình TCP Socket với ServerSocket và luồng đối tượng, đa luồng với "
        "ExecutorService, ScheduledExecutorService và BlockingQueue, đồng bộ hóa bằng synchronized và ConcurrentHashMap. "
        "Em cũng học cách tách lõi nghiệp vụ khỏi socket để kiểm thử bằng đồng hồ giả.",
        "Hạn chế: server dùng I/O chặn với hai luồng cho mỗi client nên chưa mở rộng tốt tới hàng trăm kết nối. Giao "
        "thức chưa mã hóa TLS. Luật hòa chỉ xảy ra khi hai xe về đích cùng một tick, nên khó tái hiện bằng hai người "
        "thật, em phải dùng racing-tools để kiểm tra.",
        "Hướng phát triển: chuyển sang NIO hoặc dùng số luồng cố định để giảm chi phí mỗi kết nối, lưu lịch sử diễn biến "
        "để xem lại trận trên giao diện, và mã hóa kênh truyền khi triển khai ngoài mạng LAN.",
    ],
}
