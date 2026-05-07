#!/usr/bin/env bash
#
# Build a signed release APK for kzLauncher and stage it in dist/.
#
# On first run this generates a self-signed keystore at keystore/release.jks
# and saves the credentials to keystore.properties (both gitignored). Reusing
# the same keystore on subsequent runs keeps the app's signature stable so
# upgrades install over the previous version.

set -euo pipefail

ROOT_DIR=$(cd "$(dirname "$0")/.." && pwd)
cd "$ROOT_DIR"

KEYSTORE_DIR="$ROOT_DIR/keystore"
KEYSTORE_FILE="$KEYSTORE_DIR/release.jks"
KEYSTORE_PROPS="$ROOT_DIR/keystore.properties"
DIST_DIR="$ROOT_DIR/dist"
APK_BUILT="$ROOT_DIR/app/build/outputs/apk/release/app-release.apk"

bootstrap_keystore() {
    echo "==> No keystore found. Generating a self-signed release keystore."
    mkdir -p "$KEYSTORE_DIR"

    local store_password
    store_password=$(head -c 24 /dev/urandom | base64 | tr -d '+/=' | head -c 32)

    keytool -genkeypair \
        -keystore "$KEYSTORE_FILE" \
        -alias kzlauncher \
        -keyalg RSA \
        -keysize 2048 \
        -validity 10000 \
        -storepass "$store_password" \
        -keypass "$store_password" \
        -dname "CN=kzLauncher, OU=, O=, L=, S=, C=US"

    cat > "$KEYSTORE_PROPS" <<EOF
storeFile=keystore/release.jks
storePassword=$store_password
keyAlias=kzlauncher
keyPassword=$store_password
EOF
    chmod 600 "$KEYSTORE_PROPS" "$KEYSTORE_FILE"

    echo "    keystore: $KEYSTORE_FILE"
    echo "    creds:    $KEYSTORE_PROPS"
    echo "    BACK THESE UP. Losing them means future builds get a new"
    echo "    signature and won't upgrade existing installs."
    echo
}

build_release() {
    echo "==> Building release APK"
    "$ROOT_DIR/gradlew" --console=plain :app:assembleRelease

    if [[ ! -f "$APK_BUILT" ]]; then
        echo "ERROR: expected APK not produced at $APK_BUILT" >&2
        exit 1
    fi
}

stage_artifact() {
    local version_name version_code staged_name
    version_name=$(grep -E '^\s*versionName' "$ROOT_DIR/app/build.gradle" \
        | head -n1 | sed -E 's/.*"([^"]+)".*/\1/')
    version_code=$(grep -E '^\s*versionCode' "$ROOT_DIR/app/build.gradle" \
        | head -n1 | awk '{print $2}')
    staged_name="kzlauncher-${version_name}-${version_code}.apk"

    mkdir -p "$DIST_DIR"
    cp "$APK_BUILT" "$DIST_DIR/$staged_name"

    echo
    echo "==> Release APK ready"
    echo "    file:   $DIST_DIR/$staged_name"
    echo "    size:   $(du -h "$DIST_DIR/$staged_name" | cut -f1)"
    echo "    sha256: $(sha256sum "$DIST_DIR/$staged_name" | awk '{print $1}')"
}

main() {
    if [[ ! -f "$KEYSTORE_FILE" ]]; then
        bootstrap_keystore
    fi
    if [[ ! -f "$KEYSTORE_PROPS" ]]; then
        echo "ERROR: $KEYSTORE_FILE exists but $KEYSTORE_PROPS is missing." >&2
        echo "Either restore the props file or delete the keystore to regenerate." >&2
        exit 1
    fi
    build_release
    stage_artifact
}

main "$@"
