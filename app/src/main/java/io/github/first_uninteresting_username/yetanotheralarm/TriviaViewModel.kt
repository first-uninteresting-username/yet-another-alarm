package io.github.first_uninteresting_username.yetanotheralarm

import android.app.Application
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Hands out trivia questions until one is answered correctly. */
class TriviaViewModel(app: Application) : AndroidViewModel(app) {
    private val trivia = OpenTrivia(app)
    private var lastRequestAt = 0L
    private var job: Job? = null

    /** The current question, or the one just answered while the next is loading. */
    var question by mutableStateOf<OpenTrivia.Question?>(null)
        private set
    var picked by mutableStateOf<String?>(null)
        private set
    var loading by mutableStateOf(false)
        private set
    var failures by mutableIntStateOf(0)
        private set

    /** `SystemClock.elapsedRealtime()` at which the rate limit allows the next request, while waiting for it. */
    var waitingUntil by mutableStateOf<Long?>(null)
        private set

    val solved get() = picked != null && picked == question?.correct

    fun start() {
        if (question == null && job?.isActive != true) fetch()
    }

    fun answer(choice: String) {
        if (picked != null || loading) return
        picked = choice
        if (choice != question?.correct) fetch()
    }

    private fun fetch() {
        job = viewModelScope.launch {
            loading = true
            while (true) {
                val readyAt = lastRequestAt + RATE_LIMIT_MS
                if (readyAt > SystemClock.elapsedRealtime()) {
                    waitingUntil = readyAt
                    delay(readyAt - SystemClock.elapsedRealtime())
                    waitingUntil = null
                }
                lastRequestAt = SystemClock.elapsedRealtime()
                when (val result = trivia.next()) {
                    is OpenTrivia.Result.Success -> {
                        question = result.question
                        picked = null
                        failures = 0
                        break
                    }
                    OpenTrivia.Result.Retry -> Unit
                    OpenTrivia.Result.Failure -> failures++
                }
            }
            loading = false
        }
    }

    companion object {
        const val RATE_LIMIT_MS = 5_000L
    }
}
