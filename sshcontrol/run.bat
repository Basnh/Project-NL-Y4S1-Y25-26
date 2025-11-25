@echo off
REM SSH Control System - Start/Stop Script
REM This batch file is used to run and stop the SSH Control application
REM Updated to support Maven Spring Boot runtime

setlocal enabledelayedexpansion

:menu
cls
echo.
echo ============================================
echo     SSH CONTROL SYSTEM - MENU
echo ============================================
echo.
echo 1. Start application (Maven Spring Boot)
echo 2. Build and Start application
echo 3. Stop the application
echo 4. Restart the application
echo 5. Check system status
echo 6. Clean and rebuild
echo 7. Exit
echo.
set /p choice="Please select an option (1-7): "

if "%choice%"=="1" goto start_mvn
if "%choice%"=="2" goto build_and_start
if "%choice%"=="3" goto stop_app
if "%choice%"=="4" goto restart_app
if "%choice%"=="5" goto check_status
if "%choice%"=="6" goto clean_build
if "%choice%"=="7" goto exit_menu
echo Invalid choice. Please try again.
timeout /t 2 /nobreak
goto menu

:start_mvn
cls
echo.
echo ============================================
echo     STARTING APPLICATION (Maven Spring Boot)
echo ============================================
echo.
cd /d "%~dp0"
echo Current directory: %CD%
echo.
echo Checking Maven installation...
where mvn >nul 2>&1
if errorlevel 1 (
    echo Error: Maven is not installed or not in PATH
    echo Please install Maven and add it to system PATH
    echo.
    pause
    goto menu
)
echo Maven found successfully!
echo.
echo Starting application with: mvn spring-boot:run
echo.
timeout /t 2 /nobreak
call mvn spring-boot:run
if errorlevel 1 (
    echo.
    echo Error: Failed to start application
    echo.
    pause
)
goto menu

:build_and_start
cls
echo.
echo ============================================
echo     BUILD AND START APPLICATION
echo ============================================
echo.
cd /d "%~dp0"
echo Current directory: %CD%
echo.
echo Stopping any running instances...
taskkill /IM java.exe /F 2>nul
timeout /t 2 /nobreak
echo.
echo Checking Maven installation...
where mvn >nul 2>&1
if errorlevel 1 (
    echo Error: Maven is not installed or not in PATH
    echo Please install Maven and add it to system PATH
    echo.
    pause
    goto menu
)
echo Maven found successfully!
echo.
echo Building project...
call mvn clean package -DskipTests
if errorlevel 1 (
    echo.
    echo Error: Build failed
    echo.
    pause
    goto menu
)
echo.
echo Build completed successfully!
echo.
echo Starting application with: mvn spring-boot:run
echo.
timeout /t 2 /nobreak
call mvn spring-boot:run
if errorlevel 1 (
    echo.
    echo Error: Failed to start application
    echo.
    pause
)
goto menu

:stop_app
cls
echo.
echo ============================================
echo     STOP APPLICATION
echo ============================================
echo.
echo Stopping SSH Control application...
echo.
taskkill /IM java.exe /F 2>nul
if errorlevel 1 (
    echo No running Java process found.
) else (
    echo Application stopped successfully!
)
echo.
pause
goto menu

:restart_app
cls
echo.
echo ============================================
echo     RESTART APPLICATION
echo ============================================
echo.
cd /d "%~dp0"
echo Stopping any running instances...
taskkill /IM java.exe /F 2>nul
timeout /t 2 /nobreak
echo.
echo Starting application with: mvn spring-boot:run
echo.
timeout /t 2 /nobreak
call mvn spring-boot:run
if errorlevel 1 (
    echo.
    echo Error: Failed to restart application
    echo.
    pause
)
goto menu

:check_status
cls
echo.
echo ============================================
echo     SYSTEM STATUS
echo ============================================
echo.
tasklist /FI "IMAGENAME eq java.exe" >nul 2>&1
if %errorlevel% equ 0 (
    echo [OK] Application is running
    echo.
    tasklist /FI "IMAGENAME eq java.exe" /FO TABLE
    echo.
    echo Access application at: http://localhost:8080
) else (
    echo [STOPPED] Application is not running
)
echo.
echo Checking project files...
echo.
if exist "pom.xml" (
    echo [OK] Maven configuration (pom.xml) found
) else (
    echo [ERROR] pom.xml not found
)
echo.
if exist "src\main\resources\application.properties" (
    echo [OK] Application configuration found
) else (
    echo [ERROR] application.properties not found
)
echo.
where mvn >nul 2>&1
if %errorlevel% equ 0 (
    echo [OK] Maven is installed and available
) else (
    echo [WARNING] Maven not found in PATH
)
echo.
pause
goto menu

:clean_build
cls
echo.
echo ============================================
echo     CLEAN AND REBUILD PROJECT
echo ============================================
echo.
cd /d "%~dp0"
echo Current directory: %CD%
echo.
echo Stopping any running instances...
taskkill /IM java.exe /F 2>nul
timeout /t 2 /nobreak
echo.
echo Checking Maven installation...
where mvn >nul 2>&1
if errorlevel 1 (
    echo Error: Maven is not installed or not in PATH
    echo.
    pause
    goto menu
)
echo Maven found successfully!
echo.
echo Running clean package build...
call mvn clean package -DskipTests
if errorlevel 1 (
    echo.
    echo Error: Build failed
    echo.
    pause
    goto menu
)
echo.
echo Build completed successfully!
echo.
echo Would you like to start the application now? (Y/N)
set /p start_choice="Enter choice: "
if /i "%start_choice%"=="Y" (
    echo.
    echo Starting application with: mvn spring-boot:run
    echo.
    timeout /t 2 /nobreak
    call mvn spring-boot:run
    if errorlevel 1 (
        echo.
        echo Error: Failed to start application
        echo.
        pause
    )
)
goto menu

:exit_menu
cls
echo.
echo Exiting SSH Control system menu...
echo.
timeout /t 1 /nobreak
endlocal
exit /b 0

