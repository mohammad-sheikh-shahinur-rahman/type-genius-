package com.example.data

import java.util.Calendar
import java.util.Date
import kotlin.math.floor

data class HijriDateInfo(
    val day: Int,
    val monthIndex: Int, // 0..11
    val monthNameBn: String,
    val year: Int
) {
    val formattedBn: String
        get() = "${BengaliCalendarUtils.toBengaliDigits(day.toString())} $monthNameBn ${BengaliCalendarUtils.toBengaliDigits(year.toString())} হিজরি"

    val formattedShortBn: String
        get() {
            val d = BengaliCalendarUtils.toBengaliDigits(day.toString().padStart(2, '0'))
            val m = BengaliCalendarUtils.toBengaliDigits((monthIndex + 1).toString().padStart(2, '0'))
            val y = BengaliCalendarUtils.toBengaliDigits(year.toString())
            return "$d/$m/$y হি."
        }
}

data class IslamicSnippet(
    val id: String,
    val category: String,
    val title: String,
    val text: String
)

data class TasbihDhikrPreset(
    val id: String,
    val titleBn: String,
    val arabic: String,
    val target: Int = 33
)

object IslamicToolkit {

    val HIJRI_MONTHS_BN = listOf(
        "মুহাররম",
        "সফর",
        "রবিউল আউয়াল",
        "রবিউস সানি",
        "জুমাদাল উলা",
        "জুমাদাস সানি",
        "রজব",
        "শাবান",
        "রমজান",
        "শাওয়াল",
        "জিলকদ",
        "জিলহজ"
    )

    val islamicCategories = listOf("সব", "সম্ভাষণ", "দোয়া ও জিকির", "জুমা ও ঈদ", "চিহ্ন")

    val honorificAndSymbols: List<Pair<String, String>> = listOf(
        "﷽" to "﷽",
        "ﷺ" to "ﷺ",
        "ﷻ" to "ﷻ",
        "☪" to "☪",
        "🕋" to "🕋",
        "🕌" to "🕌",
        "🤲" to "🤲",
        "(আ.)" to "আলাইহিস সালাম (আ.)",
        "(রা.)" to "রাদিয়াল্লাহু আনহু (রা.)",
        "(রহ.)" to "রহিমাহুল্লাহ (রহ.)"
    )

    val tasbihPresets: List<TasbihDhikrPreset> = listOf(
        TasbihDhikrPreset("subhanallah", "সুবহানাল্লাহ", "سُبْحَانَ اللّٰه", 33),
        TasbihDhikrPreset("alhamdulillah", "আলহামদুলিল্লাহ", "الْحَمْدُ لِلّٰه", 33),
        TasbihDhikrPreset("allahuakbar", "আল্লাহু আকবার", "اللّٰهُ أَكْبَر", 34),
        TasbihDhikrPreset("astaghfirullah", "আস্তাগফিরুল্লাহ", "أَسْتَغْفِرُ اللّٰهَ", 100),
        TasbihDhikrPreset("tahlil", "লা ইলাহা ইল্লাল্লাহ", "لَا إِلٰهَ إِلَّا اللّٰهُ", 100),
        TasbihDhikrPreset("durood", "দরূদ শরীফ ﷺ", "صَلَّى اللّٰهُ عَلَيْهِ وَسَلَّمَ", 33)
    )

    val snippets: List<IslamicSnippet> = listOf(
        IslamicSnippet(
            id = "bismillah_ar_bn",
            category = "দোয়া ও জিকির",
            title = "বিসমিল্লাহ (আরবি)",
            text = "بِسْمِ اللّٰهِ الرَّحْمٰنِ الرَّحِيْمِ"
        ),
        IslamicSnippet(
            id = "bismillah_bn",
            category = "দোয়া ও জিকির",
            title = "বিসমিল্লাহির রাহমানির রাহিম",
            text = "বিসমিল্লাহির রাহমানির রাহিম"
        ),
        IslamicSnippet(
            id = "salam_full_bn",
            category = "সম্ভাষণ",
            title = "পূর্ণ সালাম (বাংলা)",
            text = "আসসালামু আলাইকুম ওয়া রাহমাতুল্লাহি ওয়া বারাকাতুহু 🤲"
        ),
        IslamicSnippet(
            id = "salam_ar",
            category = "সম্ভাষণ",
            title = "আসসালামু আলাইকুম (আরবি)",
            text = "السَّلَامُ عَلَيْكُمْ وَرَحْمَةُ اللّٰهِ وَبَرَكَاتُهُ"
        ),
        IslamicSnippet(
            id = "wsalam_full_bn",
            category = "সম্ভাষণ",
            title = "সালামের উত্তর (বাংলা)",
            text = "ওয়ালাইকুমুস সালাম ওয়া রাহমাতুল্লাহি ওয়া বারাকাতুহু 🤍"
        ),
        IslamicSnippet(
            id = "jazakallah",
            category = "সম্ভাষণ",
            title = "জাযাকাল্লাহু খাইরান",
            text = "জাযাকাল্লাহু খাইরান কাসিরা 🤲"
        ),
        IslamicSnippet(
            id = "fiamanillah",
            category = "সম্ভাষণ",
            title = "ফী আমানিল্লাহ",
            text = "ফী আমানিল্লাহ — আল্লাহর হেফাজতে থাকুন 🤲"
        ),
        IslamicSnippet(
            id = "alhamdulillah_all",
            category = "দোয়া ও জিকির",
            title = "আলহামদুলিল্লাহ",
            text = "আলহামদুলিল্লাহি আলা কুল্লি হাল 🤍"
        ),
        IslamicSnippet(
            id = "inshallah",
            category = "দোয়া ও জিকির",
            title = "ইনশাআল্লাহ",
            text = "ইনশাআল্লাহ, আল্লাহ ভরসা 🤲"
        ),
        IslamicSnippet(
            id = "mashallah",
            category = "দোয়া ও জিকির",
            title = "মাশাআল্লাহ",
            text = "মাশাআল্লাহ, তাবারাকাল্লাহ ✨"
        ),
        IslamicSnippet(
            id = "hasbunallah",
            category = "দোয়া ও জিকির",
            title = "হাসবুনাল্লাহু ওয়া নি’মাল ওয়াকিল",
            text = "হাসবুনাল্লাহু ওয়া নি’মাল ওয়াকিল 🤲"
        ),
        IslamicSnippet(
            id = "rabbi_zidni",
            category = "দোয়া ও জিকির",
            title = "রাব্বি জিদনি ইলমা",
            text = "রাব্বি জিদনি ইলমা — হে আমার রব, আমার জ্ঞান বৃদ্ধি করুন 📖"
        ),
        IslamicSnippet(
            id = "innalillah",
            category = "দোয়া ও জিকির",
            title = "ইন্না লিল্লাহি...",
            text = "ইন্না লিল্লাহি ওয়া ইন্না ইলাইহি রাজিউন 🤲"
        ),
        IslamicSnippet(
            id = "jummah_mubarak",
            category = "জুমা ও ঈদ",
            title = "জুমা মোবারক",
            text = "জুমা মোবারক 🕌 আল্লাহ আমাদের সকল নেক দোয়া কবুল করুন। আমিন।"
        ),
        IslamicSnippet(
            id = "ramadan_mubarak",
            category = "জুমা ও ঈদ",
            title = "রমজান মোবারক",
            text = "মাহে রমজানের শুভেচ্ছা 🌙 আল্লাহ আমাদের রোজা ও ইবাদত কবুল করুন।"
        ),
        IslamicSnippet(
            id = "eid_mubarak",
            category = "জুমা ও ঈদ",
            title = "ঈদ মোবারক",
            text = "ঈদ মোবারক! তাকাব্বালাল্লাহু মিন্না ওয়া মিনকুম 🌙✨"
        ),
        IslamicSnippet(
            id = "bismillah_ligature",
            category = "চিহ্ন",
            title = "বিসমিল্লাহ চিহ্ন ﷽",
            text = "﷽"
        ),
        IslamicSnippet(
            id = "saw_ligature",
            category = "চিহ্ন",
            title = "সাল্লাল্লাহু আলাইহি ওয়াসাল্লাম ﷺ",
            text = "ﷺ"
        ),
        IslamicSnippet(
            id = "jalla_ligature",
            category = "চিহ্ন",
            title = "জাল্লা জালালুহু ﷻ",
            text = "ﷻ"
        )
    )

    private val quickTriggerMap: Map<String, List<String>> = mapOf(
        "bsm" to listOf("﷽", "বিসমিল্লাহির রাহমানির রাহিম"),
        "bismillah" to listOf("বিসমিল্লাহির রাহমানির রাহিম", "﷽", "بِسْمِ اللّٰهِ الرَّحْمٰنِ الرَّحِيْمِ"),
        "saw" to listOf("ﷺ", "সাল্লাল্লাহু আলাইহি ওয়াসাল্লাম ﷺ"),
        "jzk" to listOf("জাযাকাল্লাহু খাইরান 🤲", "জাজাকাল্লাহু খাইরান কাসিরা"),
        "jazakallah" to listOf("জাযাকাল্লাহু খাইরান কাসিরা 🤲"),
        "inshallah" to listOf("ইনশাআল্লাহ 🤲", "ইনশাআল্লাহ, আল্লাহ ভরসা"),
        "mashallah" to listOf("মাশাআল্লাহ ✨", "মাশাআল্লাহ, তাবারাকাল্লাহ"),
        "alhamdulillah" to listOf("আলহামদুলিল্লাহ 🤍", "আলহামদুলিল্লাহি আলা কুল্লি হাল"),
        "subhanallah" to listOf("সুবহানাল্লাহ 🤍", "সুবহানাল্লাহি ওয়া বিহামদিহি"),
        "astaghfirullah" to listOf("আস্তাগফিরুল্লাহ 🤲", "আস্তাগফিরুল্লাহাল আজিম"),
        "allahuakbar" to listOf("আল্লাহু আকবার ☪"),
        "innalillah" to listOf("ইন্না লিল্লাহি ওয়া ইন্না ইলাইহি রাজিউন 🤲"),
        "jummah" to listOf("জুমা মোবারক 🕌", "জুমা মোবারক 🕌 আল্লাহ আমাদের দোয়া কবুল করুন"),
        "fiamanillah" to listOf("ফী আমানিল্লাহ 🤲"),
        "eid" to listOf("ঈদ মোবারক 🌙✨", "তাকাব্বালাল্লাহু মিন্না ওয়া মিনকুম")
    )

    fun matchQuickIslamicSuggestions(rawToken: String): List<String> {
        val clean = rawToken.trim().lowercase()
        if (clean.length < 3) return emptyList()
        return quickTriggerMap[clean].orEmpty()
    }

    /**
     * Converts a Gregorian [Date] to tabular Islamic (Hijri) date using the Kuwaiti algorithm.
     * Works identically offline across Android & pure JVM tests.
     */
    fun toHijriDate(date: Date = Date()): HijriDateInfo {
        val cal = Calendar.getInstance().apply { time = date }
        var day = cal.get(Calendar.DAY_OF_MONTH).toDouble()
        var month = (cal.get(Calendar.MONTH) + 1).toDouble()
        var year = cal.get(Calendar.YEAR).toDouble()

        if (year < 1583 || (year == 1582.0 && (month < 10 || (month == 10.0 && day <= 14)))) {
            // Pre-Gregorian Julian fallback
        }
        var m = month
        var y = year
        if (m < 3) {
            y -= 1.0
            m += 12.0
        }
        val a = floor(y / 100.0)
        var b = 2 - a + floor(a / 4.0)
        if (y < 1583) b = 0.0
        if (y == 1582.0) {
            if (m > 10) b = -10.0
            if (m == 10.0) {
                b = 0.0
                if (day > 4) b = -10.0
            }
        }
        val jd = floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524

        val iYear = 10631.0 / 30.0
        val epochAstro = 1948084.0
        val shift1 = 8.01 / 60.0

        var z = jd - epochAstro
        val cyc = floor(z / 10631.0)
        z -= 10631.0 * cyc
        val j = floor((z - shift1) / iYear)
        val iy = 30.0 * cyc + j
        z -= floor(j * iYear + shift1)
        var im = floor((z + 28.5001) / 29.5)
        if (im == 13.0) im = 12.0
        val id = z - floor(29.5001 * im - 29.0)

        val hDay = id.toInt().coerceIn(1, 30)
        val hMonthIdx = (im.toInt() - 1).coerceIn(0, 11)
        val hYear = iy.toInt()

        return HijriDateInfo(
            day = hDay,
            monthIndex = hMonthIdx,
            monthNameBn = HIJRI_MONTHS_BN[hMonthIdx],
            year = hYear
        )
    }
}
