#!/bin/bash

. "$(dirname $0)/defines.sh";

cmake_root=$SOURCE_ROOT/external/zstd/build/cmake/out;

# Clean built products
[[ -d "$cmake_root" ]] && rm -rfv "$cmake_root";
mkdir -pv "$cmake_root";

cd "$cmake_root"

echo "→ Running cmake...";
cmake .. -G Xcode -DCMAKE_TOOLCHAIN_FILE="$CMAKE_TOOLCHAIN_FILE" -DPLATFORM="$PLATFORM" \
	-DDEPLOYMENT_TARGET="$DEPLOYMENT_TARGET" $CMAKE_COMPAT_FLAGS

echo "→ Building...";
cmake --build . --config Debug --target libzstd_static --parallel 8

echo "→ Copying output...";
find . -name "*.a" -exec cp -av {} "$FULL_OUTPUT" \;

echo "✅ zstd build completed successfully!"
