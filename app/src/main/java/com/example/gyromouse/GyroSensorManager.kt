package com.example.gyromouse

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import android.widget.Toast
import kotlin.math.abs
import kotlin.math.roundToInt

class GyroSensorManager(
    private val context: Context,
    private val onMovementDetected: (dx: Byte, dy: Byte) -> Unit
) : SensorEventListener {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val gyroSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    private val gyroOffset = FloatArray(3)
    private var isCalibrated = false
    private var calibrationSamples = 0
    private var lastTimestamp: Long = 0L
    private val gyroSensitivity = 0.8f
    private val deadZone = 0.15f          // Rad/s

    init {
        if (gyroSensor != null) {
            sensorManager.registerListener(this, gyroSensor, SensorManager.SENSOR_DELAY_GAME)
            Log.d("GyroSensor", "Gyroscope registered")
        } else {
            Log.e("GyroSensor", "No gyroscope available!")
            showToast("Gyroscope not available!")
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_GYROSCOPE) return

        if (!isCalibrated) {
            for (i in 0..2) {
                gyroOffset[i] += event.values[i]
            }
            calibrationSamples++
            if (calibrationSamples >= 100) {
                for (i in 0..2) {
                    gyroOffset[i] /= 100f
                }
                isCalibrated = true
                Log.d("GyroSensor", "Calibration complete: ${gyroOffset.contentToString()}")
            }
            return
        }

        val dt = if (lastTimestamp != 0L) {
            (event.timestamp - lastTimestamp) / 1_000_000_000f
        } else {
            0f
        }
        lastTimestamp = event.timestamp
        if (dt <= 0f) return

        val gyroX = event.values[0] - gyroOffset[0]
        val gyroZ = event.values[2] - gyroOffset[2]

        var dx = 0
        var dy = 0

        if (abs(gyroZ) > deadZone) {
            dx = (gyroZ * dt * 500 * gyroSensitivity)
                .roundToInt()
                .coerceIn(-127, 127)
        }
        if (abs(gyroX) > deadZone) {
            dy = (gyroX * dt * 500 * gyroSensitivity)
                .roundToInt()
                .coerceIn(-127, 127)
        }

        Log.d("GyroSensor", "dx: $dx, dy: $dy | gyroZ: $gyroZ, gyroX: $gyroX")
        if (dx != 0 || dy != 0) {
            onMovementDetected(dx.toByte(), dy.toByte())
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
    }

    fun unregister() {
        sensorManager.unregisterListener(this)
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
