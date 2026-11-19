package com.parksense.app

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.widget.*

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestAppPermissions()

        val prefs = getSharedPreferences("parksense", Context.MODE_PRIVATE)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 80, 48, 48)
            setBackgroundColor(Color.parseColor("#0F172A"))
        }

        val title = TextView(this).apply {
            text = "🅿️ ParkSense"
            textSize = 26f
            setTextColor(Color.WHITE)
        }

        val floorTv = TextView(this).apply {
            text = prefs.getString("floor", "주차 기록 없음")
            textSize = 44f
            setTextColor(Color.parseColor("#38BDF8"))
            setPadding(0, 32, 0, 8)
        }

        val infoTv = TextView(this).apply {
            val b = prefs.getString("building", "차량 블루투스 해제 시 자동 저장됩니다.")
            val t = prefs.getString("time", "")
            text = "$b\n$t"
            textSize = 15f
            setTextColor(Color.parseColor("#CBD5E1"))
            setPadding(0, 0, 0, 40)
        }

        val carInput = EditText(this).apply {
            hint = "차량 블루투스 이름 일부 (예: KIA, Hyundai)"
            setText(prefs.getString("car_bt_name", ""))
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
        }

        val saveBtBtn = Button(this).apply {
            text = "차량 블루투스 이름 저장"
            setOnClickListener {
                prefs.edit().putString("car_bt_name", carInput.text.toString().trim()).apply()
                Toast.makeText(this@MainActivity, "차량 블루투스 설정 완료", Toast.LENGTH_SHORT).show()
            }
        }

        val naverBtn = Button(this).apply {
            text = "네이버지도에서 주차 위치 열기"
            setOnClickListener {
                val building = prefs.getString("building", "") ?: ""
                val uri = Uri.parse("https://map.naver.com/p/search/${Uri.encode(building)}")
                startActivity(Intent(Intent.ACTION_VIEW, uri))
            }
        }

        val testStartBtn = Button(this).apply {
            text = "[테스트] 주행 시작 (기준 고도 설정)"
            setOnClickListener {
                startForegroundService(Intent(this@MainActivity, ParkingService::class.java).apply {
                    action = "START_DRIVING"
                })
                Toast.makeText(this@MainActivity, "기압계 측정 시작", Toast.LENGTH_SHORT).show()
            }
        }

        val testStopBtn = Button(this).apply {
            text = "[테스트] 주차 완료 및 위젯 갱신"
            setOnClickListener {
                startForegroundService(Intent(this@MainActivity, ParkingService::class.java).apply {
                    action = "STOP_DRIVING"
                })
                Toast.makeText(this@MainActivity, "주차 위치 저장 및 위젯 갱신됨", Toast.LENGTH_SHORT).show()
                recreate()
            }
        }

        layout.addView(title)
        layout.addView(floorTv)
        layout.addView(infoTv)
        layout.addView(carInput)
        layout.addView(saveBtBtn)
        layout.addView(naverBtn)
        layout.addView(testStartBtn)
        layout.addView(testStopBtn)
        setContentView(layout)
    }

    private fun requestAppPermissions() {
        val perms = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            perms.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        requestPermissions(perms.toTypedArray(), 100)
    }
}
