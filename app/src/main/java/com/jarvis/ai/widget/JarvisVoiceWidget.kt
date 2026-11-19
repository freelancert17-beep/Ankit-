package com.jarvis.ai.widget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.jarvis.ai.MainActivity
import com.jarvis.ai.ui.theme.JarvisBackground
import com.jarvis.ai.ui.theme.JarvisCyan
import com.jarvis.ai.ui.theme.JarvisTextPrimary

class JarvisVoiceWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ColorProvider(JarvisBackground))
                    .padding(12.dp)
                    .clickable(actionStartActivity<MainActivity>()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "⚡ JARVIS 5.0",
                    style = TextStyle(
                        color = ColorProvider(JarvisCyan),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = GlanceModifier.height(6.dp))
                Text(
                    text = "Tap to Command",
                    style = TextStyle(
                        color = ColorProvider(JarvisTextPrimary),
                        fontSize = 12.sp
                    )
                )
            }
        }
    }
}
