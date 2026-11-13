package com.example

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import com.example.ui.keyboard.SpacebarSwipeTutorialOverlay
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.SettingsManager
import com.example.model.LanguageId
import com.example.model.SwipeSensitivity
import com.example.ui.app.AppearanceAndLayoutScreen
import com.example.ui.app.BackendlessVerificationScreen
import com.example.ui.app.GeneralSettingsScreen
import com.example.ui.app.GestureSettingsScreen
import com.example.ui.app.HandwritingFontSettingsCard
import com.example.ui.app.LanguagesScreen
import com.example.ui.app.OfflineTranslatorCard
import com.example.ui.app.StylishFontStudioCard
import com.example.ui.keyboard.SpacebarComponent
import com.example.ui.theme.TGMagenta
import com.example.ui.theme.TGViolet
import com.example.ui.theme.TypeGeniusTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var settingsManager: SettingsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        settingsManager = SettingsManager.getInstance(this)

        setContent {
            TypeGeniusTheme {
                MainScreen(
                    settingsManager = settingsManager,
                    onOpenImeSettings = { openImeSettings() },
                    onOpenImePicker = { openImePicker() },
                    checkImeStatus = { isImeEnabledAndActive() }
                )
            }
        }
    }

    private fun openImeSettings() {
        try {
            val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Could not open keyboard settings: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openImePicker() {
        try {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showInputMethodPicker()
        } catch (e: Exception) {
            Toast.makeText(this, "Could not open keyboard picker: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isImeEnabledAndActive(): Pair<Boolean, Boolean> {
        return try {
            val enabledMethods = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_INPUT_METHODS) ?: ""
            val defaultMethod = Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD) ?: ""
            val isEnabled = enabledMethods.contains("com.itamadersomajinc.typegenius") ||
                enabledMethods.contains("com.example")
            val isActive = defaultMethod.contains("com.itamadersomajinc.typegenius") ||
                defaultMethod.contains("com.example")
            Pair(isEnabled, isActive)
        } catch (_: Exception) {
            Pair(false, false)
        }
    }
}

@Composable
fun MainScreen(
    settingsManager: SettingsManager,
    onOpenImeSettings: () -> Unit,
    onOpenImePicker: () -> Unit,
    checkImeStatus: () -> Pair<Boolean, Boolean>
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showBackendlessDetails by remember { mutableStateOf(false) }
    val tabs = listOf(
        "Home" to Icons.Default.Keyboard,
        "Layout" to Icons.Default.Palette,
        "Gestures" to Icons.Default.SwapHoriz,
        "Languages" to Icons.Default.Language,
        "Settings" to Icons.Default.Settings
    )

    BackHandler(enabled = showBackendlessDetails) {
        showBackendlessDetails = false
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpandedWidth = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (!isExpandedWidth) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary
                    ) {
                        tabs.forEachIndexed { index, (label, icon) ->
                            NavigationBarItem(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                icon = { Icon(icon, contentDescription = label) },
                                label = {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 1
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                modifier = Modifier.testTag("nav_tab_$index")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(innerPadding)
            ) {
                if (isExpandedWidth) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        tabs.forEachIndexed { index, (label, icon) ->
                            NavigationRailItem(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                icon = { Icon(icon, contentDescription = label) },
                                label = { Text(label, fontSize = 11.sp) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                modifier = Modifier.testTag("nav_tab_$index")
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(modifier = Modifier.fillMaxSize().widthIn(max = 1040.dp)) {
                        if (showBackendlessDetails) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                TextButton(
                                    onClick = { showBackendlessDetails = false },
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("← Back", color = TGMagenta, fontWeight = FontWeight.Bold)
                                }
                                BackendlessVerificationScreen()
                            }
                        } else {
                            when (selectedTab) {
                                0 -> HomeScreen(
                                    settingsManager = settingsManager,
                                    onOpenImeSettings = onOpenImeSettings,
                                    onOpenImePicker = onOpenImePicker,
                                    checkImeStatus = checkImeStatus,
                                    onNavigateToGestures = { selectedTab = 2 },
                                    onNavigateToLanguages = { selectedTab = 3 },
                                    onNavigateToBackendless = { showBackendlessDetails = true }
                                )
                                1 -> AppearanceAndLayoutScreen(settingsManager = settingsManager)
                                2 -> GestureSettingsScreen(settingsManager = settingsManager)
                                3 -> LanguagesScreen(settingsManager = settingsManager)
                                4 -> GeneralSettingsScreen(settingsManager = settingsManager)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    settingsManager: SettingsManager,
    onOpenImeSettings: () -> Unit,
    onOpenImePicker: () -> Unit,
    checkImeStatus: () -> Pair<Boolean, Boolean>,
    onNavigateToGestures: () -> Unit,
    onNavigateToLanguages: () -> Unit,
    onNavigateToBackendless: () -> Unit
) {
    val context = LocalContext.current
    var imeStatus by remember { mutableStateOf(Pair(false, false)) }
    var micGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        micGranted = granted
        if (!granted) {
            Toast.makeText(context, "Microphone permission is needed for Voice Typing", Toast.LENGTH_SHORT).show()
        }
    }
    var testText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        imeStatus = checkImeStatus()
        micGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
    }

    val isSwipeEnabled by settingsManager.isSpacebarSwipeEnabled.collectAsState(initial = true)
    val swipeSensitivity by settingsManager.swipeSensitivity.collectAsState(initial = SwipeSensitivity.MEDIUM)
    val isHapticEnabled by settingsManager.isHapticEnabled.collectAsState(initial = true)
    val showLangLabel by settingsManager.showActiveLanguageOnSpacebar.collectAsState(initial = true)
    val longPressLangPicker by settingsManager.longPressLanguageSelector.collectAsState(initial = true)
    val enabledLangCodes by settingsManager.enabledLanguages.collectAsState(initial = emptySet())
    val activeLangCode by settingsManager.activeLanguageCode.collectAsState(initial = LanguageId.BANGLA_PROBHAT.code)

    val enabledLangs = remember(enabledLangCodes) {
        val list = enabledLangCodes.map { LanguageId.fromCode(it) }
        if (list.isEmpty()) listOf(LanguageId.BANGLA_PROBHAT, LanguageId.BANGLA_PHONETIC, LanguageId.ENGLISH) else list
    }
    val activeLang = remember(activeLangCode) { LanguageId.fromCode(activeLangCode) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isTabletHome = maxWidth >= 680.dp

        @Composable
        fun ActivationCard() {
            ElevatedCard(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Keyboard Activation Status", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)

                    ActivationStepRow(
                        ready = imeStatus.first,
                        title = "1. Enable in Settings",
                        subtitle = if (imeStatus.first) "TypeGenius is enabled" else "Tap to enable in Android IME list",
                        buttonText = if (imeStatus.first) "Enabled" else "Enable",
                        buttonColor = if (imeStatus.first) Color(0xFF2E7D32) else TGMagenta,
                        testTag = "enable_ime_button",
                        onClick = {
                            onOpenImeSettings()
                            scope.launch {
                                delay(1000)
                                imeStatus = checkImeStatus()
                            }
                        }
                    )

                    ActivationStepRow(
                        ready = imeStatus.second,
                        title = "2. Set as Default IME",
                        subtitle = if (imeStatus.second) "TypeGenius is current keyboard" else "Select TypeGenius as active input",
                        buttonText = if (imeStatus.second) "Active" else "Select",
                        buttonColor = if (imeStatus.second) Color(0xFF2E7D32) else TGViolet,
                        testTag = "select_ime_button",
                        onClick = {
                            onOpenImePicker()
                            scope.launch {
                                delay(1000)
                                imeStatus = checkImeStatus()
                            }
                        }
                    )

                    ActivationStepRow(
                        ready = micGranted,
                        title = "3. Continuous Voice Typing Mic",
                        subtitle = if (micGranted) "Microphone ready for continuous dictation" else "Grant permission for continuous voice typing",
                        buttonText = if (micGranted) "Ready" else "Allow",
                        buttonColor = if (micGranted) Color(0xFF2E7D32) else TGMagenta,
                        testTag = "grant_mic_button",
                        onClick = {
                            if (!micGranted) {
                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    )
                }
            }
        }

        @Composable
        fun TypingPlaygroundCard() {
            ElevatedCard(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Interactive Typing Test", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        if (testText.isNotEmpty()) {
                            TextButton(onClick = { testText = "" }) {
                                Text("Clear", color = TGMagenta, fontSize = 12.sp)
                            }
                        }
                    }

                    Text(
                        text = "Long-press 'C' to Copy, 'V' to Paste, swipe spacebar to switch language, or tap Mic for continuous voice typing.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = testText,
                        onValueChange = { testText = it },
                        placeholder = { Text("এখানে টাইপ করে পরীক্ষা করুন...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("typing_test_field"),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TGMagenta,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        ),
                        maxLines = 4
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            text = "Chars: ${testText.length} | Words: ${if (testText.isBlank()) 0 else testText.trim().split("\\s+".toRegex()).size}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Active: ${activeLang.displayName}",
                            fontSize = 11.sp,
                            color = TGMagenta,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { onNavigateToLanguages() }
                        )
                    }
                }
            }
        }

        @Composable
        fun SpacebarSandboxCard() {
            ElevatedCard(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Spacebar Swipe Simulator", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Icon(imageVector = Icons.Default.SwapHoriz, contentDescription = null, tint = TGMagenta)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF26143F))
                            .padding(4.dp)
                    ) {
                        SpacebarComponent(
                            activeLanguage = activeLang,
                            enabledLanguages = enabledLangs,
                            isSwipeEnabled = isSwipeEnabled,
                            swipeSensitivity = swipeSensitivity,
                            isHapticEnabled = isHapticEnabled,
                            showLanguageLabel = showLangLabel,
                            isLongPressEnabled = longPressLangPicker,
                            backgroundColor = Color(0xFF381F5E),
                            textColor = Color.White,
                            accentColor = TGMagenta,
                            onTapSpace = { testText += " " },
                            onSwipeNextLanguage = {
                                val cur = enabledLangs.indexOf(activeLang).coerceAtLeast(0)
                                val next = (cur + 1) % enabledLangs.size
                                scope.launch { settingsManager.setActiveLanguageCode(enabledLangs[next].code) }
                            },
                            onSwipePrevLanguage = {
                                val cur = enabledLangs.indexOf(activeLang).coerceAtLeast(0)
                                val prev = (cur - 1 + enabledLangs.size) % enabledLangs.size
                                scope.launch { settingsManager.setActiveLanguageCode(enabledLangs[prev].code) }
                            },
                            onLongPressLanguagePicker = { onNavigateToLanguages() },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onNavigateToGestures) {
                            Text("Configure Swipe Settings →", color = TGMagenta, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ElevatedCard(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(TGMagenta.copy(alpha = 0.24f), TGViolet.copy(alpha = 0.22f))
                            )
                        )
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_tg_logo),
                                contentDescription = "TypeGenius Logo",
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(16.dp))
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text("TypeGenius Keyboard", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text("32 Languages (incl. Sylheti) • GemType AI", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                                Text("হাতের লেখা ফন্ট • কন্টিনিউয়াস ভয়েস • লং-প্রেস কপি/পেস্ট", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("হাতের লেখা ফন্ট", "Auto ∞ Voice", "C = Copy / V = Paste", "Offline Translator").forEach { tag ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(tag, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = TGMagenta)
                                }
                            }
                        }
                    }
                }
            }

            if (isTabletHome) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        ActivationCard()
                        HandwritingFontSettingsCard(settingsManager = settingsManager)
                        SpacebarSandboxCard()
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        TypingPlaygroundCard()
                        OfflineTranslatorCard(settingsManager = settingsManager)
                        StylishFontStudioCard(settingsManager = settingsManager)
                    }
                }
            } else {
                ActivationCard()
                TypingPlaygroundCard()
                HandwritingFontSettingsCard(settingsManager = settingsManager)
                OfflineTranslatorCard(settingsManager = settingsManager)
                StylishFontStudioCard(settingsManager = settingsManager)
                SpacebarSandboxCard()
            }

            ElevatedCard(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onNavigateToBackendless)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4CAF50).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Security, contentDescription = "Security", tint = Color(0xFF4CAF50))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("100% Backendless & Private", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text("${LanguageId.entries.size} languages, on-device translator, Room SQLite & DataStore — zero servers.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50))
                }
            }
        }
    }
}

@Composable
private fun ActivationStepRow(
    ready: Boolean,
    title: String,
    subtitle: String,
    buttonText: String,
    buttonColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 50.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = if (ready) Icons.Default.CheckCircle else Icons.Default.Info,
                contentDescription = null,
                tint = if (ready) Color(0xFF4CAF50) else Color(0xFFFF9800)
            )
            Column {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .defaultMinSize(minHeight = 44.dp)
                .testTag(testTag)
        ) {
            Text(buttonText, fontSize = 12.sp)
        }
    }
}
