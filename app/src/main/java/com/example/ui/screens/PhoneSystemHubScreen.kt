package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DisplaySettings
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MyraViewModel
import com.example.ui.components.AppLauncherTile
import com.example.ui.components.BatteryCard
import com.example.ui.components.DeviceTelemetryCard
import com.example.ui.components.TorchToggleCard
import com.example.ui.components.VolumeMasterCard
import com.example.ui.theme.MyraAmber
import com.example.ui.theme.MyraBgDark
import com.example.ui.theme.MyraBlue
import com.example.ui.theme.MyraCyan
import com.example.ui.theme.MyraEmerald
import com.example.ui.theme.MyraRose
import com.example.ui.theme.MyraTextSecondary
import com.example.ui.theme.MyraViolet

@Composable
fun PhoneSystemHubScreen(
    viewModel: MyraViewModel,
    modifier: Modifier = Modifier
) {
    val battery by viewModel.batteryState.collectAsState()
    val isTorchOn by viewModel.isTorchOn.collectAsState()
    val volumeInfo by viewModel.volumeInfo.collectAsState()
    val telemetry by viewModel.telemetry.collectAsState()
    val networkStatus by viewModel.networkStatus.collectAsState()

    val sysMgr = viewModel.phoneSystemManager

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MyraBgDark)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hub Header
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "PHONE SYSTEM HUB",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MyraCyan,
                            letterSpacing = 1.2.sp
                        )
                    )
                    Text(
                        text = "Hardware, Connectivity & App Launchers",
                        style = MaterialTheme.typography.bodySmall.copy(color = MyraTextSecondary)
                    )
                }

                IconButton(
                    onClick = { viewModel.refreshSystemTelemetry() },
                    modifier = Modifier.testTag("refresh_system_hub_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = MyraCyan
                    )
                }
            }
        }

        // 1. Flashlight / Torch Card
        item {
            TorchToggleCard(
                isTorchOn = isTorchOn,
                onToggle = { viewModel.toggleTorch() }
            )
        }

        // 2. Battery Telemetry Card
        item {
            BatteryCard(
                battery = battery,
                onRefresh = { viewModel.refreshSystemTelemetry() }
            )
        }

        // 3. Audio & Volume Master Controller
        item {
            VolumeMasterCard(
                volumeInfo = volumeInfo,
                onSetMediaVolume = { viewModel.setMediaVolume(it) },
                onSetRingerMode = { viewModel.setRingerMode(it) }
            )
        }

        // 4. Quick App Launchers Grid
        item {
            Text(
                text = "Quick App Launchers",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AppLauncherTile(
                        name = "YouTube",
                        icon = Icons.Default.PlayArrow,
                        color = Color(0xFFFF0000),
                        onClick = { sysMgr.openYouTube() },
                        modifier = Modifier.weight(1f)
                    )
                    AppLauncherTile(
                        name = "WhatsApp",
                        icon = Icons.AutoMirrored.Filled.Chat,
                        color = Color(0xFF25D366),
                        onClick = { sysMgr.openWhatsApp() },
                        modifier = Modifier.weight(1f)
                    )
                    AppLauncherTile(
                        name = "Camera",
                        icon = Icons.Default.CameraAlt,
                        color = MyraCyan,
                        onClick = { sysMgr.openCamera() },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AppLauncherTile(
                        name = "Phone Call",
                        icon = Icons.Default.Phone,
                        color = MyraEmerald,
                        onClick = { sysMgr.openDialer() },
                        modifier = Modifier.weight(1f)
                    )
                    AppLauncherTile(
                        name = "Browser",
                        icon = Icons.Default.Language,
                        color = MyraBlue,
                        onClick = { sysMgr.openChrome() },
                        modifier = Modifier.weight(1f)
                    )
                    AppLauncherTile(
                        name = "Maps",
                        icon = Icons.Default.Map,
                        color = MyraAmber,
                        onClick = { sysMgr.openMaps() },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AppLauncherTile(
                        name = "Settings",
                        icon = Icons.Default.Settings,
                        color = MyraTextSecondary,
                        onClick = { sysMgr.openGeneralSettings() },
                        modifier = Modifier.weight(1f)
                    )
                    AppLauncherTile(
                        name = "Timer",
                        icon = Icons.Default.Alarm,
                        color = MyraViolet,
                        onClick = { sysMgr.setTimer(300, "Myra Quick Timer") },
                        modifier = Modifier.weight(1f)
                    )
                    AppLauncherTile(
                        name = "Calculator",
                        icon = Icons.Default.Calculate,
                        color = MyraRose,
                        onClick = { sysMgr.launchAppByPackage("com.google.android.calculator") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 5. Connectivity & System Settings Direct Shortcuts
        item {
            Text(
                text = "System Settings Shortcuts",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AppLauncherTile(
                    name = "Wi-Fi (${networkStatus.connectionType})",
                    icon = Icons.Default.Wifi,
                    color = MyraCyan,
                    onClick = { sysMgr.openWifiSettings() },
                    modifier = Modifier.weight(1f)
                )
                AppLauncherTile(
                    name = "Bluetooth",
                    icon = Icons.Default.Bluetooth,
                    color = MyraBlue,
                    onClick = { sysMgr.openBluetoothSettings() },
                    modifier = Modifier.weight(1f)
                )
                AppLauncherTile(
                    name = "Display",
                    icon = Icons.Default.DisplaySettings,
                    color = MyraAmber,
                    onClick = { sysMgr.openDisplaySettings() },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 6. Device Hardware & Telemetry Specs
        item {
            DeviceTelemetryCard(telemetry = telemetry)
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
