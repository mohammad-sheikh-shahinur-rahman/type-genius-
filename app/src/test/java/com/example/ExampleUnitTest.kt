package com.example

import com.example.data.phonetic.BengaliPhoneticParser
import com.example.model.LanguageId
import com.example.model.SwipeSensitivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testBengaliPhoneticParser_commonWords() {
        val amiResults = BengaliPhoneticParser.parsePhonetic("ami")
        assertTrue("ami should produce 'আমি'", amiResults.contains("আমি"))

        val tumiResults = BengaliPhoneticParser.parsePhonetic("tumi")
        assertTrue("tumi should produce 'তুমি'", tumiResults.contains("তুমি"))

        val banglaResults = BengaliPhoneticParser.parsePhonetic("bangla")
        assertTrue("bangla should produce 'বাংলা'", banglaResults.contains("বাংলা"))

        val dhonnobadResults = BengaliPhoneticParser.parsePhonetic("dhonnobad")
        assertTrue("dhonnobad should produce 'ধন্যবাদ'", dhonnobadResults.contains("ধন্যবাদ"))
    }

    @Test
    fun testLanguageId_cycling() {
        val languages = listOf(
            LanguageId.BANGLA_PHONETIC,
            LanguageId.ENGLISH,
            LanguageId.HINDI,
            LanguageId.ARABIC
        )

        // Forward cycle (Swipe Left-to-Right)
        var currentIndex = languages.indexOf(LanguageId.BANGLA_PHONETIC)
        var nextIndex = (currentIndex + 1) % languages.size
        assertEquals(LanguageId.ENGLISH, languages[nextIndex])

        currentIndex = languages.indexOf(LanguageId.ARABIC)
        nextIndex = (currentIndex + 1) % languages.size
        assertEquals(LanguageId.BANGLA_PHONETIC, languages[nextIndex]) // wraps around

        // Backward cycle (Swipe Right-to-Left)
        currentIndex = languages.indexOf(LanguageId.BANGLA_PHONETIC)
        val prevIndex = (currentIndex - 1 + languages.size) % languages.size
        assertEquals(LanguageId.ARABIC, languages[prevIndex]) // wraps backwards
    }

    @Test
    fun testSwipeSensitivityThresholds() {
        assertTrue(SwipeSensitivity.HIGH.thresholdDp < SwipeSensitivity.MEDIUM.thresholdDp)
        assertTrue(SwipeSensitivity.MEDIUM.thresholdDp < SwipeSensitivity.LOW.thresholdDp)
        assertEquals(30f, SwipeSensitivity.HIGH.thresholdDp)
        assertEquals(50f, SwipeSensitivity.MEDIUM.thresholdDp)
        assertEquals(80f, SwipeSensitivity.LOW.thresholdDp)
    }

    @Test
    fun testLanguageAttributes() {
        assertTrue(LanguageId.ARABIC.isRtl)
        assertFalse(LanguageId.ENGLISH.isRtl)
        assertFalse(LanguageId.BANGLA_PHONETIC.isRtl)
        assertEquals("বাংলা", LanguageId.BANGLA_PHONETIC.spacebarLabel)
        assertEquals("EN", LanguageId.ENGLISH.spacebarLabel)
    }
}
