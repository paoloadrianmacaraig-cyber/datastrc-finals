@echo off
title Kingpin MNL POS and Order Queue System
echo ===================================================
echo   Compiling Kingpin MNL System...
echo ===================================================
if not exist "bin" mkdir bin
javac -d bin src\com\kingpin\model\*.java src\com\kingpin\dsa\*.java src\com\kingpin\util\*.java src\com\kingpin\service\*.java src\com\kingpin\*.java

if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Compilation failed. Please check for errors above.
    pause
    exit /b %ERRORLEVEL%
)

echo ===================================================
echo   Launching Application...
echo ===================================================
java -cp bin com.kingpin.Main
pause
