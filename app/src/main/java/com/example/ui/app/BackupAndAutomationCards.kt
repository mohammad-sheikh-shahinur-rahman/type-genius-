package com.example.ui.app

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BackupRestoreManager
import com.example.data.BackupStats
import com.example.data.BengaliCalendarUtils
import com.example.data.SettingsManager
import com.example.ui.theme.TGMagenta
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OfflineBackupRestoreCard(settingsManager: SettingsManager) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var stats by remember {
        mutableStateOf(
            BackupStats(
                learnedWordsCount = 0,
                shortcutsCount = 0,
                templatesCount = 0,
                customPromptsCount = 0,
                hasNotepadDraft = false
            )
        )
    }
    var refreshTick by remember { mutableIntStateOf(0) }
    var isBusy by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isErrorStatus by remember { mutableStateOf(false) }

    LaunchedEffect(refreshTick) {
        stats = BackupRestoreManager.getLocalStats(context)
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        isBusy = true
        statusMessage = null
        scope.launch {
            runCatching {
                val json = BackupRestoreManager.exportAllToJson(context)
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(json.toByteArray(StandardCharsets.UTF_8))
                        out.flush()
                    } ?: error("ফাইলটি সেভ করার অনুমতি মেলেনি")
                }
            }.onSuccess {
                isBusy = false
                isErrorStatus = false
                refreshTick++
                statusMessage = "✔ ১০০% অফলাইন ব্যাকআপ .json ফাইল সফলভাবে এক্সপোর্ট হয়েছে!"
                Toast.makeText(context, "ব্যাকআপ এক্সপোর্ট সম্পন্ন", Toast.LENGTH_SHORT).show()
            }.onFailure { err ->
                isBusy = false
                isErrorStatus = true
                statusMessage = "এক্সপোর্ট ব্যর্থ: ${err.message ?: "অজানা ত্রুটি"}"
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        isBusy = true
        statusMessage = null
        scope.launch {
            runCatching {
                val jsonText = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.bufferedReader(StandardCharsets.UTF_8)?.use {
                        it.readText()
                    } ?: ""
                }
                BackupRestoreManager.importAllFromJson(context, jsonText)
            }.onSuccess { summary ->
                isBusy = false
                isErrorStatus = false
                refreshTick++
                statusMessage = buildString {
                    append("✔ রিস্টোর সম্পন্ন: ")
                    append("${BengaliCalendarUtils.toBengaliDigits(summary.importedWords.toString())}টি শব্দ, ")
                    append("${BengaliCalendarUtils.toBengaliDigits(summary.importedShortcuts.toString())}টি শর্টকাট, ")
                    append("${BengaliCalendarUtils.toBengaliDigits(summary.importedTemplates.toString())}টি টেমপ্লেট, ")
                    append("${BengaliCalendarUtils.toBengaliDigits(summary.importedCustomPrompts.toString())}টি প্রম্পট")
                    if (summary.restoredNotepad) append(", নোটপ্যাড")
                    append(" ও ${BengaliCalendarUtils.toBengaliDigits(summary.restoredSettingsCount.toString())}টি সেটিংস")
                }
                Toast.makeText(context, "ডেটা সফলভাবে রিস্টোর হয়েছে!", Toast.LENGTH_SHORT).show()
            }.onFailure { err ->
                isBusy = false
                isErrorStatus = true
                statusMessage = "সঠিক টাইপজিনিয়াস .json ব্যাকআপ ফাইল নয়: ${err.message ?: ""}"
            }
        }
    }

    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("offline_backup_restore_card")
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Backup, contentDescription = null, tint = TGMagenta)
                    Column {
                        Text(
                            text = "অফলাইন ব্যাকআপ ও রিস্টোর (.json)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "সার্ভার ছাড়াই শব্দ, শর্টকাট, টেমপ্লেট, নোটপ্যাড ও সেটিংস সংরক্ষণ করুন",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF0F7B47).copy(alpha = 0.18f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("১০০% অফলাইন", color = Color(0xFF4CAF50), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StatChipPill("শেখা শব্দ: ${BengaliCalendarUtils.toBengaliDigits(stats.learnedWordsCount.toString())}")
                StatChipPill("শর্টকাট: ${BengaliCalendarUtils.toBengaliDigits(stats.shortcutsCount.toString())}")
                StatChipPill("টেমপ্লেট: ${BengaliCalendarUtils.toBengaliDigits(stats.templatesCount.toString())}")
                StatChipPill("AI প্রম্পট: ${BengaliCalendarUtils.toBengaliDigits(stats.customPromptsCount.toString())}")
                if (stats.hasNotepadDraft) {
                    StatChipPill("নোটপ্যাড খসড়া সংরক্ষিত ✓")
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { exportLauncher.launch("typegenius_backup.json") },
                    enabled = !isBusy,
                    colors = ButtonDefaults.buttonColors(containerColor = TGMagenta),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("export_backup_json_button")
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                    } else {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ব্যাকআপ এক্সপোর্ট", fontSize = 12.5.sp, maxLines = 1)
                    }
                }

                OutlinedButton(
                    onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) },
                    enabled = !isBusy,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("import_backup_json_button")
                ) {
                    Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(17.dp), tint = TGMagenta)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ফাইল থেকে রিস্টোর", fontSize = 12.5.sp, color = TGMagenta, maxLines = 1)
                }
            }

            statusMessage?.let { msg ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isErrorStatus) MaterialTheme.colorScheme.errorContainer
                            else Color(0xFF0F7B47).copy(alpha = 0.14f)
                        )
                        .padding(10.dp)
                ) {
                    Text(
                        text = msg,
                        color = if (isErrorStatus) MaterialTheme.colorScheme.onErrorContainer else Color(0xFF2E7D32),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SmartAutomationAndPrivacyCard(settingsManager: SettingsManager) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val perAppEnabled by settingsManager.perAppSmartLanguageEnabled.collectAsState(initial = true)
    val perAppMap by settingsManager.perAppLanguageMap.collectAsState(initial = emptyMap())
    val otpExpireMinutes by settingsManager.otpAutoExpireMinutes.collectAsState(initial = 3)

    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("smart_automation_privacy_card")
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = TGMagenta)
                Column {
                    Text(
                        text = "স্মার্ট অটোমেশন ও প্রাইভেসি সুরক্ষা",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "প্রতিটি অ্যাপের ভাষা মনে রাখা ও সংবেদনশীল OTP স্বয়ংক্রিয়ভাবে মুছে ফেলা",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                    Text(
                        text = "অ্যাপ অনুযায়ী স্মার্ট ভাষা মেমোরি",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "WhatsApp/Messenger-এ বাংলা এবং ব্রাউজার বা পাসওয়ার্ড বক্সে স্বয়ংক্রিয় English চালু হবে। আপনার পরিবর্তন মনে রাখা হবে।",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = perAppEnabled,
                    onCheckedChange = { scope.launch { settingsManager.setPerAppSmartLanguageEnabled(it) } },
                    colors = SwitchDefaults.colors(checkedThumbColor = TGMagenta, checkedTrackColor = TGMagenta.copy(alpha = 0.4f)),
                    modifier = Modifier.testTag("toggle_per_app_smart_lang")
                )
            }

            if (perAppMap.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "সংরক্ষিত অ্যাপ-ভাষা: ${BengaliCalendarUtils.toBengaliDigits(perAppMap.size.toString())}টি",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(
                        onClick = {
                            scope.launch {
                                settingsManager.clearPerAppLanguages()
                                Toast.makeText(context, "অ্যাপ-ভাষা মেমোরি রিসেট হয়েছে", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Text("রিসেট", color = TGMagenta, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = TGMagenta, modifier = Modifier.size(16.dp))
                    Text(
                        text = "OTP ও গোপন ক্লিপ অটো-ডিলিট টাইমার",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "ব্যাংক বা অ্যাপের OTP কপি করলে নির্বাচিত সময় পর ক্লিপবোর্ড হিস্ট্রি থেকে স্বয়ংক্রিয়ভাবে মুছে যাবে",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        3 to "৩ মিনিট",
                        5 to "৫ মিনিট",
                        0 to "বন্ধ"
                    ).forEach { (mins, label) ->
                        FilterChip(
                            selected = otpExpireMinutes == mins,
                            onClick = { scope.launch { settingsManager.setOtpAutoExpireMinutes(mins) } },
                            label = { Text(label, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TGMagenta.copy(alpha = 0.2f),
                                selectedLabelColor = TGMagenta
                            ),
                            modifier = Modifier.testTag("otp_expire_chip_$mins")
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatChipPill(label: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(TGMagenta.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = TGMagenta,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
