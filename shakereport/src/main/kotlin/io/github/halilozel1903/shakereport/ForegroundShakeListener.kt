package io.github.halilozel1903.shakereport

import android.app.Activity
import android.app.Application
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import io.github.halilozel1903.shakereport.core.ShakeConfig
import io.github.halilozel1903.shakereport.core.ShakeDetector
import java.lang.ref.WeakReference

/**
 * Tracks the resumed activity and listens to the accelerometer only while one is in the
 * foreground, so there is no battery cost while the app is in the background.
 */
internal class ForegroundShakeListener(
    context: Context,
    shakeConfig: ShakeConfig,
    private val isEnabled: () -> Boolean,
    private val onShake: (Activity) -> Unit,
) : Application.ActivityLifecycleCallbacks {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val detector = ShakeDetector(shakeConfig)
    private var resumed: WeakReference<Activity>? = null
    private var listening = false

    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.values.size < 3) return
            val shaken = detector.onAcceleration(
                x = event.values[0],
                y = event.values[1],
                z = event.values[2],
                timestampMillis = event.timestamp / 1_000_000,
            )
            if (shaken) resumed?.get()?.let(onShake)
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    /** Starts or stops the sensor to match the current state. Main thread only. */
    fun refresh() {
        val activity = resumed?.get()
        val shouldListen = activity != null && activity !is ShakeReportActivity && isEnabled()
        if (shouldListen) start() else stop()
    }

    fun stop() {
        if (!listening) return
        sensorManager?.unregisterListener(sensorListener)
        listening = false
    }

    private fun start() {
        if (listening) return
        val manager = sensorManager ?: return
        val sensor = accelerometer ?: return
        detector.reset()
        // Without a Handler, events are delivered on the main thread.
        listening = manager.registerListener(sensorListener, sensor, SensorManager.SENSOR_DELAY_GAME)
    }

    override fun onActivityResumed(activity: Activity) {
        resumed = WeakReference(activity)
        refresh()
    }

    override fun onActivityPaused(activity: Activity) {
        if (resumed?.get() === activity) resumed = null
        refresh()
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
}
