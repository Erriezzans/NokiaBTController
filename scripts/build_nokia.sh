#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TOOLS_DIR="${RUNNER_TEMP:-$ROOT_DIR/.build-tools}/nokia-j2me"
LIB_DIR="$TOOLS_DIR/lib"
PROGUARD_DIR="$TOOLS_DIR/proguard-7.8.2"
BUILD_DIR="$ROOT_DIR/build/nokia"
CLASSES_DIR="$BUILD_DIR/classes"
PREVERIFIED_DIR="$BUILD_DIR/preverified"
DIST_DIR="$ROOT_DIR/dist"

ECJ_VERSION="3.38.0"
API_VERSION="2.0.4"
PROGUARD_VERSION="7.8.2"

mkdir -p "$LIB_DIR" "$PROGUARD_DIR" "$CLASSES_DIR" "$PREVERIFIED_DIR" "$DIST_DIR"

download() {
  local url="$1"
  local output="$2"
  if [[ ! -s "$output" ]]; then
    echo "Downloading $(basename "$output")"
    curl --fail --location --retry 3 --retry-delay 2 --silent --show-error "$url" -o "$output"
  fi
}

ECJ_JAR="$TOOLS_DIR/ecj-$ECJ_VERSION.jar"
CLDC_JAR="$LIB_DIR/cldcapi11.jar"
MIDP_JAR="$LIB_DIR/midpapi20.jar"
JSR82_JAR="$LIB_DIR/microemu-jsr-82.jar"
PROGUARD_ZIP="$TOOLS_DIR/proguard-$PROGUARD_VERSION.zip"

download "https://repo.maven.apache.org/maven2/org/eclipse/jdt/ecj/$ECJ_VERSION/ecj-$ECJ_VERSION.jar" "$ECJ_JAR"
download "https://repo.maven.apache.org/maven2/org/microemu/cldcapi11/$API_VERSION/cldcapi11-$API_VERSION.jar" "$CLDC_JAR"
download "https://repo.maven.apache.org/maven2/org/microemu/midpapi20/$API_VERSION/midpapi20-$API_VERSION.jar" "$MIDP_JAR"
download "https://repo.maven.apache.org/maven2/org/microemu/microemu-jsr-82/$API_VERSION/microemu-jsr-82-$API_VERSION.jar" "$JSR82_JAR"
download "https://github.com/Guardsquare/proguard/releases/download/v$PROGUARD_VERSION/proguard-$PROGUARD_VERSION.zip" "$PROGUARD_ZIP"

if [[ ! -x "$PROGUARD_DIR/bin/proguard.sh" ]]; then
  unzip -q -o "$PROGUARD_ZIP" -d "$TOOLS_DIR"
fi
PROGUARD="$PROGUARD_DIR/bin/proguard.sh"
test -x "$PROGUARD"

rm -rf "$CLASSES_DIR" "$PREVERIFIED_DIR"
mkdir -p "$CLASSES_DIR" "$PREVERIFIED_DIR" "$DIST_DIR"

find "$ROOT_DIR/nokia/src" -name '*.java' -print | sort > "$BUILD_DIR/sources.txt"
if [[ ! -s "$BUILD_DIR/sources.txt" ]]; then
  echo "No Nokia Java sources found under nokia/src" >&2
  exit 1
fi

echo "Compiling MIDlet against CLDC 1.1, MIDP 2.0 and JSR-82 APIs"
java -jar "$ECJ_JAR" -nowarn \
  -source 1.3 -target 1.1 \
  -encoding UTF-8 \
  -bootclasspath "$CLDC_JAR:$MIDP_JAR" \
  -classpath "$CLDC_JAR:$MIDP_JAR:$JSR82_JAR" \
  -d "$CLASSES_DIR" @"$BUILD_DIR/sources.txt"

echo "Creating intermediate class archive"
jar cf "$BUILD_DIR/NokiaBTController-unverified.jar" -C "$CLASSES_DIR" .

echo "CLDC preverification with ProGuard"
"$PROGUARD" \
  -injars "$BUILD_DIR/NokiaBTController-unverified.jar" \
  -outjars "$BUILD_DIR/NokiaBTController-preverified.jar" \
  -libraryjars "$CLDC_JAR" \
  -libraryjars "$MIDP_JAR" \
  -libraryjars "$JSR82_JAR" \
  -microedition \
  -dontshrink -dontoptimize -dontobfuscate -dontwarn -dontnote \
  -keep 'public class com.erriezzans.nokiabt.NokiaBTController extends javax.microedition.midlet.MIDlet { public *; }'

MANIFEST="$BUILD_DIR/MANIFEST.MF"
cat > "$MANIFEST" <<'EOF'
Manifest-Version: 1.0
MIDlet-Name: Nokia BT Controller
MIDlet-Version: 1.0.0
MIDlet-Vendor: Erriezzans
MIDlet-1: Nokia BT Controller,,com.erriezzans.nokiabt.NokiaBTController
MIDlet-Description: Bluetooth TV remote, mouse and gamepad for Android
MicroEdition-Configuration: CLDC-1.1
MicroEdition-Profile: MIDP-2.0
MIDlet-Permissions: javax.microedition.io.Connector.bluetooth.client

EOF

JAR_OUT="$DIST_DIR/NokiaBTController-1.0.0.jar"
JAD_OUT="$DIST_DIR/NokiaBTController-1.0.0.jad"
rm -f "$JAR_OUT" "$JAD_OUT"
jar cfm "$JAR_OUT" "$MANIFEST" -C "$PREVERIFIED_DIR" .

JAR_SIZE="$(wc -c < "$JAR_OUT" | tr -d '[:space:]')"
cat > "$JAD_OUT" <<EOF
MIDlet-Name: Nokia BT Controller
MIDlet-Version: 1.0.0
MIDlet-Vendor: Erriezzans
MIDlet-1: Nokia BT Controller,,com.erriezzans.nokiabt.NokiaBTController
MIDlet-Description: Bluetooth TV remote, mouse and gamepad for Android
MicroEdition-Configuration: CLDC-1.1
MicroEdition-Profile: MIDP-2.0
MIDlet-Permissions: javax.microedition.io.Connector.bluetooth.client
MIDlet-Jar-URL: NokiaBTController-1.0.0.jar
MIDlet-Jar-Size: $JAR_SIZE
EOF

echo
echo "Build complete:"
ls -lh "$JAR_OUT" "$JAD_OUT"
echo "JAR SHA-256:"
sha256sum "$JAR_OUT"
echo "Manifest:"
unzip -p "$JAR_OUT" META-INF/MANIFEST.MF
