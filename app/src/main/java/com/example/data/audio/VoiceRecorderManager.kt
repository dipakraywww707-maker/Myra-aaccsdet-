package com.example.data.audio

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

enum class RecorderState {
    IDLE,
    RECORDING,
    STOPPED,
    PLAYING
}

class VoiceRecorderManager(private val context: Context) {

    private val tag = "VoiceRecorderManager"
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private val mainHandler = Handler(Looper.getMainLooper())

    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var speechRecognizer: SpeechRecognizer? = null

    private var pollingJob: Job? = null
    private var timerJob: Job? = null
    private var playbackProgressJob: Job? = null

    private val _state = MutableStateFlow(RecorderState.IDLE)
    val state: StateFlow<RecorderState> = _state.asStateFlow()

    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    private val _waveformSamples = MutableStateFlow<List<Float>>(List(24) { 0.1f })
    val waveformSamples: StateFlow<List<Float>> = _waveformSamples.asStateFlow()

    private val _durationSeconds = MutableStateFlow(0)
    val durationSeconds: StateFlow<Int> = _durationSeconds.asStateFlow()

    private val _recordedAudioFile = MutableStateFlow<File?>(null)
    val recordedAudioFile: StateFlow<File?> = _recordedAudioFile.asStateFlow()

    private val _transcribedText = MutableStateFlow("")
    val transcribedText: StateFlow<String> = _transcribedText.asStateFlow()

    private val _statusMessage = MutableStateFlow("Ready to record voice command")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _playbackProgress = MutableStateFlow(0f)
    val playbackProgress: StateFlow<Float> = _playbackProgress.asStateFlow()

    fun startRecording() {
        stopPlayback()
        cancelRecordingInternal()

        _transcribedText.value = ""
        _durationSeconds.value = 0
        _statusMessage.value = "Listening to microphone..."
        _waveformSamples.value = List(24) { 0.15f }

        val cacheDir = context.cacheDir
        val audioFile = File(cacheDir, "myra_cmd_${System.currentTimeMillis()}.m4a")
        _recordedAudioFile.value = audioFile

        try {
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            recorder.setAudioEncodingBitRate(128000)
            recorder.setAudioSamplingRate(44100)
            recorder.setOutputFile(audioFile.absolutePath)

            recorder.prepare()
            recorder.start()
            mediaRecorder = recorder
            _state.value = RecorderState.RECORDING
            _statusMessage.value = "🔴 Recording microphone audio..."

            // Amplitude polling loop
            startAmplitudePolling(recorder)

            // Duration timer loop
            startDurationTimer()

            // Try starting background speech recognizer for live transcription
            initSpeechRecognizer()

        } catch (e: Exception) {
            Log.e(tag, "Failed to start MediaRecorder", e)
            _statusMessage.value = "Mic capture active: speak your command"
            _state.value = RecorderState.RECORDING
            // If MediaRecorder failed (e.g. exclusive lock), still try SpeechRecognizer
            initSpeechRecognizer()
        }
    }

    private fun startAmplitudePolling(recorder: MediaRecorder) {
        pollingJob?.cancel()
        pollingJob = scope.launch(Dispatchers.IO) {
            val currentList = ArrayList<Float>(_waveformSamples.value)
            while (isActive && _state.value == RecorderState.RECORDING) {
                val rawAmp = try {
                    recorder.maxAmplitude
                } catch (e: Exception) {
                    0
                }
                val normalized = (rawAmp / 32767f).coerceIn(0.08f, 1.0f)
                _amplitude.value = normalized

                synchronized(currentList) {
                    if (currentList.size >= 24) {
                        currentList.removeAt(0)
                    }
                    currentList.add(normalized)
                    _waveformSamples.value = ArrayList(currentList)
                }

                delay(80)
            }
        }
    }

    private fun startDurationTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (isActive && _state.value == RecorderState.RECORDING) {
                delay(1000)
                _durationSeconds.value += 1
                // Auto-stop at 30 seconds
                if (_durationSeconds.value >= 30) {
                    stopRecording()
                    break
                }
            }
        }
    }

    private fun initSpeechRecognizer() {
        mainHandler.post {
            try {
                if (SpeechRecognizer.isRecognitionAvailable(context)) {
                    speechRecognizer?.destroy()
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                        setRecognitionListener(object : RecognitionListener {
                            override fun onReadyForSpeech(params: Bundle?) {
                                _statusMessage.value = "🎤 Listening for speech..."
                            }

                            override fun onBeginningOfSpeech() {
                                _statusMessage.value = "🗣️ Voice detected, speaking..."
                            }

                            override fun onRmsChanged(rmsdB: Float) {
                                // If mediaRecorder isn't providing amplitude, use rmsdB
                                if (mediaRecorder == null) {
                                    val norm = ((rmsdB + 2f) / 12f).coerceIn(0.1f, 1f)
                                    _amplitude.value = norm
                                }
                            }

                            override fun onBufferReceived(buffer: ByteArray?) {}

                            override fun onEndOfSpeech() {
                                _statusMessage.value = "Processing voice..."
                            }

                            override fun onError(error: Int) {
                                Log.w(tag, "SpeechRecognizer error: $error")
                                if (_transcribedText.value.isBlank()) {
                                    _statusMessage.value = "Voice audio captured"
                                }
                            }

                            override fun onResults(results: Bundle?) {
                                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                val text = matches?.firstOrNull()
                                if (!text.isNullOrBlank()) {
                                    _transcribedText.value = text
                                    _statusMessage.value = "✅ Voice transcribed: \"$text\""
                                }
                            }

                            override fun onPartialResults(partialResults: Bundle?) {
                                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                val partial = matches?.firstOrNull()
                                if (!partial.isNullOrBlank()) {
                                    _transcribedText.value = partial
                                }
                            }

                            override fun onEvent(eventType: Int, params: Bundle?) {}
                        })

                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
                            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                        }
                        startListening(intent)
                    }
                }
            } catch (e: Exception) {
                Log.w(tag, "Could not initialize SpeechRecognizer", e)
            }
        }
    }

    fun stopRecording() {
        pollingJob?.cancel()
        timerJob?.cancel()

        try {
            mediaRecorder?.let {
                it.stop()
                it.release()
            }
        } catch (e: Exception) {
            Log.e(tag, "Error stopping MediaRecorder", e)
        } finally {
            mediaRecorder = null
        }

        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            Log.w(tag, "Error stopping SpeechRecognizer", e)
        }

        _state.value = RecorderState.STOPPED
        _amplitude.value = 0f
        if (_statusMessage.value.startsWith("🔴")) {
            _statusMessage.value = "Audio captured (${formatDuration(_durationSeconds.value)})"
        }
    }

    fun playRecordedAudio() {
        val file = _recordedAudioFile.value
        if (file == null || !file.exists()) {
            _statusMessage.value = "No audio recording found"
            return
        }

        stopPlayback()

        try {
            val player = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                setOnCompletionListener {
                    _state.value = RecorderState.STOPPED
                    _playbackProgress.value = 0f
                    playbackProgressJob?.cancel()
                }
                start()
            }
            mediaPlayer = player
            _state.value = RecorderState.PLAYING
            _statusMessage.value = "▶️ Playing recorded voice..."

            // Progress tracking
            playbackProgressJob?.cancel()
            playbackProgressJob = scope.launch {
                val totalMs = player.duration.coerceAtLeast(1)
                while (isActive && player.isPlaying) {
                    _playbackProgress.value = (player.currentPosition.toFloat() / totalMs).coerceIn(0f, 1f)
                    delay(50)
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error playing audio file", e)
            _statusMessage.value = "Error playing recording"
            _state.value = RecorderState.STOPPED
        }
    }

    fun pausePlayback() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.pause()
                    _state.value = RecorderState.STOPPED
                    _statusMessage.value = "Audio paused"
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error pausing player", e)
        }
    }

    fun stopPlayback() {
        playbackProgressJob?.cancel()
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: Exception) {
            Log.e(tag, "Error stopping player", e)
        } finally {
            mediaPlayer = null
            _playbackProgress.value = 0f
            if (_state.value == RecorderState.PLAYING) {
                _state.value = RecorderState.STOPPED
            }
        }
    }

    fun cancelRecording() {
        cancelRecordingInternal()
        _state.value = RecorderState.IDLE
        _transcribedText.value = ""
        _durationSeconds.value = 0
        _statusMessage.value = "Ready to record voice command"
        _amplitude.value = 0f
        _waveformSamples.value = List(24) { 0.1f }
    }

    private fun cancelRecordingInternal() {
        pollingJob?.cancel()
        timerJob?.cancel()
        stopPlayback()

        try {
            mediaRecorder?.let {
                try {
                    it.stop()
                } catch (ignored: Exception) {}
                it.release()
            }
        } catch (e: Exception) {
            Log.e(tag, "Error releasing MediaRecorder", e)
        } finally {
            mediaRecorder = null
        }

        try {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.w(tag, "Error releasing SpeechRecognizer", e)
        } finally {
            speechRecognizer = null
        }

        _recordedAudioFile.value?.let { file ->
            if (file.exists()) {
                file.delete()
            }
        }
        _recordedAudioFile.value = null
    }

    fun setCustomTranscribedText(text: String) {
        _transcribedText.value = text
    }

    fun release() {
        cancelRecordingInternal()
    }

    companion object {
        fun formatDuration(seconds: Int): String {
            val mins = seconds / 60
            val secs = seconds % 60
            return String.format(Locale.US, "%02d:%02d", mins, secs)
        }
    }
}
