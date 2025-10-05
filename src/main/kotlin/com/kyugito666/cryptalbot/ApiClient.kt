package com.kyugito666.cryptalbot // Diubah

import io.ktor.client.*
// ... (sisa kode sama persis, hanya package yang diubah)
import io.ktor.client.engine.cio.*
import io.ktor.client.engine.proxy.ProxyBuilder
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable

@Serializable data class UserProfileResponse(val response: List<SocialProfile>?)
@Serializable data class SocialProfile(val display_name: String?)
// ... (sisa kode sama persis)
@Serializable data class TasksResponse(val response: TaskData?)
@Serializable data class TaskData(val data: List<Task>?)
@Serializable data class Task(val id: String, val task_name: String, val task_description: String, val task_type: String, val credits_reward: Int, val is_daily: Boolean, val is_one_time: Boolean)

@Serializable data class UserAvailableTasksResponse(val response: UserAvailableTaskData?)
@Serializable data class UserAvailableTaskData(val data: List<UserAvailableTask>?)
@Serializable data class UserAvailableTask(val id: String)

@Serializable data class StatsResponse(val response: StatsData?)
@Serializable data class StatsData(val total_credits: Int?, val leaderboard_rank: Int?)

@Serializable data class SimpleApiResponse(val success: Boolean, val message: String?)

class ApiClient(private val token: String, private val proxy: String?) {

    private val userAgents = listOf(
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.0 Safari/605.1.15"
    )

    private val client = HttpClient(CIO) {
        engine {
            if (proxy != null) {
                this.proxy = ProxyBuilder.url(proxy)
            }
        }
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
        defaultRequest {
            url("https://api.cryptal.ai/apis/v2/")
            header(HttpHeaders.Authorization, "Bearer $token")
            header(HttpHeaders.Origin, "https://api.cryptal.ai")
            header(HttpHeaders.Referrer, "https://api.cryptal.ai/")
            header(HttpHeaders.UserAgent, userAgents.random())
            header(HttpHeaders.Accept, "application/json")
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 60000
        }
    }

    suspend fun <T> requestWithRetry(
        httpMethod: HttpMethod,
        endpoint: String,
        body: Any? = null,
        retries: Int = 3,
        context: String
    ): Result<HttpResponse> {
        var currentRetry = 0
        while (currentRetry < retries) {
            try {
                val response: HttpResponse = client.request(endpoint) {
                    method = httpMethod
                    contentType(ContentType.Application.Json)
                    if (body != null) {
                        setBody(body)
                    }
                }
                if (response.status.isSuccess()) {
                    return Result.success(response)
                }
                UILogger.warn("Request gagal dengan status ${response.status}. Coba lagi (${currentRetry + 1}/$retries)", context = context)
            } catch (e: Exception) {
                UILogger.warn("Request error: ${e.message}. Coba lagi (${currentRetry + 1}/$retries)", context = context)
            }
            currentRetry++
            delay(2000L * currentRetry)
        }
        return Result.failure(Exception("Request gagal setelah $retries percobaan."))
    }

    suspend fun close() {
        client.close()
    }
}
