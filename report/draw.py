# -*- coding: utf-8 -*-
"""Draw all diagrams for the BTL report with matplotlib (clean, consistent style)."""
import os
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
from matplotlib.patches import FancyBboxPatch, FancyArrowPatch, Rectangle, Circle
from matplotlib.lines import Line2D

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "img")
os.makedirs(OUT, exist_ok=True)

plt.rcParams["font.family"] = "Segoe UI"
plt.rcParams["font.size"] = 11

# Palette
C_CLIENT = "#DCEBFA"; C_CLIENT_E = "#2F6DB5"
C_SERVER = "#FFF1D6"; C_SERVER_E = "#D98C1F"
C_DB = "#E3F3E3"; C_DB_E = "#3C8C3C"
C_GREY = "#F2F2F2"; C_GREY_E = "#7A7A7A"
C_TEXT = "#1F1F1F"
C_ARROW = "#444444"
C_RED = "#C0392B"; C_BLUE = "#2F6DB5"; C_ROAD = "#4A4A4A"


def box(ax, x, y, w, h, title, lines=(), fc=C_GREY, ec=C_GREY_E, title_size=12, body_size=10,
        radius=0.15, lw=1.6, title_bold=True):
    p = FancyBboxPatch((x, y), w, h, boxstyle=f"round,pad=0,rounding_size={radius}",
                       fc=fc, ec=ec, lw=lw)
    ax.add_patch(p)
    if lines:
        # title band
        ax.text(x + w / 2, y + h - 0.32, title, ha="center", va="center", fontsize=title_size,
                fontweight="bold" if title_bold else "normal", color=C_TEXT)
        ax.plot([x + 0.15, x + w - 0.15], [y + h - 0.62, y + h - 0.62], color=ec, lw=1)
        body_top = y + h - 0.62
        n = len(lines)
        step = (body_top - y) / (n + 1)
        for i, ln in enumerate(lines):
            ax.text(x + w / 2, body_top - step * (i + 1), ln, ha="center", va="center",
                    fontsize=body_size, color=C_TEXT)
    else:
        ax.text(x + w / 2, y + h / 2, title, ha="center", va="center", fontsize=title_size,
                fontweight="bold" if title_bold else "normal", color=C_TEXT)
    return p


def arrow(ax, p1, p2, label=None, color=C_ARROW, lw=1.6, style="-|>", lpos=0.5, loff=(0, 0.18),
          fontsize=9.5, ls="-", shrink=4):
    a = FancyArrowPatch(p1, p2, arrowstyle=style, mutation_scale=16, color=color, lw=lw,
                        linestyle=ls, shrinkA=shrink, shrinkB=shrink)
    ax.add_patch(a)
    if label:
        mx = p1[0] + (p2[0] - p1[0]) * lpos + loff[0]
        my = p1[1] + (p2[1] - p1[1]) * lpos + loff[1]
        ax.text(mx, my, label, ha="center", va="center", fontsize=fontsize, color=C_TEXT,
                bbox=dict(fc="white", ec="none", pad=1.5))


def new_fig(w, h):
    fig = plt.figure(figsize=(w / 100, h / 100), dpi=100)
    ax = fig.add_axes([0, 0, 1, 1])
    ax.set_xlim(0, w / 100)
    ax.set_ylim(0, h / 100)
    ax.set_aspect("equal")
    ax.axis("off")
    return fig, ax


def save(fig, name):
    path = os.path.join(OUT, name)
    fig.savefig(path, dpi=200, facecolor="white")
    plt.close(fig)
    print("saved", path)


# ---------------------------------------------------------------- 1. Architecture
def fig_architecture():
    fig, ax = new_fig(1400, 760)
    # Clients
    for i, (label, y) in enumerate([("Client A", 5.4), ("Client B", 3.4), ("Client N", 1.4)]):
        box(ax, 0.5, y, 3.2, 1.5, f"{label} (Java Swing)",
            ["View  ·  Controller  ·  Model", "Player, Match, RaceState"],
            fc=C_CLIENT, ec=C_CLIENT_E)
    ax.text(2.1, 1.05, "…  nhiều client kết nối đồng thời", ha="center", fontsize=9.5,
            style="italic", color="#555")
    # Server
    box(ax, 5.6, 1.6, 4.0, 4.9, "Game Server (Java)",
        ["ServerSocket (TCP :5000)", "ClientHandler – 1 thread / client",
         "SessionManager – người chơi online", "InviteManager – lời mời, timeout 30s",
         "RoomManager – phòng đua, countdown", "MatchService – trọng tài, xét thắng thua",
         "PlayerDAO / MatchDAO (JDBC)"],
        fc=C_SERVER, ec=C_SERVER_E, title_size=13)
    # DB
    box(ax, 11.0, 2.7, 2.9, 2.7, "MySQL",
        ["players", "matches", "match_events"], fc=C_DB, ec=C_DB_E, title_size=13)
    # Arrows client <-> server
    for y in (6.15, 4.15, 2.15):
        arrow(ax, (3.7, y + 0.15), (5.6, y + 0.15), color=C_BLUE)
        arrow(ax, (5.6, y - 0.15), (3.7, y - 0.15), color=C_SERVER_E)
    ax.text(4.65, 6.62, "Request / Event", ha="center", fontsize=9, color=C_BLUE)
    ax.text(4.65, 5.72, "Response / Update", ha="center", fontsize=9, color=C_SERVER_E)
    # Server <-> DB
    arrow(ax, (9.6, 4.35), (11.0, 4.35), "JDBC: SELECT / INSERT / UPDATE", color=C_DB_E, loff=(0, 0.28))
    arrow(ax, (11.0, 3.75), (9.6, 3.75), "ResultSet", color=C_DB_E, loff=(0, -0.28))
    # Legend / note
    ax.text(7.0, 0.75, "Giao thức: TCP Socket + ObjectInputStream / ObjectOutputStream, mỗi thông điệp là một đối tượng Message (type, payload).",
            ha="center", fontsize=9.5, color="#333")
    ax.text(7.0, 0.35, "Server là trọng tài duy nhất: mọi luật thắng thua, timeout và cập nhật điểm đều được quyết định tại server.",
            ha="center", fontsize=9.5, color="#333")
    save(fig, "fig1_architecture.png")


# ---------------------------------------------------------------- 2. Sequence diagram (flow)
def fig_sequence():
    fig, ax = new_fig(1400, 1000)
    W, H = 14, 10
    actors = [("Client A\n(người thách đấu)", 2.0, C_CLIENT, C_CLIENT_E),
              ("Game Server", 7.0, C_SERVER, C_SERVER_E),
              ("Client B\n(người được mời)", 12.0, C_CLIENT, C_CLIENT_E)]
    top = H - 0.4
    for name, x, fc, ec in actors:
        box(ax, x - 1.3, top - 0.9, 2.6, 0.9, name, fc=fc, ec=ec, title_size=11)
        ax.plot([x, x], [0.4, top - 0.9], color="#9A9A9A", lw=1.2, ls=(0, (4, 3)))
    # steps: (from_x, to_x, y, label, color)
    steps = [
        (2, 7, "LOGIN(username, password)", C_BLUE),
        (7, 2, "LOGIN_OK + ONLINE_LIST", C_SERVER_E),
        (2, 7, "INVITE(target = B)", C_BLUE),
        (7, 12, "INVITE_RECEIVED(from = A)  – chờ tối đa 30s", C_SERVER_E),
        (12, 7, "INVITE_ACCEPT", C_BLUE),
        (7, 2, "MATCH_START(roomId)  →  cả A và B", C_SERVER_E),
        (7, 2, "COUNTDOWN 3 · 2 · 1 · GO", C_SERVER_E),
        (2, 7, "CAR_STATE(x, lane, speed)  – liên tục 20 lần/giây", C_BLUE),
        (7, 12, "RACE_UPDATE(vị trí 2 xe)  →  cả A và B", C_SERVER_E),
        (2, 7, "FINISH(time)  – A về đích trước", C_BLUE),
        (7, 12, "MATCH_RESULT(winner = A, +1 điểm)  →  cả A và B", C_SERVER_E),
        (7, 12, "REMATCH_ASK?  →  cả A và B", C_SERVER_E),
        (12, 7, "REMATCH_YES / REMATCH_NO", C_BLUE),
    ]
    y = top - 1.35
    dy = 0.6
    for i, (fx, tx, label, col) in enumerate(steps):
        arrow(ax, (fx, y), (tx, y), color=col, lw=1.5)
        ax.text((fx + tx) / 2, y + 0.17, f"{i + 1}. {label}", ha="center", va="bottom",
                fontsize=9.3, color=C_TEXT)
        y -= dy
    # phase brackets on the far left
    phases = [("Đăng nhập", 0, 1), ("Thách đấu", 2, 4), ("Thi đấu", 5, 8), ("Kết quả", 9, 12)]
    for name, s, e in phases:
        y1 = top - 1.35 - s * dy + 0.32
        y2 = top - 1.35 - e * dy - 0.15
        ax.add_patch(Rectangle((0.15, y2), 0.35, y1 - y2, fc="#EDEDED", ec="none"))
        ax.text(0.325, (y1 + y2) / 2, name, rotation=90, ha="center", va="center", fontsize=9,
                fontweight="bold", color="#444")
    save(fig, "fig2_sequence.png")


# ---------------------------------------------------------------- 3. Server internals
def fig_server():
    fig, ax = new_fig(1400, 1010)
    # outer server box
    ax.add_patch(FancyBboxPatch((0.4, 0.5), 9.7, 8.4, boxstyle="round,pad=0,rounding_size=0.2",
                                fc="#FFFBF2", ec=C_SERVER_E, lw=2))
    ax.text(4.0, 8.55, "Game Server (tiến trình Java)", fontsize=13, fontweight="bold", color=C_TEXT)
    # accept thread
    box(ax, 0.7, 6.3, 2.7, 1.7, "ServerSocket", ["Thread Accept", "accept() → tạo ClientHandler"],
        fc=C_SERVER, ec=C_SERVER_E, title_size=11.5, body_size=9.5)
    # handlers
    hy = [6.9, 5.4, 3.9]
    for i, y in enumerate(hy):
        box(ax, 4.0, y, 2.6, 1.2, f"ClientHandler #{i + 1}",
            ["Luồng đọc + luồng ghi · hàng đợi gửi"], fc=C_CLIENT, ec=C_CLIENT_E, title_size=10.5, body_size=9)
        arrow(ax, (3.4, 7.15), (4.0, y + 0.6), color=C_ARROW, lw=1.3)
    ax.text(6.1, 3.6, "… một thread cho mỗi client", ha="center", fontsize=9, style="italic", color="#555")
    # core services (right column)
    box(ax, 7.2, 5.9, 2.6, 2.2, "SessionManager",
        ["Danh sách online", "trạng thái Rảnh / Đang đấu", "heartbeat + timeout"],
        fc=C_SERVER, ec=C_SERVER_E, title_size=11, body_size=9)
    box(ax, 7.2, 3.4, 2.6, 2.2, "InviteManager",
        ["Lời mời thách đấu", "hẹn giờ 30s tự hủy", "accept / reject"],
        fc=C_SERVER, ec=C_SERVER_E, title_size=11, body_size=9)
    # bottom row
    box(ax, 0.7, 0.8, 5.9, 2.5, "RoomManager / MatchService",
        ["Tạo Room, sinh 18 xe cộ theo seed · Countdown 3-2-1", "Nhận CAR_STATE, đồng bộ RACE_UPDATE cho cả hai",
         "Xét về đích, hòa, thoát, mất kết nối", "Cập nhật điểm, hỏi thi đấu tiếp"],
        fc=C_SERVER, ec=C_SERVER_E, title_size=11, body_size=9)
    box(ax, 7.2, 0.8, 2.6, 2.5, "PlayerDAO · MatchDAO",
        ["JDBC", "PreparedStatement", "transaction khi lưu kết quả"],
        fc=C_DB, ec=C_DB_E, title_size=11, body_size=9)
    # arrows handlers -> services
    arrow(ax, (6.6, 7.5), (7.2, 7.0), color=C_ARROW, lw=1.3)
    arrow(ax, (6.6, 6.0), (7.2, 4.5), color=C_ARROW, lw=1.3)
    arrow(ax, (5.3, 3.9), (4.5, 3.3), color=C_ARROW, lw=1.3)
    arrow(ax, (6.6, 2.05), (7.2, 2.05), color=C_DB_E, lw=1.3)
    # DB outside
    box(ax, 11.0, 0.9, 2.6, 2.4, "MySQL", ["players", "matches", "match_events"],
        fc=C_DB, ec=C_DB_E, title_size=13)
    arrow(ax, (9.8, 2.05), (11.0, 2.05), "JDBC", color=C_DB_E, lw=1.5, loff=(0, 0.25))
    # clients above the server, connecting into ServerSocket
    box(ax, 0.7, 9.25, 2.7, 0.6, "Client A, B, … N  (TCP Socket)", fc=C_CLIENT, ec=C_CLIENT_E, title_size=10)
    arrow(ax, (2.05, 9.25), (2.05, 8.0), color=C_BLUE, lw=1.5, ls=(0, (5, 3)))
    ax.text(2.35, 8.6, "connect()", fontsize=9, color=C_BLUE, va="center")
    save(fig, "fig3_server.png")


# ---------------------------------------------------------------- 4. Lobby UI mockup
def fig_lobby():
    fig, ax = new_fig(1400, 860)
    # Window
    ax.add_patch(Rectangle((0.5, 0.5), 8.6, 7.6, fc="white", ec="#555", lw=1.5))
    ax.add_patch(Rectangle((0.5, 7.5), 8.6, 0.6, fc="#2F6DB5", ec="none"))
    ax.text(0.7, 7.8, "Racing Online – Sảnh chờ", color="white", fontsize=11.5, fontweight="bold", va="center")
    ax.text(8.9, 7.8, "Xin chào, alice  ·  Điểm: 12", color="white", fontsize=10, va="center", ha="right")
    # table header
    cols = [(0.7, "Tên người chơi"), (3.5, "Điểm"), (4.7, "Thắng"), (5.8, "Trạng thái"), (7.6, "")]
    ax.add_patch(Rectangle((0.7, 6.55), 8.2, 0.55, fc="#E9EEF5", ec="#B8C4D6", lw=1))
    for x, t in cols:
        ax.text(x + 0.1, 6.82, t, fontsize=10, fontweight="bold", va="center")
    rows = [("bob", 15, 12, "Rảnh", True), ("carol", 9, 7, "Đang thi đấu", False),
            ("dave", 7, 6, "Rảnh", True), ("erin", 3, 2, "Đang thi đấu", False), ("frank", 0, 0, "Rảnh", True)]
    y = 6.0
    for name, pts, wins, st, free in rows:
        ax.add_patch(Rectangle((0.7, y - 0.28), 8.2, 0.55, fc="white" if free else "#FAFAFA", ec="#DADADA", lw=0.8))
        ax.text(0.8, y, name, fontsize=10, va="center")
        ax.text(3.6, y, str(pts), fontsize=10, va="center")
        ax.text(4.8, y, str(wins), fontsize=10, va="center")
        col = "#2E8B57" if free else "#C0392B"
        ax.add_patch(Circle((6.0, y), 0.08, fc=col, ec="none"))
        ax.text(6.15, y, st, fontsize=10, va="center", color=col)
        if free:
            ax.add_patch(FancyBboxPatch((7.6, y - 0.2), 1.2, 0.4, boxstyle="round,pad=0,rounding_size=0.08",
                                        fc="#2F6DB5", ec="none"))
            ax.text(8.2, y, "Thách đấu", color="white", fontsize=9, ha="center", va="center", fontweight="bold")
        y -= 0.62
    # bottom buttons
    for x, t, fc in [(0.7, "Bảng xếp hạng", "#6C757D"), (2.6, "Lịch sử trận", "#6C757D"), (7.4, "Đăng xuất", "#C0392B")]:
        ax.add_patch(FancyBboxPatch((x, 0.75), 1.7, 0.5, boxstyle="round,pad=0,rounding_size=0.08", fc=fc, ec="none"))
        ax.text(x + 0.85, 1.0, t, color="white", fontsize=9.5, ha="center", va="center", fontweight="bold")
    # Invite dialog on the right
    ax.add_patch(FancyBboxPatch((9.7, 3.6), 3.9, 3.0, boxstyle="round,pad=0,rounding_size=0.15",
                                fc="white", ec="#555", lw=1.5))
    ax.add_patch(Rectangle((9.7, 6.1), 3.9, 0.5, fc="#D98C1F", ec="none"))
    ax.text(9.9, 6.35, "Lời mời thách đấu", color="white", fontsize=10.5, fontweight="bold", va="center")
    ax.text(11.65, 5.55, "bob muốn thách đấu với bạn!", fontsize=10, ha="center", va="center")
    ax.text(11.65, 5.1, "Tự động hủy sau:  27 s", fontsize=10, ha="center", va="center", color="#C0392B")
    ax.add_patch(Rectangle((10.0, 4.7), 3.3, 0.14, fc="#EEE", ec="#CCC", lw=0.6))
    ax.add_patch(Rectangle((10.0, 4.7), 3.3 * 0.9, 0.14, fc="#D98C1F", ec="none"))
    for x, t, fc in [(10.0, "Chấp nhận (OK)", "#2E8B57"), (11.75, "Từ chối", "#C0392B")]:
        ax.add_patch(FancyBboxPatch((x, 3.85), 1.55, 0.5, boxstyle="round,pad=0,rounding_size=0.08", fc=fc, ec="none"))
        ax.text(x + 0.775, 4.1, t, color="white", fontsize=9.5, ha="center", va="center", fontweight="bold")
    ax.text(11.65, 3.25, "Hộp thoại hiện ở máy người được mời (B)", fontsize=9, ha="center", style="italic", color="#555")
    # Annotations
    ax.text(11.65, 1.6, "Danh sách online được server đẩy\nxuống mỗi khi có người đăng nhập,\nđăng xuất, vào trận hoặc kết thúc trận.",
            fontsize=9.5, ha="center", va="center", color="#333",
            bbox=dict(boxstyle="round,pad=0.5", fc="#F7F7F7", ec="#CCC"))
    save(fig, "fig4_lobby.png")


# ---------------------------------------------------------------- 5. Race UI (ảnh chụp thật + chú thích)
def fig_race():
    """Ảnh màn hình đua thật (RacePanel vẽ ra ảnh, không chụp desktop) kèm số chú thích khớp bảng."""
    img = plt.imread(os.path.join(OUT, "race_screen_src.png"))
    h, w = img.shape[0], img.shape[1]
    fig = plt.figure(figsize=(w / 100, h / 100), dpi=100)
    ax = fig.add_axes([0, 0, 1, 1])
    ax.imshow(img)
    ax.set_xlim(0, w)
    ax.set_ylim(h, 0)
    ax.axis("off")
    # (số, x, y) theo toạ độ pixel của ảnh 1200x800; khớp thứ tự dòng trong bảng thành phần
    callouts = [
        (1, 250, 38),    # HUD
        (2, 40, 420),    # minimap
        (3, 262, 655),   # xe mình + vụ nổ
        (4, 345, 495),   # xe cộ
        (5, 520, 470),   # cảnh quan
        (6, 1070, 527),  # vạch FINISH
        (7, 945, 705),   # xe đối thủ (đường bên phải)
        (8, 600, 787),   # dòng trạng thái
    ]
    for n, x, y in callouts:
        ax.add_patch(Circle((x, y), 17, fc=C_SERVER, ec=C_SERVER_E, lw=2.2, zorder=5))
        ax.text(x, y, str(n), ha="center", va="center", fontsize=13, fontweight="bold",
                color=C_SERVER_E, zorder=6)
    save(fig, "fig5_race.png")


# ---------------------------------------------------------------- 6. ERD
def fig_erd():
    fig, ax = new_fig(1460, 640)

    def entity(x, y, w, name, rows, fc, ec):
        rh = 0.36
        h = 0.55 + rh * len(rows)
        ax.add_patch(FancyBboxPatch((x, y), w, h, boxstyle="round,pad=0,rounding_size=0.1", fc="white", ec=ec, lw=1.6))
        ax.add_patch(Rectangle((x, y + h - 0.55), w, 0.55, fc=fc, ec=ec, lw=1.6))
        ax.text(x + w / 2, y + h - 0.275, name, ha="center", va="center", fontsize=12, fontweight="bold")
        for i, (key, col, typ) in enumerate(rows):
            yy = y + h - 0.55 - rh * (i + 0.5)
            ax.text(x + 0.12, yy, key, fontsize=8.5, va="center", color="#7A3E00" if key else "#000", fontweight="bold")
            ax.text(x + 0.5, yy, col, fontsize=9.5, va="center", family="Consolas")
            ax.text(x + w - 0.12, yy, typ, fontsize=8.5, va="center", ha="right", color="#555")
        return h

    hp = entity(0.3, 1.5, 3.7, "players", [
        ("PK", "player_id", "INT AUTO_INCREMENT"),
        ("", "username", "VARCHAR(50) UNIQUE"),
        ("", "password_hash", "VARCHAR(255)"),
        ("", "points", "INT DEFAULT 0"),
        ("", "wins", "INT DEFAULT 0"),
        ("", "losses", "INT DEFAULT 0"),
        ("", "draws", "INT DEFAULT 0"),
        ("", "created_at", "DATETIME"),
    ], C_DB, C_DB_E)
    hm = entity(5.6, 1.1, 4.0, "matches", [
        ("PK", "match_id", "INT AUTO_INCREMENT"),
        ("", "room_code", "VARCHAR(20)"),
        ("FK", "player1_id", "INT → players"),
        ("FK", "player2_id", "INT → players"),
        ("FK", "winner_id", "INT NULL → players"),
        ("", "status", "ENUM(PLAYING, FINISHED)"),
        ("", "end_reason", "ENUM(FINISH, DRAW, QUIT, DISCONNECT)"),
        ("", "started_at", "DATETIME"),
        ("", "ended_at", "DATETIME"),
    ], C_SERVER, C_SERVER_E)
    he = entity(10.9, 2.0, 3.4, "match_events", [
        ("PK", "event_id", "BIGINT AUTO_INCREMENT"),
        ("FK", "match_id", "INT → matches"),
        ("FK", "player_id", "INT → players"),
        ("", "event_type", "VARCHAR(30)"),
        ("", "event_time", "DATETIME(3)"),
        ("", "payload", "JSON"),
    ], C_CLIENT, C_CLIENT_E)

    # relationships (crow's foot simplified with 1 / N labels)
    def rel(p1, p2, label, lpos=0.5, loff=(0, 0.16)):
        ax.plot([p1[0], p2[0]], [p1[1], p2[1]], color=C_ARROW, lw=1.4)
        ax.text(p1[0] + (p2[0] - p1[0]) * lpos + loff[0], p1[1] + (p2[1] - p1[1]) * lpos + loff[1], label,
                fontsize=8.8, ha="center", va="center", bbox=dict(fc="white", ec="none", pad=1))
        # 1 side at p1 (small bar), N side at p2 (crow's foot)
        ax.plot([p1[0] + 0.12, p1[0] + 0.12], [p1[1] - 0.1, p1[1] + 0.1], color=C_ARROW, lw=1.4)
        ax.plot([p2[0] - 0.25, p2[0]], [p2[1], p2[1] + 0.12], color=C_ARROW, lw=1.2)
        ax.plot([p2[0] - 0.25, p2[0]], [p2[1], p2[1] - 0.12], color=C_ARROW, lw=1.2)

    # players -> matches (3 relations)
    rel((4.0, 3.9), (5.6, 3.9), "1 : N  player1_id", loff=(0, 0.17))
    rel((4.0, 3.25), (5.6, 3.25), "1 : N  player2_id", loff=(0, 0.17))
    rel((4.0, 2.55), (5.6, 2.55), "0..1  winner_id\n(NULL khi hòa)", loff=(0, 0.27))
    # matches -> events
    rel((9.6, 3.7), (10.9, 3.7), "1 : N  match_id", loff=(0, 0.17))
    # players -> events (route around)
    ax.plot([2.15, 2.15, 12.6, 12.6], [1.5, 0.7, 0.7, 2.0], color=C_ARROW, lw=1.4)
    ax.plot([2.05, 2.25], [1.35, 1.35], color=C_ARROW, lw=1.4)
    ax.plot([12.48, 12.6], [1.75, 2.0], color=C_ARROW, lw=1.2)
    ax.plot([12.72, 12.6], [1.75, 2.0], color=C_ARROW, lw=1.2)
    ax.text(7.4, 0.87, "1 : N  player_id (người gây ra sự kiện)", fontsize=8.8, ha="center", va="center",
            bbox=dict(fc="white", ec="none", pad=1))
    # ranking note
    ax.text(7.3, 5.85, "Bảng xếp hạng:  SELECT username, points, wins, losses, draws FROM players ORDER BY points DESC, wins DESC",
            ha="center", fontsize=9.5, family="Consolas", color="#333",
            bbox=dict(boxstyle="round,pad=0.4", fc="#F7F7F7", ec="#CCC"))
    save(fig, "fig6_erd.png")


# ---------------------------------------------------------------- 7. Match state machine
def fig_states():
    fig, ax = new_fig(1400, 560)
    states = [("WAITING", 1.4, "chờ trong phòng"), ("COUNTDOWN", 4.4, "3 · 2 · 1"), ("RACING", 7.4, "đang đua"),
              ("FINISHED", 10.4, "có kết quả"), ("CLOSED", 13.0, "kết thúc")]
    for name, x, sub in states:
        w = 2.3 if name != "CLOSED" else 1.6
        fc = C_SERVER if name in ("COUNTDOWN", "RACING") else (C_DB if name == "FINISHED" else C_GREY)
        ec = C_SERVER_E if name in ("COUNTDOWN", "RACING") else (C_DB_E if name == "FINISHED" else C_GREY_E)
        ax.add_patch(FancyBboxPatch((x - w / 2, 2.6), w, 1.1, boxstyle="round,pad=0,rounding_size=0.35", fc=fc, ec=ec, lw=1.6))
        ax.text(x, 3.35, name, ha="center", va="center", fontsize=11, fontweight="bold")
        ax.text(x, 2.9, sub, ha="center", va="center", fontsize=8.5, color="#555")
    arrow(ax, (2.55, 3.15), (3.25, 3.15), "cả hai\nvào phòng", fontsize=8.5, loff=(0, 0.85))
    arrow(ax, (5.55, 3.15), (6.25, 3.15), "hết 3 giây", fontsize=8.5, loff=(0, 0.75))
    arrow(ax, (8.55, 3.15), (9.25, 3.15), "về đích / hòa\nthoát / mất kết nối", fontsize=8.5, loff=(0, 0.85))
    arrow(ax, (11.55, 3.15), (12.2, 3.15), "một bên\ntừ chối", fontsize=8.5, loff=(0, 0.85))
    # rematch loop (below the boxes)
    a = FancyArrowPatch((10.4, 2.6), (4.4, 2.6), connectionstyle="arc3,rad=-0.3", arrowstyle="-|>",
                        mutation_scale=16, color=C_DB_E, lw=1.6, shrinkA=4, shrinkB=4)
    ax.add_patch(a)
    ax.text(7.4, 1.2, "cả hai đồng ý thi đấu tiếp → ván mới trong cùng phòng", ha="center", fontsize=9.5, color=C_DB_E,
            bbox=dict(fc="white", ec="none", pad=1))
    ax.text(7.0, 5.1, "Vòng đời một phòng đua (Room) tại server", ha="center", fontsize=12, fontweight="bold")
    # timeout note for waiting
    ax.text(1.4, 2.2, "lời mời hết 30s → hủy phòng", ha="center", fontsize=8.5, color="#C0392B")
    save(fig, "fig7_states.png")


if __name__ == "__main__":
    fig_architecture()
    fig_sequence()
    fig_server()
    fig_lobby()
    fig_race()
    fig_erd()
    fig_states()
