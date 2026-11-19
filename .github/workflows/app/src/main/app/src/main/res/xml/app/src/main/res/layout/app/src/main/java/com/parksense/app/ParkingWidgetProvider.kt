package com.parksense.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class ParkingWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAllWidgets(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, ParkingWidgetProvider::class.java))
            for (id in ids) {
                updateWidget(context, manager, id)
            }
        }

        private fun updateWidget(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val prefs = context.getSharedPreferences("parksense", Context.MODE_PRIVATE)
            val floor = prefs.getString("floor", "기록 없음") ?: "기록 없음"
            val building = prefs.getString("building", "차량 BT 해제 시 자동 저장") ?: ""
            val time = prefs.getString("time", "") ?: ""

            val views = RemoteViews(context.packageName, R.layout.widget_parking)
            views.setTextViewText(R.id.tv_widget_floor, floor)
            views.setTextViewText(R.id.tv_widget_building, building)
            views.setTextViewText(R.id.tv_widget_time, time)

            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
            manager.updateAppWidget(widgetId, views)
        }
    }
}
