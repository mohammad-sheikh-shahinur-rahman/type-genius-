package com.example.ui.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs

data class ConversionPreset(
    val id: String,
    val label: String,
    val fromUnit: String,
    val toUnit: String,
    val convert: (Double) -> Double
)

object MiniCalcEvaluator {
    val conversionPresets: List<ConversionPreset> = listOf(
        ConversionPreset("usd_bdt", "USD → BDT", "USD", "৳", { it * 121.0 }),
        ConversionPreset("bdt_usd", "BDT → USD", "৳", "USD", { it / 121.0 }),
        ConversionPreset("eur_bdt", "EUR → BDT", "EUR", "৳", { it * 131.0 }),
        ConversionPreset("sar_bdt", "SAR → BDT", "SAR", "৳", { it * 32.25 }),
        ConversionPreset("kg_g", "kg → g", "kg", "g", { it * 1000.0 }),
        ConversionPreset("km_mi", "km → mi", "km", "mi", { it * 0.621371 }),
        ConversionPreset("ft_m", "ft → m", "ft", "m", { it * 0.3048 }),
        ConversionPreset("c_f", "°C → °F", "°C", "°F", { (it * 9.0 / 5.0) + 32.0 }),
        ConversionPreset("f_c", "°F → °C", "°F", "°C", { (it - 32.0) * 5.0 / 9.0 })
    )

    fun evaluateExpression(rawExpr: String): String? {
        val cleaned = rawExpr
            .replace("×", "*")
            .replace("÷", "/")
            .replace("−", "-")
            .replace(" ", "")
        if (cleaned.isBlank()) return null
        return runCatching {
            val value = Parser(cleaned).parse()
            formatNumber(value)
        }.getOrNull()
    }

    fun formatNumber(value: Double): String {
        if (value.isNaN() || value.isInfinite()) return "Error"
        val rounded = BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP).stripTrailingZeros()
        return if (abs(rounded.toDouble()) < 1e-9) "0" else rounded.toPlainString()
    }

    private class Parser(private val s: String) {
        private var pos = 0

        fun parse(): Double {
            val res = parseExpr()
            if (pos < s.length) throw IllegalArgumentException("Unexpected char")
            return res
        }

        private fun parseExpr(): Double {
            var left = parseTerm()
            while (pos < s.length) {
                when (s[pos]) {
                    '+' -> { pos++; left += parseTerm() }
                    '-' -> { pos++; left -= parseTerm() }
                    else -> break
                }
            }
            return left
        }

        private fun parseTerm(): Double {
            var left = parseFactor()
            while (pos < s.length) {
                when (s[pos]) {
                    '*' -> { pos++; left *= parseFactor() }
                    '/' -> {
                        pos++
                        val d = parseFactor()
                        if (d == 0.0) throw ArithmeticException("Divide by zero")
                        left /= d
                    }
                    '%' -> {
                        pos++
                        val d = parseFactor()
                        if (d == 0.0) throw ArithmeticException("Mod by zero")
                        left %= d
                    }
                    else -> break
                }
            }
            return left
        }

        private fun parseFactor(): Double {
            if (pos >= s.length) throw IllegalArgumentException("EOF")
            return when (s[pos]) {
                '+' -> { pos++; parseFactor() }
                '-' -> { pos++; -parseFactor() }
                '(' -> {
                    pos++
                    val inside = parseExpr()
                    if (pos >= s.length || s[pos] != ')') throw IllegalArgumentException("Unclosed )")
                    pos++
                    inside
                }
                else -> {
                    val start = pos
                    while (pos < s.length && (s[pos].isDigit() || s[pos] == '.')) pos++
                    if (pos == start) throw IllegalArgumentException("Expected number")
                    s.substring(start, pos).toDouble()
                }
            }
        }
    }
}

@Composable
fun CalculatorAndConverterPanel(
    colors: KeyboardColors,
    onInsertResult: (String) -> Unit,
    onClose: () -> Unit,
    onHaptic: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableIntStateOf(0) } // 0 = Calculator, 1 = Converter
    var expression by remember { mutableStateOf("") }
    var selectedPresetIdx by remember { mutableIntStateOf(0) }

    val evaluated = remember(expression) {
        MiniCalcEvaluator.evaluateExpression(expression) ?: ""
    }

    val presets = MiniCalcEvaluator.conversionPresets
    val activePreset = presets[selectedPresetIdx]
    val numericInput = remember(expression, evaluated) {
        evaluated.toDoubleOrNull() ?: expression.toDoubleOrNull()
    }
    val convertedOutput = remember(numericInput, activePreset) {
        numericInput?.let { v ->
            val out = MiniCalcEvaluator.formatNumber(activePreset.convert(v))
            "$out ${activePreset.toUnit}"
        } ?: ""
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(262.dp)
            .background(colors.background)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        // Header with Mode Tabs & Close
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Calculator" to 0, "Converter" to 1).forEach { (title, idx) ->
                    val selected = activeTab == idx
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) colors.accent else colors.keySpecialBackground)
                            .clickable {
                                onHaptic()
                                activeTab = idx
                            }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                            .testTag("calc_tab_$idx")
                    ) {
                        Text(
                            text = title,
                            color = if (selected) Color.White else colors.textPrimary,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val resultToInsert = if (activeTab == 0) {
                    evaluated.ifBlank { expression }
                } else {
                    convertedOutput
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (resultToInsert.isNotBlank()) colors.accent else colors.keySpecialBackground)
                        .clickable(enabled = resultToInsert.isNotBlank()) {
                            onHaptic()
                            onInsertResult(resultToInsert)
                        }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                        .testTag("calc_insert_button")
                ) {
                    Text(
                        text = "Insert",
                        color = if (resultToInsert.isNotBlank()) Color.White else colors.textSecondary,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = { onHaptic(); onClose() },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close Calculator", tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                }
            }
        }

        // Display Box
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(colors.suggestionBackground)
                .padding(horizontal = 12.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = expression.ifBlank { if (activeTab == 0) "Tap keys to calculate..." else "Enter ${activePreset.fromUnit} value..." },
                color = if (expression.isBlank()) colors.textSecondary else colors.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (activeTab == 0 && evaluated.isNotBlank() && evaluated != expression) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "= $evaluated",
                    color = colors.accent,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            } else if (activeTab == 1 && convertedOutput.isNotBlank()) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "→ $convertedOutput",
                    color = colors.accent,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Converter presets strip when on Converter tab
        if (activeTab == 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                presets.forEachIndexed { idx, preset ->
                    val isSel = selectedPresetIdx == idx
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) colors.accent.copy(alpha = 0.22f) else colors.keySpecialBackground)
                            .clickable {
                                onHaptic()
                                selectedPresetIdx = idx
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("conv_preset_${preset.id}")
                    ) {
                        Text(
                            text = preset.label,
                            color = if (isSel) colors.accent else colors.textPrimary,
                            fontSize = 11.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Shared Calculator Keypad Grid
        val rows = if (activeTab == 0) {
            listOf(
                listOf("7", "8", "9", "÷", "C"),
                listOf("4", "5", "6", "×", "⌫"),
                listOf("1", "2", "3", "-", "("),
                listOf(".", "0", "=", "+", ")")
            )
        } else {
            listOf(
                listOf("7", "8", "9", "C"),
                listOf("4", "5", "6", "⌫"),
                listOf("1", "2", "3", "⇄"),
                listOf(".", "0", "00", "Insert")
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            rows.forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    row.forEach { key ->
                        val isOperator = key in setOf("÷", "×", "-", "+", "=", "(", ")", "C", "⌫", "⇄", "Insert")
                        val isAccentKey = key == "=" || key == "Insert"
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    when {
                                        isAccentKey -> colors.enterKeyBackground
                                        isOperator -> colors.keySpecialBackground
                                        else -> colors.keyBackground
                                    }
                                )
                                .clickable {
                                    onHaptic()
                                    when (key) {
                                        "C" -> expression = ""
                                        "⌫" -> if (expression.isNotEmpty()) expression = expression.dropLast(1)
                                        "=" -> {
                                            if (evaluated.isNotBlank() && evaluated != "Error") {
                                                expression = evaluated
                                            }
                                        }
                                        "⇄" -> {
                                            // Cycle to reverse conversion if available
                                            selectedPresetIdx = (selectedPresetIdx + 1) % presets.size
                                        }
                                        "Insert" -> {
                                            if (convertedOutput.isNotBlank()) {
                                                onInsertResult(convertedOutput)
                                            }
                                        }
                                        else -> expression += key
                                    }
                                }
                                .testTag("calc_key_$key"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (key == "⇄") {
                                Icon(Icons.Default.SwapHoriz, contentDescription = "Next Unit", tint = colors.accent, modifier = Modifier.size(18.dp))
                            } else {
                                Text(
                                    text = key,
                                    color = if (isAccentKey) colors.enterKeyText else if (isOperator) colors.accent else colors.textPrimary,
                                    fontSize = if (key == "Insert") 12.sp else 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
