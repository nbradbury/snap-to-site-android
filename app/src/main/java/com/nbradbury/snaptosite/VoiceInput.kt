package com.nbradbury.snaptosite

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * In-app speech recognition with live partial results. In [continuous] mode it keeps restarting
 * the recognizer across pauses until [stop] is called, so the owner can talk for as long as they
 * like. Must be used from the main thread.
 */
class VoiceInput(private val context: Context) {
    data class State(val listening: Boolean = false, val text: String = "")

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state

    private var recognizer: SpeechRecognizer? = null
    private var committed = ""
    private var wanted = false
    private var continuous = false
    private var onSegment: (String) -> Unit = {}
    private var onDone: (String) -> Unit = {}
    private var onError: (String) -> Unit = {}

    val isAvailable get() = SpeechRecognizer.isRecognitionAvailable(context)

    /**
     * Starts listening, appending to [initial]. [onSegment] gets the full text after each pause,
     * [onDone] the final text once listening ends.
     */
    fun start(
        continuous: Boolean,
        initial: String = "",
        onSegment: (String) -> Unit = {},
        onError: (String) -> Unit,
        onDone: (String) -> Unit,
    ) {
        cancel()
        this.continuous = continuous
        this.onSegment = onSegment
        this.onDone = onDone
        this.onError = onError
        committed = initial.trim()
        wanted = true
        _state.value = State(listening = true, text = committed)
        listen()
    }

    /** Stops after the current utterance is recognized; [onDone] follows. */
    fun stop() {
        wanted = false
        recognizer?.stopListening() ?: finish()
    }

    fun cancel() {
        wanted = false
        recognizer?.destroy()
        recognizer = null
        _state.value = State()
    }

    private fun listen() {
        val r = recognizer ?: SpeechRecognizer.createSpeechRecognizer(context).also {
            it.setRecognitionListener(listener)
            recognizer = it
        }
        r.startListening(
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true),
        )
    }

    private fun join(a: String, b: String) = listOf(a, b).filter { it.isNotBlank() }.joinToString(" ")

    private fun finish() {
        recognizer?.destroy()
        recognizer = null
        _state.value = State(listening = false, text = committed)
        onDone(committed)
    }

    private val listener = object : RecognitionListener {
        override fun onPartialResults(partialResults: Bundle) {
            val partial = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
            _state.value = State(listening = true, text = join(committed, partial))
        }

        override fun onResults(results: Bundle) {
            val text = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
            committed = join(committed, text)
            _state.value = State(listening = true, text = committed)
            if (text.isNotBlank()) onSegment(committed)
            if (wanted && continuous) listen() else finish()
        }

        override fun onError(error: Int) {
            val silence = error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT
            when {
                silence && wanted && continuous -> listen()
                silence || !wanted -> finish()
                else -> {
                    val message = when (error) {
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Snap-to-Site needs microphone access to listen."
                        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Speech recognition needs a connection."
                        else -> "Couldn't hear that. Try again, or type instead."
                    }
                    wanted = false
                    finish()
                    onError(message)
                }
            }
        }

        override fun onReadyForSpeech(params: Bundle?) = Unit
        override fun onBeginningOfSpeech() = Unit
        override fun onRmsChanged(rmsdB: Float) = Unit
        override fun onBufferReceived(buffer: ByteArray?) = Unit
        override fun onEndOfSpeech() = Unit
        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }
}
