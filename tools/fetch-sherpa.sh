#!/bin/sh
# Fetches the official sherpa-onnx Android AAR (not on Maven Central) and lays it
# out as a local Maven artifact the build resolves from third_party/m2.
# Run once after cloning: sh tools/fetch-sherpa.sh
set -e
VER="1.13.3"
DIR="$(dirname "$0")/../third_party/m2/com/k2fsa/sherpa/onnx/sherpa-onnx-android/$VER"
AAR="$DIR/sherpa-onnx-android-$VER.aar"
[ -f "$AAR" ] && { echo "sherpa-onnx $VER already present"; exit 0; }
mkdir -p "$DIR"
echo "Downloading sherpa-onnx $VER AAR (~54 MB)..."
curl -L -o "$AAR" "https://github.com/k2-fsa/sherpa-onnx/releases/download/v$VER/sherpa-onnx-$VER.aar"
cat > "$DIR/sherpa-onnx-android-$VER.pom" <<POM
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.k2fsa.sherpa.onnx</groupId>
  <artifactId>sherpa-onnx-android</artifactId>
  <version>$VER</version>
  <packaging>aar</packaging>
</project>
POM
echo "Done: $AAR"
