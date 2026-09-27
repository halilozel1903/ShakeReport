package io.github.halilozel1903.shakereport

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.View
import androidx.annotation.RequiresApi

/**
 * Captures the activity's window: [PixelCopy] on API 26+ (includes hardware-rendered content
 * such as Compose, video and maps), `View.draw` on older versions or when PixelCopy fails.
 * Dialogs and popups live in separate windows and are not part of the screenshot.
 */
internal object ScreenshotCapturer {
    fun capture(activity: Activity, onResult: (Bitmap?) -> Unit) {
        val decorView = activity.window?.decorView
        if (decorView == null || decorView.width <= 0 || decorView.height <= 0) {
            onResult(null)
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            pixelCopy(activity, decorView, onResult)
        } else {
            onResult(drawView(decorView))
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun pixelCopy(activity: Activity, decorView: View, onResult: (Bitmap?) -> Unit) {
        val bitmap = try {
            Bitmap.createBitmap(decorView.width, decorView.height, Bitmap.Config.ARGB_8888)
        } catch (e: OutOfMemoryError) {
            onResult(null)
            return
        }
        try {
            PixelCopy.request(
                activity.window,
                bitmap,
                { result ->
                    if (result == PixelCopy.SUCCESS) {
                        onResult(bitmap)
                    } else {
                        bitmap.recycle()
                        onResult(drawView(decorView))
                    }
                },
                // The listener must run on the main thread: it may touch views and start activities.
                Handler(Looper.getMainLooper()),
            )
        } catch (e: IllegalArgumentException) {
            // The window has no surface yet (or anymore).
            bitmap.recycle()
            onResult(drawView(decorView))
        }
    }

    private fun drawView(view: View): Bitmap? = try {
        Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888).also { bitmap ->
            view.draw(Canvas(bitmap))
        }
    } catch (e: Exception) {
        // For example "Software rendering doesn't support hardware bitmaps".
        null
    } catch (e: OutOfMemoryError) {
        null
    }
}
