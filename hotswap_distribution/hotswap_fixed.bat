@echo on
setlocal enabledelayedexpansion
cls

echo =====================
echo    HOTSWAP TOOL v2
echo =====================
echo.

REM Check agent jar
if not exist "Naven-Modern-1337-agent.jar" (
    echo ERROR: Agent jar not found!
    pause
    exit /b 1
)

echo 1. Agent jar: OK
echo.

REM List Java processes
echo 2. Java processes:
jps -l
echo.

REM Get PID
set /p PID="Enter game PID: "
if "%PID%" == "" (
    echo ERROR: PID empty!
    pause
    exit /b 1
)

echo.
echo 3. Checking jattach...
where jattach >nul 2>nul
if errorlevel 1 (
    echo ERROR: jattach not found!
    echo Download: https://github.com/apangin/jattach/releases
    pause
    exit /b 1
)

echo jattach: OK
echo.

echo 4. Testing jattach command:
jattach %PID% properties 2>nul
if errorlevel 1 (
    echo WARNING: Cannot access process %PID%!
    echo Try running as Administrator.
    echo.
)

echo 5. Attaching agent...
echo Command: jattach %PID% load instrument false "Naven-Modern-1337-agent.jar"
echo.

REM Attach agent with full logging
jattach %PID% load instrument false "Naven-Modern-1337-agent.jar"

echo.
echo 6. Done! Check game console for logs.
echo.
pause