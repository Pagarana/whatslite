@echo off
echo =====================================
echo Firebase Debug LogCat Commands
echo =====================================
echo.

echo 1. Firebase Connection Test:
echo adb logcat -s FirebaseTestUtils:D DEBUG_FIREBASE:D
echo.

echo 2. Firebase Manager Debug:  
echo adb logcat -s FirebaseManager:D
echo.

echo 3. User Join Process:
echo adb logcat -s FirebaseManager:D -v time | findstr "JOIN\|User joined\|User ID"
echo.

echo 4. Online Users Update:
echo adb logcat -s OnlineUsers:D -v time
echo.

echo 5. Message Send/Receive:
echo adb logcat -s ChatSend:D ChatReceive:D -v time
echo.

echo 6. All Firebase Related:
echo adb logcat -v time | findstr "Firebase\|FIREBASE\|DeepL\|ChatSend\|ChatReceive\|OnlineUsers"
echo.

echo 7. Real-time monitoring:
echo adb logcat -v time -s FirebaseManager:D OnlineUsers:D ChatSend:D ChatReceive:D FirebaseTestUtils:D
echo.

echo =====================================
echo Copy ve paste one of the commands above
echo =====================================
pause