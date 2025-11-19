@echo off
REM SSH Control System - Start/Stop Script
REM This batch file is used to run and stop the SSH Control application

setlocal enabledelayedexpansion

:menu
cls
echo.
echo ============================================
echo     SSH CONTROL SYSTEM - MENU
echo ============================================
echo.
echo 1. Start the application
echo 2. Stop the application
echo 3. Restart the application
echo 4. Check system logs
echo 5. Stop application (Graceful shutdown)
echo 6. Exit
echo.
set /p choice="Please select an option (1-6): "

if "%choice%"=="1" goto start_app
if "%choice%"=="2" goto stop_app
if "%choice%"=="3" goto restart_app
if "%choice%"=="4" goto check_logs
if "%choice%"=="5" goto shutdown_system
if "%choice%"=="6" goto exit_menu
echo Invalid choice. Please try again.
timeout /t 2 /nobreak
goto menu

:start_app
cls
echo.
echo Starting SSH Control application...
echo.
cd /d "%~dp0"
if exist "target\sshcontrol-0.0.1-SNAPSHOT.jar.original" (
    echo Application is starting in background...
    echo.
    echo Launching application...
    timeout /t 2 /nobreak
    start "" java -jar target\sshcontrol-0.0.1-SNAPSHOT.jar.original
    echo.
    echo Application started successfully! Access it at http://localhost:8080
    echo.
    timeout /t 3 /nobreak
    goto menu
) else (
    echo Error: JAR file not found. Please build the project first.
    echo.
    pause
    goto menu
)

:stop_app
cls
echo.
echo Stopping SSH Control application...
echo.
taskkill /FI "WINDOWTITLE eq SSH*" /T /F 2>nul
if errorlevel 1 (
    echo Attempting to stop Java process...
    taskkill /IM java.exe /F 2>nul
    if errorlevel 1 (
        echo No running SSH Control process found.
    ) else (
        echo Application stopped successfully.
    )
) else (
    echo Application stopped successfully.
)
echo.
pause
goto menu

:restart_app
cls
echo.
echo Restarting SSH Control application...
echo.
taskkill /IM java.exe /F 2>nul
timeout /t 2 /nobreak
cd /d "%~dp0"
if exist "target\sshcontrol-0.0.1-SNAPSHOT.jar.original" (
    echo Application is restarting in background...
    echo.
    echo Launching application...
    timeout /t 2 /nobreak
    start "" java -jar target\sshcontrol-0.0.1-SNAPSHOT.jar.original
    echo.
    echo Application restarted successfully! Access it at http://localhost:8080
    echo.
    timeout /t 3 /nobreak
    goto menu
) else (
    echo Error: JAR file not found. Please build the project first.
    echo.
    pause
    goto menu
)

:check_logs
cls
echo.
echo ============================================
echo     SYSTEM LOGS
echo ============================================
echo.
echo Getting latest logs from running Java process...
echo.
REM Get the PID of running java process
for /f "tokens=2" %%a in ('tasklist /FI "IMAGENAME eq java.exe" /FO TABLE ^| findstr java') do (
    set PID=%%a
)

if defined PID (
    echo Found Java process with PID: %PID%
    echo.
    echo Note: To view real-time logs, check the Java console window.
    echo.
    echo Log file information:
    echo - Application logs are displayed in the Java console window
    echo - Database: PostgreSQL connection logs available
    echo.
) else (
    echo No running Java process found.
    echo.
    echo Please start the application first (option 1).
    echo.
)

echo Checking application files...
echo.
if exist "target\sshcontrol-0.0.1-SNAPSHOT.jar.original" (
    echo [OK] JAR file exists: target\sshcontrol-0.0.1-SNAPSHOT.jar.original
    for /f %%a in ('dir /b target\sshcontrol-0.0.1-SNAPSHOT.jar.original') do echo File size: %%~za bytes
) else (
    echo [ERROR] JAR file not found
)

echo.
if exist "src\main\resources\application.properties" (
    echo [OK] Configuration file exists
) else (
    echo [ERROR] Configuration file not found
)

echo.
echo Application running on: http://localhost:8080
echo.
pause
goto menu

:shutdown_system
cls
echo.
echo ============================================
echo     STOP APPLICATION
echo ============================================
echo.
echo Stopping SSH Control application...
echo.
taskkill /FI "WINDOWTITLE eq SSH*" /T /F 2>nul
if errorlevel 1 (
    echo Attempting to stop Java process...
    taskkill /IM java.exe /F 2>nul
    if errorlevel 1 (
        echo No running SSH Control process found.
    ) else (
        echo Application stopped successfully!
    )
) else (
    echo Application stopped successfully!
)
echo.
pause
goto menu

:restart_system
cls
echo.
echo ============================================
echo     RESTART APPLICATION
echo ============================================
echo.
echo Restarting SSH Control application...
echo.
taskkill /IM java.exe /F 2>nul
timeout /t 2 /nobreak
cd /d "%~dp0"
if exist "target\sshcontrol-0.0.1-SNAPSHOT.jar.original" (
    echo Application is restarting in background...
    echo.
    echo Launching application...
    timeout /t 2 /nobreak
    start "" java -jar target\sshcontrol-0.0.1-SNAPSHOT.jar.original
    echo.
    echo Application restarted successfully! Access it at http://localhost:8080
    echo.
    timeout /t 3 /nobreak
    goto menu
) else (
    echo Error: JAR file not found. Please build the project first.
    echo.
    pause
    goto menu
)

:exit_menu
cls
echo.
echo Exiting SSH Control system menu...
echo.
timeout /t 1 /nobreak
endlocal
exit /b 0
