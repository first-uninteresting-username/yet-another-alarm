package io.github.first_uninteresting_username.yetanotheralarm

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder

/**
 * Client for https://opentdb.com. One question per request. The API allows one
 * request per IP every 5 seconds; callers are expected to wait that long between calls.
 */
class OpenTrivia(context: Context) {
    data class Question(val text: String, val correct: String, val answers: List<String>)

    sealed interface Result {
        data class Success(val question: Question) : Result
        /** Rate-limited or the session token had to be replaced. Try again later. */
        data object Retry : Result
        data object Failure : Result
    }

    private val prefs = context.createDeviceProtectedStorageContext()
        .getSharedPreferences("opentdb", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun next(): Result = withContext(Dispatchers.IO) {
        try {
            val token = prefs.getString(KEY_TOKEN, null) ?: requestToken() ?: return@withContext Result.Failure
            val response = json.decodeFromString<QuestionResponse>(
                get("$BASE/api.php?amount=1&encode=url3986&token=$token"),
            )
            when (response.code) {
                0 -> response.results.firstOrNull()?.let { Result.Success(it.decoded()) } ?: Result.Failure
                3 -> {
                    prefs.edit { remove(KEY_TOKEN) }
                    Result.Retry
                }
                4 -> {
                    get("$BASE/api_token.php?command=reset&token=$token")
                    Result.Retry
                }
                5 -> Result.Retry
                else -> Result.Failure
            }
        } catch (_: Exception) {
            Result.Failure
        }
    }

    /** Session tokens stop the API from repeating questions. */
    private fun requestToken(): String? {
        val response = json.decodeFromString<TokenResponse>(get("$BASE/api_token.php?command=request"))
        val token = response.token?.takeIf { response.code == 0 } ?: return null
        prefs.edit { putString(KEY_TOKEN, token) }
        return token
    }

    private fun get(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = TIMEOUT_MS
        connection.readTimeout = TIMEOUT_MS
        return try {
            connection.inputStream.use { it.readBytes().decodeToString() }
        } finally {
            connection.disconnect()
        }
    }

    @Serializable
    private class TokenResponse(
        @SerialName("response_code") val code: Int,
        val token: String? = null,
    )

    @Serializable
    private class QuestionResponse(
        @SerialName("response_code") val code: Int,
        val results: List<RawQuestion> = emptyList(),
    )

    @Serializable
    private class RawQuestion(
        val type: String,
        val question: String,
        @SerialName("correct_answer") val correct: String,
        @SerialName("incorrect_answers") val incorrect: List<String>,
    ) {
        fun decoded(): Question {
            val correct = decode(correct)
            val answers = if (type == "boolean") listOf("True", "False")
            else (incorrect.map(::decode) + correct).shuffled()
            return Question(decode(question), correct, answers)
        }
    }

    private companion object {
        const val BASE = "https://opentdb.com"
        const val KEY_TOKEN = "token"
        const val TIMEOUT_MS = 10_000

        fun decode(s: String): String = URLDecoder.decode(s, "UTF-8")
    }
}
