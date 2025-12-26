#!/bin/bash

. "$(dirname $0)/defines.sh";

cmake_root=$SOURCE_ROOT/external/protobuf/out;

# Clean built products
[[ -d "$cmake_root" ]] && rm -rfv "$cmake_root";
mkdir -pv "$cmake_root";

cd "$cmake_root"

which protoc || {
	echo "** ❌ ERROR: protoc not installed";
	exit 1;
}
protoc_version=$(protoc --version | cut -d' ' -f2);
echo "→ Using protobuf v$protoc_version";

# Switch to version
echo "→ Checking out protobuf v$protoc_version...";
(cd "$SOURCE_ROOT/external/protobuf" && git clean -f && git checkout "v$protoc_version" && git submodule update --init --recursive)

# Fix absl version
echo "→ Fixing abseil-cpp version...";
(cd "$SOURCE_ROOT/external/protobuf/third_party/abseil-cpp" && git checkout 20240722.0)

# Copy CMakeLists.txt to source folder
echo "→ Copying CMakeLists.txt...";
cp "$SOURCE_ROOT/porting/cmake/CMakeLists.protobuf.txt" "$SOURCE_ROOT/external/protobuf/CMakeLists.txt"

echo "→ Running cmake...";
cmake .. -G Xcode -DCMAKE_TOOLCHAIN_FILE="$CMAKE_TOOLCHAIN_FILE" -DPLATFORM="$PLATFORM" \
	-DDEPLOYMENT_TARGET="$DEPLOYMENT_TARGET" $CMAKE_COMPAT_FLAGS

# Hack files
echo "→ Patching source files...";
cp -av "$PORTING_ROOT/protobuf/src/google/protobuf/map_probe_benchmark.cc" \
  "$SOURCE_ROOT/external/protobuf/src/google/protobuf/map_probe_benchmark.cc"

echo "→ Building...";
cmake --build . --target protobuf --config Debug --parallel 8

echo "→ Copying output...";
find . -name "*.a" -exec cp -av {} "$FULL_OUTPUT" \;

echo "✅ protobuf build completed successfully!"
