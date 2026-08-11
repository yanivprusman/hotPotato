package com.automatelinux.hotPotato

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.automatelinux.hotPotato.hp.ApiClient
import com.automatelinux.hotPotato.overlay.OverlayService
import com.automatelinux.hotPotato.ui.theme.AppTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestRuntimePermissions()
        setContent {
            AppTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    LauncherScreen(
                        onStart = ::startRound,
                        onShowBubble = ::showBubble,
                        onPractice = ::startPractice,
                    )
                }
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

@androidx.compose.runtime.Composable
private fun LauncherScreen(
    onStart: (item: String, emoji: String, qty: Int, price: Int) -> Unit,
    onShowBubble: () -> Unit,
    onPractice: () -> Unit,
) {
    var item by remember { mutableStateOf("אבטיח") }
    var emoji by remember { mutableStateOf("🍉") }
    var qty by remember { mutableStateOf("10") }
    var price by remember { mutableStateOf("10") }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 48.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("🥔 תפוח אדמה חם", style = MaterialTheme.typography.headlineMedium)
        Text(
            "מכירת בזק: הודעות בוואטסאפ הופכות לכרטיסי בקשה בבועה, " +
                "שיתוף Waze מסמן את העצירה הנוכחית.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = item,
                onValueChange = { item = it },
                label = { Text("מה מוכרים") },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
            OutlinedTextField(
                value = emoji,
                onValueChange = { emoji = it },
                label = { Text("אימוג'י") },
                modifier = Modifier.fillMaxWidth(0.35f),
                singleLine = true,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = qty,
                onValueChange = { qty = it.filter(Char::isDigit) },
                label = { Text("כמות") },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
            OutlinedTextField(
                value = price,
                onValueChange = { price = it.filter(Char::isDigit) },
                label = { Text("מחיר ₪") },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
        }
        Button(
            onClick = {
                val q = qty.toIntOrNull()
                val p = price.toIntOrNull()
                if (item.isBlank() || q == null || q < 1 || p == null) return@Button
                onStart(item.trim(), emoji.ifBlank { "🍉" }, q, p)
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
        ) {
            Text("🚀 התחל סיבוב", Modifier.padding(vertical = 6.dp))
        }
        OutlinedButton(onClick = onShowBubble, modifier = Modifier.fillMaxWidth()) {
            Text("הצג בועה (סיבוב קיים)")
        }
        OutlinedButton(onClick = onPractice, modifier = Modifier.fillMaxWidth()) {
            Text("🧪 סיבוב תרגול — בלי אנשים אמיתיים")
        }
        Text(
            "האישורים הדרושים: הצגה מעל אפליקציות, שיחות, התראות.",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
