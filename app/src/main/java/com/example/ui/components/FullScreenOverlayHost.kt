package com.example.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex

/**
 * Root-level overlay portal rendered directly inside the Activity's edge-to-edge window
 * (above NavigationRail, NavHost, floating bottom dock, and banners).
 *
 * Rendering full-screen media viewers here instead of inside a separate floating Android
 * Dialog window guarantees exact screen bounds and unmodified Compose WindowInsets across
 * Android 14/15/16 and OEM skins (including Samsung One UI display cutouts and gesture/3-button bars).
 */
@Stable
class FullScreenOverlayHost {
    internal class Entry(
        val id: Any,
        initialContent: @Composable () -> Unit
    ) {
        var content by mutableStateOf(initialContent)
    }

    internal val entries = mutableStateListOf<Entry>()

    fun upsert(id: Any, content: @Composable () -> Unit) {
        val existing = entries.firstOrNull { it.id === id }
        if (existing != null) {
            existing.content = content
        } else {
            entries.add(Entry(id, content))
        }
    }

    fun remove(id: Any) {
        entries.removeAll { it.id === id }
    }
}

val LocalFullScreenOverlayHost = staticCompositionLocalOf<FullScreenOverlayHost?> { null }

@Composable
fun FullScreenOverlayLayer(
    host: FullScreenOverlayHost,
    modifier: Modifier = Modifier
) {
    host.entries.forEach { entry ->
        key(entry.id) {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .zIndex(100f)
            ) {
                entry.content()
            }
        }
    }
}
