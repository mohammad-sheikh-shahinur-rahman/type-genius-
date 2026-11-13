package com.example.data.phonetic

object BengaliPhoneticParser {

    private val commonWordMap = mapOf(
        "ami" to listOf("আমি", "আমী"),
        "amar" to listOf("আমার"),
        "amader" to listOf("আমাদের"),
        "amake" to listOf("আমাকে"),
        "tumi" to listOf("তুমি"),
        "tomar" to listOf("তোমার"),
        "tomader" to listOf("তোমাদের"),
        "tomake" to listOf("তোমাকে"),
        "apni" to listOf("আপনি"),
        "apnar" to listOf("আপনার"),
        "kemon" to listOf("কেমন"),
        "acho" to listOf("আছো", "আছ"),
        "achen" to listOf("আছেন"),
        "ache" to listOf("আছে"),
        "bangla" to listOf("বাংলা", "বাঙলা"),
        "bangladesh" to listOf("বাংলাদেশ"),
        "dhonnobad" to listOf("ধন্যবাদ"),
        "dhonobad" to listOf("ধন্যবাদ"),
        "valobashi" to listOf("ভালোবাসি"),
        "bhalobashi" to listOf("ভালোবাসি"),
        "bhalo" to listOf("ভালো", "ভাল"),
        "valo" to listOf("ভালো"),
        "shob" to listOf("সব", "শব"),
        "sob" to listOf("সব"),
        "shuvo" to listOf("শুভ"),
        "subho" to listOf("শুভ"),
        "shokal" to listOf("সকাল"),
        "sokal" to listOf("সকাল"),
        "shondha" to listOf("সন্ধ্যা"),
        "ratri" to listOf("রাত্রি", "রাত"),
        "raat" to listOf("রাত"),
        "desh" to listOf("দেশ"),
        "sonar" to listOf("সোনার"),
        "bondhu" to listOf("বন্ধু"),
        "kotha" to listOf("কথা"),
        "kaj" to listOf("কাজ"),
        "khabar" to listOf("খাবার"),
        "pani" to listOf("পানি"),
        "jol" to listOf("জল"),
        "jal" to listOf("জল", "জাল"),
        "ki" to listOf("কি", "কী"),
        "kee" to listOf("কী"),
        "kobe" to listOf("কবে"),
        "kothay" to listOf("কোথায়"),
        "keno" to listOf("কেন"),
        "kivabe" to listOf("কিভাবে"),
        "thik" to listOf("ঠিক"),
        "khub" to listOf("খুব"),
        "ekta" to listOf("একটা"),
        "onek" to listOf("অনেক"),
        "manush" to listOf("মানুষ"),
        "jibon" to listOf("জীবন"),
        "notun" to listOf("নতুন"),
        "shundor" to listOf("সুন্দর"),
        "sundor" to listOf("সুন্দর"),
        "ghor" to listOf("ঘর"),
        "basha" to listOf("বাসা"),
        "bhasha" to listOf("ভাষা"),
        "basa" to listOf("বাসা"),
        "porashona" to listOf("পড়াশোনা"),
        "shohor" to listOf("শহর"),
        "sohor" to listOf("শহর"),
        "gram" to listOf("গ্রাম"),
        "nodhi" to listOf("নদী"),
        "nodi" to listOf("নদী"),
        "pakhik" to listOf("পাখি"),
        "pakhi" to listOf("পাখি"),
        "gan" to listOf("গান"),
        "gaan" to listOf("গান"),
        "golpo" to listOf("গল্প"),
        "boighor" to listOf("বইঘর"),
        "boi" to listOf("বই"),
        "kolom" to listOf("কলম"),
        "khata" to listOf("খাতা"),
        "shomoy" to listOf("সময়"),
        "somoy" to listOf("সময়"),
        "din" to listOf("দিন"),
        "ajke" to listOf("আজকে", "আজ"),
        "aj" to listOf("আজ"),
        "kal" to listOf("কাল"),
        "kalke" to listOf("কালকে"),
        "shathe" to listOf("সাথে"),
        "sathe" to listOf("সাথে"),
        "shonge" to listOf("সঙ্গে"),
        "songe" to listOf("সঙ্গে"),
        "pore" to listOf("পরে"),
        "age" to listOf("আগে"),
        "khobor" to listOf("খবর"),
        "shikhok" to listOf("শিক্ষক"),
        "chhatro" to listOf("ছাত্র"),
        "bhai" to listOf("ভাই"),
        "bon" to listOf("বোন"),
        "baba" to listOf("বাবা"),
        "ma" to listOf("মা"),
        "dada" to listOf("দাদা"),
        "didi" to listOf("দিদি"),
        "nana" to listOf("নানা"),
        "nani" to listOf("নানী"),
        "ha" to listOf("হ্যাঁ", "হা"),
        "na" to listOf("না"),
        "hobe" to listOf("হবে"),
        "hoyeche" to listOf("হয়েছে"),
        "korchi" to listOf("করছি"),
        "korbo" to listOf("করবো", "করব"),
        "korlam" to listOf("করলাম"),
        "jachhi" to listOf("যাচ্ছি"),
        "jabo" to listOf("যাবো", "যাব"),
        "gechi" to listOf("গেছি"),
        "dekhi" to listOf("দেখি"),
        "dekho" to listOf("দেখো"),
        "dekhte" to listOf("দেখতে"),
        "bolo" to listOf("বলো", "বল"),
        "shuno" to listOf("শুনো", "শোন"),
        "shunlam" to listOf("শুনলাম"),
        "emon" to listOf("এমন"),
        "translator" to listOf("ট্রান্সলেটর"),
        "onubad" to listOf("অনুবাদ"),
        "obostha" to listOf("অবস্থা"),
        "kothay" to listOf("কোথায়", "কোথায়"),
        "koi" to listOf("কই"),
        "jao" to listOf("যাও"),
        "asho" to listOf("আসো"),
        "khao" to listOf("খাও"),
        "bosh" to listOf("বস"),
        "bosho" to listOf("বসো"),
        "valo" to listOf("ভালো", "ভাল"),
        "kharap" to listOf("খারাপ"),
        "shundor" to listOf("সুন্দর"),
        "khushi" to listOf("খুশি"),
        "dukkhito" to listOf("দুঃখিত"),
        "shagotom" to listOf("স্বাগতম"),
        "obhinondon" to listOf("অভিনন্দন"),
        "taka" to listOf("টাকা"),
        "dam" to listOf("দাম"),
        "koto" to listOf("কত"),
        "onek" to listOf("অনেক"),
        "ektu" to listOf("একটু"),
        "ekhon" to listOf("এখন"),
        "ekhane" to listOf("এখানে"),
        "sekhane" to listOf("সেখানে"),
        "taratari" to listOf("তাড়াতাড়ি"),
        "aste" to listOf("আস্তে"),
        "bujhte" to listOf("বুঝতে"),
        "parchi" to listOf("পারছি"),
        "pari" to listOf("পারি"),
        "lagche" to listOf("লাগছে"),
        "khida" to listOf("ক্ষুধা"),
        "bashay" to listOf("বাসায়"),
        "barite" to listOf("বাড়িতে"),
        "nam" to listOf("নাম"),
        "tomader" to listOf("তোমাদের"),
        "amra" to listOf("আমরা"),
        "tara" to listOf("তারা"),
        "se" to listOf("সে"),
        "tar" to listOf("তার"),
        "eta" to listOf("এটা"),
        "ota" to listOf("ওটা"),
        "ei" to listOf("এই"),
        "oi" to listOf("ওই"),
        "ebong" to listOf("এবং"),
        "kintu" to listOf("কিন্তু"),
        "jodi" to listOf("যদি"),
        "tahole" to listOf("তাহলে"),
        "karone" to listOf("কারণে"),
        "jonno" to listOf("জন্য"),
        "theke" to listOf("থেকে"),
        "diye" to listOf("দিয়ে"),
        "niye" to listOf("নিয়ে"),
        "kore" to listOf("করে"),
        "koro" to listOf("করো"),
        "korun" to listOf("করুন"),
        "kori" to listOf("করি"),
        "chai" to listOf("চাই"),
        "lagbe" to listOf("লাগবে"),
        "dorkar" to listOf("দরকার"),
        "shomossa" to listOf("সমস্যা"),
        "nei" to listOf("নেই"),
        "nai" to listOf("নাই"),
        "ache" to listOf("আছে"),
        "chilo" to listOf("ছিল"),
        "mon" to listOf("মন"),
        "prem" to listOf("প্রেম"),
        "bhalobasha" to listOf("ভালোবাসা"),
        "valobasha" to listOf("ভালোবাসা"),
        "shukriya" to listOf("শুকরিয়া")
    )

    private val consonants = mapOf(
        "kkh" to "ক্ষ",
        "jny" to "জ্ঞ",
        "cch" to "চ্ছ",
        "kkh" to "ক্ষ",
        "kh" to "খ",
        "gh" to "ঘ",
        "ng" to "ঙ",
        "ch" to "চ",
        "chh" to "ছ",
        "jh" to "ঝ",
        "th" to "থ",
        "dh" to "ধ",
        "ph" to "ফ",
        "bh" to "ভ",
        "sh" to "শ",
        "shh" to "ষ",
        "rh" to "ঢ়",
        "k" to "ক",
        "g" to "গ",
        "j" to "জ",
        "z" to "য",
        "t" to "ত",
        "T" to "ট",
        "d" to "দ",
        "D" to "ড",
        "n" to "ন",
        "N" to "ণ",
        "p" to "প",
        "f" to "ফ",
        "b" to "ব",
        "v" to "ভ",
        "m" to "ম",
        "r" to "র",
        "R" to "ড়",
        "l" to "ল",
        "s" to "স",
        "S" to "ষ",
        "h" to "হ",
        "y" to "য়",
        "w" to "ও"
    )

    private val initialVowels = mapOf(
        "aa" to "আ",
        "ee" to "ঈ",
        "oo" to "ঊ",
        "oi" to "ঐ",
        "ou" to "ঔ",
        "a" to "অ",
        "A" to "আ",
        "i" to "ই",
        "I" to "ঈ",
        "u" to "উ",
        "U" to "ঊ",
        "e" to "এ",
        "E" to "এ",
        "o" to "ও",
        "O" to "ও"
    )

    private val vowelKars = mapOf(
        "aa" to "া",
        "ee" to "ী",
        "oo" to "ূ",
        "oi" to "ৈ",
        "ou" to "ৌ",
        "a" to "া",
        "A" to "া",
        "i" to "ি",
        "I" to "ী",
        "u" to "ু",
        "U" to "ূ",
        "e" to "ে",
        "E" to "ে",
        "o" to "ো",
        "O" to "ো"
    )

    fun parsePhonetic(input: String): List<String> {
        if (input.isBlank()) return emptyList()
        val lower = input.lowercase().trim()

        val results = mutableListOf<String>()

        // 1. Direct dictionary match
        commonWordMap[lower]?.let {
            results.addAll(it)
        }

        // 2. Prefix matching from common words
        commonWordMap.keys.filter { it.startsWith(lower) && it != lower }
            .take(3)
            .forEach { key ->
                commonWordMap[key]?.firstOrNull()?.let {
                    if (!results.contains(it)) results.add(it)
                }
            }

        // 3. Algorithmic transliteration
        val transliterated = transliterateAlgorithmic(input)
        if (transliterated.isNotBlank() && !results.contains(transliterated)) {
            if (commonWordMap.containsKey(lower)) {
                results.add(transliterated)
            } else {
                results.add(0, transliterated)
            }
        }

        // Always keep the raw English input as a fallback candidate at the end
        if (!results.contains(input)) {
            results.add(input)
        }

        return results
    }

    private fun transliterateAlgorithmic(input: String): String {
        val sb = StringBuilder()
        var i = 0
        var isAfterConsonant = false

        while (i < input.length) {
            // Check 3-char matches
            val sub3 = if (i + 3 <= input.length) input.substring(i, i + 3) else null
            val sub2 = if (i + 2 <= input.length) input.substring(i, i + 2) else null
            val sub1 = input.substring(i, i + 1)

            if (sub3 != null && consonants.containsKey(sub3)) {
                sb.append(consonants[sub3])
                i += 3
                isAfterConsonant = true
                continue
            }

            if (sub2 != null && consonants.containsKey(sub2)) {
                sb.append(consonants[sub2])
                i += 2
                isAfterConsonant = true
                continue
            }

            if (consonants.containsKey(sub1)) {
                sb.append(consonants[sub1])
                i += 1
                isAfterConsonant = true
                continue
            }

            // Vowels
            if (isAfterConsonant) {
                if (sub2 != null && vowelKars.containsKey(sub2)) {
                    sb.append(vowelKars[sub2])
                    i += 2
                    isAfterConsonant = false
                    continue
                }
                if (vowelKars.containsKey(sub1)) {
                    // In Bengali phonetic, 'o' after consonant is often inherent অ, or ো
                    if (sub1 == "o") {
                        sb.append("ো")
                    } else {
                        sb.append(vowelKars[sub1])
                    }
                    i += 1
                    isAfterConsonant = false
                    continue
                }
            } else {
                if (sub2 != null && initialVowels.containsKey(sub2)) {
                    sb.append(initialVowels[sub2])
                    i += 2
                    isAfterConsonant = false
                    continue
                }
                if (initialVowels.containsKey(sub1)) {
                    sb.append(initialVowels[sub1])
                    i += 1
                    isAfterConsonant = false
                    continue
                }
            }

            // Punctuation or special symbols
            when (sub1) {
                "`" -> sb.append("ৎ")
                ":" -> sb.append("ঃ")
                "^" -> sb.append("ঁ")
                "$" -> sb.append("৳")
                "." -> sb.append("।")
                else -> sb.append(sub1)
            }
            i += 1
            isAfterConsonant = false
        }

        return sb.toString()
    }

    private val reverseBengaliMap: Map<String, String> by lazy {
        val map = LinkedHashMap<String, String>()
        for ((roman, bengaliList) in commonWordMap) {
            for (bn in bengaliList) {
                if (!map.containsKey(bn)) {
                    map[bn] = roman
                }
            }
        }
        map["হ্যালো"] = "hello"
        map["হাই"] = "hi"
        map["কেমন"] = "kemon"
        map["আছেন"] = "achen"
        map["ভালোবাসি"] = "valobashi"
        map["দুঃখিত"] = "dukkhito"
        map["স্বাগতম"] = "shagotom"
        map["রাত্রি"] = "ratri"
        map
    }

    fun transliterateSentenceToBengali(banglishInput: String): String {
        val trimmed = banglishInput.trim()
        if (trimmed.isEmpty()) return trimmed
        if (trimmed.none { it in 'a'..'z' || it in 'A'..'Z' }) return trimmed
        return trimmed.split(Regex("\\s+")).joinToString(" ") { raw ->
            val leading = raw.takeWhile { !it.isLetterOrDigit() }
            val trailing = raw.takeLastWhile { !it.isLetterOrDigit() }
            val core = raw.removePrefix(leading).removeSuffix(trailing)
            if (core.isEmpty() || core.none { it in 'a'..'z' || it in 'A'..'Z' }) {
                raw
            } else {
                val mapped = parsePhonetic(core).firstOrNull() ?: core
                "$leading$mapped$trailing"
            }
        }.replace(".", "।")
    }

    fun romanizeBengali(bengaliInput: String): String {
        val trimmed = bengaliInput.trim()
        if (trimmed.isEmpty()) return trimmed
        return trimmed.split(Regex("\\s+")).joinToString(" ") { raw ->
            val leading = raw.takeWhile { !Character.isLetterOrDigit(it) && Character.getType(it).toByte() !in listOf(Character.NON_SPACING_MARK, Character.COMBINING_SPACING_MARK, Character.ENCLOSING_MARK) }
            val trailing = raw.takeLastWhile { !Character.isLetterOrDigit(it) && Character.getType(it).toByte() !in listOf(Character.NON_SPACING_MARK, Character.COMBINING_SPACING_MARK, Character.ENCLOSING_MARK) }
            val core = raw.removePrefix(leading).removeSuffix(trailing)
            if (core.isEmpty()) {
                raw
            } else {
                val mapped = reverseBengaliMap[core] ?: romanizeAlgorithmic(core)
                val fixedTrailing = trailing.replace('।', '.')
                "$leading$mapped$fixedTrailing"
            }
        }
    }

    private fun romanizeAlgorithmic(word: String): String {
        val chars = mapOf(
            'অ' to "o", 'আ' to "a", 'ই' to "i", 'ঈ' to "ee", 'উ' to "u", 'ঊ' to "oo",
            'ঋ' to "ri", 'এ' to "e", 'ঐ' to "oi", 'ও' to "o", 'ঔ' to "ou",
            'া' to "a", 'ি' to "i", 'ী' to "ee", 'ু' to "u", 'ূ' to "oo",
            'ৃ' to "ri", 'ে' to "e", 'ৈ' to "oi", 'ো' to "o", 'ৌ' to "ou",
            'ক' to "k", 'খ' to "kh", 'গ' to "g", 'ঘ' to "gh", 'ঙ' to "ng",
            'চ' to "ch", 'ছ' to "chh", 'জ' to "j", 'ঝ' to "jh", 'ঞ' to "n",
            'ট' to "t", 'ঠ' to "th", 'ড' to "d", 'ঢ' to "dh", 'ণ' to "n",
            'ত' to "t", 'থ' to "th", 'দ' to "d", 'ধ' to "dh", 'ন' to "n",
            'প' to "p", 'ফ' to "f", 'ব' to "b", 'ভ' to "bh", 'ম' to "m",
            'য' to "j", 'র' to "r", 'ল' to "l", 'শ' to "sh", 'ষ' to "sh",
            'স' to "s", 'হ' to "h", 'ড়' to "r", 'ঢ়' to "rh", 'য়' to "y",
            'ৎ' to "t", 'ং' to "ng", 'ঃ' to "h", 'ঁ' to "n", '্' to ""
        )
        val out = StringBuilder()
        for (i in word.indices) {
            val ch = word[i]
            out.append(chars[ch] ?: ch.toString())
        }
        return out.toString()
    }
}
