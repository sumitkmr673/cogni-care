package com.example.cognicare.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.cognicare.R
import com.example.cognicare.core.audio.AnswerRecorder
import com.example.cognicare.core.audio.EndpointConfig
import com.example.cognicare.repository.Transcript
import com.example.cognicare.repository.VoiceRepository
import com.example.cognicare.repository.VoiceRepositoryEntryPoint
import com.example.cognicare.ui.theme.PatientTheme
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Locale

enum class SpeechError { NO_MATCH, PERMISSION_DENIED, UNAVAILABLE }

/** How far the voice assistant's green border is mixed toward white (0 = theme green, 1 = white). */
const val ASSISTANT_BORDER_LIGHTEN = 0.55f

/** What every speak button reads, whichever engine turns the patient's voice into text. */
interface SpeechController {
    val isAvailable: Boolean
    val isListening: Boolean
    val partialText: String
    val error: SpeechError?

    /** The recording is with Whisper; the answer arrives in a moment. */
    val isTranscribing: Boolean get() = false

    /** Whisper couldn't be reached after the patient spoke, so the phone is listening again. */
    val retryingOnDevice: Boolean get() = false

    var onFinalResult: ((String) -> Unit)?

    fun start(languageTag: String = Locale.getDefault().toLanguageTag())
    fun stop()
    fun cancel()
    fun reset()
    fun reportPermissionDenied()
    fun destroy()
}

/**
 * Thin Compose-friendly wrapper over Android's [SpeechRecognizer]. Callbacks arrive on the
 * main thread, so state is plain snapshot state. Create it with [rememberSpeechRecognizerController].
 */
class SpeechRecognizerController(private val context: Context) : SpeechController, RecognitionListener {

    override val isAvailable: Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    override var isListening by mutableStateOf(false)
        private set
    override var partialText by mutableStateOf("")
        private set
    override var error by mutableStateOf<SpeechError?>(null)
        private set

    override var onFinalResult: ((String) -> Unit)? = null

    private var recognizer: SpeechRecognizer? = null

    override fun start(languageTag: String) {
        if (!isAvailable) {
            error = SpeechError.UNAVAILABLE
            return
        }
        val active = recognizer ?: SpeechRecognizer.createSpeechRecognizer(context).also {
            it.setRecognitionListener(this)
            recognizer = it
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        error = null
        partialText = ""
        isListening = true
        active.startListening(intent)
    }

    override fun stop() {
        recognizer?.stopListening()
    }

    override fun cancel() {
        recognizer?.cancel()
        isListening = false
        partialText = ""
    }

    override fun reset() {
        cancel()
        error = null
    }

    override fun reportPermissionDenied() {
        isListening = false
        error = SpeechError.PERMISSION_DENIED
    }

    override fun destroy() {
        onFinalResult = null
        recognizer?.destroy()
        recognizer = null
    }

    override fun onResults(results: Bundle?) {
        isListening = false
        val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull()
            ?.trim()
            .orEmpty()
        partialText = text
        if (text.isEmpty()) error = SpeechError.NO_MATCH else onFinalResult?.invoke(text)
    }

    override fun onPartialResults(partialResults: Bundle?) {
        partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull()
            ?.takeIf { it.isNotBlank() }
            ?.let { partialText = it }
    }

    override fun onError(code: Int) {
        isListening = false
        error = when (code) {
            // ERROR_CLIENT follows our own cancel(); it is not a user-facing failure.
            SpeechRecognizer.ERROR_CLIENT -> null
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> SpeechError.PERMISSION_DENIED
            SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> SpeechError.NO_MATCH
            else -> SpeechError.UNAVAILABLE
        }
    }

    override fun onReadyForSpeech(params: Bundle?) = Unit
    override fun onBeginningOfSpeech() = Unit
    override fun onRmsChanged(rmsdB: Float) = Unit
    override fun onBufferReceived(buffer: ByteArray?) = Unit
    override fun onEndOfSpeech() = Unit
    override fun onEvent(eventType: Int, params: Bundle?) = Unit
}

/**
 * The talking games' ears: records the answer and has Whisper (voice service, GPU machine)
 * transcribe it, which copes better with Indian accents, Hinglish and elderly speech than the
 * phone's recogniser.
 *
 * [fallback] (Android's recogniser) takes over whenever Whisper can't: straight away while the
 * service is cooling off after a failure, if the microphone can't be opened for recording, and —
 * after a failed upload — by listening once more, with the status asking the patient to repeat.
 * The game only ever sees one [onFinalResult] with the words, however they were heard.
 */
class WhisperSpeechController(
    private val recorder: AnswerRecorder,
    private val repository: VoiceRepository,
    private val fallback: SpeechRecognizerController,
    private val scope: CoroutineScope
) : SpeechController {

    /** Every answer shown on screen, to steer Whisper's spelling; never just the correct one. */
    var hint: String? = null
    var endpoint: EndpointConfig = EndpointConfig.SHORT_ANSWER

    private var usingFallback by mutableStateOf(false)
    private var recording by mutableStateOf(false)
    private var ownError by mutableStateOf<SpeechError?>(null)
    private var heard by mutableStateOf("")

    override var isTranscribing by mutableStateOf(false)
        private set
    override var retryingOnDevice by mutableStateOf(false)
        private set

    override val isAvailable: Boolean = true
    override val isListening: Boolean get() = if (usingFallback) fallback.isListening else recording
    override val partialText: String get() = if (usingFallback) fallback.partialText else heard
    override val error: SpeechError? get() = if (usingFallback) fallback.error else ownError

    override var onFinalResult: ((String) -> Unit)? = null

    private var job: Job? = null

    @Volatile
    private var stopRequested = false

    init {
        fallback.onFinalResult = { onFinalResult?.invoke(it) }
    }

    override fun start(languageTag: String) {
        reset()
        if (!repository.shouldTryService()) {
            startFallback(languageTag)
            return
        }
        stopRequested = false
        job = scope.launch {
            recording = true
            val take = try {
                recorder.record(endpoint) { stopRequested }
            } catch (cancelled: CancellationException) {
                recording = false
                throw cancelled
            } catch (error: Exception) {
                // Microphone busy or recorder unsupported: the phone's recogniser still works.
                recording = false
                startFallback(languageTag)
                return@launch
            }
            recording = false
            try {
                if (!take.heardSpeech) {
                    ownError = SpeechError.NO_MATCH
                    return@launch
                }
                isTranscribing = true
                when (val transcript = repository.transcribe(take.file, languageTag, hint)) {
                    is Transcript.Text -> {
                        heard = transcript.text
                        onFinalResult?.invoke(transcript.text)
                    }
                    Transcript.Empty -> ownError = SpeechError.NO_MATCH
                    Transcript.Unavailable -> {
                        retryingOnDevice = true
                        startFallback(languageTag)
                    }
                }
            } finally {
                isTranscribing = false
                take.file.delete()
            }
        }
    }

    private fun startFallback(languageTag: String) {
        usingFallback = true
        fallback.start(languageTag)
    }

    /** Tapping the button while recording ends the answer now instead of waiting for silence. */
    override fun stop() {
        if (usingFallback) fallback.stop() else stopRequested = true
    }

    override fun cancel() {
        job?.cancel()
        job = null
        recording = false
        isTranscribing = false
        heard = ""
        fallback.cancel()
    }

    override fun reset() {
        cancel()
        ownError = null
        retryingOnDevice = false
        usingFallback = false
        fallback.reset()
    }

    override fun reportPermissionDenied() {
        usingFallback = false
        ownError = SpeechError.PERMISSION_DENIED
    }

    override fun destroy() {
        cancel()
        onFinalResult = null
    }
}

@Composable
fun rememberSpeechRecognizerController(): SpeechRecognizerController {
    val context = LocalContext.current
    val controller = remember { SpeechRecognizerController(context.applicationContext) }
    DisposableEffect(controller) {
        onDispose { controller.destroy() }
    }
    return controller
}

/** A live recognizer plus the tap handler that starts or stops it (asking for the mic first). */
class VoiceCapture(val controller: SpeechController, val onSpeakClick: () -> Unit)

/**
 * The microphone plumbing shared by every voice surface — the answer box in the talking games,
 * the suggestion dialog, and the floating assistant — so permission handling and language
 * selection exist exactly once.
 *
 * @param resetKey change it (e.g. the question index) to clear the previous attempt.
 * @param useWhisper record for the voice service's Whisper, falling back to the phone's
 *   recogniser; for the talking games. Other surfaces keep the phone's live recogniser, whose
 *   text then goes to Qwen.
 * @param whisperHint every answer on screen, comma-separated, to steer Whisper's spelling.
 */
@Composable
fun rememberVoiceCapture(
    onResult: (String) -> Unit,
    enabled: Boolean = true,
    resetKey: Any? = null,
    useWhisper: Boolean = false,
    whisperHint: String? = null,
    endpoint: EndpointConfig = EndpointConfig.SHORT_ANSWER
): VoiceCapture {
    val context = LocalContext.current
    val recognizer = rememberSpeechRecognizerController()
    val scope = rememberCoroutineScope()
    val whisper = if (useWhisper) {
        remember(recognizer) {
            val repository = EntryPointAccessors
                .fromApplication(context.applicationContext, VoiceRepositoryEntryPoint::class.java)
                .voiceRepository()
            WhisperSpeechController(AnswerRecorder(context.applicationContext), repository, recognizer, scope)
        }
    } else {
        null
    }
    val controller: SpeechController = whisper ?: recognizer
    DisposableEffect(whisper) {
        onDispose { whisper?.destroy() }
    }

    val latestOnResult by rememberUpdatedState(onResult)
    // Listen in the language the patient chose, not the phone's system language.
    val languageTag = LocalConfiguration.current.locales[0].toLanguageTag()

    SideEffect {
        controller.onFinalResult = { latestOnResult(it) }
        whisper?.hint = whisperHint
        whisper?.endpoint = endpoint
    }
    LaunchedEffect(resetKey) { controller.reset() }
    LaunchedEffect(enabled) { if (!enabled) controller.cancel() }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) controller.start(languageTag) else controller.reportPermissionDenied()
    }

    val onSpeakClick: () -> Unit = {
        when {
            controller.isListening -> controller.stop()
            controller.isTranscribing -> Unit
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED -> controller.start(languageTag)
            else -> permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
    // Rebuilt each composition on purpose: onSpeakClick closes over the current languageTag.
    return VoiceCapture(controller, onSpeakClick)
}

/** The one-line status under a speak button, or null when there is nothing to say. */
@Composable
fun speechStatusText(controller: SpeechController): String? = when {
    controller.isTranscribing -> stringResource(R.string.speech_transcribing)
    controller.isListening && controller.retryingOnDevice -> stringResource(R.string.speech_say_again)
    controller.isListening && controller.partialText.isNotBlank() ->
        stringResource(R.string.speech_heard, controller.partialText)
    controller.isListening -> stringResource(R.string.speech_listening_hint)
    controller.error == SpeechError.NO_MATCH -> stringResource(R.string.speech_error_no_match)
    controller.error == SpeechError.PERMISSION_DENIED -> stringResource(R.string.speech_error_permission)
    controller.error == SpeechError.UNAVAILABLE -> stringResource(R.string.speech_error_other)
    else -> null
}

/**
 * Big speak button plus status line. Handles the microphone permission, and falls back to a
 * note when the device has no recognition service; callers always offer tap answers too.
 *
 * @param resetKey change it (e.g. the question index) to clear the previous attempt.
 * @param useWhisper see [rememberVoiceCapture].
 */
@Composable
fun VoiceAnswerCapture(
    onResult: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    resetKey: Any? = null,
    buttonSize: Dp = 168.dp,
    idleIcon: ImageVector = Icons.Rounded.Mic,
    useWhisper: Boolean = false,
    whisperHint: String? = null,
    endpoint: EndpointConfig = EndpointConfig.SHORT_ANSWER
) {
    val capture = rememberVoiceCapture(
        onResult = onResult,
        enabled = enabled,
        resetKey = resetKey,
        useWhisper = useWhisper,
        whisperHint = whisperHint,
        endpoint = endpoint
    )
    val controller = capture.controller

    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        if (!controller.isAvailable) {
            DemoNote(stringResource(R.string.speech_unavailable))
        } else {
            SpeakButton(
                isListening = controller.isListening,
                // Nothing to tap while Whisper is working; the status line says so.
                enabled = enabled && !controller.isTranscribing,
                onClick = capture.onSpeakClick,
                size = buttonSize,
                idleIcon = idleIcon
            )
            Spacer(Modifier.height(12.dp))

            val isError = !controller.isListening && controller.error != null
            val status = speechStatusText(controller)
            if (status != null) {
                Text(
                    text = status,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { liveRegion = LiveRegionMode.Polite },
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isError) PatientTheme.colors.accentText else PatientTheme.colors.mutedText,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * The one speak button in the app. [idleIcon] lets the voice assistant show its robot while
 * keeping the same shape, pulsing halo and haptic tick as the game microphone.
 *
 * [outlined] is the voice assistant's look: a white face with a green border and a green icon,
 * so it reads as a helper rather than competing with a game's solid green microphone. Either way,
 * listening switches the accent to red.
 *
 * Tap to start and tap to stop, never press-and-hold: holding a button steady while speaking is
 * hard for many elderly patients, and listening also ends by itself when they stop talking.
 */
@Composable
fun SpeakButton(
    isListening: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = 168.dp,
    idleIcon: ImageVector = Icons.Rounded.Mic,
    showLabel: Boolean = true,
    outlined: Boolean = false
) {
    val accent = if (isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val container = if (outlined) Color.White else accent
    // A softer, opaque green at rest; full-strength red while listening, since that is the cue.
    val outline = if (isListening) accent else lerp(accent, Color.White, ASSISTANT_BORDER_LIGHTEN)
    val label = stringResource(if (isListening) R.string.speech_button_listening else R.string.speech_button_idle)
    val haptics = LocalHapticFeedback.current

    Box(modifier = modifier.size(size * 1.25f), contentAlignment = Alignment.Center) {
        if (isListening) ListeningHalo(color = accent, size = size)
        Surface(
            onClick = {
                // A physical tick confirms the press for patients who may not notice the colour change.
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
            enabled = enabled,
            // The visible label already names the button; only describe it when that label is hidden.
            modifier = Modifier
                .size(size)
                .then(if (showLabel) Modifier else Modifier.semantics { contentDescription = label }),
            shape = CircleShape,
            color = if (enabled) container else container.copy(alpha = 0.4f),
            contentColor = if (outlined) accent else Color.White,
            border = if (outlined) BorderStroke(3.dp, if (enabled) outline else outline.copy(alpha = 0.4f)) else null,
            shadowElevation = 8.dp
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Rounded.Stop else idleIcon,
                    contentDescription = null,
                    modifier = Modifier.size(size * 0.38f)
                )
                if (showLabel) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = label,
                        // The assistant's button is smaller and secondary to the page, so its label is too.
                        style = if (outlined) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun ListeningHalo(color: Color, size: Dp) {
    val transition = rememberInfiniteTransition(label = "micPulse")
    val haloScale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.22f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "micHalo"
    )
    Box(
        Modifier
            .size(size)
            .scale(haloScale)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.22f))
    )
}
