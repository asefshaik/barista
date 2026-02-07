#!/bin/bash

# Barista Queue Management System - Backend Setup Script

echo ""
echo "======================================"
echo "Barista Queue System - Backend Setup"
echo "======================================"
echo ""

# Check Java version
if ! command -v java &> /dev/null; then
    echo "ERROR: Java is not installed"
    exit 1
fi

java -version 2>&1 | grep -q "17"
if [ $? -ne 0 ]; then
    echo "ERROR: Java 17+ is required"
    exit 1
fi

echo "[OK] Java 17 detected"

# Check Maven
if ! command -v mvn &> /dev/null; then
    echo "ERROR: Maven is not installed"
    exit 1
fi

echo "[OK] Maven detected"

# Clean and build
echo ""
echo "Building project..."
mvn clean package

if [ $? -ne 0 ]; then
    echo "ERROR: Build failed"
    exit 1
fi

echo ""
echo "======================================"
echo "Build Successful!"
echo "======================================"
echo ""
echo "Starting Spring Boot server..."
mvn spring-boot:run
