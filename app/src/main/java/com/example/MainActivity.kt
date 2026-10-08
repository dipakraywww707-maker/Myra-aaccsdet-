package com.example

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.AppTab
import com.example.ui.MyraViewModel
import com.example.ui.screens.MyraAssistantScreen
import com.example.ui.screens.MyraCommandsHelpScreen
import com.example.ui.screens.MyraNotesScreen
import com.example.ui.screens.PhoneSystemHubScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.MyraBgDark
import com.example.ui.theme.MyraCyan
import com.example.ui.theme.MyraSurfaceDark
import com.example.ui.theme.MyraTextMuted
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val viewModel: MyraViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                MainContent(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainContent(viewModel: MyraViewModel) {
    val currentTab by viewModel.currentTab.collectAsState()

    // Speech-to-Text Recognition Launcher
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.setListeningState(false)
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenTextList = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = spokenTextList?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                viewModel.handleUserPrompt(spokenText)
            }
        }
    }

    // Permission launcher for Audio Record
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startSpeechInput(speechLauncher, viewModel)
        }
    }

    val onMicClick: () -> Unit = {
        permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
    }

    // Handle back button to return to Assistant screen if in sub-tabs
    BackHandler(enabled = currentTab != AppTab.ASSISTANT) {
        viewModel.setTab(AppTab.ASSISTANT)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = MyraSurfaceDark,
                contentColor = Color.White,
                tonalElevation = 8.dp,
                modifier = Modifier.testTag("main_navigation_bar")
            ) {
                NavigationBarItem(
                    selected = currentTab == AppTab.ASSISTANT,
                    onClick = { viewModel.setTab(AppTab.ASSISTANT) },
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "Assistant") },
                    label = { Text("Myra AI", fontWeight = if (currentTab == AppTab.ASSISTANT) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = MyraCyan,
                        indicatorColor = MyraCyan,
                        unselectedIconColor = MyraTextMuted,
                        unselectedTextColor = MyraTextMuted
                    ),
                    modifier = Modifier.testTag("nav_tab_assistant")
                )

                NavigationBarItem(
                    selected = currentTab == AppTab.SYSTEM_HUB,
                    onClick = { viewModel.setTab(AppTab.SYSTEM_HUB) },
                    icon = { Icon(Icons.Default.PhoneAndroid, contentDescription = "System Hub") },
                    label = { Text("Phone Hub", fontWeight = if (currentTab == AppTab.SYSTEM_HUB) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = MyraCyan,
                        indicatorColor = MyraCyan,
                        unselectedIconColor = MyraTextMuted,
                        unselectedTextColor = MyraTextMuted
                    ),
                    modifier = Modifier.testTag("nav_tab_system_hub")
                )

                NavigationBarItem(
                    selected = currentTab == AppTab.NOTES_TASKS,
                    onClick = { viewModel.setTab(AppTab.NOTES_TASKS) },
                    icon = { Icon(Icons.Default.Description, contentDescription = "Notes") },
                    label = { Text("Notes", fontWeight = if (currentTab == AppTab.NOTES_TASKS) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = MyraCyan,
                        indicatorColor = MyraCyan,
                        unselectedIconColor = MyraTextMuted,
                        unselectedTextColor = MyraTextMuted
                    ),
                    modifier = Modifier.testTag("nav_tab_notes")
                )

                NavigationBarItem(
                    selected = currentTab == AppTab.HELP_GUIDE,
                    onClick = { viewModel.setTab(AppTab.HELP_GUIDE) },
                    icon = { Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = "Commands") },
                    label = { Text("Commands", fontWeight = if (currentTab == AppTab.HELP_GUIDE) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = MyraCyan,
                        indicatorColor = MyraCyan,
                        unselectedIconColor = MyraTextMuted,
                        unselectedTextColor = MyraTextMuted
                    ),
                    modifier = Modifier.testTag("nav_tab_commands")
                )
            }
        }
    ) { innerPadding ->
        when (currentTab) {
            AppTab.ASSISTANT -> {
                MyraAssistantScreen(
                    viewModel = viewModel,
                    onStartVoiceInput = onMicClick,
                    modifier = Modifier.padding(innerPadding)
                )
            }

            AppTab.SYSTEM_HUB -> {
                PhoneSystemHubScreen(
                    viewModel = viewModel,
                    modifier = Modifier.padding(innerPadding)
                )
            }

            AppTab.NOTES_TASKS -> {
                MyraNotesScreen(
                    viewModel = viewModel,
                    modifier = Modifier.padding(innerPadding)
                )
            }

            AppTab.HELP_GUIDE -> {
                MyraCommandsHelpScreen(
                    viewModel = viewModel,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}

private fun startSpeechInput(
    launcher: androidx.activity.result.ActivityResultLauncher<Intent>,
    viewModel: MyraViewModel
) {
    try {
        viewModel.setListeningState(true)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Myra se boliye (Speak to Myra)...")
        }
        launcher.launch(intent)
    } catch (e: Exception) {
        viewModel.setListeningState(false)
    }
}
