package com.example.dailyflow.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.widget.RemoteViews
import com.example.dailyflow.R
import com.example.dailyflow.data.Storage

class HabitWidgetProvider: AppWidgetProvider() {
    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { id ->
            val views = RemoteViews(ctx.packageName, R.layout.widget_habit)
            val pct = Storage.todayPercent(ctx)
            views.setTextViewText(R.id.tvWidgetPercent, "$pct%")
            mgr.updateAppWidget(id, views)
        }
    }
    companion object {
        fun requestUpdate(ctx: Context) {
            val mgr = AppWidgetManager.getInstance(ctx)
            val ids = mgr.getAppWidgetIds(ComponentName(ctx, HabitWidgetProvider::class.java))
            if (ids.isNotEmpty()) AppWidgetProvider().onUpdate(ctx, mgr, ids)
        }
    }
}
