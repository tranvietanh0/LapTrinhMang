"""Báo cáo cá nhân: Trần Việt Anh – Nền tảng và màn hình đua (D1–D7, R1–R6)."""
import shared

CM = "common/src/main/java/racing/common/"
SRV = "server/src/main/java/racing/server/"
CL = "client/src/main/java/racing/client/"
RACE = CL + "ui/race/"

CONTENT = {
    "name": "Trần Việt Anh",
    "slug": "TranVietAnh",
    "part_intro": [
        "Trong nhóm, em phụ trách phần nền mà cả nhóm cùng dựa vào (dự án Maven nhiều module, hợp đồng thông điệp "
        "dùng chung, lược đồ MySQL, lớp truy cập dữ liệu, CI) và màn hình đua phía client: vẽ đường đua, điều khiển "
        "xe, đồng bộ với server, hiển thị kết quả và thi đấu tiếp. Em cũng làm công cụ kiểm thử giao thức, dẫn dắt "
        "kiểm thử tích hợp và đóng gói bản chạy demo.",
        "Phần nền được làm xong sớm (tuần đầu) để Vũ Quốc Hiếu và các thành viên khác có hợp đồng chung và DAO để gọi "
        "ngay. Ngoài kế hoạch ban đầu, em làm thêm giao diện phong cách game đua phố, hiệu ứng chuyển động và âm thanh "
        "sinh bằng code.",
    ],
    "tasks": [
        ["D1", "Dự án Maven nhiều module common / server / client / tools, Maven Wrapper, JDK 17, CI GitHub Actions "
               "có dịch vụ MySQL", "pom.xml, .github/workflows/build.yml"],
        ["D2", "Hợp đồng chung: Message, MessageType (22 loại), các DTO dạng record, GameConfig; test tuần tự hóa",
         "common/"],
        ["D3–D4", "Lược đồ MySQL và dữ liệu mẫu; PlayerDAO, MatchDAO (saveResult chốt trận trong transaction)",
         "db/, server/db/"],
        ["D5", "Hỗ trợ nối saveResult vào MatchService; chạy thử và review PR dữ liệu của Nguyễn Trần Mai Anh",
         "server/core/MatchService"],
        ["D6", "Kiểm thử tích hợp theo docs/TEST-PLAN.md; công cụ dòng lệnh racing-tools cho T10, T17",
         "tools/, docs/TEST-PLAN.md"],
        ["D7", "Đóng gói jar (build-dist), README, kịch bản demo", "scripts/, README.md"],
        ["R1", "RacePanel, TrackView, PixelArt: chia đôi màn hình, camera bám xe, 3 làn, cảnh quan, HUD, minimap",
         "ui/race/"],
        ["R2", "CarModel và điều khiển: giữ W tăng tốc, giữ S phanh, A / D đổi làn, nhả ga tự giảm tốc",
         "model/CarModel, RaceFrame"],
        ["R3", "Nối mạng: gửi CAR_STATE 20 lần / giây, đếm ngược theo COUNTDOWN, khóa phím trước GO",
         "RaceFrame"],
        ["R4", "Nhận RACE_UPDATE: đồng bộ xe mình và xe đối thủ, làm mượt tick bằng RaceClock, vụ nổ khi va chạm",
         "RaceFrame, CarModel, RaceClock, TrackView"],
        ["R5", "ResultDialog, RematchDialog, thoát trận có xác nhận (Esc)", "ui/race/"],
        ["R6", "Hiệu năng: đệm sprite dựng sẵn, chỉ vẽ vật trong khung nhìn, đồng hồ vẽ cố định nhịp",
         "ui/race/"],
    ],
    "part_analysis": [
        "Phần nền phải ổn định trước vì mọi thành viên khác phụ thuộc vào nó: nếu Message hay lược đồ thay đổi giữa "
        "chừng, cả ba module phải sửa cùng lúc. Vì vậy em đóng băng hợp đồng sau tuần 1, mọi thay đổi đi kèm test "
        "tuần tự hóa và được gộp trong một PR. CI chạy mvnw verify trên MySQL thật nên test DAO và test tích hợp "
        "server chạy trên mỗi PR.",
        "Màn hình đua phải thỏa hai ràng buộc: hình ảnh mượt trên máy client và vị trí xe khớp với server. Client mô "
        "phỏng cục bộ để phản hồi tức thì, nhưng server là trọng tài. Với xe đối thủ, client ghi đè theo RACE_UPDATE. "
        "Với xe của mình, client giữ làn và tốc độ theo phím vì RACE_UPDATE có thể được tính trước khi server nhận "
        "CAR_STATE mới nhất. Em phối hợp với Vũ Quốc Hiếu để dùng NetworkClient và với Phạm Thị Thu Phương để thống "
        "nhất luật hòa cùng tick.",
    ],
    "part_reqs": [
        ["V1", "Dự án Maven nhiều module, build bằng Maven Wrapper trên Windows, chạy được với JDK 17", "YC12"],
        ["V2", "Hợp đồng Message / MessageType / DTO / GameConfig dùng chung cho client, server, tools", "YC05"],
        ["V3", "Lưu kết quả trận (điểm, thắng / thua / hòa, diễn biến) trong một transaction, lỗi thì rollback",
         "YC09"],
        ["V4", "Vẽ đường đua hai nửa màn hình, vị trí xe cộ tính theo tick đúng công thức server", "YC05"],
        ["V5", "Điều khiển xe mượt bằng phím giữ (ga, phanh) và phím đổi làn; CAR_STATE đúng 20 lần / giây", "YC05"],
        ["V6", "Server quyết định kết quả: client không tự cộng quãng đường hay về đích; RACE_UPDATE ghi đè trạng thái",
         "YC06"],
        ["V7", "Đếm ngược 3 – 2 – 1 – GO, khóa phím trước GO; hỏi và trả lời thi đấu tiếp", "YC05, YC08"],
        ["V8", "Giao diện không đơ khi nhận mạng: xử lý thông điệp trên luồng Swing, đồng hồ hình ảnh làm mượt jitter",
         "YC11"],
        ["V9", "Thoát trận có xác nhận (Esc hoặc nút); đóng cửa sổ đua bị hỏi xác nhận trước", "YC07"],
        ["V10", "Thông điệp RACE_UPDATE mang roomId; màn hình chỉ xử lý đúng phòng của mình", "YC10"],
    ],
    "uc": {
        "title": "Ca thi đấu: điều khiển xe và xem kết quả",
        "actors": [("P", "Người chơi", "left", 1.5)],
        "cases": [
            ("dd", "Xem đếm ngược 3 – 2 – 1", 0, 0), ("dk", "Điều khiển xe trong trận", 0, 1),
            ("hud", "Xem HUD và minimap", 0, 2), ("kq", "Xem kết quả trận", 0, 3),
            ("dl", "Đổi làn (A / D)", 1, 0), ("ga", "Tăng tốc, phanh (W / S)", 1, 1),
            ("thoat", "Thoát trận (Esc)", 1, 2), ("tt", "Thi đấu tiếp", 1, 3),
        ],
        "links": [("P", c, "assoc") for c in ("dd", "dk", "hud", "kq")]
        + [("dk", "dl", "include"), ("dk", "ga", "include"), ("thoat", "dk", "extend"), ("tt", "kq", "extend")],
        "cols": 2, "col_w": 2.9, "row_h": 0.78, "width": 12.5,
    },
    "uc_text": (
        "Biểu đồ chi tiết hóa các ca sử dụng của người chơi trong trận đua. Ca “Điều khiển xe trong trận” là ca gốc; "
        "đổi làn và tăng tốc / phanh được bao gồm (include) trong ca này. Thoát trận và thi đấu tiếp là hai nhánh mở "
        "rộng (extend): người chơi có thể thoát bất kỳ lúc nào trong trận, còn thi đấu tiếp chỉ xuất hiện sau khi xem "
        "kết quả. Các ca được đặc tả chi tiết bên dưới."),
    "uc_specs": [
        {"name": "Điều khiển xe trong trận", "actor": "Người chơi",
         "pre": "Trận đã vào giai đoạn đua (client đã nhận GO từ server).",
         "main": [
             "Người chơi giữ W (hoặc ↑) để tăng tốc, hoặc giữ S (hoặc ↓) để phanh.",
             "Client cập nhật tốc độ và quãng đường cục bộ, đồng thời gửi CAR_STATE cho server mỗi 50 ms.",
             "Người chơi nhấn A / D (hoặc ←, →) để đổi làn; xe không đổi làn ra ngoài 3 làn của đường đua.",
             "Server nhận CAR_STATE, cập nhật làn và tốc độ (cắt ngưỡng 360 km/h), tiến xe một tick và gửi RACE_UPDATE "
             "cho hai người chơi.",
             "Client áp dụng RACE_UPDATE: xe mình giữ làn và tốc độ theo phím, xe đối thủ được ghi đè theo server.",
         ],
         "alt": [
             "3a. Trước GO: phím bị khóa; riêng việc nhả phím vẫn được nhận để xe không bị kẹt ga.",
             "5a. Xe đâm vào xe cộ: server đặt tốc độ về 0 trong 1,5 s; client nháy đỏ và phát âm thanh va chạm.",
         ],
         "post": "Vị trí hai xe trên màn hình khớp với trạng thái server. Va chạm được ghi vào match_events."},
        {"name": "Thi đấu tiếp sau trận", "actor": "Người chơi",
         "pre": "Trận đã kết thúc; client nhận MATCH_RESULT và REMATCH_ASK từ server.",
         "main": [
             "Thẻ kết quả hiển thị thắng / thua / hòa, lý do kết thúc và điểm mới.",
             "Khi người chơi đóng thẻ kết quả, hộp thoại Thi đấu tiếp hiện ra; người chơi bấm Đồng ý hoặc Từ chối.",
             "Client gửi REMATCH_REPLY; server chờ phản hồi của đối thủ trong tối đa 30 giây.",
             "Khi cả hai đồng ý, server tạo trận mới trong cùng phòng và gửi MATCH_START; màn hình đua đặt lại xe và "
             "chờ đếm ngược mới.",
         ],
         "alt": [
             "3a. Một bên từ chối: server đóng phòng và gửi ROOM_CLOSED; người chơi quay về sảnh.",
             "3b. Hết 30 giây không trả lời: server đóng phòng; người chơi quay về sảnh.",
         ],
         "post": "Ván mới bắt đầu với chướng ngại vật mới và quãng đường về 0, hoặc phòng đã đóng."},
    ],
    "classes": {
        "classes": [
            ("RaceFrame", None,
             ["-myCar : CarModel", "-opponentCar : CarModel", "-phase : Phase"],
             ["+handle(Message): void", "-onTick(): void", "-onRaceUpdate(RaceState): void"], 1, 0, "blue"),
            ("ResultDialog", None, [],
             ["+show(Window, MatchResult, String, String): void", "~reasonText(EndReason, MatchOutcome): String"],
             0, 0, "blue"),
            ("RematchDialog", None, [],
             ["+ask(Window, String): boolean"], 2, 0, "blue"),
            ("RacePanel", None,
             ["-raceClock : RaceClock", "-mine : TrackView", "-opponent : TrackView"],
             ["+setRaceTick(long): void", "#paintComponent(Graphics): void"], 0, 1, "blue"),
            ("CarModel", None,
             ["-distance : double", "-lane : int", "-speed : double"],
             ["+setThrottle(boolean): void", "+laneLeft(): void", "+advance(double): void",
              "+applyServerOwn(CarSnapshot): boolean", "+applyServer(CarSnapshot): boolean"], 1, 1, "purple"),
            ("RaceState", "record",
             ["-roomId : int", "-tick : long", "-me : CarSnapshot", "-opponent : CarSnapshot"],
             [], 2, 1, "grey"),
            ("RaceClock", None, ["-visualTick : double"],
             ["~accept(long, long): void", "~sample(long): double"], 0, 2, "blue"),
            ("TrackView", None, ["-car : CarModel"],
             ["~trafficPos(Obstacle, double): double", "~tileSizeFor(int, int): int"], 1, 2, "blue"),
            ("Room", None, ["-tick : long", "-matchId : int"],
             ["+tick(): void", "+countdown(int): void", "+onCarState(Session, CarState): void",
              "-endRound(...): void"], 2, 3, "orange"),
            ("CarSim", None, ["-distance : double", "-speed : double", "-lane : int"],
             ["+applyClient(CarState, long): void", "+advance(long, long, List): Obstacle",
              "+snapshot(long): CarSnapshot"], 1, 3, "orange"),
        ],
        "relations": [
            ("RaceFrame", "ResultDialog", "dep", ""),
            ("RaceFrame", "RematchDialog", "dep", ""),
            ("RaceFrame", "RacePanel", "comp", "1"),
            ("RaceFrame", "CarModel", "assoc", "myCar, opponentCar"),
            ("RaceFrame", "RaceState", "dep", "nhận RACE_UPDATE"),
            ("RacePanel", "RaceClock", "comp", "1"),
            ("RacePanel", "TrackView", "comp", "2"),
            ("TrackView", "CarModel", "assoc", "car"),
            ("Room", "RaceState", "dep", "tạo mỗi tick"),
            ("Room", "CarSim", "comp", "2"),
        ],
        "col_w": 2.8, "pt": 8.5, "width": 16.0,
    },
    "class_files": {
        "RaceFrame": RACE + "RaceFrame.java",
        "ResultDialog": RACE + "ResultDialog.java",
        "RematchDialog": RACE + "RematchDialog.java",
        "RacePanel": RACE + "RacePanel.java",
        "CarModel": CL + "model/CarModel.java",
        "RaceState": CM + "dto/RaceState.java",
        "RaceClock": RACE + "RaceClock.java",
        "TrackView": RACE + "TrackView.java",
        "Room": SRV + "core/Room.java",
        "CarSim": SRV + "core/CarSim.java",
    },
    "class_text": (
        "Biểu đồ lớp gồm mười lớp chính. Bên client, RaceFrame là cửa sổ đua: nó giữ hai CarModel (xe mình và xe đối "
        "thủ), giao RacePanel vẽ hai nửa màn hình, nhận RaceState từ mạng và mở hai hộp thoại kết quả. Bên server, Room "
        "tạo RaceState mỗi tick và giữ hai CarSim làm trọng tài. Hai phía nối với nhau qua RaceState và các thông điệp "
        "trong common; biểu đồ không vẽ mạng để giữ gọn."),
    "class_rows": [
        ["RaceFrame", "Cửa sổ đua: nhận thông điệp, xử lý phím, vòng lặp 50 ms, hộp thoại kết quả"],
        ["RacePanel", "Vẽ hai nửa màn hình, HUD, đếm ngược và hiệu ứng thứ hạng"],
        ["TrackView", "Camera bám một xe, vẽ đường, làn, xe cộ theo tick và vụ nổ"],
        ["RaceClock", "Đồng hồ hình ảnh làm mượt tick mạng đến không đều"],
        ["CarModel", "Trạng thái một xe phía client; dự đoán cục bộ và áp dụng trạng thái server"],
        ["RaceState", "Record: vị trí hai xe, số tick và thời gian đua, gửi trong RACE_UPDATE"],
        ["ResultDialog", "Hiển thị thắng / thua / hòa và lý do kết thúc"],
        ["RematchDialog", "Hộp thoại Đồng ý / Từ chối thi đấu tiếp"],
        ["Room", "Máy trạng thái của một phòng: đếm ngược, tick trọng tài, kết quả, thi đấu tiếp"],
        ["CarSim", "Mô phỏng xe phía server: cắt tốc độ, va chạm, về đích"],
    ],
    "sequences": [
        {"title": "Điều khiển xe và đồng bộ trạng thái trong trận",
         "text": (
             "Hình tuần tự dưới đây theo một chu kỳ điều khiển: phím được nhấn ở client, CAR_STATE đi lên server, "
             "server tiến xe một tick rồi phát RACE_UPDATE về hai máy. Chu kỳ này lặp lại mỗi 50 ms. Thông điệp đi qua "
             "NetworkClient và RoomManager (chuyển tới đúng phòng) nhưng hình lược bỏ hai khối này cho gọn."),
         "participants": [("P", "Người chơi", "actor"), ("RF", "RaceFrame", "object"),
                          ("CH", "ClientHandler", "object"), ("RM", "Room", "object"), ("CS", "CarSim", "object")],
         "messages": [
             ("P", "RF", "nhấn W / A / D", "sync"),
             ("RF", "RF", "setThrottle / laneLeft", "self"),
             ("RF", "CH", "CAR_STATE (qua TCP)", "sync"),
             ("CH", "RM", "onCarState(s, cs)", "sync"),
             ("RM", "CS", "applyClient(cs, now)", "sync"),
             ("RM", "CS", "advance(tick)", "sync"),
             ("CS", "RM", "CarSnapshot", "return"),
             ("RM", "CH", "RACE_UPDATE (RaceState)", "sync"),
             ("CH", "RF", "RACE_UPDATE (qua TCP)", "sync"),
             ("RF", "RF", "applyServerOwn / applyServer", "self"),
         ],
         "frames": [(2, 9, "lặp mỗi 50 ms")], "gap": 1.6, "step": 0.42, "width": 16.0},
        {"title": "Kết thúc trận, thi đấu tiếp và đóng phòng",
         "text": (
             "Khi một xe về đích hoặc có người thoát, phòng ghi kết quả vào CSDL rồi gửi MATCH_RESULT cho hai người "
             "chơi và hỏi thi đấu tiếp. Phòng chỉ bắt đầu ván mới khi cả hai cùng đồng ý; nếu một bên từ chối hoặc hết "
             "30 giây thì phòng đóng."),
         "participants": [("RM", "Room", "object"), ("CH", "ClientHandler", "object"),
                          ("RF", "RaceFrame", "object"), ("RD", "RematchDialog", "object"),
                          ("P", "Người chơi", "actor")],
         "messages": [
             ("RM", "CH", "MATCH_RESULT", "sync"),
             ("CH", "RF", "MATCH_RESULT", "sync"),
             ("RF", "RF", "ResultDialog.show()", "self"),
             ("RM", "CH", "REMATCH_ASK", "sync"),
             ("CH", "RF", "REMATCH_ASK", "sync"),
             ("RF", "RD", "RematchDialog.ask()", "sync"),
             ("P", "RD", "bấm Đồng ý / Từ chối", "sync"),
             ("RD", "RF", "agree", "return"),
             ("RF", "CH", "REMATCH_REPLY", "sync"),
             ("CH", "RM", "onRematchReply(s, agree)", "sync"),
             ("RM", "RM", "startRound()", "self"),
         ],
         "frames": [(8, 10, "khi cả hai đồng ý")], "gap": 1.6, "step": 0.42, "width": 16.0},
    ],
    "er": {
        "entities": [
            ("players", [("player_id", "PK", "int"), ("username", "", "varchar(50)"),
                         ("password_hash", "", "varchar(255)"), ("points", "", "int"), ("wins", "", "int"),
                         ("losses", "", "int"), ("draws", "", "int")], 0.15, 4.95, "blue"),
            ("matches", [("match_id", "PK", "int"), ("room_code", "", "varchar(20)"),
                         ("player1_id", "FK", "int"), ("player2_id", "FK", "int"), ("winner_id", "FK", "int"),
                         ("status", "", "enum"), ("end_reason", "", "enum"), ("started_at", "", "datetime"),
                         ("ended_at", "", "datetime")], 4.85, 4.95, "orange"),
            ("match_events", [("event_id", "PK", "bigint"), ("match_id", "FK", "int"),
                              ("player_id", "FK", "int"), ("event_type", "", "varchar(30)"),
                              ("event_time", "", "datetime(3)"), ("payload", "", "json")], 4.85, 1.69, "green"),
        ],
        "relations": [
            ("players", "matches", "1", "N", "tham gia", (0.0, 0.3)),
            ("matches", "match_events", "1", "N", "gồm", (0.0, 0.0)),
            ("players", "match_events", "1", "N", "ghi bởi", (-0.2, 0.25)),
        ],
        "W": 7.8, "H": 5.2, "width": 13.0,
    },
    "er_text": [
        "Lược đồ có ba bảng do em thiết kế, tạo bởi db/schema.sql. Bảng players lưu tài khoản và thống kê (điểm, thắng, "
        "thua, hòa); username là khóa duy nhất và chỉ mục idx_rank phục vụ bảng xếp hạng. Bảng matches lưu mỗi trận, "
        "với player1_id, player2_id và winner_id là khóa ngoại tới players; winner_id để NULL khi hòa hoặc hủy trận. "
        "Bảng match_events lưu diễn biến trận (START, COLLISION, FINISH, QUIT, DISCONNECT, REMATCH) để xem lại, mỗi dòng "
        "gắn với một trận và một người chơi.",
        "Trên sơ đồ, ba khóa ngoại player1_id, player2_id và winner_id được gộp thành một đường “tham gia” giữa players "
        "và matches để hình đỡ rối. Chi tiết kiểu dữ liệu và chỉ mục nằm trong db/schema.sql.",
    ],
    "screens": [
        (shared.SHOTS / "race.png",
         "Màn hình đua hai nửa: mỗi nửa là camera bám xe của một người chơi, có HUD tốc độ, quãng đường, thứ hạng và "
         "dòng gợi ý phím phía dưới", 15),
    ],
    "results": [
        ["Màn hình đua: hai nửa, camera bám xe, 3 làn, cảnh quan, HUD, minimap",
         "Hiển thị đúng thiết kế khi chạy; hình học xe cộ được kiểm bằng TrackViewTest"],
        ["Điều khiển W / S / A / D, nhả ga tự giảm tốc, giới hạn làn và tốc độ",
         "CarModelTest kiểm tăng tốc đến tối đa trong khoảng 3 s, phanh về 0, không ra khỏi làn"],
        ["Đồng bộ RACE_UPDATE không làm bật ngược lần đổi làn vừa bấm",
         "CarModelTest.ownCarKeepsLocalLaneAndSpeedWhenServerUpdateIsOneTickBehind"],
        ["Đồng hồ hình ảnh làm mượt tick mạng, không chạy lùi", "RaceClockTest"],
        ["Kết quả và lý do kết thúc đúng từng trường hợp (thắng, thua, hòa, thoát, mất kết nối)",
         "ResultDialogTest; logic thắng / hòa / thi đấu tiếp phía server có trong RoomTest"],
        ["Va chạm, cắt tốc độ và trọng tài phía server", "CarSimTest"],
        ["Hiệu năng (đệm sprite, chỉ vẽ trong khung nhìn)",
         "Chưa đo bằng số liệu; đánh giá bằng quan sát khi chạy demo"],
        ["Phần làm thêm: giao diện, chuyển động, âm thanh", "Chạy trong ứng dụng, hiển thị theo thiết kế"],
    ],
    "tests": [
        ["T7", "Chấp nhận, đếm ngược, đua (logic server)", "Phím chỉ hoạt động sau GO", "Đạt (RoomTest)"],
        ["T8", "Đâm vào xe cộ", "Tốc độ về 0 trong 1,5 s trên cả hai màn hình, có vụ nổ",
         "Chạy thủ công khi demo"],
        ["T9", "Về đích trước (logic server)", "Người về trước thắng và được +1 điểm", "Đạt (RoomTest)"],
        ["T10", "Về đích cùng tick (racing-tools)", "Hòa, mỗi người +1", "Đạt (racing-tools, RoomTest)"],
        ["T11", "Thoát trận giữa chừng (racing-tools)", "Người thoát 0 điểm, đối thủ +1, end_reason = QUIT",
         "Đạt (racing-tools, RoomTest)"],
        ["T12", "Rút mạng client đang đua", "Trong ≤ 15 s đối thủ thắng và về sảnh",
         "Chạy thủ công khi demo"],
        ["T13", "Cả hai đồng ý thi đấu tiếp (logic server)", "Ván mới trong cùng phòng, xe cộ khác",
         "Đạt (RoomTest)"],
        ["T15", "2 phòng đua cùng lúc (logic server)", "Không lẫn RACE_UPDATE giữa các phòng", "Đạt (RoomTest)"],
        ["T17", "CAR_STATE tốc độ 999 (racing-tools; lần chạy 24/09 khi ngưỡng còn 200 km/h)",
         "Server cắt về MAX_SPEED", "Đạt (racing-tools, CarSimTest)"],
    ],
    "unit_tests": [
        ("common/src/test/java/racing/common/net/MessageSerializationTest.java",
         "Mọi loại thông điệp và DTO tuần tự hóa rồi đọc lại được"),
        ("client/src/test/java/racing/client/model/CarModelTest.java",
         "Tăng tốc, phanh, giới hạn tốc độ và làn, áp trạng thái server, va chạm"),
        ("client/src/test/java/racing/client/ui/race/TrackViewTest.java",
         "Vị trí xe cộ theo tick đúng công thức server, kích thước ô vẽ"),
        ("client/src/test/java/racing/client/ui/race/RaceClockTest.java",
         "Đồng hồ hình ảnh không chạy lùi và bám tick mới nhất"),
        ("client/src/test/java/racing/client/ui/race/ResultDialogTest.java",
         "Câu lý do kết thúc đúng theo kết quả và lý do"),
        ("server/src/test/java/racing/server/core/CarSimTest.java",
         "Cắt tốc độ, bỏ qua quãng đường client gửi, về đích, va chạm và stun"),
    ],
    "conclusion": [
        "Em hoàn thành các task D1–D7 và R1–R6, cộng thêm giao diện, chuyển động và âm thanh. Phần nền xong sớm giúp "
        "cả nhóm làm song song, và màn hình đua hiển thị đúng vị trí do server tính.",
        "Qua phần việc này em hiểu cách thiết kế giao thức tầng ứng dụng trên TCP, tuần tự hóa đối tượng, đồng bộ trạng "
        "thái giữa client và server có độ trễ, vẽ đồ họa bằng Swing và Java2D, và tổ chức kiểm thử tự động với CI.",
        "Hạn chế: hiệu năng chưa được đo bằng số liệu (khung hình, CPU); kịch bản T8 và T12 chưa được ghi kết quả "
        "chạy hai máy; kịch bản T17 được chạy khi ngưỡng tốc độ còn 200 km/h, còn bản hiện tại là 360 km/h và đã có "
        "CarSimTest kiểm tra; kết quả thủ công trong docs/TEST-PLAN.md chưa được ghi lại.",
        "Hướng phát triển: đo khung hình và CPU trên máy yếu, bổ sung kiểm thử tự động cho giao diện đua, và chạy lại "
        "toàn bộ T1–T19 trên hai máy trước buổi demo.",
    ],
}
