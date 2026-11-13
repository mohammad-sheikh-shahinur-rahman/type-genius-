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
        "achi" to listOf("আছি"),
        "bangla" to listOf("বাংলা", "বাঙলা"),
        "bangladesh" to listOf("বাংলাদেশ"),
        "dhaka" to listOf("ঢাকা"),
        "sylhet" to listOf("সিলেট"),
        "sylheti" to listOf("সিলেটি"),
        "dhonnobad" to listOf("ধন্যবাদ"),
        "dhonobad" to listOf("ধন্যবাদ"),
        "valobashi" to listOf("ভালোবাসি"),
        "bhalobashi" to listOf("ভালোবাসি"),
        "bhalo" to listOf("ভালো", "ভাল"),
        "valo" to listOf("ভালো", "ভাল"),
        "shob" to listOf("সব", "শব"),
        "sob" to listOf("সব"),
        "shuvo" to listOf("শুভ"),
        "subho" to listOf("শুভ"),
        "shokal" to listOf("সকাল"),
        "sokal" to listOf("সকাল"),
        "shondha" to listOf("সন্ধ্যা"),
        "sondha" to listOf("সন্ধ্যা"),
        "ratri" to listOf("রাত্রি", "রাত"),
        "raat" to listOf("রাত"),
        "desh" to listOf("দেশ"),
        "sonar" to listOf("সোনার"),
        "bondhu" to listOf("বন্ধু"),
        "kotha" to listOf("কথা"),
        "kobita" to listOf("কবিতা"),
        "kaj" to listOf("কাজ"),
        "khabar" to listOf("খাবার"),
        "pani" to listOf("পানি"),
        "jol" to listOf("জল"),
        "jal" to listOf("জল", "জাল"),
        "ki" to listOf("কি", "কী"),
        "kee" to listOf("কী"),
        "kobe" to listOf("কবে"),
        "kothay" to listOf("কোথায়", "কোথায়"),
        "keno" to listOf("কেন"),
        "kivabe" to listOf("কিভাবে", "কীভাবে"),
        "thik" to listOf("ঠিক"),
        "khub" to listOf("খুব"),
        "ekta" to listOf("একটা"),
        "ekti" to listOf("একটি"),
        "onek" to listOf("অনেক"),
        "manush" to listOf("মানুষ"),
        "jibon" to listOf("জীবন"),
        "notun" to listOf("নতুন"),
        "shundor" to listOf("সুন্দর"),
        "sundor" to listOf("সুন্দর"),
        "ghor" to listOf("ঘর"),
        "bari" to listOf("বাড়ি", "বাড়ি"),
        "basha" to listOf("বাসা"),
        "bhasha" to listOf("ভাষা"),
        "basa" to listOf("বাসা"),
        "porashona" to listOf("পড়াশোনা"),
        "shohor" to listOf("শহর"),
        "sohor" to listOf("শহর"),
        "gram" to listOf("গ্রাম"),
        "nodhi" to listOf("নদী"),
        "nodi" to listOf("নদী"),
        "pakhi" to listOf("পাখি"),
        "pakhik" to listOf("পাখি"),
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
        "ekshathe" to listOf("একসাথে"),
        "eksathe" to listOf("একসাথে"),
        "pore" to listOf("পরে"),
        "age" to listOf("আগে"),
        "khobor" to listOf("খবর"),
        "shikhok" to listOf("শিক্ষক"),
        "chhatro" to listOf("ছাত্র"),
        "bhai" to listOf("ভাই"),
        "vai" to listOf("ভাই"),
        "bon" to listOf("বোন"),
        "baba" to listOf("বাবা"),
        "ma" to listOf("মা"),
        "dada" to listOf("দাদা"),
        "didi" to listOf("দিদি"),
        "nana" to listOf("নানা"),
        "nani" to listOf("নানী"),
        "ha" to listOf("হ্যাঁ", "হা"),
        "hya" to listOf("হ্যাঁ"),
        "na" to listOf("না"),
        "nai" to listOf("নাই"),
        "nei" to listOf("নেই"),
        "hobe" to listOf("হবে"),
        "hoyeche" to listOf("হয়েছে"),
        "korchi" to listOf("করছি"),
        "korbo" to listOf("করবো", "করব"),
        "korlam" to listOf("করলাম"),
        "korben" to listOf("করবেন"),
        "jachhi" to listOf("যাচ্ছি"),
        "jabo" to listOf("যাবো", "যাব"),
        "gechi" to listOf("গেছি"),
        "dekhi" to listOf("দেখি"),
        "dekho" to listOf("দেখো"),
        "dekhte" to listOf("দেখতে"),
        "bolo" to listOf("বলো", "বল"),
        "cholo" to listOf("চলো", "চল"),
        "shuno" to listOf("শুনো", "শোন"),
        "shunlam" to listOf("শুনলাম"),
        "emon" to listOf("এমন"),
        "kemon" to listOf("কেমন"),
        "translator" to listOf("ট্রান্সলেটর"),
        "onubad" to listOf("অনুবাদ"),
        "obostha" to listOf("অবস্থা"),
        "koi" to listOf("কই"),
        "jao" to listOf("যাও"),
        "asho" to listOf("আসো"),
        "khao" to listOf("খাও"),
        "bosh" to listOf("বস"),
        "bosho" to listOf("বসো"),
        "kharap" to listOf("খারাপ"),
        "khushi" to listOf("খুশি"),
        "dukkhito" to listOf("দুঃখিত"),
        "shagotom" to listOf("স্বাগতম"),
        "sagotom" to listOf("স্বাগতম"),
        "obhinondon" to listOf("অভিনন্দন"),
        "taka" to listOf("টাকা"),
        "dam" to listOf("দাম"),
        "koto" to listOf("কত"),
        "ektu" to listOf("একটু"),
        "ekhon" to listOf("এখন"),
        "ekhane" to listOf("এখানে"),
        "sekhane" to listOf("সেখানে"),
        "taratari" to listOf("তাড়াতাড়ি"),
        "aste" to listOf("আস্তে"),
        "bujhte" to listOf("বুঝতে"),
        "parchi" to listOf("পারছি"),
        "parbo" to listOf("পারবো", "পারব"),
        "pari" to listOf("পারি"),
        "lagche" to listOf("লাগছে"),
        "khida" to listOf("ক্ষুধা"),
        "bashay" to listOf("বাসায়"),
        "barite" to listOf("বাড়িতে"),
        "nam" to listOf("নাম"),
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
        "koro" to listOf("করো", "কর"),
        "korun" to listOf("করুন"),
        "kori" to listOf("করি"),
        "chai" to listOf("চাই"),
        "lagbe" to listOf("লাগবে"),
        "dorkar" to listOf("দরকার"),
        "shomossa" to listOf("সমস্যা"),
        "somossa" to listOf("সমস্যা"),
        "shomadhan" to listOf("সমাধান"),
        "somadhan" to listOf("সমাধান"),
        "chilo" to listOf("ছিল"),
        "mon" to listOf("মন"),
        "prem" to listOf("প্রেম"),
        "bhalobasha" to listOf("ভালোবাসা"),
        "valobasha" to listOf("ভালোবাসা"),
        "shukriya" to listOf("শুকরিয়া"),
        "prithibi" to listOf("পৃথিবী"),
        "krishi" to listOf("কৃষি"),
        "bristi" to listOf("বৃষ্টি"),
        "brishti" to listOf("বৃষ্টি"),
        "hridoy" to listOf("হৃদয়"),
        "rito" to listOf("ঋতু"),
        "prothom" to listOf("প্রথম"),
        "proshno" to listOf("প্রশ্ন"),
        "proyojon" to listOf("প্রয়োজন"),
        "byabohar" to listOf("ব্যবহার"),
        "bebohar" to listOf("ব্যবহার"),
        "byasto" to listOf("ব্যস্ত"),
        "besto" to listOf("ব্যস্ত"),
        "satya" to listOf("সত্য"),
        "sotto" to listOf("সত্য"),
        "dhormo" to listOf("ধর্ম"),
        "kormo" to listOf("কর্ম"),
        "shurjo" to listOf("সূর্য"),
        "surjo" to listOf("সূর্য"),
        "bortoman" to listOf("বর্তমান"),
        "porjonto" to listOf("পর্যন্ত"),
        "bornona" to listOf("বর্ণনা"),
        "karjo" to listOf("কার্য"),
        "nirbachon" to listOf("নির্বাচন"),
        "sorkar" to listOf("সরকার"),
        "shorkar" to listOf("সরকার"),
        "kosto" to listOf("কষ্ট"),
        "koshto" to listOf("কষ্ট"),
        "nischoy" to listOf("নিশ্চয়"),
        "oboshshoi" to listOf("অবশ্যই"),
        "obosshoi" to listOf("অবশ্যই"),
        "chinta" to listOf("চিন্তা"),
        "shikkha" to listOf("শিক্ষা"),
        "biggan" to listOf("বিজ্ঞান"),
        "onchol" to listOf("অঞ্চল"),
        "hotath" to listOf("হঠাৎ"),
        "utshob" to listOf("উৎসব"),
        "hothat" to listOf("হঠাৎ"),
        "srishti" to listOf("সৃষ্টি"),
        "drishti" to listOf("দৃষ্টি"),
        "swadhin" to listOf("স্বাধীন"),
        "swopno" to listOf("স্বপ্ন"),
        "shopno" to listOf("স্বপ্ন"),
        "bissash" to listOf("বিশ্বাস"),
        "bishwash" to listOf("বিশ্বাস"),
        "shongbad" to listOf("সংবাদ"),
        "songbad" to listOf("সংবাদ"),
        "rong" to listOf("রং"),
        "rongin" to listOf("রঙিন"),
        "tomra" to listOf("তোমরা"),
        "onno" to listOf("অন্য"),
        "onurodh" to listOf("অনুরোধ"),
        "nomoshkar" to listOf("নমস্কার"),
        "assalamu" to listOf("আসসালামু"),
        "alaikum" to listOf("আলাইকুম"),
        "inshallah" to listOf("ইনশাআল্লাহ"),
        "alhamdulillah" to listOf("আলহামদুলিল্লাহ"),
        "akash" to listOf("আকাশ"),
        "alo" to listOf("আলো"),
        "torko" to listOf("তর্ক"),
        "borsha" to listOf("বর্ষা"),
        "sworgo" to listOf("স্বর্গ"),
        "gorbo" to listOf("গর্ব"),
        "uttor" to listOf("উত্তর"),
        "proshongsha" to listOf("প্রশংসা"),
        "bikkhato" to listOf("বিখ্যাত"),
        "onubhuti" to listOf("অনুভূতি"),
        "odhikar" to listOf("অধিকার"),
        "obak" to listOf("অবাক")
    )

    // Ordered 4 -> 3 -> 2 -> 1 char consonant/conjunct mappings
    private val multiCharConsonants = linkedMapOf(
        "cchh" to "চ্ছ",
        "shTh" to "ষ্ঠ",
        "ngsh" to "ংশ",
        "ngsk" to "ংস্ক",
        "kkh" to "ক্ষ",
        "kSh" to "ক্ষ",
        "jny" to "জ্ঞ",
        "ggn" to "জ্ঞ",
        "cch" to "চ্ছ",
        "chh" to "ছ",
        "nch" to "ঞ্ছ",
        "ndh" to "ন্ধ",
        "nth" to "ন্থ",
        "sth" to "স্থ",
        "sTh" to "ষ্ঠ",
        "shT" to "ষ্ট",
        "shN" to "ষ্ণ",
        "shm" to "শ্ম",
        "shr" to "শ্র",
        "shw" to "শ্ব",
        "shl" to "শ্ল",
        "shh" to "ষ",
        "mbh" to "ম্ভ",
        "ngk" to "ঙ্ক",
        "ngg" to "ঙ্গ",
        "ddh" to "দ্ধ",
        "tth" to "ত্থ",
        "bhr" to "ভ্র",
        "ghr" to "ঘ্র",
        "khr" to "খ্র",
        "bhy" to "ভ্য",
        "kh" to "খ",
        "gh" to "ঘ",
        "ng" to "ঙ",
        "NG" to "ঞ",
        "ch" to "চ",
        "jh" to "ঝ",
        "Th" to "ঠ",
        "Dh" to "ঢ",
        "th" to "থ",
        "dh" to "ধ",
        "ph" to "ফ",
        "bh" to "ভ",
        "sh" to "শ",
        "rh" to "ঢ়",
        "nt" to "ন্ত",
        "nd" to "ন্দ",
        "st" to "স্ত",
        "sT" to "ষ্ট",
        "kt" to "ক্ত",
        "kT" to "ক্ট",
        "mp" to "ম্প",
        "mb" to "ম্ব",
        "nk" to "ঙ্ক",
        "nc" to "ঞ্চ",
        "nj" to "ঞ্জ",
        "nn" to "ন্ন",
        "mm" to "ম্ম",
        "tt" to "ত্ত",
        "TT" to "ট্ট",
        "kk" to "ক্ক",
        "dd" to "দ্দ",
        "ll" to "ল্ল",
        "pp" to "প্প",
        "bb" to "ব্ব",
        "jj" to "জ্জ",
        "sp" to "স্প",
        "sk" to "স্ক",
        "sm" to "স্ম",
        "sn" to "স্ন",
        "nm" to "ন্ম",
        "tm" to "ত্ম",
        "gm" to "গ্ম",
        "gn" to "গ্ন",
        "bd" to "ব্দ",
        "pt" to "প্ত",
        "dg" to "দ্গ",
        "sw" to "স্ব",
        "dw" to "দ্ব",
        "tw" to "ত্ব",
        "tr" to "ত্র",
        "kr" to "ক্র",
        "pr" to "প্র",
        "gr" to "গ্র",
        "br" to "ব্র",
        "dr" to "দ্র",
        "sr" to "স্র",
        "hr" to "হ্র",
        "hm" to "হ্ম",
        "hy" to "হ্য",
        "hl" to "হ্ল"
    )

    private val singleConsonants = mapOf(
        "k" to "ক",
        "g" to "গ",
        "c" to "চ",
        "q" to "ক",
        "j" to "জ",
        "z" to "য",
        "Z" to "\u09CD\u09AF",
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
        "w" to "ও",
        "x" to "ক্স"
    )

    private val initialVowels = mapOf(
        "rri" to "ঋ",
        "aa" to "আ",
        "ee" to "ঈ",
        "oo" to "ঊ",
        "oi" to "ঐ",
        "ou" to "ঔ",
        "a" to "আ",
        "A" to "আ",
        "i" to "ই",
        "I" to "ঈ",
        "u" to "উ",
        "U" to "ঊ",
        "e" to "এ",
        "E" to "এ",
        "o" to "অ",
        "O" to "ও"
    )

    private val vowelKars = mapOf(
        "rri" to "ৃ",
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
        "o" to "",
        "O" to "ো"
    )

    private val nonSpecialAvroCaps = setOf('B', 'C', 'E', 'F', 'G', 'H', 'J', 'K', 'L', 'M', 'P', 'Q', 'V', 'W', 'X', 'Y')

    private fun normalizeCaseForAvro(input: String): String {
        return buildString(input.length) {
            for (idx in input.indices) {
                val ch = input[idx]
                val isAvroNg = ch == 'G' && idx > 0 && input[idx - 1] == 'N'
                append(if (ch in nonSpecialAvroCaps && !isAvroNg) ch.lowercaseChar() else ch)
            }
        }
    }

    fun parsePhonetic(input: String): List<String> {
        if (input.isBlank()) return emptyList()
        val trimmed = input.trim()
        val lower = trimmed.lowercase()

        val results = mutableListOf<String>()

        // 1. Direct dictionary match (top priority for exact idioms/common Banglish words)
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

        // 3. Algorithmic transliteration (Avro rules + conjuncts + inherent 'o')
        val normalized = normalizeCaseForAvro(trimmed)
        val transliterated = transliterateAlgorithmic(normalized, trailingOAsOkar = false)
        val rephVariant = transliterateAlgorithmic(normalized, trailingOAsOkar = false, preferRephOnR = true)
        val trailingOVariant = if (normalized.endsWith("o") && !normalized.endsWith("oo")) {
            transliterateAlgorithmic(normalized, trailingOAsOkar = true)
        } else null
        val loweredTranslit = if (normalized != lower) {
            transliterateAlgorithmic(lower, trailingOAsOkar = false)
        } else null

        val algoCandidates = listOfNotNull(
            transliterated,
            trailingOVariant,
            rephVariant,
            loweredTranslit
        ).filter { it.isNotBlank() }.distinct()

        if (commonWordMap.containsKey(lower)) {
            algoCandidates.forEach { if (!results.contains(it)) results.add(it) }
        } else {
            algoCandidates.reversed().forEach { cand ->
                if (!results.contains(cand)) results.add(0, cand)
            }
        }

        // Always keep the raw English input as a fallback candidate at the end
        if (!results.contains(trimmed)) {
            results.add(trimmed)
        }

        return results
    }

    private fun isVowelChar(c: Char?): Boolean {
        return c != null && c in charArrayOf('a', 'e', 'i', 'o', 'u', 'A', 'E', 'I', 'O', 'U')
    }

    private fun isConsonantStart(input: String, idx: Int): Boolean {
        if (idx >= input.length) return false
        val ch = input[idx]
        if (isVowelChar(ch)) return false
        return ch.isLetter()
    }

    private fun transliterateAlgorithmic(
        input: String,
        trailingOAsOkar: Boolean = false,
        preferRephOnR: Boolean = false
    ): String {
        val sb = StringBuilder()
        var i = 0
        var isAfterConsonant = false

        while (i < input.length) {
            val sub4 = if (i + 4 <= input.length) input.substring(i, i + 4) else null
            val sub3 = if (i + 3 <= input.length) input.substring(i, i + 3) else null
            val sub2 = if (i + 2 <= input.length) input.substring(i, i + 2) else null
            val sub1 = input.substring(i, i + 1)

            // Explicit hasanta (,,)
            if (sub2 == ",,") {
                sb.append("্")
                i += 2
                isAfterConsonant = false
                continue
            }

            // Explicit Reph in strict Avro: 'rr' followed by consonant (not rri)
            if (sub2 == "rr" && sub3 != "rri" && isConsonantStart(input, i + 2)) {
                sb.append("\u09B0\u09CD")
                i += 2
                isAfterConsonant = false
                continue
            }

            // Vowel kar/initial 'rri' takes priority over 'r'+'r'+'i'
            if (sub3 == "rri") {
                sb.append(if (isAfterConsonant) "ৃ" else "ঋ")
                i += 3
                isAfterConsonant = false
                continue
            }

            // Initial 'x' -> 'এক্স', mid-word 'x' -> 'ক্স'
            if (sub1 == "x" && i == 0) {
                sb.append("এক্স")
                i += 1
                isAfterConsonant = true
                continue
            }

            // Multi-char consonants & conjunct clusters (4 -> 3 -> 2)
            // Don't consume trailing 'r' of a conjunct if it starts 'rri' (ঋ-কার, e.g., prri -> পৃ)
            val matchedMulti = when {
                sub4 != null && multiCharConsonants.containsKey(sub4) &&
                    !(sub4.endsWith("r") && input.startsWith("rri", i + 3)) -> sub4
                sub3 != null && multiCharConsonants.containsKey(sub3) &&
                    !(sub3.endsWith("r") && input.startsWith("rri", i + 2)) -> sub3
                sub2 != null && multiCharConsonants.containsKey(sub2) &&
                    !(sub2.endsWith("r") && input.startsWith("rri", i + 1)) -> sub2
                else -> null
            }
            if (matchedMulti != null) {
                val mapped = if (matchedMulti == "ng") {
                    val nextIdx = i + 2
                    val atEnd = nextIdx >= input.length
                    val nextC = input.getOrNull(nextIdx)
                    if (atEnd || (nextC != null && !isVowelChar(nextC) && nextC != 'g' && nextC != 'k')) "ং" else "ঙ"
                } else {
                    multiCharConsonants[matchedMulti]!!
                }
                sb.append(mapped)
                i += matchedMulti.length
                isAfterConsonant = mapped != "ং"
                continue
            }

            // Explicit Z -> য-ফলা after consonant, or য at word start
            if (sub1 == "Z") {
                if (isAfterConsonant) {
                    sb.append("\u09CD\u09AF")
                } else {
                    sb.append("য")
                }
                i += 1
                isAfterConsonant = true
                continue
            }

            // Single consonant
            if (singleConsonants.containsKey(sub1)) {
                if (preferRephOnR && sub1 == "r" && !isAfterConsonant && isConsonantStart(input, i + 1)) {
                    sb.append("\u09B0\u09CD")
                    i += 1
                    isAfterConsonant = false
                    continue
                }
                if (isAfterConsonant && sub1 in listOf("r", "y", "l", "w") && sb.isNotEmpty() && !sb.endsWith("্")) {
                    when (sub1) {
                        "r" -> sb.append("\u09CD\u09B0")
                        "y" -> sb.append("\u09CD\u09AF")
                        "l" -> sb.append("\u09CD\u09B2")
                        "w" -> sb.append("\u09CD\u09AC")
                    }
                } else {
                    sb.append(singleConsonants[sub1])
                }
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
                    if (sub1 == "o") {
                        val isAtWordEnd = i == input.lastIndex
                        if (trailingOAsOkar && isAtWordEnd) {
                            sb.append("ো")
                        }
                        // Otherwise inherent অ adds no kar
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
