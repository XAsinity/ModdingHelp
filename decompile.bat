@echo off
REM Example script demonstrating JAR decompilation on Windows

echo === HytaleSourceAPI JAR Decompiler Example ===
echo.

if "%~1"=="" (
    echo Usage: %0 ^<path-to-jar-file^>
    echo.
    echo Example:
    echo   %0 hytale-client.jar
    echo   %0 C:\path\to\myapp.jar
    exit /b 1
)

set JAR_FILE=%~1

REM Check if file exists
if not exist "%JAR_FILE%" (
    echo Error: File not found: %JAR_FILE%
    exit /b 1
)

REM Get the base name of the jar file
for %%F in ("%JAR_FILE%") do set JAR_NAME=%%~nF

echo Decompiling: %JAR_FILE%
echo Output will be saved to: decompiled\%JAR_NAME%\
echo.

REM Run the decompiler
python decompile.py "%JAR_FILE%"

if %ERRORLEVEL% EQU 0 (
    echo.
    echo Success! Decompilation completed successfully!
    echo.
    echo You can now explore the decompiled source code in: decompiled\%JAR_NAME%\
    echo.
    echo Example commands:
    echo   dir decompiled\%JAR_NAME%\
    echo   dir /s /b decompiled\%JAR_NAME%\*.java
) else (
    echo.
    echo Error: Decompilation failed. Please check the error messages above.
    exit /b 1
)
