package com.example.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.*
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.MainActivity
import com.example.core.storage.PreferencesManager
import com.example.core.storage.AppDatabase
import kotlinx.coroutines.flow.first
import androidx.glance.appwidget.updateAll

suspend fun updateWidget(context: Context) {
    try {
        AgentiveWidget().updateAll(context)
    } catch (e: Exception) {
        android.util.Log.e("AgentiveWidget", "Failed to update widget: ${e.message}")
    }
}

class AgentiveWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = PreferencesManager(context)
        val newsEnabled = prefs.isTaskEnabledFlow("news").first()
        val callReminderEnabled = prefs.isTaskEnabledFlow("callreminder").first()
        val massEmailEnabled = prefs.isTaskEnabledFlow("massemail").first()
        val whatsappEnabled = prefs.isTaskEnabledFlow("whatsapp").first()

        val db = AppDatabase.getDatabase(context)
        val customCount = try {
            db.taskDao().getAllUserTasks().size
        } catch (e: Exception) {
            0
        }

        provideContent {
            WidgetContent(
                context,
                newsEnabled,
                callReminderEnabled,
                massEmailEnabled,
                whatsappEnabled,
                customCount
            )
        }
    }

    @Composable
    private fun WidgetContent(
        context: Context,
        newsEnabled: Boolean,
        callReminderEnabled: Boolean,
        massEmailEnabled: Boolean,
        whatsappEnabled: Boolean,
        customCount: Int
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(12.dp)
                .background(ImageProvider(android.R.drawable.dialog_holo_dark_frame)) // Retro dark glass container
        ) {
            // Header Row
            Row(
                modifier = GlanceModifier.fillMaxWidth().padding(bottom = 6.dp),
                horizontalAlignment = Alignment.Horizontal.CenterHorizontally
            ) {
                Text(
                    text = "🤖 Agentive TaskAI",
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        color = ColorProvider(Color.White)
                    )
                )
            }

            // Task list summaries
            Column(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                TaskInfoRow(title = "📰 News Brief", status = if (newsEnabled) "ACTIVE (11 AM)" else "HALTED")
                TaskInfoRow(title = "📞 Missed Call", status = if (callReminderEnabled) "ACTIVE (Call)" else "HALTED")
                TaskInfoRow(title = "✉️ Mass Email", status = if (massEmailEnabled) "ACTIVE (Email)" else "HALTED")
                TaskInfoRow(title = "💬 WhatsApp", status = if (whatsappEnabled) "ACTIVE (AI)" else "HALTED")
                TaskInfoRow(title = "🔮 Custom Tasks", status = "$customCount schemas active")
            }

            // Quick Actions segment
            Row(
                modifier = GlanceModifier.fillMaxWidth().padding(top = 4.dp),
                horizontalAlignment = Alignment.Horizontal.End
            ) {
                Button(
                    text = "Open Dashboard",
                    onClick = actionStartActivity<MainActivity>()
                )
            }
        }
    }

    @Composable
    private fun TaskInfoRow(title: String, status: String) {
        Row(
            modifier = GlanceModifier.fillMaxWidth().padding(vertical = 2.dp),
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            Text(
                text = "$title: ",
                style = TextStyle(
                    fontWeight = FontWeight.Medium,
                    color = ColorProvider(Color.LightGray)
                )
            )
            Text(
                text = status,
                style = TextStyle(
                    color = ColorProvider(Color.White)
                )
            )
        }
    }
}
