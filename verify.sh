#!/usr/bin/env bash
# Sanity-checks the generated project structure.
set -euo pipefail
cd "$(dirname "$0")"
KT=$(find . -name '*.kt' | wc -l)
GAMES=$(grep -rl '@Composable' app/src/main/java/com/neura/brain/game | wc -l)
ICONS=$(ls app/src/main/res/drawable/ic_*.xml | wc -l)
echo "Kotlin files:        $KT   (expect 34: 24 main + 1 test + sources counted once)"
echo "Game composables:    $GAMES (expect 20)"
echo "Vector icons:        $ICONS (expect 29 + launcher foreground = 30)"
echo "Manifest:            $([ -f app/src/main/AndroidManifest.xml ] && echo OK)"
echo "DataStore repo:      $(grep -q 'preferencesDataStore' app/src/main/java/com/neura/brain/data/Repository.kt && echo OK)"
echo "Milestones:          $(grep -q 'evaluateMilestones' app/src/main/java/com/neura/brain/data/Milestones.kt && echo OK)"
echo ""
echo "Next: open this folder in Android Studio (it will generate the Gradle wrapper),"
echo "or run 'gradle wrapper --gradle-version 8.9' if Gradle is installed, then:"
echo "  ./gradlew :app:assembleDebug   # build the APK"
echo "  ./gradlew test                 # run the 6 JVM logic tests"
