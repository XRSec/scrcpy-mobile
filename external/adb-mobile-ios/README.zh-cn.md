# adb-mobile

Android Debug Bridge (adb) 移植到移动端的版本，目标是支持 iOS App 和 Android App 通过集成本项目生成的库可以连接开发者模式 (TCP/IP Mode) 的 Android 设备，并执行支持的 ADB 命令。

当前仅支持 iOS 平台，Android 版本正在开发中。

[英文文档](README.md)

## 编译库

项目已经适配好了编译参数和脚本，仅需通过简单几步即可生成 libadb：

```sh
# 1. 检出依赖的 submodules
git submodule update --init --recursive

# 2. 编译
make
```

生成的 libadb 产物可以在项目目录下的 output 文件夹里找到，iOS 为 libadb.a。

## 集成到工程

iOS 工程：

```ini
# 设置头文件路径 - Header Search Paths
HEADER_SEARCH_PATHS = $(SRCROOT)/../../output/include

# 设置库搜索路径 - Library Search Paths
LIBRARY_SEARCH_PATHS = $(SRCROOT)/../../output

# 设置连接器参数 - Other Linker Flags
OTHER_LDFLAGS = -ladb -lc++ -lz
```

## 使用说明

iOS App:

```objc
#import "adb_puiblic.h"

// 开启调试信息输出，有助于排查问题
adb_enable_trace();

// 设置 ADB 的监听端口，默认为 5037
adb_set_server_port("15037");

// 设置 ADB 私钥和数据存放目录，一般为 App 的 Documents 目录
NSArray <NSString *> *documentPaths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
const char *document_home = documentPaths.lastObject.UTF8String;
adb_set_home(document_home);

// 连接 Android 设备
const char *argv[] = { "connect", "x.x.x." };
const char *output = null;
int ret = adb_commandline_porting(2, argv, &output);

// 执行 ADB 命令
const char *argv[] = { "shell", "ip", "route" };
const char *output = null;
int ret = adb_commandline_porting(2, argv, &output);
```

ADB 状态回调：

```objc
// 实现 adb_connect_status_updated 回调方法，可以获取 adb 连接状态
void adb_connect_status_updated(const char *serial, const char *status) {
    NSLog(@"ADB Status: %s, %s", serial, status);
}
```

也可以通过 example/adb-demo 工程查看具体使用方法。