@echo off
REM Barista Queue Management System - Backend Setup Script

echo.
echo ======================================
echo Barista Queue System - Backend Setup
echo ======================================
echo.

REM Check Java version
java -version 2>&1 | find "17" >nul
if errorlevel 1 (
    echo ERROR: Java 17+ is required
    exit /b 1
)

echo [OK] Java 17 detected

REM Check Maven
mvn -version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Maven is not installed
    exit /b 1
)

echo [OK] Maven detected

REM Clean and build
echo.
echo Building project...
mvn clean package

if errorlevel 1 (
    echo ERROR: Build failed
    exit /b 1
)

echo.
echo ======================================
echo Build Successful!
echo ======================================
echo.
echo Starting Spring Boot server...
mvn spring-boot:run

pause
