#!/usr/bin/env bash
# Đóng gói bản chạy vào dist/: jar server, client, tools, SQL, file cấu hình mẫu và script chạy.
set -euo pipefail
cd "$(dirname "$0")/.."

./mvnw -B -ntp -q package
rm -rf dist
mkdir -p dist/db

cp server/target/racing-server.jar client/target/racing-client.jar tools/target/racing-tools.jar dist/
cp db/schema.sql db/seed.sql db/reset-db.sql dist/db/
cp server/src/main/resources/db.properties.example dist/db.properties.example
cp docker-compose.yml dist/

cat > dist/run-server.sh <<'EOF'
#!/usr/bin/env bash
cd "$(dirname "$0")"
case " $* " in *" --memory "*) ;; *)
  [ -f db.properties ] || { echo "Chưa có db.properties: sao chép db.properties.example và sửa mật khẩu (hoặc chạy với --memory để thử không cần MySQL)"; exit 1; } ;;
esac
exec java -jar racing-server.jar "$@"
EOF
cat > dist/run-client.sh <<'EOF'
#!/usr/bin/env bash
cd "$(dirname "$0")"
exec java -jar racing-client.jar "$@"
EOF
cat > dist/run-server.cmd <<'EOF'
@echo off
cd /d %~dp0
echo %* | findstr /C:"--memory" >nul
if errorlevel 1 if not exist db.properties (
  echo Chua co db.properties: sao chep db.properties.example va sua mat khau ^(hoac chay run-server.cmd --memory de thu khong can MySQL^)
  exit /b 1
)
java -jar racing-server.jar %*
EOF
cat > dist/run-client.cmd <<'EOF'
@echo off
cd /d %~dp0
java -jar racing-client.jar %*
EOF
cat > dist/README.txt <<'EOF'
Game dua xe thi dau doi khang online - ban chay

1. Cai MySQL 8 (hoac: docker compose up -d) va nap db/schema.sql roi db/seed.sql.
2. Sao chep db.properties.example thanh db.properties, sua user/mat khau.
3. Chay server: run-server.cmd (Windows) hoac ./run-server.sh
   Chua co MySQL? Chay run-server.cmd --memory (du lieu chi nam trong bo nho, mat khi tat server).
4. Chay client tren moi may: run-client.cmd hoac ./run-client.sh
   Client ket noi toi localhost:5000; doi may khac thi sua host trong man hinh dang nhap.
Tai khoan thu: alice, bob, carol, dave, erin, frank / mat khau 123456
EOF
chmod +x dist/*.sh
echo "Đã tạo dist/:"
ls -1 dist
