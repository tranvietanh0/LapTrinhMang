# Game đua xe thi đấu đối kháng online (Java)

Bài tập lớn môn Lập trình mạng, lớp LTM-2026-2-N01. Client Java Swing, server TCP Socket, dữ liệu MySQL.

- Báo cáo thiết kế: [`report/LTM_Nhom_GameDuaXe_Java.docx`](report/)
- Kế hoạch chia việc: [`PLAN.md`](PLAN.md)

## Cấu trúc

| Module | Nội dung | Phụ trách |
|---|---|---|
| `common` | `Message`, `MessageType`, DTO, `GameConfig` dùng chung | Trần Việt Anh |
| `server` | `GameServer`, `ClientHandler`, các manager, `db/` (DAO) | Phạm Thị Thu Phương (server), Trần Việt Anh (`db/`) |
| `client` | `NetworkClient`, sảnh, màn hình đua | Vũ Văn Hiếu (sảnh), Nguyễn Trần Mai Anh (đua) |
| `db/` | `schema.sql`, `seed.sql`, `reset-db.sql` | Trần Việt Anh |

## Yêu cầu máy

- JDK 17 trở lên (`java -version`). Không cần cài Maven, dùng `mvnw` / `mvnw.cmd` có sẵn.
- MySQL 8. Cách nhanh nhất là Docker: `docker compose up -d` (tự nạp schema và dữ liệu mẫu lần đầu).
  Không dùng Docker thì cài MySQL rồi chạy:
  ```
  mysql -u root -p < db/schema.sql
  mysql -u root -p racing < db/seed.sql
  ```

## Cấu hình kết nối DB

Sao chép `server/src/main/resources/db.properties.example` thành `db.properties` cùng thư mục và sửa user, mật khẩu.
File `db.properties` nằm trong `.gitignore`. Có thể thay bằng biến môi trường `RACING_DB_URL`, `RACING_DB_USER`, `RACING_DB_PASSWORD`.

Tài khoản mẫu: `alice`, `bob`, `carol`, `dave`, `erin`, `frank`, mật khẩu đều là `123456`.

## Build và chạy

```
./mvnw package                       # Windows: mvnw.cmd package
java -jar server/target/racing-server.jar
java -jar client/target/racing-client.jar
```

Chạy nhanh khi đang phát triển:

```
./mvnw -pl server -am exec:java
./mvnw -pl client -am exec:java
```

Test tích hợp DAO chỉ chạy khi có DB:

```
RACING_TEST_DB_URL=jdbc:mysql://localhost:3306/racing ./mvnw test
```

## Tạo hash mật khẩu cho tài khoản mới

```
./mvnw -q -pl server -am compile exec:java -Dexec.mainClass=racing.server.db.PasswordHasher -Dexec.args="matkhau"
```

## Quy ước Git

Mỗi task một nhánh `feature/<mã-task>-<tên>`, mở Pull Request vào `main`, review chéo rồi merge. Chi tiết trong `PLAN.md`.
