package com.automatelinux.hotPotato

import android.Manifest
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.automatelinux.hotPotato.hp.ApiClient
import com.automatelinux.hotPotato.overlay.OverlayService
import com.automatelinux.hotPotato.ui.theme.AppTheme
import com.automatelinux.hotPotato.ui.theme.Ember
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The hero sits on the icon's burnt-orange gradient, so the status bar
        // needs light icons rather than the auto-chosen dark ones.
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT))
        requestRuntimePermissions()
        setContent {
            AppTheme {
                LauncherScreen(
                    onStart = ::startRound,
                    onShowBubble = ::showBubble,
                    onPractice = ::startPractice,
                )
            }
        }
    }

    private fun requestRuntimePermissions() {
        permissionLauncher.launch(
            arrayOf(Manifest.permission.CALL_PHONE, Manifest.permission.POST_NOTIFICATIONS),
        )
    }

    private fun ensureOverlayPermission(): Boolean {
        if (Settings.canDrawOverlays(this)) return true
        Toast.makeText(this, "אשר הצגה מעל אפליקציות אחרות, ואז חזור", Toast.LENGTH_LONG).show()
        startActivity(
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName"),
            ),
        )
        return false
    }

    private fun startRound(item: String, emoji: String, qty: Int, price: Int) {
        if (!ensureOverlayPermission()) return
        lifecycleScope.launch {
            try {
                ApiClient.startSale(item, emoji, qty, price)
                startForegroundService(Intent(this@MainActivity, OverlayService::class.java))
                Toast.makeText(this@MainActivity, "הסיבוב התחיל — הבועה למעלה", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "שגיאה: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showBubble() {
        if (!ensureOverlayPermission()) return
        startForegroundService(Intent(this, OverlayService::class.java))
    }

    private fun startPractice() {
        if (!ensureOverlayPermission()) return
        lifecycleScope.launch {
            try {
                ApiClient.startPractice()
                startForegroundService(Intent(this@MainActivity, OverlayService::class.java))
                Toast.makeText(
                    this@MainActivity,
                    "סיבוב תרגול: 3 יחידות, שני לקוחות מדומים — אף אחד אמיתי לא מעורב",
                    Toast.LENGTH_LONG,
                ).show()
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "שגיאה: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}

/**
 * The launcher screen is the icon at full size: the potato sits on the heat, and
 * the sale form is a cream sheet rising out of it. The app is Hebrew-only, so the
 * layout direction is pinned RTL rather than left to the device locale.
 */
@Composable
private fun LauncherScreen(
    onStart: (item: String, emoji: String, qty: Int, price: Int) -> Unit,
    onShowBubble: () -> Unit,
    onPractice: () -> Unit,
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Ember.heat),
        ) {
            // the soft white bloom the icon paints behind the potato
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .background(Ember.glow),
            )
            Column(Modifier.fillMaxSize()) {
                Hero()
                SaleSheet(onStart, onShowBubble, onPractice)
            }
        }
    }
}

@Composable
private fun Hero() {
    Column(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 22.dp)
            .padding(top = 24.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            PotatoMark()
            Column {
                Text(
                    "תפוח אדמה חם",
                    color = Ember.FlameCore,
                    fontSize = 30.sp,
                    lineHeight = 34.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    "מכירת בזק",
                    color = Ember.FlameLight,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 3.sp,
                )
            }
        }
        Text(
            "הודעות בוואטסאפ הופכות לכרטיסי בקשה בבועה, ושיתוף Waze מסמן את העצירה הנוכחית.",
            color = Ember.FlameCore.copy(alpha = 0.86f),
            fontSize = 14.sp,
            lineHeight = 20.sp,
        )
    }
}

/** The icon's potato, in miniature: the white bloom plus the potato itself. */
@Composable
private fun PotatoMark() {
    Box(
        Modifier
            .size(62.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.20f))
            .border(1.5.dp, Ember.FlameCore.copy(alpha = 0.45f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text("🥔", fontSize = 30.sp)
    }
}

@Composable
private fun ColumnScope.SaleSheet(
    onStart: (item: String, emoji: String, qty: Int, price: Int) -> Unit,
    onShowBubble: () -> Unit,
    onPractice: () -> Unit,
) {
    var item by remember { mutableStateOf("אבטיח") }
    var emoji by remember { mutableStateOf("🍉") }
    var qty by remember { mutableStateOf("10") }
    var price by remember { mutableStateOf("10") }

    val parsedQty = qty.toIntOrNull()
    val parsedPrice = price.toIntOrNull()
    val canStart = item.isNotBlank() && parsedQty != null && parsedQty >= 1 && parsedPrice != null

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        color = Ember.Cream,
        shadowElevation = 18.dp,
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
                .padding(top = 14.dp, bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(width = 44.dp, height = 5.dp)
                        .clip(CircleShape)
                        .background(Ember.CreamEdge),
                )
            }

            SheetLabel("מה מוכרים היום")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                EmberField(
                    value = item,
                    onValueChange = { item = it },
                    label = "מה מוכרים",
                    modifier = Modifier.weight(1f),
                )
                EmberField(
                    value = emoji,
                    onValueChange = { emoji = it },
                    label = "אימוג'י",
                    modifier = Modifier.width(96.dp),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                EmberField(
                    value = qty,
                    onValueChange = { qty = it.filter(Char::isDigit) },
                    label = "כמות",
                    modifier = Modifier.weight(1f),
                    numeric = true,
                )
                EmberField(
                    value = price,
                    onValueChange = { price = it.filter(Char::isDigit) },
                    // "₪" lives in the field, not in the label: a currency sign
                    // beside a Hebrew word gets reordered by bidi and reads wrong.
                    label = "מחיר",
                    modifier = Modifier.weight(1f),
                    numeric = true,
                    trailing = "₪",
                )
            }

            HotButton("התחל סיבוב", "🚀", enabled = canStart) {
                if (!canStart) return@HotButton
                onStart(item.trim(), emoji.ifBlank { "🍉" }, parsedQty!!, parsedPrice!!)
            }
            GhostButton("הצג בועה (סיבוב קיים)", onClick = onShowBubble)
            GhostButton("סיבוב תרגול — בלי אנשים אמיתיים", "🧪", onClick = onPractice)

            Text(
                "האישורים הדרושים: הצגה מעל אפליקציות, שיחות, התראות.",
                color = Ember.InkSoft,
                fontSize = 12.sp,
                lineHeight = 17.sp,
            )
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}

@Composable
private fun SheetLabel(text: String) {
    Text(
        text,
        color = Ember.PotatoDark,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
    )
}

@Composable
private fun EmberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    numeric: Boolean = false,
    trailing: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier,
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        textStyle = LocalTextStyle.current.copy(fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
        keyboardOptions = if (numeric) {
            KeyboardOptions(keyboardType = KeyboardType.Number)
        } else {
            KeyboardOptions.Default
        },
        trailingIcon = if (trailing != null) {
            { Text(trailing, color = Ember.InkSoft, fontSize = 16.sp) }
        } else {
            null
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Ember.Ink,
            unfocusedTextColor = Ember.Ink,
            focusedContainerColor = Ember.CreamSunk,
            unfocusedContainerColor = Ember.CreamSunk,
            cursorColor = Ember.Mid,
            focusedBorderColor = Ember.Mid,
            unfocusedBorderColor = Ember.CreamEdge,
            focusedLabelColor = Ember.Mid,
            unfocusedLabelColor = Ember.InkSoft,
        ),
    )
}

/** The primary action, wearing the icon's own gradient. */
@Composable
private fun HotButton(
    text: String,
    emoji: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(58.dp)
            .shadow(
                elevation = if (enabled) 12.dp else 0.dp,
                shape = RoundedCornerShape(18.dp),
                ambientColor = Ember.Deep,
                spotColor = Ember.Deep,
            )
            .clip(RoundedCornerShape(18.dp))
            .background(if (enabled) Ember.heatWide else Ember.ashGradient)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(emoji, fontSize = 18.sp)
            Text(
                text,
                color = Ember.FlameCore,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun GhostButton(text: String, emoji: String? = null, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Ember.CreamSunk)
            .border(1.5.dp, Ember.CreamEdge, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (emoji != null) Text(emoji, fontSize = 15.sp)
            Text(text, color = Ember.Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
