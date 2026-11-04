/*
 * NEXCRAFT Main Connector - user-approved screen capture.
 * Screen frames are uploaded only to the paired relay while explicit Android capture consent is active.
 */
package com.movtery.zalithlauncher.connector

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.DisplayMetrics
import android.util.Base64
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.movtery.zalithlauncher.R
import java.io.File
import java.io.FileOutputStream
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.json.JSONObject

class ConnectorScreenCaptureService : Service() {
    companion object {
        const val ACTION_START = "com.movtery.zalithlauncher.connector.START_CAPTURE"
        const val ACTION_STOP = "com.movtery.zalithlauncher.connector.STOP_CAPTURE"
        const val EXTRA_RESULT_CODE = "projection_result_code"
        const val EXTRA_RESULT_DATA = "projection_result_data"
        private const val CHANNEL_ID = "nexcraft_connector_capture"
        private const val NOTIFICATION_ID = 7314
        private const val FRAME_FILE = "connector_latest_frame.jpg"
    }

    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var worker: HandlerThread? = null
    private var workerHandler: Handler? = null
    private val stopping = AtomicBoolean(false)
    private val lastFrameSavedAt = AtomicLong(0L)
    private val uploadInFlight = AtomicBoolean(false)
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopCapture()
            stopSelf()
            return START_NOT_STICKY
        }
        if (intent?.action != ACTION_START || projection != null) return START_NOT_STICKY

        val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
        val resultData = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_RESULT_DATA)
        } ?: run { stopSelf(); return START_NOT_STICKY }

        createNotificationChannel()
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        try {
            val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            projection = manager.getMediaProjection(resultCode, resultData)
            projection?.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    stopCapture()
                    stopSelf()
                }
            }, Handler(mainLooper))
            startFrameCapture()
            startDiagnosticsMonitoring()
        } catch (_: Exception) {
            stopCapture()
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun startFrameCapture() {
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        (getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.getRealMetrics(metrics)
        val width = metrics.widthPixels.coerceAtLeast(1)
        val height = metrics.heightPixels.coerceAtLeast(1)
        val density = metrics.densityDpi.coerceAtLeast(1)
        worker = HandlerThread("NEXCRAFT-ScreenCapture").also { it.start() }
        workerHandler = Handler(worker!!.looper)
        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        imageReader?.setOnImageAvailableListener({ reader ->
            val image = runCatching { reader.acquireLatestImage() }.getOrNull() ?: return@setOnImageAvailableListener
            image.use {
                val now = System.currentTimeMillis()
                if (stopping.get() || now - lastFrameSavedAt.get() < 5_000L) return@setOnImageAvailableListener
                lastFrameSavedAt.set(now)
                val plane = it.planes.firstOrNull() ?: return@setOnImageAvailableListener
                val pixelStride = plane.pixelStride
                val rowStride = plane.rowStride
                val rowPadding = (rowStride - pixelStride * width).coerceAtLeast(0)
                val paddedWidth = width + rowPadding / pixelStride
                val padded = Bitmap.createBitmap(paddedWidth, height, Bitmap.Config.ARGB_8888)
                try {
                    padded.copyPixelsFromBuffer(plane.buffer)
                    val frame = Bitmap.createBitmap(padded, 0, 0, width, height)
                    try {
                        val directory = File(cacheDir, "nexcraft_connector").apply { mkdirs() }
                        FileOutputStream(File(directory, FRAME_FILE)).use { output ->
                            frame.compress(Bitmap.CompressFormat.JPEG, 68, output)
                        }
                        if (ConnectorTokenStore(this@ConnectorScreenCaptureService).read() != null &&
                            uploadInFlight.compareAndSet(false, true)
                        ) {
                            val scale = minOf(1f, 960f / maxOf(frame.width, frame.height))
                            val uploadBitmap = if (scale < 1f) {
                                Bitmap.createScaledBitmap(
                                    frame,
                                    (frame.width * scale).toInt().coerceAtLeast(1),
                                    (frame.height * scale).toInt().coerceAtLeast(1),
                                    true
                                )
                            } else frame
                            val bytes = ByteArrayOutputStream().use { output ->
                                uploadBitmap.compress(Bitmap.CompressFormat.JPEG, 48, output)
                                output.toByteArray()
                            }
                            if (uploadBitmap !== frame) uploadBitmap.recycle()
                            if (bytes.size <= 300_000) {
                                serviceScope.launch {
                                    try {
                                        ConnectorRelayClient(this@ConnectorScreenCaptureService).uploadDiagnostic(
                                            "screen",
                                            JSONObject()
                                                .put("mimeType", "image/jpeg")
                                                .put("encoding", "base64")
                                                .put("frame", Base64.encodeToString(bytes, Base64.NO_WRAP))
                                                .put("capturedAt", System.currentTimeMillis())
                                        )
                                    } catch (_: Exception) {
                                        if (ConnectorTokenStore(this@ConnectorScreenCaptureService).read() == null) {
                                            Handler(mainLooper).post {
                                                stopCapture()
                                                stopSelf()
                                            }
                                        }
                                    } finally {
                                        uploadInFlight.set(false)
                                    }
                                }
                            } else {
                                uploadInFlight.set(false)
                            }
                        }
                    } finally {
                        frame.recycle()
                    }
                } catch (_: Exception) {
                    // A transient frame failure must not crash the launcher.
                } finally {
                    padded.recycle()
                }
            }
        }, workerHandler)
        virtualDisplay = projection?.createVirtualDisplay(
            "NEXCRAFT Main Connector",
            width, height, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface, null, workerHandler
        )
    }

    private fun startDiagnosticsMonitoring() {
        serviceScope.launch {
            while (isActive && !stopping.get()) {
                runCatching { ConnectorDiagnosticsReporter(this@ConnectorScreenCaptureService).uploadSnapshot() }
                delay(30_000L)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(CHANNEL_ID, "NEXCRAFT screen sharing", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val stopIntent = Intent(this, ConnectorScreenCaptureService::class.java).setAction(ACTION_STOP)
        val stopPending = PendingIntent.getService(
            this, 7315, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("NEXCRAFT screen sharing is active")
            .setContentText("Screen capture is active. Frames upload only while paired; tap Stop to end.")
            .setOngoing(true)
            .addAction(0, "Stop", stopPending)
            .build()
    }

    private fun stopCapture() {
        if (!stopping.compareAndSet(false, true)) return
        if (ConnectorTokenStore(this).read() != null) {
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                runCatching { ConnectorRelayClient(this@ConnectorScreenCaptureService).clearScreenFrames() }
            }
        }
        runCatching { File(File(cacheDir, "nexcraft_connector"), FRAME_FILE).delete() }
        virtualDisplay?.release()
        virtualDisplay = null
        imageReader?.setOnImageAvailableListener(null, null)
        imageReader?.close()
        imageReader = null
        projection?.stop()
        projection = null
        serviceScope.cancel()
        worker?.quitSafely()
        worker = null
        workerHandler = null
        if (Build.VERSION.SDK_INT >= 24) stopForeground(STOP_FOREGROUND_REMOVE) else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    override fun onDestroy() {
        stopCapture()
        super.onDestroy()
    }
}
