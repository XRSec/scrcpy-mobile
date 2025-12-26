cmake_minimum_required(VERSION 3.10)

project(adb-porting)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

set(ADB_CLIENT_DIR ${CMAKE_CURRENT_SOURCE_DIR}/adb/client)
set(ADB_VENDOR_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../android-tools/vendor/adb)

file(GLOB_RECURSE ADB_CLIENT_SOURCES ${ADB_CLIENT_DIR}/*.cpp ${ADB_CLIENT_DIR}/*.h)
file(GLOB_RECURSE ADB_VENDOR_SOURCES ${ADB_VENDOR_DIR}/*.cpp ${ADB_VENDOR_DIR}/*.h)

set(ADB_PORTING_SOURCES ${ADB_CLIENT_SOURCES} ${ADB_VENDOR_SOURCES})
add_library(adb-porting ${ADB_PORTING_SOURCES})

target_compile_definitions(adb-porting PUBLIC ADB_HOST)

target_include_directories(adb-porting PUBLIC
        ${ADB_CLIENT_DIR}
        ${ADB_VENDOR_DIR}
        ${ADB_VENDOR_DIR}/client
        ${CMAKE_CURRENT_SOURCE_DIR}/../android-tools/vendor/libbase/include
        ${CMAKE_CURRENT_SOURCE_DIR}/../android-tools/vendor/core/include
        ${CMAKE_CURRENT_SOURCE_DIR}/../external/protobuf/src
        )
