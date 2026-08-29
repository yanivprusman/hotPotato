package com.automatelinux.hotPotato.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// Multiplatform theme (no Android-only dynamic color, so it compiles for iOS too).
// The scheme is the launcher icon's palette — see Ember.
private val EmberColors = lightColorScheme(
    primary = Ember.Mid,
    onPrimary = Ember.FlameCore,
    primaryContainer = Ember.Top,
    onPrimaryContainer = Ember.Ink,
    secondary = Ember.PotatoBody,
    onSecondary = Ember.FlameCore,
    secondaryContainer = Ember.CreamSunk,
    onSecondaryContainer = Ember.Ink,
    tertiary = Ember.FlameAmber,
    onTertiary = Ember.Ink,
    background = Ember.Cream,
    onBackground = Ember.Ink,
    surface = Ember.Cream,
    onSurface = Ember.Ink,
    surfaceVariant = Ember.CreamSunk,
    onSurfaceVariant = Ember.InkSoft,
    outline = Ember.CreamEdge,
    outlineVariant = Ember.CreamEdge,
    error = Ember.SoldDeep,
    onError = Ember.FlameCore,
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = EmberColors, content = content)
}
