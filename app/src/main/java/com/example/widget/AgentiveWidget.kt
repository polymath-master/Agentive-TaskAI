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

class AgentiveWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            WidgetContent(context)
        }
    }

    @Composable
    private fun WidgetContent(context: Context) {
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
                TaskInfoRow(title = "📰 News Brief", status = "Daily 11:00 AM • ACTIVE")
                TaskInfoRow(title = "📞 Missed Call", status = "Immediate callback • READY")
                TaskInfoRow(title = "✉️ Mass Email", status = "Spreading invitations • IDLE")
            }

            // Quick Actions segment
            Row(
                modifier = GlanceModifier.fillMaxWidth().padding(top = 4.dp),
                horizontalAlignment = Alignment.Horizontal.End
            ) {
                Button(
                    text = "Run News",
                    onClick = actionStartActivity<MainActivity>()
                )
                Spacer(modifier = GlanceModifier.width(8.dp))
                Button(
                    text = "Create Task +",
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
