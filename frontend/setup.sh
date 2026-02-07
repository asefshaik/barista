#!/bin/bash

# Frontend Setup Script

echo ""
echo "======================================"
echo "Barista Queue System - Frontend Setup"
echo "======================================"
echo ""

# Check Node.js
if ! command -v node &> /dev/null; then
    echo "ERROR: Node.js is not installed"
    exit 1
fi

echo "[OK] Node.js detected:"
node --version

# Check npm
if ! command -v npm &> /dev/null; then
    echo "ERROR: npm is not installed"
    exit 1
fi

echo "[OK] npm detected:"
npm --version

# Install dependencies
echo ""
echo "Installing dependencies..."
npm install

if [ $? -ne 0 ]; then
    echo "ERROR: npm install failed"
    exit 1
fi

echo ""
echo "======================================"
echo "Dependencies Installed!"
echo "======================================"
echo ""
echo "Starting development server..."
echo "Application will open at http://localhost:3000"
echo ""

npm start
