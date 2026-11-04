/*
 * NEXCRAFT Main Connector - user-approved screen capture.
 * Screen frames remain on-device until an authenticated relay uploader is attached.
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
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.movtery.zalithlauncher.R
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicBoolean

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
                if (stopping.get()) return@setOnImageAvailableListener
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
                            frame.compress(Bitmap.CompressFormat.JPEG, 72, output)
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
            .setContentText("Your screen is being captured. Tap Stop to end sharing.")
            .setOngoing(true)
            .addAction(0, "Stop", stopPending)
            .build()
    }

    private fun stopCapture() {
        if (!stopping.compareAndSet(false, true)) return
        virtualDisplay?.release()
        virtualDisplay = null
        imageReader?.setOnImageAvailableListener(null, null)
        imageReader?.close()
        imageReader = null
        projection?.stop()
        projection = null
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
