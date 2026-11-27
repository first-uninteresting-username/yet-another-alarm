package io.github.first_uninteresting_username.yetanotheralarm.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.first_uninteresting_username.yetanotheralarm.Alarm
import io.github.first_uninteresting_username.yetanotheralarm.OpenTrivia
import io.github.first_uninteresting_username.yetanotheralarm.TriviaViewModel
import io.github.first_uninteresting_username.yetanotheralarm.formatDays
import io.github.first_uninteresting_username.yetanotheralarm.formatTime

private const val FAILURES_BEFORE_FREE_SNOOZE = 3

@Composable
fun RingScreen(alarm: Alarm, onSnooze: () -> Unit, onStop: () -> Unit) {
    val context = LocalContext.current
    var answering by rememberSaveable { mutableStateOf(false) }
    var confirmStop by rememberSaveable { mutableStateOf(false) }

    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AnimatedVisibility(confirmStop) {
                StopBanner(onConfirm = onStop, onCancel = { confirmStop = false })
            }
            Text(
                formatTime(context, alarm.hour, alarm.minute),
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 88.sp, lineHeight = 96.sp),
                modifier = Modifier.padding(top = 48.dp),
            )
            Text(formatDays(alarm.days), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.size(32.dp))

            AnimatedContent(answering, label = "ring-actions") { isAnswering ->
                if (isAnswering) {
                    TriviaPanel(alarm, onSolved = onSnooze)
                } else {
                    Button(
                        onClick = { answering = true },
                        shapes = ButtonDefaults.shapesFor(ButtonDefaults.ExtraLargeContainerHeight),
                        contentPadding = ButtonDefaults.contentPaddingFor(ButtonDefaults.ExtraLargeContainerHeight),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = ButtonDefaults.ExtraLargeContainerHeight),
                    ) {
                        Text(
                            "Snooze ${alarm.snoozeMinutes} min",
                            style = ButtonDefaults.textStyleFor(ButtonDefaults.ExtraLargeContainerHeight),
                        )
                    }
                }
            }
            OutlinedButton(
                onClick = { confirmStop = true },
                shapes = ButtonDefaults.shapesFor(ButtonDefaults.MediumContainerHeight),
                contentPadding = ButtonDefaults.contentPaddingFor(ButtonDefaults.MediumContainerHeight),
                modifier = Modifier.heightIn(min = ButtonDefaults.MediumContainerHeight),
            ) {
                Text("Stop", style = ButtonDefaults.textStyleFor(ButtonDefaults.MediumContainerHeight))
            }
        }
    }
}

@Composable
private fun StopBanner(onConfirm: () -> Unit, onCancel: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Stop this alarm?", style = MaterialTheme.typography.titleLarge)
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                TextButton(onClick = onCancel, shapes = ButtonDefaults.shapes()) { Text("Keep ringing") }
                Button(
                    onClick = onConfirm,
                    shapes = ButtonDefaults.shapes(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) { Text("Stop") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TriviaPanel(alarm: Alarm, onSolved: () -> Unit) {
    val trivia: TriviaViewModel = viewModel()
    LaunchedEffect(Unit) { trivia.start() }
    LaunchedEffect(trivia.solved) { if (trivia.solved) onSolved() }

    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val question = trivia.question
        if (question == null) {
            LoadingIndicator(Modifier.size(64.dp))
            Text("Fetching a question…", style = MaterialTheme.typography.bodyLarge)
        } else {
            Text(
                question.text,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            question.answers.forEach { answer ->
                FilledTonalButton(
                    onClick = { trivia.answer(answer) },
                    shapes = ButtonDefaults.shapesFor(ButtonDefaults.MediumContainerHeight),
                    colors = answerColors(question, answer, trivia.picked),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = ButtonDefaults.MediumContainerHeight),
                ) {
                    Text(answer, textAlign = TextAlign.Center)
                }
            }
            if (trivia.loading) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LoadingIndicator(Modifier.size(40.dp))
                    Text("Wrong. Next question coming…", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }

        if (trivia.failures >= FAILURES_BEFORE_FREE_SNOOZE) {
            Text(
                "Can't reach Open Trivia Database.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
            Button(onClick = onSolved, shapes = ButtonDefaults.shapes()) {
                Text("Snooze ${alarm.snoozeMinutes} min without a question")
            }
        }

        Text(
            "Questions from Open Trivia Database (opentdb.com), CC BY-SA 4.0",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun answerColors(question: OpenTrivia.Question, answer: String, picked: String?): ButtonColors {
    val scheme = MaterialTheme.colorScheme
    return when {
        picked == null -> ButtonDefaults.filledTonalButtonColors()
        answer == question.correct -> ButtonDefaults.filledTonalButtonColors(
            containerColor = scheme.primary,
            contentColor = scheme.onPrimary,
        )
        answer == picked -> ButtonDefaults.filledTonalButtonColors(
            containerColor = scheme.error,
            contentColor = scheme.onError,
        )
        else -> ButtonDefaults.filledTonalButtonColors()
    }
}
