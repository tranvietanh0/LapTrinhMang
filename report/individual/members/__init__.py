"""Nội dung báo cáo cá nhân của 4 thành viên. TEAM là danh sách in trên bìa và bảng phân công."""
from . import hieu, mai_anh, phuong, viet_anh

MSV_BLANK = "……………"

TEAM = [
    {"name": "Phạm Thị Thu Phương", "short": "Server", "msv": MSV_BLANK, "codes": "S1–S7",
     "role": "Server: kết nối, phiên, lời mời, phòng đua, trọng tài"},
    {"name": "Vũ Văn Hiếu", "short": "Client sảnh", "msv": MSV_BLANK, "codes": "C1–C7",
     "role": "Client sảnh: mạng phía client, đăng nhập, sảnh chờ, lời mời"},
    {"name": "Nguyễn Trần Mai Anh", "short": "Dữ liệu và SQL", "msv": MSV_BLANK, "codes": "M1–M6",
     "role": "Dữ liệu SQL: xếp hạng, đăng ký, lịch sử trận, ghi diễn biến"},
    {"name": "Trần Việt Anh", "short": "Nền tảng và màn hình đua", "msv": "B23DCCN050", "codes": "D1–D7, R1–R6",
     "role": "Nền tảng (Maven, common, schema, DAO), màn hình đua, kiểm thử, đóng gói"},
]

_BY_NAME = {t["name"]: t for t in TEAM}
ALL = []
for _mod in (phuong, hieu, mai_anh, viet_anh):
    _m = dict(_mod.CONTENT)
    _m["msv"] = _BY_NAME[_m["name"]]["msv"]
    _m["cover_role"] = f'{_BY_NAME[_m["name"]]["short"]} ({_BY_NAME[_m["name"]]["codes"]})'
    ALL.append(_m)
