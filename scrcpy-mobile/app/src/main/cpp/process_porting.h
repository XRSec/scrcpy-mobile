#ifndef PROCESS_PORTING_H
#define PROCESS_PORTING_H

#include <stdint.h>
#include <stdbool.h>
#include <sys/types.h>

#ifdef __cplusplus
extern "C" {
#endif

// 进程 ID 类型
typedef int sc_pid;

// 管道类型
typedef int sc_pipe;

// 退出码类型
typedef int sc_exit_code;

// 进程标志
enum sc_process_flags {
    SC_PROCESS_NO_STDOUT = 1 << 0,
    SC_PROCESS_NO_STDERR = 1 << 1,
    SC_PROCESS_NO_STDIN = 1 << 2,
};

// 中断结构（用于取消操作）
struct sc_intr {
    bool interrupted;
};

/**
 * 执行进程（替换 scrcpy 的进程创建）
 * 
 * @param argv 命令参数数组（argv[0] 是 "adb"）
 * @param pid 输出参数，返回进程 ID
 * @param flags 进程标志
 * @param pin 输入管道（可选）
 * @param pout 输出管道（可选）
 * @param perr 错误管道（可选）
 * @return 0 成功，-1 失败
 */
int sc_process_execute_p(const char *const argv[], sc_pid *pid, unsigned flags,
                        sc_pipe *pin, sc_pipe *pout, sc_pipe *perr);

/**
 * 等待进程完成
 * 
 * @param pid 进程 ID
 * @param close 是否关闭进程
 * @return 退出码（0 成功，非 0 失败）
 */
sc_exit_code sc_process_wait(sc_pid pid, bool close);

/**
 * 终止进程
 * 
 * @param pid 进程 ID
 * @return true 成功，false 失败
 */
bool sc_process_terminate(sc_pid pid);

/**
 * 关闭管道
 * 
 * @param pipe 管道描述符
 */
void sc_pipe_close(sc_pipe pipe);

/**
 * 从管道读取数据（支持中断）
 * 
 * @param intr 中断结构
 * @param pid 进程 ID
 * @param pipe 管道描述符
 * @param data 数据缓冲区
 * @param len 缓冲区大小
 * @return 读取的字节数，-1 表示失败
 */
ssize_t sc_pipe_read_all_intr(struct sc_intr *intr, sc_pid pid, sc_pipe pipe,
                              char *data, size_t len);

/**
 * 从管道读取数据
 * 
 * @param pipe 管道描述符
 * @param data 数据缓冲区
 * @param len 缓冲区大小
 * @return 读取的字节数，-1 表示失败
 */
ssize_t sc_pipe_read(sc_pipe pipe, char *data, size_t len);

/**
 * 向管道写入数据
 * 
 * @param pipe 管道描述符
 * @param data 数据
 * @param len 数据长度
 * @return 写入的字节数，-1 表示失败
 */
ssize_t sc_pipe_write(sc_pipe pipe, const char *data, size_t len);

#ifdef __cplusplus
}
#endif

#endif // PROCESS_PORTING_H
