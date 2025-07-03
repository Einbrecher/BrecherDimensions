@echo off
echo Cleaning up NeoForge server environment...

:: Kill any running Java processes
taskkill /F /IM java.exe 2>nul

:: Wait a moment
timeout /t 2 >nul

:: Remove lock files
del /F /Q neoforge\run\brecher_test\session.lock 2>nul
del /F /Q neoforge\run\world\session.lock 2>nul

:: Remove log locks
del /F /Q neoforge\run\logs\*.log 2>nul

echo Starting NeoForge server...
call gradlew.bat :neoforge:runServer --no-daemon

pause