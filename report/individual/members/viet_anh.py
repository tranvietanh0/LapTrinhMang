"""Báo cáo cá nhân: Trần Việt Anh – Nền tảng, màn hình đua, kiểm thử, đóng gói (D1–D7, R1–R6)."""
from source import lines_of, matching, method

CM = "common/src/main/java/racing/common/"
SRV = "server/src/main/java/racing/server/"
CL = "client/src/main/java/racing/client/"
RACE = CL + "ui/race/"

CONTENT = {
    "name": "Trần Việt Anh",
    "slug": "TranVietAnh",
    "role": "Nền tảng dự án, màn hình đua, kiểm thử tích hợp, đóng gói (D1–D7, R1–R6)",
    "intro": (
        "Trong nhóm, Trần Việt Anh phụ trách phần nền mà cả nhóm cùng dựa vào (dự án Maven nhiều module, hợp đồng "
        "thông điệp dùng chung, lược đồ MySQL, lớp DAO, CI), màn hình đua phía client (vẽ đường đua, điều khiển xe, "
        "đồng bộ với server, kết quả và thi đấu tiếp), công cụ kiểm thử giao thức, dẫn dắt kiểm thử tích hợp và "
        "đóng gói bản chạy demo. Ngoài kế hoạch ban đầu còn làm thêm giao diện theo phong cách game đua phố, hiệu "
        "ứng chuyển động và âm thanh."),
    "tasks": [
        ["D1", "Dự án Maven nhiều module common / server / client / tools, mvnw, JDK 17, CI GitHub Actions có MySQL",
         "pom.xml, .github/workflows", "Cả nhóm build được bằng mvnw; CI chạy test trên MySQL thật"],
        ["D2", "Hợp đồng chung: Message, MessageType (22 loại), DTO dạng record, GameConfig",
         "common/", "Ba module dùng chung, có test tuần tự hóa"],
        ["D3–D4", "Lược đồ MySQL, dữ liệu mẫu; PlayerDAO, MatchDAO (chốt kết quả trong transaction)",
         "db/, server/db/", "Test DAO với MySQL thật trên CI"],
        ["R1", "RacePanel, TrackView, PixelArt: chia đôi màn hình, camera cuộn bám xe, 3 làn, cảnh quan, HUD, minimap",
         "ui/race/*", "Hiển thị đúng thiết kế, chạy được offline bằng RaceDemo"],
        ["R2", "CarModel và điều khiển: giữ W tăng tốc, S phanh, A / D đổi làn, nhả ga tự giảm tốc",
         "model/CarModel, RaceFrame", "Đường cuộn mượt, không vượt 360 km/h"],
        ["R3", "Nối mạng: gửi CAR_STATE 20 lần / giây, đếm ngược từ COUNTDOWN, khóa phím trước GO",
         "RaceFrame", "Server nhận 20 CAR_STATE / s; không bấm được trước GO"],
        ["R4", "Nhận RACE_UPDATE: hiệu chỉnh xe mình, cập nhật xe đối thủ, vẽ xe cộ theo tick, vụ nổ khi va chạm",
         "RaceFrame, TrackView, RaceClock", "Hai máy thấy vị trí như nhau"],
        ["R5", "ResultDialog, RematchDialog, Thoát trận có xác nhận (Esc)", "ui/race/*",
         "Đủ 4 loại kết quả; rematch mở đường đua mới"],
        ["R6", "Hiệu năng: đệm sprite dựng sẵn, chỉ vẽ trong khung nhìn, đồng hồ vẽ cố định nhịp", "ui/race/*",
         "Khung hình đều, CPU thấp"],
        ["D5", "Hỗ trợ nối saveResult vào MatchService; review PR của Mai Anh", "MatchService, review",
         "Điểm trong DB đúng mọi kịch bản"],
        ["D6", "Kiểm thử tích hợp theo docs/TEST-PLAN.md; công cụ racing-tools cho T10, T17", "tools/, docs/TEST-PLAN.md",
         "19 kịch bản có kết quả"],
        ["D7", "Đóng gói jar (build-dist), README, kịch bản demo", "scripts/, README.md",
         "Người ngoài nhóm chạy được theo README"],
    ],
    "files": [
        (CM + "net/Message.java", "Gói thông điệp duy nhất đi qua socket"),
        (CM + "net/MessageType.java", "Danh sách loại thông điệp và hợp đồng hành vi"),
        (CM + "GameConfig.java", "Hằng số dùng chung: cổng, tick, tốc độ, luật điểm"),
        ("db/schema.sql", "Lược đồ MySQL"),
        (SRV + "db/PlayerDAO.java", "Truy cập bảng players"),
        (SRV + "db/MatchDAO.java", "Truy cập matches / match_events, transaction chốt kết quả"),
        (SRV + "db/DbConnection.java", "Cấu hình kết nối từ biến môi trường hoặc db.properties"),
        (CL + "model/CarModel.java", "Mô hình xe phía client"),
        (RACE + "RaceFrame.java", "Cửa sổ trận đấu: phím, mạng, kết quả"),
        (RACE + "RacePanel.java", "Vẽ hai nửa màn hình đua"),
        (RACE + "TrackView.java", "Camera cuộn, đường, xe cộ, vụ nổ"),
        (RACE + "RaceClock.java", "Đồng hồ hình ảnh làm mượt tick mạng"),
        (RACE + "RaceHud.java", "HUD tốc độ, quãng đường, minimap"),
        (RACE + "PixelArt.java", "Sprite xe và cảnh quan"),
        ("tools/src/main/java/racing/tools/ScriptedClient.java", "Client dòng lệnh chạy kịch bản kiểm thử"),
    ],
    "details_intro": (
        "Phần này trình bày cách hiện thực từng nhóm task, kèm đoạn mã trích trực tiếp từ mã nguồn hiện tại của "
        "repo. Phần nền được làm xong sớm (tuần đầu) để các thành viên khác có hợp đồng chung và DAO để gọi ngay."),
    "sections": [
        ("3.1. Dự án Maven nhiều module và CI (D1)", [
            ("table", ["Module", "Nội dung", "Phụ thuộc"], [
                ["common", "Message, MessageType, DTO, GameConfig", "–"],
                ["server", "GameServer, ClientHandler, lõi trận đấu, DAO", "common, MySQL Connector/J"],
                ["client", "Ứng dụng Swing: sảnh, màn hình đua, âm thanh", "common, FlatLaf"],
                ["tools", "ScriptedClient chạy kịch bản giao thức", "common"],
            ], [2.4, 8.6, 5.0], "Các module Maven"),
            ("p", "Dự án đi kèm Maven Wrapper (mvnw / mvnw.cmd) nên máy không cài Maven vẫn build được. Mỗi module "
                  "đóng gói thành một jar chạy được (racing-server.jar, racing-client.jar, racing-tools.jar). "
                  "GitHub Actions chạy mvnw verify với một MySQL service thật, chờ cổng 3306 mở rồi nạp schema trước "
                  "khi chạy test, nên test DAO và test tích hợp server chạy thật trên mỗi PR."),
        ]),
        ("3.2. Hợp đồng thông điệp dùng chung (D2)", [
            ("p", "Toàn bộ giao tiếp client – server dùng một lớp Message duy nhất gồm loại thông điệp, payload và "
                  "thời điểm tạo. Đây là giao thức tầng ứng dụng nằm trên TCP: TCP lo truyền byte tin cậy, đúng thứ "
                  "tự; Message quy định ý nghĩa của từng gói."),
            ("code", method(CM + "net/Message.java", r"public Message\(MessageType type, Object payload\)"),
             "Message – hàm tạo"),
            ("code", matching(CM + "GameConfig.java", r"public static final (int|double) "),
             "GameConfig – hằng số dùng chung"),
            ("p", "Hợp đồng được đóng băng sau tuần 1; mọi thay đổi phải sửa cả ba module trong cùng một PR. "
                  "MessageSerializationTest tuần tự hóa và đọc lại từng loại payload để bảo đảm mọi DTO đều "
                  "Serializable."),
        ]),
        ("3.3. Lược đồ và DAO chốt kết quả trong transaction (D3, D4)", [
            ("p", "Chốt một trận cần sửa ba dòng: dòng matches và dòng players của hai người. Nếu làm rời rạc, lỗi "
                  "giữa chừng sẽ để trận đã có người thắng mà điểm chưa cộng. saveResult() kiểm tra đầu vào, tắt "
                  "auto-commit, cập nhật tất cả rồi commit; lỗi bất kỳ thì rollback."),
            ("code", method(SRV + "db/MatchDAO.java", r"public void saveResult\(", 62), "MatchDAO.saveResult()"),
        ]),
        ("3.4. Màn hình đua: vẽ đường đua (R1)", [
            ("p", "Màn hình chia đôi, mỗi nửa là một TrackView có camera riêng bám theo xe của một người, nhìn từ "
                  "trên xuống kiểu game đua cổ điển: 3 làn, cảnh quan hai bên, vạch START / FINISH, HUD tốc độ, "
                  "quãng đường và minimap. Vị trí xe cộ được tính từ tick server theo cùng công thức với server nên "
                  "hai máy thấy xe cộ ở cùng chỗ:"),
            ("fig", "fig5_race.png", "Màn hình đua và các thành phần", 14.5),
            ("code", method(RACE + "TrackView.java", r"static double trafficPos\("), "TrackView.trafficPos()"),
        ]),
        ("3.5. Mô hình xe và điều khiển (R2)", [
            ("p", "Phím dùng Key Bindings (không dùng KeyListener) để không phụ thuộc thành phần đang có focus. Phím "
                  "ga và phanh là phím giữ: nhấn gửi true, nhả gửi false; sự kiện nhả luôn được nhận kể cả trước GO "
                  "để xe không bị kẹt ga."),
            ("code", method(RACE + "RaceFrame.java", r"private void bindHold\("), "RaceFrame.bindHold()"),
            ("code", method(CL + "model/CarModel.java", r"public void advance\(double"), "CarModel.advance()"),
        ]),
        ("3.6. Nối mạng và đếm ngược (R3)", [
            ("p", "Màn hình chờ COUNTDOWN từ server: 3, 2, 1 rồi 0 nghĩa là GO; chỉ khi đó vòng lặp mô phỏng mới "
                  "chạy và phím mới có tác dụng. Mỗi khung hình xe được mô phỏng cục bộ để mượt, còn CAR_STATE được "
                  "gửi đúng nhịp 50 ms (20 lần / giây) bằng cách giữ mốc thời gian lần gửi trước."),
            ("code", method(RACE + "RaceFrame.java", r"private void onTick\(\)"), "RaceFrame.onTick()"),
        ]),
        ("3.7. Đồng bộ với server (R4)", [
            ("p", "Server là nguồn sự thật. Với xe đối thủ, client ghi đè toàn bộ theo RACE_UPDATE. Với xe của mình, "
                  "client nhận quãng đường, trạng thái choáng và về đích từ server nhưng giữ làn và tốc độ theo phím: "
                  "RACE_UPDATE được tính trước khi server nhận CAR_STATE mới nhất, nếu ghi đè làn thì lần đổi làn "
                  "vừa bấm sẽ bị bật ngược (lỗi đã gặp khi kiểm thử)."),
            ("code", method(CL + "model/CarModel.java", r"public boolean applyServerOwn\("), "CarModel.applyServerOwn()"),
            ("code", method(RACE + "RaceFrame.java", r"private void onRaceUpdate\("), "RaceFrame.onRaceUpdate()"),
            ("p", "Tick mạng đến không đều (jitter). RaceClock giữ một đồng hồ hình ảnh tăng đều, kéo dần về tick "
                  "mới nhất nhận được, không bao giờ chạy lùi, nên xe cộ không giật dù gói tin đến sớm muộn khác nhau."),
            ("code", method(RACE + "RaceClock.java", r"double sample\("), "RaceClock.sample()"),
        ]),
        ("3.8. Kết quả, thi đấu tiếp và thoát trận (R5, R6)", [
            ("p", "Khi nhận MATCH_RESULT, vòng mô phỏng dừng và thẻ kết quả hiện thắng / thua / hòa, lý do (về đích, "
                  "cùng về đích, đối thủ thoát, đối thủ mất kết nối) và điểm mới. REMATCH_ASK mở hộp thoại Đồng ý / "
                  "Từ chối gửi REMATCH_REPLY; cả hai đồng ý thì server gửi MATCH_START mới và cùng cửa sổ bắt đầu ván "
                  "mới. Phím Esc hoặc nút Thoát hỏi xác nhận rồi gửi QUIT_MATCH."),
            ("code", method(RACE + "RaceFrame.java", r"public void handle\(Message"), "RaceFrame.handle()"),
            ("p", "Tối ưu hiệu năng (R6): sprite xe và cảnh quan được dựng sẵn vào BufferedImage, chỉ vẽ vật nằm "
                  "trong khung nhìn, không tạo Font / Stroke mới mỗi khung hình, vẽ theo đồng hồ hạn chót cố định "
                  "thay vì Timer chồng nhau."),
        ]),
        ("3.9. Công cụ kiểm thử, kiểm thử tích hợp và đóng gói (D5–D7)", [
            ("p", "ScriptedClient là client dòng lệnh nói đúng giao thức, chạy kịch bản dạng văn bản. Nó giúp kiểm "
                  "thử những tình huống khó làm bằng tay: hai xe về đích cùng tick (T10) hay client gian lận gửi tốc "
                  "độ 999 km/h (T17)."),
            ("code", lines_of("tools/scripts/t17-speed-cheat.txt", r"^login alice", 11),
             "tools/scripts/t17-speed-cheat.txt (dòng drive 999 gửi tốc độ gian lận)"),
            ("b", [("docs/TEST-PLAN.md: ", "19 kịch bản T1–T19, mỗi kịch bản có kết quả mong đợi, người test và "
                    "câu SQL xác nhận dữ liệu."),
                   ("scripts/build-dist: ", "build và gom racing-server.jar, racing-client.jar, racing-tools.jar, "
                    "schema và hướng dẫn vào thư mục dist/ để chép sang máy demo."),
                   ("Server --memory: ", "chạy không cần MySQL, dùng khi máy demo chưa cài cơ sở dữ liệu."),
                   ("Review: ", "chạy thử và review các PR dữ liệu của Mai Anh trước khi gộp (D5).")]),
        ]),
        ("3.10. Phần làm thêm ngoài kế hoạch", [
            ("b", [("Giao diện phong cách game đua phố: ", "hệ thống màu, phông, nút vát góc dùng chung cho đăng "
                    "nhập, sảnh, xếp hạng, lịch sử, lời mời và màn hình đua."),
                   ("Chuyển động: ", "lớp Motion (easing, tween) và một nhịp khung hình chung để các hiệu ứng mượt "
                    "mà không tốn CPU."),
                   ("Âm thanh: ", "hiệu ứng âm thanh, tiếng động cơ theo tốc độ và nhạc nền chiptune đều sinh bằng "
                    "code (không cần file âm thanh)."),
                   ("Màn hình vừa khung: ", "mọi cửa sổ và nút luôn nằm trong vùng nhìn thấy của màn hình.")]),
        ]),
    ],
    "tests": [
        ["T1", "Đăng nhập sai mật khẩu", "Báo lỗi, không vào sảnh", "Người test"],
        ["T7", "Chấp nhận, đếm ngược, đua", "Phím chỉ hoạt động sau GO", "Người test"],
        ["T8", "Đâm vào xe cộ", "Tốc độ về 0 trong 1,5 s trên cả hai màn hình, có vụ nổ", "Người test"],
        ["T9", "Về đích trước", "Người về trước +1, end_reason = FINISH", "Người test, sửa lỗi"],
        ["T10", "Về đích cùng tick (racing-tools)", "Hòa, mỗi người +1", "Người test"],
        ["T11", "Thoát trận giữa chừng", "Người thoát 0 điểm, đối thủ +1, QUIT", "Người test"],
        ["T13", "Cả hai đồng ý thi đấu tiếp", "Ván mới, dòng xe cộ khác, quãng đường về 0", "Người test"],
        ["T15", "2 phòng đua cùng lúc", "Không lẫn dữ liệu giữa phòng", "Người test"],
        ["T17", "CAR_STATE tốc độ 999 (racing-tools)", "Server cắt về 360", "Người test"],
    ],
    "unit_tests": [
        ("common/src/test/java/racing/common/net/MessageSerializationTest.java",
         "Mọi loại thông điệp và DTO tuần tự hóa / đọc lại được"),
        ("client/src/test/java/racing/client/model/CarModelTest.java",
         "Tăng tốc, phanh, trôi, giới hạn 360 km/h, đổi làn, áp trạng thái server"),
        ("client/src/test/java/racing/client/ui/race/TrackViewTest.java", "Vị trí xe cộ theo tick, kích thước ô vẽ"),
        ("client/src/test/java/racing/client/ui/race/RaceClockTest.java", "Đồng hồ hình ảnh không chạy lùi, bám tick"),
        ("client/src/test/java/racing/client/ui/race/ResultDialogTest.java", "Câu lý do kết thúc đúng từng trường hợp"),
        ("tools/src/test/java/racing/tools/ScriptCommandTest.java", "Phân tích cú pháp kịch bản"),
        ("server/src/test/java/racing/server/db/DaoIntegrationTest.java", "DAO với MySQL thật, transaction saveResult"),
    ],
    "collab": [
        ("Với cả nhóm: ", "cung cấp hợp đồng common và DAO từ tuần đầu, giữ CI xanh để mọi PR được kiểm tra tự động."),
        ("Với Phạm Thị Thu Phương: ", "thống nhất luật hòa cùng tick và RACE_UPDATE theo góc nhìn từng người; hỗ "
         "trợ nối lưu kết quả vào MatchService."),
        ("Với Vũ Văn Hiếu: ", "dùng NetworkClient của Hiếu, RaceLauncher nối sảnh với màn hình đua."),
        ("Với Nguyễn Trần Mai Anh: ", "review, chạy thử các PR M2–M5; phối hợp kiểm tra SQL khi kiểm thử T9–T12."),
    ],
    "problems": [
        ["Đổi làn bị bật ngược khi nhận RACE_UPDATE", "Xe mình giữ làn / tốc độ theo phím (applyServerOwn)"],
        ["Xe cộ giật do gói tin đến không đều", "RaceClock làm mượt tick hình ảnh, không chạy lùi"],
        ["Kẹt ga khi nhả phím trước GO", "Sự kiện nhả phím luôn được nhận"],
        ["Test DAO không chạy được trên máy không có Docker", "CI chạy MySQL service; sau cài MySQL cục bộ"],
        ["Khó tái hiện hòa và gian lận bằng tay", "Viết ScriptedClient và kịch bản t10, t17"],
    ],
    "summary": (
        "Tự đánh giá: hoàn thành các task D1–D7 và R1–R6, cộng thêm giao diện, chuyển động và âm thanh. Phần nền "
        "xong sớm giúp cả nhóm làm song song; màn hình đua đồng bộ đúng với server trọng tài. Qua phần việc này em "
        "nắm được cách thiết kế giao thức tầng ứng dụng trên TCP, tuần tự hóa đối tượng, đồng bộ trạng thái giữa "
        "client và server có độ trễ, lập trình đồ họa Swing / Java2D và tổ chức kiểm thử tự động với CI."),
}
