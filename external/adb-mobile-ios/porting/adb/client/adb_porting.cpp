#include <cstdio>
#include <cstdarg>
#include <cstdlib>
#include "commandline.h"
#include "adb_porting.h"
#include "adb_trace.h"

int capture_printf(char **output_buffer, size_t *output_size, const char *format, ...) {
    if (output_buffer == nullptr || output_size == nullptr || format == nullptr) {
        return -1;
    }

    va_list args;
    va_start(args, format);

    // Calculate the required buffer size.
    va_list args_copy;
    va_copy(args_copy, args);
    const int required_size = vsnprintf(nullptr, 0, format, args_copy) + 1;  // +1 for null terminator
    va_end(args_copy);

    // If the buffer is empty or not large enough, expand the buffer.
    size_t current_used_size = *output_size;  // Used output buffer size
    *output_buffer = static_cast<char*>(realloc(*output_buffer, current_used_size + required_size));
    if (*output_buffer == nullptr) {
        perror("realloc");
        va_end(args);
        return -1;
    }

    // Append new content to the end of the buffer.
    vsnprintf(*output_buffer + current_used_size, required_size, format, args);
    *output_size = current_used_size + required_size - 1;  // Update the used buffer size (excluding null terminator)

    va_end(args);

    // return size of this output
    return required_size - 1;
}

int adb_commandline_porting(char **output_buffer, size_t *output_buffer_size, int argc, const char** argv) {
    return adb_commandline(output_buffer, output_buffer_size, argc, argv);
}

void adb_trace_init_porting(char** argv) {
    adb_trace_init(argv);
}

void adb_trace_enable_porting(int trace_tag) {
    adb_trace_enable((AdbTrace)trace_tag);
}
