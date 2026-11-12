package com.example.ui.keyboard

import com.example.model.KeyAction
import com.example.model.KeyType
import com.example.model.LanguageId

object KeyboardLayouts {

    val numberRow = listOf(
        KeyAction("1", null, KeyType.CHARACTER),
        KeyAction("2", null, KeyType.CHARACTER),
        KeyAction("3", null, KeyType.CHARACTER),
        KeyAction("4", null, KeyType.CHARACTER),
        KeyAction("5", null, KeyType.CHARACTER),
        KeyAction("6", null, KeyType.CHARACTER),
        KeyAction("7", null, KeyType.CHARACTER),
        KeyAction("8", null, KeyType.CHARACTER),
        KeyAction("9", null, KeyType.CHARACTER),
        KeyAction("0", null, KeyType.CHARACTER)
    )

    fun getAlphabetRows(language: LanguageId, isShifted: Boolean): List<List<KeyAction>> {
        return when (language) {
            LanguageId.BANGLA_PROBHAT -> getBengaliProbhatRows(isShifted)
            LanguageId.BANGLA_NATIONAL -> getBengaliNationalRows(isShifted)
            LanguageId.HINDI -> getHindiRows(isShifted)
            LanguageId.ARABIC -> getArabicRows(isShifted)
            else -> getEnglishRows(isShifted)
        }
    }

    private fun getBengaliProbhatRows(isShifted: Boolean): List<List<KeyAction>> {
        val pairsRow1 = listOf(
            "দ" to "ধ",
            "ূ" to "ঊ",
            "ী" to "ঈ",
            "র" to "ড়",
            "ট" to "ঠ",
            "এ" to "ঐ",
            "ু" to "উ",
            "ি" to "ই",
            "ও" to "ঔ",
            "প" to "ফ",
            "ে" to "ৈ",
            "ো" to "ৌ"
        )

        val pairsRow2 = listOf(
            "া" to "অ",
            "স" to "ষ",
            "ড" to "ঢ",
            "ত" to "থ",
            "গ" to "ঘ",
            "হ" to "ঃ",
            "জ" to "ঝ",
            "ক" to "খ",
            "ল" to "ং"
        )

        val pairsRow3 = listOf(
            "য়" to "্য",
            "শ" to "ঢ়",
            "চ" to "ছ",
            "আ" to "ঋ",
            "ব" to "ভ",
            "ন" to "ণ",
            "ম" to "ঙ",
            "," to "<",
            "।" to "ঁ"
        )

        val row1 = pairsRow1.map { (normal, shifted) ->
            if (isShifted) KeyAction(shifted, normal, KeyType.CHARACTER)
            else KeyAction(normal, shifted, KeyType.CHARACTER)
        }

        val row2 = pairsRow2.map { (normal, shifted) ->
            if (isShifted) KeyAction(shifted, normal, KeyType.CHARACTER)
            else KeyAction(normal, shifted, KeyType.CHARACTER)
        }

        val row3 = buildList {
            add(KeyAction("⇧", null, KeyType.SHIFT, weight = 1.25f))
            addAll(pairsRow3.map { (normal, shifted) ->
                if (isShifted) KeyAction(shifted, normal, KeyType.CHARACTER)
                else KeyAction(normal, shifted, KeyType.CHARACTER)
            })
            add(KeyAction("⌫", null, KeyType.BACKSPACE, weight = 1.25f))
        }

        return listOf(row1, row2, row3)
    }

    private fun getEnglishRows(isShifted: Boolean): List<List<KeyAction>> {
        val row1 = listOf("q" to "1", "w" to "2", "e" to "3", "r" to "4", "t" to "5", "y" to "6", "u" to "7", "i" to "8", "o" to "9", "p" to "0")
        val row2 = listOf("a" to "@", "s" to "#", "d" to "$", "f" to "%", "g" to "&", "h" to "-", "j" to "+", "k" to "(", "l" to ")")
        val row3Chars = listOf("z" to "*", "x" to "\"", "c" to "'", "v" to ":", "b" to ";", "n" to "!", "m" to "?")

        return listOf(
            row1.map { (char, sec) ->
                val text = if (isShifted) char.uppercase() else char
                KeyAction(text, sec, KeyType.CHARACTER)
            },
            row2.map { (char, sec) ->
                val text = if (isShifted) char.uppercase() else char
                KeyAction(text, sec, KeyType.CHARACTER)
            },
            buildList {
                add(KeyAction("⇧", null, KeyType.SHIFT, weight = 1.3f))
                addAll(row3Chars.map { (char, sec) ->
                    val text = if (isShifted) char.uppercase() else char
                    KeyAction(text, sec, KeyType.CHARACTER)
                })
                add(KeyAction("⌫", null, KeyType.BACKSPACE, weight = 1.3f))
            }
        )
    }

    private fun getBengaliNationalRows(isShifted: Boolean): List<List<KeyAction>> {
        val row1 = if (isShifted) {
            listOf("ঢ়", "ঈ", "ূ", "ঋ", "ঐ", "ঔ", "ভ", "ঙ", "ঘ", "ধ")
        } else {
            listOf("ড়", "ই", "উ", "র", "ত", "য", "ব", "হ", "গ", "দ")
        }

        val row2 = if (isShifted) {
            listOf("অ", "আ", "ৈ", "ৌ", "ী", "ূ", "খ", "ঝ", "ঠ", "ছ")
        } else {
            listOf("া", "ি", "ে", "ো", "ু", "প", "ক", "জ", "ট", "চ")
        }

        val row3 = if (isShifted) {
            listOf("ৎ", "ঃ", "ঁ", "ণ", "ষ", "শ", "ফ")
        } else {
            listOf("ং", "্", "ম", "ন", "স", "ল", "।")
        }

        return listOf(
            row1.map { KeyAction(it, null, KeyType.CHARACTER) },
            row2.map { KeyAction(it, null, KeyType.CHARACTER) },
            buildList {
                add(KeyAction("⇧", null, KeyType.SHIFT, weight = 1.3f))
                addAll(row3.map { KeyAction(it, null, KeyType.CHARACTER) })
                add(KeyAction("⌫", null, KeyType.BACKSPACE, weight = 1.3f))
            }
        )
    }

    private fun getHindiRows(isShifted: Boolean): List<List<KeyAction>> {
        val row1 = if (isShifted) {
            listOf("औ", "ऐ", "ा", "ी", "ू", "भ", "ङ", "घ", "ध", "झ")
        } else {
            listOf("ौ", "ै", "ा", "ि", "ु", "ब", "ह", "ग", "द", "ज")
        }

        val row2 = if (isShifted) {
            listOf("ओ", "ए", "अ", "ई", "ऊ", "फ", "ख", "थ", "छ", "ठ")
        } else {
            listOf("ो", "े", "्", "ि", "ु", "प", "र", "क", "त", "च", "ट")
        }

        val row3 = if (isShifted) {
            listOf("ँ", "ं", "ण", "ष", "श", "ळ")
        } else {
            listOf("ं", "म", "न", "व", "ल", "स", "य")
        }

        return listOf(
            row1.map { KeyAction(it, null, KeyType.CHARACTER) },
            row2.map { KeyAction(it, null, KeyType.CHARACTER) },
            buildList {
                add(KeyAction("⇧", null, KeyType.SHIFT, weight = 1.3f))
                addAll(row3.map { KeyAction(it, null, KeyType.CHARACTER) })
                add(KeyAction("⌫", null, KeyType.BACKSPACE, weight = 1.3f))
            }
        )
    }

    private fun getArabicRows(isShifted: Boolean): List<List<KeyAction>> {
        val row1 = listOf("ض", "ص", "ث", "ق", "ف", "غ", "ع", "ه", "خ", "ح", "ج")
        val row2 = listOf("ش", "س", "ي", "ب", "ل", "ا", "ت", "ن", "م", "ك", "ط")
        val row3 = listOf("ئ", "ء", "ؤ", "ر", "لا", "ى", "ة", "و", "ز", "ظ")

        return listOf(
            row1.map { KeyAction(it, null, KeyType.CHARACTER) },
            row2.map { KeyAction(it, null, KeyType.CHARACTER) },
            buildList {
                add(KeyAction("⇧", null, KeyType.SHIFT, weight = 1.1f))
                addAll(row3.map { KeyAction(it, null, KeyType.CHARACTER) })
                add(KeyAction("⌫", null, KeyType.BACKSPACE, weight = 1.2f))
            }
        )
    }

    val symbols1 = listOf(
        listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0").map { KeyAction(it, null, KeyType.CHARACTER) },
        listOf("@", "#", "$", "%", "&", "-", "+", "(", ")", "/").map { KeyAction(it, null, KeyType.CHARACTER) },
        buildList {
            add(KeyAction("1/2", null, KeyType.MODE_SWITCH, weight = 1.3f))
            addAll(listOf("*", "\"", "'", ":", ";", "!", "?").map { KeyAction(it, null, KeyType.CHARACTER) })
            add(KeyAction("⌫", null, KeyType.BACKSPACE, weight = 1.3f))
        }
    )

    val symbols2 = listOf(
        listOf("~", "`", "|", "•", "√", "π", "÷", "×", "§", "Δ").map { KeyAction(it, null, KeyType.CHARACTER) },
        listOf("£", "€", "¥", "৳", "₹", "¢", "^", "°", "=", "≠").map { KeyAction(it, null, KeyType.CHARACTER) },
        buildList {
            add(KeyAction("2/2", null, KeyType.MODE_SWITCH, weight = 1.3f))
            addAll(listOf("{", "}", "\\", "%", "©", "®", "™", "[", "]").map { KeyAction(it, null, KeyType.CHARACTER) })
            add(KeyAction("⌫", null, KeyType.BACKSPACE, weight = 1.3f))
        }
    )
}
