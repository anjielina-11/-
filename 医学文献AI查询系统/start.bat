@echo off
echo Starting MedSearch Backend...
cd /d E:\work\码道智医\backend
start "MedSearch Backend" python run.py

echo Waiting for backend to start...
timeout /t 5 /nobreak >nul

echo Starting MedSearch Frontend...
cd /d E:\work\码道智医\frontend
start "MedSearch Frontend" npm run dev

echo.
echo Both services are starting...
echo Backend:  http://localhost:5000
echo Frontend: http://localhost:5173
echo.
echo Close the two windows to stop the services.
pause
