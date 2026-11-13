package com.example.data.dictionary

import com.example.model.LanguageId
import java.util.concurrent.ConcurrentHashMap

object BigramPredictionEngine {

    private val dynamicBigrams = ConcurrentHashMap<String, MutableList<String>>()

    private val bengaliBigrams: Map<String, List<String>> = mapOf(
        "আমি" to listOf("ভালোবাসি", "এখন", "তোমাকে", "যাচ্ছি", "করছি", "ভালো"),
        "আমার" to listOf("নাম", "বাড়ি", "দেশ", "বন্ধু", "মনে", "সব"),
        "আমরা" to listOf("সবাই", "এখন", "একসাথে", "বাংলাদেশী"),
        "তুমি" to listOf("কেমন", "কোথায়", "এখন", "কি", "আমার"),
        "তোমার" to listOf("নাম", "বাড়ি", "সাথে", "কথা", "জন্য"),
        "আপনি" to listOf("কেমন", "কোথায়", "কখন", "কিভাবে", "আছেন"),
        "আপনার" to listOf("নাম", "সাথে", "জন্য", "কথা", "বাড়ি"),
        "কেমন" to listOf("আছেন", "আছো", "লাগছে", "হলো"),
        "ভালো" to listOf("আছি", "আছেন", "লাগছে", "বাসি", "থেকো"),
        "শুভ" to listOf("সকাল", "রাত্রি", "সন্ধ্যা", "জন্মদিন", "নববর্ষ"),
        "আসসালামু" to listOf("আলাইকুম"),
        "ওয়ালাইকুমুস" to listOf("সালাম"),
        "আফনে" to listOf("ভালা", "কই", "কিতা"),
        "ভালা" to listOf("আছইননি", "আছি", "লাগলো"),
        "অনেক" to listOf("ধন্যবাদ", "সুন্দর", "ভালো", "সময়"),
        "এখন" to listOf("কি", "কোথায়", "বাড়ি", "কাজ", "সময়"),
        "কী" to listOf("করছো", "খবর", "অবস্থা", "হয়েছে"),
        "কি" to listOf("করছো", "খবর", "হলো", "অবস্থা"),
        "কোথায়" to listOf("আছো", "যাচ্ছো", "আছেন", "থাকো"),
        "বাংলাদেশ" to listOf("আমার", "জিন্দাবাদ", "সুন্দর", "ক্রিকেট"),
        "ইনশাআল্লাহ" to listOf("হবে", "ভালো", "দেখা"),
        "ধন্যবাদ" to listOf("ভাই", "আপনাকে", "অনেক", "বন্ধু"),
        "একটু" to listOf("অপেক্ষা", "পরে", "সময়", "পানি"),
        "সব" to listOf("সময়", "কিছু", "ঠিক", "ভালো")
    )

    private val englishBigrams: Map<String, List<String>> = mapOf(
        "i" to listOf("am", "will", "have", "love", "need", "think"),
        "i'm" to listOf("fine", "good", "sorry", "ready", "coming", "happy"),
        "you" to listOf("are", "can", "have", "will", "know", "want"),
        "how" to listOf("are", "is", "much", "about", "many", "do"),
        "are" to listOf("you", "we", "they", "going", "ready"),
        "thank" to listOf("you", "God", "everyone"),
        "thanks" to listOf("a", "for", "so", "again"),
        "good" to listOf("morning", "night", "luck", "job", "evening", "afternoon"),
        "have" to listOf("a", "you", "to", "fun", "some"),
        "nice" to listOf("to", "day", "work", "meeting"),
        "see" to listOf("you", "soon", "tomorrow", "later"),
        "what" to listOf("is", "are", "about", "time", "happened"),
        "where" to listOf("are", "is", "do", "did", "can"),
        "please" to listOf("wait", "send", "help", "check", "call"),
        "on" to listOf("my", "the", "time", "your"),
        "my" to listOf("friend", "name", "phone", "home", "work"),
        "love" to listOf("you", "this", "it", "bangladesh"),
        "happy" to listOf("birthday", "new", "to", "holiday"),
        "don't" to listOf("worry", "know", "forget", "mind"),
        "let" to listOf("me", "us", "it", "go")
    )

    fun learnPair(prevWord: String, nextWord: String) {
        val prev = prevWord.trim().lowercase()
        val next = nextWord.trim()
        if (prev.length < 2 || next.isBlank() || prev == next.lowercase()) return
        val list = dynamicBigrams.getOrPut(prev) { mutableListOf() }
        synchronized(list) {
            list.removeAll { it.equals(next, ignoreCase = true) }
            list.add(0, next)
            if (list.size > 6) {
                list.subList(6, list.size).clear()
            }
        }
    }

    fun predictNextWords(
        previousWord: String,
        languageId: LanguageId,
        limit: Int = 4
    ): List<String> {
        val clean = previousWord.trim().trimEnd(',', '.', '।', '!', '?')
        if (clean.isBlank()) return emptyList()

        val lower = clean.lowercase()
        val results = mutableListOf<String>()

        dynamicBigrams[lower]?.let { learned ->
            synchronized(learned) {
                results.addAll(learned)
            }
        }

        val isBanglaFamily = languageId in setOf(
            LanguageId.BANGLA_PROBHAT,
            LanguageId.BANGLA_PHONETIC,
            LanguageId.BANGLA_NATIONAL,
            LanguageId.BANGLA_UNIBIJOY,
            LanguageId.SYLHETI
        )

        if (isBanglaFamily) {
            bengaliBigrams[clean]?.let { results.addAll(it) }
            englishBigrams[lower]?.let { results.addAll(it) }
        } else {
            englishBigrams[lower]?.let { results.addAll(it) }
            bengaliBigrams[clean]?.let { results.addAll(it) }
        }

        return results.distinct().take(limit)
    }

    fun extractPreviousWordBeforeCursor(textBeforeCursor: String): String {
        if (textBeforeCursor.isEmpty() || !textBeforeCursor.last().isWhitespace()) return ""
        val trimmed = textBeforeCursor.trimEnd()
        if (trimmed.isEmpty()) return ""
        return trimmed.takeLastWhile { !it.isWhitespace() }
    }
}
