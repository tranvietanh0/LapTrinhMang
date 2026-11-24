"""Báo cáo cá nhân: Nguyễn Trần Mai Anh – Dữ liệu và SQL (M1–M6)."""
from source import lines_of, method

SRV = "server/src/main/java/racing/server/"
CL = "client/src/main/java/racing/client/"

CONTENT = {
    "name": "Nguyễn Trần Mai Anh",
    "slug": "NguyenTranMaiAnh",
    "role": "Dữ liệu và SQL – xếp hạng, đăng ký, lịch sử trận, ghi diễn biến (M1–M6)",
    "intro": (
        "Trong nhóm, Nguyễn Trần Mai Anh phụ trách phần dữ liệu: cài đặt và mô tả cơ sở dữ liệu MySQL, các chức năng "
        "đọc / ghi dữ liệu đi xuyên từ màn hình client qua server tới MySQL (bảng xếp hạng, đăng ký tài khoản, lịch "
        "sử trận), ghi diễn biến từng trận vào bảng match_events và kiểm tra dữ liệu trong DB khi cả nhóm chạy kiểm "
        "thử tích hợp. Mỗi chức năng là một lát cắt đầy đủ: câu SQL trong DAO, một nhánh xử lý trên server và một "
        "màn hình bảng trên client."),
    "tasks": [
        ["M1", "Cài MySQL, nạp schema.sql + seed.sql, chạy test DAO; viết docs/DB.md mô tả 3 bảng, ý nghĩa từng "
               "cột và các truy vấn hay dùng", "db/*.sql, docs/DB.md", "Test DAO chạy (không bị bỏ qua); DB.md đầy đủ"],
        ["M2", "Bảng xếp hạng: server trả LEADERBOARD khi nhận LEADERBOARD_REQ; client hiển thị Hạng, Tên, Điểm, "
               "Thắng, Thua, Hòa", "PlayerDAO.getLeaderboard, ClientHandler, ui/LeaderboardFrame",
         "Đúng thứ tự points DESC, wins DESC"],
        ["M3", "Đăng ký tài khoản: REGISTER → REGISTER_RESULT, báo lỗi trùng tên; form đăng ký",
         "AccountService.register, PlayerDAO.create, ui/RegisterDialog", "Tài khoản mới đăng nhập được; trùng tên báo lỗi"],
        ["M4", "Lịch sử trận: SQL JOIN lấy tên đối thủ, 20 trận mới nhất; HistoryFrame hiển thị Thời gian, Đối thủ, "
               "Kết quả, Lý do", "MatchDAO.findRecentByPlayer, ui/HistoryFrame", "Sau 3 trận thấy đúng 3 dòng"],
        ["M5", "Ghi diễn biến trận START, COLLISION, FINISH, QUIT, DISCONNECT, REMATCH kèm JSON; câu SQL xem lại",
         "MatchDAO.addEvent, MatchService.event, docs/DB.md", "match_events đủ sự kiện, đúng thứ tự thời gian"],
        ["M6", "Kiểm tra dữ liệu khi kiểm thử tích hợp: reset-db.sql trước mỗi buổi, chạy SQL xác nhận sau T9–T15",
         "db/reset-db.sql, docs/TEST-PLAN.md", "Mọi kịch bản có xác nhận dữ liệu trong DB"],
    ],
    "files": [
        ("db/schema.sql", "Lược đồ 3 bảng players, matches, match_events"),
        ("db/seed.sql", "6 tài khoản mẫu"),
        ("db/reset-db.sql", "Xóa lịch sử, đưa điểm về 0 trước buổi test"),
        ("docs/DB.md", "Mô tả bảng, cột và các truy vấn hay dùng"),
        (SRV + "core/AccountService.java", "Đăng ký, bảng xếp hạng, lịch sử trận phía server"),
        (SRV + "db/PasswordHasher.java", "Băm mật khẩu sha256 có salt"),
        (CL + "ui/LeaderboardFrame.java", "Cửa sổ bảng xếp hạng"),
        (CL + "ui/LeaderboardTableModel.java", "Mô hình bảng xếp hạng"),
        (CL + "ui/RegisterDialog.java", "Hộp thoại đăng ký"),
        (CL + "ui/HistoryFrame.java", "Cửa sổ lịch sử trận"),
        (CL + "ui/HistoryTableModel.java", "Mô hình bảng lịch sử, đổi mã kết quả sang tiếng Việt"),
    ],
    "details_intro": (
        "Phần này trình bày cách hiện thực từng task, kèm đoạn mã và câu SQL trích trực tiếp từ mã nguồn hiện tại. "
        "Mọi truy vấn đều dùng PreparedStatement với tham số ? để tránh SQL injection; client không bao giờ kết "
        "nối thẳng tới MySQL mà luôn hỏi qua server."),
    "sections": [
        ("3.1. Cơ sở dữ liệu và tài liệu DB.md (M1)", [
            ("p", "Cơ sở dữ liệu racing gồm ba bảng. players lưu tài khoản và thống kê; matches lưu mỗi ván đấu, "
                  "người thắng và lý do kết thúc; match_events lưu diễn biến chi tiết trong trận. Bảng dùng "
                  "InnoDB để có khóa ngoại và transaction, mã utf8mb4 để lưu được tiếng Việt."),
            ("fig", "fig6_erd.png", "Lược đồ quan hệ của cơ sở dữ liệu", 13.5),
            ("code", lines_of("db/schema.sql", r"CREATE TABLE IF NOT EXISTS matches", 16), "db/schema.sql – bảng matches"),
            ("table", ["Cột", "Ý nghĩa"], [
                ["winner_id", "NULL khi hòa hoặc trận bị hủy; ngược lại là player_id người thắng"],
                ["status", "PLAYING khi đang đua, FINISHED khi có kết quả, ABORTED khi hủy"],
                ["end_reason", "FINISH, DRAW, QUIT, DISCONNECT hoặc ABORTED"],
                ["idx_rank (players)", "Chỉ mục (points DESC, wins DESC) phục vụ truy vấn bảng xếp hạng"],
                ["idx_events_match", "Chỉ mục (match_id, event_time) để xem lại diễn biến một trận theo thời gian"],
            ], [4.2, 11.8], "Một số cột và chỉ mục quan trọng"),
            ("p", "docs/DB.md mô tả từng bảng, ý nghĩa từng cột và các truy vấn hay dùng: bảng xếp hạng, lịch sử 20 "
                  "trận của một người, số trận theo ngày và theo lý do kết thúc, xem lại diễn biến một trận, đếm số "
                  "lần va chạm của mỗi người, và cách dọn dữ liệu trước buổi test."),
        ]),
        ("3.2. Bảng xếp hạng (M2)", [
            ("p", "Phía server, câu SQL sắp theo điểm giảm dần, cùng điểm thì theo số trận thắng, cùng nữa thì theo "
                  "tên để thứ tự luôn ổn định; hạng được đánh số khi duyệt ResultSet."),
            ("code", method(SRV + "db/PlayerDAO.java", r"public List<RankRow> getLeaderboard\(int"),
             "PlayerDAO.getLeaderboard()"),
            ("p", "ClientHandler chỉ cần một dòng case gọi hàm dùng chung query(): nếu truy vấn lỗi, client nhận "
                  "ERROR \"Lỗi máy chủ khi lấy bảng xếp hạng\" thay vì bị ngắt kết nối."),
            ("code", method(SRV + "net/ClientHandler.java", r"private void query\("), "ClientHandler.query()"),
            ("p", "Phía client, LeaderboardFrame đăng ký listener LEADERBOARD rồi gửi LEADERBOARD_REQ; dữ liệu về được "
                  "nạp vào LeaderboardTableModel (Hạng, Tên, Điểm, Thắng, Thua, Hòa), ba hạng đầu hiển thị nổi bật."),
            ("code", method(CL + "ui/LeaderboardFrame.java", r"private void request\(\)"), "LeaderboardFrame.request()"),
        ]),
        ("3.3. Đăng ký tài khoản (M3)", [
            ("p", "Đăng ký được kiểm tra hai lớp. Client kiểm tra ngay trên form để báo lỗi nhanh (độ dài tên, ký tự "
                  "cho phép, độ dài mật khẩu, nhập lại khớp); server kiểm tra lại vì không được tin client."),
            ("code", method(CL + "ui/RegisterDialog.java", r"static String validate\("), "RegisterDialog.validate()"),
            ("code", method(SRV + "core/AccountService.java", r"public LoginResult register\("),
             "AccountService.register() – kiểm tra phía server"),
            ("p", "Trùng tên được phát hiện nhờ ràng buộc UNIQUE của cột username: MySQL ném "
                  "SQLIntegrityConstraintViolationException và server trả \"Tên tài khoản đã tồn tại\". Cách này "
                  "đúng cả khi hai người đăng ký cùng tên cùng lúc, điều mà SELECT kiểm tra trước không bảo đảm."),
            ("code", method(SRV + "db/PlayerDAO.java", r"public int create\("), "PlayerDAO.create()"),
            ("p", "Mật khẩu không lưu dạng rõ: PasswordHasher sinh salt ngẫu nhiên và lưu dạng "
                  "sha256$<salt>$<hash>; khi đăng nhập so sánh bằng MessageDigest.isEqual (thời gian không đổi). "
                  "Mảng char của ô mật khẩu được xóa trắng ngay sau khi gửi."),
            ("code", method(SRV + "db/PasswordHasher.java", r"public static String hash\("), "PasswordHasher.hash()"),
        ]),
        ("3.4. Lịch sử trận (M4)", [
            ("p", "Một trận có hai cột người chơi nên câu SQL JOIN bảng players hai lần (p1, p2) và dùng CASE để "
                  "lấy tên đối thủ theo góc nhìn người hỏi. Chỉ lấy trận đã kết thúc (status khác PLAYING), mới "
                  "nhất trước, giới hạn 20 dòng. Kết quả thắng / thua / hòa / hủy được suy ra từ winner_id và "
                  "end_reason."),
            ("code", method(SRV + "db/MatchDAO.java", r"public List<MatchRow> findRecentByPlayer\(", 34),
             "MatchDAO.findRecentByPlayer()"),
            ("p", "HistoryTableModel đổi mã sang chữ tiếng Việt để hiển thị, HistoryFrame tô màu kết quả và hiện dòng "
                  "tổng kết số trận thắng / thua / hòa."),
            ("code", method(CL + "ui/HistoryTableModel.java", r"public static String reasonText\("),
             "HistoryTableModel.reasonText()"),
        ]),
        ("3.5. Ghi diễn biến trận (M5)", [
            ("p", "Mỗi sự kiện là một dòng match_events với payload JSON (làn, quãng đường, vị trí xe cộ bị đâm). "
                  "MySQL kiểm tra payload đúng định dạng JSON nhờ kiểu cột JSON."),
            ("code", method(SRV + "db/MatchDAO.java", r"public void addEvent\("), "MatchDAO.addEvent()"),
            ("p", "Server gọi ghi sự kiện ở các thời điểm START (bắt đầu ván), COLLISION (đâm xe cộ), FINISH (về "
                  "đích), QUIT, DISCONNECT và REMATCH. Vì va chạm xảy ra trong luồng tick 50 ms, việc INSERT được "
                  "đẩy sang một executor riêng để tick không bị chậm bởi truy vấn DB:"),
            ("code", method(SRV + "core/MatchService.java", r"public void event\("), "MatchService.event()"),
            ("code", lines_of("docs/DB.md", r"^SELECT e.event_time", 7), "docs/DB.md – xem lại diễn biến một trận"),
        ]),
        ("3.6. Kiểm tra dữ liệu khi kiểm thử tích hợp (M6)", [
            ("p", "Trước mỗi buổi kiểm thử chạy db/reset-db.sql để xóa lịch sử và đưa điểm về 0, nhờ đó kết quả "
                  "từng kịch bản đọc được rõ ràng. Sau các kịch bản T9–T15, chạy câu SQL trong docs/TEST-PLAN.md để "
                  "xác nhận server đã lưu đúng:"),
            ("table", ["Kịch bản", "Dữ liệu cần thấy trong DB"], [
                ["T9 – về đích trước", "matches.end_reason = FINISH, winner_id đúng người; người thắng +1 điểm, +1 wins"],
                ["T10 – hòa cùng tick", "winner_id NULL, end_reason = DRAW; mỗi người +1 điểm, +1 draws"],
                ["T11 – thoát trận", "end_reason = QUIT, đối thủ thắng; có sự kiện QUIT trong match_events"],
                ["T12 – rút mạng", "end_reason = DISCONNECT; có sự kiện DISCONNECT"],
                ["T13 – thi đấu tiếp", "Hai dòng matches liên tiếp cùng room_code, đều có START"],
            ], [4.2, 11.8], "Xác nhận dữ liệu cho các kịch bản"),
            ("p", "Điểm và thống kê được cập nhật trong một transaction cùng với dòng matches (MatchDAO.saveResult): "
                  "nếu một câu lỗi thì rollback toàn bộ, không có trường hợp trận đã ghi người thắng mà điểm chưa cộng."),
        ]),
    ],
    "tests": [
        ["T9", "Về đích trước", "Người về trước +1, end_reason = FINISH", "Kiểm tra SQL"],
        ["T10", "Về đích cùng tick", "Hòa, mỗi người +1, winner_id NULL, end_reason = DRAW", "Kiểm tra SQL"],
        ["T11", "Thoát trận giữa chừng", "Đối thủ +1, end_reason = QUIT", "Kiểm tra SQL"],
        ["T12", "Rút mạng client đang đua", "end_reason = DISCONNECT", "Kiểm tra SQL"],
        ["T18", "Đăng ký mới rồi đăng nhập; đăng ký trùng tên", "Tài khoản mới vào được sảnh; trùng tên báo lỗi rõ",
         "Người test, sửa lỗi"],
        ["T19", "Xem lịch sử sau 3 trận", "Đúng 3 dòng, đúng đối thủ và kết quả", "Người test, sửa lỗi"],
    ],
    "unit_tests": [
        ("server/src/test/java/racing/server/db/DaoIntegrationTest.java",
         "Chạy với MySQL thật: tạo tài khoản, trùng tên, xếp hạng, saveResult thắng / hòa / hủy, lịch sử, sự kiện"),
        ("server/src/test/java/racing/server/db/PasswordHasherTest.java",
         "Băm có salt khác nhau mỗi lần, xác thực đúng / sai, chuỗi hỏng"),
    ],
    "collab": [
        ("Với Trần Việt Anh: ", "dùng PlayerDAO / MatchDAO nền và schema có sẵn; Việt Anh review và chạy thử các "
         "PR M2–M5 trước khi gộp."),
        ("Với Phạm Thị Thu Phương: ", "thêm các case LEADERBOARD_REQ, REGISTER, MATCH_HISTORY_REQ vào ClientHandler "
         "theo mẫu Phương đưa; cùng đặt các điểm ghi sự kiện trong Room / MatchService."),
        ("Với Vũ Văn Hiếu: ", "Hiếu đặt nút mở Bảng xếp hạng, Lịch sử trận trên sảnh và nút Đăng ký trên màn hình "
         "đăng nhập."),
    ],
    "problems": [
        ["Hai người đăng ký cùng tên cùng lúc có thể cùng lọt bước kiểm tra",
         "Dựa vào ràng buộc UNIQUE của MySQL và bắt SQLIntegrityConstraintViolationException"],
        ["Lịch sử cần tên đối thủ nhưng trận có hai cột người chơi", "JOIN players hai lần và dùng CASE theo người hỏi"],
        ["Ghi sự kiện va chạm làm chậm tick 50 ms", "Đẩy INSERT sang executor ghi riêng"],
        ["Chạy test tích hợp làm bẩn dữ liệu demo", "reset-db.sql trước mỗi buổi; khi cần nạp lại schema + seed"],
    ],
    "summary": (
        "Tự đánh giá: hoàn thành đủ 6 task M1–M6; ba chức năng xếp hạng, đăng ký, lịch sử chạy đầu cuối từ "
        "client qua server tới MySQL và dữ liệu sau mọi kịch bản kiểm thử đều được xác nhận bằng SQL. Qua phần "
        "việc này em nắm được thiết kế bảng với khóa ngoại và chỉ mục, JDBC với PreparedStatement, transaction "
        "commit / rollback, băm mật khẩu có salt, và cách một yêu cầu đi qua giao thức tự định nghĩa trên TCP từ "
        "client tới cơ sở dữ liệu. Hạn chế: chưa phân trang lịch sử (giới hạn 20 trận gần nhất)."),
}
