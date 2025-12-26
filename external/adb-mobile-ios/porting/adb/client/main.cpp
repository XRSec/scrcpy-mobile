// → adding adb_porting.cpp to build workflow

#include "adb_porting.h"
#include "adb_porting.cpp"

#include "sysdeps.h"

// Fix shutdown defined in sysdeps.h
#undef shutdown
#undef close

// include additional source codes
#include "sysdeps/env.cpp"

// include all file from CMakeList.txt

/** hijack launch_server **/
#define launch_server(...)    launch_server_unused(__VA_ARGS__)
/** hijack this func to handle adb connect status **/
#define update_transport_status(...)		update_transport_status_real(__VA_ARGS__)
#include "adb.cpp"
#undef launch_server
#undef update_transport_status

#include "adb_io.cpp"
#include "adb_utils.cpp"
#include "adb_listeners.cpp"
#include "shell_service_protocol.cpp"
#include "adb_trace.cpp"
#include "adb_unique_fd.cpp"
#include "types.cpp"
#include "fdevent/fdevent.cpp"
#include "fdevent/fdevent_poll.cpp"
#include "fdevent/fdevent_epoll.cpp"

#include "sockets.cpp"
#include "socket_spec.cpp"
#include "transport.cpp"
#include "transport_fd.cpp"
#include "transport_usb.cpp"
#include "transport_local.cpp"

static void setup_daemon_logging() {
    const std::string log_file_path(GetLogFilePath());
    int fd = unix_open(log_file_path, O_WRONLY | O_CREAT | O_APPEND, 0640);
    if (fd == -1) {
        PLOG(FATAL) << "cannot open " << log_file_path;
    }
    if (dup2(fd, STDOUT_FILENO) == -1) {
        PLOG(FATAL) << "cannot redirect stdout";
    }
    if (dup2(fd, STDERR_FILENO) == -1) {
        PLOG(FATAL) << "cannot redirect stderr";
    }
    unix_close(fd);

    fprintf(stderr, "--- adb starting (pid %d) ---\n", getpid());
    LOG(INFO) << adb_version();
}

void adb_server_cleanup() {
    // Upon exit, we want to clean up in the following order:
    //   1. close_smartsockets, so that we don't get any new clients
    //   2. kick_all_transports, to avoid writing only part of a packet to a transport.
    //   3. usb_cleanup, to tear down the USB stack.
    close_smartsockets();
    kick_all_transports();
    // Porting fix: disable usb
    // usb_cleanup();
}

static void intentionally_leak() {
    void* p = ::operator new(1);
    // The analyzer is upset about this leaking. NOLINTNEXTLINE
    LOG(INFO) << "leaking pointer " << p;
}

// one_device: if null, server owns all devices, else server owns only
//     device where atransport::MatchesTarget(one_device) is true.
int adb_server_main(int is_daemon, const std::string& socket_spec, const char* one_device,
                    int ack_reply_fd) {
#if defined(_WIN32)
    // adb start-server starts us up with stdout and stderr hooked up to
    // anonymous pipes. When the C Runtime sees this, it makes stderr and
    // stdout buffered, but to improve the chance that error output is seen,
    // unbuffer stdout and stderr just like if we were run at the console.
    // This also keeps stderr unbuffered when it is redirected to adb.log.
    if (is_daemon) {
        if (setvbuf(stdout, nullptr, _IONBF, 0) == -1) {
            PLOG(FATAL) << "cannot make stdout unbuffered";
        }
        if (setvbuf(stderr, nullptr, _IONBF, 0) == -1) {
            PLOG(FATAL) << "cannot make stderr unbuffered";
        }
    }

    // TODO: On Ctrl-C, consider trying to kill a starting up adb server (if we're in
    // launch_server) by calling GenerateConsoleCtrlEvent().

    // On Windows, SIGBREAK is when Ctrl-Break is pressed or the console window is closed. It should
    // act like Ctrl-C.
    signal(SIGBREAK, [](int) { raise(SIGINT); });
#endif
    signal(SIGINT, [](int) { fdevent_run_on_looper([]() { exit(0); }); });

    if (one_device) {
        transport_set_one_device(one_device);
    }

    const char* reject_kill_server = getenv("ADB_REJECT_KILL_SERVER");
    if (reject_kill_server && strcmp(reject_kill_server, "1") == 0) {
        adb_set_reject_kill_server(true);
    }

    const char* leak = getenv("ADB_LEAK");
    if (leak && strcmp(leak, "1") == 0) {
        intentionally_leak();
    }

    if (is_daemon) {
        close_stdin();
        setup_daemon_logging();
    }

    atexit(adb_server_cleanup);

    init_reconnect_handler();

    // Porting fix: disable usb
    // if (!getenv("ADB_USB") || strcmp(getenv("ADB_USB"), "0") != 0) {
    //     if (is_libusb_enabled()) {
    //         libusb::usb_init();
    //     } else {
    //         usb_init();
    //     }
    // } else {
        adb_notify_device_scan_complete();
    // }

    if (!getenv("ADB_EMU") || strcmp(getenv("ADB_EMU"), "0") != 0) {
        local_init(android::base::StringPrintf("tcp:%d", DEFAULT_ADB_LOCAL_TRANSPORT_PORT));
    }

    std::string error;

    auto start = std::chrono::steady_clock::now();

    // If we told a previous adb server to quit because of version mismatch, we can get to this
    // point before it's finished exiting. Retry for a while to give it some time. Don't actually
    // accept any connections until adb_wait_for_device_initialization finishes below.
    while (install_listener(socket_spec, kSmartSocketConnectTo, nullptr, INSTALL_LISTENER_DISABLED,
                            nullptr, &error) != INSTALL_STATUS_OK) {
        if (std::chrono::steady_clock::now() - start > 0.5s) {
            LOG(FATAL) << "could not install *smartsocket* listener: " << error;
        }

        std::this_thread::sleep_for(100ms);
    }

    adb_auth_init();

    if (is_daemon) {
#if !defined(_WIN32)
        // Start a new session for the daemon. Do this here instead of after the fork so
        // that a ctrl-c between the "starting server" and "done starting server" messages
        // gets a chance to terminate the server.
        // setsid will fail with EPERM if it's already been a lead process of new session.
        // Ignore such error.
        if (setsid() == -1 && errno != EPERM) {
            PLOG(FATAL) << "setsid() failed";
        }
#endif
    }

    // Wait for the USB scan to complete before notifying the parent that we're up.
    // We need to perform this in a thread, because we would otherwise block the event loop.
    std::thread notify_thread([ack_reply_fd]() {
        adb_wait_for_device_initialization();

        if (ack_reply_fd >= 0) {
            // Any error output written to stderr now goes to adb.log. We could
            // keep around a copy of the stderr fd and use that to write any errors
            // encountered by the following code, but that is probably overkill.
#if defined(_WIN32)
            const HANDLE ack_reply_handle = cast_int_to_handle(ack_reply_fd);
            const CHAR ack[] = "OK\n";
            const DWORD bytes_to_write = arraysize(ack) - 1;
            DWORD written = 0;
            if (!WriteFile(ack_reply_handle, ack, bytes_to_write, &written, NULL)) {
                LOG(FATAL) << "cannot write ACK to handle " << ack_reply_handle
                           << android::base::SystemErrorCodeToString(GetLastError());
            }
            if (written != bytes_to_write) {
                LOG(FATAL) << "cannot write " << bytes_to_write << " bytes of ACK: only wrote "
                           << written << " bytes";
            }
            CloseHandle(ack_reply_handle);
#else
            // TODO(danalbert): Can't use SendOkay because we're sending "OK\n", not
            // "OKAY".
            if (!android::base::WriteStringToFd("OK\n", ack_reply_fd)) {
                PLOG(FATAL) << "error writing ACK to fd " << ack_reply_fd;
            }
            unix_close(ack_reply_fd);
#endif
        }
        // We don't accept() client connections until this point: this way, clients
        // can't see wonky state early in startup even if they're connecting directly
        // to the server instead of going through the adb program.
        fdevent_run_on_looper([] { enable_server_sockets(); });
    });
    notify_thread.detach();

#if defined(__linux__)
    // Write our location to .android/adb.$PORT, so that older clients can exec us.
    std::string path;
    if (!android::base::Readlink("/proc/self/exe", &path)) {
        PLOG(ERROR) << "failed to readlink /proc/self/exe";
    }

    std::optional<std::string> server_executable_path = adb_get_server_executable_path();
    if (server_executable_path) {
      if (!android::base::WriteStringToFile(path, *server_executable_path)) {
          PLOG(ERROR) << "failed to write server path to " << path;
      }
    }
#endif

    D("Event loop starting");
    fdevent_loop();
    return 0;
}


/**
 * Rewrite launch_server to launch in thread but not by new process
 */
void fdevent_reset_porting(void) {
    fdevent_reset();
}

void clear_listener_list(void) {
    listener_list.clear();
}

void clear_transport_list(void) {
    std::lock_guard<std::recursive_mutex> lock(transport_lock);
    for (auto t : transport_list) {
        t->Kick();
    }
    transport_list.clear();
    pending_list.clear();
}

void launch_server_thread(int pipe_write, const char *thread_socket_spec) {
    // Launch server with pipe
    fcntl(pipe_write, F_SETFD, 0);
    printf("[adb_porting] launch_server_main: %d\n", pipe_write);

    // Start main server
    adb_server_main(false, thread_socket_spec, nullptr, pipe_write);
}

int launch_server(const std::string& socket_spec, const char* one_device) {
    // Before launch server, we need to clear all the transport list
    static bool not_first = false;
    if (not_first == true) {
        printf("[adb_porting] 🔄 terminating loop\n");
        fdevent_get_ambient()->TerminateLoop();

        // Wait for the fdevent loop to actually exit before destroying the context
        // This fixes the race condition where the loop thread was still accessing
        // installed_fdevents_ while we were destroying it
        printf("[adb_porting] ⏳ waiting for loop to exit\n");
        fdevent_wait_for_loop_exit();
        printf("[adb_porting] ✅ loop exited\n");

        printf("[adb_porting] 🧹 cleanup sockets and threads\n");
        adb_server_cleanup();
        clear_transport_list();
        fdevent_reset_porting();
    }
    not_first = true;

    static char *thread_socket_spec;
    static int pipe_server[2];

	printf("[adb_porting] socket %s\n", socket_spec.c_str());
    thread_socket_spec = strdup(socket_spec.c_str());

    // Reset last used pipe
    pipe(pipe_server);

    // set up a pipe so the child can tell us when it is ready.
    if (pipe_server[0] < 0 || pipe_server[1] < 0) {
        fprintf(stderr, "pipe failed in launch_server, errno: %d\n", errno);
        return -1;
    }

    std::string path = android::base::GetExecutablePath();

    // launch server in new thread
    std::thread adb_thread([]() {
        launch_server_thread(pipe_server[1], thread_socket_spec);
    });
    adb_thread.detach();

    // parent side
    printf("[adb_porting] launch_server pipe_write: %d\n", pipe_server[0]);
    printf("[adb_porting] launch_server pipe_read: %d\n", pipe_server[1]);

    // wait for the "OK\n" message from the child
    char temp[3] = {};
    int ret = adb_read(pipe_server[0], temp, 3);
    int saved_errno = errno;
    if (ret < 0) {
        fprintf(stderr, "[adb_porting] could not read ok from ADB Server, errno = %d\n", saved_errno);
        return -1;
    }
    if (ret != 3 || temp[0] != 'O' || temp[1] != 'K' || temp[2] != '\n') {
        ReportServerStartupFailure(0);
        return -1;
    }

    return 0;
}

extern "C" {

__attribute__ ((weak))
void adb_connect_status_updated(const char *serial, const char *status) {
    printf("%s", __FUNCTION__);
}

int adb_auth_key_generate(const char* filename) {
    return adb_auth_keygen(filename);
}

}

void update_transport_status() {
    update_transport_status_real();

    // Check all the tcp transports connect status
    iterate_transports([](const atransport *t) {
        printf("adb connect status changed: %s -> %s\n", t->serial.c_str(), to_string(t->GetConnectionState()).c_str());
        adb_connect_status_updated(t->serial.c_str(), to_string(t->GetConnectionState()).c_str());
        return true;
    });
}
