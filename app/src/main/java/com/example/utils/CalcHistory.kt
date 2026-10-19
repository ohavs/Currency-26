package com.example.utils

/** One conversion in the history: an amount (typed, or the result of a calculation) and what it came to. */
data class HistoryEntry(
    val time: Long,
    val source: String,
    val target: String,
    val extra: String?,
    /** The source amount as the keypad keeps it ("1550", "12.5"). */
    val amount: String,
    /** The calculation behind [amount] ("1250+300"), or null when it was typed as is. */
    val expression: String?,
    val targetAmount: Double,
    val extraAmount: Double?,
) {
    /** The same conversion (amount, calculation, currencies), whenever it was made. */
    fun sameAs(other: HistoryEntry): Boolean =
        source == other.source && target == other.target && amount == other.amount && expression == other.expression
}

/** The newest conversions first, without repeats, kept as text in the preferences (one line per entry). */
object CalcHistory {
    const val MAX_ENTRIES = 40

    /** [entry] on top; an earlier copy of the same conversion moves up instead of repeating. */
    fun add(entries: List<HistoryEntry>, entry: HistoryEntry): List<HistoryEntry> =
        (listOf(entry) + entries.filterNot { it.sameAs(entry) }).take(MAX_ENTRIES)

    fun encode(entries: List<HistoryEntry>): String = entries.joinToString("\n") { e ->
        listOf(e.time, e.source, e.target, e.extra.orEmpty(), e.amount, e.expression.orEmpty(), e.targetAmount, e.extraAmount ?: "")
            .joinToString("|")
    }

    fun decode(text: String?): List<HistoryEntry> = text.orEmpty().lines().mapNotNull { line ->
        val f = line.split('|')
        if (f.size != 8) return@mapNotNull null
        HistoryEntry(
            time = f[0].toLongOrNull() ?: return@mapNotNull null,
            source = f[1].ifEmpty { return@mapNotNull null },
            target = f[2].ifEmpty { return@mapNotNull null },
            extra = f[3].ifEmpty { null },
            amount = f[4].ifEmpty { return@mapNotNull null },
            expression = f[5].ifEmpty { null },
            targetAmount = f[6].toDoubleOrNull() ?: return@mapNotNull null,
            extraAmount = f[7].toDoubleOrNull(),
        )
    }
}
