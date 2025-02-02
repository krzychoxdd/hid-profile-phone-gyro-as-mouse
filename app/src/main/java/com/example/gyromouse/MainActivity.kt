package com.example.gyromouse

import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity

@RequiresApi(Build.VERSION_CODES.S)
class MainActivity : AppCompatActivity() {

    private lateinit var hidManager: HidManager
    private lateinit var gyroSensorManager: GyroSensorManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        hidManager = HidManager(this)

        gyroSensorManager = GyroSensorManager(this) { dx, dy ->
            if (dx != 0.toByte() || dy != 0.toByte()) {
                hidManager.sendMouseReport(dx, dy)
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            hidManager.performLeftClick()
            true
        } else {
            super.onKeyDown(keyCode, event)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        gyroSensorManager.unregister()
        hidManager.unregister()
    }
}
