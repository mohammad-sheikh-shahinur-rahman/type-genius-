package com.example.data.dictionary

object EmojiPredictionEngine {

    private val emojiMap: Map<String, List<String>> = mapOf(
        // Love / Affection
        "love" to listOf("❤️", "💖", "😍"),
        "valobashi" to listOf("❤️", "😍", "💕"),
        "valobasha" to listOf("❤️", "🥰", "💞"),
        "bhalobashi" to listOf("❤️", "😍", "💕"),
        "bhalobasha" to listOf("❤️", "🥰", "💞"),
        "ভালোবাসি" to listOf("❤️", "😍", "💕"),
        "ভালোবাসা" to listOf("❤️", "🥰", "💞"),
        "prem" to listOf("❤️", "💘", "🌹"),
        "প্রেম" to listOf("❤️", "💘", "🌹"),
        "heart" to listOf("❤️", "💖", "💓"),
        "mon" to listOf("❤️", "🤍", "💖"),
        "মন" to listOf("❤️", "🤍", "💖"),

        // Tea / Coffee / Drinks
        "tea" to listOf("☕", "🫖", "🍵"),
        "cha" to listOf("☕", "🫖", "🍵"),
        "চা" to listOf("☕", "🫖", "🍵"),
        "coffee" to listOf("☕", "🤎", "🧋"),
        "kofi" to listOf("☕", "🤎", "🧋"),
        "কফি" to listOf("☕", "🤎", "🧋"),
        "water" to listOf("💧", "🥤", "🌊"),
        "pani" to listOf("💧", "🥤", "🌊"),
        "পানি" to listOf("💧", "🥤", "🌊"),
        "jol" to listOf("💧", "🥤", "🌊"),
        "জল" to listOf("💧", "🥤", "🌊"),

        // Fire / Excited / Lit
        "fire" to listOf("🔥", "⚡", "💥"),
        "agun" to listOf("🔥", "⚡", "💥"),
        "আগুন" to listOf("🔥", "⚡", "💥"),
        "osthir" to listOf("🔥", "🤯", "💥"),
        "অস্থির" to listOf("🔥", "🤯", "💥"),
        "lit" to listOf("🔥", "✨", "💯"),
        "hot" to listOf("🔥", "🥵", "☀️"),

        // Happy / Laugh / Fun
        "happy" to listOf("😊", "😄", "🎉"),
        "khushi" to listOf("😊", "😄", "🎉"),
        "খুশি" to listOf("😊", "😄", "🎉"),
        "anondo" to listOf("🎉", "🥳", "✨"),
        "আনন্দ" to listOf("🎉", "🥳", "✨"),
        "smile" to listOf("😊", "😁", "✨"),
        "hasi" to listOf("😄", "😂", "🤣"),
        "হাসি" to listOf("😄", "😂", "🤣"),
        "lol" to listOf("😂", "🤣", "💀"),
        "haha" to listOf("😂", "🤣", "😆"),

        // Sad / Cry
        "sad" to listOf("😢", "😭", "💔"),
        "kanna" to listOf("😭", "😢", "🥺"),
        "কান্না" to listOf("😭", "😢", "🥺"),
        "monkharap" to listOf("😔", "😞", "💔"),
        "কষ্ট" to listOf("💔", "😢", "😞"),
        "cry" to listOf("😭", "😢", "💧"),

        // Anger / Annoyed
        "angry" to listOf("😡", "🤬", "😤"),
        "rag" to listOf("😡", "🤬", "😤"),
        "রাগ" to listOf("😡", "🤬", "😤"),
        "gussa" to listOf("😡", "👿", "🔥"),
        "mad" to listOf("😡", "💢", "😠"),

        // Thanks / Respect / Greetings
        "thanks" to listOf("🙏", "✨", "🤍"),
        "dhonnobad" to listOf("🙏", "✨", "🤍"),
        "ধন্যবাদ" to listOf("🙏", "✨", "🤍"),
        "shukriya" to listOf("🙏", "✨", "🤲"),
        "শুকরিয়া" to listOf("🙏", "✨", "🤲"),
        "hello" to listOf("👋", "✨", "🤝"),
        "hi" to listOf("👋", "😊", "✨"),
        "salam" to listOf("🤲", "🌙", "🤝"),
        "সালাম" to listOf("🤲", "🌙", "🤝"),
        "bye" to listOf("👋", "🚶", "✌️"),
        "tata" to listOf("👋", "😊", "✨"),
        "বিদায়" to listOf("👋", "🚶", "✌️"),

        // Flower / Nature
        "flower" to listOf("🌸", "🌹", "💐"),
        "ful" to listOf("🌸", "🌹", "💐"),
        "ফুল" to listOf("🌸", "🌹", "💐"),
        "golap" to listOf("🌹", "🌸", "🌺"),
        "গোলাপ" to listOf("🌹", "🌸", "🌺"),
        "rain" to listOf("🌧️", "☔", "⛈️"),
        "bristi" to listOf("🌧️", "☔", "⛈️"),
        "বৃষ্টি" to listOf("🌧️", "☔", "⛈️"),
        "sun" to listOf("☀️", "🌅", "🌞"),
        "shurjo" to listOf("☀️", "🌅", "🌞"),
        "সূর্য" to listOf("☀️", "🌅", "🌞"),
        "moon" to listOf("🌙", "🌕", "✨"),
        "chand" to listOf("🌙", "🌕", "✨"),
        "চাঁদ" to listOf("🌙", "🌕", "✨"),
        "star" to listOf("⭐", "✨", "🌟"),
        "tara" to listOf("⭐", "✨", "🌟"),
        "তারা" to listOf("⭐", "✨", "🌟"),

        // Food / Party
        "food" to listOf("🍛", "🍕", "🍔"),
        "khabar" to listOf("🍛", "🍲", "🍕"),
        "খাবার" to listOf("🍛", "🍲", "🍕"),
        "bhat" to listOf("🍚", "🍛", "🍲"),
        "ভাত" to listOf("🍚", "🍛", "🍲"),
        "biryani" to listOf("🍛", "🍗", "😋"),
        "বিরিয়ানি" to listOf("🍛", "🍗", "😋"),
        "birthday" to listOf("🎂", "🎉", "🎈"),
        "jonmodin" to listOf("🎂", "🎉", "🎈"),
        "জন্মদিন" to listOf("🎂", "🎉", "🎈"),
        "hbd" to listOf("🎂", "🎉", "🥳"),

        // Sleep / Rest
        "sleep" to listOf("😴", "💤", "🛌"),
        "ghum" to listOf("😴", "💤", "🛌"),
        "ঘুম" to listOf("😴", "💤", "🛌"),
        "night" to listOf("🌙", "🌃", "💤"),
        "raat" to listOf("🌙", "🌃", "💤"),
        "রাত" to listOf("🌙", "🌃", "💤"),

        // Cool / Success / Good
        "cool" to listOf("😎", "🔥", "👌"),
        "boss" to listOf("😎", "👑", "🔥"),
        "দারুণ" to listOf("👌", "🔥", "✨"),
        "সেরা" to listOf("🏆", "👑", "🔥"),
        "sera" to listOf("🏆", "👑", "🔥"),
        "good" to listOf("👍", "✨", "👌"),
        "valo" to listOf("👍", "😊", "✨"),
        "ভালো" to listOf("👍", "😊", "✨"),
        "yes" to listOf("✅", "👍", "👌"),
        "ha" to listOf("✅", "👍", "👌"),
        "হ্যাঁ" to listOf("✅", "👍", "👌"),
        "no" to listOf("❌", "🙅‍♂️", "👎"),
        "na" to listOf("❌", "🙅‍♂️", "👎"),
        "না" to listOf("❌", "🙅‍♂️", "👎"),

        // Objects / Tech
        "phone" to listOf("📱", "📞", "📲"),
        "ফোন" to listOf("📱", "📞", "📲"),
        "call" to listOf("📞", "📲", "🤙"),
        "কল" to listOf("📞", "📲", "🤙"),
        "car" to listOf("🚗", "🏎️", "🚘"),
        "gari" to listOf("🚗", "🛵", "🚘"),
        "গাড়ি" to listOf("🚗", "🛵", "🚘"),
        "money" to listOf("💰", "💵", "💸"),
        "taka" to listOf("💰", "💵", "💸"),
        "টাকা" to listOf("💰", "💵", "💸"),
        "music" to listOf("🎵", "🎶", "🎧"),
        "gaan" to listOf("🎵", "🎶", "🎧"),
        "গান" to listOf("🎵", "🎶", "🎧")
    )

    private val allPredictedEmojis: Set<String> by lazy {
        emojiMap.values.flatten().toSet()
    }

    fun predictEmojis(word: String): List<String> {
        val clean = word.trim().lowercase().trim(',', '.', '!', '?', ';', ':', '।', '"')
        if (clean.isBlank()) return emptyList()
        return emojiMap[clean] ?: emptyList()
    }

    fun isEmojiSuggestion(candidate: String): Boolean {
        return candidate in allPredictedEmojis
    }
}
