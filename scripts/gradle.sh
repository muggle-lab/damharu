#!/bin/sh
# Keep the checked-in Gradle wrapper standard. This helper also works from Xcode.
set -eu
cd "$(dirname "$0")/.."
if [ -z "${JAVA_HOME:-}" ]; then
    if [ -d "/Applications/Android Studio.app/Contents/jbr/Contents/Home" ]; then
        export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
    else
        export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
    fi
fi
exec ./gradlew "$@"
