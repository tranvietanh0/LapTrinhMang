# -*- coding: utf-8 -*-
"""Build the BTL report docx (Vietnamese, Times New Roman 13, A4) with python-docx."""
import os
from docx import Document
from docx.shared import Pt, Cm, Inches, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH, WD_LINE_SPACING, WD_BREAK
from docx.enum.table import WD_TABLE_ALIGNMENT, WD_CELL_VERTICAL_ALIGNMENT
from docx.enum.section import WD_ORIENT
from docx.oxml.ns import qn
from docx.oxml import OxmlElement

HERE = os.path.dirname(os.path.abspath(__file__))
IMG = os.path.join(HERE, "img")
OUT = os.path.join(HERE, "LTM_Nhom_GameDuaXe_Java.docx")

FONT = "Times New Roman"
BODY_PT = 13
CODE_FONT = "Consolas"
HEADER_FILL = "DCE6F1"
CODE_FILL = "F3F3F3"
CONTENT_WIDTH_IN = 6.27  # A4 minus 1in margins

doc = Document()

# ---------------------------------------------------------------- page setup
sec = doc.sections[0]
sec.page_width = Cm(21.0)
sec.page_height = Cm(29.7)
for side in ("left_margin", "right_margin", "top_margin", "bottom_margin"):
    setattr(sec, side, Inches(1))


def set_font(run, name=FONT, size=BODY_PT, bold=None, italic=None, color=None):
    run.font.name = name
    run.font.size = Pt(size)
    rpr = run._element.get_or_add_rPr()
    rfonts = rpr.find(qn("w:rFonts"))
    if rfonts is None:
        rfonts = OxmlElement("w:rFonts")
        rpr.insert(0, rfonts)
    for attr in ("w:ascii", "w:hAnsi", "w:cs", "w:eastAsia"):
        rfonts.set(qn(attr), name)
    if bold is not None:
        run.font.bold = bold
    if italic is not None:
        run.font.italic = italic
    if color is not None:
        run.font.color.rgb = RGBColor.from_string(color)


def style_base(style, size=BODY_PT, bold=False, color="000000"):
    style.font.name = FONT
    style.font.size = Pt(size)
    style.font.bold = bold
    style.font.color.rgb = RGBColor.from_string(color)
    rpr = style.element.get_or_add_rPr()
    rfonts = rpr.find(qn("w:rFonts"))
    if rfonts is None:
        rfonts = OxmlElement("w:rFonts")
        rpr.insert(0, rfonts)
    for attr in ("w:ascii", "w:hAnsi", "w:cs", "w:eastAsia"):
        rfonts.set(qn(attr), FONT)
    # drop theme fonts so Word does not override
    for attr in ("w:asciiTheme", "w:hAnsiTheme", "w:cstheme", "w:eastAsiaTheme"):
        if rfonts.get(qn(attr)) is not None:
            del rfonts.attrib[qn(attr)]


normal = doc.styles["Normal"]
style_base(normal)
normal.paragraph_format.space_after = Pt(6)
normal.paragraph_format.line_spacing = 1.15

for name, size in (("Heading 1", 14), ("Heading 2", 13), ("Heading 3", 13)):
    st = doc.styles[name]
    style_base(st, size=size, bold=True)
    st.font.italic = name == "Heading 3"
    st.paragraph_format.space_before = Pt(14 if name == "Heading 1" else 8)
    st.paragraph_format.space_after = Pt(6)
    st.paragraph_format.keep_with_next = True

for name in ("List Bullet", "List Number"):
    style_base(doc.styles[name])
    doc.styles[name].paragraph_format.space_after = Pt(3)

# ---------------------------------------------------------------- helpers
fig_no = [0]
tab_no = [0]


def heading(text, level=1):
    return doc.add_heading(text, level=level)


def para(text, align="justify", bold=False, italic=False, size=BODY_PT, space_after=6, indent=True):
    p = doc.add_paragraph()
    if align == "justify":
        p.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
    elif align == "center":
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    elif align == "left":
        p.alignment = WD_ALIGN_PARAGRAPH.LEFT
    if indent and align == "justify":
        p.paragraph_format.first_line_indent = Cm(1.0)
    p.paragraph_format.space_after = Pt(space_after)
    r = p.add_run(text)
    set_font(r, size=size, bold=bold, italic=italic)
    return p


def rich(parts, align="justify", indent=True):
    """parts: list of (text, bold) tuples."""
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY if align == "justify" else WD_ALIGN_PARAGRAPH.LEFT
    if indent:
        p.paragraph_format.first_line_indent = Cm(1.0)
    for text, b in parts:
        set_font(p.add_run(text), bold=b)
    return p


def bullets(items, style="List Bullet"):
    for it in items:
        p = doc.add_paragraph(style=style)
        p.paragraph_format.left_indent = Cm(1.25)
        p.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
        if isinstance(it, tuple):
            set_font(p.add_run(it[0]), bold=True)
            set_font(p.add_run(it[1]))
        else:
            set_font(p.add_run(it))


def shade(cell, fill):
    tcpr = cell._element.get_or_add_tcPr()
    shd = OxmlElement("w:shd")
    shd.set(qn("w:val"), "clear")
    shd.set(qn("w:color"), "auto")
    shd.set(qn("w:fill"), fill)
    tcpr.append(shd)


def cell_margins(table, top=60, bottom=60, left=100, right=100):
    tblpr = table._element.tblPr
    mar = OxmlElement("w:tblCellMar")
    for k, v in (("top", top), ("left", left), ("bottom", bottom), ("right", right)):
        e = OxmlElement(f"w:{k}")
        e.set(qn("w:w"), str(v))
        e.set(qn("w:type"), "dxa")
        mar.append(e)
    tblpr.append(mar)


def fix_layout(t, widths_in):
    tblpr = t._element.tblPr
    lay = OxmlElement("w:tblLayout")
    lay.set(qn("w:type"), "fixed")
    tblpr.append(lay)
    tblw = tblpr.find(qn("w:tblW"))
    if tblw is None:
        tblw = OxmlElement("w:tblW")
        tblpr.append(tblw)
    tblw.set(qn("w:type"), "dxa")
    tblw.set(qn("w:w"), str(int(sum(widths_in) * 1440)))
    for i, w in enumerate(widths_in):
        t.columns[i].width = Inches(w)


def no_split(row):
    trpr = row._tr.get_or_add_trPr()
    e = OxmlElement("w:cantSplit")
    e.set(qn("w:val"), "true")
    trpr.append(e)


def repeat_header(row):
    trpr = row._tr.get_or_add_trPr()
    e = OxmlElement("w:tblHeader")
    e.set(qn("w:val"), "true")
    trpr.append(e)


def table(headers, rows, widths_in, caption=None, size=12, first_col_bold=False, align_center_cols=()):
    if caption:
        tab_no[0] += 1
        p = doc.add_paragraph()
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
        p.paragraph_format.space_after = Pt(4)
        p.paragraph_format.keep_with_next = True
        set_font(p.add_run(f"Bảng {tab_no[0]}. {caption}"), bold=True, size=12)
    t = doc.add_table(rows=1, cols=len(headers))
    t.style = "Table Grid"
    t.alignment = WD_TABLE_ALIGNMENT.CENTER
    t.autofit = False
    cell_margins(t)
    fix_layout(t, widths_in)
    hdr = t.rows[0]
    repeat_header(hdr)
    for i, h in enumerate(headers):
        c = hdr.cells[i]
        c.width = Inches(widths_in[i])
        c.vertical_alignment = WD_CELL_VERTICAL_ALIGNMENT.CENTER
        shade(c, HEADER_FILL)
        p = c.paragraphs[0]
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
        p.paragraph_format.space_after = Pt(0)
        set_font(p.add_run(h), bold=True, size=size)
    for r in rows:
        row = t.add_row()
        no_split(row)
        cells = row.cells
        for i, val in enumerate(r):
            c = cells[i]
            c.width = Inches(widths_in[i])
            c.vertical_alignment = WD_CELL_VERTICAL_ALIGNMENT.CENTER
            p = c.paragraphs[0]
            p.paragraph_format.space_after = Pt(0)
            p.paragraph_format.line_spacing = 1.0
            if i in align_center_cols:
                p.alignment = WD_ALIGN_PARAGRAPH.CENTER
            lines = val if isinstance(val, list) else [val]
            for k, ln in enumerate(lines):
                if k > 0:
                    p = c.add_paragraph()
                    p.paragraph_format.space_after = Pt(0)
                    p.paragraph_format.line_spacing = 1.0
                set_font(p.add_run(ln), size=size, bold=(first_col_bold and i == 0))
    doc.add_paragraph().paragraph_format.space_after = Pt(2)
    return t


def figure(filename, caption, width_in=CONTENT_WIDTH_IN):
    fig_no[0] += 1
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.space_after = Pt(2)
    p.paragraph_format.keep_with_next = True
    p.add_run().add_picture(os.path.join(IMG, filename), width=Inches(width_in))
    c = doc.add_paragraph()
    c.alignment = WD_ALIGN_PARAGRAPH.CENTER
    c.paragraph_format.space_after = Pt(8)
    set_font(c.add_run(f"Hình {fig_no[0]}. {caption}"), italic=True, size=12)


def code(lines, size=9):
    t = doc.add_table(rows=1, cols=1)
    t.style = "Table Grid"
    t.alignment = WD_TABLE_ALIGNMENT.CENTER
    t.autofit = False
    cell_margins(t, top=80, bottom=80, left=140, right=140)
    fix_layout(t, [CONTENT_WIDTH_IN])
    no_split(t.rows[0])
    c = t.rows[0].cells[0]
    c.width = Inches(CONTENT_WIDTH_IN)
    shade(c, CODE_FILL)
    p = c.paragraphs[0]
    for i, ln in enumerate(lines):
        if i > 0:
            p = c.add_paragraph()
        p.paragraph_format.space_after = Pt(0)
        p.paragraph_format.line_spacing = 1.0
        set_font(p.add_run(ln if ln else " "), name=CODE_FONT, size=size)
    doc.add_paragraph().paragraph_format.space_after = Pt(2)


def page_break():
    doc.add_paragraph().add_run().add_break(WD_BREAK.PAGE)


def add_page_number(section):
    footer = section.footer
    p = footer.paragraphs[0]
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r = p.add_run()
    set_font(r, size=11)
    for tag, text in (("begin", None), (None, "PAGE"), ("end", None)):
        if tag:
            e = OxmlElement("w:fldChar")
            e.set(qn("w:fldCharType"), tag)
        else:
            e = OxmlElement("w:instrText")
            e.set(qn("xml:space"), "preserve")
            e.text = text
        r._r.append(e)


add_page_number(sec)

# ================================================================ TITLE
for txt, sz in (("BÀI TẬP LỚN", 16), ("MÔN LẬP TRÌNH MẠNG", 14)):
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.space_after = Pt(4)
    set_font(p.add_run(txt), bold=True, size=sz)
p = doc.add_paragraph()
p.alignment = WD_ALIGN_PARAGRAPH.CENTER
p.paragraph_format.space_before = Pt(10)
p.paragraph_format.space_after = Pt(4)
set_font(p.add_run("GAME ĐUA XE THI ĐẤU ĐỐI KHÁNG ONLINE"), bold=True, size=18)
p = doc.add_paragraph()
p.alignment = WD_ALIGN_PARAGRAPH.CENTER
p.paragraph_format.space_after = Pt(16)
set_font(p.add_run("Bài tập 2 – Mô tả kiến trúc, luồng và thiết kế hệ thống"), bold=True, size=13)
p = doc.add_paragraph()
p.alignment = WD_ALIGN_PARAGRAPH.CENTER
p.paragraph_format.space_after = Pt(2)
set_font(p.add_run("Lớp: LTM-2026-2-N01 · Ngôn ngữ Java · TCP Socket · Java Swing · MySQL (JDBC)"), italic=True, size=13)
table(["STT", "Thành viên", "Phần phụ trách"], [
    ["1", "Phạm Thị Thu Phương", "Server và kiến trúc (mục 4)"],
    ["2", "Vũ Văn Hiếu", "Client: đăng nhập, sảnh chờ, thách đấu (mục 5)"],
    ["3", "Nguyễn Trần Mai Anh", "Giao diện màn hình đua (mục 6)"],
    ["4", "Trần Việt Anh", "Cơ sở dữ liệu và bảng xếp hạng (mục 7)"],
], [0.6, 2.2, 3.47], caption="Danh sách thành viên nhóm", align_center_cols=(0,))

# ================================================================ 1. BÀI TOÁN
heading("1. Mô tả bài toán và yêu cầu")
para("Đề tài xây dựng một trò chơi đua xe thi đấu đối kháng trực tuyến theo mô hình Client – Server. "
     "Hệ thống có một server và nhiều client. Server lưu toàn bộ thông tin người chơi, dữ liệu trận đấu và bảng "
     "xếp hạng; client là chương trình Java Swing để người chơi đăng nhập, xem danh sách người chơi đang online, "
     "thách đấu và điều khiển xe trong trận. Toàn bộ luật thắng thua, thời gian chờ và việc đồng bộ trạng thái "
     "hai xe do server quyết định, client chỉ hiển thị và gửi thao tác của người chơi.")
para("Các yêu cầu chức năng được tách từ đề bài và đánh mã để tham chiếu trong các phần thiết kế phía sau:", indent=True)

table(["Mã", "Yêu cầu", "Mô tả chi tiết"], [
    ["YC01", "Đăng nhập", "Người chơi đăng nhập từ máy client bằng tài khoản đã đăng ký; server kiểm tra với CSDL."],
    ["YC02", "Danh sách online", "Sau khi đăng nhập, client hiển thị danh sách người chơi đang online gồm tên, tổng điểm và trạng thái (Rảnh / Đang thi đấu)."],
    ["YC03", "Thách đấu", "Người chơi click vào tên đối thủ đang Rảnh trong danh sách để gửi lời mời thách đấu."],
    ["YC04", "Phản hồi lời mời", "Người bị thách đấu chọn Chấp nhận (OK) hoặc Từ chối (Reject); sau 30 giây không phản hồi lời mời tự hủy."],
    ["YC05", "Tạo phòng đua", "Khi chấp nhận, hai người được đưa vào một phòng đua; server làm trọng tài; đếm ngược 3, 2, 1 trước khi bắt đầu."],
    ["YC06", "Đường đua", "Mỗi người điều khiển một xe trên đường đua riêng có cùng chiều dài và chướng ngại vật giống nhau; hai đường đua hiển thị song song trên màn hình."],
    ["YC07", "Điều khiển và đồng bộ", "Người chơi điều khiển xe bằng bàn phím; client liên tục gửi trạng thái xe lên server; server đồng bộ và cập nhật vị trí hai xe cho cả hai người chơi."],
    ["YC08", "Xác định kết quả", "Người về đích trước thắng: thắng 1 điểm, thua 0 điểm. Về đích cùng lúc tính hòa, mỗi người 1 điểm."],
    ["YC09", "Thi đấu tiếp", "Sau mỗi trận server hỏi hai người có muốn thi đấu tiếp; cả hai đồng ý thì tạo ván mới, một người từ chối thì kết thúc."],
    ["YC10", "Thoát trận", "Người chơi có thể thoát khỏi trận bất cứ lúc nào; hệ thống báo cho người còn lại, xử thua người thoát và kết thúc trận."],
    ["YC11", "Mất kết nối", "Server phát hiện client mất mạng hoặc tắt ứng dụng sau khoảng timeout, xử thua người đó và thông báo cho đối thủ."],
    ["YC12", "Bảng xếp hạng", "Kết quả các trận được lưu tại server; mọi người chơi xem được bảng xếp hạng sắp xếp theo tổng điểm giảm dần rồi tổng số trận thắng giảm dần."],
], [0.7, 1.45, 4.12], caption="Yêu cầu chức năng của hệ thống", first_col_bold=True, align_center_cols=(0,))

para("Ngoài các yêu cầu chức năng, hệ thống phải đáp ứng một số yêu cầu phi chức năng: viết bằng Java, dùng "
     "TCP Socket để bảo đảm thông điệp đến đúng thứ tự và không mất; server phục vụ đồng thời nhiều client bằng "
     "đa luồng; dữ liệu lâu dài lưu trong MySQL; độ trễ đồng bộ vị trí xe đủ nhỏ để hai người chơi thấy trận đấu "
     "diễn ra mượt mà (khoảng 50 ms một lần cập nhật).")

# ================================================================ 2. KIẾN TRÚC CHUNG
heading("2. Kiến trúc chung của hệ thống")
para("Hệ thống sử dụng kiến trúc Client – Server. Nhiều client Java Swing kết nối đến một Game Server qua TCP "
     "Socket. Server là nơi duy nhất quản lý người chơi online, lời mời thách đấu, phòng đua, luật thắng thua, "
     "trạng thái kết nối và kết quả trận. MySQL lưu dữ liệu lâu dài về người chơi, trận đấu và các sự kiện trong trận.")
para("Mỗi client được tổ chức theo mô hình MVC: View hiển thị giao diện Swing, Controller nhận thao tác của người "
     "chơi và giao tiếp mạng, Model chứa dữ liệu Player, Match, RaceState và RaceEvent. Server dùng ServerSocket "
     "để nhận kết nối; mỗi client được xử lý bởi một ClientHandler chạy trên luồng riêng để nhiều người chơi có thể "
     "hoạt động đồng thời. Thông điệp trao đổi là đối tượng Message (kiểu, dữ liệu kèm theo) được gửi qua "
     "ObjectOutputStream và nhận bằng ObjectInputStream.")
figure("fig1_architecture.png", "Kiến trúc tổng thể Client – Server theo MVC")

table(["Thành phần trên hình", "Chức năng", "Mô tả"], [
    ["Client A, B, … N", "Giao diện và gửi sự kiện", "Đăng nhập, xem sảnh, thách đấu, điều khiển xe và hiển thị dữ liệu trận. Mỗi client là một tiến trình Java Swing độc lập."],
    ["Mũi tên Client → Server", "Gửi yêu cầu", "LOGIN, INVITE, INVITE_REPLY, CAR_STATE, FINISH, QUIT_MATCH, REMATCH_REPLY, LEADERBOARD_REQ."],
    ["Game Server", "Điều phối và trọng tài", "Quản lý kết nối, danh sách online, lời mời (timeout 30 s), phòng đua, countdown, đồng bộ vị trí, luật thắng thua, phát hiện mất kết nối."],
    ["Mũi tên Server → Client", "Phản hồi và đồng bộ", "LOGIN_RESULT, ONLINE_LIST, INVITE_RECEIVED, MATCH_START, COUNTDOWN, RACE_UPDATE, MATCH_RESULT, REMATCH_ASK, LEADERBOARD."],
    ["MySQL", "Lưu dữ liệu lâu dài", "Bảng players (tài khoản, điểm, thắng/thua/hòa), matches (kết quả trận), match_events (diễn biến trận)."],
], [1.55, 1.35, 3.37], caption="Vai trò các thành phần trong kiến trúc", first_col_bold=True)

table(["Thành phần", "Công nghệ", "Ghi chú"], [
    ["Ngôn ngữ", "Java 17 (JDK 17 LTS)", "Build bằng Maven, hai module client và server dùng chung module common (Message, model)."],
    ["Giao diện client", "Java Swing", "JFrame, JTable cho sảnh; JPanel tự vẽ (paintComponent) cho màn hình đua; javax.swing.Timer cho đếm ngược."],
    ["Mạng", "java.net.Socket / ServerSocket", "TCP; ObjectInputStream / ObjectOutputStream; cổng mặc định 5000."],
    ["Đa luồng", "Thread, ExecutorService", "Một luồng nhận cho mỗi client; ScheduledExecutorService cho tick trận đấu và hẹn giờ lời mời."],
    ["Cơ sở dữ liệu", "MySQL 8 + JDBC", "Driver mysql-connector-j; PreparedStatement; transaction khi lưu kết quả và cập nhật điểm."],
], [1.35, 1.9, 3.02], caption="Công nghệ sử dụng", first_col_bold=True)

# ================================================================ 3. THIẾT KẾ CHUNG
heading("3. Thiết kế chung của hệ thống")
heading("3.1. Luồng hoạt động tổng thể", 2)
para("Sau khi đăng nhập thành công, người chơi xem danh sách người đang online kèm điểm và trạng thái Rảnh hoặc "
     "Đang thi đấu. Người chơi A chọn B để thách đấu. Server chuyển lời mời đến B và chờ tối đa 30 giây; B có thể "
     "chấp nhận hoặc từ chối, hết thời gian thì lời mời tự hủy và A được thông báo.")
para("Nếu B chấp nhận, server tạo phòng đua, chuyển cả hai client sang màn hình đua và đếm ngược 3, 2, 1. Hai "
     "người cùng thấy hai đường đua song song có cùng chiều dài và cùng bộ chướng ngại vật do server sinh ra. "
     "Trong lúc đua, client gửi trạng thái xe (vị trí, làn, tốc độ) lên server 20 lần mỗi giây; server kiểm tra "
     "hợp lệ, cập nhật vị trí hai xe rồi phát RACE_UPDATE cho cả hai. Khi một xe chạm vạch đích, server xác định "
     "kết quả, lưu vào CSDL, cập nhật điểm và hỏi hai bên có muốn thi đấu tiếp.")
figure("fig2_sequence.png", "Biểu đồ tuần tự từ đăng nhập đến kết thúc trận đấu")

heading("3.2. Giao thức trao đổi thông điệp", 2)
para("Mọi thông điệp giữa client và server đều là đối tượng Message cài đặt Serializable, gồm kiểu thông điệp "
     "(enum MessageType), dữ liệu kèm theo (payload) và thời điểm gửi. Cách này giúp hai phía chỉ cần một vòng "
     "lặp readObject / writeObject duy nhất và dễ mở rộng thêm loại thông điệp mới.")
code([
    "public class Message implements Serializable {",
    "    private static final long serialVersionUID = 1L;",
    "    private final MessageType type;     // LOGIN, INVITE, CAR_STATE, ...",
    "    private final Object payload;       // DTO tương ứng với từng loại",
    "    private final long timestamp = System.currentTimeMillis();",
    "",
    "    public Message(MessageType type, Object payload) {",
    "        this.type = type; this.payload = payload;",
    "    }",
    "    public MessageType getType() { return type; }",
    "    @SuppressWarnings(\"unchecked\")",
    "    public <T> T getPayload() { return (T) payload; }",
    "}",
])
table(["Loại thông điệp", "Hướng", "Dữ liệu kèm theo", "Ý nghĩa"], [
    ["LOGIN", "C → S", "username, password", "Yêu cầu đăng nhập (YC01)."],
    ["LOGIN_RESULT", "S → C", "ok, message, PlayerInfo", "Kết quả đăng nhập; kèm điểm hiện có nếu thành công."],
    ["ONLINE_LIST", "S → C", "List<PlayerInfo>", "Danh sách online: tên, điểm, thắng, trạng thái (YC02). Gửi lại mỗi khi danh sách đổi."],
    ["INVITE", "C → S", "targetUsername", "A thách đấu B (YC03)."],
    ["INVITE_RECEIVED", "S → C", "fromUsername, inviteId", "Server chuyển lời mời tới B; B hiện hộp thoại 30 giây."],
    ["INVITE_REPLY", "C → S", "inviteId, accept", "B chấp nhận hoặc từ chối (YC04)."],
    ["INVITE_RESULT", "S → C", "inviteId, status", "Báo cho A: REJECTED, TIMEOUT hoặc BUSY."],
    ["MATCH_START", "S → C", "roomId, opponent, trackSeed, obstacles", "Vào phòng đua; cả hai nhận cùng danh sách chướng ngại vật (YC05, YC06)."],
    ["COUNTDOWN", "S → C", "value (3, 2, 1, 0)", "Đếm ngược; giá trị 0 nghĩa là bắt đầu đua."],
    ["CAR_STATE", "C → S", "distance, lane, speed", "Trạng thái xe của người chơi, gửi 20 lần/giây (YC07)."],
    ["RACE_UPDATE", "S → C", "RaceState hai xe", "Vị trí, làn, tốc độ, quãng đường của cả hai xe do server đồng bộ."],
    ["FINISH", "C → S", "clientTime", "Client báo xe đã qua vạch đích; server đối chiếu quãng đường trước khi công nhận."],
    ["MATCH_RESULT", "S → C", "winner, reason, điểm mới", "Kết quả trận: WIN, LOSE hoặc DRAW cùng lý do (FINISH, DRAW, QUIT, DISCONNECT) (YC08)."],
    ["REMATCH_ASK", "S → C", "roomId", "Hỏi có thi đấu tiếp không (YC09)."],
    ["REMATCH_REPLY", "C → S", "roomId, agree", "Trả lời thi đấu tiếp; cả hai đồng ý mới tạo ván mới."],
    ["QUIT_MATCH", "C → S", "roomId", "Chủ động thoát trận (YC10)."],
    ["PING / PONG", "C ⇄ S", "—", "Heartbeat mỗi 5 giây để phát hiện mất kết nối (YC11)."],
    ["LEADERBOARD_REQ / LEADERBOARD", "C ⇄ S", "List<RankRow>", "Xem bảng xếp hạng (YC12)."],
    ["LOGOUT", "C → S", "—", "Đăng xuất, server xóa khỏi danh sách online."],
], [1.75, 0.6, 1.6, 2.32], caption="Danh sách thông điệp của giao thức", first_col_bold=True, size=11, align_center_cols=(1,))

heading("3.3. Vòng đời phòng đua tại server", 2)
para("Mỗi phòng đua (Room) tại server là một máy trạng thái. Phòng được tạo khi lời mời được chấp nhận, đếm ngược "
     "ba giây, chuyển sang trạng thái đua, kết thúc khi có kết quả và chỉ quay lại ván mới khi cả hai người chơi "
     "đồng ý. Mọi chuyển trạng thái đều do server thực hiện nên hai client không thể lệch nhau.")
figure("fig7_states.png", "Máy trạng thái của một phòng đua")
table(["Trạng thái", "Điều kiện vào", "Server làm gì"], [
    ["WAITING", "Lời mời được chấp nhận", "Tạo Room, đặt cả hai người chơi sang trạng thái Đang thi đấu, sinh danh sách chướng ngại vật, gửi MATCH_START."],
    ["COUNTDOWN", "Cả hai client báo đã vào phòng", "Gửi COUNTDOWN 3, 2, 1 cách nhau 1 giây; chưa nhận CAR_STATE."],
    ["RACING", "Đếm ngược về 0", "Chạy tick 50 ms: nhận CAR_STATE, kiểm tra hợp lệ, tính va chạm, phát RACE_UPDATE, ghi match_events."],
    ["FINISHED", "Về đích, hòa, thoát hoặc mất kết nối", "Chốt kết quả, lưu matches, cập nhật điểm, gửi MATCH_RESULT rồi REMATCH_ASK."],
    ["CLOSED", "Một bên từ chối hoặc rời phòng", "Trả hai người chơi về trạng thái Rảnh, phát lại ONLINE_LIST, hủy Room."],
], [1.35, 1.6, 3.32], caption="Các trạng thái của phòng đua", first_col_bold=True)

heading("3.4. Luật thắng thua và tính điểm", 2)
para("Server là trọng tài duy nhất. Kết quả được xác định tại tick mà một xe có quãng đường lớn hơn hoặc bằng chiều "
     "dài đường đua. Nếu trong cùng một tick (50 ms) cả hai xe cùng đạt điều kiện thì tính hòa. Điểm được cộng "
     "trong một transaction cùng với việc lưu trận để tránh sai lệch khi có lỗi giữa chừng.")
table(["Tình huống", "Kết quả", "Điểm người A", "Điểm người B", "end_reason"], [
    ["A về đích trước B", "A thắng", "+1", "0", "FINISH"],
    ["A và B về đích trong cùng một tick", "Hòa", "+1", "+1", "DRAW"],
    ["A chủ động thoát trận", "B thắng", "0", "+1", "QUIT"],
    ["A mất kết nối quá timeout (15 giây)", "B thắng", "0", "+1", "DISCONNECT"],
    ["Cả hai cùng mất kết nối", "Hủy trận", "0", "0", "ABORTED"],
], [2.1, 1.0, 0.95, 0.95, 1.27], caption="Luật tính điểm", align_center_cols=(1, 2, 3, 4))

heading("3.5. Xử lý các tình huống đặc biệt", 2)
table(["Tình huống", "Xử lý của server"], [
    ["B từ chối lời mời", "Gửi INVITE_RESULT(REJECTED) cho A; không tạo phòng; cả hai vẫn Rảnh."],
    ["B không phản hồi trong 30 giây", "Hẹn giờ trong InviteManager hết hạn, hủy lời mời, gửi INVITE_RESULT(TIMEOUT) cho A và đóng hộp thoại ở B."],
    ["A mời B nhưng B vừa vào trận khác", "Trả INVITE_RESULT(BUSY); danh sách online được gửi lại để A thấy trạng thái mới."],
    ["Một người thoát trận", "Gửi MATCH_RESULT cho đối thủ với reason = QUIT, xử người thoát thua, lưu trận, đóng phòng."],
    ["Mất kết nối quá timeout", "Không nhận PING trong 15 giây hoặc readObject ném IOException: xử thua, lưu end_reason = DISCONNECT, báo đối thủ."],
    ["Client gửi CAR_STATE bất thường", "Server giới hạn tốc độ tối đa và quãng đường tăng tối đa mỗi tick; giá trị vượt ngưỡng bị cắt về ngưỡng."],
    ["Cả hai chọn thi đấu tiếp", "Tạo ván mới trong cùng phòng: sinh lại chướng ngại vật, đặt lại quãng đường bằng 0, đếm ngược lại."],
    ["Một trong hai từ chối thi đấu tiếp", "Đóng phòng, cả hai trở về sảnh với trạng thái Rảnh."],
], [2.0, 4.27], caption="Xử lý tình huống đặc biệt", first_col_bold=True)

# ================================================================ 4. CÁ NHÂN 1
heading("4. Phần cá nhân 1 – Server và kiến trúc (Phạm Thị Thu Phương)")
para("Phạm Thị Thu Phương (thành viên 1) thực hiện phần server. Server nhận nhiều kết nối TCP, tạo ClientHandler cho từng client, quản "
     "lý danh sách online, lời mời, phòng đua và xác định kết quả trận đấu. Server đọc ghi dữ liệu với MySQL bằng "
     "JDBC thông qua lớp DAO. Toàn bộ trạng thái dùng chung (danh sách online, lời mời, phòng) được giữ trong các "
     "cấu trúc thread-safe để nhiều luồng ClientHandler truy cập đồng thời không gây lỗi.")
figure("fig3_server.png", "Cấu trúc bên trong Game Server", width_in=6.0)
table(["Thành phần trên hình", "Chức năng", "Mô tả"], [
    ["ServerSocket", "Nhận kết nối", "Mở cổng 5000, vòng lặp accept(); mỗi Socket mới được bọc trong một ClientHandler và giao cho ExecutorService."],
    ["ClientHandler", "Xử lý từng client", "Luồng riêng đọc Message bằng ObjectInputStream, chuyển cho bộ xử lý theo type; gửi trả bằng ObjectOutputStream có synchronized."],
    ["SessionManager", "Người chơi online", "ConcurrentHashMap<username, ClientHandler>; trạng thái FREE / IN_MATCH; phát ONLINE_LIST khi có thay đổi; kiểm tra heartbeat."],
    ["InviteManager", "Lời mời thách đấu", "Lưu lời mời đang chờ, hẹn giờ 30 giây bằng ScheduledExecutorService, xử lý accept / reject / timeout."],
    ["RoomManager / MatchService", "Trọng tài trận đấu", "Tạo Room, countdown, tick 50 ms, đồng bộ RACE_UPDATE, xét về đích, hòa, thoát, mất kết nối, hỏi thi đấu tiếp."],
    ["PlayerDAO / MatchDAO", "Truy cập CSDL", "Đăng nhập, lấy bảng xếp hạng, lưu trận và cập nhật điểm trong transaction."],
], [1.55, 1.25, 3.47], caption="Thành phần của server", first_col_bold=True)

heading("4.1. Các lớp chính phía server", 2)
table(["Lớp", "Gói", "Trách nhiệm chính"], [
    ["GameServer", "server", "main(); mở ServerSocket; vòng lặp accept; khởi tạo các manager."],
    ["ClientHandler", "server.net", "implements Runnable; vòng lặp đọc Message; gửi Message; đóng kết nối và báo SessionManager khi lỗi."],
    ["SessionManager", "server.core", "Đăng nhập / đăng xuất, danh sách online, trạng thái người chơi, heartbeat."],
    ["InviteManager", "server.core", "Tạo, hủy, hết hạn lời mời; kiểm tra người được mời đang Rảnh."],
    ["RoomManager", "server.core", "Tạo và hủy Room; ánh xạ người chơi → Room."],
    ["Room", "server.core", "Trạng thái một trận: hai RaceState, danh sách chướng ngại vật, máy trạng thái, tick."],
    ["MatchService", "server.core", "Luật thắng thua, tính điểm, gọi DAO lưu kết quả."],
    ["PlayerDAO, MatchDAO", "server.db", "JDBC với PreparedStatement; DbConnection quản lý kết nối."],
    ["Message, MessageType, các DTO", "common", "Dùng chung cho client và server (PlayerInfo, RaceState, Obstacle, MatchResult…)."],
], [1.7, 1.1, 3.47], caption="Các lớp phía server", first_col_bold=True, size=11)

heading("4.2. Vòng lặp xử lý của ClientHandler", 2)
code([
    "public void run() {",
    "    try (ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {",
    "        socket.setSoTimeout(15_000);      // không nhận gì trong 15 s → mất kết nối",
    "        while (running) {",
    "            Message msg = (Message) in.readObject();",
    "            switch (msg.getType()) {",
    "                case LOGIN        -> sessionManager.login(this, msg.getPayload());",
    "                case INVITE       -> inviteManager.invite(player, msg.getPayload());",
    "                case INVITE_REPLY -> inviteManager.reply(player, msg.getPayload());",
    "                case CAR_STATE    -> roomManager.onCarState(player, msg.getPayload());",
    "                case FINISH       -> roomManager.onFinish(player);",
    "                case QUIT_MATCH   -> roomManager.onQuit(player);",
    "                case REMATCH_REPLY-> roomManager.onRematch(player, msg.getPayload());",
    "                case PING         -> send(new Message(MessageType.PONG, null));",
    "                default           -> log.warn(\"Unknown message {}\", msg.getType());",
    "            }",
    "        }",
    "    } catch (IOException | ClassNotFoundException e) {",
    "        // SocketTimeoutException cũng rơi vào đây",
    "        roomManager.onDisconnect(player);   // xử thua nếu đang trong trận",
    "        sessionManager.logout(player);      // gỡ khỏi danh sách online",
    "    }",
    "}",
])
para("Phát hiện mất kết nối (YC11) dựa trên hai cơ chế bổ sung nhau: client gửi PING mỗi 5 giây, và socket phía "
     "server đặt SoTimeout 15 giây. Khi client tắt ứng dụng, readObject ném IOException ngay; khi client mất mạng "
     "im lặng, SocketTimeoutException xuất hiện sau 15 giây. Cả hai trường hợp cùng đi vào nhánh xử lý "
     "onDisconnect, nên logic xử thua chỉ viết một lần.")
para("Về đồng bộ đa luồng: mỗi Room có khóa riêng (synchronized trên đối tượng Room) để hai ClientHandler của "
     "hai người chơi và luồng tick không đồng thời sửa RaceState; việc gửi Message qua ObjectOutputStream được "
     "bọc trong phương thức send() có synchronized để tránh xen kẽ dữ liệu trên cùng một stream.")

# ================================================================ 5. CÁ NHÂN 2
heading("5. Phần cá nhân 2 – Client: đăng nhập, sảnh chờ và thách đấu (Vũ Văn Hiếu)")
para("Vũ Văn Hiếu (thành viên 2) thực hiện phần client trước khi vào trận: màn hình đăng nhập, sảnh chờ hiển thị danh sách "
     "người chơi online, gửi lời mời thách đấu và hộp thoại nhận lời mời có đếm ngược 30 giây. Client được tổ "
     "chức theo MVC: View là các JFrame / JDialog, Controller là lớp NetworkClient chạy một luồng nhận riêng, "
     "Model là dữ liệu PlayerInfo nhận từ server. Mọi cập nhật giao diện đều được đẩy về luồng Swing bằng "
     "SwingUtilities.invokeLater để không vi phạm quy tắc một luồng của Swing.")
figure("fig4_lobby.png", "Sảnh chờ và hộp thoại nhận lời mời thách đấu")
table(["Thành phần trên hình", "Chức năng", "Mô tả"], [
    ["Thanh tiêu đề", "Thông tin phiên", "Tên người chơi đang đăng nhập và tổng điểm hiện có, cập nhật sau mỗi trận."],
    ["Bảng danh sách online", "Hiển thị YC02", "JTable với các cột Tên, Điểm, Thắng, Trạng thái; dữ liệu là ONLINE_LIST mới nhất từ server."],
    ["Nút Thách đấu", "Gửi INVITE (YC03)", "Chỉ hiện với người Rảnh; click gửi INVITE rồi khóa nút và hiện trạng thái Đang chờ B trả lời."],
    ["Hộp thoại Lời mời thách đấu", "Nhận INVITE_RECEIVED (YC04)", "Hiện ở máy người được mời; thanh tiến trình và số giây còn lại giảm dần từ 30; nút Chấp nhận / Từ chối."],
    ["Nút Bảng xếp hạng", "Gửi LEADERBOARD_REQ (YC12)", "Mở cửa sổ xếp hạng do phần cá nhân 4 cung cấp dữ liệu."],
    ["Nút Làm mới / Đăng xuất", "Tiện ích", "Yêu cầu ONLINE_LIST mới; gửi LOGOUT và quay về màn hình đăng nhập."],
], [1.5, 1.75, 3.02], caption="Thành phần giao diện sảnh chờ", first_col_bold=True)

heading("5.1. Các lớp chính phía client", 2)
table(["Lớp", "Vai trò MVC", "Trách nhiệm chính"], [
    ["LoginFrame", "View", "Nhập username, password; gửi LOGIN; hiện lỗi nếu LOGIN_RESULT thất bại."],
    ["LobbyFrame", "View", "JTable danh sách online; nút Thách đấu, Bảng xếp hạng, Làm mới, Đăng xuất."],
    ["InviteDialog", "View", "JDialog modal hiển thị lời mời với javax.swing.Timer đếm ngược 30 giây; tự đóng khi hết giờ hoặc nhận INVITE_RESULT."],
    ["LeaderboardFrame", "View", "Bảng xếp hạng nhận từ LEADERBOARD."],
    ["NetworkClient", "Controller", "Mở Socket, luồng nhận readObject, gửi Message; phân phối thông điệp tới màn hình đang mở; PING mỗi 5 giây."],
    ["ClientState", "Model", "Người chơi hiện tại, danh sách PlayerInfo, lời mời đang chờ, roomId hiện tại."],
], [1.5, 1.05, 3.72], caption="Các lớp phía client (phần sảnh)", first_col_bold=True, size=11)

heading("5.2. Xử lý lời mời thách đấu ở client", 2)
code([
    "// Nhận INVITE_RECEIVED từ NetworkClient (đã ở luồng Swing)",
    "void onInviteReceived(InviteInfo info) {",
    "    InviteDialog dlg = new InviteDialog(lobbyFrame, info.from(), 30);",
    "    dlg.onAccept(() -> net.send(new Message(MessageType.INVITE_REPLY,",
    "                                            new InviteReply(info.id(), true))));",
    "    dlg.onReject(() -> net.send(new Message(MessageType.INVITE_REPLY,",
    "                                            new InviteReply(info.id(), false))));",
    "    dlg.onTimeout(dlg::dispose);   // server cũng tự hủy sau 30 s, client chỉ đóng hộp thoại",
    "    dlg.setVisible(true);",
    "}",
])
para("Điểm quan trọng là thời hạn 30 giây được tính tại server (InviteManager) chứ không phụ thuộc đồng hồ của "
     "client; bộ đếm trên hộp thoại chỉ để người chơi thấy thời gian còn lại. Nếu người được mời bấm Chấp nhận "
     "sau khi server đã hủy, server trả INVITE_RESULT(TIMEOUT) và client hiển thị thông báo lời mời đã hết hạn.")

# ================================================================ 6. CÁ NHÂN 3
heading("6. Phần cá nhân 3 – Giao diện màn hình đua (Nguyễn Trần Mai Anh)")
para("Nguyễn Trần Mai Anh (thành viên 3) thực hiện giao diện đua. Màn hình chia thành hai đường đua song song: bên trái là xe của "
     "người chơi, bên phải là xe đối thủ. Mỗi đường đua dài 1000 m (quy đổi ra pixel khi vẽ) và chia thành ba làn "
     "để xe có thể né chướng ngại vật; hai đường đua nhận cùng danh sách chướng ngại vật từ MATCH_START nên hoàn "
     "toàn giống nhau. Người chơi chỉ điều khiển xe của mình; dữ liệu xe đối thủ do server đồng bộ qua RACE_UPDATE.")
figure("fig5_race.png", "Giao diện khi hai người đang đua")
table(["Thành phần trên hình", "Chức năng", "Mô tả"], [
    ["Đường đua bên trái", "Xe của bạn", "Vẽ bằng JPanel tùy biến; xe đổi làn theo A / D, tăng giảm tốc theo W / S; vị trí do client dự đoán rồi được server hiệu chỉnh."],
    ["Đường đua bên phải", "Xe đối thủ", "Chỉ hiển thị theo RACE_UPDATE; không nhận phím."],
    ["Vạch xuất phát, vạch đích", "Mốc đường đua", "Xe bắt đầu ở quãng đường 0; qua vạch đích khi quãng đường ≥ 1000 m."],
    ["Chướng ngại vật", "Luật chơi", "Cùng làn và vị trí ở cả hai đường; va chạm làm tốc độ xe về 0 trong 1 giây. Server là nơi xác định va chạm để hai bên không lệch nhau."],
    ["Bảng chỉ số dưới mỗi đường", "HUD", "Tốc độ (km/h), quãng đường đã đi và thanh tiến trình phần trăm của từng xe."],
    ["Vòng tròn đếm ngược", "COUNTDOWN", "Hiện 3, 2, 1 rồi GO ở giữa màn hình; bàn phím chỉ có tác dụng sau GO."],
    ["Nút Thoát trận", "QUIT_MATCH (YC10)", "Xác nhận rồi gửi QUIT_MATCH; người thoát bị xử thua."],
], [1.55, 1.45, 3.27], caption="Thành phần giao diện màn hình đua", first_col_bold=True)

heading("6.1. Điều khiển và vòng lặp trò chơi", 2)
table(["Phím", "Hành động", "Mô tả"], [
    ["W hoặc ↑", "Tăng tốc", "Tốc độ tăng 10 km/h mỗi lần nhấn, tối đa 200 km/h."],
    ["S hoặc ↓", "Giảm tốc / phanh", "Tốc độ giảm 15 km/h mỗi lần nhấn, tối thiểu 0."],
    ["A hoặc ←", "Sang làn trái", "Chuyển làn 1 → 0 hoặc 2 → 1; không đổi nếu đang ở làn ngoài cùng."],
    ["D hoặc →", "Sang làn phải", "Chuyển làn 0 → 1 hoặc 1 → 2."],
    ["Esc", "Thoát trận", "Tương đương nút Thoát trận."],
], [1.1, 1.45, 3.72], caption="Phím điều khiển", first_col_bold=True, align_center_cols=(0,))
para("Vòng lặp trò chơi ở client chạy bằng javax.swing.Timer chu kỳ 50 ms. Mỗi chu kỳ client cập nhật quãng đường "
     "theo tốc độ hiện tại (quãng đường tăng thêm tốc độ nhân với thời gian chu kỳ), gửi CAR_STATE lên server và "
     "vẽ lại màn hình. Khi nhận RACE_UPDATE, client ghi đè vị trí của cả hai xe theo giá trị server để hai màn "
     "hình luôn thống nhất; phần dự đoán cục bộ chỉ giúp xe của mình phản hồi tức thì khi bấm phím.")
code([
    "timer = new Timer(50, e -> {",
    "    myCar.advance(0.05);                 // quãng đường += tốc độ * 0.05 s",
    "    net.send(new Message(MessageType.CAR_STATE,",
    "             new CarState(myCar.distance(), myCar.lane(), myCar.speed())));",
    "    racePanel.repaint();",
    "});",
    "",
    "// Nhận RACE_UPDATE từ server (đã ở luồng Swing)",
    "void onRaceUpdate(RaceState state) {",
    "    myCar.applyServer(state.me());        // hiệu chỉnh theo server",
    "    opponentCar.applyServer(state.opponent());",
    "    if (state.finished()) timer.stop();",
    "}",
])

# ================================================================ 7. CÁ NHÂN 4
heading("7. Phần cá nhân 4 – Cơ sở dữ liệu và bảng xếp hạng (Trần Việt Anh)")
para("Trần Việt Anh (thành viên 4) thực hiện thiết kế dữ liệu và bảng xếp hạng. Khi trận kết thúc, server lưu kết quả vào bảng "
     "matches, ghi các sự kiện quan trọng vào match_events và cập nhật điểm, số trận thắng, thua, hòa trong bảng "
     "players. Bảng xếp hạng được lấy trực tiếp từ players, sắp xếp theo tổng điểm giảm dần rồi tổng số trận thắng "
     "giảm dần (YC12).")
figure("fig6_erd.png", "Mô hình dữ liệu quan hệ")
table(["Thành phần trên hình", "Chức năng", "Mô tả"], [
    ["players", "Lưu người chơi", "Tài khoản (username duy nhất, mật khẩu băm), tổng điểm, số trận thắng, thua, hòa. Điểm và số thắng là cột lưu sẵn để truy vấn xếp hạng nhanh."],
    ["matches", "Lưu kết quả trận", "Mã phòng, hai người chơi, người thắng, trạng thái, lý do kết thúc, thời gian bắt đầu và kết thúc."],
    ["Ba quan hệ players → matches", "Hai người chơi và người thắng", "player1_id, player2_id, winner_id cùng là khóa ngoại tới players; winner_id NULL khi hòa hoặc hủy trận."],
    ["match_events", "Lưu diễn biến", "Sự kiện theo thời gian: START, LANE_CHANGE, COLLISION, FINISH, QUIT, DISCONNECT với payload JSON."],
    ["Truy vấn xếp hạng", "YC12", "ORDER BY points DESC, wins DESC; có chỉ mục (points, wins) để sắp xếp nhanh."],
], [1.6, 1.3, 3.37], caption="Thành phần của mô hình dữ liệu", first_col_bold=True)

heading("7.1. Lược đồ cơ sở dữ liệu", 2)
code([
    "CREATE TABLE players (",
    "    player_id     INT AUTO_INCREMENT PRIMARY KEY,",
    "    username      VARCHAR(50)  NOT NULL UNIQUE,",
    "    password_hash VARCHAR(255) NOT NULL,",
    "    points        INT NOT NULL DEFAULT 0,",
    "    wins          INT NOT NULL DEFAULT 0,",
    "    losses        INT NOT NULL DEFAULT 0,",
    "    draws         INT NOT NULL DEFAULT 0,",
    "    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,",
    "    INDEX idx_rank (points DESC, wins DESC)",
    ");",
    "",
    "CREATE TABLE matches (",
    "    match_id    INT AUTO_INCREMENT PRIMARY KEY,",
    "    room_code   VARCHAR(20) NOT NULL,",
    "    player1_id  INT NOT NULL,",
    "    player2_id  INT NOT NULL,",
    "    winner_id   INT NULL,",
    "    status      ENUM('PLAYING','FINISHED','ABORTED') NOT NULL DEFAULT 'PLAYING',",
    "    end_reason  ENUM('FINISH','DRAW','QUIT','DISCONNECT','ABORTED') NULL,",
    "    started_at  DATETIME NOT NULL,",
    "    ended_at    DATETIME NULL,",
    "    FOREIGN KEY (player1_id) REFERENCES players(player_id),",
    "    FOREIGN KEY (player2_id) REFERENCES players(player_id),",
    "    FOREIGN KEY (winner_id)  REFERENCES players(player_id)",
    ");",
    "",
    "CREATE TABLE match_events (",
    "    event_id   BIGINT AUTO_INCREMENT PRIMARY KEY,",
    "    match_id   INT NOT NULL,",
    "    player_id  INT NOT NULL,",
    "    event_type VARCHAR(30) NOT NULL,",
    "    event_time DATETIME(3) NOT NULL,",
    "    payload    JSON NULL,",
    "    FOREIGN KEY (match_id)  REFERENCES matches(match_id),",
    "    FOREIGN KEY (player_id) REFERENCES players(player_id)",
    ");",
], size=9)

heading("7.2. Lưu kết quả và cập nhật điểm", 2)
para("Việc lưu kết quả và cộng điểm phải xảy ra cùng nhau, vì vậy MatchDAO thực hiện trong một transaction JDBC: "
     "tắt auto-commit, cập nhật bản ghi matches, cộng điểm cho từng người chơi rồi commit; nếu có lỗi thì rollback "
     "để không xảy ra tình trạng trận đã ghi mà điểm chưa cộng.")
code([
    "public void saveResult(MatchResult r) throws SQLException {",
    "    try (Connection c = DbConnection.get()) {",
    "        c.setAutoCommit(false);",
    "        String sqlMatch  = \"UPDATE matches SET winner_id=?, status='FINISHED', \"",
    "                         + \"end_reason=?, ended_at=NOW() WHERE match_id=?\";",
    "        String sqlPlayer = \"UPDATE players SET points=points+?, wins=wins+?, \"",
    "                         + \"losses=losses+?, draws=draws+? WHERE player_id=?\";",
    "        try (PreparedStatement m = c.prepareStatement(sqlMatch);",
    "             PreparedStatement p = c.prepareStatement(sqlPlayer)) {",
    "            m.setObject(1, r.winnerId());",
    "            m.setString(2, r.reason().name());",
    "            m.setInt(3, r.matchId());",
    "            m.executeUpdate();",
    "            for (PlayerDelta d : r.deltas()) {   // mỗi người chơi một dòng",
    "                p.setInt(1, d.points()); p.setInt(2, d.win());",
    "                p.setInt(3, d.loss());   p.setInt(4, d.draw());",
    "                p.setInt(5, d.playerId());",
    "                p.addBatch();",
    "            }",
    "            p.executeBatch();",
    "            c.commit();",
    "        } catch (SQLException e) { c.rollback(); throw e; }",
    "    }",
    "}",
], size=9)

heading("7.3. Truy vấn bảng xếp hạng", 2)
code([
    "SELECT RANK() OVER (ORDER BY points DESC, wins DESC) AS hang,",
    "       username, points, wins, losses, draws",
    "FROM players",
    "ORDER BY points DESC, wins DESC",
    "LIMIT 100;",
])
para("Client nhận danh sách RankRow qua thông điệp LEADERBOARD và hiển thị trong JTable với các cột Hạng, Tên, "
     "Điểm, Thắng, Thua, Hòa. Vì điểm và số trận thắng được cập nhật ngay trong transaction lưu kết quả, bảng "
     "xếp hạng luôn phản ánh đúng trận vừa kết thúc mà không cần tính lại từ bảng matches.")

# ================================================================ 8. PHÂN CÔNG
heading("8. Phân công công việc")
table(["Thành viên", "Phần phụ trách", "Sản phẩm bàn giao"], [
    ["Phạm Thị Thu Phương", "Server và kiến trúc (mục 4)", "GameServer, ClientHandler, SessionManager, InviteManager, RoomManager, MatchService; module common (Message, DTO)."],
    ["Vũ Văn Hiếu", "Client: đăng nhập, sảnh, thách đấu (mục 5)", "LoginFrame, LobbyFrame, InviteDialog, LeaderboardFrame, NetworkClient, ClientState."],
    ["Nguyễn Trần Mai Anh", "Giao diện màn hình đua (mục 6)", "RaceFrame, RacePanel, CarModel, xử lý phím, vòng lặp 50 ms, hiệu chỉnh theo RACE_UPDATE."],
    ["Trần Việt Anh", "CSDL và bảng xếp hạng (mục 7)", "Script schema.sql, DbConnection, PlayerDAO, MatchDAO, dữ liệu mẫu, truy vấn xếp hạng."],
    ["Cả nhóm", "Tích hợp và kiểm thử", "Chạy 3 client cùng lúc trên 2 máy, kiểm thử các tình huống ở Bảng 7, viết báo cáo."],
], [1.6, 1.9, 2.77], caption="Phân công công việc trong nhóm", first_col_bold=True)

# ================================================================ 9. KẾT LUẬN
heading("9. Kết luận")
para("Thiết kế bảo đảm Server là trọng tài trung tâm: mọi luật thắng thua, thời hạn lời mời, phát hiện mất kết "
     "nối và cập nhật điểm đều được quyết định tại server nên hai client không thể lệch kết quả. Server phục vụ "
     "nhiều client đồng thời nhờ mô hình một luồng cho mỗi kết nối cùng các cấu trúc dữ liệu thread-safe. Giao "
     "thức thông điệp thống nhất trên TCP giúp việc mở rộng thêm tính năng chỉ cần bổ sung loại thông điệp mới. "
     "Dữ liệu trận được lưu đầy đủ trong MySQL để phục vụ bảng xếp hạng và xem lại diễn biến, và từng phần cá "
     "nhân đều được ánh xạ rõ ràng với thành phần cụ thể trên các hình thiết kế.")

doc.save(OUT)
print("saved", OUT)
