package com.kyugito666.cryptalbot // Diubah

import com.github.ajalt.mordant.rendering.TextColors
// ... (sisa kode sama persis, hanya package yang diubah)
import com.github.ajalt.mordant.terminal.Terminal
import kotlinx.coroutines.*
import me.tongfei.progressbar.ProgressBar
import kotlin.random.Random
import kotlinx.serialization.Serializable

data class ProcessedTask(
    val id: String,
    val name: String,
    val description: String,
    val category: String,
    val creditsReward: Int,
    var status: String
)

suspend fun main() = coroutineScope {
    UILogger.displayBanner()

    val useProxy = askQuestion("🔌 Apakah Anda ingin menggunakan proxy? (y/n): ")
    val proxies = if (useProxy) ProfileManager.loadProxies() else emptyList()
    if (useProxy && proxies.isEmpty()) {
        UILogger.warn("Tidak ada proxy yang tersedia, melanjutkan tanpa proxy.")
    }

    while (true) {
        val profiles = ProfileManager.loadProfiles()
        if (profiles.isEmpty()) {
            UILogger.error("Tidak ada profil yang ditemukan. Keluar.")
            return@coroutineScope
        }

        profiles.forEachIndexed { index, profile ->
            val proxy = if (useProxy && proxies.isNotEmpty()) proxies[index % proxies.size] else null
            try {
                processAccount(profile, index, profiles.size, proxy)
            } catch (e: Exception) {
                UILogger.error("Error pada akun ${profile.name}: ${e.message}", context = "Account ${index + 1}/${profiles.size}")
            }
            if (index < profiles.size - 1) {
                println("\n\n")
                delay(5000)
            }
        }

        UILogger.info("Siklus selesai. Menunggu 24 jam...", emoji = "🔄 ")
        delay(24 * 60 * 60 * 1000) // 24 hours
    }
}

fun askQuestion(query: String): Boolean {
    val terminal = Terminal()
    terminal.println(TextColors.cyan(query))
    val answer = readlnOrNull()?.trim()?.lowercase()
    return answer == "y"
}

suspend fun processAccount(profile: Profile, index: Int, total: Int, proxy: String?) {
    val context = "Akun ${index + 1}/$total | ${profile.name}"
    val client = ApiClient(profile.token, proxy)

    UILogger.info("Memulai proses untuk akun...", emoji = "🚀", context = context)
    UILogger.printHeader("Info Akun - $context")

    val userInfo = fetchUserInfo(client, context)
    UILogger.printInfo("Username", userInfo, context)
    
    val tasks = fetchTasks(client, context)
    if (tasks.isEmpty()) {
        UILogger.warn("Tidak ada tugas yang tersedia untuk diproses.", context = context)
    } else {
        ProgressBar.wrap(tasks.filter { it.status == "pending" }, "Memproses Tugas").use { pb ->
            for (task in pb) {
                pb.extraMessage = task.name
                val result = completeTask(client, task, context)
                if (result) {
                    task.status = "completed"
                }
                delay(Random.nextLong(2000, 5000)) // Random delay between tasks
            }
        }
    }
    
    UILogger.printHeader("Statistik Akun - $context")
    fetchStatistics(client, context)

    client.close()
    UILogger.info("Selesai memproses akun.", emoji = "🎉", context = context)
}
// ... (sisa fungsi sama persis)
suspend fun fetchUserInfo(client: ApiClient, context: String): String {
    val result = client.requestWithRetry<UserProfileResponse>(io.ktor.http.HttpMethod.Get, "auth/social-profiles", context = context)
    return result.fold(
        onSuccess = { response ->
            val data = response.body<UserProfileResponse>()
            data.response?.firstOrNull()?.display_name ?: "Unknown User"
        },
        onFailure = {
            UILogger.error("Gagal mengambil info user: ${it.message}", context = context)
            "Error"
        }
    )
}

suspend fun fetchTasks(client: ApiClient, context: String): List<ProcessedTask> {
    val allTasksResult = client.requestWithRetry<TasksResponse>(io.ktor.http.HttpMethod.Get, "vibe-credit/tasks?take=100&skip=0", context = context)
    val userAvailableResult = client.requestWithRetry<UserAvailableTasksResponse>(io.ktor.http.HttpMethod.Get, "vibe-credit/tasks/user-available?take=100&skip=0", context = context)

    return coroutineScope {
        val allTasks = async { allTasksResult.getOrNull()?.body<TasksResponse>() }
        val availableTasks = async { userAvailableResult.getOrNull()?.body<UserAvailableTasksResponse>() }

        val allTasksData = allTasks.await()?.response?.data ?: emptyList()
        val availableIds = availableTasks.await()?.response?.data?.map { it.id }?.toSet() ?: emptySet()

        allTasksData
            .filter { it.task_type != "invite_friend" && it.task_type != "share_post" }
            .map { task ->
                ProcessedTask(
                    id = task.id,
                    name = task.task_name,
                    description = task.task_description,
                    category = task.task_type,
                    creditsReward = task.credits_reward,
                    status = if (availableIds.contains(task.id)) "pending" else "completed"
                )
            }
    }
}

suspend fun fetchStatistics(client: ApiClient, context: String) {
    val result = client.requestWithRetry<StatsResponse>(io.ktor.http.HttpMethod.Get, "vibe-credit", context = context)
    result.fold(
        onSuccess = { response ->
            val data = response.body<StatsResponse>().response
            UILogger.printInfo("Total Credits", data?.total_credits?.toString() ?: "N/A", context)
            UILogger.printInfo("Leaderboard Rank", data?.leaderboard_rank?.toString() ?: "N/A", context)
        },
        onFailure = {
            UILogger.error("Gagal mengambil statistik: ${it.message}", context = context)
        }
    )
}

@Serializable
data class FeedbackPayload(val feedback: String)
@Serializable
data class WaitlistPayload(val email: String, val first_name: String = "")

suspend fun completeTask(client: ApiClient, task: ProcessedTask, context: String): Boolean {
    val taskContext = "$context|T...${task.id.takeLast(6)}"
    val endpoint: String
    val method: io.ktor.http.HttpMethod
    val body: Any?

    when (task.category) {
        "daily_login" -> { method = io.ktor.http.HttpMethod.Get; endpoint = "vibe-credit/tasks/daily-login"; body = null }
        "follow_cryptal" -> { method = io.ktor.http.HttpMethod.Get; endpoint = "vibe-credit/tasks/follow-cryptal"; body = null }
        "join_discord" -> { method = io.ktor.http.HttpMethod.Get; endpoint = "vibe-credit/tasks/follow-discord"; body = null }
        "join_waitlist" -> {
            method = io.ktor.http.HttpMethod.Post
            endpoint = "vibe-credit/tasks/waitlist"
            body = WaitlistPayload("user${Random.nextInt(10000)}@gmail.com")
        }
        "submit_feedback" -> {
            method = io.ktor.http.HttpMethod.Post
            endpoint = "vibe-credit/tasks/feedback"
            val feedbacks = listOf("Great platform!", "Love the features!", "Very user-friendly.")
            body = FeedbackPayload(feedbacks.random())
        }
        else -> {
            UILogger.warn("Tugas dilewati (tidak didukung): ${task.name}", context = taskContext)
            return false
        }
    }

    val result = client.requestWithRetry<SimpleApiResponse>(method, endpoint, body, context = taskContext)
    return result.fold(
        onSuccess = { response ->
            val apiResponse = response.body<SimpleApiResponse>()
            if (apiResponse.success || apiResponse.message?.contains("already completed") == true) {
                UILogger.info("Verified: ${task.name}", context = taskContext, emoji = "✅")
                true
            } else {
                UILogger.warn("Gagal verifikasi ${task.name}: ${apiResponse.message}", context = taskContext)
                false
            }
        },
        onFailure = {
            UILogger.error("Gagal verifikasi ${task.name}: ${it.message}", context = taskContext)
            false
        }
    )
}
