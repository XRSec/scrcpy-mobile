#include <jni.h>
#include <string>
#include <vector>
#include <android/log.h>

#define TAG "AdbBridgeJNI"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

// 全局 JVM 引用
static JavaVM* g_jvm = nullptr;
static jclass g_adb_bridge_class = nullptr;
static jmethodID g_execute_method = nullptr;
static jmethodID g_wait_method = nullptr;
static jmethodID g_read_method = nullptr;
static jmethodID g_terminate_method = nullptr;
static jmethodID g_cleanup_method = nullptr;

extern "C" {

/**
 * JNI 初始化
 */
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
    LOGD("JNI_OnLoad called");
    g_jvm = vm;
    
    JNIEnv* env;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        LOGE("Failed to get JNIEnv");
        return JNI_ERR;
    }
    
    // 查找 AdbBridge 类
    jclass local_class = env->FindClass("com/mobile/scrcpy/android/adb/AdbBridge");
    if (local_class == nullptr) {
        LOGE("Failed to find AdbBridge class");
        return JNI_ERR;
    }
    
    // 创建全局引用
    g_adb_bridge_class = reinterpret_cast<jclass>(env->NewGlobalRef(local_class));
    env->DeleteLocalRef(local_class);
    
    // 获取方法 ID
    g_execute_method = env->GetStaticMethodID(g_adb_bridge_class, "executeAdbCommand", "([Ljava/lang/String;)I");
    g_wait_method = env->GetStaticMethodID(g_adb_bridge_class, "waitProcess", "(I)I");
    g_read_method = env->GetStaticMethodID(g_adb_bridge_class, "readProcessOutput", "(I)Ljava/lang/String;");
    g_terminate_method = env->GetStaticMethodID(g_adb_bridge_class, "terminateProcess", "(I)Z");
    g_cleanup_method = env->GetStaticMethodID(g_adb_bridge_class, "cleanupProcess", "(I)V");
    
    if (!g_execute_method || !g_wait_method || !g_read_method || !g_terminate_method || !g_cleanup_method) {
        LOGE("Failed to get method IDs");
        return JNI_ERR;
    }
    
    LOGD("JNI initialized successfully");
    return JNI_VERSION_1_6;
}

/**
 * JNI 清理
 */
JNIEXPORT void JNICALL JNI_OnUnload(JavaVM* vm, void* reserved) {
    LOGD("JNI_OnUnload called");
    
    JNIEnv* env;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) == JNI_OK) {
        if (g_adb_bridge_class != nullptr) {
            env->DeleteGlobalRef(g_adb_bridge_class);
            g_adb_bridge_class = nullptr;
        }
    }
    
    g_jvm = nullptr;
}

/**
 * 获取 JNIEnv（支持多线程）
 */
static JNIEnv* GetJNIEnv() {
    if (g_jvm == nullptr) {
        LOGE("JVM is null");
        return nullptr;
    }
    
    JNIEnv* env = nullptr;
    int status = g_jvm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6);
    
    if (status == JNI_EDETACHED) {
        // 当前线程未附加到 JVM，需要附加
        if (g_jvm->AttachCurrentThread(&env, nullptr) != JNI_OK) {
            LOGE("Failed to attach thread to JVM");
            return nullptr;
        }
    } else if (status != JNI_OK) {
        LOGE("Failed to get JNIEnv");
        return nullptr;
    }
    
    return env;
}

/**
 * 执行 ADB 命令（供 scrcpy 调用）
 * 模拟 sc_process_execute_p 函数
 */
int adb_execute_command(const char* const argv[], int* pid) {
    LOGD("adb_execute_command called");
    
    JNIEnv* env = GetJNIEnv();
    if (env == nullptr) {
        LOGE("Failed to get JNIEnv");
        return -1;
    }
    
    // 计算参数数量（跳过 argv[0]，即 "adb"）
    int argc = 0;
    while (argv[argc + 1] != nullptr) {
        argc++;
    }
    
    LOGD("argc: %d", argc);
    
    // 创建 Java String 数组
    jclass string_class = env->FindClass("java/lang/String");
    jobjectArray args_array = env->NewObjectArray(argc, string_class, nullptr);
    
    // 填充参数（从 argv[1] 开始）
    for (int i = 0; i < argc; i++) {
        jstring arg = env->NewStringUTF(argv[i + 1]);
        env->SetObjectArrayElement(args_array, i, arg);
        env->DeleteLocalRef(arg);
        
        LOGD("arg[%d]: %s", i, argv[i + 1]);
    }
    
    // 调用 Java 方法
    jint result_pid = env->CallStaticIntMethod(g_adb_bridge_class, g_execute_method, args_array);
    
    // 清理
    env->DeleteLocalRef(args_array);
    env->DeleteLocalRef(string_class);
    
    *pid = result_pid;
    LOGD("Process started with PID: %d", *pid);
    
    return 0;
}

/**
 * 等待进程完成
 */
int adb_wait_process(int pid) {
    LOGD("adb_wait_process: %d", pid);
    
    JNIEnv* env = GetJNIEnv();
    if (env == nullptr) {
        return -1;
    }
    
    jint exit_code = env->CallStaticIntMethod(g_adb_bridge_class, g_wait_method, pid);
    
    LOGD("Process %d exited with code: %d", pid, exit_code);
    return exit_code;
}

/**
 * 读取进程输出
 */
int adb_read_output(int pid, char* buffer, int buffer_size) {
    LOGD("adb_read_output: %d", pid);
    
    JNIEnv* env = GetJNIEnv();
    if (env == nullptr) {
        return -1;
    }
    
    jstring output = (jstring)env->CallStaticObjectMethod(g_adb_bridge_class, g_read_method, pid);
    if (output == nullptr) {
        LOGD("No output for PID %d", pid);
        return 0;
    }
    
    const char* output_str = env->GetStringUTFChars(output, nullptr);
    int output_len = strlen(output_str);
    
    int copy_len = (output_len < buffer_size - 1) ? output_len : (buffer_size - 1);
    memcpy(buffer, output_str, copy_len);
    buffer[copy_len] = '\0';
    
    env->ReleaseStringUTFChars(output, output_str);
    env->DeleteLocalRef(output);
    
    LOGD("Read %d bytes from PID %d", copy_len, pid);
    return copy_len;
}

/**
 * 终止进程
 */
int adb_terminate_process(int pid) {
    LOGD("adb_terminate_process: %d", pid);
    
    JNIEnv* env = GetJNIEnv();
    if (env == nullptr) {
        return -1;
    }
    
    jboolean result = env->CallStaticBooleanMethod(g_adb_bridge_class, g_terminate_method, pid);
    
    return result ? 0 : -1;
}

/**
 * 清理进程资源
 */
void adb_cleanup_process(int pid) {
    LOGD("adb_cleanup_process: %d", pid);
    
    JNIEnv* env = GetJNIEnv();
    if (env == nullptr) {
        return;
    }
    
    env->CallStaticVoidMethod(g_adb_bridge_class, g_cleanup_method, pid);
}

} // extern "C"
