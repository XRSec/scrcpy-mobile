#!/bin/bash

. "$(dirname $0)/defines.sh";

cmake_root=$SOURCE_ROOT/android-tools/out;

# Init update android tools submodules FIRST (before creating output dir)
# Force checkout to discard local changes
echo "→ Reset android tools and submodules";
(cd "$SOURCE_ROOT/android-tools" && git checkout -f . && git clean -fd -e out)

# Force reset all submodules
for dep_repo in "$SOURCE_ROOT"/android-tools/vendor/*; do
  [[ -d "$dep_repo" ]] || continue;
  echo "→ Force reset $dep_repo";
  (cd "$dep_repo" && git reset --hard HEAD && git clean -fd && git am --abort 2>/dev/null || true)
done

# Now update submodules
echo "→ Updating submodules...";
(cd "$SOURCE_ROOT/android-tools" && git submodule update --init --recursive --force)

# Clean and create build directory AFTER git operations
[[ -d "$cmake_root" ]] && rm -rfv "$cmake_root";
mkdir -pv "$cmake_root";

cd "$cmake_root"

echo "→ Running cmake...";
# Point OpenSSL to BoringSSL and exclude Homebrew's OpenSSL from include paths
BORINGSSL_DIR="$SOURCE_ROOT/android-tools/vendor/boringssl"

cmake -DCMAKE_OSX_SYSROOT="$SDK_NAME" -DCMAKE_OSX_ARCHITECTURES="$ARCH_NAME" \
	-DCMAKE_OSX_DEPLOYMENT_TARGET="$DEPLOYMENT_TARGET" -DCMAKE_BUILD_TYPE=Debug \
	-DOPENSSL_ROOT_DIR="$BORINGSSL_DIR" \
	-DOPENSSL_INCLUDE_DIR="$BORINGSSL_DIR/include" \
	-DCMAKE_DISABLE_FIND_PACKAGE_OpenSSL=ON \
	-DANDROID_TOOLS_USE_BUNDLED_FMT=ON \
	-DANDROID_TOOLS_USE_BUNDLED_LIBUSB=ON \
	$CMAKE_COMPAT_FLAGS ..

# Patch generated Makefiles to prioritize BoringSSL includes
# Prepend BoringSSL include to CXX_INCLUDES so it's searched first for openssl headers
echo "→ Patching include paths to prioritize BoringSSL..."
find "$cmake_root" -name "flags.make" -exec sed -i '' \
	"s|^CXX_INCLUDES = |CXX_INCLUDES = -I$BORINGSSL_DIR/include |g" {} \;

# Hack to fix build files, these actions must execute after cmake, otherwise may cause file conflict
# copy sys/user.h for libbase
echo "→ Copying porting files...";
cp -av "$PORTING_ROOT/adb/include/sys" "$SOURCE_ROOT/android-tools/vendor/libbase/include/"
ln -sfv "$SOURCE_ROOT/android-tools/vendor/core/diagnose_usb/include/diagnose_usb.h" \
  "$SOURCE_ROOT/android-tools/vendor/core/include"

# Hack source codes
cp -av "$PORTING_ROOT/adb/"*.cpp "$SOURCE_ROOT/android-tools/vendor/adb/"
cp -av "$PORTING_ROOT/adb/client/"* "$SOURCE_ROOT/android-tools/vendor/adb/client/"
cp -av "$PORTING_ROOT/adb/fdevent/"* "$SOURCE_ROOT/android-tools/vendor/adb/fdevent/"
cp -av "$PORTING_ROOT/adb/crypto/include/adb/crypto/"* "$SOURCE_ROOT/android-tools/vendor/adb/crypto/include/adb/crypto/"

# Remove abort
echo "→ Patching libbase logging.cpp...";
grep -v "abort();" "$SOURCE_ROOT/android-tools/vendor/libbase/logging.cpp" > "$SOURCE_ROOT/android-tools/vendor/libbase/logging.cpp.tmp"
mv -fv "$SOURCE_ROOT/android-tools/vendor/libbase/logging.cpp.tmp" \
  "$SOURCE_ROOT/android-tools/vendor/libbase/logging.cpp"

echo "→ Building...";
make -j16 libadb crypto decrepit libcutils libzip libdiagnoseusb libbase \
	libadb_crypto_defaults libcrypto libadb_tls_connection_defaults

echo "→ Copying output...";
find . -name "*.a" -exec cp -av {} "$FULL_OUTPUT" \;

echo "✅ Build completed successfully!"
