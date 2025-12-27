cmake_minimum_required(VERSION 3.18.1)

project("scrcpy_adb_bridge")

# 设置 C++ 标准
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# 添加源文件
add_library(
    scrcpy_adb_bridge
    SHARED
    adb_bridge_jni.cpp
    process_porting.cpp
)

# 查找并链接 log 库
find_library(log-lib log)

# 链接库
target_link_libraries(
    scrcpy_adb_bridge
    ${log-lib}
)

# 导出符号
set_target_properties(
    scrcpy_adb_bridge
    PROPERTIES
    LINK_FLAGS "-Wl,--export-dynamic"
)
