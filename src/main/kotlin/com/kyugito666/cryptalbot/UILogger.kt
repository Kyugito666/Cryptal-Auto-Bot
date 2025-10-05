package com.kyugito666.cryptalbot // Diubah

import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.rendering.TextStyles.*
import com.github.ajalt.mordant.terminal.Terminal
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object UILogger {
    private val terminal = Terminal()
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    private fun log(level: String, levelColor: com.github.ajalt.mordant.rendering.TextColor, msg: String, emoji: String = "", context: String = "") {
        val timestamp = gray(LocalDateTime.now().format(formatter))
        val contextStr = if (context.isNotBlank()) white("[${context.padEnd(20)}]") else ""
        val fullMessage = "[ $timestamp ] $emoji${levelColor(level)} $contextStr ${white(msg)}"
        terminal.println(fullMessage)
    }

    fun info(msg: String, emoji: String = "ℹ️ ", context: String = "") = log("INFO ", green, msg, emoji, context)
    fun warn(msg: String, emoji: String = "⚠️ ", context: String = "") = log("WARN ", yellow, msg, emoji, context)
    fun error(msg: String, emoji: String = "❌ ", context: String = "") = log("ERROR", red, msg, emoji, context)
    fun printInfo(label: String, value: String, context: String) {
        info("${label.padEnd(15)}: ${(cyan)(value)}", emoji = "📍 ", context = context)
    }

    fun printHeader(title: String) {
        val width = 80
        val headerColor = magenta
        terminal.println(headerColor("┬${"─".repeat(width - 2)}┬"))
        terminal.println(headerColor("│ ${(bold)(title.padEnd(width - 4))} │"))
        terminal.println(headerColor("┴${"─".repeat(width - 2)}┴"))
    }

    // Banner diubah total
    fun displayBanner() {
        terminal.println((cyan + bold)(" __   __  __   __  __   __  _______  __   __  _______  _______  _______ "))
        terminal.println((cyan + bold)("|  | |  ||  | |  ||  |_|  ||       ||  | |  ||       ||       ||       |"))
        terminal.println((cyan + bold)("|  |_|  ||  | |  ||       ||    ___||  |_|  ||  _____||_     _||    ___|"))
        terminal.println((cyan + bold)("|       ||  |_|  ||       ||   |___ |       || |_____   |   |  |   |___ "))
        terminal.println((cyan + bold)("|_     _||       || ||_|| ||    ___||_     _||_____  |  |   |  |    ___|"))
        terminal.println((cyan + bold)("  |   |  |       || |   | ||   |___   |   |   _____| |  |   |  |   |___ "))
        terminal.println((cyan + bold)("  |___|  |_______||_|   |_||_______|  |___|  |_______|  |___|  |_______|"))
        terminal.println()
        terminal.println(brightMagenta(centerText("=== Created with <3 by Kyugito666 ===")))
        terminal.println(brightMagenta(centerText("✪ BOT CRYPTAL AI AUTO COMPLETE DAILY TASKS ✪")))
        terminal.println()
    }

    private fun centerText(text: String, width: Int = 80): String {
        val padding = (width - text.length).coerceAtLeast(0) / 2
        return " ".repeat(padding) + text
    }
}
