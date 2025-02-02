package com.example.gyromouse

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import java.util.concurrent.Executors

@RequiresApi(Build.VERSION_CODES.S)
class HidManager(private val context: Context) {

    private var connectedDevice: BluetoothDevice? = null
    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter = bluetoothManager.adapter
    private lateinit var hidDevice: BluetoothHidDevice

    init {
        initHidProfile()
    }

    private fun initHidProfile() {
        bluetoothAdapter.getProfileProxy(
            context,
            object : BluetoothProfile.ServiceListener {
                override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
                    if (profile == BluetoothProfile.HID_DEVICE) {
                        hidDevice = proxy as BluetoothHidDevice
                        registerHidDevice()
                        Log.d("HID", "HID profile initialized")
                        connectToPairedDevice()
                    }
                }

                override fun onServiceDisconnected(profile: Int) {
                    Log.w("HID", "HID profile disconnected")
                }
            },
            BluetoothProfile.HID_DEVICE
        )
    }

    private fun registerHidDevice() {
        val sdpSettings = BluetoothHidDeviceAppSdpSettings(
            "Gyro Mouse", "Bluetooth Gyro Controller", "AnyNameForProvider",
            0x80.toByte(),
            byteArrayOf(
                0x05, 0x01, 0x09, 0x02, 0xA1.toByte(), 0x01,
                0x09, 0x01, 0xA1.toByte(), 0x00, 0x05, 0x09,
                0x19, 0x01, 0x29, 0x03, 0x15, 0x00, 0x25, 0x01,
                0x95.toByte(), 0x03, 0x75, 0x01, 0x81.toByte(), 0x02,
                0x95.toByte(), 0x01, 0x75, 0x05, 0x81.toByte(), 0x03,
                0x05, 0x01, 0x09, 0x30, 0x09, 0x31, 0x15, 0x81.toByte(),
                0x25, 0x7F, 0x75, 0x08, 0x95.toByte(), 0x02, 0x81.toByte(), 0x06,
                0xC0.toByte(), 0xC0.toByte()
            )
        )

        hidDevice.registerApp(
            sdpSettings,
            null,
            null,
            Executors.newSingleThreadExecutor(),
            object : BluetoothHidDevice.Callback() {
                override fun onConnectionStateChanged(device: BluetoothDevice, state: Int) {
                    when (state) {
                        BluetoothProfile.STATE_CONNECTED -> {
                            connectedDevice = device
                            showToast("Connected to ${device.name}")
                        }
                        BluetoothProfile.STATE_DISCONNECTED -> {
                            connectedDevice = null
                            showToast("Disconnected")
                        }
                    }
                }
            }
        )
    }

    private fun connectToPairedDevice() {
        val device = bluetoothAdapter.bondedDevices.firstOrNull()
        if (device != null) {
            Log.d("HID", "Connecting to ${device.name}")
            try {
                hidDevice.connect(device)
                Handler(Looper.getMainLooper()).postDelayed({
                    connectedDevice?.let {
                        Log.i("HID", "Connected with ${it.name}")
                    }
                }, 3000)
            } catch (e: Exception) {
                Log.e("HID", "Connection error", e)
            }
        } else {
            Log.w("HID", "No paired devices found")
            showToast("No paired devices")
        }
    }

    fun sendMouseReport(dx: Byte, dy: Byte) {
        if (::hidDevice.isInitialized && connectedDevice != null) {
            val buttons: Byte = 0x00 // Stan przycisków (przy kliknięciu ustawiany osobno)
            try {
                hidDevice.sendReport(connectedDevice, 0x00, byteArrayOf(buttons, dx, dy))
            } catch (e: Exception) {
                Log.e("HID", "Error sending report", e)
            }
        }
    }

    fun performLeftClick() {
        if (::hidDevice.isInitialized && connectedDevice != null) {
            try {
                // Symulacja naciśnięcia lewego przycisku
                hidDevice.sendReport(connectedDevice, 0x00, byteArrayOf(LEFT_BUTTON, 0, 0))
                // Zwolnienie przycisku po krótkim opóźnieniu
                Handler(Looper.getMainLooper()).postDelayed({
                    hidDevice.sendReport(connectedDevice, 0x00, byteArrayOf(0, 0, 0))
                }, 50)
            } catch (e: Exception) {
                Log.e("HID", "Error performing left click", e)
            }
        }
    }

    fun unregister() {
        hidDevice.unregisterApp()
    }

    private fun showToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private val LEFT_BUTTON: Byte = 0x01
    }
}
