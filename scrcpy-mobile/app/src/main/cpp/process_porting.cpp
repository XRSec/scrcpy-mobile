#include "process_porting.h"
#include <string.h>
#include <unistd.h>
#include <android/log.h>

#define TAG "ProcessPorting"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

// 声明 JNI 桥接函数
extern "C" {
    int adb_execute_command(const char* const argv[], int* pid);
    int adb_wait_process(int pid);
    int adb_read_output(int pid, char* buffer, int buffer_size);
    int adb_terminate_process(int pid);
    void adb_cleanup_process(int pid);
}

/**
 * 执行进程
 * 这是 scrcpy 调用的主要入口点
 */
int sc_process_execute_p(const char *const argv[], sc_pid *pid, unsigned flags,
                        sc_pipe *pin, sc_pipe *pout, sc_pipe *perr) {
    LOGD("========== sc_process_execute_p ==========");
    
    // 打印命令
    int i = 0;
    while (argv[i] != nullptr) {
        LOGD("argv[%d]: %s", i, argv[i]);
        i++;
    }
    
    // 检查是否是 adb 命令
    if (argv[0] == nullptr || strcmp(argv[0], "adb") != 0) {
        LOGE("Not an adb command: %s", argv[0] ? argv[0] : "(null)");
        return -1;
    }
    
    // 创建假的管道（如果需要）
    if (pout != nullptr) {
        int pipe_fd[2];
        if (pipe(pipe_fd) == 0) {
            *pout = pipe_fd[0];  // 读端
            // 写端会被忽略，因为我们通过 JNI 获取输出
            close(pipe_fd[1]);
        }
    }
    
    // 通过 JNI 执行 ADB 命令
    int result_pid = 0;
    int ret = adb_execute_command(argv, &result_pid);
    
    if (ret == 0 && result_pid > 0) {
        *pid = result_pid;
        LOGD("Process started with PID: %d", *pid);
        return 0;
    } else {
        LOGE("Failed to execute command");
        return -1;
    }
}

/**
 * 等待进程完成
 */
sc_exit_code sc_process_wait(sc_pid pid, bool close) {
    LOGD("sc_process_wait: pid=%d, close=%d", pid, close);
    
    int exit_code = adb_wait_process(pid);
    
    if (close) {
        adb_cleanup_process(pid);
    }
    
    LOGD("Process %d exited with code: %d", pid, exit_code);
    return exit_code;
}

/**
 * 终止进程
 */
bool sc_process_terminate(sc_pid pid) {
    LOGD("sc_process_terminate: pid=%d", pid);
    
    int ret = adb_terminate_process(pid);
    
    if (ret == 0) {
        adb_cleanup_process(pid);
        return true;
    }
    
    return false;
}

/**
 * 关闭管道
 */
void sc_pipe_close(sc_pipe pipe) {
    if (pipe >= 0) {
        close(pipe);
    }
}

/**
 * 从管道读取数据（支持中断）
 * 这个函数会等待进程完成，然后读取输出
 */
ssize_t sc_pipe_read_all_intr(struct sc_intr *intr, sc_pid pid, sc_pipe pipe,
                              char *data, size_t len) {
    LOGD("sc_pipe_read_all_intr: pid=%d, len=%zu", pid, len);
    
    // 检查是否被中断
    if (intr != nullptr && intr->interrupted) {
        LOGD("Operation interrupted");
        return -1;
    }
    
    // 等待进程完成
    sc_process_wait(pid, false);
    
    // 读取输出
    int bytes_read = adb_read_output(pid, data, len);
    
    LOGD("Read %d bytes from PID %d", bytes_read, pid);
    return bytes_read;
}

/**
 * 从管道读取数据
 */
ssize_t sc_pipe_read(sc_pipe pipe, char *data, size_t len) {
    if (pipe < 0) {
        return -1;
    }
    return read(pipe, data, len);
}

/**
 * 向管道写入数据
 */
ssize_t sc_pipe_write(sc_pipe pipe, const char *data, size_t len) {
    if (pipe < 0) {
        return -1;
    }
    return write(pipe, data, len);
}
