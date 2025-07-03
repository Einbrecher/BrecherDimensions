@echo off
echo Cleaning up Fabric server environment...

:: Kill any running Java processes
taskkill /F /IM java.exe 2>nul

:: Wait a moment
timeout /t 2 >nul

:: Remove lock files
del /F /Q fabric\run\brecher_test\session.lock 2>nul
del /F /Q fabric\run\world\session.lock 2>nul

:: Remove log locks
del /F /Q fabric\run\logs\*.log 2>nul

echo Starting Fabric server (suppressing Architectury error)...
:: Redirect stderr to nul to suppress the StringIndexOutOfBoundsException
call gradlew.bat :fabric:runServer --no-daemon 2>nul

pause