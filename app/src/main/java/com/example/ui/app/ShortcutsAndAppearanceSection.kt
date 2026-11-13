package com.example.ui.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.ShortText
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SettingsManager
import com.example.data.local.TextShortcutRepository
import com.example.ui.theme.TGMagenta
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PhotoBackgroundThemeCard(settingsManager: SettingsManager) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val wallpaperPath by settingsManager.wallpaperPath.collectAsState(initial = "")
    val dimAlpha by settingsManager.wallpaperDimAlpha.collectAsState(initial = 0.42f)
    val keyAlpha by settingsManager.keyBackgroundAlpha.collectAsState(initial = 0.78f)

    val previewBitmap = remember(wallpaperPath) {
        if (wallpaperPath.isNotBlank() && File(wallpaperPath).exists()) {
            try {
                BitmapFactory.decodeFile(wallpaperPath)?.asImageBitmap()
            } catch (_: Throwable) {
                null
            }
        } else null
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val savedPath = saveScaledWallpaperToInternalStorage(context, uri)
                if (savedPath != null) {
                    settingsManager.setWallpaperPath(savedPath)
                    Toast.makeText(context, "Custom keyboard photo updated!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Could not load selected image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Image, contentDescription = null, tint = TGMagenta)
                Column {
                    Text("Custom Photo Background Theme", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text("Uses Android zero-permission Photo Picker • 100% private", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (previewBitmap != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                ) {
                    Image(
                        bitmap = previewBitmap,
                        contentDescription = "Keyboard Wallpaper Preview",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = dimAlpha))
                    )
                    Row(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("ক", "খ", "A", "S", "D").forEach { key ->
                            Box(
                                modifier = Modifier
                                    .size(width = 38.dp, height = 34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF1F1F24).copy(alpha = keyAlpha)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(key, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }

                Text("Key Opacity (${(keyAlpha * 100).toInt()}%)", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Slider(
                    value = keyAlpha,
                    onValueChange = { value -> scope.launch { settingsManager.setKeyBackgroundAlpha(value) } },
                    valueRange = 0.25f..1.0f,
                    colors = SliderDefaults.colors(thumbColor = TGMagenta, activeTrackColor = TGMagenta)
                )

                Text("Photo Darkness Overlay (${(dimAlpha * 100).toInt()}%)", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Slider(
                    value = dimAlpha,
                    onValueChange = { value -> scope.launch { settingsManager.setWallpaperDimAlpha(value) } },
                    valueRange = 0.0f..0.85f,
                    colors = SliderDefaults.colors(thumbColor = TGMagenta, activeTrackColor = TGMagenta)
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TGMagenta),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("pick_keyboard_photo_button")
                ) {
                    Text(if (previewBitmap == null) "Choose Photo" else "Change Photo", fontSize = 13.sp)
                }

                if (previewBitmap != null) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                deleteOldWallpapers(context)
                                settingsManager.setWallpaperPath("")
                                Toast.makeText(context, "Wallpaper cleared", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.testTag("clear_keyboard_photo_button")
                    ) {
                        Text("Reset", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun PersonalShortcutsCard() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { TextShortcutRepository(context) }
    val shortcuts by repo.allShortcuts.collectAsState(initial = emptyList())

    var triggerInput by remember { mutableStateOf("") }
    var expansionInput by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        repo.ensureSeededDefaults()
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.ShortText, contentDescription = null, tint = TGMagenta)
                Column {
                    Text("Personal Dictionary & Text Shortcuts", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text("Type shortcut code (e.g. gm, slm) to get instant phrase suggestions", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = triggerInput,
                    onValueChange = { triggerInput = it },
                    label = { Text("Code", fontSize = 12.sp) },
                    placeholder = { Text("addr") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(0.36f)
                        .testTag("shortcut_trigger_input")
                )
                OutlinedTextField(
                    value = expansionInput,
                    onValueChange = { expansionInput = it },
                    label = { Text("Expanded Text / Phrase", fontSize = 12.sp) },
                    placeholder = { Text("Sylhet, Bangladesh") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(0.64f)
                        .testTag("shortcut_expansion_input")
                )
            }

            Button(
                onClick = {
                    val t = triggerInput.trim()
                    val e = expansionInput.trim()
                    if (t.isNotEmpty() && e.isNotEmpty()) {
                        scope.launch {
                            repo.addOrUpdateShortcut(t, e)
                            triggerInput = ""
                            expansionInput = ""
                            Toast.makeText(context, "Shortcut '$t' saved", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                enabled = triggerInput.isNotBlank() && expansionInput.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = TGMagenta),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_shortcut_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Text Shortcut")
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                shortcuts.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(TGMagenta.copy(alpha = 0.2f))
                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                Text(item.trigger, color = TGMagenta, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                text = item.expansion,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(
                            onClick = { scope.launch { repo.deleteShortcut(item) } },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete shortcut", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

private suspend fun saveScaledWallpaperToInternalStorage(context: Context, uri: Uri): String? =
    withContext(Dispatchers.IO) {
        try {
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOpts)
            }
            val maxDim = 1080
            var sampleSize = 1
            while (boundsOpts.outWidth / sampleSize > maxDim * 2 || boundsOpts.outHeight / sampleSize > maxDim * 2) {
                sampleSize *= 2
            }
            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOpts)
            } ?: return@withContext null

            deleteOldWallpapers(context)
            val outFile = File(context.filesDir, "keyboard_wallpaper_${System.currentTimeMillis()}.jpg")
            FileOutputStream(outFile).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 88, fos)
            }
            bitmap.recycle()
            outFile.absolutePath
        } catch (_: Throwable) {
            null
        }
    }

private fun deleteOldWallpapers(context: Context) {
    try {
        context.filesDir.listFiles { file ->
            file.name.startsWith("keyboard_wallpaper")
        }?.forEach { it.delete() }
    } catch (_: Exception) {}
}
