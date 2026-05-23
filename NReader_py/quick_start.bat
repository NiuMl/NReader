@echo off
echo ========================================
echo NReader Backend Quick Start
echo ========================================
echo.

echo [1] Installing dependencies...
pip install -r requirements.txt
if errorlevel 1 (
    echo Error installing dependencies
    pause
    exit /b 1
)
echo Dependencies installed successfully!
echo.

echo [2] Starting server...
start "NReader Backend" python app.py

echo Waiting for server to start...
timeout /t 3 /nobreak > nul

echo [3] Testing API...
python test_api.py

echo.
echo ========================================
echo Server is running at http://localhost:5000
echo Press Ctrl+C to stop the server
echo ========================================
pause