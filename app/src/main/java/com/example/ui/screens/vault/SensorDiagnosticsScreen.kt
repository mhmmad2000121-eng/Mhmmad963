package com.example.ui.screens.vault

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.sensor.SensorTriggerState
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentRed
import com.example.ui.theme.CyanPrimary
import java.util.Locale

@Composable
fun SensorDiagnosticsScreen(
    sensorState: SensorTriggerState,
    onSimulateTrigger: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Info Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = CyanPrimary.copy(alpha = 0.2f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Sensors,
                                contentDescription = null,
                                tint = CyanPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.sensor_status_title),
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.sensor_instructions),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
            }
        }

        // Live Telemetry Readings
        Text(
            text = "قراءات الحساسات المباشرة (Live Telemetry)",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = CyanPrimary
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Accelerometer Card
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.ScreenRotation,
                            contentDescription = null,
                            tint = CyanPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "التسارع (Accel)",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "X: ${String.format(Locale.US, "%.1f", sensorState.accelX)} m/s²",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Y: ${String.format(Locale.US, "%.1f", sensorState.accelY)} m/s²",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (sensorState.isUpsideDown) AccentGreen else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Z: ${String.format(Locale.US, "%.1f", sensorState.accelZ)} m/s²",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Proximity Card
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Sensors,
                            contentDescription = null,
                            tint = CyanPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "التقارب (Proximity)",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "${String.format(Locale.US, "%.1f", sensorState.proximity)} cm",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (sensorState.isProximityCovered) AccentGreen else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (sensorState.isProximityCovered) "مغطى (0 سم)" else "غير مغطى",
                        fontSize = 12.sp,
                        color = if (sensorState.isProximityCovered) AccentGreen else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Checklist of Trigger Conditions
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "شروط الفتح الفيزيائي المتزامنة:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Condition 1: Inverted
                ConditionRow(
                    title = "قلب الهاتف رأساً على عقب (Y < -8.0)",
                    isMet = sensorState.isUpsideDown
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Condition 2: Proximity Covered
                ConditionRow(
                    title = "تغطية حساس التقارب العلوي (0 سم)",
                    isMet = sensorState.isProximityCovered
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Continuous Hold Progress
                Text(
                    text = "الاستمرار لمدة ثانيتين: ${(sensorState.progress * 100).toInt()}%",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { sensorState.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (sensorState.progress >= 1f) AccentGreen else CyanPrimary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }

        // Test / Simulation Button
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = CyanPrimary.copy(alpha = 0.12f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Vibration,
                        contentDescription = null,
                        tint = CyanPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "اختبار الفتح والاهتزاز البرمجي",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "يتيح هذا الزر تجربة محاكاة حركة القلب وتغطية الحساس لثانيتين مع إطلاق الاهتزاز الخفيف فوراً للتأكد من سلاسة الانتقال.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onSimulateTrigger,
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("simulate_sensor_trigger_btn")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.test_trigger_simulation),
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun ConditionRow(
    title: String,
    isMet: Boolean
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = if (isMet) Icons.Default.CheckCircle else Icons.Default.RemoveCircle,
            contentDescription = null,
            tint = if (isMet) AccentGreen else AccentRed,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = title,
            fontSize = 13.sp,
            color = if (isMet) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
