"""Vẽ sơ đồ cho báo cáo cá nhân bằng matplotlib: use case, lớp, tuần tự, ER, sơ đồ khối, triển khai.

Mọi hàm nhận đặc tả dạng dữ liệu (list / dict) và ghi PNG. Toạ độ tính bằng inch (1 đơn vị = 1 inch) để
cỡ chữ và kích thước hộp nhất quán giữa các hình.
"""
from __future__ import annotations

import math
from pathlib import Path

import matplotlib

matplotlib.use("Agg")
import matplotlib.pyplot as plt  # noqa: E402
from matplotlib.patches import Ellipse, FancyBboxPatch, Polygon, Rectangle  # noqa: E402

plt.rcParams["font.family"] = "Segoe UI"
DPI = 220
INK = "#1F1F1F"
LINE = "#3A3A3A"
BLUE, BLUE_E = "#DCEBFA", "#2F6DB5"
ORANGE, ORANGE_E = "#FFF1D6", "#C77A12"
GREEN, GREEN_E = "#E3F3E3", "#3C8C3C"
GREY, GREY_E = "#F4F4F4", "#7A7A7A"
PURPLE, PURPLE_E = "#EEE6F7", "#6B4C9A"
PALETTE = {"blue": (BLUE, BLUE_E), "orange": (ORANGE, ORANGE_E), "green": (GREEN, GREEN_E),
           "grey": (GREY, GREY_E), "purple": (PURPLE, PURPLE_E)}
CHAR_W = 0.0072  # inch / pt / ký tự (ước lượng trung bình cho Segoe UI)


def text_w(s: str, pt: float) -> float:
    return len(s) * pt * CHAR_W


def _canvas(w: float, h: float):
    fig = plt.figure(figsize=(w, h), dpi=DPI)
    ax = fig.add_axes([0, 0, 1, 1])
    ax.set_xlim(0, w)
    ax.set_ylim(0, h)
    ax.axis("off")
    return fig, ax


def _save(fig, path: Path) -> Path:
    path.parent.mkdir(parents=True, exist_ok=True)
    fig.savefig(path, dpi=DPI, facecolor="white")
    plt.close(fig)
    return path


def _arrow(ax, p1, p2, dashed=False, head="open", lw=1.2, color=LINE):
    """Đường từ p1 tới p2 với đầu mũi tên: open (>), filled, triangle (kế thừa), none."""
    ax.plot([p1[0], p2[0]], [p1[1], p2[1]], color=color, lw=lw, ls=(0, (4, 3)) if dashed else "-",
            solid_capstyle="butt", zorder=2)
    if head == "none":
        return
    ang = math.atan2(p2[1] - p1[1], p2[0] - p1[0])
    size = 0.13
    left = (p2[0] - size * math.cos(ang - 0.42), p2[1] - size * math.sin(ang - 0.42))
    right = (p2[0] - size * math.cos(ang + 0.42), p2[1] - size * math.sin(ang + 0.42))
    if head == "open":
        ax.plot([left[0], p2[0], right[0]], [left[1], p2[1], right[1]], color=color, lw=lw, zorder=3)
    else:
        ax.add_patch(Polygon([left, p2, right], closed=True, fc="white" if head == "triangle" else color,
                             ec=color, lw=lw, zorder=3))


def _diamond(ax, at, toward, filled):
    ang = math.atan2(toward[1] - at[1], toward[0] - at[0])
    s, w = 0.22, 0.08
    tip = at
    mid = (at[0] + s / 2 * math.cos(ang), at[1] + s / 2 * math.sin(ang))
    far = (at[0] + s * math.cos(ang), at[1] + s * math.sin(ang))
    l = (mid[0] - w * math.sin(ang), mid[1] + w * math.cos(ang))
    r = (mid[0] + w * math.sin(ang), mid[1] - w * math.cos(ang))
    ax.add_patch(Polygon([tip, l, far, r], closed=True, fc=LINE if filled else "white", ec=LINE, lw=1.1,
                         zorder=4))
    return far


def _edge_point(box, toward):
    """Giao điểm của đoạn tâm hộp -> toward với cạnh hộp (x, y, w, h)."""
    x, y, w, h = box
    cx, cy = x + w / 2, y + h / 2
    dx, dy = toward[0] - cx, toward[1] - cy
    if dx == 0 and dy == 0:
        return cx, cy
    tx = (w / 2) / abs(dx) if dx else math.inf
    ty = (h / 2) / abs(dy) if dy else math.inf
    t = min(tx, ty)
    return cx + dx * t, cy + dy * t


def _label(ax, x, y, s, pt=9, italic=False, color=INK, bg=True):
    ax.text(x, y, s, ha="center", va="center", fontsize=pt, style="italic" if italic else "normal", color=color,
            zorder=6, bbox=dict(fc="white", ec="none", pad=0.6) if bg else None)


# ---------------------------------------------------------------------------------------------------- use case
def _actor(ax, x, y, name):
    ax.add_patch(plt.Circle((x, y + 0.42), 0.12, fc="white", ec=INK, lw=1.3, zorder=4))
    ax.plot([x, x], [y + 0.30, y - 0.05], color=INK, lw=1.3, zorder=4)
    ax.plot([x - 0.22, x + 0.22], [y + 0.18, y + 0.18], color=INK, lw=1.3, zorder=4)
    ax.plot([x - 0.18, x, x + 0.18], [y - 0.35, y - 0.05, y - 0.35], color=INK, lw=1.3, zorder=4)
    ax.text(x, y - 0.52, name, ha="center", va="top", fontsize=10, fontweight="bold", color=INK, zorder=4)


def usecase(path: Path, title: str, actors: list, cases: list, links: list, cols: int = 2,
            col_w: float = 3.2, row_h: float = 0.78) -> Path:
    """actors = [(id, tên, 'left'|'right', hàng)]; cases = [(id, nhãn, cột, hàng)];
    links = [(a, b, 'assoc'|'include'|'extend')]. Hàng có thể là số thực để căn giữa."""
    rows = max([c[3] for c in cases] + [a[3] for a in actors]) + 1
    has_right = any(a[2] == "right" for a in actors)
    margin_l, margin_r = 1.4, (1.4 if has_right else 0.4)
    box_w = cols * col_w + 0.4
    W = margin_l + box_w + margin_r
    H = rows * row_h + 1.0
    fig, ax = _canvas(W, H)
    bx, by = margin_l, 0.25
    ax.add_patch(Rectangle((bx, by), box_w, H - 0.5, fc="#FAFBFD", ec=GREY_E, lw=1.3, zorder=0))
    ax.text(bx + box_w / 2, H - 0.42, title, ha="center", va="center", fontsize=11, fontweight="bold", color=INK)

    def row_y(r):
        return H - 0.95 - r * row_h

    pos = {}
    for cid, label, col, row in cases:
        cx, cy = bx + 0.2 + col * col_w + col_w / 2, row_y(row)
        lines = label.split("\n")
        ew = max(text_w(s, 9.5) for s in lines) + 0.55
        ew = min(max(ew, 1.9), col_w - 0.15)
        eh = 0.5 if len(lines) == 1 else 0.62
        ax.add_patch(Ellipse((cx, cy), ew, eh, fc=BLUE, ec=BLUE_E, lw=1.2, zorder=3))
        ax.text(cx, cy, label, ha="center", va="center", fontsize=9.5, color=INK, zorder=4, linespacing=1.05)
        pos[cid] = ("uc", cx, cy, ew / 2, eh / 2)
    for aid, name, side, row in actors:
        ax_x = 0.7 if side == "left" else W - 0.7
        _actor(ax, ax_x, row_y(row), name)
        pos[aid] = ("actor", ax_x, row_y(row) + 0.1, 0.3, 0.45)

    def anchor(item, toward):
        kind, cx, cy, rx, ry = item
        ang = math.atan2(toward[1] - cy, toward[0] - cx)
        if kind == "uc":
            return cx + rx * math.cos(ang), cy + ry * math.sin(ang)
        return cx + rx * math.copysign(1, math.cos(ang)), cy

    for a, b, kind in links:
        pa, pb = pos[a], pos[b]
        p1 = anchor(pa, (pb[1], pb[2]))
        p2 = anchor(pb, (pa[1], pa[2]))
        if kind == "assoc":
            _arrow(ax, p1, p2, head="none", lw=1.1)
        else:
            _arrow(ax, p1, p2, dashed=True, head="open", lw=1.0)
            _label(ax, (p1[0] + p2[0]) / 2, (p1[1] + p2[1]) / 2, f"«{kind}»", pt=8, italic=True)
    return _save(fig, path)


# ---------------------------------------------------------------------------------------------------- lớp
def class_diagram(path: Path, classes: list, relations: list, col_w: float = 3.3, row_gap: float = 0.55,
                  pt: float = 8.5) -> Path:
    """classes = [(tên, stereotype|None, [thuộc tính], [phương thức], cột, hàng, màu)];
    relations = [(a, b, kiểu, nhãn)] với kiểu: assoc, dep, comp, agg, impl, inh.
    comp/agg: hình thoi đặt ở phía a (a chứa b)."""
    line_h = pt * 1.55 / 72
    sizes = {}
    for name, stereo, attrs, ops, col, row, _ in classes:
        texts = [name] + attrs + ops + ([f"«{stereo}»"] if stereo else [])
        w = max(text_w(s, pt) for s in texts) + 0.3
        h = line_h * (1 + (1 if stereo else 0)) + 0.12 + line_h * max(len(attrs), 0.5) + 0.12 \
            + line_h * max(len(ops), 0.5) + 0.12
        sizes[name] = (min(w, col_w - 0.25), h)
    rows = sorted({c[5] for c in classes})
    row_h = {r: max(sizes[c[0]][1] for c in classes if c[5] == r) for r in rows}
    ncols = max(c[4] for c in classes) + 1
    W = ncols * col_w + 0.3
    H = sum(row_h.values()) + row_gap * (len(rows) + 1)
    fig, ax = _canvas(W, H)
    top = {}
    y = H - row_gap
    for r in rows:
        top[r] = y
        y -= row_h[r] + row_gap
    boxes = {}
    for name, stereo, attrs, ops, col, row, color in classes:
        w, h = sizes[name]
        x = 0.15 + col * col_w + (col_w - w) / 2
        yt = top[row] - (row_h[row] - h) / 2
        fc, ec = PALETTE[color]
        ax.add_patch(Rectangle((x, yt - h), w, h, fc=fc, ec=ec, lw=1.3, zorder=3))
        cy = yt - 0.06
        if stereo:
            ax.text(x + w / 2, cy - line_h / 2, f"«{stereo}»", ha="center", va="center", fontsize=pt - 0.5,
                    style="italic", color=INK, zorder=4)
            cy -= line_h
        ax.text(x + w / 2, cy - line_h / 2, name, ha="center", va="center", fontsize=pt + 1, fontweight="bold",
                color=INK, zorder=4)
        cy -= line_h + 0.06
        ax.plot([x, x + w], [cy, cy], color=ec, lw=1, zorder=4)
        cy -= 0.06
        for a in attrs:
            ax.text(x + 0.1, cy - line_h / 2, a, ha="left", va="center", fontsize=pt, color=INK, zorder=4)
            cy -= line_h
        if not attrs:
            cy -= line_h * 0.5
        cy -= 0.06
        ax.plot([x, x + w], [cy, cy], color=ec, lw=1, zorder=4)
        cy -= 0.06
        for o in ops:
            ax.text(x + 0.1, cy - line_h / 2, o, ha="left", va="center", fontsize=pt, color=INK, zorder=4)
            cy -= line_h
        boxes[name] = (x, yt - h, w, h)
    for a, b, kind, label in relations:
        ba, bb = boxes[a], boxes[b]
        ca = (ba[0] + ba[2] / 2, ba[1] + ba[3] / 2)
        cb = (bb[0] + bb[2] / 2, bb[1] + bb[3] / 2)
        p1, p2 = _edge_point(ba, cb), _edge_point(bb, ca)
        if kind in ("comp", "agg"):
            start = _diamond(ax, p1, p2, filled=kind == "comp")
            _arrow(ax, start, p2, head="none")
        elif kind in ("impl", "inh"):
            _arrow(ax, p1, p2, dashed=kind == "impl", head="triangle")
        else:
            _arrow(ax, p1, p2, dashed=kind == "dep", head="open")
        if label:
            _label(ax, (p1[0] + p2[0]) / 2, (p1[1] + p2[1]) / 2, label, pt=7.5, italic=True)
    return _save(fig, path)


# ---------------------------------------------------------------------------------------------------- tuần tự
def sequence(path: Path, participants: list, messages: list, gap: float = 2.15, step: float = 0.42,
             frames: list | None = None) -> Path:
    """participants = [(id, nhãn, 'actor'|'object')];
    messages = [(từ, tới, nhãn, 'sync'|'return'|'self')] hoặc ('note', id, nội dung);
    frames = [(chỉ số msg đầu, chỉ số msg cuối, nhãn)] (vd vòng lặp, alt)."""
    n = len(participants)
    W = gap * n + 0.2
    H = 1.35 + step * (len(messages) + 1.2)
    fig, ax = _canvas(W, H)
    xs = {pid: 0.1 + gap * (i + 0.5) for i, (pid, _, _) in enumerate(participants)}
    head_y = H - 0.5
    bottom = 0.25
    for pid, label, kind in participants:
        x = xs[pid]
        if kind == "actor":
            ax.add_patch(plt.Circle((x, head_y + 0.27), 0.08, fc="white", ec=INK, lw=1.1, zorder=4))
            ax.plot([x, x], [head_y + 0.19, head_y - 0.02], color=INK, lw=1.1)
            ax.plot([x - 0.13, x + 0.13], [head_y + 0.12, head_y + 0.12], color=INK, lw=1.1)
            ax.plot([x - 0.1, x, x + 0.1], [head_y - 0.16, head_y - 0.02, head_y - 0.16], color=INK, lw=1.1)
            ax.text(x, head_y - 0.27, label, ha="center", va="center", fontsize=8.5, fontweight="bold")
        else:
            w = min(max(text_w(label, 8.5) + 0.3, 1.1), gap - 0.15)
            ax.add_patch(Rectangle((x - w / 2, head_y - 0.38), w, 0.5, fc=BLUE, ec=BLUE_E, lw=1.2, zorder=4))
            ax.text(x, head_y - 0.13, label, ha="center", va="center", fontsize=8.5, fontweight="bold", zorder=5)
        ax.plot([x, x], [head_y - 0.4, bottom], color=GREY_E, lw=0.9, ls=(0, (3, 3)), zorder=1)
    ys = [head_y - 0.75 - step * i for i in range(len(messages))]
    for (i, j, label) in frames or []:
        y1, y2 = ys[i] + step * 0.62, ys[j] - step * 0.45
        ax.add_patch(Rectangle((0.15, y2), W - 0.3, y1 - y2, fc="none", ec=PURPLE_E, lw=1.0, zorder=1))
        lw_ = text_w(label, 8) + 0.25
        ax.add_patch(Polygon([(0.15, y1), (0.15 + lw_, y1), (0.15 + lw_, y1 - 0.14), (0.05 + lw_, y1 - 0.24),
                              (0.15, y1 - 0.24)], closed=True, fc=PURPLE, ec=PURPLE_E, lw=1.0, zorder=2))
        ax.text(0.22, y1 - 0.12, label, ha="left", va="center", fontsize=8, fontweight="bold", color=PURPLE_E)
    for k, msg in enumerate(messages):
        y = ys[k]
        if msg[0] == "note":
            x = xs[msg[1]]
            w = text_w(msg[2], 8) + 0.3
            ax.add_patch(Rectangle((x - w / 2, y - 0.14), w, 0.28, fc="#FFFBE6", ec="#C9A227", lw=0.9, zorder=5))
            ax.text(x, y, msg[2], ha="center", va="center", fontsize=8, color=INK, zorder=6)
            continue
        a, b, label, kind = msg
        xa, xb = xs[a], xs[b]
        if kind == "self":
            ax.plot([xa, xa + 0.35, xa + 0.35, xa + 0.04], [y + 0.08, y + 0.08, y - 0.12, y - 0.12], color=LINE,
                    lw=1.1, zorder=3)
            _arrow(ax, (xa + 0.2, y - 0.12), (xa + 0.03, y - 0.12), head="open", lw=1.1)
            ax.text(xa + 0.42, y - 0.02, label, ha="left", va="center", fontsize=8, color=INK, zorder=6)
            continue
        _arrow(ax, (xa, y), (xb + (-0.03 if xb > xa else 0.03), y), dashed=kind == "return",
               head="open" if kind == "return" else "filled", lw=1.1)
        ax.text((xa + xb) / 2, y + 0.1, label, ha="center", va="bottom", fontsize=8, color=INK, zorder=6,
                bbox=dict(fc="white", ec="none", pad=0.4))
    return _save(fig, path)


# ---------------------------------------------------------------------------------------------------- ER
def er(path: Path, entities: list, relations: list, W: float, H: float, pt: float = 9) -> Path:
    """entities = [(tên, [(thuộc tính, 'PK'|'FK'|'PK,FK'|'', kiểu)], x, y_top, màu)];
    relations = [(e1, e2, card1, card2, tên quan hệ, (dx, dy) lệch nhãn)]."""
    fig, ax = _canvas(W, H)
    line_h = pt * 1.6 / 72
    boxes = {}
    for name, attrs, x, ytop, color in entities:
        w = max(text_w(f"{k}  {a}  {t}", pt) for a, k, t in attrs) + 0.6
        w = max(w, text_w(name, pt + 2) + 0.4)
        h = line_h * (len(attrs) + 1.3)
        fc, ec = PALETTE[color]
        ax.add_patch(Rectangle((x, ytop - h), w, h, fc="white", ec=ec, lw=1.4, zorder=3))
        ax.add_patch(Rectangle((x, ytop - line_h * 1.3), w, line_h * 1.3, fc=fc, ec=ec, lw=1.4, zorder=3))
        ax.text(x + w / 2, ytop - line_h * 0.65, name, ha="center", va="center", fontsize=pt + 1.5,
                fontweight="bold", zorder=4)
        for i, (a, key, t) in enumerate(attrs):
            yy = ytop - line_h * (1.3 + i + 0.5)
            ax.text(x + 0.1, yy, key, ha="left", va="center", fontsize=pt - 1, fontweight="bold",
                    color="#B03A2E" if "PK" in key else BLUE_E, zorder=4)
            ax.text(x + 0.62, yy, a, ha="left", va="center", fontsize=pt, zorder=4,
                    fontweight="bold" if "PK" in key else "normal")
            ax.text(x + w - 0.1, yy, t, ha="right", va="center", fontsize=pt - 1, color=GREY_E, zorder=4)
        boxes[name] = (x, ytop - h, w, h)
    for e1, e2, c1, c2, label, off in relations:
        b1, b2 = boxes[e1], boxes[e2]
        c1p = (b1[0] + b1[2] / 2, b1[1] + b1[3] / 2)
        c2p = (b2[0] + b2[2] / 2, b2[1] + b2[3] / 2)
        p1, p2 = _edge_point(b1, c2p), _edge_point(b2, c1p)
        _arrow(ax, p1, p2, head="none", lw=1.3)
        mx, my = (p1[0] + p2[0]) / 2 + off[0], (p1[1] + p2[1]) / 2 + off[1]
        dw = text_w(label, 8.5) / 2 + 0.3
        ax.add_patch(Polygon([(mx - dw, my), (mx, my + 0.22), (mx + dw, my), (mx, my - 0.22)], closed=True,
                             fc=ORANGE, ec=ORANGE_E, lw=1.1, zorder=5))
        ax.text(mx, my, label, ha="center", va="center", fontsize=8.5, zorder=6)
        for p, q, c in ((p1, p2, c1), (p2, p1, c2)):
            ang = math.atan2(q[1] - p[1], q[0] - p[0])
            _label(ax, p[0] + 0.28 * math.cos(ang) - 0.14 * math.sin(ang),
                   p[1] + 0.28 * math.sin(ang) + 0.14 * math.cos(ang), c, pt=9, color="#B03A2E")
    return _save(fig, path)


# ---------------------------------------------------------------------------------------------------- sơ đồ khối
def blocks(path: Path, root: str, groups: list, color: str = "blue", per_row: int = 3,
           col_w: float = 2.55, item_pt: float = 8.5) -> Path:
    """Sơ đồ khối chức năng: hộp gốc ở trên, các nhóm chức năng bên dưới, mỗi nhóm liệt kê chức năng con.
    groups = [(tên nhóm, [chức năng con])]."""
    fc, ec = PALETTE[color]
    nrows = math.ceil(len(groups) / per_row)
    line_h = item_pt * 1.55 / 72
    gh = max(len(g[1]) for g in groups) * line_h + 0.62
    W = per_row * col_w + 0.3
    H = 1.15 + nrows * (gh + 0.55)
    fig, ax = _canvas(W, H)
    rw = text_w(root, 11) + 0.8
    ax.add_patch(FancyBboxPatch((W / 2 - rw / 2, H - 0.75), rw, 0.5, boxstyle="round,pad=0,rounding_size=0.08",
                                fc=ec, ec=ec, lw=1.3, zorder=3))
    ax.text(W / 2, H - 0.5, root, ha="center", va="center", fontsize=11, fontweight="bold", color="white",
            zorder=4)
    bus_y = H - 1.0
    ax.plot([W / 2, W / 2], [H - 0.75, bus_y], color=LINE, lw=1.1)
    for r in range(nrows):
        row = groups[r * per_row:(r + 1) * per_row]
        top = bus_y - 0.3 - r * (gh + 0.55)
        offs = (per_row - len(row)) * col_w / 2
        xs = [0.15 + offs + i * col_w + col_w / 2 for i in range(len(row))]
        line_y = top + 0.3 if r == 0 else top + 0.25
        if r > 0:
            ax.plot([W / 2, W / 2], [bus_y, line_y], color=LINE, lw=1.1, zorder=1)
        ax.plot([min(xs + [W / 2]), max(xs + [W / 2])], [line_y, line_y], color=LINE, lw=1.1, zorder=1)
        for x, (title, items) in zip(xs, row):
            ax.plot([x, x], [line_y, top], color=LINE, lw=1.1, zorder=1)
            w = col_w - 0.25
            ax.add_patch(Rectangle((x - w / 2, top - gh), w, gh, fc="white", ec=ec, lw=1.2, zorder=3))
            ax.add_patch(Rectangle((x - w / 2, top - 0.42), w, 0.42, fc=fc, ec=ec, lw=1.2, zorder=3))
            ax.text(x, top - 0.21, title, ha="center", va="center", fontsize=9.5, fontweight="bold", zorder=4)
            for i, it in enumerate(items):
                ax.text(x - w / 2 + 0.12, top - 0.6 - i * line_h, f"• {it}", ha="left", va="center",
                        fontsize=item_pt, zorder=4)
    return _save(fig, path)


def deployment(path: Path) -> Path:
    """Kiến trúc triển khai: người dùng – máy client – mạng LAN/TCP – máy server – MySQL."""
    W, H = 10.8, 4.6
    fig, ax = _canvas(W, H)

    def node(x, y, w, h, title, lines, color):
        fc, ec = PALETTE[color]
        ax.add_patch(FancyBboxPatch((x, y), w, h, boxstyle="round,pad=0,rounding_size=0.1", fc=fc, ec=ec, lw=1.4,
                                    zorder=3))
        ax.text(x + w / 2, y + h - 0.25, title, ha="center", va="center", fontsize=10, fontweight="bold", zorder=4)
        for i, ln in enumerate(lines):
            ax.text(x + w / 2, y + h - 0.6 - i * 0.26, ln, ha="center", va="center", fontsize=8.5, zorder=4)

    for i, (who, y) in enumerate((("Người chơi A", 3.0), ("Người chơi B", 0.85))):
        _actor(ax, 0.55, y + 0.35, who)
        node(1.25, y - 0.15, 2.3, 1.25, f"Máy client {'AB'[i]}", ["racing-client.jar", "Java Swing + FlatLaf",
                                                                 "NetworkClient (TCP)"], "blue")
        _arrow(ax, (0.85, y + 0.45), (1.23, y + 0.45), head="filled")
        p1, p2 = (3.57, y + 0.47), (5.78, 2.55 if i == 0 else 1.95)
        _arrow(ax, p1, p2, head="filled")
        _arrow(ax, p2, p1, head="filled")
    _label(ax, 4.7, 3.45, "TCP :5000\nMessage (ObjectStream)", pt=8, italic=True, bg=False)
    _label(ax, 4.7, 1.0, "CAR_STATE, PING 5 s\nRACE_UPDATE 50 ms", pt=8, italic=True, bg=False)
    node(5.8, 1.05, 2.4, 2.4, "Máy server", ["racing-server.jar", "GameServer :5000", "ClientHandler / phiên",
                                             "RoomManager / CarSim", "MatchService (JDBC)"], "orange")
    _arrow(ax, (8.22, 2.25), (8.88, 2.25), head="filled")
    _label(ax, 8.55, 2.55, "JDBC :3306", pt=8, italic=True)
    node(8.9, 1.45, 1.75, 1.6, "MySQL 8", ["CSDL racing", "players, matches", "match_events"], "green")
    return _save(fig, path)
