"""Nội dung báo cáo cá nhân của 4 thành viên. TEAM là danh sách in trên bìa và trang phân công."""
from . import hieu, mai_anh, phuong, viet_anh

TEAM = [
    {"name": "Phạm Thị Thu Phương", "msv": "B23DCCN666", "short": "Server", "codes": "S1–S7", "percent": 25,
     "work": "Server: nhận kết nối, quản lý phiên, lời mời thách đấu, phòng đua, trọng tài trận đấu"},
    {"name": "Vũ Quốc Hiếu", "msv": "B23DCCN315", "short": "Client sảnh", "codes": "C1–C7", "percent": 25,
     "work": "Client sảnh: mạng phía client, đăng nhập, sảnh chờ, hộp thoại lời mời, điều hướng"},
    {"name": "Nguyễn Trần Mai Anh", "msv": "B23DCCN038", "short": "Dữ liệu và SQL", "codes": "M1–M6",
     "percent": 25, "work": "Dữ liệu: đăng ký tài khoản, bảng xếp hạng, lịch sử trận, ghi diễn biến trận"},
    {"name": "Trần Việt Anh", "msv": "B23DCCN050", "short": "Nền tảng và màn hình đua", "codes": "D1–D7, R1–R6",
     "percent": 25, "work": "Nền tảng (Maven, common, lược đồ CSDL, DAO), màn hình đua, kiểm thử, đóng gói"},
]

_BY_NAME = {t["name"]: t for t in TEAM}
ALL = []
for _mod in (phuong, hieu, mai_anh, viet_anh):
    _m = dict(_mod.CONTENT)
    _t = _BY_NAME[_m["name"]]
    _m["msv"], _m["short"], _m["short_lower"] = _t["msv"], _t["short"], _t["short"].lower()
    ALL.append(_m)
