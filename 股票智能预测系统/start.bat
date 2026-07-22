@echo off
chcp 65001 >nul
echo ============================================================
echo   股票智能预测系统 (Stock Intelligent Prediction System)
echo   模块架构: frontend / backend / algorithm
echo ============================================================
echo.
echo 正在启动服务...
echo.

cd /d "%~dp0"
python run.py
pause
