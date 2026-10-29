package com.jayv.dontscroll.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * What the tracker saw and decided, kept in memory only, for the Detection check screen.
 * Holds view IDs and decisions, never screen text.
 */
object DebugLog {
    data class Entry(val ts: Long, val app: String, val msg: String)

    private const val MAX = 300
    private val entries = ArrayDeque<Entry>()
    private val _version = MutableStateFlow(0L)
    val version: StateFlow<Long> get() = _version

    @Volatile var serviceRunning: Boolean = false
    @Volatile var foregroundApp: String? = null
    @Volatile var surfaceApp: String? = null
    @Volatile var lastHow: String = "-"
    @Volatile var lastScrollId: String = "-"

    @Synchronized
    fun add(app: String, msg: String) {
        entries.addLast(Entry(System.currentTimeMillis(), app, msg))
        while (entries.size > MAX) entries.removeAt(0)
        _version.value = _version.value + 1
    }

    @Synchronized
    fun snapshot(): List<Entry> = entries.toList().asReversed()

    @Synchronized
    fun clear() {
        entries.clear()
        _version.value = _version.value + 1
    }

    fun asText(): String {
        val f = SimpleDateFormat("HH:mm:ss", Locale.US)
        val sb = StringBuilder()
        sb.append("DontScroll detection log\n")
        sb.append("service=").append(serviceRunning)
            .append(" foreground=").append(foregroundApp)
            .append(" surface=").append(surfaceApp)
            .append(" how=").append(lastHow)
            .append(" lastScrollId=").append(lastScrollId).append('\n')
        snapshot().forEach { e ->
            sb.append(f.format(Date(e.ts))).append(' ').append(e.app).append(" | ").append(e.msg).append('\n')
        }
        return sb.toString()
    }
}
