/**
 * adb.h used for export adb functions
 */

#include <stdio.h>
#include <stdlib.h>

enum AdbTrace {
    ADB = 0, /* 0x001 */
    SOCKETS,
    PACKETS,
    TRANSPORT,
    RWX, /* 0x010 */
    USB,
    SYNC,
    SYSDEPS,
    JDWP, /* 0x100 */
    SERVICES,
    AUTH,
    FDEVENT,
    SHELL,
    INCREMENTAL,
};

void adb_trace_init_porting(char**);
void adb_trace_enable_porting(enum AdbTrace trace_tag);

// execute adb_commoandline
int adb_commandline_porting(char **output_buffer, size_t *output_buffer_size, int argc, const char** argv);

// enable adb verbose trace
static inline void adb_enable_trace(void) {
	setenv("ADB_TRACE", "all", 1);
	const char *argv[] = { "adb" };
	adb_trace_init_porting((char **)argv);
    adb_trace_enable_porting(ADB);
}

// set home directory
static inline void adb_set_home(const char *home_dir) {
	setenv("HOME", home_dir, 1);
}

// set adb daemon port
static inline void adb_set_server_port(const char *port) {
    setenv("ANDROID_ADB_SERVER_PORT", port, 1);
}

// required function body
void adb_connect_status_updated(const char *serial, const char *status);
