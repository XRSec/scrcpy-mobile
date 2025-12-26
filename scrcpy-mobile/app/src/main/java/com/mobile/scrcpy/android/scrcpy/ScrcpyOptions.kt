package com.mobile.scrcpy.android.scrcpy

data class ScrcpyOptions(
    val maxSize: Int = 1920,
    val bitRate: Int = 8000000,
    val maxFps: Int = 60,
    val displayId: Int = 0,
    val showTouches: Boolean = false,
    val stayAwake: Boolean = true,
    val codecOptions: String = "profile=1,level=2",
    val encoderName: String? = null,
    val powerOffOnClose: Boolean = false
) {
    fun toServerArgs(): List<String> {
        return buildList {
            add("log_level=info")
            add("max_size=$maxSize")
            add("bit_rate=$bitRate")
            add("max_fps=$maxFps")
            add("display_id=$displayId")
            add("show_touches=$showTouches")
            add("stay_awake=$stayAwake")
            add("codec_options=$codecOptions")
            add("tunnel_forward=true")
            add("control=true")
            add("audio=false")
            add("video=true")
            add("video_codec=h264")
            if (encoderName != null) {
                add("video_encoder=$encoderName")
            }
            add("power_off_on_close=$powerOffOnClose")
        }
    }
}
