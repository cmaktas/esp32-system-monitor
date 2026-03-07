@echo off
cd /d "%~dp0"

echo ==========================================
echo   ESP32 Monitor Host - Deploy Script
echo ==========================================

if "%LHM_USERNAME%"=="" set /p LHM_USERNAME="Enter LHM Username: "
if "%LHM_PASSWORD%"=="" set /p LHM_PASSWORD="Enter LHM Password: "

echo [1/4] Stopping old service (if exists)...
if exist host-service.exe (
    host-service.exe stop
    host-service.exe uninstall
)

echo [2/4] Building new code (Maven)...
call mvn clean package -DskipTests

echo [3/4] Installing service configuration...
host-service.exe install

echo [4/4] Starting new service...
host-service.exe start

echo ==========================================
echo Deployment Completed!
echo ==========================================
pause