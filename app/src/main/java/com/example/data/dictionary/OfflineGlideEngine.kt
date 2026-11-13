package com.example.data.dictionary

import com.example.data.phonetic.BengaliPhoneticParser
import com.example.model.LanguageId
import kotlin.math.hypot
import kotlin.math.min

object OfflineGlideEngine {

    private val banglishGlideWords = listOf(
        "ami", "tumi", "apni", "amra", "amar", "tomar", "amader", "bangla", "bangladesh",
        "kemon", "acho", "achen", "bhalo", "valo", "dhonnobad", "bhai", "bon", "bondhu",
        "kotha", "khub", "sundor", "bhalobashi", "valobashi", "sylhet", "sylheti", "din",
        "raat", "shokal", "bikal", "khabar", "pani", "bari", "desh", "manush", "jibon",
        "notun", "shomoy", "ajke", "kal", "sathe", "pore", "thik", "ekta", "onek", "shob",
        "kichu", "keno", "kothay", "kobe", "kivabe", "ma", "baba", "cholo", "jai", "ashi"
    )

    fun resolveGlideCandidates(
        trail: List<Pair<Float, Float>>,
        keyCenters: Map<String, Pair<Float, Float>>,
        vocabulary: List<String>,
        languageId: LanguageId,
        limit: Int = 5
    ): List<String> {
        if (trail.size < 3 || keyCenters.isEmpty()) return emptyList()

        val normalizedCenters = HashMap<Char, Pair<Float, Float>>(keyCenters.size)
        for ((label, pos) in keyCenters) {
            if (label.length == 1) {
                normalizedCenters[label[0].lowercaseChar()] = pos
            }
        }
        if (normalizedCenters.isEmpty()) return emptyList()

        val sampledTrail = resampleTrail(trail, targetCount = 24)
        val startPoint = sampledTrail.first()
        val endPoint = sampledTrail.last()

        val totalSpan = hypot(endPoint.first - startPoint.first, endPoint.second - startPoint.second)
        val pathLength = computePathLength(sampledTrail)
        if (pathLength < 30f && totalSpan < 25f) return emptyList()

        val approxKeyPitch = estimateKeyPitch(normalizedCenters)
        val maxAnchorRadius = approxKeyPitch * 2.2f

        val candidateSource: List<String> = if (languageId == LanguageId.BANGLA_PHONETIC) {
            (banglishGlideWords + vocabulary).distinct()
        } else {
            vocabulary.distinct()
        }

        val scored = ArrayList<Pair<String, Float>>()

        for (rawWord in candidateSource) {
            val clean = rawWord.trim()
            if (clean.length < 2) continue
            val lower = clean.lowercase()

            // Verify first and last letter exist in visible key layout
            val firstCenter = normalizedCenters[lower.first()] ?: continue
            val lastCenter = normalizedCenters[lower.last()] ?: continue

            val startDist = dist(startPoint, firstCenter)
            val endDist = dist(endPoint, lastCenter)
            if (startDist > maxAnchorRadius || endDist > maxAnchorRadius) continue

            // Check remaining chars exist on keyboard
            var missingCount = 0
            val wordCenters = ArrayList<Pair<Float, Float>>(lower.length)
            for (ch in lower) {
                val c = normalizedCenters[ch]
                if (c == null) {
                    missingCount++
                } else {
                    wordCenters.add(c)
                }
            }
            if (missingCount > 1 || wordCenters.size < 2) continue

            // Order-preserving monotonic alignment score along sampledTrail
            val alignmentCost = computeMonotonicPathCost(wordCenters, sampledTrail)
            val lengthPenalty = kotlin.math.abs(lower.length - estimateDistinctKeyCount(sampledTrail, normalizedCenters)) * (approxKeyPitch * 0.14f)
            val totalScore = (startDist * 1.35f) + (endDist * 1.35f) + (alignmentCost * 2.0f) + lengthPenalty

            if (alignmentCost <= approxKeyPitch * 1.85f) {
                scored.add(clean to totalScore)
            }
        }

        val topRaw = scored
            .sortedBy { it.second }
            .map { it.first }
            .distinct()
            .take(limit)

        if (languageId == LanguageId.BANGLA_PHONETIC && topRaw.isNotEmpty()) {
            val mapped = mutableListOf<String>()
            for (latin in topRaw) {
                if (latin in banglishGlideWords) {
                    BengaliPhoneticParser.parsePhonetic(latin).firstOrNull()?.let { mapped.add(it) }
                } else {
                    mapped.add(latin)
                    BengaliPhoneticParser.parsePhonetic(latin).firstOrNull()?.let {
                        if (it != latin) mapped.add(it)
                    }
                }
            }
            return mapped.distinct().take(limit)
        }

        return topRaw
    }

    private fun computeMonotonicPathCost(
        wordCenters: List<Pair<Float, Float>>,
        trail: List<Pair<Float, Float>>
    ): Float {
        var trailIdx = 0
        var sumDist = 0f
        val lastIdx = trail.lastIndex
        for (i in wordCenters.indices) {
            val target = wordCenters[i]
            val remainingLetters = wordCenters.size - 1 - i
            val maxSearchIdx = (lastIdx - min(remainingLetters, lastIdx)).coerceAtLeast(trailIdx)
            var bestDist = Float.MAX_VALUE
            var bestIdx = trailIdx
            for (j in trailIdx..maxSearchIdx) {
                val d = dist(target, trail[j])
                if (d < bestDist) {
                    bestDist = d
                    bestIdx = j
                }
            }
            sumDist += bestDist
            trailIdx = bestIdx
        }
        return sumDist / wordCenters.size
    }

    private fun estimateDistinctKeyCount(
        trail: List<Pair<Float, Float>>,
        centers: Map<Char, Pair<Float, Float>>
    ): Int {
        var prev: Char? = null
        var count = 0
        for (pt in trail) {
            var nearest: Char? = null
            var bestD = Float.MAX_VALUE
            for ((ch, center) in centers) {
                val d = dist(pt, center)
                if (d < bestD) {
                    bestD = d
                    nearest = ch
                }
            }
            if (nearest != null && nearest != prev) {
                count++
                prev = nearest
            }
        }
        return count
    }

    private fun estimateKeyPitch(centers: Map<Char, Pair<Float, Float>>): Float {
        val values = centers.values.toList()
        if (values.size < 2) return 75f
        var minDist = Float.MAX_VALUE
        for (i in 0 until min(values.size, 15)) {
            for (j in i + 1 until min(values.size, 15)) {
                val d = dist(values[i], values[j])
                if (d > 10f && d < minDist) {
                    minDist = d
                }
            }
        }
        return if (minDist < 400f) minDist else 75f
    }

    private fun resampleTrail(trail: List<Pair<Float, Float>>, targetCount: Int): List<Pair<Float, Float>> {
        if (trail.size <= targetCount) return trail
        val result = ArrayList<Pair<Float, Float>>(targetCount)
        val step = (trail.size - 1).toFloat() / (targetCount - 1)
        for (i in 0 until targetCount) {
            val idx = min((i * step).toInt(), trail.lastIndex)
            result.add(trail[idx])
        }
        return result
    }

    private fun computePathLength(trail: List<Pair<Float, Float>>): Float {
        var total = 0f
        for (i in 1 until trail.size) {
            total += dist(trail[i - 1], trail[i])
        }
        return total
    }

    private fun dist(a: Pair<Float, Float>, b: Pair<Float, Float>): Float {
        return hypot(a.first - b.first, a.second - b.second)
    }
}
