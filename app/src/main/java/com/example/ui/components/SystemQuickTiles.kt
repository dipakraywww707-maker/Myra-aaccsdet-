package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DisplaySettings
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.system.BatteryInfo
import com.example.data.system.DeviceTelemetry
import com.example.data.system.VolumeInfo
import com.example.ui.theme.MyraAmber
import com.example.ui.theme.MyraCyan
import com.example.ui.theme.MyraEmerald
import com.example.ui.theme.MyraRose
import com.example.ui.theme.MyraSurfaceElevated
import com.example.ui.theme.MyraTextMuted
import com.example.ui.theme.MyraTextSecondary
import com.example.ui.theme.MyraViolet

@Composable
fun BatteryCard(
    battery: BatteryInfo,
    onRefresh: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("battery_card")
            .clickable(onClick = onRefresh),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MyraSurfaceElevated),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                if (battery.isCharging) MyraEmerald.copy(alpha = 0.15f)
                                else if (battery.level <= 20) MyraRose.copy(alpha = 0.15f)
                                else MyraCyan.copy(alpha = 0.15f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (battery.isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryFull,
                            contentDescription = "Battery Status",
                            tint = if (battery.isCharging) MyraEmerald else if (battery.level <= 20) MyraRose else MyraCyan,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Battery Status",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = if (battery.isCharging) "Charging (${battery.chargingSource})" else "On Battery",
                            style = MaterialTheme.typography.bodySmall.copy(color = MyraTextSecondary)
                        )
                    }
                }
                Text(
                    text = "${battery.level}%",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (battery.level <= 20) MyraRose else MyraCyan
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            LinearProgressIndicator(
                progress = { (battery.level / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (battery.isCharging) MyraEmerald else if (battery.level <= 20) MyraRose else MyraCyan,
                trackColor = Color(0xFF1E293B)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Temp: ${"%.1f".format(battery.temperatureCelsius)}°C",
                    style = MaterialTheme.typography.bodySmall.copy(color = MyraTextMuted)
                )
                Text(
                    text = if (battery.isPowerSaveMode) "⚡ Battery Saver Active" else "Standard Mode",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (battery.isPowerSaveMode) MyraAmber else MyraTextMuted
                    )
                )
            }
        }
    }
}

@Composable
fun TorchToggleCard(
    isTorchOn: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .testTag("torch_toggle_card")
            .clickable(onClick = onToggle),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isTorchOn) MyraAmber.copy(alpha = 0.18f) else MyraSurfaceElevated
        ),
        border = BorderStroke(
            1.dp,
            if (isTorchOn) MyraAmber else MaterialTheme.colorScheme.outline
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(if (isTorchOn) MyraAmber else Color(0xFF26334D)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isTorchOn) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                        contentDescription = "Flashlight",
                        tint = if (isTorchOn) Color.Black else MyraTextSecondary,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "Flashlight / Torch",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = if (isTorchOn) "Torch is currently ON" else "Torch is turned OFF",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (isTorchOn) MyraAmber else MyraTextSecondary
                        )
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isTorchOn) MyraAmber else Color(0xFF1E283E)
            ) {
                Text(
                    text = if (isTorchOn) "ON" else "OFF",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isTorchOn) Color.Black else MyraTextSecondary
                    )
                )
            }
        }
    }
}

@Composable
fun VolumeMasterCard(
    volumeInfo: VolumeInfo,
    onSetMediaVolume: (Int) -> Unit,
    onSetRingerMode: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("volume_master_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MyraSurfaceElevated),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Audio Volume",
                        tint = MyraCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Audio & Sound Control",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
                Text(
                    text = "${volumeInfo.mediaVolumePercent}%",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MyraCyan
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Media Volume",
                style = MaterialTheme.typography.bodySmall.copy(color = MyraTextSecondary)
            )

            Slider(
                value = volumeInfo.mediaVolumePercent.toFloat(),
                onValueChange = { onSetMediaVolume(it.toInt()) },
                valueRange = 0f..100f,
                colors = SliderDefaults.colors(
                    thumbColor = MyraCyan,
                    activeTrackColor = MyraCyan,
                    inactiveTrackColor = Color(0xFF1E293B)
                ),
                modifier = Modifier.testTag("media_volume_slider")
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Ringer Mode Selectors
            Text(
                text = "Ringer Mode",
                style = MaterialTheme.typography.bodySmall.copy(color = MyraTextSecondary)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    Triple("Normal", "Normal 🔔", Icons.AutoMirrored.Filled.VolumeUp),
                    Triple("Vibrate", "Vibrate 📳", Icons.AutoMirrored.Filled.VolumeDown),
                    Triple("Silent", "Silent 🔕", Icons.AutoMirrored.Filled.VolumeMute)
                ).forEach { (modeKey, label, _) ->
                    val isSelected = volumeInfo.ringerMode.equals(modeKey, ignoreCase = true)
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onSetRingerMode(modeKey) },
                        color = if (isSelected) MyraViolet else Color(0xFF192237),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) MyraViolet else Color(0xFF2B3A5A)
                        )
                    ) {
                        Text(
                            text = label,
                            modifier = Modifier.padding(vertical = 10.dp),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else MyraTextSecondary
                            ),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AppLauncherTile(
    name: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("app_launcher_${name.lowercase()}"),
        color = MyraSurfaceElevated,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = name,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp
                ),
                maxLines = 1
            )
        }
    }
}

@Composable
fun DeviceTelemetryCard(
    telemetry: DeviceTelemetry,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("device_telemetry_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MyraSurfaceElevated),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Phone Specifications & Hardware",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(text = "Device Model", style = MaterialTheme.typography.bodySmall.copy(color = MyraTextMuted))
                    Text(text = "${telemetry.manufacturer} ${telemetry.deviceModel}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "Android OS", style = MaterialTheme.typography.bodySmall.copy(color = MyraTextMuted))
                    Text(text = "Android ${telemetry.androidVersion} (API ${telemetry.sdkInt})", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium, color = MyraCyan))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(text = "RAM (Total / Available)", style = MaterialTheme.typography.bodySmall.copy(color = MyraTextMuted))
                    Text(text = "${telemetry.totalRamMb} MB / ${telemetry.availableRamMb} MB Free", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium, color = MyraEmerald))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "System Uptime", style = MaterialTheme.typography.bodySmall.copy(color = MyraTextMuted))
                    Text(text = "${"%.1f".format(telemetry.uptimeHours)} hrs", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                }
            }
        }
    }
}
