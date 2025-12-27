package com.mobile.scrcpy.android.video

import android.media.MediaCodec
import android.media.MediaFormat
import android.view.Surface
import dadb.AdbShellStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import com.mobile.scrcpy.android.utils.LogManager

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
            LogManager.d(TAG, "========== 开始视频解码 ==========")
            LogManager.d(TAG, "videoStream 类型: ${videoStream.javaClass.name}")
            
            // 先读取一些数据看看格式
            LogManager.d(TAG, "尝试读取第一个数据包（10秒超时）...")
            
            var firstPacket: dadb.AdbShellPacket? = null
            val startTime = System.currentTimeMillis()
            val timeout = 10000L // 10秒超时
            
            try {
                firstPacket = videoStream.read()
                val elapsed = System.currentTimeMillis() - startTime
                LogManager.d(TAG, "收到第一个数据包，耗时: ${elapsed}ms")
            } catch (e: Exception) {
                LogManager.e(TAG, "读取第一个数据包失败: ${e.message}", e)
                return@withContext
            }
            
            if (firstPacket == null) {
                LogManager.e(TAG, "第一个数据包为 null")
                return@withContext
            }
            
            LogManager.d(TAG, "第一个数据包类型: ${firstPacket.javaClass.simpleName}")
            
            if (firstPacket is dadb.AdbShellPacket.StdOut) {
                val data = firstPacket.payload
                LogManager.d(TAG, "第一个数据包大小: ${data.size} 字节")
                if (data.size >= 20) {
                    val preview = data.take(20).joinToString(" ") { "%02X".format(it) }
                    LogManager.d(TAG, "数据预览: $preview")
                }
            } else if (firstPacket is dadb.AdbShellPacket.StdError) {
                val errorMsg = String(firstPacket.payload)
                LogManager.e(TAG, "收到错误输出: $errorMsg")
                return@withContext
            } else if (firstPacket is dadb.AdbShellPacket.Exit) {
                val exitCode = if (firstPacket.payload.isNotEmpty()) firstPacket.payload[0].toInt() else -1
                LogManager.e(TAG, "流立即退出，退出码: $exitCode")
                return@withContext
            }
            
            // 读取设备信息（64 字节）
            LogManager.d(TAG, "读取设备信息...")
            val deviceInfoBuffer = mutableListOf<Byte>()
            if (firstPacket is dadb.AdbShellPacket.StdOut) {
                deviceInfoBuffer.addAll(firstPacket.payload.toList())
            }
            
            while (deviceInfoBuffer.size < 64) {
                val packet = videoStream.read()
                if (packet is dadb.AdbShellPacket.StdOut) {
                    deviceInfoBuffer.addAll(packet.payload.toList())
                    LogManager.d(TAG, "设备信息累计: ${deviceInfoBuffer.size} / 64 字节")
                } else if (packet is dadb.AdbShellPacket.StdError) {
                    LogManager.w(TAG, "StdErr: ${String(packet.payload)}")
                } else if (packet is dadb.AdbShellPacket.Exit) {
                    LogManager.e(TAG, "流在读取设备信息时退出")
                    return@withContext
                }
            }
            
            val deviceInfo = deviceInfoBuffer.take(64).toByteArray()
            val deviceName = String(deviceInfo.takeWhile { it != 0.toByte() }.toByteArray())
            LogManager.d(TAG, "设备名称: $deviceName")
            
            // 移除已读取的设备信息
            repeat(64) { if (deviceInfoBuffer.isNotEmpty()) deviceInfoBuffer.removeAt(0) }
            
            // 读取视频元数据（12 字节）
            LogManager.d(TAG, "读取视频元数据...")
            while (deviceInfoBuffer.size < 12) {
                val packet = videoStream.read()
                if (packet is dadb.AdbShellPacket.StdOut) {
                    deviceInfoBuffer.addAll(packet.payload.toList())
                    LogManager.d(TAG, "元数据累计: ${deviceInfoBuffer.size} / 12 字节")
                } else if (packet is dadb.AdbShellPacket.StdError) {
                    LogManager.w(TAG, "StdErr: ${String(packet.payload)}")
                } else if (packet is dadb.AdbShellPacket.Exit) {
                    LogManager.e(TAG, "流在读取元数据时退出")
                    return@withContext
                }
            }
            
            val metaData = deviceInfoBuffer.take(12).toByteArray()
            val width = ((metaData[0].toInt() and 0xFF) shl 8) or (metaData[1].toInt() and 0xFF)
            val height = ((metaData[2].toInt() and 0xFF) shl 8) or (metaData[3].toInt() and 0xFF)
            LogManager.d(TAG, "视频分辨率: ${width}x${height}")
            
            // 移除已读取的元数据
            repeat(12) { if (deviceInfoBuffer.isNotEmpty()) deviceInfoBuffer.removeAt(0) }
            
            decoder = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            isRunning = true
            LogManager.d(TAG, "========== 开始解码循环 ==========")
            
            // 将剩余数据传递给解码循环
            decodeLoop(videoStream, width, height, deviceInfoBuffer)
        } catch (e: Exception) {
            LogManager.e(TAG, "解码器启动失败: ${e.message}", e)
        }
    }
    
    private fun readExactly(stream: AdbShellStream, size: Int): ByteArray? {
        val buffer = ByteArray(size)
        var offset = 0
        LogManager.d(TAG, "准备读取 $size 字节数据...")
        while (offset < size) {
            try {
                val packet = stream.read()
                LogManager.d(TAG, "收到数据包类型: ${packet.javaClass.simpleName}")
                if (packet is dadb.AdbShellPacket.StdOut) {
                    val data = packet.payload
                    LogManager.d(TAG, "收到 StdOut 数据: ${data.size} 字节")
                    if (data.isNotEmpty()) {
                        val preview = data.take(minOf(20, data.size)).joinToString(" ") { "%02X".format(it) }
                        LogManager.d(TAG, "数据预览: $preview")
                    }
                    val remaining = size - offset
                    val toCopy = minOf(data.size, remaining)
                    System.arraycopy(data, 0, buffer, offset, toCopy)
                    offset += toCopy
                    LogManager.d(TAG, "已读取: $offset / $size 字节")
                } else if (packet is dadb.AdbShellPacket.Exit) {
                    LogManager.e(TAG, "流意外结束")
                    return null
                } else {
                    LogManager.w(TAG, "收到其他类型数据包: ${packet.javaClass.simpleName}")
                }
            } catch (e: Exception) {
                LogManager.e(TAG, "读取数据包失败: ${e.message}", e)
                return null
            }
        }
        LogManager.d(TAG, "成功读取 $size 字节数据")
        return buffer
    }
    
    private fun decodeLoop(videoStream: AdbShellStream, width: Int, height: Int, initialBuffer: MutableList<Byte>) {
        try {
            val bufferInfo = MediaCodec.BufferInfo()
            var configured = false
            var sps: ByteArray? = null
            var pps: ByteArray? = null
            val frameBuffer = initialBuffer
            
            LogManager.d(TAG, "解码循环开始，初始缓冲区: ${frameBuffer.size} 字节")
            
            while (isRunning) {
                val packet = videoStream.read()
                
                if (packet is dadb.AdbShellPacket.StdOut) {
                    val data = packet.payload
                    if (data.isNotEmpty()) {
                        data.forEach { frameBuffer.add(it) }
                        LogManager.d(TAG, "收到视频数据: ${data.size} 字节，缓冲区总计: ${frameBuffer.size} 字节")
                    }
                } else if (packet is dadb.AdbShellPacket.StdError) {
                    val errorMsg = String(packet.payload)
                    LogManager.w(TAG, "StdErr: $errorMsg")
                    continue
                } else if (packet is dadb.AdbShellPacket.Exit) {
                    val exitCode = if (packet.payload.isNotEmpty()) packet.payload[0].toInt() else -1
                    LogManager.d(TAG, "视频流结束，退出码: $exitCode")
                    break
                }
                
                // 处理缓冲区中的 NAL 单元
                while (true) {
                    val nalUnit = extractNalUnit(frameBuffer) ?: break
                    if (nalUnit.isEmpty()) continue
                    
                    val nalType = nalUnit[0].toInt() and 0x1F
                    LogManager.d(TAG, "提取 NAL 单元，类型: $nalType, 大小: ${nalUnit.size}")
                    
                    when (nalType) {
                        NAL_TYPE_SPS -> {
                            LogManager.d(TAG, "收到 SPS，大小: ${nalUnit.size}")
                            sps = nalUnit
                            if (pps != null && !configured) {
                                configureDecoder(width, height, sps!!, pps!!)
                                configured = true
                            }
                        }
                        NAL_TYPE_PPS -> {
                            LogManager.d(TAG, "收到 PPS，大小: ${nalUnit.size}")
                            pps = nalUnit
                            if (sps != null && !configured) {
                                configureDecoder(width, height, sps!!, pps!!)
                                configured = true
                            }
                        }
                        else -> {
                            if (configured) {
                                LogManager.d(TAG, "解码帧，NAL 类型: $nalType")
                                decodeFrame(nalUnit, bufferInfo)
                            } else {
                                LogManager.w(TAG, "解码器未配置，跳过 NAL 类型: $nalType")
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            LogManager.e(TAG, "解码循环错误: ${e.message}", e)
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
            LogManager.d(TAG, "配置解码器: ${width}x${height}")
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
            LogManager.d(TAG, "解码器配置成功并已启动")
        } catch (e: Exception) {
            LogManager.e(TAG, "配置解码器失败: ${e.message}", e)
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
            LogManager.e(TAG, "解码帧失败: ${e.message}", e)
        }
    }
    
    fun stop() {
        isRunning = false
        try {
            decoder?.stop()
            decoder?.release()
            decoder = null
            LogManager.d(TAG, "解码器已停止")
        } catch (e: Exception) {
            LogManager.e(TAG, "停止解码器失败: ${e.message}", e)
        }
    }
}
