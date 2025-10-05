package com.kyugito666.cryptalbot // Diubah

import java.io.File
import java.io.IOException

data class Profile(val name: String, val token: String)

object ProfileManager {
    fun loadProfiles(): List<Profile> {
        val profilesDir = File("profiles")
        if (!profilesDir.exists() || !profilesDir.isDirectory) {
            UILogger.error("Direktori 'profiles' tidak ditemukan. Buatlah dan tambahkan subfolder akun di dalamnya.")
            return emptyList()
        }

        val profiles = profilesDir.listFiles { file -> file.isDirectory }?.mapNotNull { dir ->
            val tokenFile = File(dir, "token.txt")
            if (tokenFile.exists()) {
                try {
                    val token = tokenFile.readText().trim()
                    if (token.isNotBlank()) {
                        Profile(dir.name, token)
                    } else {
                        UILogger.warn("File token kosong untuk profil: ${dir.name}")
                        null
                    }
                } catch (e: IOException) {
                    UILogger.error("Gagal membaca token untuk profil ${dir.name}: ${e.message}")
                    null
                }
            } else {
                UILogger.warn("Profil '${dir.name}' tidak memiliki file 'token.txt'.")
                null
            }
        } ?: emptyList()

        if (profiles.isNotEmpty()) {
            UILogger.info("Berhasil memuat ${profiles.size} profil.", emoji = "📄")
        }
        return profiles
    }

    fun loadProxies(): List<String> {
        val proxyFile = File("proxy.txt")
        return try {
            if (proxyFile.exists()) {
                val proxies = proxyFile.readLines().map { it.trim() }.filter { it.isNotEmpty() }
                if (proxies.isNotEmpty()) {
                    UILogger.info("Berhasil memuat ${proxies.size} proxy.", emoji = "🌐")
                } else {
                     UILogger.warn("File 'proxy.txt' ditemukan tapi kosong. Melanjutkan tanpa proxy.", emoji = "⚠️")
                }
                proxies
            } else {
                UILogger.warn("'proxy.txt' tidak ditemukan. Melanjutkan tanpa proxy.", emoji = "⚠️")
                emptyList()
            }
        } catch (e: IOException) {
            UILogger.error("Gagal membaca 'proxy.txt': ${e.message}")
            emptyList()
        }
    }
}
