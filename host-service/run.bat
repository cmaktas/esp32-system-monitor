@echo off
echo ==========================================
echo   ESP32 Monitor Host - Deploy Script
echo ==========================================

echo [1/4] Building new code (Maven)...
call mvn clean package -DskipTests

echo [2/4] Stopping old service (if exists)...
if exist host-service.exe (
    host-service.exe stop
    host-service.exe uninstall
)

echo [3/4] Installing service configuration...
host-service.exe install

echo [4/4] Starting new service...
host-service.exe start

echo ==========================================
echo Deployment Completed!
echo Service Logs: target\winsw-logs\
echo App Logs: logs\hardware-monitor.log
echo ==========================================
pause