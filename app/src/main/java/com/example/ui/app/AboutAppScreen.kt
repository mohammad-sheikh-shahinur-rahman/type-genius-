package com.example.ui.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.translator.OfflineTranslator
import com.example.model.LanguageId
import com.example.ui.theme.TGMagenta
import com.example.ui.theme.TGViolet

object AppAboutMetadata {
    const val APP_NAME = "TypeGenius Keyboard"
    const val VERSION_NAME = "1.0"
    const val BUILD_NUMBER = 1
    const val PACKAGE_ID = "com.itamadersomajinc.typegenius"
    const val COMPANY_NAME = "IT Amadersomaj Inc."
    const val COPYRIGHT_NOTICE = "© 2026 IT Amadersomaj Inc. All Rights Reserved."
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AboutAppScreen(
    onBack: () -> Unit,
    onOpenBackendlessCheck: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("about_app_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("about_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TGMagenta
                )
            }
            Column {
                Text(
                    text = "অ্যাপ ও কোম্পানি পরিচিতি",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "About ${AppAboutMetadata.APP_NAME} • ${AppAboutMetadata.COMPANY_NAME}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Brand Identity Hero Card
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
                            listOf(TGMagenta.copy(alpha = 0.25f), TGViolet.copy(alpha = 0.22f))
                        )
                    )
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_tg_logo),
                            contentDescription = "TypeGenius Keyboard Logo",
                            modifier = Modifier
                                .size(68.dp)
                                .clip(RoundedCornerShape(18.dp))
                        )
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = AppAboutMetadata.APP_NAME,
                                fontSize = 21.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Version ${AppAboutMetadata.VERSION_NAME} (Build ${AppAboutMetadata.BUILD_NUMBER})",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TGMagenta
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(TGMagenta)
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = AppAboutMetadata.COMPANY_NAME,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF2E7D32).copy(alpha = 0.18f))
                                        .padding(horizontal = 7.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "100% Backendless",
                                        color = Color(0xFF2E7D32),
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SpecPill("${LanguageId.entries.size} Keyboards")
                        SpecPill("${OfflineTranslator.supportedLanguages.size} Translator Languages")
                        SpecPill("Avro + UniBijoy + Probhat")
                        SpecPill("Zero Ads & Zero Telemetry")
                    }
                }
            }
        }

        // Corporate Profile Card: IT Amadersomaj Inc.
        ElevatedCard(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(TGMagenta.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Business, contentDescription = null, tint = TGMagenta)
                    }
                    Column {
                        Text(
                            text = "Developed & Published By",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = AppAboutMetadata.COMPANY_NAME,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Text(
                    text = "IT Amadersomaj Inc. একটি আধুনিক সফটওয়্যার ও ভাষা-প্রযুক্তি প্রতিষ্ঠান, যারা নিরাপদ, বিজ্ঞাপনমুক্ত এবং সম্পূর্ণ অফলাইন-সক্ষম বাংলা ও বহুভাষিক মোবাইল অ্যাপ্লিকেশন নির্মাণে প্রতিশ্রুতিবদ্ধ। TypeGenius Keyboard ব্যবহারকারীর গোপনীয়তা শতভাগ সুরক্ষিত রেখে দ্রুত ও নিখুঁত টাইপিং অভিজ্ঞতা দেওয়ার জন্য ডিজাইন করা হয়েছে।",
                    fontSize = 12.5.sp,
                    lineHeight = 19.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                MetadataRow("Company", AppAboutMetadata.COMPANY_NAME)
                MetadataRow("Application", AppAboutMetadata.APP_NAME)
                MetadataRow("Package ID", AppAboutMetadata.PACKAGE_ID)
                MetadataRow("Version", "${AppAboutMetadata.VERSION_NAME} (Build ${AppAboutMetadata.BUILD_NUMBER})")
                MetadataRow("Architecture", "Kotlin • Jetpack Compose • Room SQLite • DataStore")
            }
        }

        // Complete Feature & Capability Breakdown Card
        ElevatedCard(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "অ্যাপের সকল ফিচার ও কারিগরি তথ্য",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                AboutFeatureItem(
                    icon = Icons.Default.Language,
                    title = "${LanguageId.entries.size} ভাষা ও ৫টি বাংলা লেআউট",
                    description = "অভ্র ফোনেটিক (Avro), ইউনিবিজয় (UniBijoy), প্রভাত (Probhat), জাতীয় (National) এবং সিলেটি নাগরি/বাংলা — সম্পূর্ণ অফলাইন ডিকশনারি ও বাইগ্রাম প্রেডিকশনসহ।"
                )
                AboutFeatureItem(
                    icon = Icons.Default.Translate,
                    title = "${OfflineTranslator.supportedLanguages.size} ভাষার অফলাইন ট্রান্সলেটর ও প্যাক ডাউনলোড",
                    description = "২২টি বিল্ট-ইন ও ১৬টি ডাউনলোডযোগ্য অফলাইন ভাষা প্যাক (তামিল, তেলুগু, গুজরাটি, পাঞ্জাবি, মারাঠি, থাই, হিব্রু, ডাচ, পোলিশ ইত্যাদি) এবং Banglish বাইডাইরেকশনাল অনুবাদ।"
                )
                AboutFeatureItem(
                    icon = Icons.Default.AutoAwesome,
                    title = "TypeGenius AI ও ডাইরেক্ট Gemini সংযোগ",
                    description = "বানান ও ব্যাকরণ সংশোধন (Check/Fix), Formal/Casual/Shorten, কাস্টম প্রম্পট ও মাল্টি-মডেল ফলব্যাকসহ ডাইরেক্ট Google Gemini API সাপোর্ট।"
                )
                AboutFeatureItem(
                    icon = Icons.Default.Keyboard,
                    title = "স্মার্ট টেক্সট এডিটিং ও প্রোডাক্টিভিটি টুলস",
                    description = "Select All, Copy, Cut, Paste, কার্সার ন্যাভিগেশন, OTP স্মার্ট ক্লিপবোর্ড, ক্যালকুলেটর ও ইউনিট কনভার্টার, নোটপ্যাড ও TTS রিডার।"
                )
                AboutFeatureItem(
                    icon = Icons.Default.Calculate,
                    title = "ফন্ট, থিম ও কাস্টমাইজেশন",
                    description = "হাতের লেখা বাংলা ফন্ট (Atma, Galada ও কাস্টম TTF/OTF), ইউনিকোড স্টাইলিশ ফন্ট, ফটো ওয়ালপেপার, স্প্লিট ও ফ্লোটিং মোড এবং ৫টি কী-সাউন্ড প্রোফাইল।"
                )
                AboutFeatureItem(
                    icon = Icons.Default.Security,
                    title = "১০০% গোপনীয়তা ও শূন্য সার্ভার নিশ্চয়তা",
                    description = "কোনো ব্যক্তিগত তথ্য, টাইপিং ডেটা বা ক্লিপবোর্ড ইতিহাস কখনোই কোনো রিমোট সার্ভারে পাঠানো হয় না; সব ডেটা কেবল ডিভাইসের লোকাল স্টোরেজে থাকে।"
                )
            }
        }

        // Interactive Verification & Share Actions
        ElevatedCard(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onOpenBackendlessCheck,
                    colors = ButtonDefaults.buttonColors(containerColor = TGMagenta),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("about_backendless_button")
                ) {
                    Icon(Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("আর্কিটেকচার ও প্রাইভেসি যাচাই করুন", fontWeight = FontWeight.SemiBold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val summary = "${AppAboutMetadata.APP_NAME} v${AppAboutMetadata.VERSION_NAME} (Build ${AppAboutMetadata.BUILD_NUMBER})\n" +
                                "Package: ${AppAboutMetadata.PACKAGE_ID}\n" +
                                "Company: ${AppAboutMetadata.COMPANY_NAME}\n" +
                                "${LanguageId.entries.size} Keyboard Languages • ${OfflineTranslator.supportedLanguages.size} Offline Translator Languages"
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            cm?.setPrimaryClip(ClipData.newPlainText("TypeGenius Info", summary))
                            Toast.makeText(context, "অ্যাপ তথ্য কপি হয়েছে", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("about_copy_info_button")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp), tint = TGMagenta)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("তথ্য কপি", fontSize = 13.sp, color = TGMagenta)
                    }

                    OutlinedButton(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "Check out ${AppAboutMetadata.APP_NAME} by ${AppAboutMetadata.COMPANY_NAME} — ${LanguageId.entries.size} keyboard languages (Avro, UniBijoy, Probhat, Sylheti), ${OfflineTranslator.supportedLanguages.size} offline translator languages, and privacy-first AI tools!"
                                    )
                                }
                                context.startActivity(Intent.createChooser(intent, "Share ${AppAboutMetadata.APP_NAME}"))
                            } catch (_: Exception) {}
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("about_share_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = TGMagenta)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("শেয়ার করুন", fontSize = 13.sp, color = TGMagenta)
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = AppAboutMetadata.COPYRIGHT_NOTICE,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Crafted with precision in Kotlin & Jetpack Compose",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun SpecPill(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.88f))
            .padding(horizontal = 9.dp, vertical = 4.dp)
    ) {
        Text(text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TGMagenta)
    }
}

@Composable
private fun MetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun AboutFeatureItem(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(TGMagenta.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = TGMagenta, modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = title,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(14.dp))
            }
            Text(
                text = description,
                fontSize = 11.5.sp,
                lineHeight = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
