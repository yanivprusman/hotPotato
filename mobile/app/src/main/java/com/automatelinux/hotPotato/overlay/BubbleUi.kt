package com.automatelinux.hotPotato.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.automatelinux.hotPotato.hp.Claim
import com.automatelinux.hotPotato.hp.HpState
import com.automatelinux.hotPotato.ui.theme.Ember
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
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        if (expanded) {
            ExpandedPanel(ui, actionError, actions)
        } else {
            CollapsedBubble(ui, actions)
        }
    }
}

/**
 * The bubble is read at a glance, mid-delivery, so its traffic light keeps its
 * meaning: green = selling, orange = claims waiting, red = sold out, ash = no
 * round. Only the material changed — each state is now the icon's gradient-and-
 * glow treatment instead of a flat disc, and "waiting" is literally the icon's
 * own heat.
 */
private fun bubbleBrush(ui: UiState): Brush = when (ui) {
    is UiState.Loading -> Ember.ashGradient
    is UiState.Error -> Ember.ashGradient
    is UiState.Data ->
        if (ui.state.sale == null || ui.state.sale.status != "active") Ember.ashGradient
        else if (ui.state.remaining <= 0) Ember.soldGradient
        else if (ui.state.pending.isNotEmpty()) Ember.heatWide
        else Ember.goGradient
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
        Box(
            modifier = Modifier
                .size(62.dp)
                .align(Alignment.Center)
                .shadow(
                    elevation = 6.dp,
                    shape = CircleShape,
                    ambientColor = Ember.Deep,
                    spotColor = Ember.Deep,
                )
                .clip(CircleShape)
                .background(bubbleBrush(ui))
                .border(2.dp, Ember.FlameCore.copy(alpha = 0.55f), CircleShape)
                .clickable { actions.onToggleExpanded() },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                label,
                color = Ember.FlameCore,
                fontSize = if (label == "נגמר") 16.sp else 18.sp,
                fontWeight = FontWeight.Black,
            )
        }
        if (pendingCount > 0) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.TopEnd)
                    .clip(CircleShape)
                    .background(Ember.FlameCore)
                    .border(1.5.dp, Ember.Deep, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "$pendingCount",
                    color = Ember.Deep,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                )
            }
        }
    }
}

@Composable
private fun ExpandedPanel(ui: UiState, actionError: String?, actions: BubbleActions) {
    Card(
        modifier = Modifier.width(330.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Ember.Cream),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
    ) {
        Column {
            HeaderBand(ui, actions)
            Column(
                Modifier
                    .padding(12.dp)
                    .heightIn(max = 500.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (actionError != null) {
                    Text(actionError, color = Ember.SoldDeep, fontSize = 13.sp)
                }
                when (ui) {
                    is UiState.Loading -> Text("טוען…", color = Ember.InkSoft, fontSize = 14.sp)
                    is UiState.Error ->
                        Text("שרת לא זמין: ${ui.message}", color = Ember.SoldDeep, fontSize = 13.sp)
                    is UiState.Data -> DataSections(ui.state, actions)
                }
            }
        }
    }
}

/** The panel's header carries the same heat as the icon's ground. */
@Composable
private fun HeaderBand(ui: UiState, actions: BubbleActions) {
    val title = when (ui) {
        is UiState.Data -> {
            val s = ui.state
            if (s.sale == null) "אין סיבוב פעיל"
            else if (s.remaining <= 0) "${s.sale.emoji} נגמר!"
            else "${s.sale.emoji} נשארו ${s.remaining} מתוך ${s.sale.qtyTotal}"
        }
        else -> "🥔 תפוח אדמה חם"
    }
    Row(
        Modifier
            .fillMaxWidth()
            .background(bubbleBrush(ui))
            .padding(horizontal = 12.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            color = Ember.FlameCore,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(end = 8.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            BandButton("▾") { actions.onToggleExpanded() }
            BandButton("✕") { actions.onCloseBubble() }
        }
    }
}

@Composable
private fun DataSections(state: HpState, actions: BubbleActions) {
    val sale = state.sale
    if (sale == null || sale.status != "active") {
        Text("התחל סיבוב חדש מתוך האפליקציה.", color = Ember.Ink, fontSize = 14.sp)
        return
    }

    state.currentStop?.let { stop ->
        Card(
            colors = CardDefaults.cardColors(containerColor = Ember.CreamSunk),
            shape = RoundedCornerShape(14.dp),
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
                    Text("🚗 עצירה נוכחית", fontSize = 12.sp, color = Ember.PotatoDark)
                    Text(
                        stop.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Ember.Ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                BigButton("📞 חייג", Ember.goGradient) { actions.onCall(stop.phone) }
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
                color = Ember.InkSoft,
            )
        }
    }

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("מסירה מחוץ לוואטסאפ:", fontSize = 13.sp, color = Ember.Ink)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChipButton("‑1") { actions.onAdjust(-1) }
            ChipButton("+1") { actions.onAdjust(1) }
        }
    }

    BigButton("סיים סיבוב", Ember.soldGradient, fillWidth = true) { actions.onEndSale() }
}

@Composable
private fun PendingCard(claim: Claim, actions: BubbleActions) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF1E0)),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, Ember.FlameAmber.copy(alpha = 0.65f), RoundedCornerShape(14.dp)),
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(claim.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Ember.Ink)
            Text(
                claim.text,
                fontSize = 13.sp,
                color = Ember.InkSoft,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                QtyPicker(claim, actions)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BigButton("✗", Ember.ashGradient) { actions.onReject(claim) }
                    BigButton("✓ אשר", Ember.goGradient) { actions.onConfirm(claim, claim.qty) }
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
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .then(
                        if (selected) {
                            Modifier.background(Ember.heatWide)
                        } else {
                            Modifier
                                .background(Ember.Cream)
                                .border(1.5.dp, Ember.CreamEdge, CircleShape)
                        },
                    )
                    .clickable { actions.onConfirm(claim, n) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "$n",
                    color = if (selected) Ember.FlameCore else Ember.Ink,
                    fontWeight = FontWeight.Bold,
                )
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
            color = Ember.Ink,
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
    Text(
        text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp,
        color = Ember.PotatoDark,
    )
}

@Composable
private fun ChipButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Ember.CreamSunk)
            .border(1.dp, Ember.CreamEdge, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
    ) {
        Text(
            text,
            Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Ember.Ink,
        )
    }
}

/** Header chips sit on the heat band, so they are cream-on-translucent. */
@Composable
private fun BandButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.22f))
            .clickable(onClick = onClick),
    ) {
        Text(
            text,
            Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Ember.FlameCore,
        )
    }
}

@Composable
private fun BigButton(
    text: String,
    brush: Brush,
    fillWidth: Boolean = false,
    onClick: () -> Unit,
) {
    Box(
        modifier = (if (fillWidth) Modifier.fillMaxWidth() else Modifier)
            .clip(RoundedCornerShape(12.dp))
            .background(brush)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            color = Ember.FlameCore,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
        )
    }
}
