@echo off
set ADB_PATH=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe

echo =====================================
echo WhatsLite Firebase Debug Commands
echo =====================================
echo.
echo Connected devices:
"%ADB_PATH%" devices
echo.

echo Choose a debug option:
echo.
echo 1. Firebase Connection Test
echo 2. User Join Process Debug  
echo 3. Message Send/Receive Debug
echo 4. Online Users List Debug
echo 5. All Firebase Logs (Comprehensive)
echo 6. Clear Logcat Buffer
echo 7. Real-time Firebase Monitor
echo.

set /p choice=Enter your choice (1-7): 

if "%choice%"=="1" (
    echo Starting Firebase Connection Test...
    "%ADB_PATH%" logcat -v time -s FirebaseTestUtils:D DEBUG_FIREBASE:D
) else if "%choice%"=="2" (
    echo Starting User Join Debug...
    "%ADB_PATH%" logcat -v time -s FirebaseManager:D | findstr "JOIN\|User joined\|FIREBASE"
) else if "%choice%"=="3" (
    echo Starting Message Debug...
    "%ADB_PATH%" logcat -v time -s ChatSend:D ChatReceive:D
) else if "%choice%"=="4" (
    echo Starting Online Users Debug...
    "%ADB_PATH%" logcat -v time -s OnlineUsers:D
) else if "%choice%"=="5" (
    echo Starting Comprehensive Firebase Debug...
    "%ADB_PATH%" logcat -v time | findstr "Firebase\|FIREBASE\|DeepL\|ChatSend\|ChatReceive\|OnlineUsers"
) else if "%choice%"=="6" (
    echo Clearing logcat buffer...
    "%ADB_PATH%" logcat -c
    echo Logcat buffer cleared.
    pause
) else if "%choice%"=="7" (
    echo Starting Real-time Firebase Monitor...
    echo Press Ctrl+C to stop
    "%ADB_PATH%" logcat -v time -s FirebaseManager:D OnlineUsers:D ChatSend:D ChatReceive:D FirebaseTestUtils:D DeepLTranslationService:D
) else (
    echo Invalid choice. Exiting...
)

pause