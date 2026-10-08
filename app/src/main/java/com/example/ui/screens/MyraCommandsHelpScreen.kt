package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MyraViewModel
import com.example.ui.theme.MyraBgDark
import com.example.ui.theme.MyraCyan
import com.example.ui.theme.MyraSurfaceElevated
import com.example.ui.theme.MyraTextMuted
import com.example.ui.theme.MyraTextSecondary
import com.example.ui.theme.MyraViolet

data class CommandCategory(
    val title: String,
    val commands: List<Pair<String, String>> // (Command text, Description)
)

@Composable
fun MyraCommandsHelpScreen(
    viewModel: MyraViewModel,
    modifier: Modifier = Modifier
) {
    val categories = listOf(
        CommandCategory(
            title = "🔦 Flashlight / Torch",
            commands = listOf(
                "Torch chalu karo" to "Turns on phone flashlight",
                "Torch band karo" to "Turns off phone flashlight",
                "Light on / Flashlight off" to "Toggles hardware torch"
            )
        ),
        CommandCategory(
            title = "🔋 Battery & Power",
            commands = listOf(
                "Battery kitna hai?" to "Checks live battery % and charging state",
                "Charge kitna bacha hai" to "Speaks battery telemetry & temp"
            )
        ),
        CommandCategory(
            title = "🔊 Volume & Ringer",
            commands = listOf(
                "Volume 50% karo" to "Sets media sound level to half",
                "Volume full karo" to "Sets media volume to 100% max",
                "Mute karo / Awaz band karo" to "Mutes media playback",
                "Silent mode lagao" to "Puts phone in Silent ringer mode",
                "Vibrate mode par switch karo" to "Switches phone to Vibrate mode",
                "Normal mode lagao" to "Turns on normal ringtone mode"
            )
        ),
        CommandCategory(
            title = "📲 App Launchers",
            commands = listOf(
                "YouTube kholo" to "Launches YouTube app or web",
                "WhatsApp kholo" to "Opens WhatsApp instantly",
                "Camera chalu karo" to "Opens phone camera app",
                "Phone lagao / Dialer kholo" to "Opens phone dialer",
                "Google Chrome kholo" to "Launches web browser",
                "Maps kholo" to "Opens Google Maps navigation"
            )
        ),
        CommandCategory(
            title = "⚙️ System Settings",
            commands = listOf(
                "Wi-Fi settings kholo" to "Opens Wi-Fi management screen",
                "Bluetooth settings open karo" to "Opens Bluetooth settings screen",
                "Display settings kholo" to "Opens brightness and screen settings",
                "Sound settings kholo" to "Opens audio configuration",
                "Phone specs batao" to "Displays RAM, OS version & specs"
            )
        ),
        CommandCategory(
            title = "📝 Notes & Smart Q&A",
            commands = listOf(
                "Note: Subah 7 baje call karna" to "Saves note directly to local database",
                "Kaise ho Myra?" to "Conversational AI chat in Hinglish",
                "Tum kya kya kar sakti ho?" to "Explains all abilities and guides you"
            )
        )
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MyraBgDark)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "MYRA VOICE & TEXT COMMANDS",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MyraCyan,
                    letterSpacing = 1.2.sp
                )
            )
            Text(
                text = "Tap any command to run it instantly or speak to Myra!",
                style = MaterialTheme.typography.bodySmall.copy(color = MyraTextSecondary)
            )
        }

        items(categories) { cat ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MyraSurfaceElevated),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = cat.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MyraCyan
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    cat.commands.forEach { (cmd, desc) ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    viewModel.setTab(com.example.ui.AppTab.ASSISTANT)
                                    viewModel.handleUserPrompt(cmd)
                                }
                                .testTag("help_cmd_${cmd.take(8)}"),
                            color = Color(0xFF13192A)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "\"$cmd\"",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Medium,
                                            color = Color.White
                                        )
                                    )
                                    Text(
                                        text = desc,
                                        style = MaterialTheme.typography.bodySmall.copy(color = MyraTextMuted)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MyraViolet.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "RUN ❯",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MyraViolet
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
