package com.example.data.translator

import android.content.Context
import com.example.data.local.TranslationMemoryDao
import com.example.data.local.TranslationMemoryEntry
import com.example.data.local.TypeGeniusDatabase
import com.example.data.phonetic.BengaliPhoneticParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class LexiconEntry(
    val pos: PosTag,
    val translations: Map<String, String>
)

data class OfflineTranslationResult(
    val translatedText: String,
    val sourceLanguage: String,
    val targetLanguage: String,
    val isOfflineExactOrPhrase: Boolean,
    val engineLabel: String = "On-Device Offline Engine",
    val confidence: Float = if (isOfflineExactOrPhrase) 1.0f else 0.0f,
    val matchedTokens: Int = 0,
    val totalTokens: Int = 0,
    val detectedSourceLanguage: String? = null,
    val strategy: String = "Local Model"
)

object LocalTranslationEngine {

    private val loadMutex = Mutex()

    @Volatile
    private var isAssetPackLoaded = false

    @Volatile
    private var assetPackVersion = "typegenius-local-core-v2"

    // Core high-frequency phrases embedded in Kotlin for instant zero-I/O boot & JVM unit tests
    private val corePhraseTable: List<Map<String, String>> = listOf(
        mapOf("en" to "hello", "bn" to "হ্যালো", "syl" to "আসসালামু আলাইকুম", "hi" to "नमस्ते", "zh" to "你好", "ar" to "مرحبا", "ur" to "سلام", "as" to "নমস্কাৰ", "ne" to "नमस्ते", "fa" to "سلام", "es" to "hola", "fr" to "bonjour", "de" to "hallo", "pt" to "olá", "it" to "ciao", "tr" to "merhaba", "id" to "halo", "ms" to "hai", "ru" to "привет", "ja" to "こんにちは", "ko" to "안녕하세요"),
        mapOf("en" to "hi", "bn" to "হাই", "syl" to "বালা নি", "hi" to "नमस्ते", "zh" to "嗨", "ar" to "أهلا", "ur" to "ہیلو", "as" to "হাই", "ne" to "नमस्ते", "fa" to "سلام", "es" to "hola", "fr" to "salut", "de" to "hallo", "pt" to "oi", "it" to "ciao", "tr" to "selam", "id" to "hai", "ms" to "hai", "ru" to "привет", "ja" to "やあ", "ko" to "안녕"),
        mapOf("en" to "how are you", "bn" to "আপনি কেমন আছেন?", "syl" to "আফনে বালা আছইননি?", "hi" to "आप कैसे हैं?", "zh" to "你好吗？", "ar" to "كيف حالك؟", "ur" to "آپ کیسے ہیں؟", "as" to "আপুনি কেনে আছে?", "ne" to "तपाईं कस्तो हुनुहुन्छ?", "fa" to "حال شما چطور است؟", "es" to "¿cómo estás?", "fr" to "comment allez-vous?", "de" to "wie geht es dir?", "pt" to "como vai você?", "it" to "come stai?", "tr" to "nasılsın?", "id" to "apa kabar?", "ms" to "apa khabar?", "ru" to "как дела?", "ja" to "お元気ですか？", "ko" to "어떻게 지내세요?"),
        mapOf("en" to "how are you?", "bn" to "তুমি কেমন আছো?", "syl" to "তুমি বালা আছোনি?", "hi" to "तुम कैसे हो?", "zh" to "你好吗？", "ar" to "كيف حالك؟", "ur" to "آپ کیسے ہیں؟", "as" to "তুমি কেনে আছা?", "ne" to "तिमी कस्तो छौ?", "fa" to "حالت چطور است؟", "es" to "¿cómo estás?", "fr" to "comment vas-tu?", "de" to "wie geht es dir?", "pt" to "como você está?", "it" to "come stai?", "tr" to "nasılsın?", "id" to "apa kabar?", "ms" to "apa khabar?", "ru" to "как дела?", "ja" to "お元気ですか？", "ko" to "잘 지내요?"),
        mapOf("en" to "i am fine", "bn" to "আমি ভালো আছি", "syl" to "আমি বালা আছি", "hi" to "मैं ठीक हूँ", "zh" to "我很好", "ar" to "أنا بخير", "ur" to "میں ٹھیک ہوں", "as" to "মই ভাল আছো", "ne" to "म ठीक छु", "fa" to "من خوبم", "es" to "estoy bien", "fr" to "je vais bien", "de" to "mir geht es gut", "pt" to "estou bem", "it" to "sto bene", "tr" to "iyiyim", "id" to "saya baik", "ms" to "saya baik", "ru" to "я в порядке", "ja" to "元気です", "ko" to "잘 지내요"),
        mapOf("en" to "thank you", "bn" to "ধন্যবাদ", "syl" to "শুকরিয়া", "hi" to "धन्यवाद", "zh" to "谢谢", "ar" to "شكرا لك", "ur" to "شکریہ", "as" to "ধন্যবাদ", "ne" to "धन्यवाद", "fa" to "ممنون", "es" to "gracias", "fr" to "merci", "de" to "danke", "pt" to "obrigado", "it" to "grazie", "tr" to "teşekkürler", "id" to "terima kasih", "ms" to "terima kasih", "ru" to "спасибо", "ja" to "ありがとう", "ko" to "감사합니다"),
        mapOf("en" to "thanks", "bn" to "ধন্যবাদ", "syl" to "শুকরিয়া", "hi" to "शुक्रिया", "zh" to "谢谢", "ar" to "شكرا", "ur" to "شکریہ", "as" to "ধন্যবাদ", "ne" to "धन्यवाद", "fa" to "متشکرم", "es" to "gracias", "fr" to "merci", "de" to "danke", "pt" to "obrigado", "it" to "grazie", "tr" to "teşekkürler", "id" to "makasih", "ms" to "terima kasih", "ru" to "спасибо", "ja" to "ありがとう", "ko" to "고마워요"),
        mapOf("en" to "good morning", "bn" to "শুভ সকাল", "syl" to "শুব সকাল", "hi" to "सुप्रभात", "zh" to "早上好", "ar" to "صباح الخير", "ur" to "صبح بخیر", "as" to "শুভ প্ৰভাত", "ne" to "शुभ प्रभात", "fa" to "صبح بخیر", "es" to "buenos días", "fr" to "bonjour", "de" to "guten morgen", "pt" to "bom dia", "it" to "buongiorno", "tr" to "günaydın", "id" to "selamat pagi", "ms" to "selamat pagi", "ru" to "доброе утро", "ja" to "おはようございます", "ko" to "좋은 아침이에요"),
        mapOf("en" to "good night", "bn" to "শুভ রাত্রি", "syl" to "শুব রাইত", "hi" to "शुभ रात्रि", "zh" to "晚安", "ar" to "تصبح على خير", "ur" to "شب بخیر", "as" to "শুভ ৰাত্ৰি", "ne" to "शुभ रात्रि", "fa" to "شب بخیر", "es" to "buenas noches", "fr" to "bonne nuit", "de" to "gute nacht", "pt" to "boa noite", "it" to "buonanotte", "tr" to "iyi geceler", "id" to "selamat malam", "ms" to "selamat malam", "ru" to "спокойной ночи", "ja" to "おやすみなさい", "ko" to "안녕히 주무세요"),
        mapOf("en" to "i love you", "bn" to "আমি তোমাকে ভালোবাসি", "syl" to "আমি তুমারে মায়া করি", "hi" to "मैं तुमसे प्यार करता हूँ", "zh" to "我爱你", "ar" to "أحبك", "ur" to "میں تم سے پیار کرتا ہوں", "as" to "মই তোমাক ভাল পাওঁ", "ne" to "म तिमीलाई माया गर्छु", "fa" to "دوستت دارم", "es" to "te amo", "fr" to "je t'aime", "de" to "ich liebe dich", "pt" to "eu te amo", "it" to "ti amo", "tr" to "seni seviyorum", "id" to "aku cinta kamu", "ms" to "saya cinta kamu", "ru" to "я тебя люблю", "ja" to "愛してる", "ko" to "사랑해요"),
        mapOf("en" to "sorry", "bn" to "দুঃখিত", "syl" to "মাফ করিও", "hi" to "माफ़ कीजिए", "zh" to "对不起", "ar" to "آسف", "ur" to "معاف کیجیے", "as" to "দুঃখিত", "ne" to "माफ गर्नुहोस्", "fa" to "متاسفم", "es" to "lo siento", "fr" to "désolé", "de" to "entschuldigung", "pt" to "desculpe", "it" to "scusa", "tr" to "üzgünüm", "id" to "maaf", "ms" to "maaf", "ru" to "извините", "ja" to "ごめんなさい", "ko" to "미안해요"),
        mapOf("en" to "please", "bn" to "দয়া করে", "syl" to "মেহেরবানি করি", "hi" to "कृपया", "zh" to "请", "ar" to "من فضلك", "ur" to "برائے مہربانی", "as" to "অনুগ্ৰহ কৰি", "ne" to "कृपया", "fa" to "لطفاً", "es" to "por favor", "fr" to "s'il vous plaît", "de" to "bitte", "pt" to "por favor", "it" to "per favore", "tr" to "lütfen", "id" to "tolong", "ms" to "tolong", "ru" to "пожалуйста", "ja" to "お願いします", "ko" to "부탁합니다"),
        mapOf("en" to "yes", "bn" to "হ্যাঁ", "syl" to "অয়", "hi" to "हाँ", "zh" to "是的", "ar" to "نعم", "ur" to "جی ہاں", "as" to "হয়", "ne" to "हो", "fa" to "بله", "es" to "sí", "fr" to "oui", "de" to "ja", "pt" to "sim", "it" to "sì", "tr" to "evet", "id" to "ya", "ms" to "ya", "ru" to "да", "ja" to "はい", "ko" to "네"),
        mapOf("en" to "no", "bn" to "না", "syl" to "না", "hi" to "नहीं", "zh" to "不是", "ar" to "لا", "ur" to "نہیں", "as" to "নহয়", "ne" to "होइन", "fa" to "نه", "es" to "no", "fr" to "non", "de" to "nein", "pt" to "não", "it" to "no", "tr" to "hayır", "id" to "tidak", "ms" to "tidak", "ru" to "нет", "ja" to "いいえ", "ko" to "아니요"),
        mapOf("en" to "welcome", "bn" to "স্বাগতম", "syl" to "খুশ আমদেদ", "hi" to "स्वागत है", "zh" to "欢迎", "ar" to "أهلا وسهلا", "ur" to "خوش آمدید", "as" to "স্বাগতম", "ne" to "स्वागत छ", "fa" to "خوش آمدید", "es" to "bienvenido", "fr" to "bienvenue", "de" to "willkommen", "pt" to "bem-vindo", "it" to "benvenuto", "tr" to "hoş geldiniz", "id" to "selamat datang", "ms" to "selamat datang", "ru" to "добро пожаловать", "ja" to "ようこそ", "ko" to "환영합니다"),
        mapOf("en" to "what is your name", "bn" to "আপনার নাম কী?", "syl" to "আফনার নাম কিতা?", "hi" to "आपका नाम क्या है?", "zh" to "你叫什么名字？", "ar" to "ما اسمك؟", "ur" to "آپ کا نام کیا ہے؟", "as" to "আপোনাৰ নাম কি?", "ne" to "तपाईंको नाम के हो?", "fa" to "اسم شما چیست؟", "es" to "¿cómo te llamas?", "fr" to "comment tu t'appelles?", "de" to "wie heißt du?", "pt" to "qual é o seu nome?", "it" to "come ti chiami?", "tr" to "adın ne?", "id" to "siapa namamu?", "ms" to "siapa nama anda?", "ru" to "как тебя зовут?", "ja" to "お名前は何ですか？", "ko" to "이름이 뭐예요?"),
        mapOf("en" to "where are you", "bn" to "তুমি কোথায়?", "syl" to "তুমি কই?", "hi" to "आप कहाँ हैं?", "zh" to "你在哪里？", "ar" to "أين أنت؟", "ur" to "آپ کہاں ہیں؟", "as" to "আপুনি ক'ত আছে?", "ne" to "तपाईं कहाँ हुनुहुन्छ?", "fa" to "کجا هستید؟", "es" to "¿dónde estás?", "fr" to "où es-tu?", "de" to "wo bist du?", "pt" to "onde você está?", "it" to "dove sei?", "tr" to "neredesin?", "id" to "kamu di mana?", "ms" to "awak di mana?", "ru" to "где ты?", "ja" to "どこにいますか？", "ko" to "어디예요?"),
        mapOf("en" to "i love bangladesh", "bn" to "আমি বাংলাদেশকে ভালোবাসি", "syl" to "আমি বাংলাদেশরে মায়া করি", "hi" to "मैं बांग्लादेश से प्यार करता हूँ", "zh" to "我爱孟加拉国", "ar" to "أنا أحب بنغلاديش", "ur" to "مجھے بنگلہ دیش سے پیار ہے", "as" to "মই বাংলাদেশক ভাল পাওঁ", "ne" to "म बंगलादेशलाई माया गर्छु", "fa" to "من بنگلادش را دوست دارم", "es" to "amo a bangladés", "fr" to "j'aime le bangladesh", "de" to "ich liebe bangladesch", "pt" to "eu amo bangladesh", "it" to "amo il bangladesh", "tr" to "bangladeş'i seviyorum", "id" to "saya cinta bangladesh", "ms" to "saya cinta bangladesh", "ru" to "я люблю бангладеш", "ja" to "バングラデシュが大好きです", "ko" to "방글라데시를 사랑해요"),
        mapOf("en" to "congratulations", "bn" to "অভিনন্দন", "syl" to "মোবারকবাদ", "hi" to "बधाई हो", "zh" to "恭喜", "ar" to "مبروك", "ur" to "مبارک ہو", "as" to "অভিনন্দন", "ne" to "बधाई छ", "fa" to "تبریک", "es" to "felicidades", "fr" to "félicitations", "de" to "glückwunsch", "pt" to "parabéns", "it" to "complimenti", "tr" to "tebrikler", "id" to "selamat", "ms" to "tahniah", "ru" to "поздравляю", "ja" to "おめでとう", "ko" to "축하해요"),
        mapOf("en" to "happy birthday", "bn" to "শুভ জন্মদিন", "syl" to "শুব জন্মদিন", "hi" to "जन्मदिन मुबारक", "zh" to "生日快乐", "ar" to "عيد ميلاد سعيد", "ur" to "سالگرہ مبارک", "as" to "ওপজা দিনৰ শুভেচ্ছা", "ne" to "जन्मदिनको शुभकामना", "fa" to "تولدت مبارک", "es" to "feliz cumpleaños", "fr" to "joyeux anniversaire", "de" to "alles gute zum geburtstag", "pt" to "feliz aniversário", "it" to "buon compleanno", "tr" to "doğum günün kutlu olsun", "id" to "selamat ulang tahun", "ms" to "selamat hari lahir", "ru" to "с днём рождения", "ja" to "お誕生日おめでとう", "ko" to "생일 축하해요"),
        mapOf("en" to "see you later", "bn" to "পরে দেখা হবে", "syl" to "বাদে দেখা অইবো", "hi" to "फिर मिलेंगे", "zh" to "回头见", "ar" to "أراك لاحقا", "ur" to "پھر ملیں گے", "as" to "পিছত লগ পাম", "ne" to "पछी भेटौला", "fa" to "بعداً می‌بینمت", "es" to "hasta luego", "fr" to "à plus tard", "de" to "bis später", "pt" to "até logo", "it" to "a dopo", "tr" to "sonra görüşürüz", "id" to "sampai jumpa lagi", "ms" to "jumpa lagi", "ru" to "увидимся позже", "ja" to "またね", "ko" to "나중에 봐요"),
        mapOf("en" to "help me", "bn" to "আমাকে সাহায্য করুন", "syl" to "আমারে মদদ করইন", "hi" to "मेरी मदद करें", "zh" to "帮帮我", "ar" to "ساعدني", "ur" to "میری مدد کریں", "as" to "মোক সহায় কৰক", "ne" to "मलाई मद्दत गर्नुहोस्", "fa" to "کمکم کن", "es" to "ayúdame", "fr" to "aidez-moi", "de" to "hilf mir", "pt" to "me ajude", "it" to "aiutami", "tr" to "bana yardım et", "id" to "tolong saya", "ms" to "tolong saya", "ru" to "помогите мне", "ja" to "助けて", "ko" to "도와주세요"),
        mapOf("en" to "how much is this", "bn" to "এটার দাম কত?", "syl" to "ইটার দাম কতো?", "hi" to "इसकी कीमत क्या है?", "zh" to "这个多少钱？", "ar" to "كم سعر هذا؟", "ur" to "اس کی قیمت کیا ہے؟", "as" to "ইয়াৰ দাম কিমান?", "ne" to "यसको मूल्य कति हो?", "fa" to "قیمت این چقدر است؟", "es" to "¿cuánto cuesta esto?", "fr" to "combien ça coûte?", "de" to "wie viel kostet das?", "pt" to "quanto custa isso?", "it" to "quanto costa?", "tr" to "bu ne kadar?", "id" to "berapa harganya?", "ms" to "berapa harganya?", "ru" to "сколько это стоит?", "ja" to "いくらですか？", "ko" to "얼마예요?")
    )

    private val coreLexiconEntries: List<LexiconEntry> = listOf(
        LexiconEntry(PosTag.PRONOUN, mapOf("en" to "i", "bn" to "আমি", "syl" to "আমি", "hi" to "मैं", "ar" to "أنا", "ur" to "میں", "as" to "মই", "ne" to "म", "fa" to "من", "es" to "yo", "fr" to "je", "de" to "ich", "pt" to "eu", "it" to "io", "tr" to "ben", "id" to "saya", "ms" to "saya", "ru" to "я", "ja" to "私", "ko" to "나")),
        LexiconEntry(PosTag.PRONOUN, mapOf("en" to "you", "bn" to "তুমি", "syl" to "তুমি", "hi" to "तुम", "ar" to "أنت", "ur" to "تم", "as" to "তুমি", "ne" to "तिमी", "fa" to "تو", "es" to "tú", "fr" to "tu", "de" to "du", "pt" to "você", "it" to "tu", "tr" to "sen", "id" to "kamu", "ms" to "awak", "ru" to "ты", "ja" to "あなた", "ko" to "너")),
        LexiconEntry(PosTag.PRONOUN, mapOf("en" to "we", "bn" to "আমরা", "syl" to "আমরা", "hi" to "हम", "ar" to "نحن", "ur" to "ہم", "as" to "আমি", "ne" to "हामी", "fa" to "ما", "es" to "nosotros", "fr" to "nous", "de" to "wir", "pt" to "nós", "it" to "noi", "tr" to "biz", "id" to "kami", "ms" to "kami", "ru" to "мы", "ja" to "私たち", "ko" to "우리")),
        LexiconEntry(PosTag.PRONOUN, mapOf("en" to "my", "bn" to "আমার", "syl" to "আমার", "hi" to "मेरा", "ar" to "لي", "ur" to "میرا", "as" to "মোৰ", "ne" to "मेरो", "fa" to "مال من", "es" to "mi", "fr" to "mon", "de" to "mein", "pt" to "meu", "it" to "mio", "tr" to "benim", "id" to "saya", "ms" to "saya", "ru" to "мой", "ja" to "私の", "ko" to "내")),
        LexiconEntry(PosTag.PRONOUN, mapOf("en" to "your", "bn" to "তোমার", "syl" to "তুমার", "hi" to "तुम्हारा", "ar" to "لك", "ur" to "تمہارا", "as" to "তোমাৰ", "ne" to "तिम्रो", "fa" to "مال تو", "es" to "tu", "fr" to "ton", "de" to "dein", "pt" to "seu", "it" to "tuo", "tr" to "senin", "id" to "kamu", "ms" to "awak", "ru" to "твой", "ja" to "あなたの", "ko" to "네")),
        LexiconEntry(PosTag.ADJ, mapOf("en" to "good", "bn" to "ভালো", "syl" to "বালা", "hi" to "अच्छा", "ar" to "جيد", "ur" to "اچھا", "as" to "ভাল", "ne" to "राम्रो", "fa" to "خوب", "es" to "bueno", "fr" to "bon", "de" to "gut", "pt" to "bom", "it" to "buono", "tr" to "iyi", "id" to "baik", "ms" to "baik", "ru" to "хороший", "ja" to "良い", "ko" to "좋은")),
        LexiconEntry(PosTag.ADJ, mapOf("en" to "bad", "bn" to "খারাপ", "syl" to "খারাপ", "hi" to "बुरा", "ar" to "سيئ", "ur" to "برا", "as" to "বেয়া", "ne" to "नराम्रो", "fa" to "بد", "es" to "malo", "fr" to "mauvais", "de" to "schlecht", "pt" to "mau", "it" to "cattivo", "tr" to "kötü", "id" to "buruk", "ms" to "buruk", "ru" to "плохой", "ja" to "悪い", "ko" to "나쁜")),
        LexiconEntry(PosTag.ADJ, mapOf("en" to "beautiful", "bn" to "সুন্দর", "syl" to "সুন্দর", "hi" to "सुंदर", "ar" to "جميل", "ur" to "خوبصورت", "as" to "ধুনীয়া", "ne" to "सुन्दर", "fa" to "زیبا", "es" to "hermoso", "fr" to "beau", "de" to "schön", "pt" to "bonito", "it" to "bello", "tr" to "güzel", "id" to "indah", "ms" to "cantik", "ru" to "красивый", "ja" to "美しい", "ko" to "아름다운")),
        LexiconEntry(PosTag.NOUN, mapOf("en" to "friend", "bn" to "বন্ধু", "syl" to "বন্ধু", "hi" to "दोस्त", "ar" to "صديق", "ur" to "دوست", "as" to "বন্ধু", "ne" to "साथी", "fa" to "دوست", "es" to "amigo", "fr" to "ami", "de" to "freund", "pt" to "amigo", "it" to "amico", "tr" to "arkadaş", "id" to "teman", "ms" to "kawan", "ru" to "друг", "ja" to "友達", "ko" to "친구")),
        LexiconEntry(PosTag.VERB, mapOf("en" to "love", "bn" to "ভালোবাসা", "syl" to "মায়া", "hi" to "प्यार", "ar" to "حب", "ur" to "محبت", "as" to "ভালপোৱা", "ne" to "माया", "fa" to "عشق", "es" to "amor", "fr" to "amour", "de" to "liebe", "pt" to "amor", "it" to "amore", "tr" to "aşk", "id" to "cinta", "ms" to "cinta", "ru" to "любовь", "ja" to "愛", "ko" to "사랑")),
        LexiconEntry(PosTag.NOUN, mapOf("en" to "home", "bn" to "বাড়ি", "syl" to "বাড়ি", "hi" to "घर", "ar" to "منزل", "ur" to "گھر", "as" to "ঘৰ", "ne" to "घर", "fa" to "خانه", "es" to "casa", "fr" to "maison", "de" to "haus", "pt" to "casa", "it" to "casa", "tr" to "ev", "id" to "rumah", "ms" to "rumah", "ru" to "дом", "ja" to "家", "ko" to "집")),
        LexiconEntry(PosTag.NOUN, mapOf("en" to "water", "bn" to "পানি", "syl" to "পানি", "hi" to "पानी", "ar" to "ماء", "ur" to "پانی", "as" to "পানী", "ne" to "पानी", "fa" to "آب", "es" to "agua", "fr" to "eau", "de" to "wasser", "pt" to "água", "it" to "acqua", "tr" to "su", "id" to "air", "ms" to "air", "ru" to "вода", "ja" to "水", "ko" to "물")),
        LexiconEntry(PosTag.NOUN, mapOf("en" to "food", "bn" to "খাবার", "syl" to "খানি", "hi" to "खाना", "ar" to "طعام", "ur" to "کھانا", "as" to "খাদ্য", "ne" to "खाना", "fa" to "غذا", "es" to "comida", "fr" to "nourriture", "de" to "essen", "pt" to "comida", "it" to "cibo", "tr" to "yemek", "id" to "makanan", "ms" to "makanan", "ru" to "еда", "ja" to "食べ物", "ko" to "음식")),
        LexiconEntry(PosTag.NOUN, mapOf("en" to "time", "bn" to "সময়", "syl" to "সময়", "hi" to "समय", "ar" to "وقت", "ur" to "وقت", "as" to "সময়", "ne" to "समय", "fa" to "زمان", "es" to "tiempo", "fr" to "temps", "de" to "zeit", "pt" to "tempo", "it" to "tempo", "tr" to "zaman", "id" to "waktu", "ms" to "masa", "ru" to "время", "ja" to "時間", "ko" to "시간")),
        LexiconEntry(PosTag.ADV, mapOf("en" to "today", "bn" to "আজ", "syl" to "আইজ", "hi" to "आज", "ar" to "اليوم", "ur" to "آج", "as" to "আজি", "ne" to "आज", "fa" to "امروز", "es" to "hoy", "fr" to "aujourd'hui", "de" to "heute", "pt" to "hoje", "it" to "oggi", "tr" to "bugün", "id" to "hari ini", "ms" to "hari ini", "ru" to "сегодня", "ja" to "今日", "ko" to "오늘")),
        LexiconEntry(PosTag.ADV, mapOf("en" to "tomorrow", "bn" to "আগামীকাল", "syl" to "আগামীকাইল", "hi" to "कल", "ar" to "غدا", "ur" to "کل", "as" to "কাইলৈ", "ne" to "भोलि", "fa" to "فردا", "es" to "mañana", "fr" to "demain", "de" to "morgen", "pt" to "amanhã", "it" to "domani", "tr" to "yarın", "id" to "besok", "ms" to "esok", "ru" to "завтра", "ja" to "明日", "ko" to "내일")),
        LexiconEntry(PosTag.NOUN, mapOf("en" to "book", "bn" to "বই", "syl" to "কিতাব", "hi" to "किताब", "ar" to "كتاب", "ur" to "کتاب", "as" to "কিতাপ", "ne" to "किताब", "fa" to "کتاب", "es" to "libro", "fr" to "livre", "de" to "buch", "pt" to "livro", "it" to "libro", "tr" to "kitap", "id" to "buku", "ms" to "buku", "ru" to "книга", "ja" to "本", "ko" to "책")),
        LexiconEntry(PosTag.VERB, mapOf("en" to "work", "bn" to "কাজ", "syl" to "কাম", "hi" to "काम", "ar" to "عمل", "ur" to "کام", "as" to "কাম", "ne" to "काम", "fa" to "کار", "es" to "trabajo", "fr" to "travail", "de" to "arbeit", "pt" to "trabalho", "it" to "lavoro", "tr" to "iş", "id" to "kerja", "ms" to "kerja", "ru" to "работа", "ja" to "仕事", "ko" to "일")),
        LexiconEntry(PosTag.NOUN, mapOf("en" to "country", "bn" to "দেশ", "syl" to "দেশ", "hi" to "देश", "ar" to "بلد", "ur" to "ملک", "as" to "দেশ", "ne" to "देश", "fa" to "کشور", "es" to "país", "fr" to "pays", "de" to "land", "pt" to "país", "it" to "paese", "tr" to "ülke", "id" to "negara", "ms" to "negara", "ru" to "страна", "ja" to "国", "ko" to "나라")),
        LexiconEntry(PosTag.NOUN, mapOf("en" to "language", "bn" to "ভাষা", "syl" to "ভাষা", "hi" to "भाषा", "ar" to "لغة", "ur" to "زبان", "as" to "ভাষা", "ne" to "भाषा", "fa" to "زبان", "es" to "idioma", "fr" to "langue", "de" to "sprache", "pt" to "idioma", "it" to "lingua", "tr" to "dil", "id" to "bahasa", "ms" to "bahasa", "ru" to "язык", "ja" to "言語", "ko" to "언어")),
        LexiconEntry(PosTag.NOUN, mapOf("en" to "bangla", "bn" to "বাংলা", "syl" to "বাংলা", "hi" to "बंगाली", "ar" to "بنغالي", "ur" to "بنگالی", "es" to "bengalí", "fr" to "bengali", "de" to "bengalisch")),
        LexiconEntry(PosTag.NOUN, mapOf("en" to "bengali", "bn" to "বাংলা", "syl" to "বাংলা", "hi" to "बंगाली", "ar" to "بنغالي", "ur" to "بنگالی", "es" to "bengalí", "fr" to "bengali", "de" to "bengalisch")),
        LexiconEntry(PosTag.ADJ, mapOf("en" to "happy", "bn" to "খুশি", "syl" to "খুশি", "hi" to "खुश", "ar" to "سعيد", "ur" to "خوش", "es" to "feliz", "fr" to "heureux", "de" to "glücklich")),
        LexiconEntry(PosTag.ADJ, mapOf("en" to "new", "bn" to "নতুন", "syl" to "নয়া", "hi" to "नया", "ar" to "جديد", "ur" to "نیا", "es" to "nuevo", "fr" to "nouveau", "de" to "neu")),
        LexiconEntry(PosTag.NOUN, mapOf("en" to "keyboard", "bn" to "কিবোর্ড", "syl" to "কিবোর্ড", "hi" to "कीबोर्ड", "ar" to "لوحة مفاتيح", "ur" to "کی بورڈ", "es" to "teclado", "fr" to "clavier", "de" to "tastatur")),
        LexiconEntry(PosTag.NOUN, mapOf("en" to "phone", "bn" to "ফোন", "syl" to "ফোন", "hi" to "फ़ोन", "ar" to "هاتف", "ur" to "فون", "es" to "teléfono", "fr" to "téléphone", "de" to "telefon")),
        LexiconEntry(PosTag.NOUN, mapOf("en" to "mother", "bn" to "মা", "syl" to "মা", "hi" to "माँ", "ar" to "أم", "ur" to "ماں", "es" to "madre", "fr" to "mère", "de" to "mutter")),
        LexiconEntry(PosTag.NOUN, mapOf("en" to "father", "bn" to "বাবা", "syl" to "আব্বা", "hi" to "पिता", "ar" to "أب", "ur" to "والد", "es" to "padre", "fr" to "père", "de" to "vater")),
        LexiconEntry(PosTag.NOUN, mapOf("en" to "brother", "bn" to "ভাই", "syl" to "ভাই", "hi" to "भाई", "ar" to "أخ", "ur" to "بھائی", "es" to "hermano", "fr" to "frère", "de" to "bruder")),
        LexiconEntry(PosTag.NOUN, mapOf("en" to "sister", "bn" to "বোন", "syl" to "বইন", "hi" to "बहन", "ar" to "أخت", "ur" to "بہن", "es" to "hermana", "fr" to "sœur", "de" to "schwester")),
        LexiconEntry(PosTag.NOUN, mapOf("en" to "day", "bn" to "দিন", "syl" to "দিন", "hi" to "दिन", "ar" to "يوم", "ur" to "دن", "es" to "día", "fr" to "jour", "de" to "tag")),
        LexiconEntry(PosTag.NOUN, mapOf("en" to "night", "bn" to "রাত", "syl" to "রাইত", "hi" to "रात", "ar" to "ليل", "ur" to "رات", "es" to "noche", "fr" to "nuit", "de" to "nacht")),
        LexiconEntry(PosTag.NOUN, mapOf("en" to "morning", "bn" to "সকাল", "syl" to "বিয়ান", "hi" to "सुबह", "ar" to "صباح", "ur" to "صبح", "es" to "mañana", "fr" to "matin", "de" to "morgen")),
        LexiconEntry(PosTag.NOUN, mapOf("en" to "money", "bn" to "টাকা", "syl" to "টেকা", "hi" to "पैसा", "ar" to "مال", "ur" to "پیسہ", "es" to "dinero", "fr" to "argent", "de" to "geld")),
        LexiconEntry(PosTag.VERB, mapOf("en" to "come", "bn" to "আসো", "syl" to "আও", "hi" to "आओ", "ar" to "تعال", "ur" to "آؤ", "es" to "ven", "fr" to "viens", "de" to "komm")),
        LexiconEntry(PosTag.VERB, mapOf("en" to "go", "bn" to "যাও", "syl" to "যাও", "hi" to "जाओ", "ar" to "اذهب", "ur" to "جاؤ", "es" to "ve", "fr" to "va", "de" to "geh")),
        LexiconEntry(PosTag.VERB, mapOf("en" to "eat", "bn" to "খাও", "syl" to "খাও", "hi" to "खाओ", "zh" to "吃", "ar" to "كل", "ur" to "کھاؤ", "fa" to "بخور", "es" to "come", "fr" to "mange", "de" to "iss")),
        LexiconEntry(PosTag.OTHER, mapOf("en" to "ok", "bn" to "ঠিক আছে", "syl" to "ঠিক আছে", "hi" to "ठीक है", "zh" to "好的", "ar" to "حسنا", "ur" to "ٹھیک ہے", "fa" to "باشه", "es" to "vale", "fr" to "d'accord", "de" to "okay")),
        LexiconEntry(PosTag.NOUN, mapOf("en" to "translator", "bn" to "ট্রান্সলেটর", "syl" to "ট্রান্সলেটর", "hi" to "अनुवादक", "zh" to "翻译器", "ar" to "مترجم", "ur" to "مترجم", "fa" to "مترجم")),
        LexiconEntry(PosTag.NOUN, mapOf("en" to "translation", "bn" to "অনুবাদ", "syl" to "অনুবাদ", "hi" to "अनुवाद", "zh" to "翻译", "ar" to "ترجمة", "ur" to "ترجمہ", "fa" to "ترجمه")),
        LexiconEntry(PosTag.ADJ, mapOf("en" to "such", "bn" to "এমন", "syl" to "ইলাখান", "hi" to "ऐसा", "zh" to "这样", "ar" to "مثل هذا", "ur" to "ایسا", "fa" to "چنین"))
    )

    @Volatile
    private var phraseRows: List<Map<String, String>> = corePhraseTable

    @Volatile
    private var lexiconRows: List<LexiconEntry> = coreLexiconEntries

    // Fast normalized lookup index: cleanPhraseLower -> Map<langCode, phrase>
    @Volatile
    private var phraseIndex: Map<String, Map<String, String>> = buildPhraseIndex(corePhraseTable)

    private fun buildPhraseIndex(rows: List<Map<String, String>>): Map<String, Map<String, String>> {
        val map = LinkedHashMap<String, Map<String, String>>(rows.size * 10)
        for (row in rows) {
            for (value in row.values) {
                val key = cleanPhraseKey(value)
                if (key.isNotEmpty() && !map.containsKey(key)) {
                    map[key] = row
                }
            }
        }
        return map
    }

    private fun cleanPhraseKey(raw: String): String {
        return raw.trim().lowercase().trimEnd('?', '!', '.', '।', '؟', ' ')
    }

    suspend fun ensureLoaded(context: Context) {
        if (isAssetPackLoaded) return
        loadMutex.withLock {
            if (isAssetPackLoaded) return
            withContext(Dispatchers.IO) {
                try {
                    val jsonText = context.assets.open("typegenius_translation_pack.json")
                        .bufferedReader(Charsets.UTF_8)
                        .use { it.readText() }
                    val root = JSONObject(jsonText)
                    assetPackVersion = root.optString("modelVersion", "typegenius-local-smt-v2")

                    val loadedPhrases = mutableListOf<Map<String, String>>()
                    loadedPhrases.addAll(corePhraseTable)

                    val phrasesArr = root.optJSONArray("phrases")
                    if (phrasesArr != null) {
                        for (i in 0 until phrasesArr.length()) {
                            val obj = phrasesArr.optJSONObject(i) ?: continue
                            val row = LinkedHashMap<String, String>()
                            val keys = obj.keys()
                            while (keys.hasNext()) {
                                val k = keys.next()
                                val v = obj.optString(k)
                                if (v.isNotBlank()) row[k] = v
                            }
                            if (row.isNotEmpty()) loadedPhrases.add(row)
                        }
                    }

                    val loadedLexicon = mutableListOf<LexiconEntry>()
                    loadedLexicon.addAll(coreLexiconEntries)

                    val lexArr = root.optJSONArray("lexicon")
                    if (lexArr != null) {
                        for (i in 0 until lexArr.length()) {
                            val obj = lexArr.optJSONObject(i) ?: continue
                            val posStr = obj.optString("pos", "OTHER")
                            val pos = runCatching { PosTag.valueOf(posStr) }.getOrDefault(PosTag.OTHER)
                            val translations = LinkedHashMap<String, String>()
                            val keys = obj.keys()
                            while (keys.hasNext()) {
                                val k = keys.next()
                                if (k == "pos") continue
                                val v = obj.optString(k)
                                if (v.isNotBlank()) translations[k] = v
                            }
                            if (translations.isNotEmpty()) {
                                loadedLexicon.add(LexiconEntry(pos = pos, translations = translations))
                            }
                        }
                    }

                    phraseRows = loadedPhrases
                    lexiconRows = loadedLexicon
                    phraseIndex = buildPhraseIndex(loadedPhrases)
                    isAssetPackLoaded = true
                } catch (_: Exception) {
                    // Graceful fallback to embedded core model if asset is unavailable
                }
            }
        }
    }

    fun getModelStats(): Pair<Int, Int> {
        return phraseRows.size to lexiconRows.size
    }

    fun isPackLoadedFromAssets(): Boolean = isAssetPackLoaded

    fun translate(
        input: String,
        sourceLangCode: String,
        targetLangCode: String
    ): OfflineTranslationResult {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) {
            return OfflineTranslationResult(
                translatedText = "",
                sourceLanguage = sourceLangCode,
                targetLanguage = targetLangCode,
                isOfflineExactOrPhrase = false,
                engineLabel = "On-Device Local Model ($assetPackVersion)",
                confidence = 0f
            )
        }

        val detected = LocalTranslationGrammar.detectLikelyLanguageCode(trimmed)

        if (sourceLangCode == targetLangCode) {
            return OfflineTranslationResult(
                translatedText = trimmed,
                sourceLanguage = sourceLangCode,
                targetLanguage = targetLangCode,
                isOfflineExactOrPhrase = true,
                engineLabel = "On-Device Local Model • Identity",
                confidence = 1.0f,
                detectedSourceLanguage = detected,
                strategy = "Identity"
            )
        }

        // Dedicated Banglish Translator pipeline (Banglish <-> Bengali or Banglish -> Any Language)
        if (sourceLangCode == "banglish") {
            val bengaliNormalized = BengaliPhoneticParser.transliterateSentenceToBengali(trimmed)
            if (targetLangCode == "bn") {
                return OfflineTranslationResult(
                    translatedText = bengaliNormalized,
                    sourceLanguage = "banglish",
                    targetLanguage = "bn",
                    isOfflineExactOrPhrase = true,
                    engineLabel = "Banglish → Bengali Engine (100% Offline)",
                    confidence = 1.0f,
                    matchedTokens = trimmed.split(Regex("\\s+")).size,
                    totalTokens = trimmed.split(Regex("\\s+")).size,
                    detectedSourceLanguage = "banglish",
                    strategy = "Banglish → Bengali"
                )
            }
            val bridged = translate(bengaliNormalized, "bn", targetLangCode)
            return bridged.copy(
                sourceLanguage = "banglish",
                engineLabel = "Banglish → ${targetLangCode.uppercase()} • ${bridged.strategy}",
                detectedSourceLanguage = "banglish"
            )
        }

        if (targetLangCode == "banglish") {
            if (sourceLangCode == "bn") {
                val romanized = BengaliPhoneticParser.romanizeBengali(trimmed)
                val tokenCount = trimmed.split(Regex("\\s+")).size
                return OfflineTranslationResult(
                    translatedText = romanized,
                    sourceLanguage = "bn",
                    targetLanguage = "banglish",
                    isOfflineExactOrPhrase = true,
                    engineLabel = "Bengali → Banglish Engine (100% Offline)",
                    confidence = 1.0f,
                    matchedTokens = tokenCount,
                    totalTokens = tokenCount,
                    detectedSourceLanguage = detected,
                    strategy = "Bengali → Banglish"
                )
            }
            val toBengali = translate(trimmed, sourceLangCode, "bn")
            val romanized = BengaliPhoneticParser.romanizeBengali(toBengali.translatedText)
            return toBengali.copy(
                translatedText = romanized,
                targetLanguage = "banglish",
                engineLabel = "Local Model → Banglish (100% Offline)",
                strategy = "${toBengali.strategy} → Banglish"
            )
        }

        val sentences = LocalTranslationGrammar.splitSentences(trimmed)
        if (sentences.isEmpty()) {
            return OfflineTranslationResult(trimmed, sourceLangCode, targetLangCode, false)
        }

        val outClauses = mutableListOf<String>()
        var totalTokens = 0
        var totalMatched = 0
        var exactMatches = 0
        var ngramMatches = 0
        var grammarApplied = false

        for ((clause, rawPunct) in sentences) {
            if (clause.isEmpty()) {
                outClauses.add(rawPunct)
                continue
            }

            val localizedPunct = LocalTranslationGrammar.localizePunctuation(rawPunct, targetLangCode)
            val normalizedClause = LocalTranslationGrammar.normalizeClauseInput(clause, sourceLangCode)
            val cleanClauseKey = cleanPhraseKey(normalizedClause)

            // 1. Exact phrase match on clause
            val exactRow = findPhraseRow(cleanClauseKey, sourceLangCode)
            if (exactRow != null) {
                val targetPhrase = exactRow[targetLangCode] ?: exactRow["en"]
                if (targetPhrase != null) {
                    val wordCount = clause.split(Regex("\\s+")).size
                    totalTokens += wordCount
                    totalMatched += wordCount
                    exactMatches++
                    val cleanTarget = if (localizedPunct.isNotEmpty()) {
                        targetPhrase.trimEnd('?', '!', '.', '।', '؟') + localizedPunct
                    } else {
                        targetPhrase
                    }
                    outClauses.add(cleanTarget)
                    continue
                }
            }

            // 2. Greedy N-Gram + Lexical + Stemming + SVO<->SOV Grammar Transfer
            val rawTokens = normalizedClause.split(Regex("\\s+")).filter { it.isNotEmpty() }
            val translatedTokens = mutableListOf<TranslatedToken>()
            var idx = 0
            while (idx < rawTokens.size) {
                // Attempt longest n-gram phrase match (5 down to 2 words)
                var ngramMatchedLen = 0
                var ngramText: String? = null
                val maxWindow = minOf(5, rawTokens.size - idx)
                for (len in maxWindow downTo 2) {
                    val slice = rawTokens.subList(idx, idx + len)
                    val joinedClean = cleanPhraseKey(slice.joinToString(" "))
                    val matchRow = findPhraseRow(joinedClean, sourceLangCode)
                    val target = matchRow?.get(targetLangCode) ?: matchRow?.get("en")
                    if (target != null) {
                        ngramMatchedLen = len
                        ngramText = target.trimEnd('?', '!', '.', '।', '؟')
                        break
                    }
                }

                if (ngramMatchedLen >= 2 && ngramText != null) {
                    val firstLeading = rawTokens[idx].takeWhile { !LocalTranslationGrammar.isWordChar(it) }
                    val lastTrailing = rawTokens[idx + ngramMatchedLen - 1].takeLastWhile { !LocalTranslationGrammar.isWordChar(it) }
                    translatedTokens.add(
                        TranslatedToken(
                            sourceRaw = rawTokens.subList(idx, idx + ngramMatchedLen).joinToString(" "),
                            translated = ngramText,
                            leadingPunct = firstLeading,
                            trailingPunct = lastTrailing,
                            pos = PosTag.OTHER,
                            matched = true
                        )
                    )
                    totalTokens += ngramMatchedLen
                    totalMatched += ngramMatchedLen
                    ngramMatches++
                    idx += ngramMatchedLen
                    continue
                }

                // Single token lookup with morphological stemming
                val rawToken = rawTokens[idx]
                val leadingPunct = rawToken.takeWhile { !LocalTranslationGrammar.isWordChar(it) }
                val trailingPunct = rawToken.takeLastWhile { !LocalTranslationGrammar.isWordChar(it) }
                val core = rawToken.removePrefix(leadingPunct).removeSuffix(trailingPunct)
                totalTokens++

                if (core.isEmpty()) {
                    translatedTokens.add(TranslatedToken(rawToken, rawToken, "", "", PosTag.OTHER, false))
                } else {
                    val found = findWordWithPos(core, sourceLangCode, targetLangCode)
                    if (found != null) {
                        totalMatched++
                        translatedTokens.add(
                            TranslatedToken(
                                sourceRaw = rawToken,
                                translated = found.first,
                                leadingPunct = leadingPunct,
                                trailingPunct = trailingPunct,
                                pos = found.second,
                                matched = true
                            )
                        )
                    } else {
                        translatedTokens.add(
                            TranslatedToken(
                                sourceRaw = rawToken,
                                translated = core,
                                leadingPunct = leadingPunct,
                                trailingPunct = trailingPunct,
                                pos = PosTag.OTHER,
                                matched = false
                            )
                        )
                    }
                }
                idx++
            }

            // Apply SVO <-> SOV grammar reordering if applicable
            val reordered = LocalTranslationGrammar.applyWordOrderTransfer(
                tokens = translatedTokens,
                sourceLang = sourceLangCode,
                targetLang = targetLangCode
            )
            if (reordered != translatedTokens) {
                grammarApplied = true
            }

            val assembledClause = reordered.joinToString(" ") { tok ->
                "${tok.leadingPunct}${tok.translated}${tok.trailingPunct}"
            } + localizedPunct
            outClauses.add(assembledClause)
        }

        val finalOutput = outClauses.joinToString(" ") { it }.replace(" \n", "\n").trim()
        val confidence = if (totalTokens > 0) (totalMatched.toFloat() / totalTokens.toFloat()).coerceIn(0f, 1f) else 0f

        val strategy = when {
            exactMatches == sentences.size && exactMatches > 0 -> "Exact Phrase Model"
            ngramMatches > 0 && grammarApplied -> "N-Gram + Grammar Transfer"
            ngramMatches > 0 -> "Multi-Word N-Gram"
            grammarApplied -> "SOV/SVO Grammar + Lexicon"
            totalMatched > 0 -> "Morphological Lexicon"
            else -> "No Match (Pass-Through)"
        }

        return OfflineTranslationResult(
            translatedText = finalOutput,
            sourceLanguage = sourceLangCode,
            targetLanguage = targetLangCode,
            isOfflineExactOrPhrase = totalMatched > 0,
            engineLabel = "On-Device Engine • $strategy",
            confidence = confidence,
            matchedTokens = totalMatched,
            totalTokens = totalTokens,
            detectedSourceLanguage = detected,
            strategy = strategy
        )
    }

    private fun findPhraseRow(cleanKey: String, sourceLangCode: String): Map<String, String>? {
        if (cleanKey.isEmpty()) return null
        // Prefer rows that match explicitly in sourceLangCode first
        for (row in phraseRows) {
            val srcVal = row[sourceLangCode]?.let { cleanPhraseKey(it) }
            if (srcVal == cleanKey) return row
        }
        return phraseIndex[cleanKey]
    }

    private fun findWordWithPos(word: String, src: String, tgt: String): Pair<String, PosTag>? {
        val stemCandidates = LocalTranslationGrammar.generateStemCandidates(word, src)

        for (stem in stemCandidates) {
            for (entry in lexiconRows) {
                val rowSrc = entry.translations[src]?.lowercase()
                if (rowSrc == stem || entry.translations.values.any { it.lowercase() == stem }) {
                    val out = entry.translations[tgt] ?: entry.translations["en"]
                    if (out != null) return out to entry.pos
                }
            }
            for (entry in phraseRows) {
                val rowSrc = entry[src]?.lowercase()
                if (rowSrc == stem || entry.values.any { it.lowercase() == stem }) {
                    val out = entry[tgt] ?: entry["en"]
                    if (out != null) return out to PosTag.OTHER
                }
            }
        }
        return null
    }

    fun buildCacheKey(sourceLang: String, targetLang: String, sourceText: String): String {
        return "$sourceLang:$targetLang:${sourceText.trim().lowercase()}"
    }

    suspend fun translateWithMemory(
        context: Context,
        input: String,
        sourceLangCode: String,
        targetLangCode: String
    ): OfflineTranslationResult {
        ensureLoaded(context)
        val trimmed = input.trim()
        if (trimmed.isEmpty()) {
            return translate(trimmed, sourceLangCode, targetLangCode)
        }

        val dao: TranslationMemoryDao = TypeGeniusDatabase.getDatabase(context).translationMemoryDao()
        val key = buildCacheKey(sourceLangCode, targetLangCode, trimmed)

        // 1. Check user-saved/pinned local Room Translation Memory first
        val cached = withContext(Dispatchers.IO) {
            dao.findByKey(key)
        }
        if (cached != null && cached.isPinned) {
            withContext(Dispatchers.IO) { dao.recordHit(key) }
            return OfflineTranslationResult(
                translatedText = cached.translatedText,
                sourceLanguage = sourceLangCode,
                targetLanguage = targetLangCode,
                isOfflineExactOrPhrase = true,
                engineLabel = "On-Device Engine • Local Memory",
                confidence = cached.confidence,
                matchedTokens = 1,
                totalTokens = 1,
                detectedSourceLanguage = LocalTranslationGrammar.detectLikelyLanguageCode(trimmed),
                strategy = "Room Translation Memory"
            )
        }

        // 2. Run on-device engine
        val computed = translate(trimmed, sourceLangCode, targetLangCode)

        // 3. Persist locally in SQLite if meaningfully translated
        if (computed.isOfflineExactOrPhrase && sourceLangCode != targetLangCode) {
            withContext(Dispatchers.IO) {
                dao.upsert(
                    TranslationMemoryEntry(
                        cacheKey = key,
                        sourceText = trimmed,
                        translatedText = computed.translatedText,
                        sourceLang = sourceLangCode,
                        targetLang = targetLangCode,
                        strategy = computed.strategy,
                        confidence = computed.confidence,
                        isPinned = cached?.isPinned ?: false,
                        hitCount = (cached?.hitCount ?: 0) + 1,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
        return computed
    }

    suspend fun saveCustomTranslation(
        context: Context,
        sourceText: String,
        translatedText: String,
        sourceLang: String,
        targetLang: String
    ) = withContext(Dispatchers.IO) {
        val cleanSrc = sourceText.trim()
        val cleanTgt = translatedText.trim()
        if (cleanSrc.isEmpty() || cleanTgt.isEmpty()) return@withContext
        val dao = TypeGeniusDatabase.getDatabase(context).translationMemoryDao()
        dao.upsert(
            TranslationMemoryEntry(
                cacheKey = buildCacheKey(sourceLang, targetLang, cleanSrc),
                sourceText = cleanSrc,
                translatedText = cleanTgt,
                sourceLang = sourceLang,
                targetLang = targetLang,
                strategy = "Saved Local Phrase",
                confidence = 1.0f,
                isPinned = true,
                hitCount = 1,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    fun observeHistory(context: Context): Flow<List<TranslationMemoryEntry>> {
        return TypeGeniusDatabase.getDatabase(context).translationMemoryDao().observeHistory()
    }

    suspend fun togglePinHistory(context: Context, entry: TranslationMemoryEntry) = withContext(Dispatchers.IO) {
        TypeGeniusDatabase.getDatabase(context).translationMemoryDao().setPinned(entry.cacheKey, !entry.isPinned)
    }

    suspend fun deleteHistoryEntry(context: Context, entry: TranslationMemoryEntry) = withContext(Dispatchers.IO) {
        TypeGeniusDatabase.getDatabase(context).translationMemoryDao().deleteByKey(entry.cacheKey)
    }
}
