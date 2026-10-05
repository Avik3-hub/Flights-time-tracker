package com.example.flightlog.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Bitmap
import android.graphics.Canvas
import android.widget.RemoteViews
import com.example.flightlog.MainActivity
import com.example.flightlog.R
import com.example.flightlog.ui.theme.AppTheme

class FlightWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { manager.updateAppWidget(it, views(context)) }
    }

    companion object {
        const val ACTION_ADD_FLIGHT = "com.example.flightlog.ADD_FLIGHT"

        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, FlightWidgetProvider::class.java))
            ids.forEach { manager.updateAppWidget(it, views(context)) }
        }

        private fun views(context: Context): RemoteViews {
            val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            val dark = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
            val theme = AppTheme.fromPreferences(prefs.getString("theme_mode", null), prefs.getBoolean("is_dark_theme", false)).resolve(dark)
            val light = theme == AppTheme.BLUE
            val accent = Color.parseColor(if (light) "#1069A6" else "#D8A64F")
            val text = Color.parseColor(if (light) "#173650" else "#C1C0BA")
            val backgroundResource = when (theme) {
                AppTheme.BLUE -> R.drawable.widget_background_light
                AppTheme.AMOLED -> R.drawable.widget_background_dark
                else -> R.drawable.widget_background
            }
            val bitmap = Bitmap.createBitmap(600, 240, Bitmap.Config.ARGB_8888)
            context.getDrawable(backgroundResource)?.mutate()?.apply {
                setBounds(0, 0, bitmap.width, bitmap.height)
                alpha = ((100 - prefs.getInt("widget_transparency", 0).coerceIn(0, 100)) * 255 / 100)
                draw(Canvas(bitmap))
            }
            val intent = Intent(context, MainActivity::class.java).apply {
                action = ACTION_ADD_FLIGHT
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val pending = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            return RemoteViews(context.packageName, R.layout.flight_widget).apply {
                setImageViewBitmap(R.id.widget_background_image, bitmap)
                setInt(R.id.widget_add_flight, "setBackgroundResource", if (light) R.drawable.widget_button_light else R.drawable.widget_button)
                setTextColor(R.id.widget_title, text)
                setTextColor(R.id.widget_add_flight, accent)
                setOnClickPendingIntent(R.id.widget_root, pending)
                setOnClickPendingIntent(R.id.widget_add_flight, pending)
            }
        }
    }
}
