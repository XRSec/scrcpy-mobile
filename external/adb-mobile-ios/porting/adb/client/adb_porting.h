/**
 * @brief this file contains the hijack functions for porting adb
 */

#ifndef ADB_PORTING
#define ADB_PORTING 1

#include <cstdio>
#include <cstdlib>

#define __android_log_print(...)   porting_log_print(__VA_ARGS__)
#define __android_log_buf_print(...)   porting_log_buf_print(__VA_ARGS__)

#ifdef __cplusplus
extern "C" {
#endif

// enum AdbTrace {
//  ADB = 0, /* 0x001 */
//  SOCKETS,
//  PACKETS,
//  TRANSPORT,
//  RWX, /* 0x010 */
//  USB,
//  SYNC,
//  SYSDEPS,
//  JDWP, /* 0x100 */
//  SERVICES,
//  AUTH,
//  FDEVENT,
//  SHELL,
//  INCREMENTAL,
// }


// capture printf output as adb command result
int capture_printf(char **output_buffer, size_t *output_size, const char *format, ...);

// new method to call adb_commandline for public
int adb_commandline_porting(char **output_buffer, size_t *output_buffer_size, int argc, const char** argv);

// for init adb trace
void adb_trace_init_porting(char**);

// for enable adb trace
void adb_trace_enable_porting(int trace_tag);

#ifdef __cplusplus
}
#endif

#endif
