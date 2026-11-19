package com.parksense.app

import android.annotation.SuppressLint
import android.app.*
import android.bluetooth.BluetoothDevice
import android.content.*
import android.hardware.*
import android.location.Geocoder
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

class BluetoothReceiver : BroadcastReceiver() {
    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        val prefs = context.getSharedPreferences("parksense", Context.MODE_PRIVATE)
        val targetCar = prefs.getString("car_bt_name", "") ?: ""

        // 차량 이름이 등록되어 있으면 해당 기기만 반응, 미등록 시 모든 BT에 반응
        val deviceName = try { device?.name ?: "" } catch (e: Exception) { "" }
        if (targetCar.isNotEmpty() && !deviceName.contains(targetCar, ignoreCase = true)) return

        val serviceIntent = Intent(context, ParkingService::class.java)
        when (intent.action) {
            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                serviceIntent.action = "START_DRIVING"
                context.startForegroundService(serviceIntent)
            }
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                serviceIntent.action = "STOP_DRIVING"
                context.startForegroundService(serviceIntent)
            }
        }
    }
}

class ParkingService : Service(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var basePressure: Float? = null
    private var currentPressure: Float? = null
    private var lastBuilding = "위치 확인 중"
    private var lastLat = 0.0
    private var lastLng = 0.0

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(1, buildNotification("ParkSense 주행 감지 중..."))

        when (intent?.action) {
            "START_DRIVING" -> {
                basePressure = null
                val pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)
                pressureSensor?.let {
                    sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
                }
                captureLocation()
            }
            "STOP_DRIVING" -> {
                saveParkingRecord()
                sensorManager.unregisterListener(this)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_PRESSURE) {
            val p = event.values[0]
            if (basePressure == null) basePressure = p
            currentPressure = p
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    @SuppressLint("MissingPermission")
    private fun captureLocation() {
        try {
            val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            if (loc != null) {
                lastLat = loc.latitude
                lastLng = loc.longitude
                val geocoder = Geocoder(this, Locale.KOREA)
                val addrs = geocoder.getFromLocation(lastLat, lastLng, 1)
                if (!addrs.isNullOrEmpty()) {
                    val a = addrs[0]
                    lastBuilding = a.featureName ?: a.thoroughfare ?: a.getAddressLine(0) ?: "알 수 없는 건물"
                }
            }
        } catch (_: Exception) {}
    }

    private fun saveParkingRecord() {
        val base = basePressure
        val curr = currentPressure
        val floorText = if (base != null && curr != null) {
            // 기압 1hPa 증가 ≈ 약 8.3m 하강, 층고 3.3m 기준
            val altDiff = SensorManager.getAltitude(base, curr) // m (음수면 지하)
            val floorNum = (altDiff / 3.3f).roundToInt()
            when {
                floorNum < 0 -> "B${-floorNum}F"
                floorNum == 0 -> "1F"
                else -> "${floorNum + 1}F"
            }
        } else {
            "1F (기압계 미감지)"
        }

        val timeStr = SimpleDateFormat("MM/dd HH:mm 저장", Locale.KOREA).format(Date())
        getSharedPreferences("parksense", Context.MODE_PRIVATE).edit()
            .putString("floor", floorText)
            .putString("building", lastBuilding)
            .putString("time", timeStr)
            .putString("lat", lastLat.toString())
            .putString("lng", lastLng.toString())
            .apply()

        ParkingWidgetProvider.updateAllWidgets(this)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel("parksense_ch", "주차 감지", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(text: String): Notification {
        return Notification.Builder(this, "parksense_ch")
            .setContentTitle("ParkSense")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
