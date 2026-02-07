@echo off
REM Frontend Setup Script

echo.
echo ======================================
echo Barista Queue System - Frontend Setup
echo ======================================
echo.

REM Check Node.js
node --version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Node.js is not installed
    exit /b 1
)

echo [OK] Node.js detected: 
node --version

REM Check npm
npm --version >nul 2>&1
if errorlevel 1 (
    echo ERROR: npm is not installed
    exit /b 1
)

echo [OK] npm detected:
npm --version

REM Install dependencies
echo.
echo Installing dependencies...
call npm install

if errorlevel 1 (
    echo ERROR: npm install failed
    exit /b 1
)

echo.
echo ======================================
echo Dependencies Installed!
echo ======================================
echo.
echo Starting development server...
echo Application will open at http://localhost:3000
echo.

call npm start

pause
