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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
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
import com.example.cognicare.ui.theme.PatientTheme
import java.util.Locale

enum class SpeechError { NO_MATCH, PERMISSION_DENIED, UNAVAILABLE }

/**
 * Thin Compose-friendly wrapper over Android's [SpeechRecognizer]. Callbacks arrive on the
 * main thread, so state is plain snapshot state. Create it with [rememberSpeechRecognizerController].
 */
class SpeechRecognizerController(private val context: Context) : RecognitionListener {

    val isAvailable: Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    var isListening by mutableStateOf(false)
        private set
    var partialText by mutableStateOf("")
        private set
    var error by mutableStateOf<SpeechError?>(null)
        private set

    var onFinalResult: ((String) -> Unit)? = null

    private var recognizer: SpeechRecognizer? = null

    fun start(languageTag: String = Locale.getDefault().toLanguageTag()) {
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

    fun stop() {
        recognizer?.stopListening()
    }

    fun cancel() {
        recognizer?.cancel()
        isListening = false
        partialText = ""
    }

    fun reset() {
        cancel()
        error = null
    }

    fun reportPermissionDenied() {
        isListening = false
        error = SpeechError.PERMISSION_DENIED
    }

    fun destroy() {
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
class VoiceCapture(val controller: SpeechRecognizerController, val onSpeakClick: () -> Unit)

/**
 * The microphone plumbing shared by every voice surface — the answer box in the talking games,
 * the suggestion dialog, and the floating assistant — so permission handling and language
 * selection exist exactly once.
 *
 * @param resetKey change it (e.g. the question index) to clear the previous attempt.
 */
@Composable
fun rememberVoiceCapture(
    onResult: (String) -> Unit,
    enabled: Boolean = true,
    resetKey: Any? = null
): VoiceCapture {
    val context = LocalContext.current
    val controller = rememberSpeechRecognizerController()
    val latestOnResult by rememberUpdatedState(onResult)
    // Listen in the language the patient chose, not the phone's system language.
    val languageTag = LocalConfiguration.current.locales[0].toLanguageTag()

    SideEffect { controller.onFinalResult = { latestOnResult(it) } }
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
fun speechStatusText(controller: SpeechRecognizerController): String? = when {
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
 */
@Composable
fun VoiceAnswerCapture(
    onResult: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    resetKey: Any? = null,
    buttonSize: Dp = 168.dp,
    idleIcon: ImageVector = Icons.Rounded.Mic
) {
    val capture = rememberVoiceCapture(onResult = onResult, enabled = enabled, resetKey = resetKey)
    val controller = capture.controller

    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        if (!controller.isAvailable) {
            DemoNote(stringResource(R.string.speech_unavailable))
        } else {
            SpeakButton(
                isListening = controller.isListening,
                enabled = enabled,
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
 * keeping the same shape, colours, pulsing halo and haptic tick as the game microphone.
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
    containerColor: Color = if (isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
    contentColor: Color = Color.White,
    border: BorderStroke? = null
) {
    val label = stringResource(if (isListening) R.string.speech_button_listening else R.string.speech_button_idle)
    val haptics = LocalHapticFeedback.current

    Box(modifier = modifier.size(size * 1.25f), contentAlignment = Alignment.Center) {
        if (isListening) ListeningHalo(color = containerColor, size = size)
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
            color = if (enabled) containerColor else containerColor.copy(alpha = 0.4f),
            contentColor = contentColor,
            border = border,
            shadowElevation = 8.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (showLabel) {
                    Column(
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = if (isListening) Icons.Rounded.Stop else idleIcon,
                            contentDescription = null,
                            modifier = Modifier.size(size * 0.38f)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(text = label, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
                    }
                } else {
                    Icon(
                        imageVector = if (isListening) Icons.Rounded.Stop else idleIcon,
                        contentDescription = null,
                        modifier = Modifier.size(size * 0.38f)
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
