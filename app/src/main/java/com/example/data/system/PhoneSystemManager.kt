package com.example.data.system

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.AlarmClock
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

data class BatteryInfo(
    val level: Int = 100,
    val isCharging: Boolean = false,
    val chargingSource: String = "Not Charging",
    val temperatureCelsius: Float = 28.0f,
    val isPowerSaveMode: Boolean = false
)

data class VolumeInfo(
    val mediaVolumePercent: Int = 50,
    val ringVolumePercent: Int = 70,
    val notificationVolumePercent: Int = 70,
    val ringerMode: String = "Normal" // Normal, Vibrate, Silent
)

data class DeviceTelemetry(
    val deviceModel: String,
    val manufacturer: String,
    val androidVersion: String,
    val sdkInt: Int,
    val totalRamMb: Long,
    val availableRamMb: Long,
    val uptimeHours: Float
)

data class NetworkStatus(
    val isConnected: Boolean,
    val connectionType: String // "Wi-Fi", "Cellular", "Ethernet", "Disconnected"
)

sealed class SystemCommandResult {
    data class Handled(val message: String, val actionTag: String? = null) : SystemCommandResult()
    data class FallbackToAI(val originalQuery: String) : SystemCommandResult()
}

class PhoneSystemManager(private val context: Context) {

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private val _isTorchOn = MutableStateFlow(false)
    val isTorchOn: StateFlow<Boolean> = _isTorchOn.asStateFlow()

    private val _batteryState = MutableStateFlow(getBatteryInfo())
    val batteryState: StateFlow<BatteryInfo> = _batteryState.asStateFlow()

    private var cameraIdWithFlash: String? = null

    init {
        initCamera()
        refreshBatteryInfo()
    }

    private fun initCamera() {
        try {
            cameraManager?.let { cm ->
                for (id in cm.cameraIdList) {
                    val characteristics = cm.getCameraCharacteristics(id)
                    val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                    val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                    if (hasFlash && facing == CameraCharacteristics.LENS_FACING_BACK) {
                        cameraIdWithFlash = id
                        break
                    }
                }
                if (cameraIdWithFlash == null && cm.cameraIdList.isNotEmpty()) {
                    cameraIdWithFlash = cm.cameraIdList[0]
                }
            }
        } catch (e: Exception) {
            Log.e("PhoneSystemManager", "Failed to init camera flash", e)
        }
    }

    fun vibrateShort() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(50)
            }
        } catch (e: Exception) {
            Log.e("PhoneSystemManager", "Vibrate failed", e)
        }
    }

    // --- Flashlight Control ---
    fun toggleTorch(): Boolean {
        return setTorch(!_isTorchOn.value)
    }

    fun setTorch(enabled: Boolean): Boolean {
        val cid = cameraIdWithFlash ?: return false
        return try {
            cameraManager?.setTorchMode(cid, enabled)
            _isTorchOn.value = enabled
            vibrateShort()
            true
        } catch (e: CameraAccessException) {
            Log.e("PhoneSystemManager", "Error toggling torch", e)
            false
        } catch (e: Exception) {
            Log.e("PhoneSystemManager", "Torch exception", e)
            false
        }
    }

    // --- Battery Manager ---
    fun refreshBatteryInfo(): BatteryInfo {
        val info = getBatteryInfo()
        _batteryState.value = info
        return info
    }

    fun getBatteryInfo(): BatteryInfo {
        val iFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = context.registerReceiver(null, iFilter)

        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 100
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: 100
        val batteryPct = if (scale > 0 && level >= 0) ((level / scale.toFloat()) * 100).toInt() else 100

        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        val chargePlug = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        val source = when (chargePlug) {
            BatteryManager.BATTERY_PLUGGED_USB -> "USB Cable"
            BatteryManager.BATTERY_PLUGGED_AC -> "AC Fast Charger"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless Charger"
            else -> if (isCharging) "Charging" else "Unplugged"
        }

        val tempTenths = batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 280) ?: 280
        val tempCelsius = tempTenths / 10.0f

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val isPowerSave = powerManager?.isPowerSaveMode ?: false

        return BatteryInfo(
            level = batteryPct,
            isCharging = isCharging,
            chargingSource = source,
            temperatureCelsius = tempCelsius,
            isPowerSaveMode = isPowerSave
        )
    }

    // --- Audio Volume Manager ---
    fun getVolumeInfo(): VolumeInfo {
        val am = audioManager ?: return VolumeInfo()
        val mediaMax = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val mediaCurr = am.getStreamVolume(AudioManager.STREAM_MUSIC)
        val mediaPct = (mediaCurr * 100) / mediaMax

        val ringMax = am.getStreamMaxVolume(AudioManager.STREAM_RING).coerceAtLeast(1)
        val ringCurr = am.getStreamVolume(AudioManager.STREAM_RING)
        val ringPct = (ringCurr * 100) / ringMax

        val notifMax = am.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION).coerceAtLeast(1)
        val notifCurr = am.getStreamVolume(AudioManager.STREAM_NOTIFICATION)
        val notifPct = (notifCurr * 100) / notifMax

        val ringerMode = when (am.ringerMode) {
            AudioManager.RINGER_MODE_SILENT -> "Silent"
            AudioManager.RINGER_MODE_VIBRATE -> "Vibrate"
            else -> "Normal"
        }

        return VolumeInfo(
            mediaVolumePercent = mediaPct,
            ringVolumePercent = ringPct,
            notificationVolumePercent = notifPct,
            ringerMode = ringerMode
        )
    }

    fun setMediaVolumePercent(percent: Int): Boolean {
        val am = audioManager ?: return false
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val target = ((percent.coerceIn(0, 100) / 100.0) * max).toInt()
        am.setStreamVolume(AudioManager.STREAM_MUSIC, target, AudioManager.FLAG_SHOW_UI)
        vibrateShort()
        return true
    }

    fun setRingerMode(mode: Int): Boolean {
        val am = audioManager ?: return false
        return try {
            am.ringerMode = mode
            vibrateShort()
            true
        } catch (e: Exception) {
            Log.e("PhoneSystemManager", "Failed to set ringer mode", e)
            false
        }
    }

    // --- Network & Connectivity ---
    fun getNetworkStatus(): NetworkStatus {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return NetworkStatus(false, "Disconnected")

        val activeNetwork = cm.activeNetwork ?: return NetworkStatus(false, "Disconnected")
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return NetworkStatus(false, "Disconnected")

        val isConnected = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        val type = when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular 4G/5G"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            else -> "Connected"
        }

        return NetworkStatus(isConnected, type)
    }

    // --- Device Telemetry ---
    fun getDeviceTelemetry(): DeviceTelemetry {
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager?.getMemoryInfo(memInfo)

        val totalRamMb = memInfo.totalMem / (1024 * 1024)
        val availRamMb = memInfo.availMem / (1024 * 1024)

        val uptimeHours = SystemClock.elapsedRealtime() / (1000f * 60f * 60f)

        return DeviceTelemetry(
            deviceModel = Build.MODEL,
            manufacturer = Build.MANUFACTURER.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() },
            androidVersion = Build.VERSION.RELEASE,
            sdkInt = Build.VERSION.SDK_INT,
            totalRamMb = totalRamMb,
            availableRamMb = availRamMb,
            uptimeHours = uptimeHours
        )
    }

    // --- System Settings Launchers ---
    fun openSettings(action: String): Boolean {
        return try {
            val intent = Intent(action).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            vibrateShort()
            true
        } catch (e: Exception) {
            Log.e("PhoneSystemManager", "Failed to open settings: $action", e)
            false
        }
    }

    fun openWifiSettings() = openSettings(Settings.ACTION_WIFI_SETTINGS)
    fun openBluetoothSettings() = openSettings(Settings.ACTION_BLUETOOTH_SETTINGS)
    fun openDisplaySettings() = openSettings(Settings.ACTION_DISPLAY_SETTINGS)
    fun openSoundSettings() = openSettings(Settings.ACTION_SOUND_SETTINGS)
    fun openBatterySettings() = openSettings(Settings.ACTION_BATTERY_SAVER_SETTINGS)
    fun openGeneralSettings() = openSettings(Settings.ACTION_SETTINGS)
    fun openAirplaneModeSettings() = openSettings(Settings.ACTION_AIRPLANE_MODE_SETTINGS)

    // --- App Launchers ---
    fun launchAppByPackage(packageName: String): Boolean {
        val pm = context.packageManager
        val intent = pm.getLaunchIntentForPackage(packageName)
        return if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            vibrateShort()
            true
        } else {
            false
        }
    }

    fun openYouTube(): Boolean {
        return if (!launchAppByPackage("com.google.android.youtube")) {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try {
                context.startActivity(webIntent)
                vibrateShort()
                true
            } catch (e: Exception) {
                false
            }
        } else true
    }

    fun openWhatsApp(): Boolean {
        return if (!launchAppByPackage("com.whatsapp")) {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://web.whatsapp.com")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try {
                context.startActivity(webIntent)
                vibrateShort()
                true
            } catch (e: Exception) {
                false
            }
        } else true
    }

    fun openCamera(): Boolean {
        val intent = Intent("android.media.action.IMAGE_CAPTURE").apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return try {
            context.startActivity(intent)
            vibrateShort()
            true
        } catch (e: Exception) {
            openSettings(Settings.ACTION_SETTINGS)
        }
    }

    fun openDialer(phoneNumber: String = ""): Boolean {
        val uri = if (phoneNumber.isNotBlank()) Uri.parse("tel:$phoneNumber") else Uri.parse("tel:")
        val intent = Intent(Intent.ACTION_DIAL, uri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return try {
            context.startActivity(intent)
            vibrateShort()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun openSms(recipient: String = "", message: String = ""): Boolean {
        val uri = Uri.parse("smsto:$recipient")
        val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
            if (message.isNotBlank()) putExtra("sms_body", message)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return try {
            context.startActivity(intent)
            vibrateShort()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun openChrome(url: String = "https://www.google.com"): Boolean {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return try {
            context.startActivity(intent)
            vibrateShort()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun openMaps(): Boolean {
        return if (!launchAppByPackage("com.google.android.apps.maps")) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=nearby")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try {
                context.startActivity(intent)
                vibrateShort()
                true
            } catch (e: Exception) {
                false
            }
        } else true
    }

    fun setTimer(seconds: Int, message: String = "Myra Timer"): Boolean {
        val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
            putExtra(AlarmClock.EXTRA_LENGTH, seconds)
            putExtra(AlarmClock.EXTRA_MESSAGE, message)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return try {
            context.startActivity(intent)
            vibrateShort()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun setAlarm(hour: Int, minute: Int, message: String = "Myra Alarm"): Boolean {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_MESSAGE, message)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return try {
            context.startActivity(intent)
            vibrateShort()
            true
        } catch (e: Exception) {
            false
        }
    }

    // --- Fast Local System Command Parser (Voice & Text) ---
    // Recognizes Hindi, Hinglish, and English phone commands instantly
    fun processVoiceCommand(input: String): SystemCommandResult {
        val q = input.trim().lowercase(Locale.ROOT)

        // Torch / Flashlight
        if (q.contains("torch on") || q.contains("flashlight on") || q.contains("torch chalu") ||
            q.contains("flashlight chalu") || q.contains("light on") || q.contains("batti jalao") ||
            q.contains("torch jalao")) {
            val success = setTorch(true)
            return SystemCommandResult.Handled(
                if (success) "Flashlight (Torch) on kar di hai! 🔦" else "Torch access nahi ho payi.",
                "torch_on"
            )
        }
        if (q.contains("torch off") || q.contains("flashlight off") || q.contains("torch band") ||
            q.contains("flashlight band") || q.contains("light off") || q.contains("batti bujhao") ||
            q.contains("torch bujhao")) {
            val success = setTorch(false)
            return SystemCommandResult.Handled(
                if (success) "Flashlight (Torch) band kar di gayi hai. 🔦" else "Torch access nahi ho payi.",
                "torch_off"
            )
        }

        // Battery Check
        if (q.contains("battery") || q.contains("charging") || q.contains("battery kitna") ||
            q.contains("charge kitna") || q.contains("battery percent")) {
            val b = refreshBatteryInfo()
            val stateText = if (b.isCharging) "charging ho raha hai (${b.chargingSource})" else "unplugged hai"
            val response = "Aapke phone ki battery ${b.level}% hai aur ye $stateText. Temperature lagbhag ${"%.1f".format(b.temperatureCelsius)}°C hai. 🔋"
            return SystemCommandResult.Handled(response, "battery_check")
        }

        // Volume Controls
        if (q.contains("volume full") || q.contains("volume max") || q.contains("awaz badhao full")) {
            setMediaVolumePercent(100)
            return SystemCommandResult.Handled("Media volume 100% (Maximum) par set kar diya hai! 🔊", "volume_max")
        }
        if (q.contains("volume mute") || q.contains("volume zero") || q.contains("mute karo") || q.contains("awaz band karo")) {
            setMediaVolumePercent(0)
            return SystemCommandResult.Handled("Media volume mute (0%) kar diya gaya hai. 🔇", "volume_mute")
        }
        if (q.contains("volume 50") || q.contains("aadha volume") || q.contains("half volume")) {
            setMediaVolumePercent(50)
            return SystemCommandResult.Handled("Media volume 50% par set kar diya hai. 🔉", "volume_50")
        }
        if (q.contains("silent mode") || q.contains("phone silent karo")) {
            setRingerMode(AudioManager.RINGER_MODE_SILENT)
            return SystemCommandResult.Handled("Phone ko Silent mode me set kar diya hai. 🔕", "silent_mode")
        }
        if (q.contains("vibrate mode") || q.contains("vibration par lagao")) {
            setRingerMode(AudioManager.RINGER_MODE_VIBRATE)
            return SystemCommandResult.Handled("Phone ko Vibrate mode par switch kar diya hai. 📳", "vibrate_mode")
        }
        if (q.contains("normal mode") || q.contains("sound on karo") || q.contains("ringtone on")) {
            setRingerMode(AudioManager.RINGER_MODE_NORMAL)
            return SystemCommandResult.Handled("Phone Ringer Normal mode par set ho gaya hai. 🔔", "normal_mode")
        }

        // App Launchers
        if (q.contains("youtube") && (q.contains("open") || q.contains("kholo") || q.contains("chalao") || q.contains("start"))) {
            openYouTube()
            return SystemCommandResult.Handled("YouTube khol diya hai! Enjoy your videos. ▶️", "open_youtube")
        }
        if (q.contains("whatsapp") && (q.contains("open") || q.contains("kholo") || q.contains("chalao"))) {
            openWhatsApp()
            return SystemCommandResult.Handled("WhatsApp open kar diya hai! 💬", "open_whatsapp")
        }
        if (q.contains("camera") && (q.contains("open") || q.contains("kholo") || q.contains("chalu") || q.contains("photo"))) {
            openCamera()
            return SystemCommandResult.Handled("Camera chalu kar diya hai! 📸", "open_camera")
        }
        if (q.contains("call") || q.contains("dialer") || q.contains("phone lagao") || q.contains("phone kholo")) {
            openDialer()
            return SystemCommandResult.Handled("Phone dialer open kar diya hai. 📞", "open_dialer")
        }
        if (q.contains("browser") || q.contains("chrome") || q.contains("google kholo")) {
            openChrome()
            return SystemCommandResult.Handled("Google Chrome khol diya hai. 🌐", "open_chrome")
        }
        if (q.contains("map") || q.contains("maps kholo") || q.contains("location")) {
            openMaps()
            return SystemCommandResult.Handled("Google Maps launch kar diya hai! 🗺️", "open_maps")
        }

        // Settings Shortcuts
        if (q.contains("wifi setting") || q.contains("wi-fi setting") || q.contains("wifi kholo")) {
            openWifiSettings()
            return SystemCommandResult.Handled("Wi-Fi settings screen open kar di hai. 📶", "open_wifi_settings")
        }
        if (q.contains("bluetooth setting") || q.contains("bluetooth kholo")) {
            openBluetoothSettings()
            return SystemCommandResult.Handled("Bluetooth settings open kar di hai. ᛒ", "open_bluetooth_settings")
        }
        if (q.contains("display setting") || q.contains("brightness setting")) {
            openDisplaySettings()
            return SystemCommandResult.Handled("Display aur Brightness settings open kar di hai. ☀️", "open_display_settings")
        }
        if (q.contains("sound setting") || q.contains("volume setting")) {
            openSoundSettings()
            return SystemCommandResult.Handled("Sound aur Vibration settings khol di hai. 🔊", "open_sound_settings")
        }
        if (q.contains("phone setting") || q.contains("system setting") || q.contains("settings kholo")) {
            openGeneralSettings()
            return SystemCommandResult.Handled("Phone ki Settings khol di hai. ⚙️", "open_settings")
        }

        // Device Telemetry
        if (q.contains("device info") || q.contains("phone info") || q.contains("specs") ||
            q.contains("model kya hai") || q.contains("ram kitna") || q.contains("specifications")) {
            val t = getDeviceTelemetry()
            val net = getNetworkStatus()
            val response = "Aapka device hai ${t.manufacturer} ${t.deviceModel}, running Android ${t.androidVersion}. Total RAM: ${t.totalRamMb} MB (${t.availableRamMb} MB free). Network: ${net.connectionType}. 📱"
            return SystemCommandResult.Handled(response, "device_info")
        }

        // Timer / Alarm quick triggers
        if (q.contains("timer") && (q.contains("set") || q.contains("lagao") || q.contains("chalu"))) {
            setTimer(300, "Myra 5 Min Timer")
            return SystemCommandResult.Handled("5 minute ka timer start karne ke liye clock prompt khol diya hai! ⏱️", "set_timer")
        }

        // Otherwise delegate to Gemini AI with full context
        return SystemCommandResult.FallbackToAI(input)
    }
}
