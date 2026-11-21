package com.ayattla.app.audio

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Listens to the user with the speech recognition built into the device (free, no account): on the
 * device itself when Arabic is available there, otherwise the phone's own recognition service.
 * Keeps listening across pauses until [stop], collecting what was heard in [finals] and [partial].
 * Nothing is recorded or kept: the text lives only while the screen is open.
 */
class Listener(private val context: Context) {
    var listening by mutableStateOf(false)
        private set
    var partial by mutableStateOf("")
        private set
    val finals = mutableStateListOf<String>()
    var problem by mutableStateOf<String?>(null)
        private set
    /** Recognition runs fully on the device (works without internet). */
    var onDevice by mutableStateOf(false)
        private set

    private var recognizer: SpeechRecognizer? = null
    private var wanted = false
    private var triedOnDevice = false
    private var errors = 0
    private val main = Handler(Looper.getMainLooper())

    val available: Boolean get() = SpeechRecognizer.isRecognitionAvailable(context) ||
        (Build.VERSION.SDK_INT >= 31 && SpeechRecognizer.isOnDeviceRecognitionAvailable(context))

    /** Everything heard so far, in order. */
    fun text(): String = (finals + partial).filter { it.isNotBlank() }.joinToString(" ")

    fun clear() { finals.clear(); partial = "" }

    fun start() {
        problem = null
        if (!available) { problem = "لا يوجد في هذا الجهاز خدمة للتعرف على الكلام"; return }
        wanted = true
        listening = true
        if (recognizer == null) create(preferDevice = true)
        listen()
    }

    fun stop() {
        wanted = false
        listening = false
        if (partial.isNotBlank()) { finals += partial; partial = "" }
        runCatching { recognizer?.stopListening() }
    }

    fun release() {
        wanted = false
        listening = false
        main.removeCallbacksAndMessages(null)
        runCatching { recognizer?.destroy() }
        recognizer = null
    }

    private fun create(preferDevice: Boolean) {
        runCatching { recognizer?.destroy() }
        val device = preferDevice && !triedOnDevice && Build.VERSION.SDK_INT >= 31 && SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
        if (device) triedOnDevice = true
        onDevice = device
        recognizer = (if (device && Build.VERSION.SDK_INT >= 31) SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
            else SpeechRecognizer.createSpeechRecognizer(context)).apply { setRecognitionListener(callbacks) }
    }

    private fun intent() = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-SA")
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ar-SA")
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 4000L)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 15000L)
        if (Build.VERSION.SDK_INT >= 33) putExtra(RecognizerIntent.EXTRA_ENABLE_FORMATTING, RecognizerIntent.FORMATTING_OPTIMIZE_QUALITY)
    }

    private fun listen() {
        if (!wanted) return
        runCatching { recognizer?.startListening(intent()) }.onFailure { problem = "تعذر بدء الاستماع"; listening = false; wanted = false }
    }

    /** Listening ends after each pause; it starts again by itself while the user has not stopped. */
    private fun again(delayMs: Long = 150) { if (wanted) main.postDelayed({ listen() }, delayMs) }

    private val callbacks = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) { problem = null }
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onEvent(eventType: Int, params: Bundle?) {}

        override fun onPartialResults(partialResults: Bundle?) {
            partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let { partial = it; errors = 0 }
        }

        override fun onResults(results: Bundle?) {
            val t = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
            if (t.isNotBlank()) { finals += t; errors = 0 }
            partial = ""
            if (wanted) again() else listening = false
        }

        override fun onError(error: Int) {
            if (partial.isNotBlank()) { finals += partial; partial = "" }
            when (error) {
                SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> again()
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> again(600)
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> fail("اسمح للتطبيق باستخدام الميكروفون")
                SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT, SpeechRecognizer.ERROR_SERVER ->
                    fail("التعرف على الصوت في هذا الجهاز يحتاج اتصالا بالإنترنت")
                else -> {
                    // Arabic not installed for on-device recognition (or another device error): use the
                    // phone's regular recognition service once, then report.
                    if (onDevice) { create(preferDevice = false); again(300) }
                    else if (error == 12 || error == 13) fail("اللغة العربية غير متاحة للتعرف على الصوت في هذا الجهاز")
                    else if (++errors > 6) fail("توقف التعرف على الصوت، اضغط للمتابعة")
                    else again(500)
                }
            }
        }
    }

    private fun fail(msg: String) {
        problem = msg
        wanted = false
        listening = false
    }
}
