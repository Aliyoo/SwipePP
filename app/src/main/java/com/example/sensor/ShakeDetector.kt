package com.example.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.sqrt

/**
 * Hardware accelerometer listener for FlipYear's signature "Shake to Undo" mechanic.
 * Emits a Unit event whenever physical shake g-force exceeds threshold with debounce cooldown.
 */
class ShakeDetector(context: Context) {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    val shakeEvents: Flow<Unit> = callbackFlow {
        val sm = sensorManager
        val sensor = accelerometer
        if (sm == null || sensor == null) {
            awaitClose { }
            return@callbackFlow
        }

        var lastShakeTimestamp = 0L
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val gX = event.values[0] / SensorManager.GRAVITY_EARTH
                val gY = event.values[1] / SensorManager.GRAVITY_EARTH
                val gZ = event.values[2] / SensorManager.GRAVITY_EARTH
                val gForce = sqrt(gX * gX + gY * gY + gZ * gZ)
                val now = System.currentTimeMillis()
                if (gForce > SHAKE_THRESHOLD_G && now - lastShakeTimestamp > SHAKE_COOLDOWN_MS) {
                    lastShakeTimestamp = now
                    trySend(Unit)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        awaitClose { sm.unregisterListener(listener) }
    }

    companion object {
        private const val SHAKE_THRESHOLD_G = 2.35f
        private const val SHAKE_COOLDOWN_MS = 3000L
    }
}
