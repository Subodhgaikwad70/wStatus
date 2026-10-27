package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import com.example.model.MediaType
import com.example.model.StatusItem
import com.example.model.WhatsAppAppType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer

object DemoMediaGenerator {
    private const val TAG = "DemoMediaGenerator"

    suspend fun generateDemoStatuses(context: Context): List<StatusItem> = withContext(Dispatchers.IO) {
        val demoDir = File(context.filesDir, "demo_statuses").apply { mkdirs() }
        val items = mutableListOf<StatusItem>()

        val imageConfigs = listOf(
            ImageConfig("Vibrant Sunset 🌅", "Golden hour in the mountains ✨", Color.parseColor("#FF512F"), Color.parseColor("#DD2476"), WhatsAppAppType.WHATSAPP, "Travel"),
            ImageConfig("Morning Motivation 💡", "Focus on progress, not perfection.", Color.parseColor("#059669"), Color.parseColor("#10B981"), WhatsAppAppType.WHATSAPP, "Quotes"),
            ImageConfig("Cozy Coffee ☕", "Espresso & quiet morning thoughts", Color.parseColor("#795548"), Color.parseColor("#3E2723"), WhatsAppAppType.WHATSAPP_BUSINESS, "Daily"),
            ImageConfig("Beach Getaway 🏖️", "Clear waters and salty breezes", Color.parseColor("#00B4DB"), Color.parseColor("#0083B0"), WhatsAppAppType.WHATSAPP, "Travel"),
            ImageConfig("Android Dev 🚀", "Building modern Jetpack Compose apps!", Color.parseColor("#1E3A8A"), Color.parseColor("#3B82F6"), WhatsAppAppType.GB_WHATSAPP, "Tech"),
            ImageConfig("Workout Done 💪", "Day 45 / 90 — Crushing all limits", Color.parseColor("#B91C1C"), Color.parseColor("#EF4444"), WhatsAppAppType.DUAL_WHATSAPP, "Fitness")
        )

        val baseTime = System.currentTimeMillis() - (1000 * 60 * 60 * 3) // 3 hours ago

        imageConfigs.forEachIndexed { index, config ->
            val file = File(demoDir, "demo_status_img_${index + 1}.jpg")
            if (!file.exists() || file.length() == 0L) {
                createImageStatus(file, config)
            }
            items.add(
                StatusItem(
                    id = "demo_img_${index + 1}",
                    uriString = file.toURI().toString(),
                    filePath = file.absolutePath,
                    title = config.title,
                    mediaType = MediaType.IMAGE,
                    sizeBytes = file.length(),
                    dateModified = baseTime + (index * 1000 * 60 * 20),
                    appType = config.appType,
                    isSaved = false,
                    isFavorite = index == 0,
                    albumTag = config.tag,
                    isDemo = true
                )
            )
        }

        // Generate 2 demo videos
        val videoConfigs = listOf(
            VideoConfig("City Lights Motion 🌃", WhatsAppAppType.WHATSAPP, "Travel", Color.parseColor("#1E1B4B"), Color.parseColor("#4338CA")),
            VideoConfig("Nature Waterfall Loop 🌊", WhatsAppAppType.WHATSAPP_BUSINESS, "Nature", Color.parseColor("#064E3B"), Color.parseColor("#047857"))
        )

        videoConfigs.forEachIndexed { index, vConfig ->
            val videoFile = File(demoDir, "demo_status_vid_${index + 1}.mp4")
            if (!videoFile.exists() || videoFile.length() == 0L) {
                createSyntheticVideo(videoFile, vConfig)
            }
            if (videoFile.exists() && videoFile.length() > 0L) {
                items.add(
                    StatusItem(
                        id = "demo_vid_${index + 1}",
                        uriString = videoFile.toURI().toString(),
                        filePath = videoFile.absolutePath,
                        title = vConfig.title,
                        mediaType = MediaType.VIDEO,
                        sizeBytes = videoFile.length(),
                        dateModified = baseTime + ((index + 6) * 1000 * 60 * 15),
                        appType = vConfig.appType,
                        isSaved = false,
                        isFavorite = false,
                        albumTag = vConfig.tag,
                        durationMs = 5000L,
                        isDemo = true
                    )
                )
            }
        }

        items
    }

    private data class ImageConfig(
        val title: String,
        val subtitle: String,
        val colorStart: Int,
        val colorEnd: Int,
        val appType: WhatsAppAppType,
        val tag: String
    )

    private data class VideoConfig(
        val title: String,
        val appType: WhatsAppAppType,
        val tag: String,
        val colorStart: Int,
        val colorEnd: Int
    )

    private fun createImageStatus(file: File, config: ImageConfig) {
        val width = 720
        val height = 1280
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Gradient background
        val gradient = LinearGradient(0f, 0f, 0f, height.toFloat(), config.colorStart, config.colorEnd, Shader.TileMode.CLAMP)
        val bgPaint = Paint().apply {
            shader = gradient
            isAntiAlias = true
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Decorative background elements
        val circlePaint = Paint().apply {
            color = Color.argb(40, 255, 255, 255)
            isAntiAlias = true
        }
        canvas.drawCircle(width * 0.85f, height * 0.15f, 220f, circlePaint)
        canvas.drawCircle(width * 0.15f, height * 0.85f, 300f, circlePaint)

        // Top Status header bar
        val barPaint = Paint().apply {
            color = Color.argb(120, 0, 0, 0)
            isAntiAlias = true
        }
        val cardRect = RectF(40f, height * 0.35f, width - 40f, height * 0.65f)
        canvas.drawRoundRect(cardRect, 40f, 40f, barPaint)

        // Title text
        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 52f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText(config.title, width / 2f, height * 0.48f, titlePaint)

        // Subtitle text
        val subPaint = Paint().apply {
            color = Color.parseColor("#E0E7FF")
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText(config.subtitle, width / 2f, height * 0.54f, subPaint)

        // App badge at bottom
        val badgePaint = Paint().apply {
            color = Color.argb(180, 255, 255, 255)
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("Shared via ${config.appType.displayName}", width / 2f, height * 0.88f, badgePaint)

        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }
        bitmap.recycle()
    }

    private fun createSyntheticVideo(file: File, config: VideoConfig) {
        try {
            val width = 480
            val height = 854
            val frameRate = 25
            val durationSeconds = 4
            val totalFrames = frameRate * durationSeconds
            val bitRate = 1_000_000

            val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
                setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }

            val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            val inputSurface = encoder.createInputSurface()
            encoder.start()

            val muxer = MediaMuxer(file.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var videoTrackIndex = -1
            var muxerStarted = false
            val bufferInfo = MediaCodec.BufferInfo()

            for (frame in 0 until totalFrames) {
                // Draw frame to input surface
                val canvas = inputSurface.lockCanvas(null)
                if (canvas != null) {
                    val progress = frame.toFloat() / totalFrames
                    val bgPaint = Paint().apply {
                        shader = LinearGradient(0f, 0f, 0f, height.toFloat(), config.colorStart, config.colorEnd, Shader.TileMode.CLAMP)
                    }
                    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

                    // Moving pulse circle
                    val pulseRadius = 80f + 60f * Math.sin(progress * Math.PI * 4).toFloat()
                    val circlePaint = Paint().apply {
                        color = Color.argb(180, 255, 255, 255)
                        isAntiAlias = true
                    }
                    canvas.drawCircle(width / 2f, height * 0.45f, pulseRadius, circlePaint)

                    // Play icon in center
                    val playPaint = Paint().apply {
                        color = Color.parseColor("#10B981")
                        isAntiAlias = true
                    }
                    val path = android.graphics.Path().apply {
                        moveTo(width / 2f - 20f, height * 0.45f - 30f)
                        lineTo(width / 2f + 30f, height * 0.45f)
                        lineTo(width / 2f - 20f, height * 0.45f + 30f)
                        close()
                    }
                    canvas.drawPath(path, playPaint)

                    // Title
                    val titlePaint = Paint().apply {
                        color = Color.WHITE
                        textSize = 36f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        textAlign = Paint.Align.CENTER
                        isAntiAlias = true
                    }
                    canvas.drawText(config.title, width / 2f, height * 0.65f, titlePaint)

                    val timerPaint = Paint().apply {
                        color = Color.parseColor("#A7F3D0")
                        textSize = 28f
                        textAlign = Paint.Align.CENTER
                        isAntiAlias = true
                    }
                    canvas.drawText("Video Status • 00:${String.format("%02d", frame / frameRate)}", width / 2f, height * 0.72f, timerPaint)

                    inputSurface.unlockCanvasAndPost(canvas)
                }

                // Drain encoder
                drainEncoder(encoder, muxer, bufferInfo, false, { index ->
                    videoTrackIndex = index
                    muxerStarted = true
                }, { muxerStarted })
            }

            // Signal EOS
            encoder.signalEndOfInputStream()
            drainEncoder(encoder, muxer, bufferInfo, true, { index ->
                videoTrackIndex = index
                muxerStarted = true
            }, { muxerStarted })

            encoder.stop()
            encoder.release()
            if (muxerStarted) {
                muxer.stop()
            }
            muxer.release()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create synthetic video, fallback to image-based placeholder", e)
        }
    }

    private fun drainEncoder(
        encoder: MediaCodec,
        muxer: MediaMuxer,
        bufferInfo: MediaCodec.BufferInfo,
        endOfStream: Boolean,
        onTrackAdded: (Int) -> Unit,
        isMuxerStarted: () -> Boolean
    ) {
        val timeoutUs = 10000L
        while (true) {
            val status = encoder.dequeueOutputBuffer(bufferInfo, timeoutUs)
            if (status == MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (!endOfStream) break
            } else if (status == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                val newFormat = encoder.outputFormat
                val trackIndex = muxer.addTrack(newFormat)
                muxer.start()
                onTrackAdded(trackIndex)
            } else if (status >= 0) {
                val encodedData: ByteBuffer? = encoder.getOutputBuffer(status)
                if (encodedData != null && isMuxerStarted() && (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                    encodedData.position(bufferInfo.offset)
                    encodedData.limit(bufferInfo.offset + bufferInfo.size)
                    muxer.writeSampleData(0, encodedData, bufferInfo)
                }
                encoder.releaseOutputBuffer(status, false)
                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    break
                }
            }
        }
    }
}
