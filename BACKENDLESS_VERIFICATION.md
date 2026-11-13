# TypeGenius Keyboard — Backendless Architecture Verification Checklist

## Application Identity
- **Application Name**: TypeGenius Keyboard
- **Application ID / Package**: `com.itamadersomajinc.typegenius`
- **Architecture Philosophy**: 100% Offline, Zero Backend, Privacy-by-Design

---

## Backendless Architecture Verification Checklist

| Pillar | Requirement | Verification Method & Implementation | Status |
|---|---|---|---|
| **1. Custom Backend Server** | Absolutely NO Node.js, Express, PHP, Laravel, Python or custom backends | Codebase inspection: zero custom backend endpoints or socket listeners | VERIFIED (NONE) |
| **2. Remote Databases** | Absolutely NO remote Firebase, Supabase, Cloud Firestore, MongoDB or AWS RDS | Verified: 0 remote database connections. All tables run on Android's embedded SQLite | VERIFIED (NONE) |
| **3. Local Persistence** | Local Room SQLite Database for dictionary, user learned words & pinned clipboard | Implemented via `TypeGeniusDatabase.kt`, `WordDao`, and `ClipboardDao` | VERIFIED (ACTIVE) |
| **4. Local Settings & State** | Jetpack DataStore preferences for all keyboard settings, themes, & layouts | Implemented via `SettingsManager.kt` using `androidx.datastore.preferences` | VERIFIED (ACTIVE) |
| **5. 32 Offline Language Packages** | Bundled native keyboard layouts & offline vocabularies (incl. Sylheti & Bengali Probhat) | Implemented in `LanguageId`, `KeyboardLayouts.kt`, and `DictionaryManager.kt` | VERIFIED (ACTIVE) |
| **6. On-Device Local Translation Engine** | Bundled APK JSON model (`typegenius_translation_pack.json`), greedy N-gram phrase alignment, morphological stemmer, `SVO↔SOV` grammar transfer, Banglish/Sylheti normalization & Room SQLite translation memory | Implemented via `LocalTranslationEngine.kt`, `LocalTranslationGrammar.kt`, `OfflineTranslator.kt` & `TranslationMemoryDao` with 0 network calls | VERIFIED (ACTIVE) |
| **7. Hunspell-Style Offline Spell Checker** | Damerau-Levenshtein fuzzy correction running locally on-device | Implemented via `OfflineSpellChecker.kt` | VERIFIED (ACTIVE) |
| **8. Material You & Responsive UI** | Wallpaper-adaptive Material You dynamic colors + foldable/tablet responsive UI | Implemented in `KeyboardThemeData.kt`, `Theme.kt`, and `BoxWithConstraints` layouts | VERIFIED (ACTIVE) |
| **9. Modern Keyboard Top Toolbox & Panels** | Suggestion bar + 10-icon Top Toolbox, Numpad, `<I>` Text Editor, Emoji/Kaomoji, Voice Typing | Implemented in `KeyboardTopBar.kt`, `KeyboardToolPanels.kt`, `VoiceInputController.kt` | VERIFIED (ACTIVE) |
| **10. Optional Direct Gemini AI** | Direct-only connection to official Google Gemini API | If user enters their own API key, requests go directly to `generativelanguage.googleapis.com` | VERIFIED (ACTIVE) |

---

## Spacebar Swipe Language Switching Implementation

### Gestures:
1. **Swipe Left-to-Right (`ACTION_MOVE` with `deltaX > threshold`)**:
   - Cycles to the next enabled language across the 31 supported packages.
   - Visual pill indicator and optional haptic pulse via `HapticFeedbackConstants.KEYBOARD_TAP`.
2. **Swipe Right-to-Left (`ACTION_MOVE` with `deltaX < -threshold`)**:
   - Cycles to the previous enabled language.
3. **Single Tap (`ACTION_UP` within touch slop)**:
   - Inserts standard space character without duplicate space insertion.
4. **Long Press (`ViewConfiguration.getLongPressTimeout()`)**:
   - Opens scrollable inline language selector modal inside the IME window.
5. **Configurable Sensitivity**:
   - Low: 80dp threshold
   - Medium: 50dp threshold
   - High: 30dp threshold
6. **RTL & One-Handed Compatibility**:
   - Arabic, Urdu, Persian, and Hebrew RTL layouts + Left/Right/Full one-handed modes and Compact/Normal/Tall height presets.
