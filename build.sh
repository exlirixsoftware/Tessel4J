#!/usr/bin/env bash
# Builds Tessel4J with the JDK tools only (no Maven/Gradle needed).
#
#   ./build.sh         -> dist/tessel-laf-<v>.jar, tessel-laf-<v>-sources.jar, tessel-gallery-<v>.jar
#   ./build.sh --run   -> builds, then starts the gallery
#
# Requires JDK 17+. Uses $JAVA_HOME, or the JDK of the 'java' on the PATH.
set -euo pipefail

VERSION=1.1.0
ROOT="$(cd "$(dirname "$0")" && pwd)"
# Git Bash / Cygwin: hand Windows-style paths to the Windows JDK tools.
if command -v cygpath >/dev/null 2>&1; then ROOT="$(cygpath -m "$ROOT")"; fi
BUILD="$ROOT/build"
DIST="$ROOT/dist"

JDK="${JAVA_HOME:-}"
if [ -z "$JDK" ]; then
  JDK="$(java -XshowSettings:properties -version 2>&1 | sed -n 's/^ *java\.home = //p' | tr -d '\r')"
fi
JAVAC="$JDK/bin/javac"
JAR="$JDK/bin/jar"
JAVA="$JDK/bin/java"
[ -x "$JAVAC" ] || [ -x "$JAVAC.exe" ] || { echo "No JDK found. Set JAVA_HOME to a JDK 17+." >&2; exit 1; }
echo "Using JDK: $JDK"

# Only this version's jars are replaced, so the jars of earlier releases stay in dist/.
rm -rf "$BUILD"
mkdir -p "$BUILD/lib" "$BUILD/gallery" "$DIST"
rm -f "$DIST"/tessel-laf-$VERSION.jar "$DIST"/tessel-laf-$VERSION-sources.jar "$DIST"/tessel-gallery-$VERSION.jar
LINT='-Xlint:all,-serial,-this-escape'

# JNA gives TesselFrame native Windows chrome (shadow, rounded corners, Snap Layouts); see lib/README.md.
if command -v cygpath >/dev/null 2>&1; then CP_SEP=';'; else CP_SEP=':'; fi
JNA_JAR=$(ls "$ROOT"/lib/jna-[0-9]*.jar 2>/dev/null | head -1 || true)
JNA_PLATFORM_JAR=$(ls "$ROOT"/lib/jna-platform-*.jar 2>/dev/null | head -1 || true)
[ -n "$JNA_JAR" ] && [ -n "$JNA_PLATFORM_JAR" ] || { echo "lib/jna-*.jar and lib/jna-platform-*.jar are required (see README.md)." >&2; exit 1; }
JNA_CP="$JNA_JAR$CP_SEP$JNA_PLATFORM_JAR"

echo "Compiling tessel-laf..."
find "$ROOT/tessel-laf/src/main/java" -name '*.java' > "$BUILD/lib-sources.txt"
"$JAVAC" --release 17 -encoding UTF-8 "$LINT" -cp "$JNA_CP" -d "$BUILD/lib" @"$BUILD/lib-sources.txt"
# Bundle JNA's classes (and its native dispatch library) so consumers still only need this one jar.
(cd "$BUILD/lib" && "$JAR" xf "$JNA_JAR" && "$JAR" xf "$JNA_PLATFORM_JAR" && rm -f META-INF/MANIFEST.MF)
cat > "$BUILD/lib-manifest.txt" <<EOF
Automatic-Module-Name: net.tessel.laf
Implementation-Title: Tessel Look and Feel
Implementation-Version: $VERSION
Implementation-Vendor: Tessel
EOF
"$JAR" --create --file "$DIST/tessel-laf-$VERSION.jar" --manifest "$BUILD/lib-manifest.txt" -C "$BUILD/lib" .
"$JAR" --create --file "$DIST/tessel-laf-$VERSION-sources.jar" -C "$ROOT/tessel-laf/src/main/java" .

echo "Compiling tessel-gallery..."
find "$ROOT/tessel-gallery/src/main/java" -name '*.java' > "$BUILD/gallery-sources.txt"
"$JAVAC" --release 17 -encoding UTF-8 "$LINT" -cp "$DIST/tessel-laf-$VERSION.jar" -d "$BUILD/gallery" @"$BUILD/gallery-sources.txt"
cp -R "$ROOT/tessel-gallery/src/main/resources/." "$BUILD/gallery/"
cat > "$BUILD/gallery-manifest.txt" <<EOF
Class-Path: tessel-laf-$VERSION.jar
Implementation-Title: Tessel Gallery
Implementation-Version: $VERSION
EOF
"$JAR" --create --file "$DIST/tessel-gallery-$VERSION.jar" --manifest "$BUILD/gallery-manifest.txt" \
  --main-class net.tessel.gallery.TesselGallery -C "$BUILD/gallery" .

echo
ls -l "$DIST"

if [ "${1:-}" = "--run" ]; then
  "$JAVA" -jar "$DIST/tessel-gallery-$VERSION.jar"
fi
