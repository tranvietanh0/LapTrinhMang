@echo off
REM Đóng gói bản chạy vào dist\ (phiên bản Windows của build-dist.sh)
setlocal
cd /d %~dp0\..

call mvnw.cmd -B -ntp -q package || exit /b 1
if exist dist rmdir /s /q dist
mkdir dist\db

copy /y server\target\racing-server.jar dist\ >nul
copy /y client\target\racing-client.jar dist\ >nul
copy /y tools\target\racing-tools.jar dist\ >nul
copy /y db\schema.sql dist\db\ >nul
copy /y db\seed.sql dist\db\ >nul
copy /y db\reset-db.sql dist\db\ >nul
copy /y server\src\main\resources\db.properties.example dist\db.properties.example >nul
copy /y docker-compose.yml dist\ >nul

(
echo @echo off
echo cd /d %%~dp0
echo echo %%* ^| findstr /C:"--memory" ^>nul
echo if errorlevel 1 if not exist db.properties ^(
echo   echo Chua co db.properties: sao chep db.properties.example va sua mat khau ^(hoac chay run-server.cmd --memory de thu khong can MySQL^)
echo   exit /b 1
echo ^)
echo java -jar racing-server.jar %%*
) > dist\run-server.cmd

(
echo @echo off
echo cd /d %%~dp0
echo java -jar racing-client.jar %%*
) > dist\run-client.cmd

(
echo Game dua xe thi dau doi khang online - ban chay
echo.
echo 1. Cai MySQL 8 ^(hoac: docker compose up -d^) va nap db\schema.sql roi db\seed.sql.
echo 2. Sao chep db.properties.example thanh db.properties, sua user/mat khau.
echo 3. Chay server: run-server.cmd
echo    Chua co MySQL? Chay run-server.cmd --memory ^(du lieu chi nam trong bo nho, mat khi tat server^).
echo 4. Chay client tren moi may: run-client.cmd
echo    Client ket noi toi localhost:5000; doi may khac thi sua host trong man hinh dang nhap.
echo Tai khoan thu: alice, bob, carol, dave, erin, frank / mat khau 123456
) > dist\README.txt

echo Da tao dist\:
dir /b dist
endlocal
