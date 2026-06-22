#!/usr/bin/env bash
# Build a macOS .dmg installer with bundled JRE using jpackage.
# Requirements: JDK 21+, Maven 3.8+
set -euo pipefail

cd "$(dirname "$0")/.."

echo "==> Building JAR..."
mvn -q clean package -DskipTests

MAIN_JAR=target/budget-app.jar
LIBS_DIR=target/libs
OUT_DIR=target/installer

rm -rf "$OUT_DIR"
mkdir -p "$OUT_DIR"

echo "==> Running jpackage..."
jpackage \
  --type dmg \
  --name "BudgetApp" \
  --app-version "1.0.0" \
  --vendor "Budget App" \
  --description "Personal Budget Tracker" \
  --input target \
  --main-jar budget-app.jar \
  --main-class com.budget.Main \
  --java-options "--enable-preview" \
  --java-options "-Djava.library.path=\$APPDIR" \
  --dest "$OUT_DIR" \
  --mac-package-name "BudgetApp" \
  --icon packaging/mac/BudgetApp.icns 2>/dev/null || true

# If no icon found, build without it
if [ ! -f "$OUT_DIR"/*.dmg ]; then
  jpackage \
    --type dmg \
    --name "BudgetApp" \
    --app-version "1.0.0" \
    --vendor "Budget App" \
    --input target \
    --main-jar budget-app.jar \
    --main-class com.budget.Main \
    --dest "$OUT_DIR"
fi

echo ""
echo "==> Installer created:"
ls "$OUT_DIR"/*.dmg
