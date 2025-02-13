#!/bin/bash

# Check if the user provided a path
if [ -z "$1" ]; then
    echo "Usage: $0 <path_to_classes>"
    exit 1
fi

TARGET_DIR="$1"
JAR_NAME="output.jar"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

rm -rf $TARGET_DIR/sources

jar cf "$JAR_NAME" -C "$TARGET_DIR" .

mkdir -p $TARGET_DIR/sources
java -jar $SCRIPT_DIR/fernflower.jar $JAR_NAME $TARGET_DIR/sources
mv $TARGET_DIR/sources/$JAR_NAME $TARGET_DIR/sources/output-sources.jar