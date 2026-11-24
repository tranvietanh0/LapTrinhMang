"""Trích đoạn mã nguồn thật từ repo để đưa vào báo cáo (không chép tay, tránh lệch với code)."""
from __future__ import annotations

import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


def method(path: str, header: str, max_lines: int = 40) -> str:
    """Trả về khối mã bắt đầu từ dòng khớp regex `header`, kết thúc ở dấu } cân bằng.

    Ném lỗi nếu không tìm thấy: báo cáo phải bám code, không được im lặng bỏ qua.
    """
    lines = (ROOT / path).read_text(encoding="utf-8").splitlines()
    pat = re.compile(header)
    start = next((i for i, ln in enumerate(lines) if pat.search(ln)), None)
    if start is None:
        raise ValueError(f"không thấy '{header}' trong {path}")
    depth, seen, out = 0, False, []
    for ln in lines[start:]:
        out.append(ln)
        depth += ln.count("{") - ln.count("}")
        seen = seen or "{" in ln
        if seen and depth <= 0:
            break
    indent = min(len(ln) - len(ln.lstrip()) for ln in out if ln.strip())
    out = [ln[indent:] for ln in out]
    if len(out) > max_lines:
        out = out[:max_lines - 1] + ["    // ... (lược bớt, xem đầy đủ trong mã nguồn)"]
    return "\n".join(out)


def lines_of(path: str, start: str, count: int) -> str:
    """`count` dòng tính từ dòng đầu tiên khớp regex `start` (dùng cho SQL, khai báo)."""
    lines = (ROOT / path).read_text(encoding="utf-8").splitlines()
    pat = re.compile(start)
    i = next((k for k, ln in enumerate(lines) if pat.search(ln)), None)
    if i is None:
        raise ValueError(f"không thấy '{start}' trong {path}")
    return "\n".join(lines[i:i + count])


def matching(path: str, pattern: str) -> str:
    """Mọi dòng khớp regex (bỏ thụt lề), ví dụ chỉ lấy khai báo hằng số."""
    pat = re.compile(pattern)
    found = [ln.strip() for ln in (ROOT / path).read_text(encoding="utf-8").splitlines() if pat.search(ln)]
    if not found:
        raise ValueError(f"không thấy '{pattern}' trong {path}")
    return "\n".join(found)


def loc(path: str) -> int:
    """Số dòng của một file mã nguồn."""
    return len((ROOT / path).read_text(encoding="utf-8").splitlines())
