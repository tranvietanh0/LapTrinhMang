---
title: Setup va chay game dua xe tren Windows
date: 2026-10-12
summary: "Java 17, MySQL rieng, build thanh cong va hai client da mo."
---

# Setup va chay game dua xe tren Windows

Đã cài Temurin JDK 17 bằng winget và MySQL 8.4.11 ZIP từ Oracle; kiểm tra checksum trước khi giải nén. MySQL riêng cho dự án ở %LOCALAPPDATA%\LapTrinhMang, chỉ bind 127.0.0.1:3306. Đã backup trước khi nạp schema/seed và trước/sau test DAO; tạo mật khẩu DB riêng, giữ trong file cấu hình cục bộ được Git bỏ qua.

Build toàn bộ module thành công. Lần gọi DAO đầu thiếu biến môi trường do đọc properties theo từng dòng; sửa lệnh đọc bằng -Raw rồi 9 test DAO đạt. Đã dọn dữ liệu t_ do test tạo sau khi backup, giữ 6 tài khoản mẫu. Smoke test qua socket xác nhận alice và bob đăng nhập được và nhận bảng xếp hạng. Thử lệnh history không được ScriptedClient hỗ trợ; không xác nhận kiểm thử lịch sử qua công cụ này.

Đã tạo launcher cục bộ dist/run-game.cmd và dist/stop-game.cmd, script runtime kiểm tra chủ cổng và PID để tránh tiến trình trùng. Kiểm tra tắt/khởi động lại thành công, dữ liệu giữ nguyên; chạy launcher lần nữa vẫn chỉ có hai cửa sổ client. Server PID 22340, cổng 5000; MySQL parent PID 20852 và worker PID 8408, cổng 3306; client PID 1856 và 2040. Các tiến trình được để chạy phục vụ yêu cầu mở dự án của người dùng, có stop-game.cmd để dừng.

Cập nhật docs/WINDOWS-LOCAL-SETUP.md và liên kết từ README. Chưa kiểm thử một trận đấu bằng GUI hoặc giữa hai máy. Không commit. AgentWiki publish skipped.

> Historical work record — not durable authority. Prefer docs/specs/ADRs for current decisions.
