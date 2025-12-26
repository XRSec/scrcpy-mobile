# Exit immediately on error, treat unset variables as error, and fail on pipe errors
set -euo pipefail

# Common Supports:
# - TARGET=lz4/iphoneos/arm64
# - OUTPUT=...

[[ -z "${TARGET:-}" ]] && echo "** ❌ ERROR: TARGET is REQUIRED." && exit 1;
[[ -z "${OUTPUT:-}" ]] && echo "** ❌ ERROR: OUTPUT is REQUIRED." && exit 1;

LIB_NAME=$(echo "$TARGET" | cut -d/ -f1);
SDK_NAME=$(echo "$TARGET" | cut -d/ -f2);
ARCH_NAME=$(echo "$TARGET" | cut -d/ -f3);

# Src root
SOURCE_ROOT=$(cd "$(dirname $0)/../.." && pwd);

# Porting root
PORTING_ROOT=$SOURCE_ROOT/porting;

# Prepare output path
FULL_OUTPUT=$(cd $OUTPUT && pwd)/$SDK_NAME/$ARCH_NAME;
[[ ! -d $FULL_OUTPUT ]] && mkdir -p $FULL_OUTPUT;

# For iphone, change to platform
PLATFORM=$([[ $SDK_NAME == iphoneos ]] && echo "OS64" || echo "SIMULATOR64");
[[ $SDK_NAME == iphonesimulator ]] && [[ $ARCH_NAME == arm64 ]] && PLATFORM=SIMULATORARM64;

# Setup iphone deploy target
DEPLOYMENT_TARGET=12.0;

# Setup toolchains
if [[ $SDK_NAME == *iphone* ]]; then
	CMAKE_TOOLCHAIN_FILE=$SOURCE_ROOT/ios-cmake/ios.toolchain.cmake;
fi

# CMake compatibility flag for older CMakeLists.txt files
CMAKE_COMPAT_FLAGS="-DCMAKE_POLICY_VERSION_MINIMUM=3.5"

# Print summary
echo " - Lib Name: $LIB_NAME";
echo " - SDK Name: $SDK_NAME";
echo " - Arch Name: $ARCH_NAME";
echo " - CMake Toolchain: $CMAKE_TOOLCHAIN_FILE";
echo " - CMake PLATFORM: $PLATFORM";
