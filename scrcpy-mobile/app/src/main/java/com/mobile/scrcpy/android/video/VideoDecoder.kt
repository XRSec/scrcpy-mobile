package com.mobile.scrcpy.android.video

import android.media.MediaCodec
import android.media.MediaFormat
import android.util.Log
import android.view.Surface
import dadb.AdbShellStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer

class VideoDecoder(private val surface: Surface) {
    private val TAG = "VideoDecoder"
    private var decoder: MediaCodec? = null
    private var isRunning = false
    
    private companion object {
        const val NAL_TYPE_SPS = 7
        const val NAL_TYPE_PPS = 8
        const val NAL_TYPE_IDR = 5
    }
    
    suspend fun start(videoStream: AdbShellStream) = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "========== 开始视频解码 ==========")
            val deviceInfo = readExactly(videoStream, 64)
            if (deviceInfo == null) {
                Log.e(TAG, "无法读取设备信息")
                return@withContext
            }
            val deviceName = String(deviceInfo.takeWhile { it != 0.toByte() }.toByteArray())
            Log.d(TAG, "设备名称: $deviceName")
            
            val metaData = readExactly(videoStream, 12)
            if (metaData == null) {
                Log.e(TAG, "无法读取视频元数据")
                return@withContext
            }
            val width = ((metaData[0].toInt() and 0xFF) shl 8) or (metaData[1].toInt() and 0xFF)
            val height = ((metaData[2].toInt() and 0xFF) shl 8) or (metaData[3].toInt() and 0xFF)
            Log.d(TAG, "视频分辨率: ${width}x${height}")
            
            decoder = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            isRunning = true
            Log.d(TAG, "========== 开始解码循环 ==========")
            decodeLoop(videoStream, width, height)
        } catch (e: Exception) {
            Log.e(TAG, "解码器启动失败: ${e.message}", e)
        }
    }
    
    private fun readExactly(stream: AdbShellStream, size: Int): ByteArray? {
        val buffer = ByteArray(size)
        var offset = 0
        Log.d(TAG, "准备读取 $size 字节数据...")
        while (offset < size) {
            try {
                val packet = stream.read()
                Log.d(TAG, "收到数据包类型: ${packet.javaClass.simpleName}")
                if (packet is dadb.AdbShellPacket.StdOut) {
                    val data = packet.payload
                    Log.d(TAG, "收到 StdOut 数据: ${data.size} 字节")
                    if (data.isNotEmpty()) {
                        val preview = data.take(minOf(20, data.size)).joinToString(" ") { "%02X".format(it) }
                        Log.d(TAG, "数据预览: $preview")
                    }
                    val remaining = size - offset
                    val toCopy = minOf(data.size, remaining)
                    System.arraycopy(data, 0, buffer, offset, toCopy)
                    offset += toCopy
                    Log.d(TAG, "已读取: $offset / $size 字节")
                } else if (packet is dadb.AdbShellPacket.Exit) {
                    Log.e(TAG, "流意外结束")
                    return null
                } else {
                    Log.w(TAG, "收到其他类型数据包: ${packet.javaClass.simpleName}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "读取数据包失败: ${e.message}", e)
                return null
            }
        }
        Log.d(TAG, "成功读取 $size 字节数据")
        return buffer
    }
    
    private fun decodeLoop(videoStream: AdbShellStream, width: Int, height: Int) {
        try {
            val bufferInfo = MediaCodec.BufferInfo()
            var configured = false
            var sps: ByteArray? = null
            var pps: ByteArray? = null
            val frameBuffer = mutableListOf<Byte>()
            
            while (isRunning) {
                val packet = videoStream.read()
                if (packet is dadb.AdbShellPacket.StdOut) {
                    val data = packet.payload
                    data.forEach { frameBuffer.add(it) }
                    
                    while (true) {
                        val nalUnit = extractNalUnit(frameBuffer) ?: break
                        if (nalUnit.isEmpty()) continue
                        val nalType = nalUnit[0].toInt() and 0x1F
                        
                        when (nalType) {
                            NAL_TYPE_SPS -> {
                                Log.d(TAG, "收到 SPS，大小: ${nalUnit.size}")
                                sps = nalUnit
                                if (pps != null && !configured) {
                                    configureDecoder(width, height, sps!!, pps!!)
                                    configured = true
                                }
                            }
                            NAL_TYPE_PPS -> {
                                Log.d(TAG, "收到 PPS，大小: ${nalUnit.size}")
                                pps = nalUnit
                                if (sps != null && !configured) {
                                    configureDecoder(width, height, sps!!, pps!!)
                                    configured = true
                                }
                            }
                            else -> {
                                if (configured) {
                                    decodeFrame(nalUnit, bufferInfo)
                                }
                            }
                        }
                    }
                } else if (packet is dadb.AdbShellPacket.Exit) {
                    Log.d(TAG, "视频流结束")
                    break
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "解码循环错误: ${e.message}", e)
        }
    }
    
    private fun extractNalUnit(buffer: MutableList<Byte>): ByteArray? {
        if (buffer.size < 4) return null
        var startIndex = -1
        for (i in 0 until buffer.size - 3) {
            if (buffer[i] == 0.toByte() && buffer[i + 1] == 0.toByte() && 
                buffer[i + 2] == 0.toByte() && buffer[i + 3] == 1.toByte()) {
                startIndex = i
                break
            }
        }
        if (startIndex == -1) return null
        
        var endIndex = -1
        for (i in startIndex + 4 until buffer.size - 3) {
            if (buffer[i] == 0.toByte() && buffer[i + 1] == 0.toByte() && 
                buffer[i + 2] == 0.toByte() && buffer[i + 3] == 1.toByte()) {
                endIndex = i
                break
            }
        }
        if (endIndex == -1) return null
        
        val nalUnit = buffer.subList(startIndex + 4, endIndex).toByteArray()
        repeat(endIndex) { buffer.removeAt(0) }
        return nalUnit
    }
    
    private fun configureDecoder(width: Int, height: Int, sps: ByteArray, pps: ByteArray) {
        try {
            Log.d(TAG, "配置解码器: ${width}x${height}")
            val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height)
            val csd0 = ByteBuffer.allocate(sps.size + 4)
            csd0.put(byteArrayOf(0, 0, 0, 1))
            csd0.put(sps)
            csd0.flip()
            format.setByteBuffer("csd-0", csd0)
            
            val csd1 = ByteBuffer.allocate(pps.size + 4)
            csd1.put(byteArrayOf(0, 0, 0, 1))
            csd1.put(pps)
            csd1.flip()
            format.setByteBuffer("csd-1", csd1)
            
            decoder?.configure(format, surface, null, 0)
            decoder?.start()
            Log.d(TAG, "解码器配置成功并已启动")
        } catch (e: Exception) {
            Log.e(TAG, "配置解码器失败: ${e.message}", e)
        }
    }
    
    private fun decodeFrame(nalUnit: ByteArray, bufferInfo: MediaCodec.BufferInfo) {
        try {
            val inputBufferIndex = decoder?.dequeueInputBuffer(10000) ?: -1
            if (inputBufferIndex >= 0) {
                val inputBuffer = decoder?.getInputBuffer(inputBufferIndex)
                inputBuffer?.clear()
                inputBuffer?.put(byteArrayOf(0, 0, 0, 1))
                inputBuffer?.put(nalUnit)
                decoder?.queueInputBuffer(inputBufferIndex, 0, nalUnit.size + 4, System.nanoTime() / 1000, 0)
            }
            val outputBufferIndex = decoder?.dequeueOutputBuffer(bufferInfo, 0) ?: -1
            if (outputBufferIndex >= 0) {
                decoder?.releaseOutputBuffer(outputBufferIndex, true)
            }
        } catch (e: Exception) {
            Log.e(TAG, "解码帧失败: ${e.message}", e)
        }
    }
    
    fun stop() {
        isRunning = false
        try {
            decoder?.stop()
            decoder?.release()
            decoder = null
            Log.d(TAG, "解码器已停止")
        } catch (e: Exception) {
            Log.e(TAG, "停止解码器失败: ${e.message}", e)
        }
    }
}
