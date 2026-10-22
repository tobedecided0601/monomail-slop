package com.shrivatsav.monomail.util

import java.util.concurrent.CopyOnWriteArrayList

object AppLog {
    private const val MAX_ENTRIES = 1000
    private val entries = CopyOnWriteArrayList<String>()

    fun d(tag: String, message: String) = append("D", tag, message)
    fun w(tag: String, message: String) = append("W", tag, message)
    fun e(tag: String, message: String) = append("E", tag, message)

    fun snapshot(): List<String> = entries.toList()

    fun clear() {
        entries.clear()
    }

    private fun append(level: String, tag: String, message: String) {
        entries += "${System.currentTimeMillis()} $level/$tag: $message"
        while (entries.size > MAX_ENTRIES) entries.removeAt(0)
    }
}
