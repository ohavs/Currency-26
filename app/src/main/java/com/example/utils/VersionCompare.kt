package com.example.utils

/** "v5.0.12" / "5.0.0-dev" -> [5, 0, 12] / [5, 0, 0]: the leading dot-separated numbers. */
private fun versionParts(version: String): List<Int> =
    Regex("""\d+(\.\d+)*""").find(version.removePrefix("v"))?.value
        ?.split('.')
        ?.map { it.toIntOrNull() ?: 0 }
        ?: emptyList()

/** True when [candidate] is a strictly higher version than [installed]. */
fun isNewerVersion(candidate: String, installed: String): Boolean {
    val a = versionParts(candidate)
    val b = versionParts(installed)
    if (a.isEmpty()) return false
    for (i in 0 until maxOf(a.size, b.size)) {
        val x = a.getOrElse(i) { 0 }
        val y = b.getOrElse(i) { 0 }
        if (x != y) return x > y
    }
    return false
}
