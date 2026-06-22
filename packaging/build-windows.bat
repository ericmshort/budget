@echo off
REM Build a Windows .exe installer with bundled JRE using jpackage.
REM Requirements: JDK 21+, Maven 3.8+, WiX Toolset 3.x (for MSI) or NSIS (for exe)

cd /d "%~dp0\.."

echo =^> Building JAR...
call mvn -q clean package -DskipTests
if errorlevel 1 goto :error

set OUT_DIR=target\installer
if exist "%OUT_DIR%" rd /s /q "%OUT_DIR%"
mkdir "%OUT_DIR%"

echo =^> Running jpackage...
jpackage ^
  --type exe ^
  --name "BudgetApp" ^
  --app-version "1.0.0" ^
  --vendor "Budget App" ^
  --description "Personal Budget Tracker" ^
  --input target ^
  --main-jar budget-app.jar ^
  --main-class com.budget.Main ^
  --dest "%OUT_DIR%" ^
  --win-dir-chooser ^
  --win-menu ^
  --win-shortcut ^
  --win-shortcut-prompt

if errorlevel 1 goto :error

echo.
echo =^> Installer created in %OUT_DIR%\
dir "%OUT_DIR%\*.exe"
goto :end

:error
echo Build failed.
exit /b 1

:end
