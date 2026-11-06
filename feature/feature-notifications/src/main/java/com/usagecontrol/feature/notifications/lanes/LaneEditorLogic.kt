package com.usagecontrol.feature.notifications.lanes

import com.usagecontrol.core.model.Lane
import com.usagecontrol.core.model.LanePolicy

/** Editable lane fields; [breakthroughs] are people or keywords that bypass the lane policy. */
data class LaneDraft(
    val id: Long = 0,
    val name: String = "",
    val colorArgb: Int,
    val policy: LanePolicy = LanePolicy.Batched,
    val decayMinutes: Int = 180,
    val sound: String = "",
    val breakthroughs: List<String> = emptyList(),
    val isDefault: Boolean = false,
    val sortOrder: Int = 0,
)

/** Pure lane editor rules: validation and the breakthroughs JSON (a flat array of strings). */
object LaneEditorLogic {
    const val MAX_NAME_LENGTH = 24
    const val MAX_DECAY_MINUTES = 7 * 24 * 60

    /** Decay presets offered in the editor: never, 1 h, 3 h, 12 h, 1 day, 1 week. */
    val decayPresets: List<Int> = listOf(0, 60, 180, 720, 1440, MAX_DECAY_MINUTES)

    fun fromLane(lane: Lane): LaneDraft = LaneDraft(
        id = lane.id,
        name = lane.name,
        colorArgb = lane.colorArgb,
        policy = lane.policy,
        decayMinutes = lane.decayMinutes,
        sound = lane.sound.orEmpty(),
        breakthroughs = decodeBreakthroughs(lane.breakthroughsJson),
        isDefault = lane.isDefault,
        sortOrder = lane.sortOrder,
    )

    fun toLane(draft: LaneDraft): Lane = Lane(
        id = draft.id,
        name = draft.name.trim(),
        colorArgb = draft.colorArgb,
        policy = draft.policy,
        decayMinutes = draft.decayMinutes,
        sound = draft.sound.trim().ifEmpty { null },
        breakthroughsJson = encodeBreakthroughs(draft.breakthroughs),
        isDefault = draft.isDefault,
        sortOrder = draft.sortOrder,
    )

    /** First validation error for [draft] against the other [lanes], or null when it can be saved. */
    fun validate(draft: LaneDraft, lanes: List<Lane>): String? {
        val name = draft.name.trim()
        return when {
            name.isEmpty() -> "Name is required"
            name.length > MAX_NAME_LENGTH -> "Name must be at most $MAX_NAME_LENGTH characters"
            lanes.any { it.id != draft.id && it.name.trim().equals(name, ignoreCase = true) } ->
                "A lane called \"$name\" already exists"
            draft.decayMinutes !in 0..MAX_DECAY_MINUTES -> "Decay must be between 0 and $MAX_DECAY_MINUTES minutes"
            else -> null
        }
    }

    /** New lanes go after every existing lane. */
    fun nextSortOrder(lanes: List<Lane>): Int = (lanes.maxOfOrNull { it.sortOrder } ?: -1) + 1

    /** Comma/newline separated editor text to a de-duplicated, trimmed list. */
    fun parseBreakthroughs(text: String): List<String> =
        text.split(',', '\n').map { it.trim() }.filter { it.isNotEmpty() }.distinctBy { it.lowercase() }

    fun encodeBreakthroughs(items: List<String>): String =
        items.joinToString(prefix = "[", postfix = "]", separator = ",") { "\"" + escape(it) + "\"" }

    /** Decodes a JSON array of strings; non-string elements and malformed JSON yield what parsed so far / empty. */
    fun decodeBreakthroughs(json: String?): List<String> {
        val s = json?.trim().orEmpty()
        if (s.length < 2 || s.first() != '[' || s.last() != ']') return emptyList()
        val out = mutableListOf<String>()
        var i = 1
        while (i < s.length - 1) {
            val c = s[i]
            when {
                c.isWhitespace() || c == ',' -> i++
                c == '"' -> {
                    val sb = StringBuilder()
                    i++
                    var closed = false
                    while (i < s.length - 1) {
                        val ch = s[i]
                        if (ch == '\\' && i + 1 < s.length - 1) {
                            val next = s[i + 1]
                            when (next) {
                                'n' -> sb.append('\n')
                                't' -> sb.append('\t')
                                'u' -> {
                                    val hex = s.substring(i + 2, minOf(i + 6, s.length - 1))
                                    val code = hex.takeIf { it.length == 4 }?.toIntOrNull(16) ?: return out
                                    sb.append(code.toChar())
                                    i += 4
                                }
                                else -> sb.append(next)
                            }
                            i += 2
                        } else if (ch == '"') {
                            closed = true
                            i++
                            break
                        } else {
                            sb.append(ch)
                            i++
                        }
                    }
                    if (!closed) return out
                    out += sb.toString()
                }
                else -> return out
            }
        }
        return out
    }

    private fun escape(s: String): String = buildString {
        for (c in s) when (c) {
            '"' -> append("\\\"")
            '\\' -> append("\\\\")
            '\n' -> append("\\n")
            '\t' -> append("\\t")
            else -> if (c < ' ') append("\\u%04x".format(c.code)) else append(c)
        }
    }

    /** "Never", "45 min", "3 h", "1 d". */
    fun formatDecay(minutes: Int): String = when {
        minutes <= 0 -> "Never"
        minutes % 1440 == 0 -> "${minutes / 1440} d"
        minutes % 60 == 0 -> "${minutes / 60} h"
        else -> "$minutes min"
    }
}
