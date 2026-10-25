package com.reverb.app.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.reverb.core.designsystem.theme.ReverbTheme
import com.reverb.core.model.ReverbTone

// ────────────────────────────────────────────────────────────────────
// BubbleState shared between service and overlay
// ────────────────────────────────────────────────────────────────────
sealed class BubbleUiState {
    object Idle : BubbleUiState()
    object Menu : BubbleUiState()
    object Loading : BubbleUiState()
    data class Done(val preview: String, val reason: String? = null) : BubbleUiState()
    data class Error(val message: String) : BubbleUiState()
    object SmartReplyMenu : BubbleUiState()
}

/**
 * Draws and manages the floating Reverb bubble.
 *
 * The bubble is a Compose view attached to a `WindowManager` overlay. It snaps to the
 * screen edge, opens the tone menu, the Smart Reply menu and the Undo action, and shows
 * loading, done and error states. All callbacks are handled by [ReverbForegroundService].
 */
class ReverbBubbleController(
    private val context: Context,
    private val onToneSelected: (ReverbTone) -> Unit,
    private val onReplySelected: (ReverbTone) -> Unit,
    private val onUndoSelected: () -> Unit,
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var containerView: View? = null
    private var lifecycleStub: LifecycleOwnerStub? = null
    private val uiState: MutableState<BubbleUiState> = mutableStateOf(BubbleUiState.Idle)
    private val canUndoState: MutableState<Boolean> = mutableStateOf(false)
    private var lastX = 0
    private var lastY = 0

    fun show(): Boolean {
        if (containerView != null) return true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
            android.widget.Toast.makeText(context, "Overlay permission revoked. Reverb hidden.", android.widget.Toast.LENGTH_SHORT).show()
            return false
        }

        lifecycleStub = LifecycleOwnerStub()
        val lifecycle = lifecycleStub!!
        val vmStore = ViewModelStoreOwnerStub()
        val savedStateOwner = SavedStateRegistryOwnerStub()

        val params = bubbleLayoutParams()
        val frame = FrameLayout(context)

        // CRITICAL FIX: Compose traverses the view tree upward to find LifecycleOwner.
        // If the outer FrameLayout doesn't have one set, it crashes with
        // "ViewTreeLifecycleOwner not found". We must attach all owners to the root frame.
        frame.setViewTreeLifecycleOwner(lifecycle)
        frame.setViewTreeViewModelStoreOwner(vmStore)
        frame.setViewTreeSavedStateRegistryOwner(savedStateOwner)

        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels

        val composeView = ComposeView(context).apply {
            setViewTreeLifecycleOwner(lifecycle)
            setViewTreeViewModelStoreOwner(vmStore)
            setViewTreeSavedStateRegistryOwner(savedStateOwner)
            setContent {
                ReverbTheme {
                    ReverbBubble(
                        uiState = uiState.value,
                        onBubbleTap = { uiState.value = BubbleUiState.Menu },
                        onToneSelected = { tone ->
                            uiState.value = BubbleUiState.Loading
                            onToneSelected(tone)
                        },
                        onReplySelected = { tone ->
                            uiState.value = BubbleUiState.Loading
                            // Launch the ghost activity from here instead of the service!
                            val intent = android.content.Intent(context, com.reverb.app.clipboard.GhostClipboardActivity::class.java).apply {
                                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_NO_ANIMATION)
                                putExtra("TONE_NAME", tone.name)
                            }
                            context.startActivity(intent)
                        },
                        onReplyFlowSelected = {
                            uiState.value = BubbleUiState.SmartReplyMenu
                        },
                        canUndo = canUndoState.value,
                        onUndoSelected = onUndoSelected,
                        onDismiss = { uiState.value = BubbleUiState.Idle },
                        onDrag = { dx, dy ->
                            params.x += dx.toInt()
                            params.y += dy.toInt()
                            lastX = params.x
                            lastY = params.y
                            try {
                                windowManager.updateViewLayout(frame, params)
                            } catch (e: Exception) {}
                        },
                        onDragEnd = {
                            val targetX = if (params.x < screenWidth / 2) 0 else screenWidth
                            val animator = android.animation.ValueAnimator.ofInt(params.x, targetX)
                            animator.duration = 200
                            animator.addUpdateListener { anim ->
                                params.x = anim.animatedValue as Int
                                lastX = params.x
                                try {
                                    windowManager.updateViewLayout(frame, params)
                                } catch (e: Exception) {}
                            }
                            animator.start()
                        },
                    )
                }
            }
        }

        frame.addView(composeView)
        params.x = lastX
        params.y = lastY

        windowManager.addView(frame, params)
        containerView = frame
        lifecycle.start()
        return true
    }

    fun hide() {
        lifecycleStub?.stop()
        lifecycleStub = null

        containerView?.let { frame ->
            try {
                // IMPORTANT: Must dispose the Compose view to detach it from the dead lifecycle,
                // otherwise relaunching the bubble crashes silently when it tries to reuse the Compose state.
                val composeView = (frame as? FrameLayout)?.getChildAt(0) as? ComposeView
                composeView?.disposeComposition()

                windowManager.removeView(frame)
            } catch (e: Exception) {
                android.util.Log.e("ReverbBubble", "Failed to remove bubble view", e)
            }
        }
        containerView = null
    }

    fun setDone(preview: String, reason: String? = null) {
        uiState.value = BubbleUiState.Done(preview, reason)
    }

    fun setError(msg: String) {
        uiState.value = BubbleUiState.Error(msg)
    }

    fun resetToIdle() {
        uiState.value = BubbleUiState.Idle
    }

    fun setCanUndo(canUndo: Boolean) {
        canUndoState.value = canUndo
    }

    private fun bubbleLayoutParams() = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        },
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        x = 24
        y = 300
    }
}

// ────────────────────────────────────────────────────────────────────
// Compose UI for the bubble
// ────────────────────────────────────────────────────────────────────
@Composable
private fun ReverbBubble(
    uiState: BubbleUiState,
    onBubbleTap: () -> Unit,
    onToneSelected: (ReverbTone) -> Unit,
    onReplySelected: (ReverbTone) -> Unit,
    onReplyFlowSelected: () -> Unit,
    canUndo: Boolean,
    onUndoSelected: () -> Unit,
    onDismiss: () -> Unit,
    onDrag: (Float, Float) -> Unit,
    onDragEnd: () -> Unit,
) {
    AnimatedContent(
        targetState = uiState,
        transitionSpec = {
            (fadeIn(spring()) + scaleIn(spring(stiffness = Spring.StiffnessMedium))) togetherWith
                (fadeOut() + scaleOut())
        },
        label = "bubble_state",
    ) { state ->
        when (state) {
            is BubbleUiState.Idle -> IdleBubble(onTap = onBubbleTap, onDrag = onDrag, onDragEnd = onDragEnd)
            is BubbleUiState.Menu -> ToneMenu(
                onToneSelected = onToneSelected,
                onReplyFlowSelected = onReplyFlowSelected,
                canUndo = canUndo,
                onUndoSelected = onUndoSelected,
                onDismiss = onDismiss,
            )
            is BubbleUiState.Loading -> LoadingBubble(onDismiss = onDismiss)
            is BubbleUiState.Done -> DoneBubble(preview = state.preview, reason = state.reason, onDismiss = onDismiss)
            is BubbleUiState.Error -> ErrorBubble(message = state.message, onDismiss = onDismiss)
            is BubbleUiState.SmartReplyMenu -> SmartReplyMenuBubble(
                onReplySelected = onReplySelected,
                onDismiss = onDismiss,
            )
        }
    }
}

@Composable
private fun IdleBubble(onTap: () -> Unit, onDrag: (Float, Float) -> Unit, onDragEnd: () -> Unit) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .background(
                color = Color(0xFF0D1117),
                shape = CircleShape,
            )
            .clip(CircleShape)
            .clickable(onClick = onTap)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = onDragEnd,
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount.x, dragAmount.y)
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(id = com.reverb.core.designsystem.R.drawable.ic_reverb_logo),
            contentDescription = "Reverb Logo",
            modifier = Modifier.size(32.dp),
        )
    }
}

@Composable
private fun LoadingBubble(onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .background(Color(0xFF355C7D), CircleShape)
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(28.dp),
            color = Color.White,
            strokeWidth = 2.5.dp,
        )
    }
}

@Composable
private fun ToneMenu(
    onToneSelected: (ReverbTone) -> Unit,
    onReplyFlowSelected: () -> Unit,
    canUndo: Boolean,
    onUndoSelected: () -> Unit,
    onDismiss: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF172026)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .heightIn(max = 240.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (canUndo) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f))
                            .clickable {
                                onUndoSelected()
                                onDismiss()
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("↩️", fontSize = 16.sp)
                    }
                }
            }
            Text("Pick a tone", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
            Spacer(Modifier.height(4.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ToneButton(
                    emoji = "👔",
                    label = "Pro",
                    onClick = { onToneSelected(ReverbTone.PROFESSIONAL) },
                    modifier = Modifier.weight(1f),
                )
                ToneButton(
                    emoji = "👋",
                    label = "Friendly",
                    onClick = { onToneSelected(ReverbTone.FRIENDLY) },
                    modifier = Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ToneButton(
                    emoji = "🔥",
                    label = "Edgy",
                    onClick = { onToneSelected(ReverbTone.EDGY) },
                    modifier = Modifier.weight(1f),
                )
                ToneButton(
                    emoji = "📣",
                    label = "Promo",
                    onClick = { onToneSelected(ReverbTone.PROMOTION) },
                    modifier = Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ToneButton(
                    emoji = "✅",
                    label = "Fact",
                    onClick = { onToneSelected(ReverbTone.FACT_CHECK) },
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.weight(1f))
            }

            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF3B82F6).copy(alpha = 0.2f))
                    .clickable(onClick = onReplyFlowSelected)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("✨", fontSize = 16.sp, modifier = Modifier.padding(end = 8.dp))
                Text("Smart Reply", style = MaterialTheme.typography.labelMedium, color = Color.White, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "✕ Cancel",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.4f),
                modifier = Modifier.clickable(onClick = onDismiss),
            )
        }
    }
}

@Composable
private fun ToneButton(emoji: String, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(emoji, fontSize = 16.sp, modifier = Modifier.padding(end = 4.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = Color.White, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun DoneBubble(preview: String, reason: String? = null, onDismiss: () -> Unit) {
    var showReason by remember { mutableStateOf(false) }
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF162B1A)),
        elevation = CardDefaults.cardElevation(8.dp),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Text("✅ Done!", color = Color(0xFF22C55E), fontWeight = FontWeight.Bold)
                if (reason != null) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f))
                            .clickable { showReason = !showReason }
                            .padding(4.dp),
                    ) {
                        Text("❓", fontSize = 12.sp)
                    }
                }
            }
            if (showReason && reason != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Reason: $reason",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFEAB308),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
            if (preview.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = preview.take(80) + if (preview.length > 80) "…" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.75f),
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Tap to close",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.4f),
                modifier = Modifier.clickable(onClick = onDismiss),
            )
        }
    }
}

@Composable
private fun ErrorBubble(message: String, onDismiss: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A1616)),
        elevation = CardDefaults.cardElevation(8.dp),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("⚠️ Error", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                text = message.take(80),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.75f),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Tap to dismiss",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.4f),
                modifier = Modifier.clickable(onClick = onDismiss),
            )
        }
    }
}

@Composable
private fun SmartReplyMenuBubble(
    onReplySelected: (ReverbTone) -> Unit,
    onDismiss: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF172026)),
        elevation = CardDefaults.cardElevation(8.dp),
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .heightIn(max = 240.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("Auto-Reply options", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ToneButton(
                    emoji = "👍",
                    label = "Accept",
                    onClick = { onReplySelected(ReverbTone.PROFESSIONAL) },
                    modifier = Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ToneButton(
                    emoji = "👎",
                    label = "Decline",
                    onClick = { onReplySelected(ReverbTone.EDGY) },
                    modifier = Modifier.weight(1f),
                )
                ToneButton(
                    emoji = "🤔",
                    label = "Clarify",
                    onClick = { onReplySelected(ReverbTone.FRIENDLY) },
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "✕ Cancel",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.4f),
                modifier = Modifier.clickable(onClick = onDismiss),
            )
        }
    }
}

// ────────────────────────────────────────────────────────────────────
// Lifecycle stubs needed to host Compose in a WindowManager view
// ────────────────────────────────────────────────────────────────────
private class LifecycleOwnerStub : LifecycleOwner, ViewModelStoreOwner {
    private val registry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = registry
    override val viewModelStore = ViewModelStore()
    fun start() {
        registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }
    fun stop() {
        registry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }
}

private class ViewModelStoreOwnerStub : ViewModelStoreOwner {
    override val viewModelStore = ViewModelStore()
}

private class SavedStateRegistryOwnerStub : SavedStateRegistryOwner {
    private val controller = SavedStateRegistryController.create(this)
    init {
        controller.performAttach()
        controller.performRestore(null)
    }
    override val lifecycle: Lifecycle get() = LifecycleRegistry(this)
    override val savedStateRegistry: SavedStateRegistry get() = controller.savedStateRegistry
}
