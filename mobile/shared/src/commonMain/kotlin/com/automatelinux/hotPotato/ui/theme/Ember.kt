package com.automatelinux.hotPotato.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * "Ember" — the app's design language, lifted straight out of the launcher icon
 * (`res/drawable/ic_launcher_background.xml` + `ic_launcher_foreground.xml`).
 *
 * Every colour below already exists in that icon, so the home-screen icon, the
 * launcher screen and the floating bubble read as one object rather than three.
 * Nothing here was invented: if you need a new colour, take it from the icon or
 * add it to the icon first.
 */
object Ember {
    /** The icon's background gradient: amber -> orange -> burnt orange. */
    val Top = Color(0xFFFF8F00)
    val Mid = Color(0xFFF4610E)
    val Deep = Color(0xFFD84315)

    /** The three flames licking up under the potato. */
    val FlameAmber = Color(0xFFFFC107)
    val FlameLight = Color(0xFFFFD54F)
    val FlameCore = Color(0xFFFFF8E1)

    /** The potato body, its highlight and its speckles. */
    val PotatoDark = Color(0xFF8D5B3F)
    val PotatoBody = Color(0xFFC08552)
    val PotatoLight = Color(0xFFD8A878)

    /** The potato's face is drawn in this brown — so is every line of text. */
    val Ink = Color(0xFF4E342E)
    val InkSoft = Color(0xFF8D6E63)
    val Blush = Color(0xFFE57373)

    /** Warm neutrals extended from FlameCore, for surfaces that sit on the heat. */
    val Cream = Color(0xFFFFF8F0)
    val CreamSunk = Color(0xFFFBEADA)
    val CreamEdge = Color(0xFFF1DCC8)
    val Ash = Color(0xFF7D6E66)
    val AshDeep = Color(0xFF5A4F49)

    /**
     * State accents. The bubble's traffic light is load-bearing — green = selling,
     * orange = claims waiting, red = sold out — so the hues keep their meaning and
     * only the material changes. See BubbleUi.
     */
    val Go = Color(0xFF43A047)
    val GoDeep = Color(0xFF2E7D32)
    val Sold = Color(0xFFE53935)
    val SoldDeep = Color(0xFFB71C1C)

    /** The icon's ground, for full-bleed backdrops. */
    val heat = Brush.verticalGradient(listOf(Top, Mid, Deep))

    /** The same heat, laid across a button or a header band. */
    val heatWide = Brush.horizontalGradient(listOf(Deep, Mid, Top))

    val flame = Brush.verticalGradient(listOf(FlameLight, FlameAmber))
    val goGradient = Brush.horizontalGradient(listOf(Go, GoDeep))
    val soldGradient = Brush.horizontalGradient(listOf(Sold, SoldDeep))
    val ashGradient = Brush.horizontalGradient(listOf(Ash, AshDeep))

    /** The soft white bloom the icon paints behind the potato. */
    val glow = Brush.radialGradient(listOf(Color(0x40FFFFFF), Color(0x00FFFFFF)))
}
