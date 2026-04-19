#!/bin/sh
# bootstrap.sh — Download Gradle and regenerate the wrapper jar.
# Run this once after cloning if gradle-wrapper.jar is missing.

set -e

GRADLE_VERSION="8.11"
GRADLE_URL="https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip"
GRADLE_ZIP="/tmp/gradle-${GRADLE_VERSION}-bin.zip"
GRADLE_DIR="/tmp/gradle-${GRADLE_VERSION}"

echo "Downloading Gradle ${GRADLE_VERSION}..."
curl -L "$GRADLE_URL" -o "$GRADLE_ZIP"

echo "Extracting..."
unzip -q "$GRADLE_ZIP" -d /tmp/

echo "Regenerating wrapper..."
"${GRADLE_DIR}/bin/gradle" wrapper --gradle-version="${GRADLE_VERSION}"

echo "Done. You can now use ./gradlew normally."
