"""Báo cáo cá nhân: Nguyễn Trần Mai Anh – Dữ liệu và SQL (M1–M6)."""
import shared

SRV = "server/src/main/java/racing/server/"
CL = "client/src/main/java/racing/client/"

CONTENT = {
    "name": "Nguyễn Trần Mai Anh",
    "slug": "NguyenTranMaiAnh",
    "part_intro": [
        "Trong nhóm, em phụ trách phần dữ liệu và SQL: thiết kế cơ sở dữ liệu MySQL gồm ba bảng players, matches và "
        "match_events, và hiện thực các thao tác đọc / ghi đi từ màn hình client qua server tới CSDL. Phần việc gồm "
        "đăng ký tài khoản, bảng xếp hạng, lịch sử trận, ghi diễn biến trận và kiểm tra dữ liệu khi kiểm thử tích hợp.",
        "Mỗi chức năng là một lát cắt đầy đủ theo kiến trúc của hệ thống: câu lệnh SQL nằm trong lớp DAO, một nhánh xử "
        "lý trong AccountService hoặc MatchService phía server, và một màn hình trên client (RegisterDialog, "
        "LeaderboardFrame, HistoryFrame). Client không kết nối trực tiếp tới MySQL; mọi yêu cầu đi qua server bằng "
        "thông điệp của giao thức chung.",
    ],
    "tasks": [
        ["M1", "Thiết kế lược đồ, nạp schema.sql và seed.sql, viết docs/DB.md mô tả ba bảng, từng cột và các truy vấn "
               "hay dùng", "db/schema.sql, db/seed.sql, docs/DB.md"],
        ["M2", "Bảng xếp hạng: server trả LEADERBOARD khi nhận LEADERBOARD_REQ; client hiển thị Hạng, Tên, Điểm, "
               "Thắng, Thua, Hòa", "PlayerDAO.getLeaderboard, ClientHandler, LeaderboardFrame"],
        ["M3", "Đăng ký tài khoản: REGISTER → REGISTER_RESULT, kiểm tra hai phía, báo lỗi trùng tên, băm mật khẩu có "
               "muối", "AccountService.register, PlayerDAO.create, PasswordHasher, RegisterDialog"],
        ["M4", "Lịch sử trận: JOIN hai lần bảng players lấy tên đối thủ, 20 trận gần nhất; HistoryFrame hiển thị Thời "
               "gian, Đối thủ, Kết quả, Lý do", "MatchDAO.findRecentByPlayer, HistoryTableModel, HistoryFrame"],
        ["M5", "Ghi diễn biến trận START, COLLISION, FINISH, QUIT, DISCONNECT, REMATCH vào match_events kèm payload "
               "JSON", "MatchService.event, MatchDAO.addEvent, Room"],
        ["M6", "Kiểm tra dữ liệu khi kiểm thử tích hợp: reset-db.sql trước mỗi buổi, câu SQL xác nhận sau T9–T13 và "
               "T18–T19", "db/reset-db.sql, docs/TEST-PLAN.md"],
    ],
    "part_analysis": [
        "Từ đề bài, phần dữ liệu có ba yêu cầu chính: tài khoản phải được lưu bền vững và mật khẩu không được lưu dạng "
        "rõ (YC01, YC02); điểm và lịch sử phải được cộng và lưu đúng sau mỗi trận, kể cả khi trận kết thúc vì thoát "
        "hoặc mất kết nối (YC07, YC09); người chơi xem được bảng xếp hạng và lịch sử của mình (YC09).",
        "Từ các yêu cầu đó, em chọn ba hướng xử lý. Thứ nhất, mọi truy cập MySQL nằm trong lớp DAO, dùng "
        "PreparedStatement, và chỉ server được kết nối CSDL. Thứ hai, việc chốt trận gồm nhiều câu UPDATE nên phải "
        "nằm trong một transaction để không có trạng thái nửa vời. Thứ ba, ghi sự kiện không được làm chậm vòng lặp "
        "đua 50 ms, nên các câu INSERT vào match_events được chuyển sang một luồng ghi riêng.",
        "Phần này ghép với các thành viên khác ở hai điểm: các case LEADERBOARD_REQ, REGISTER và MATCH_HISTORY_REQ "
        "nằm trong ClientHandler; LobbyFrame mở LeaderboardFrame và HistoryFrame, còn LoginFrame mở RegisterDialog "
        "(phần của Vũ Quốc Hiếu).",
    ],
    "part_reqs": [
        ["M-1", "Lược đồ có ba bảng, khóa chính, khóa ngoại và chỉ mục phục vụ bảng xếp hạng và lịch sử trận", "YC09"],
        ["M-2", "Bảng xếp hạng sắp theo điểm, cùng điểm xét số trận thắng, đánh hạng liên tục từ 1", "YC09"],
        ["M-3", "Tài khoản mới đăng nhập được; tên trùng bị từ chối; mật khẩu lưu dạng băm có muối", "YC02"],
        ["M-4", "Lịch sử hiển thị đúng đối thủ, kết quả và lý do kết thúc của 20 trận gần nhất", "YC09"],
        ["M-5", "Mỗi sự kiện trong trận được ghi đúng loại, đúng thời điểm, không làm chậm vòng lặp đua", "YC06"],
        ["M-6", "Điểm và thống kê của hai người chơi được cập nhật trong một transaction, lỗi thì rollback", "YC09"],
    ],
    "uc": {
        "title": "Chức năng dữ liệu: đăng ký, xếp hạng, lịch sử",
        "actors": [("P", "Người chơi", "left", 1.0), ("S", "Máy chủ (Server)", "right", 3.5)],
        "cases": [
            ("dk", "Đăng ký tài khoản", 0, 0), ("bxh", "Xem bảng xếp hạng", 0, 1),
            ("ls", "Xem lịch sử trận", 0, 2),
            ("bm", "Băm mật khẩu có muối", 1, 0), ("sx", "Sắp xếp theo điểm, thắng", 1, 1),
            ("lh", "Lấy 20 trận gần nhất", 1, 2),
            ("kq", "Lưu kết quả trận (transaction)", 1, 3), ("ev", "Ghi diễn biến trận", 1, 4),
        ],
        "links": [("P", "dk", "assoc"), ("P", "bxh", "assoc"), ("P", "ls", "assoc"),
                  ("dk", "bm", "include"), ("bxh", "sx", "include"), ("ls", "lh", "include"),
                  ("S", "kq", "assoc"), ("S", "ev", "assoc")],
    },
    "uc_text": (
        "Người chơi là tác nhân trực tiếp dùng các chức năng dữ liệu: đăng ký tài khoản (trước khi đăng nhập), xem "
        "bảng xếp hạng và xem lịch sử trận. Hai ca “Lưu kết quả trận” và “Ghi diễn biến trận” do máy chủ thực hiện "
        "khi trận đang diễn ra hoặc vừa kết thúc, nên gắn với tác nhân Máy chủ. Ba ca “Băm mật khẩu”, “Sắp xếp theo "
        "điểm, thắng” và “Lấy 20 trận gần nhất” là bước bắt buộc (include) của ca tương ứng."),
    "uc_specs": [
        {
            "name": "Đăng ký tài khoản",
            "actor": "Người chơi (chưa đăng nhập)",
            "pre": "Đang ở màn hình đăng nhập và máy đã kết nối tới server.",
            "main": [
                "Người chơi bấm “Đăng ký”, nhập tên tài khoản, mật khẩu và nhập lại mật khẩu.",
                "RegisterDialog kiểm tra cục bộ: tên 3–50 ký tự chữ, số hoặc gạch dưới; mật khẩu ít nhất 4 ký tự; hai "
                "lần nhập khớp nhau.",
                "Client gửi REGISTER; AccountService kiểm tra lại độ dài và ký tự, rồi PasswordHasher băm mật khẩu.",
                "PlayerDAO thêm một dòng vào players; cột username là UNIQUE.",
                "Server trả REGISTER_RESULT; hộp thoại đóng và LoginFrame điền sẵn tên vừa tạo.",
            ],
            "alt": [
                "A1. Tên đã tồn tại: MySQL ném SQLIntegrityConstraintViolationException, server trả “Tên tài khoản đã "
                "tồn tại”; hộp thoại vẫn mở để người chơi đổi tên.",
                "A2. Dữ liệu sai ở phía client: báo lỗi ngay trên form, không gửi yêu cầu. Sai ở phía server: báo lỗi "
                "tương ứng, không ghi vào CSDL.",
                "A3. Lỗi CSDL khác: server trả “Lỗi máy chủ khi tạo tài khoản” và ghi log cảnh báo.",
            ],
            "post": "Có một dòng mới trong players với password_hash dạng sha256$<salt>$<hash>.",
        },
        {
            "name": "Xem lịch sử trận",
            "actor": "Người chơi đã đăng nhập",
            "pre": "Đã đăng nhập; người chơi bấm “Lịch sử” trên sảnh chờ.",
            "main": [
                "LobbyFrame mở HistoryFrame, HistoryFrame gửi MATCH_HISTORY_REQ.",
                "ClientHandler gọi AccountService.history(session) với playerId của phiên đăng nhập.",
                "MatchDAO.findRecentByPlayer nối bảng matches với players hai lần, lọc các trận có status khác PLAYING, "
                "sắp mới nhất trước, giới hạn 20 dòng.",
                "Server trả MATCH_HISTORY; HistoryTableModel hiển thị Thời gian, Đối thủ, Kết quả (Thắng, Thua, Hòa, "
                "Hủy) và Lý do.",
                "HistoryFrame hiển thị dòng tổng kết số trận thắng, thua và hòa.",
            ],
            "alt": [
                "A1. Chưa có trận nào đã kết thúc: bảng rỗng, không báo lỗi.",
                "A2. Lỗi CSDL: server trả ERROR “Lỗi máy chủ khi lấy lịch sử trận”; client không bị ngắt kết nối.",
            ],
            "post": "Không thay đổi dữ liệu.",
        },
    ],
    "classes": {
        "classes": [
            ("RegisterDialog", None, ["- net: NetworkClient", "- registeredUsername: String"],
             ["+ validate(user, pass): String"], 0, 0, "blue"),
            ("LeaderboardFrame", None, ["- model: LeaderboardTableModel"], ["- request(): void"], 0, 1, "blue"),
            ("HistoryFrame", None, ["- net: NetworkClient"], ["- request(): void", "- showSummary(rows): void"],
             0, 2, "blue"),
            ("AccountService", None, ["- players: PlayerRepository", "- matches: MatchRepository"],
             ["+ register(req): LoginResult", "+ leaderboard(): List<RankRow>", "+ history(s): List<MatchRow>"],
             1, 0, "orange"),
            ("PlayerRepository", "interface", [],
             ["+ login(user, pw): Optional", "+ create(user, pw): int", "+ leaderboard(): List<RankRow>"],
             1, 1, "orange"),
            ("MatchDAO", None, [],
             ["+ saveResult(...): void", "+ findRecentByPlayer(id, n)", "+ addEvent(...): void"], 1, 2, "green"),
            ("JdbcPlayerRepository", None, ["- dao: PlayerDAO"],
             ["+ login(user, pw)", "+ create(user, pw): int", "+ leaderboard(): List<RankRow>"], 2, 0, "green"),
            ("PlayerDAO", None, [],
             ["+ login(user, pw): Optional", "+ create(user, pw): int", "+ getLeaderboard(): List<RankRow>"],
             2, 1, "green"),
            ("PasswordHasher", None, [], ["+ hash(pw): String", "+ verify(pw, stored): boolean"], 2, 2, "green"),
        ],
        "relations": [
            ("AccountService", "PlayerRepository", "dep", ""),
            ("JdbcPlayerRepository", "PlayerRepository", "impl", ""),
            ("JdbcPlayerRepository", "PlayerDAO", "comp", ""),
            ("PlayerDAO", "PasswordHasher", "dep", ""),
        ],
        "col_w": 2.9,
        "pt": 7.8,
    },
    "class_text": (
        "Biểu đồ lớp gồm ba tầng em phụ trách. Tầng màn hình client (RegisterDialog, LeaderboardFrame, HistoryFrame) "
        "chỉ gửi thông điệp và nhận kết quả. Tầng nghiệp vụ AccountService làm việc qua giao diện PlayerRepository, "
        "nên có thể kiểm thử bằng bản trong bộ nhớ. Tầng truy cập dữ liệu gồm JdbcPlayerRepository, PlayerDAO, "
        "MatchDAO và PasswordHasher. Các DTO RankRow và MatchRow là record dùng chung trong module common, xuất hiện "
        "làm kiểu trả về của các phương thức."),
    "class_rows": [
        ["RegisterDialog", "Hộp thoại đăng ký: kiểm tra dữ liệu cục bộ, gửi REGISTER, chờ REGISTER_RESULT"],
        ["LeaderboardFrame", "Bảng xếp hạng: gửi LEADERBOARD_REQ, nạp dữ liệu vào bảng và bục top 3"],
        ["HistoryFrame", "Lịch sử trận: gửi MATCH_HISTORY_REQ, hiển thị bảng và dòng tổng kết"],
        ["AccountService", "Nghiệp vụ đăng ký (kiểm tra phía server), bảng xếp hạng, lịch sử trận"],
        ["PlayerRepository", "Giao diện truy cập người chơi, tách nghiệp vụ khỏi JDBC"],
        ["JdbcPlayerRepository", "Cài đặt PlayerRepository bằng PlayerDAO"],
        ["PlayerDAO", "Truy vấn bảng players: đăng nhập, tạo tài khoản, bảng xếp hạng"],
        ["MatchDAO", "Truy vấn matches và match_events: chốt kết quả trong transaction, lịch sử, ghi sự kiện"],
        ["PasswordHasher", "Băm mật khẩu SHA-256 có muối, so sánh bằng thời gian không đổi"],
    ],
    "class_files": {
        "RegisterDialog": CL + "ui/RegisterDialog.java",
        "LeaderboardFrame": CL + "ui/LeaderboardFrame.java",
        "HistoryFrame": CL + "ui/HistoryFrame.java",
        "AccountService": SRV + "core/AccountService.java",
        "PlayerRepository": SRV + "core/PlayerRepository.java",
        "MatchDAO": SRV + "db/MatchDAO.java",
        "JdbcPlayerRepository": SRV + "db/JdbcPlayerRepository.java",
        "PlayerDAO": SRV + "db/PlayerDAO.java",
        "PasswordHasher": SRV + "db/PasswordHasher.java",
    },
    "sequences": [
        {
            "title": "Tuần tự đăng ký tài khoản (REGISTER → REGISTER_RESULT)",
            "text": (
                "Hình dưới đây thể hiện đường đi của một yêu cầu đăng ký. Thông điệp đi qua NetworkClient và "
                "ClientHandler, nhưng hình chỉ vẽ các đối tượng có logic: sau khi server ghi dòng mới, kết quả "
                "trả ngược về đúng hộp thoại đã gửi yêu cầu."),
            "participants": [("RD", "RegisterDialog", "object"), ("CH", "ClientHandler", "object"),
                             ("AS", "AccountService", "object"), ("PD", "PlayerDAO", "object"),
                             ("DB", "MySQL", "object")],
            "messages": [
                ("RD", "CH", "REGISTER(user, pass)", "sync"),
                ("CH", "AS", "register(req)", "sync"),
                ("AS", "PD", "create(user, pass)", "sync"),
                ("PD", "DB", "INSERT INTO players", "sync"),
                ("DB", "PD", "id, hoặc lỗi trùng UNIQUE", "return"),
                ("PD", "AS", "id hoặc exception", "return"),
                ("AS", "CH", "LoginResult(ok, msg)", "return"),
                ("CH", "RD", "REGISTER_RESULT", "return"),
                ("note", "RD", "Đóng hộp thoại"),
            ],
            "gap": 1.55,
        },
        {
            "title": "Tuần tự chốt kết quả trận trong một transaction",
            "text": (
                "Khi một ván kết thúc, Room gọi MatchService.saveResult. MatchDAO thực hiện các câu UPDATE trong cùng "
                "một transaction: nếu một câu lỗi thì rollback, nên không có trường hợp trận đã ghi người thắng mà điểm "
                "chưa được cộng. Sau khi commit, MatchService nạp lại điểm mới vào hai phiên đăng nhập."),
            "participants": [("RM", "Room", "object"), ("MS", "MatchService", "object"),
                             ("MD", "MatchDAO", "object"), ("DB", "MySQL", "object")],
            "messages": [
                ("RM", "MS", "saveResult(...)", "sync"),
                ("MS", "MD", "saveResult(...)", "sync"),
                ("MD", "DB", "UPDATE matches", "sync"),
                ("MD", "DB", "UPDATE players (x2)", "sync"),
                ("MD", "DB", "COMMIT", "sync"),
                ("DB", "MD", "OK", "return"),
                ("MD", "MS", "void", "return"),
                ("MS", "MS", "refresh(a), refresh(b)", "self"),
            ],
            "frames": [(2, 4, "transaction")],
            "gap": 1.75,
        },
    ],
    "er": {
        "entities": [
            ("players", [("player_id", "PK", "INT"), ("username", "", "VARCHAR(50)"),
                         ("password_hash", "", "VARCHAR(255)"), ("points", "", "INT"), ("wins", "", "INT"),
                         ("losses", "", "INT"), ("draws", "", "INT"), ("created_at", "", "DATETIME")],
             0.25, 4.35, "blue"),
            ("matches", [("match_id", "PK", "INT"), ("room_code", "", "VARCHAR(20)"),
                         ("player1_id", "FK", "INT"), ("player2_id", "FK", "INT"), ("winner_id", "FK", "INT"),
                         ("status", "", "ENUM"), ("end_reason", "", "ENUM"), ("started_at", "", "DATETIME"),
                         ("ended_at", "", "DATETIME")],
             4.1, 4.35, "orange"),
            ("match_events", [("event_id", "PK", "BIGINT"), ("match_id", "FK", "INT"),
                              ("player_id", "FK", "INT"), ("event_type", "", "VARCHAR(30)"),
                              ("event_time", "", "DATETIME(3)"), ("payload", "", "JSON")],
             2.2, 1.55, "green"),
        ],
        "relations": [
            ("players", "matches", "1", "0..*", "tham gia", (0, 0.3)),
            ("players", "match_events", "1", "0..*", "thực hiện", (-0.5, 0)),
            ("matches", "match_events", "1", "0..*", "gồm", (0.8, 0)),
        ],
        "W": 7.0,
        "H": 4.75,
    },
    "er_text": [
        "Khóa chính là player_id, match_id và event_id. Ba cột matches.player1_id, player2_id và winner_id đều là "
        "khóa ngoại trỏ tới players, nên quan hệ “tham gia” trên sơ đồ gộp cả ba vai trò; winner_id có thể NULL khi "
        "hòa hoặc hủy. Mỗi người chơi tham gia 0 hoặc nhiều trận, mỗi trận có đúng hai người chơi, và mỗi sự kiện "
        "thuộc đúng một trận và một người chơi.",
        "Chỉ mục: idx_rank (points DESC, wins DESC) trên players phục vụ bảng xếp hạng; idx_matches_p1 và "
        "idx_matches_p2 tìm nhanh các trận của một người ở vai trò người chơi 1 hoặc 2; idx_events_match (match_id, "
        "event_time) phục vụ xem lại diễn biến theo thời gian. Ràng buộc UNIQUE trên username là cơ sở để phát hiện "
        "trùng tên khi đăng ký.",
        "Bảng xếp hạng dùng SELECT ... ORDER BY points DESC, wins DESC, username ASC LIMIT 100. Hạng được đánh số khi "
        "duyệt ResultSet, nên các hạng liên tiếp và không có khoảng trống.",
        "Mật khẩu lưu dạng sha256$<salt hex>$<hash hex>, với salt 16 byte ngẫu nhiên và hash = SHA-256(salt ‖ mật "
        "khẩu). Cách này che được mật khẩu rõ, nhưng SHA-256 không phải hàm băm chậm nên đây là một hạn chế được nêu "
        "ở phần kết luận.",
    ],
    "screens": [
        (shared.SHOTS / "register.png", "Hộp thoại đăng ký tài khoản (RegisterDialog)", 8.0),
        (shared.SHOTS / "leaderboard-loading.png",
         "Bảng xếp hạng khi đang tải dữ liệu từ server (LeaderboardFrame)", 12.0),
    ],
    "results": [
        ["Lược đồ và CSDL (M1)", "Ba bảng với khóa ngoại, chỉ mục, mã utf8mb4; schema.sql và seed.sql nạp được; "
                                 "docs/DB.md đầy đủ."],
        ["Bảng xếp hạng (M2)", "Sắp theo điểm, cùng điểm xét số trận thắng; bục top 3 và bảng sáu cột."],
        ["Đăng ký (M3)", "Kiểm tra ở cả hai phía; trùng tên báo lỗi; mật khẩu lưu dạng băm có muối."],
        ["Lịch sử trận (M4)", "20 trận gần nhất với tên đối thủ, kết quả và lý do kết thúc."],
        ["Ghi diễn biến (M5)", "Ghi START, COLLISION, FINISH, QUIT, DISCONNECT, REMATCH kèm JSON trên luồng riêng."],
        ["Dữ liệu kiểm thử (M6)", "reset-db.sql dọn dữ liệu; câu SQL xác nhận cho T9–T13 và T18–T19 trong "
                                  "docs/TEST-PLAN.md."],
    ],
    "tests": [
        ["T9", "Về đích trước", "Người về trước +1 điểm, +1 thắng; end_reason = FINISH", "Chạy thủ công khi demo"],
        ["T10", "Về đích cùng tick", "Mỗi người +1 điểm, +1 hòa; winner_id NULL; end_reason = DRAW",
         "Chạy thủ công khi demo"],
        ["T11", "Thoát trận giữa chừng", "Người thoát +1 thua; end_reason = QUIT; có sự kiện QUIT",
         "Chạy thủ công khi demo"],
        ["T12", "Rút mạng khi đang đua", "end_reason = DISCONNECT; có sự kiện DISCONNECT",
         "Chạy thủ công khi demo"],
        ["T13", "Thi đấu tiếp", "Hai dòng matches cùng room_code, mỗi dòng có sự kiện START",
         "Chạy thủ công khi demo"],
        ["T18", "Đăng ký rồi đăng nhập; đăng ký trùng tên", "Tài khoản mới vào được sảnh; trùng tên báo lỗi rõ",
         "Chạy thủ công khi demo"],
        ["T19", "Lịch sử sau 3 trận", "Đúng 3 dòng, đúng đối thủ và kết quả", "Chạy thủ công khi demo"],
    ],
    "unit_tests": [
        ("server/src/test/java/racing/server/db/DaoIntegrationTest.java",
         "Chạy với MySQL thật (CI): đăng nhập đúng và sai; thắng, hòa, thoát / mất kết nối, hủy trận cộng điểm đúng; "
         "người thắng không hợp lệ bị từ chối; bảng xếp hạng đúng thứ tự; lưu JSON của sự kiện"),
        ("server/src/test/java/racing/server/db/PasswordHasherTest.java",
         "Băm có muối nên hai lần băm cùng mật khẩu khác nhau; xác thực đúng và sai; chuỗi lưu hỏng bị từ chối; "
         "hash mẫu khớp giá trị sinh bằng Python"),
        ("server/src/test/java/racing/server/core/MatchServiceTest.java",
         "Ghi sự kiện chậm không chặn bên gọi và giữ đúng thứ tự"),
    ],
    "conclusion": [
        "Về kết quả, em hoàn thành đủ sáu task M1–M6. Ba chức năng đăng ký, bảng xếp hạng và lịch sử trận chạy đầu cuối "
        "từ client qua server tới MySQL. Việc chốt kết quả và ghi diễn biến được thực hiện trong transaction hoặc "
        "luồng ghi riêng, không làm chậm vòng lặp đua. Các quy tắc cộng điểm được kiểm thử tự động bằng "
        "DaoIntegrationTest trên CI; các kịch bản T9–T19 được kiểm tra thủ công khi demo.",
        "Qua phần việc này, em nắm được thiết kế bảng với khóa ngoại và chỉ mục theo truy vấn thực tế, JDBC với "
        "PreparedStatement, transaction với commit và rollback, băm mật khẩu có muối, và cách một yêu cầu đi qua giao "
        "thức tự định nghĩa trên TCP để tới cơ sở dữ liệu.",
        "Hạn chế: lịch sử chỉ lấy 20 trận gần nhất, chưa phân trang; SHA-256 có muối nhanh hơn nhiều so với các hàm "
        "băm chậm (bcrypt, PBKDF2, Argon2), nên chưa đủ mạnh nếu bảng players bị lộ; các kịch bản T9–T19 chưa tự động "
        "hóa đầu cuối; match_events có thể được ghi chậm hơn trạng thái trận một khoảng ngắn.",
        "Hướng phát triển: chuyển sang hàm băm chậm có tham số, thêm phân trang lịch sử, và viết kịch bản tích hợp tự "
        "động cho T9–T13 để không phải kiểm tra SQL bằng tay.",
    ],
}

