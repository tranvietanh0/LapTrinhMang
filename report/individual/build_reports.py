"""Sinh 4 báo cáo cá nhân (mỗi thành viên một file) cho BTL Lập trình mạng – Game đua xe đối kháng online.

    python report/individual/build_reports.py        -> report/individual/out/BaoCao_<Ten>.docx (+ .pdf)

Bìa theo mẫu PTIT dùng chung với đồ án CSDL-DPT (khung bìa + logo trong assets/). Nội dung từng người nằm
trong members/*.py; đoạn mã được trích trực tiếp từ mã nguồn lúc build (source.py) nên luôn khớp code.
Mục lục có số trang thật: build hai lượt, lượt đầu đổi sang PDF để dò trang của từng tiêu đề.
"""
from __future__ import annotations

import re
import subprocess
import sys
from pathlib import Path

from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.shared import Cm, Pt

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))

from docx_helpers import Report  # noqa: E402
from members import ALL, TEAM  # noqa: E402
from source import loc  # noqa: E402

ASSETS = HERE / "assets"
IMG = HERE.parent / "img"
OUT = HERE / "out"
SOFFICE = Path(r"C:\Program Files\LibreOffice\program\soffice.exe")
LECTURER = "Nguyễn Hoàng Anh"
CLASS = "LTM-2026-2-N01"


def line(r: Report, text, size, before=0, after=0, bold=True):
    para = r.p(text, bold=bold, align="center", size=size)
    fmt = para.paragraph_format
    fmt.space_before, fmt.space_after, fmt.line_spacing = Pt(before), Pt(after), 1.0
    return para


def cover(r: Report, m: dict):
    """Bìa mẫu PTIT: khung, logo, đề tài, tên người viết báo cáo nổi bật, rồi danh sách cả nhóm."""
    r.cover_frame(ASSETS / "cover_frame.jpg", 17.67, 24.13)
    line(r, "HỌC VIỆN CÔNG NGHỆ BƯU CHÍNH VIỄN THÔNG", 15, before=40, after=3)
    line(r, "KHOA CÔNG NGHỆ THÔNG TIN", 13)
    logo = r.p(align="center")
    logo.paragraph_format.space_before = Pt(18)
    logo.add_run().add_picture(str(ASSETS / "ptit_logo.png"), width=Cm(3.3))
    line(r, "BÁO CÁO CÁ NHÂN BÀI TẬP LỚN", 20, before=16, after=4)
    line(r, "LẬP TRÌNH MẠNG", 20, after=10)
    line(r, "Đề tài: Game đua xe thi đấu đối kháng online", 17, after=3)
    line(r, f"Phần đóng góp: {m['cover_role']}", 14, before=6, after=12, bold=False).runs[0].italic = True

    rows = [("Giảng viên hướng dẫn", LECTURER, False), ("Lớp", CLASS, False),
            ("Sinh viên thực hiện", f"{m['name']} – {m['msv']}", True)]
    for i, other in enumerate(TEAM):
        rows.append(("Thành viên nhóm" if i == 0 else "", f"{i + 1}. {other['name']} – {other['msv']}",
                     other["name"] == m["name"]))
    t = r.doc.add_table(rows=0, cols=2)
    t.alignment = WD_TABLE_ALIGNMENT.CENTER
    t.autofit = False
    widths = (4.8, 9.8)
    for col, w in zip(t._tbl.tblGrid.gridCol_lst, widths):
        col.w = Cm(w)
    for label, value, strong in rows:
        cells = t.add_row().cells
        for cell, text, bold, w in ((cells[0], label, True, widths[0]), (cells[1], f": {value}" if label else
                                    f"  {value}", strong, widths[1])):
            cell.width = Cm(w)
            para = cell.paragraphs[0]
            para.paragraph_format.space_after = Pt(0)
            run = para.add_run(text)
            run.bold, run.font.size = bold, Pt(13)
    line(r, "Hà Nội, 2026", 14, before=30, bold=False)
    r.page_break()


def toc(r: Report, m: dict, pages: dict[str, int]):
    r.h(1, "MỤC LỤC")
    for level, title in heading_list(m):
        r.toc_line(title, str(pages.get(title, "")), level)
    r.page_break()


def heading_list(m: dict) -> list[tuple[int, str]]:
    out = [(1, t) for t in ("1. Giới thiệu đề tài và vai trò trong nhóm", "2. Tổng quan phần việc đã đóng góp")]
    out.append((1, "3. Chi tiết thực hiện"))
    out += [(2, s[0]) for s in m["sections"]]
    out += [(1, t) for t in ("4. Kiểm thử phần phụ trách", "5. Phối hợp với các thành viên khác",
                             "6. Khó khăn, cách giải quyết và tự đánh giá")]
    return out


def intro(r: Report, m: dict):
    r.h(1, "1. Giới thiệu đề tài và vai trò trong nhóm")
    r.h(2, "1.1. Đề tài")
    r.p("Nhóm xây dựng trò chơi đua xe thi đấu đối kháng 1 – 1 theo mô hình Client – Server bằng Java. Hai người "
        "chơi chạy chương trình client (Java Swing), cùng kết nối tới một server trung tâm qua TCP Socket cổng 5000. "
        "Server xác thực tài khoản, giữ danh sách người chơi online, chuyển lời mời thách đấu, tạo phòng đua, làm "
        "trọng tài cho trận đấu (tự tính quãng đường, va chạm, người thắng) và lưu tài khoản, điểm, lịch sử trận "
        "vào MySQL qua JDBC. Client chỉ gửi ý định điều khiển (làn, tốc độ), không được tự quyết định kết quả.")
    r.h(2, "1.2. Kiến trúc tổng thể")
    r.figure(IMG / "fig1_architecture.png", "Kiến trúc tổng thể của hệ thống", 15.5)
    r.p("Mã nguồn chia thành bốn module Maven: common (hợp đồng thông điệp và DTO dùng chung), server, client và "
        "tools (client dòng lệnh để kiểm thử giao thức), cùng thư mục db/ chứa lược đồ MySQL. Mọi thông điệp đi qua "
        "socket là một đối tượng Message gồm MessageType và payload, được tuần tự hóa bằng ObjectOutputStream.")
    r.h(2, "1.3. Phân công trong nhóm")
    rows = [[str(i + 1), t["name"] + (" (người viết)" if t["name"] == m["name"] else ""), t["role"], t["codes"]]
            for i, t in enumerate(TEAM)]
    r.table(["STT", "Thành viên", "Vai trò", "Mã task"], rows, [1.2, 4.6, 7.4, 2.8],
            caption="Phân công công việc của nhóm (theo PLAN.md)", size=11, align_center_cols={0, 3})
    r.p(m["intro"])


def overview(r: Report, m: dict):
    r.h(1, "2. Tổng quan phần việc đã đóng góp")
    r.p(f"Bảng dưới liệt kê từng task {m['name']} phụ trách theo kế hoạch chia việc, file / lớp tương ứng trong "
        "mã nguồn và kết quả đạt được. Tất cả các task đều đã được gộp vào nhánh main của repo.")
    r.table(["Mã", "Công việc", "File / lớp", "Kết quả"], m["tasks"], [1.3, 5.6, 4.4, 4.7],
            caption=f"Danh sách task của {m['name']}", size=10.5, align_center_cols={0})
    rows = [[f, str(loc(f)), d] for f, d in m["files"]]
    total = sum(loc(f) for f, _ in m["files"])
    rows.append(["Tổng", str(total), ""])
    r.p("Các file mã nguồn chính thuộc phần phụ trách và số dòng hiện tại (đếm trực tiếp từ repo lúc sinh báo cáo):")
    r.table(["File", "Số dòng", "Nội dung"], rows, [7.4, 1.6, 7.0],
            caption="File mã nguồn thuộc phần phụ trách", size=9.5, align_center_cols={1})


def details(r: Report, m: dict):
    r.h(1, "3. Chi tiết thực hiện")
    r.p(m["details_intro"])
    for title, blocks in m["sections"]:
        r.h(2, title)
        render(r, blocks)


def render(r: Report, blocks: list):
    for b in blocks:
        kind = b[0]
        if kind == "p":
            r.p(b[1])
        elif kind == "b":
            r.bullets(b[1])
        elif kind == "code":
            cap = r.p(f"Đoạn mã: {b[2]}", italic=True, align="left", size=12)
            cap.paragraph_format.keep_with_next = True
            r.code(b[1], size=9)
        elif kind == "fig":
            r.figure(IMG / b[1], b[2], b[3] if len(b) > 3 else 14.5)
        elif kind == "table":
            r.table(b[1], b[2], b[3], caption=b[4], size=10.5)
        else:
            raise ValueError(f"khối không hỗ trợ: {kind}")


def tail(r: Report, m: dict):
    r.h(1, "4. Kiểm thử phần phụ trách")
    r.p("Các kịch bản kiểm thử tích hợp (docs/TEST-PLAN.md) liên quan tới phần việc, chạy trên hai máy cùng mạng LAN "
        "hoặc bằng công cụ racing-tools:")
    r.table(["Mã", "Kịch bản", "Kết quả mong đợi", "Vai trò"], m["tests"], [1.3, 5.0, 6.7, 3.0],
            caption="Kịch bản kiểm thử tích hợp liên quan", size=10.5, align_center_cols={0})
    rows = [[f.split("/")[-1], str(len(re.findall(r"@Test\b", (HERE.parents[1] / f).read_text(encoding="utf-8")))),
             d] for f, d in m["unit_tests"]]
    r.p("Kiểm thử tự động (JUnit 5, chạy bằng mvnw test và trên GitHub Actions) bao phủ phần việc này:")
    r.table(["Lớp kiểm thử", "Số test", "Nội dung kiểm tra"], rows, [5.0, 1.6, 9.4],
            caption="Kiểm thử tự động liên quan", size=10.5, align_center_cols={1})
    r.h(1, "5. Phối hợp với các thành viên khác")
    r.bullets(m["collab"])
    r.h(1, "6. Khó khăn, cách giải quyết và tự đánh giá")
    r.table(["Khó khăn", "Cách giải quyết"], m["problems"], [6.0, 10.0],
            caption="Khó khăn gặp phải và cách xử lý", size=10.5)
    r.p(m["summary"])


def build(m: dict, pages: dict[str, int]) -> Path:
    r = Report()
    cover(r, m)
    toc(r, m, pages)
    intro(r, m)
    overview(r, m)
    details(r, m)
    tail(r, m)
    r.page_numbers()
    return r.save(OUT / f"BaoCao_CaNhan_{m['slug']}.docx")


def to_pdf(docx: Path) -> Path:
    if not SOFFICE.exists():
        raise FileNotFoundError(f"cần LibreOffice để đánh số trang mục lục: {SOFFICE}")
    subprocess.run([str(SOFFICE), "--headless", "--convert-to", "pdf", "--outdir", str(docx.parent), str(docx)],
                   check=True, capture_output=True, timeout=300)
    return docx.with_suffix(".pdf")


def find_pages(pdf: Path, m: dict) -> dict[str, int]:
    """Trang (đánh số từ trang bìa = 1) của mỗi tiêu đề, bỏ qua trang mục lục."""
    import pymupdf

    doc = pymupdf.open(pdf)
    texts = [" ".join(p.get_text().split()) for p in doc]
    doc.close()
    pages, start = {}, 2
    for _, title in heading_list(m):
        key = " ".join(title.split())
        for i in range(start, len(texts)):
            if key.upper() in texts[i].upper():
                pages[title], start = i + 1, i
                break
        else:
            raise ValueError(f"không dò được trang của tiêu đề '{title}'")
    return pages


if __name__ == "__main__":
    for member in ALL:
        out = build(member, {})
        pages = find_pages(to_pdf(out), member)
        out = build(member, pages)
        pdf = to_pdf(out)
        print(f"[ok] {out.name} + {pdf.name}")
