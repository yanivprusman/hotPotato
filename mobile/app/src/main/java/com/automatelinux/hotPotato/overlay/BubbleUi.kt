package com.automatelinux.hotPotato.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.automatelinux.hotPotato.hp.Claim
import com.automatelinux.hotPotato.hp.HpState
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.roundToInt

sealed interface UiState {
    data object Loading : UiState
    data class Error(val message: String) : UiState
    data class Data(val state: HpState) : UiState
}

interface BubbleActions {
    fun onToggleExpanded()
    fun onDrag(dx: Int, dy: Int)
    fun onCall(phone: String)
    fun onConfirm(claim: Claim, qty: Int)
    fun onReject(claim: Claim)
    fun onDelivered(claim: Claim, paid: Boolean)
    fun onSetStop(claim: Claim)
    fun onAdjust(delta: Int)
    fun onEndSale()
    fun onCloseBubble()
}

private val GoneRed = Color(0xFFC62828)
private val PendingOrange = Color(0xFFEF6C00)
private val OkGreen = Color(0xFF2E7D32)

@Composable
fun BubbleRoot(
    uiFlow: StateFlow<UiState>,
    expandedFlow: StateFlow<Boolean>,
    actionErrorFlow: StateFlow<String?>,
    actions: BubbleActions,
) {
    val ui by uiFlow.collectAsState()
    val expanded by expandedFlow.collectAsState()
    val actionError by actionErrorFlow.collectAsState()
    if (expanded) {
        ExpandedPanel(ui, actionError, actions)
    } else {
        CollapsedBubble(ui, actions)
    }
}

private fun bubbleColor(ui: UiState): Color = when (ui) {
    is UiState.Loading -> Color(0xFF616161)
    is UiState.Error -> Color(0xFF37474F)
    is UiState.Data ->
        if (ui.state.sale == null || ui.state.sale.status != "active") Color(0xFF616161)
        else if (ui.state.remaining <= 0) GoneRed
        else if (ui.state.pending.isNotEmpty()) PendingOrange
        else OkGreen
}

@Composable
private fun CollapsedBubble(ui: UiState, actions: BubbleActions) {
    val label = when (ui) {
        is UiState.Loading -> "…"
        is UiState.Error -> "!"
        is UiState.Data -> {
            val s = ui.state
            if (s.sale == null || s.sale.status != "active") "—"
            else if (s.remaining <= 0) "נגמר"
            else "${s.sale.emoji} ${s.remaining}"
        }
    }
    val pendingCount = (ui as? UiState.Data)?.state?.pending?.size ?: 0
    Box(
        modifier = Modifier
            .size(72.dp)
            .pointerInput(Unit) {
                detectDragGestures { change, drag ->
                    change.consume()
                    actions.onDrag(drag.x.roundToInt(), drag.y.roundToInt())
                }
            },
    ) {
        Surface(
            shape = CircleShape,
            color = bubbleColor(ui),
            shadowElevation = 6.dp,
            modifier = Modifier
                .size(64.dp)
                .align(Alignment.Center)
                .clickable { actions.onToggleExpanded() },
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    label,
                    color = Color.White,
                    fontSize = if (label == "נגמר") 16.sp else 18.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        if (pendingCount > 0) {
            Surface(
                shape = CircleShape,
                color = Color.White,
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.TopEnd),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "$pendingCount",
                        color = PendingOrange,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpandedPanel(ui: UiState, actionError: String?, actions: BubbleActions) {
    Card(
        modifier = Modifier.width(330.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
    ) {
        Column(
            Modifier
                .padding(12.dp)
                .heightIn(max = 520.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Header(ui, actions)
            if (actionError != null) {
                Text(actionError, color = GoneRed, fontSize = 13.sp)
            }
            when (ui) {
                is UiState.Loading -> Text("טוען…")
                is UiState.Error -> Text("שרת לא זמין: ${ui.message}", color = GoneRed, fontSize = 13.sp)
                is UiState.Data -> DataSections(ui.state, actions)
            }
        }
    }
}

@Composable
private fun Header(ui: UiState, actions: BubbleActions) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val title = when (ui) {
            is UiState.Data -> {
                val s = ui.state
                if (s.sale == null) "אין סיבוב פעיל"
                else if (s.remaining <= 0) "${s.sale.emoji} נגמר!"
                else "${s.sale.emoji} נשארו ${s.remaining} מתוך ${s.sale.qtyTotal}"
            }
            else -> "תפוח אדמה חם"
        }
        Text(title, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ChipButton("▾") { actions.onToggleExpanded() }
            ChipButton("✕") { actions.onCloseBubble() }
        }
    }
}

@Composable
private fun DataSections(state: HpState, actions: BubbleActions) {
    val sale = state.sale
    if (sale == null || sale.status != "active") {
        Text("התחל סיבוב חדש מתוך האפליקציה.", fontSize = 14.sp)
        return
    }

    state.currentStop?.let { stop ->
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.padding(end = 8.dp)) {
                    Text("🚗 עצירה נוכחית", fontSize = 12.sp, color = Color(0xFF546E7A))
                    Text(stop.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                BigButton("📞 חייג", OkGreen) { actions.onCall(stop.phone) }
            }
        }
    }

    if (state.pending.isNotEmpty()) {
        SectionTitle("ממתינים לאישור (${state.pending.size})")
        state.pending.forEach { claim -> PendingCard(claim, actions) }
    }

    if (state.confirmed.isNotEmpty()) {
        SectionTitle("מאושרים (${state.confirmed.size})")
        state.confirmed.forEach { claim -> ConfirmedRow(claim, actions) }
    }

    if (state.delivered.isNotEmpty()) {
        SectionTitle("נמסרו (${state.delivered.size})")
        state.delivered.forEach { claim ->
            Text(
                "✅ ${claim.name} ×${claim.qty}" + if (claim.paid == false) "  💸 לא שילם" else "",
                fontSize = 13.sp,
                color = Color(0xFF607D8B),
            )
        }
    }

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("מסירה מחוץ לוואטסאפ:", fontSize = 13.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChipButton("‑1") { actions.onAdjust(-1) }
            ChipButton("+1") { actions.onAdjust(1) }
        }
    }

    BigButton("סיים סיבוב", GoneRed, fillWidth = true) { actions.onEndSale() }
}

@Composable
private fun PendingCard(claim: Claim, actions: BubbleActions) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(claim.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(claim.text, fontSize = 13.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                QtyPicker(claim, actions)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BigButton("✗", Color(0xFF78909C)) { actions.onReject(claim) }
                    BigButton("✓ אשר", OkGreen) { actions.onConfirm(claim, claim.qty) }
                }
            }
        }
    }
}

@Composable
private fun QtyPicker(claim: Claim, actions: BubbleActions) {
    // Quick qty override: confirm with 1/2/3 in one tap.
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        listOf(1, 2, 3).forEach { n ->
            val selected = claim.qty == n
            Surface(
                shape = CircleShape,
                color = if (selected) PendingOrange else Color(0xFFECEFF1),
                modifier = Modifier
                    .size(34.dp)
                    .clickable { actions.onConfirm(claim, n) },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "$n",
                        color = if (selected) Color.White else Color(0xFF455A64),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun ConfirmedRow(claim: Claim, actions: BubbleActions) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "${claim.name} ×${claim.qty}",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(end = 6.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ChipButton("📞") { actions.onCall(claim.phone) }
            ChipButton("🚗") { actions.onSetStop(claim) }
            ChipButton("💰 נמסר") { actions.onDelivered(claim, paid = true) }
            ChipButton("🎁") { actions.onDelivered(claim, paid = false) }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF546E7A))
}

@Composable
private fun ChipButton(text: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFFECEFF1),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Text(
            text,
            Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            fontSize = 14.sp,
            color = Color(0xFF37474F),
        )
    }
}

@Composable
private fun BigButton(
    text: String,
    color: Color,
    fillWidth: Boolean = false,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color,
        modifier = (if (fillWidth) Modifier.fillMaxWidth() else Modifier).clickable(onClick = onClick),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text,
                Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
            )
        }
    }
}
