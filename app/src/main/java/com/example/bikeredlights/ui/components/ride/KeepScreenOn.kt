package com.example.bikeredlights.ui.components.ride

import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView

/**
 * Keeps the screen on while this composable is in composition.
 *
 * **Purpose**: Prevents screen from auto-locking during active ride recording.
 *
 * **Implementation**:
 * - Uses DisposableEffect to apply FLAG_KEEP_SCREEN_ON to the window
 * - Automatically releases the flag when composable leaves composition
 * - Safe for configuration changes (rotation, etc.)
 *
 * **Usage**:
 * ```kotlin
 * if (isRecording) {
 *     KeepScreenOn()
 * }
 * ```
 *
 * **Important Notes**:
 * - Only keeps screen on while app is in foreground
 * - Does NOT prevent screen lock when app is backgrounded
 * - Background recording continues via foreground service (not affected)
 * - Wake lock is released when:
 *   1. Recording stops (composable leaves composition)
 *   2. App is backgrounded (Activity loses focus)
 *   3. Configuration change (automatically reapplied)
 *
 * **User Story**: US6 - Screen stays awake during recording
 *
 * **Spec Reference**: spec.md SC-011 (UX Constraint)
 */
@Composable
fun KeepScreenOn() {
    val view = LocalView.current

    DisposableEffect(Unit) {
        // Apply FLAG_KEEP_SCREEN_ON to the window
        val window = view.context.findActivity()?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Release the flag when composable leaves composition
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}

/**
 * Helper extension to find the Activity from a Context.
 *
 * **Rationale**: We need the Activity to access the Window.
 * Context might be wrapped (Application, ContextWrapper, etc.).
 */
private fun android.content.Context.findActivity(): android.app.Activity? {
    var context = this
    while (context is android.content.ContextWrapper) {
        if (context is android.app.Activity) {
            return context
        }
        context = context.baseContext
    }
    return null
}
