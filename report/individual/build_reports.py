"""Sinh 4 báo cáo cá nhân (mỗi thành viên một file) cho BTL Lập trình mạng – Game đua xe đối kháng online.

    python report/individual/build_reports.py            -> out/BaoCao_CaNhan_<Ten>.docx (+ .pdf), cả 4 người
    python report/individual/build_reports.py VuQuocHieu -> chỉ một người (theo slug)

Bố cục theo “Yêu cầu đối với quyển báo cáo BTL (cá nhân)”: bìa (bôi đậm người viết), trang phân công và tỉ lệ
đóng góp, mục lục, từ viết tắt, danh sách hình, danh sách bảng; Chương 1 giới thiệu và phân tích yêu cầu (chung +
phần cá nhân); Chương 2 thiết kế tổng quan (kiến trúc, sơ đồ khối client / server, use case tổng quan) và thiết kế
chi tiết phần cá nhân (use case chi tiết, lớp, tuần tự, ER); Chương 3 kết quả (kiến trúc, cài đặt triển khai,
chức năng, thử nghiệm); kết luận; tài liệu tham khảo. TNR 13, căn đều hai bên.

Nội dung từng người nằm trong members/*.py (khóa CONTENT, xem MEMBER_KEYS); sơ đồ vẽ bằng uml.py lúc build; tên
phương thức trên biểu đồ lớp được đối chiếu với mã nguồn (class_files) nên sai tên là build lỗi. Mục lục và danh
sách hình / bảng có số trang thật: build hai lượt, lượt đầu đổi sang PDF để dò trang.
"""
from __future__ import annotations

import re
import subprocess
import sys
from pathlib import Path

from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.shared import Cm, Pt

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
sys.path.insert(0, str(HERE))

import shared  # noqa: E402
import uml  # noqa: E402
from docx_helpers import Report  # noqa: E402
from members import ALL, TEAM  # noqa: E402

ASSETS = HERE / "assets"
OUT = HERE / "out"
IMG_OUT = OUT / "img"
SOFFICE = Path(r"C:\Program Files\LibreOffice\program\soffice.exe")

MEMBER_KEYS = ("name", "slug", "part_intro", "tasks", "part_analysis", "part_reqs", "uc", "uc_text", "uc_specs",
               "classes", "class_files", "class_text", "class_rows", "sequences", "er", "er_text", "screens",
               "results", "tests", "unit_tests", "conclusion")
FRONT = ("PHÂN CÔNG CÔNG VIỆC VÀ TỈ LỆ ĐÓNG GÓP", "MỤC LỤC", "DANH MỤC TỪ VIẾT TẮT", "DANH SÁCH HÌNH VẼ",
         "DANH SÁCH BẢNG BIỂU")


# ------------------------------------------------------------------------------------------------ trang đầu
def line(r: Report, text, size, before=0, after=0, bold=True):
    para = r.p(text, bold=bold, align="center", size=size)
    fmt = para.paragraph_format
    fmt.space_before, fmt.space_after, fmt.line_spacing = Pt(before), Pt(after), 1.0
    return para


def cover(r: Report, m: dict):
    """Bìa mẫu PTIT: họ tên, MSSV, lớp, đề tài, danh sách nhóm với người viết bôi đậm."""
    r.cover_frame(ASSETS / "cover_frame.jpg", 17.67, 24.13)
    line(r, "HỌC VIỆN CÔNG NGHỆ BƯU CHÍNH VIỄN THÔNG", 14, before=36, after=3)
    line(r, "KHOA CÔNG NGHỆ THÔNG TIN 1", 13)
    logo = r.p(align="center")
    logo.paragraph_format.space_before = Pt(14)
    logo.add_run().add_picture(str(ASSETS / "ptit_logo.png"), width=Cm(3.0))
    line(r, "BÁO CÁO BÀI TẬP LỚN (CÁ NHÂN)", 18, before=12, after=2)
    line(r, "MÔN: LẬP TRÌNH MẠNG", 16, after=10)
    line(r, f"ĐỀ TÀI: {shared.TOPIC.upper()}", 15, after=14)

    rows = [("Họ và tên", m["name"], True), ("Mã sinh viên", m["msv"], True), ("Lớp", shared.CLASS, False),
            ("Giảng viên hướng dẫn", shared.LECTURER, False)]
    for i, other in enumerate(TEAM):
        rows.append(("Thành viên nhóm" if i == 0 else "", f"{i + 1}. {other['name']} – {other['msv']}",
                     other["name"] == m["name"]))
    t = r.doc.add_table(rows=0, cols=2)
    t.alignment = WD_TABLE_ALIGNMENT.CENTER
    t.autofit = False
    widths = (5.4, 9.2)
    for col, w in zip(t._tbl.tblGrid.gridCol_lst, widths):
        col.w = Cm(w)
    for label, value, strong in rows:
        cells = t.add_row().cells
        for cell, text, bold, w in ((cells[0], label, True, widths[0]),
                                    (cells[1], f": {value}" if label else f"  {value}", strong, widths[1])):
            cell.width = Cm(w)
            para = cell.paragraphs[0]
            para.paragraph_format.space_after = Pt(1)
            run = para.add_run(text)
            run.bold, run.font.size = bold, Pt(13)
    line(r, "Hà Nội, 2026", 13, before=26, bold=False)
    r.page_break()


def assignment(r: Report, m: dict):
    r.h(1, FRONT[0])
    rows = []
    for i, t in enumerate(TEAM):
        name = t["name"] + (" (người viết)" if t["name"] == m["name"] else "")
        rows.append([str(i + 1), f"{name}\n{t['msv']}", t["work"], t["codes"], f"{t['percent']}%"])
    r.table(["STT", "Họ tên, MSSV", "Công việc phụ trách", "Mã task", "Tỉ lệ"], rows, [1.1, 4.2, 7.2, 2.0, 1.5],
            caption="Phân công công việc và tỉ lệ đóng góp của các thành viên", size=11,
            align_center_cols={0, 3, 4})
    total = sum(t["percent"] for t in TEAM)
    if total != 100:
        raise ValueError(f"tổng tỉ lệ đóng góp phải là 100%, đang là {total}%")
    r.p(shared.TEAM_NOTE)


def index_page(r: Report, title: str, entries: list[tuple[int, str]], pages: dict[str, int], new_page=True):
    if not new_page:
        r.p()
    r.h(1, title, new_page=new_page)
    for level, text in entries:
        r.toc_line(text, str(pages.get(text, "")), level)


def abbreviations(r: Report):
    r.h(1, FRONT[2])
    r.table(["Viết tắt", "Tiếng Anh / đầy đủ", "Ý nghĩa"], [list(a) for a in shared.ABBREVIATIONS],
            [2.4, 6.0, 7.6], size=11, align_center_cols={0})


# ------------------------------------------------------------------------------------------------ chương 1
def chapter1(r: Report, m: dict):
    r.start_chapter(1, "GIỚI THIỆU VÀ PHÂN TÍCH YÊU CẦU")
    r.h(2, "1.1. Giới thiệu ứng dụng")
    for t in shared.INTRO:
        r.p(t)
    r.h(2, "1.2. Phân tích yêu cầu ứng dụng")
    r.p("Từ đề bài, nhóm xác định các yêu cầu chức năng và phi chức năng của hệ thống như bảng dưới. Mã yêu cầu "
        "(YC) được dùng lại ở các phần sau để chỉ rõ phần việc nào đáp ứng yêu cầu nào.")
    r.table(["Mã", "Yêu cầu", "Loại"], shared.REQUIREMENTS, [1.5, 11.5, 3.0], caption="Yêu cầu của hệ thống",
            size=11, align_center_cols={0, 2})
    r.h(2, "1.3. Nội dung cá nhân đảm nhận")
    r.h(3, "1.3.1. Giới thiệu phần việc")
    for t in m["part_intro"]:
        r.p(t)
    r.table(["Mã", "Công việc", "File / lớp chính"], m["tasks"], [1.4, 9.0, 5.6],
            caption=f"Các task do {m['name']} thực hiện", size=11, align_center_cols={0})
    r.h(3, "1.3.2. Phân tích nội dung phần việc")
    for t in m["part_analysis"]:
        r.p(t)
    r.table(["Mã", "Yêu cầu cụ thể của phần việc", "Đáp ứng YC"], m["part_reqs"], [1.5, 11.7, 2.8],
            caption="Yêu cầu chi tiết của phần cá nhân", size=11, align_center_cols={0, 2})


# ------------------------------------------------------------------------------------------------ chương 2
def chapter2(r: Report, m: dict, figs: dict[str, Path]):
    r.start_chapter(2, "PHÂN TÍCH THIẾT KẾ")
    r.h(2, "2.1. Thiết kế tổng quan ứng dụng")
    r.h(3, "2.1.1. Kiến trúc tổng quan")
    r.p(shared.ARCH_TEXT[0])
    r.figure(figs["arch"], "Kiến trúc tổng quan của hệ thống", 14.5)
    r.p(shared.ARCH_TEXT[1])
    r.table(["Nhóm", "Loại thông điệp (MessageType)", "Mục đích"], shared.MESSAGES, [2.4, 8.6, 5.0],
            caption="Các nhóm thông điệp trong giao thức", size=10.5)
    r.h(3, "2.1.2. Sơ đồ khối chức năng của Client")
    r.p("Chương trình client gồm sáu khối chức năng. Khối kết nối mạng nằm dưới mọi khối khác: các màn hình chỉ "
        "gọi send() và đăng ký listener theo loại thông điệp, không làm việc trực tiếp với socket.")
    r.figure(figs["client_blocks"], "Sơ đồ khối chức năng của Client", 13.5)
    r.h(3, "2.1.3. Sơ đồ khối chức năng của Server")
    r.p("Server gồm sáu khối. Mỗi client có một ClientHandler riêng; phần lõi (phiên, lời mời, phòng, trọng tài) "
        "không phụ thuộc socket nên kiểm thử được bằng JUnit; khối dữ liệu là nơi duy nhất truy cập MySQL.")
    r.figure(figs["server_blocks"], "Sơ đồ khối chức năng của Server", 13.5)
    r.h(3, "2.1.4. Biểu đồ use case tổng quan")
    r.p("Tác nhân duy nhất là người chơi. Người chơi phải đăng nhập trước khi dùng các chức năng khác (trừ đăng ký). "
        "Trong trận, kết quả trận luôn được hiển thị (include); thi đấu tiếp và thoát trận là các nhánh mở rộng "
        "(extend) của ca thi đấu.")
    r.figure(figs["uc_overall"], "Biểu đồ use case tổng quan của hệ thống", 11.0)

    r.h(2, "2.2. Thiết kế chi tiết chức năng cá nhân")
    r.h(3, "2.2.1. Biểu đồ use case chi tiết")
    r.p(m["uc_text"])
    r.figure(diagram(m, "uc"), f"Biểu đồ use case chi tiết phần {m['short_lower']}", m["uc"].get("width", 12.0))
    for spec in m["uc_specs"]:
        uc_spec(r, spec)
    r.h(3, "2.2.2. Biểu đồ lớp")
    r.p(m["class_text"])
    r.figure(diagram(m, "classes"), f"Biểu đồ lớp phần {m['short_lower']}", m["classes"].get("width", 16.0))
    r.table(["Lớp", "Trách nhiệm"], m["class_rows"], [4.2, 11.8], caption="Mô tả các lớp chính", size=11)
    r.h(3, "2.2.3. Biểu đồ tuần tự")
    for i, seq in enumerate(m["sequences"]):
        r.p(seq["text"])
        r.figure(diagram(m, f"seq{i}"), seq["title"], seq.get("width", 16.0))
    r.h(3, "2.2.4. Sơ đồ thực thể quan hệ (ER)")
    for t in m["er_text"]:
        r.p(t)
    r.figure(diagram(m, "er"), f"Sơ đồ thực thể quan hệ liên quan phần {m['short_lower']}",
             m["er"].get("width", 15.5))


def uc_spec(r: Report, s: dict):
    steps = "\n".join(f"{i + 1}. {x}" for i, x in enumerate(s["main"]))
    alt = "\n".join(s["alt"]) if s["alt"] else "Không"
    rows = [["Tên use case", s["name"]], ["Tác nhân", s["actor"]], ["Tiền điều kiện", s["pre"]],
            ["Luồng sự kiện chính", steps], ["Luồng thay thế / ngoại lệ", alt], ["Hậu điều kiện", s["post"]]]
    t = r.table(["Mục", "Nội dung"], rows, [4.0, 12.0], caption=f"Đặc tả use case “{s['name']}”", size=11)
    for row in t.rows[1:]:
        row.cells[0].paragraphs[0].runs[0].bold = True


# ------------------------------------------------------------------------------------------------ chương 3
def chapter3(r: Report, m: dict, figs: dict[str, Path]):
    r.start_chapter(3, "KẾT QUẢ")
    r.h(2, "3.1. Kiến trúc ứng dụng")
    r.p(shared.ARCH_RESULT_TEXT)
    r.figure(figs["deploy"], "Kiến trúc ứng dụng khi triển khai: Server, Client và người chơi", 16.0)
    r.h(2, "3.2. Cài đặt và triển khai ứng dụng")
    r.p("Môi trường và công nghệ sử dụng:")
    r.table(["Thành phần", "Công nghệ / phiên bản"], shared.ENVIRONMENT, [4.0, 12.0],
            caption="Môi trường cài đặt", size=11)
    r.p("Các bước triển khai trên hai máy Windows trong cùng mạng LAN:")
    r.numbered(shared.DEPLOY_STEPS)
    r.h(2, "3.3. Các kết quả thực hiện được")
    r.h(3, "3.3.1. Tính năng của ứng dụng")
    r.table(["Chức năng", "Kết quả"], shared.FEATURES, [5.5, 10.5], caption="Các chức năng của ứng dụng", size=11)
    r.h(3, "3.3.2. Kết quả phần cá nhân")
    r.table(["Chức năng / thành phần", "Kết quả đạt được"], m["results"], [5.5, 10.5],
            caption=f"Kết quả phần việc của {m['name']}", size=11)
    for path, caption, width in m["screens"]:
        r.figure(path, caption, width)
    r.h(3, "3.3.3. Kết quả thử nghiệm")
    rows = [[f.split("/")[-1], str(len(re.findall(r"@Test\b", (ROOT / f).read_text(encoding="utf-8")))), d]
            for f, d in m["unit_tests"]]
    r.p("Kiểm thử tự động bằng JUnit 5 (chạy bằng mvnw test và trên GitHub Actions). Toàn bộ dự án có "
        f"{count_tests()} test; lần chạy gần nhất các test không cần MySQL đều đạt, các test tích hợp CSDL chạy trên "
        "CI có dịch vụ MySQL. Các lớp kiểm thử liên quan phần việc:")
    r.table(["Lớp kiểm thử", "Số test", "Nội dung kiểm tra"], rows, [5.2, 1.6, 9.2],
            caption="Kiểm thử tự động liên quan phần cá nhân", size=11, align_center_cols={1})
    r.p("Kịch bản kiểm thử tích hợp (docs/TEST-PLAN.md) chạy trên hai máy trong mạng LAN:")
    r.table(["Mã", "Kịch bản", "Kết quả mong đợi", "Kết quả"], m["tests"], [1.2, 4.6, 6.0, 4.2],
            caption="Kết quả kiểm thử tích hợp", size=11, align_center_cols={0, 3})


def ending(r: Report, m: dict):
    r.h(1, "KẾT LUẬN")
    for t in m["conclusion"]:
        r.p(t)
    r.p()
    r.h(1, "TÀI LIỆU THAM KHẢO", new_page=False)
    for i, ref in enumerate(shared.REFERENCES):
        para = r.p(f"[{i + 1}] {ref}", align="left")
        para.paragraph_format.left_indent = Cm(0.9)
        para.paragraph_format.first_line_indent = Cm(-0.9)


# ------------------------------------------------------------------------------------------------ hình, kiểm tra
def count_tests() -> int:
    return sum(len(re.findall(r"@Test\b", p.read_text(encoding="utf-8")))
               for p in ROOT.glob("*/src/test/java/**/*.java"))


def diagram(m: dict, key: str) -> Path:
    path = IMG_OUT / f"{m['slug']}_{key}.png"
    if key == "uc":
        s = m["uc"]
        return uml.usecase(path, s["title"], s["actors"], s["cases"], s["links"],
                           **{k: s[k] for k in ("cols", "col_w", "row_h") if k in s})
    if key == "classes":
        s = m["classes"]
        return uml.class_diagram(path, s["classes"], s["relations"],
                                 **{k: s[k] for k in ("col_w", "row_gap", "pt") if k in s})
    if key.startswith("seq"):
        s = m["sequences"][int(key[3:])]
        return uml.sequence(path, s["participants"], s["messages"], frames=s.get("frames"),
                            **{k: s[k] for k in ("gap", "step") if k in s})
    if key == "er":
        s = m["er"]
        return uml.er(path, s["entities"], s["relations"], s["W"], s["H"])
    raise ValueError(key)


def check_member(m: dict):
    missing = [k for k in MEMBER_KEYS if k not in m]
    if missing:
        raise ValueError(f"{m.get('slug')}: thiếu khóa {missing}")
    files = m["class_files"]
    for name, _, attrs, ops, *_ in m["classes"]["classes"]:
        if name not in files:
            raise ValueError(f"{m['slug']}: lớp {name} chưa khai báo file trong class_files")
        src = (ROOT / files[name]).read_text(encoding="utf-8")
        if not re.search(rf"\b(class|record|interface|enum)\s+{re.escape(name)}\b", src):
            raise ValueError(f"{m['slug']}: không thấy khai báo {name} trong {files[name]}")
        for op in ops:
            meth = re.match(r"[+\-#~]?\s*(\w+)\s*\(", op)
            if meth and not re.search(rf"\b{meth.group(1)}\s*\(", src):
                raise ValueError(f"{m['slug']}: {name}.{meth.group(1)}() không có trong {files[name]}")
    for path, *_ in m["screens"]:
        if not Path(path).exists():
            raise FileNotFoundError(path)


# ------------------------------------------------------------------------------------------------ build
def build(m: dict, figs: dict[str, Path], pages: dict[str, int], lists: tuple[list, list] | None) -> Report:
    r = Report()
    cover(r, m)
    assignment(r, m)
    heads = headings(m)
    index_page(r, FRONT[1], [(1, t) for t in FRONT[2:]] + heads, pages)
    abbreviations(r)
    figs_list, tabs_list = lists or ([], [])
    index_page(r, FRONT[3], [(2, f) for f in figs_list], pages)
    index_page(r, FRONT[4], [(2, t) for t in tabs_list], pages, new_page=False)
    chapter1(r, m)
    chapter2(r, m, figs)
    chapter3(r, m, figs)
    ending(r, m)
    r.page_numbers()
    r.save(OUT / f"BaoCao_CaNhan_{m['slug']}.docx")
    return r


def headings(m: dict) -> list[tuple[int, str]]:
    """Danh sách tiêu đề cho mục lục, dựng bằng một lượt build nháp (không ghi file)."""
    probe = Report()
    probe.doc.add_paragraph()
    chapter1(probe, m)
    chapter2(probe, m, PROBE_FIGS)
    chapter3(probe, m, PROBE_FIGS)
    ending(probe, m)
    return [(min(lvl, 3), t) for lvl, t in probe.headings]


def to_pdf(docx: Path) -> Path:
    if not SOFFICE.exists():
        raise FileNotFoundError(f"cần LibreOffice để đánh số trang mục lục: {SOFFICE}")
    subprocess.run([str(SOFFICE), "--headless", "--convert-to", "pdf", "--outdir", str(docx.parent), str(docx)],
                   check=True, capture_output=True, timeout=300)
    return docx.with_suffix(".pdf")


def find_pages(pdf: Path, titles: list[str], body_start: str) -> dict[str, int]:
    """Trang (bìa = 1) của mỗi mục, tìm tuần tự từ trang có tiêu đề chương 1 trở đi."""
    import pymupdf

    doc = pymupdf.open(pdf)
    texts = [page_text(p) for p in doc]
    doc.close()
    body = next(i for i, t in enumerate(texts) if t.startswith(body_start.upper()))
    toc = next(i for i, t in enumerate(texts) if t.startswith(FRONT[1]))
    skip = set(range(toc, body))  # mục lục, danh sách hình / bảng nhắc lại chính các chú thích
    start = 1  # bảng phân công (trang 2) đứng trước phần thân
    pages = {}
    for title in titles:
        key = " ".join(title.split()).upper()
        for i in range(start, len(texts)):
            if i not in skip and key in texts[i]:
                pages[title], start = i + 1, i
                break
        else:
            raise ValueError(f"không dò được trang của '{title}'")
    return pages


def page_text(page) -> str:
    """Chữ của một trang PDF, viết hoa, bỏ số trang ở chân trang (LibreOffice xuất nó ra đầu tiên)."""
    return re.sub(r"^\d+ ", "", " ".join(page.get_text().split()).upper())


def front_pages(pdf: Path) -> dict[str, int]:
    import pymupdf

    doc = pymupdf.open(pdf)
    texts = [page_text(p) for p in doc]
    doc.close()
    toc = next(i for i, t in enumerate(texts) if t.startswith(FRONT[1]))
    out = {FRONT[1]: toc + 1}
    for title in FRONT[2:]:  # danh sách bảng có thể nằm giữa trang, ngay sau danh sách hình
        out[title] = next(i + 1 for i in range(toc + 1, len(texts)) if title in texts[i])
    return out


PROBE_FIGS: dict[str, Path] = {}


def main(only: list[str]):
    global PROBE_FIGS
    figs = shared.figures()
    PROBE_FIGS = figs
    members = [m for m in ALL if not only or m["slug"] in only]
    if not members:
        raise SystemExit(f"không có thành viên nào khớp {only}; slug hợp lệ: {[m['slug'] for m in ALL]}")
    for m in members:
        check_member(m)
        first = build(m, figs, {}, None)
        lists = (first.fig_list, first.tab_list)
        build(m, figs, {}, lists)  # lượt 2: danh sách hình / bảng đã đủ dòng, chỉ thiếu số trang
        out = OUT / f"BaoCao_CaNhan_{m['slug']}.docx"
        pdf = to_pdf(out)
        titles = [t for _, t in headings(m)]  # chỉ phần thân, trang đầu dò bằng front_pages
        pages = find_pages(pdf, titles, titles[0])
        pages.update(locate(pdf, lists, titles[0]))
        pages.update(front_pages(pdf))
        build(m, figs, pages, lists)
        pdf = to_pdf(out)
        print(f"[ok] {out.name} + {pdf.name}")


def locate(pdf: Path, lists: tuple[list, list], body_start: str) -> dict[str, int]:
    """Hình và bảng xen kẽ nhau nên dò riêng từng danh sách, mỗi danh sách theo thứ tự xuất hiện."""
    out = {}
    for items in lists:
        out.update(find_pages(pdf, items, body_start))
    return out


if __name__ == "__main__":
    main(sys.argv[1:])
