package io.github.halilozel1903.shakereport

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import androidx.annotation.MainThread
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Shake the phone to report a bug.
 *
 * ```kotlin
 * class App : Application() {
 *     override fun onCreate() {
 *         super.onCreate()
 *         if (BuildConfig.DEBUG) ShakeReport.install(this)
 *     }
 * }
 * ```
 *
 * A shake captures the current screen, device and app details and the app's recent log lines,
 * then opens a report screen where the tester marks up the screenshot, describes the problem
 * and sends it through the share sheet or a custom [ReportSender].
 */
public object ShakeReport {
    internal val scope: CoroutineScope = MainScope()

    private var application: Application? = null
    private var listener: ForegroundShakeListener? = null
    private var capturing = false

    internal var config: ShakeReportConfig = ShakeReportConfig()
        private set

    /** Whether [install] has been called (and [uninstall] has not). */
    public val isInstalled: Boolean get() = listener != null

    /**
     * Pauses (`false`) or resumes (`true`) shake detection without uninstalling, for example
     * during a game level or a camera screen. [show] keeps working.
     */
    public var isShakeEnabled: Boolean = true
        @MainThread set(value) {
            field = value
            listener?.refresh()
        }

    /**
     * Starts listening for shakes while any activity of the app is in the foreground.
     * Call it once from `Application.onCreate`, typically only in debug or beta builds.
     * Calling it again replaces the previous configuration.
     */
    @MainThread
    public fun install(application: Application, config: ShakeReportConfig = ShakeReportConfig()) {
        uninstall()
        this.config = config
        this.application = application
        val listener = ForegroundShakeListener(
            context = application,
            shakeConfig = config.shake,
            isEnabled = { isShakeEnabled },
            onShake = { activity -> if (activity !is ShakeReportActivity) show(activity) },
        )
        application.registerActivityLifecycleCallbacks(listener)
        this.listener = listener
    }

    /** Stops listening for shakes. An open report screen stays usable. */
    @MainThread
    public fun uninstall() {
        listener?.let { current ->
            current.stop()
            application?.unregisterActivityLifecycleCallbacks(current)
        }
        listener = null
        application = null
    }

    /**
     * Opens the report screen from code, for example from a "Report a bug" menu item or on
     * emulators where shaking is awkward. Works without [install] too (with default settings).
     *
     * @param activity The screen being reported; it is captured when [screenshot] is `null`.
     * @param screenshot Use this image instead of capturing the window, for example a frame of a
     *   `SurfaceView` you rendered yourself.
     * @param description Pre-filled description text.
     * @param openMarkup Start in the drawing editor instead of the form.
     */
    @MainThread
    public fun show(
        activity: Activity,
        screenshot: Bitmap? = null,
        description: String = "",
        openMarkup: Boolean = false,
    ) {
        if (ReportSessionHolder.current != null) {
            // A report is already open: bring it back instead of starting a second one.
            activity.startActivity(
                Intent(activity, ShakeReportActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT),
            )
            return
        }
        if (screenshot != null) {
            openReport(activity, screenshot, description, openMarkup)
            return
        }
        if (capturing) return
        capturing = true
        ScreenshotCapturer.capture(activity) { captured ->
            capturing = false
            openReport(activity, captured, description, openMarkup)
        }
    }

    /**
     * Captures [activity]'s window as a bitmap (PixelCopy on API 26+, `View.draw` before).
     * [onResult] runs on the main thread and receives `null` when the capture failed.
     */
    @MainThread
    public fun captureScreenshot(activity: Activity, onResult: (Bitmap?) -> Unit) {
        ScreenshotCapturer.capture(activity, onResult)
    }

    private fun openReport(activity: Activity, screenshot: Bitmap?, description: String, openMarkup: Boolean) {
        if (activity.isFinishing || activity.isDestroyed || ReportSessionHolder.current != null) return
        val config = config
        val customData = try {
            config.customData()
        } catch (e: Exception) {
            mapOf("customData failed" to e.toString())
        }
        val session = ReportSession(
            screenshot = screenshot,
            deviceInfo = DeviceInfoCollector.collect(activity),
            customData = customData,
            config = config,
            createdAtMillis = System.currentTimeMillis(),
            initialDescription = description,
            openMarkup = openMarkup,
        )
        ReportSessionHolder.current = session
        try {
            activity.startActivity(Intent(activity, ShakeReportActivity::class.java))
        } catch (e: RuntimeException) {
            ReportSessionHolder.current = null
            throw e
        }
        if (config.includeLogs) {
            scope.launch {
                val lines = withContext(Dispatchers.IO) { LogcatCollector.read(config.maxLogLines) }
                session.logs = lines
            }
        }
    }
}
