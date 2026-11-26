"""Phần chung của 4 báo cáo cá nhân: giới thiệu, yêu cầu, thiết kế tổng quan, triển khai, tài liệu tham khảo.

Hình chung được vẽ lại bằng uml.py mỗi lần build (out/img/shared_*.png).
"""
from __future__ import annotations

from pathlib import Path

import uml

HERE = Path(__file__).resolve().parent
IMG_OUT = HERE / "out" / "img"
SHOTS = HERE.parents[1] / "plans" / "261012-0946-game-ui-performance" / "screenshots"
OLD_IMG = HERE.parent / "img"

TOPIC = "Game đua xe thi đấu đối kháng online"
LECTURER = "Nguyễn Hoàng Anh"
CLASS = "Nhóm 01"

ABBREVIATIONS = [
    ("API", "Application Programming Interface", "Giao diện lập trình ứng dụng"),
    ("CSDL", "Cơ sở dữ liệu", "Database"),
    ("DAO", "Data Access Object", "Lớp truy cập dữ liệu"),
    ("DTO", "Data Transfer Object", "Đối tượng chỉ chứa dữ liệu để truyền qua mạng"),
    ("EDT", "Event Dispatch Thread", "Luồng xử lý sự kiện và vẽ giao diện của Swing"),
    ("ER", "Entity – Relationship", "Mô hình thực thể – liên kết"),
    ("FIFO", "First In, First Out", "Vào trước, ra trước (hàng đợi)"),
    ("FK / PK", "Foreign Key / Primary Key", "Khóa ngoại / khóa chính"),
    ("HUD", "Head-Up Display", "Thông tin hiển thị chồng lên màn hình đua"),
    ("IP", "Internet Protocol", "Giao thức liên mạng, địa chỉ máy"),
    ("JDBC", "Java Database Connectivity", "API kết nối CSDL của Java"),
    ("JDK", "Java Development Kit", "Bộ công cụ phát triển Java"),
    ("LAN", "Local Area Network", "Mạng cục bộ"),
    ("SQL", "Structured Query Language", "Ngôn ngữ truy vấn có cấu trúc"),
    ("TCP", "Transmission Control Protocol", "Giao thức điều khiển truyền vận"),
    ("UC", "Use Case", "Ca sử dụng"),
    ("UI", "User Interface", "Giao diện người dùng"),
    ("UML", "Unified Modeling Language", "Ngôn ngữ mô hình hóa thống nhất"),
]

INTRO = [
    "Trò chơi trực tuyến nhiều người chơi là bài toán điển hình của lập trình mạng: nhiều máy khách cùng nối tới "
    "một máy chủ, trao đổi thông điệp liên tục với độ trễ thấp, và máy chủ phải giữ trạng thái chung nhất quán cho "
    "mọi người. Nhóm chọn đề tài “" + TOPIC + "”: hai người chơi trên hai máy khác nhau thách đấu nhau và cùng đua "
    "trên đường đua 3 làn dài 2 500 m, né các xe cộ đang chạy cùng chiều; ai về đích trước thì thắng và được cộng "
    "điểm vào bảng xếp hạng.",
    "Ứng dụng viết hoàn toàn bằng Java theo mô hình Client – Server. Client dùng Java Swing, kết nối tới server qua "
    "TCP Socket cổng 5000; mỗi thông điệp là một đối tượng Message được tuần tự hóa bằng ObjectOutputStream. Server "
    "xử lý mỗi client trên một luồng riêng, làm trọng tài cho trận đua (tự tính quãng đường, va chạm, người thắng "
    "theo nhịp 50 ms) và lưu tài khoản, điểm, lịch sử trận vào MySQL qua JDBC. Client chỉ gửi ý định điều khiển "
    "(làn đường, ga / phanh), không được tự quyết định kết quả, nhờ vậy không gian lận được bằng cách sửa client.",
]

REQUIREMENTS = [
    ["YC01", "Đăng nhập bằng tài khoản lưu trong CSDL; một tài khoản chỉ đăng nhập ở một nơi", "Chức năng"],
    ["YC02", "Đăng ký tài khoản mới, mật khẩu lưu dạng băm có muối", "Chức năng"],
    ["YC03", "Hiển thị danh sách người chơi online kèm điểm và trạng thái Rảnh / Đang đấu, cập nhật tức thì",
     "Chức năng"],
    ["YC04", "Thách đấu người chơi đang Rảnh; người được mời chấp nhận / từ chối trong 30 giây", "Chức năng"],
    ["YC05", "Tạo phòng đua, đếm ngược 3 – 2 – 1, đồng bộ vị trí hai xe theo thời gian thực", "Chức năng"],
    ["YC06", "Server làm trọng tài: cắt tốc độ tối đa 360 km/h, xử lý va chạm, xác định thắng / thua / hòa",
     "Chức năng"],
    ["YC07", "Thoát trận hoặc mất kết nối giữa chừng thì đối thủ thắng", "Chức năng"],
    ["YC08", "Sau trận hỏi “thi đấu tiếp”; cả hai đồng ý thì đua ván mới trong cùng phòng", "Chức năng"],
    ["YC09", "Cộng điểm, lưu kết quả và diễn biến trận vào CSDL; xem bảng xếp hạng và lịch sử trận",
     "Chức năng"],
    ["YC10", "Nhiều phòng đua chạy song song, không lẫn dữ liệu giữa các phòng", "Phi chức năng"],
    ["YC11", "Phát hiện mất kết nối trong ≤ 15 giây (heartbeat 5 giây); giao diện không bị đơ khi chờ mạng",
     "Phi chức năng"],
    ["YC12", "Chạy được trên hai máy Windows trong cùng mạng LAN, chỉ cần JDK 17 và MySQL 8", "Phi chức năng"],
]

TEAM_NOTE = ("Tỉ lệ đóng góp do cả nhóm thống nhất dựa trên khối lượng task trong kế hoạch chia việc (PLAN.md) và "
             "lịch sử commit trên repo. Mỗi thành viên phụ trách trọn một phần độc lập của hệ thống, các phần nối "
             "với nhau qua hợp đồng thông điệp chung trong module common.")

ARCH_TEXT = [
    "Hệ thống gồm ba khối: chương trình client chạy trên máy mỗi người chơi, chương trình server chạy trên một máy "
    "trong mạng LAN, và CSDL MySQL đặt cùng máy server. Client và server chỉ nói chuyện với nhau qua một kết nối "
    "TCP lâu dài; server là nơi duy nhất truy cập CSDL.",
    "Mã nguồn chia thành bốn module Maven: common (hợp đồng thông điệp Message / MessageType, các DTO dạng record "
    "và hằng số GameConfig dùng chung), server, client và tools (client dòng lệnh để kiểm thử giao thức). Nhờ "
    "module common, hai phía luôn dùng cùng một định nghĩa thông điệp; đổi hợp đồng thì cả hai cùng biên dịch lại.",
]

MESSAGES = [
    ["Tài khoản", "LOGIN, LOGIN_RESULT, LOGOUT, REGISTER, REGISTER_RESULT", "Đăng nhập, đăng ký, đăng xuất"],
    ["Sảnh chờ", "ONLINE_LIST, INVITE, INVITE_RECEIVED, INVITE_REPLY, INVITE_RESULT",
     "Danh sách online, lời mời thách đấu"],
    ["Trận đua", "MATCH_START, COUNTDOWN, CAR_STATE, RACE_UPDATE, FINISH, MATCH_RESULT",
     "Bắt đầu, đồng bộ trạng thái xe, kết quả"],
    ["Sau trận", "REMATCH_ASK, REMATCH_REPLY, QUIT_MATCH, ROOM_CLOSED", "Thi đấu tiếp, thoát, đóng phòng"],
    ["Thống kê", "LEADERBOARD_REQ, LEADERBOARD, MATCH_HISTORY_REQ, MATCH_HISTORY", "Bảng xếp hạng, lịch sử trận"],
    ["Duy trì", "PING, PONG", "Heartbeat phát hiện mất kết nối"],
]

CLIENT_BLOCKS = [
    ("Kết nối mạng", ["Kết nối TCP tới server", "Luồng đọc / luồng ghi FIFO", "Heartbeat PING 5 s",
                      "Listener theo MessageType"]),
    ("Tài khoản", ["Đăng nhập", "Đăng ký tài khoản", "Đăng xuất", "Báo mất kết nối"]),
    ("Sảnh chờ", ["Danh sách người online", "Gửi lời mời thách đấu", "Hộp thoại lời mời 30 s",
                  "Chuyển sang màn hình đua"]),
    ("Màn hình đua", ["Đếm ngược 3 – 2 – 1", "Điều khiển làn, ga, phanh", "Vẽ đường đua, HUD",
                      "Kết quả, thi đấu tiếp"]),
    ("Thống kê", ["Bảng xếp hạng", "Lịch sử trận đấu"]),
    ("Giao diện, âm thanh", ["Theme và co giãn màn hình", "Hiệu ứng âm thanh, nhạc nền"]),
]

SERVER_BLOCKS = [
    ("Kết nối", ["GameServer nhận kết nối :5000", "ClientHandler: 1 luồng / client", "Hàng đợi gửi riêng",
                 "Timeout im lặng 15 s"]),
    ("Phiên, tài khoản", ["Xác thực đăng nhập", "Đăng ký, băm mật khẩu", "Chặn đăng nhập trùng",
                          "Phát ONLINE_LIST"]),
    ("Lời mời", ["Kiểm tra OFFLINE / BUSY", "Hạn trả lời 30 s", "Báo kết quả cho người mời"]),
    ("Phòng đua", ["Tạo / đóng phòng", "Máy trạng thái phòng", "Đếm ngược, thi đấu tiếp", "Thoát, mất kết nối"]),
    ("Trọng tài", ["Tick 50 ms (CarSim)", "Cắt tốc độ ≤ 360 km/h", "Va chạm, choáng 1,5 s",
                   "Về đích, thắng / hòa"]),
    ("Dữ liệu", ["Lưu kết quả (transaction)", "Ghi diễn biến match_events", "Bảng xếp hạng",
                 "Lịch sử trận"]),
]

OVERALL_UC = {
    "title": "Hệ thống game đua xe đối kháng online",
    "actors": [("P", "Người chơi", "left", 4.0)],
    "cases": [
        ("dk", "Đăng ký tài khoản", 0, 0), ("dn", "Đăng nhập", 0, 1), ("dx", "Đăng xuất", 0, 2),
        ("ol", "Xem danh sách online", 0, 3), ("td", "Thách đấu người chơi", 0, 4), ("tl", "Trả lời lời mời", 0, 5),
        ("dua", "Thi đấu (điều khiển xe)", 0, 6), ("bxh", "Xem bảng xếp hạng", 0, 7),
        ("ls", "Xem lịch sử trận", 0, 8),
        ("kq", "Xem kết quả trận", 1, 5), ("tt", "Thi đấu tiếp", 1, 6.3), ("thoat", "Thoát trận", 1, 7.6),
    ],
    "links": [("P", c, "assoc") for c in ("dk", "dn", "dx", "ol", "td", "tl", "dua", "bxh", "ls")]
    + [("dua", "kq", "include"), ("tt", "dua", "extend"), ("thoat", "dua", "extend")],
}

ARCH_RESULT_TEXT = (
    "Kiến trúc khi chạy thực tế gồm một máy server (chạy racing-server.jar và MySQL) và nhiều máy client (chạy "
    "racing-client.jar), tất cả trong cùng mạng LAN. Mỗi người chơi dùng một máy client; client nối tới địa chỉ "
    "IPv4 của máy server ở cổng 5000. Trong trận, mỗi client gửi CAR_STATE khi người chơi đổi làn hoặc ga / phanh, "
    "server tính toán và phát RACE_UPDATE cho cả hai người mỗi 50 ms.")

ENVIRONMENT = [
    ["Ngôn ngữ", "Java 17 (biên dịch với --release 17), chạy được trên JDK 17 – 25"],
    ["Giao diện", "Java Swing + FlatLaf, vẽ đường đua bằng Java2D"],
    ["Mạng", "java.net.Socket / ServerSocket, ObjectInputStream / ObjectOutputStream"],
    ["CSDL", "MySQL 8.0, MySQL Connector/J (JDBC)"],
    ["Build, kiểm thử", "Maven Wrapper (mvnw), JUnit 5, GitHub Actions (CI có dịch vụ MySQL)"],
    ["Môi trường chạy", "Windows 10 / 11, 2 máy cùng mạng LAN"],
]

DEPLOY_STEPS = [
    ("Build: ", "chạy mvnw.cmd package ở thư mục gốc, tạo server/target/racing-server.jar và "
                "client/target/racing-client.jar."),
    ("CSDL: ", "cài MySQL 8 (hoặc docker compose up -d), nạp db/schema.sql và db/seed.sql (6 tài khoản mẫu, mật "
               "khẩu 123456)."),
    ("Cấu hình: ", "chép db.properties.example thành db.properties, điền URL, user, mật khẩu MySQL."),
    ("Mở cổng: ", "trên máy server mở TCP 5000 trong Windows Firewall."),
    ("Chạy server: ", "java -jar racing-server.jar (thêm --memory để chạy không cần MySQL khi demo nhanh)."),
    ("Chạy client: ", "java -jar racing-client.jar trên mỗi máy, nhập địa chỉ IPv4 của máy server ở màn hình "
                      "đăng nhập."),
]

FEATURES = [
    ["Đăng nhập, đăng ký, đăng xuất", "Hoạt động; đăng nhập trùng bị từ chối, mật khẩu lưu dạng sha256 có muối"],
    ["Danh sách người chơi online", "Cập nhật tức thì khi có người vào / ra / bắt đầu đấu"],
    ["Thách đấu và trả lời lời mời", "Đủ các trường hợp: chấp nhận, từ chối, hết 30 s, BUSY, OFFLINE"],
    ["Trận đua thời gian thực", "Đếm ngược, đồng bộ 50 ms, xe cộ, va chạm, về đích, hòa"],
    ["Thoát trận, mất kết nối", "Đối thủ được xử thắng, cả hai về sảnh, CSDL ghi lý do QUIT / DISCONNECT"],
    ["Thi đấu tiếp", "Cả hai đồng ý thì ván mới trong cùng phòng; một bên từ chối thì về sảnh"],
    ["Bảng xếp hạng, lịch sử trận", "Lấy từ MySQL, sắp theo điểm và số trận thắng"],
    ["Nhiều phòng song song", "Hai cặp đua cùng lúc không lẫn dữ liệu"],
]

REFERENCES = [
    "Học viện Công nghệ Bưu chính Viễn thông, Bài giảng môn Lập trình mạng, Khoa Công nghệ Thông tin 1.",
    "Oracle, “Lesson: All About Sockets”, The Java Tutorials. "
    "https://docs.oracle.com/javase/tutorial/networking/sockets/index.html",
    "Oracle, “Concurrency in Swing”, The Java Tutorials. "
    "https://docs.oracle.com/javase/tutorial/uiswing/concurrency/index.html",
    "Oracle, Java SE 17 API Specification: java.io.ObjectOutputStream. "
    "https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/io/ObjectOutputStream.html",
    "Oracle, MySQL 8.0 Reference Manual. https://dev.mysql.com/doc/refman/8.0/en/",
    "Oracle, “Lesson: JDBC Basics”, The Java Tutorials. https://docs.oracle.com/javase/tutorial/jdbc/basics/",
    "JUnit Team, JUnit 5 User Guide. https://junit.org/junit5/docs/current/user-guide/",
    "Object Management Group, Unified Modeling Language Specification, Version 2.5.1, 2017. "
    "https://www.omg.org/spec/UML/2.5.1",
]


def figures() -> dict[str, Path]:
    """Vẽ các hình dùng chung, trả về {khóa: đường dẫn}."""
    out = {
        "arch": OLD_IMG / "fig1_architecture.png",
        "client_blocks": uml.blocks(IMG_OUT / "shared_client_blocks.png", "Chương trình Client (racing-client)",
                                    CLIENT_BLOCKS, color="blue"),
        "server_blocks": uml.blocks(IMG_OUT / "shared_server_blocks.png", "Chương trình Server (racing-server)",
                                    SERVER_BLOCKS, color="orange"),
        "uc_overall": uml.usecase(IMG_OUT / "shared_uc_overall.png", OVERALL_UC["title"], OVERALL_UC["actors"],
                                  OVERALL_UC["cases"], OVERALL_UC["links"], row_h=0.66),
        "deploy": uml.deployment(IMG_OUT / "shared_deployment.png"),
    }
    for k, p in out.items():
        if not p.exists():
            raise FileNotFoundError(f"thiếu hình chung {k}: {p}")
    return out
