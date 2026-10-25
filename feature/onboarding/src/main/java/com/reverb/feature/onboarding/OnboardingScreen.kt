package com.reverb.feature.onboarding

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.reverb.core.domain.storage.StyleSamples

@Composable
internal fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    versionName: String,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onStartBackgroundService: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    // Re-check permissions whenever we return to the screen
    LaunchedEffect(Unit) {
        viewModel.checkPermissions(context)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { HeroSection(versionName = versionName) }
            item {
                ApiKeyCard(
                    apiKey = state.apiKey,
                    apiKeySaved = state.apiKeySaved,
                    validationError = state.validationError,
                    onApiKeyChanged = viewModel::onApiKeyChanged,
                    onSave = viewModel::saveApiKey,
                    onClear = viewModel::clearApiKey,
                )
            }
            item {
                PermissionsCard(
                    hasAccessibility = state.hasAccessibilityPermission,
                    hasOverlay = state.hasOverlayPermission,
                    onOpenAccessibilitySettings = {
                        onOpenAccessibilitySettings()
                    },
                    onOpenOverlaySettings = {
                        onOpenOverlaySettings()
                    },
                    onRefresh = { viewModel.checkPermissions(context) },
                )
            }
            item {
                LaunchCard(
                    enabled = state.apiKeySaved &&
                        state.hasAccessibilityPermission &&
                        state.hasOverlayPermission,
                    serviceStarted = state.serviceStarted,
                    onStart = {
                        onStartBackgroundService()
                        viewModel.onServiceStarted()
                    },
                )
            }
            item { ToneSection(viewModel) }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun HeroSection(versionName: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(
                    color = Color.Black,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = com.reverb.core.designsystem.R.drawable.ic_reverb_logo),
                contentDescription = "Reverb Logo",
                modifier = Modifier.size(40.dp),
            )
        }
        Text(
            text = "Reverb",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Type messy. Look like a genius.",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.60f),
            textAlign = TextAlign.Center,
        )

        val context = LocalContext.current
        val annotatedString = androidx.compose.ui.text.buildAnnotatedString {
            append("v$versionName • Created by ")
            pushStringAnnotation(tag = "URL", annotation = "https://www.vigneshbs.xyz/")
            withStyle(
                style = androidx.compose.ui.text.SpanStyle(
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                ),
            ) {
                append("Vignesh B S")
            }
            pop()
        }

        androidx.compose.foundation.text.ClickableText(
            text = annotatedString,
            style = MaterialTheme.typography.labelSmall.copy(
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
            ),
            modifier = Modifier.padding(top = 8.dp),
            onClick = { offset ->
                annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                    .firstOrNull()?.let { annotation ->
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(annotation.item))
                        context.startActivity(intent)
                    }
            },
        )
    }
}

@Composable
private fun ApiKeyCard(
    apiKey: String,
    apiKeySaved: Boolean,
    validationError: String?,
    onApiKeyChanged: (String) -> Unit,
    onSave: () -> Unit,
    onClear: () -> Unit,
) {
    var showKey by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    ReverbCard(title = "1. Your OpenAI Key 🔑") {
        Text(
            text = "Reverb sends text directly from your phone to OpenAI. Your key is encrypted on-device — we never see it.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )
        Spacer(Modifier.height(12.dp))

        if (!apiKeySaved) {
            OutlinedTextField(
                value = apiKey,
                onValueChange = onApiKeyChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("sk-...") },
                singleLine = true,
                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = {
                    focusManager.clearFocus()
                    onSave()
                }),
                trailingIcon = {
                    IconButton(onClick = { showKey = !showKey }) {
                        Text(if (showKey) "🙈" else "👁️", fontSize = 18.sp)
                    }
                },
                isError = validationError != null,
                supportingText = validationError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    focusManager.clearFocus()
                    onSave()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Save & Encrypt Key")
            }
        } else {
            StatusRow(label = "API Key", ok = true, text = "Saved & encrypted ✓")
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onClear,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text("Clear Key")
            }
        }
    }
}

@Composable
private fun PermissionsCard(
    hasAccessibility: Boolean,
    hasOverlay: Boolean,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onRefresh: () -> Unit,
) {
    ReverbCard(title = "2. Permissions 🔒") {
        Text(
            text = "Reverb needs two permissions to work across your apps:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )
        Spacer(Modifier.height(12.dp))

        StatusRow(
            label = "Accessibility Service",
            ok = hasAccessibility,
            text = if (hasAccessibility) "Enabled ✓" else "Needed to read & replace text",
        )
        if (!hasAccessibility) {
            Spacer(Modifier.height(6.dp))
            FilledTonalButton(onClick = onOpenAccessibilitySettings, modifier = Modifier.fillMaxWidth()) {
                Text("Enable Accessibility")
            }
        }

        Spacer(Modifier.height(10.dp))

        StatusRow(
            label = "Draw Over Other Apps",
            ok = hasOverlay,
            text = if (hasOverlay) "Enabled ✓" else "Needed for the floating bubble",
        )
        if (!hasOverlay) {
            Spacer(Modifier.height(6.dp))
            FilledTonalButton(onClick = onOpenOverlaySettings, modifier = Modifier.fillMaxWidth()) {
                Text("Grant Overlay Permission")
            }
        }

        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) {
            Text("↻  Re-check Permissions")
        }
    }
}

@Composable
private fun LaunchCard(
    enabled: Boolean,
    serviceStarted: Boolean,
    onStart: () -> Unit,
) {
    ReverbCard(title = "3. Launch Reverb 🚀") {
        AnimatedContent(
            targetState = serviceStarted,
            transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
            label = "launch_state",
        ) { started ->
            if (started) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("✅", fontSize = 40.sp)
                    Text(
                        text = "Reverb is active!",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = "Tap the floating \"Bat\" bubble in any app, pick a tone, and watch your text transform.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                    )
                    Button(
                        onClick = onStart,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        ),
                    ) {
                        Text("Start Again")
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!enabled) {
                        Text(
                            text = "Complete steps 1 and 2 above first.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                    }
                    Button(
                        onClick = onStart,
                        enabled = enabled,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                        ),
                    ) {
                        Text("Start Reverb")
                    }
                }
            }
        }
    }
}

@Composable
private fun ToneSection(viewModel: OnboardingViewModel) {
    ReverbCard(title = "Tone Personalization") {
        Text(
            text = "Answer the scenarios below in your authentic voice. Reverb will analyze your style and mimic it when rewriting text.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
        Spacer(Modifier.height(12.dp))

        // Emoji, ToneName, Description, Scenario Question
        val tones = listOf(
            listOf(
                "👔",
                "PROFESSIONAL",
                "Polished, formal, authoritative",
                "Imagine emailing your team about a project. Ask them to prioritize it and finish by Thursday, while offering your help. Write the exact message.",
            ),
            listOf(
                "👋",
                "FRIENDLY",
                "Warm, approachable, conversational",
                "Imagine texting a close friend. You're 20 mins late for dinner. Apologize, tell them to order apps, and show excitement to see them. Write the exact text.",
            ),
            listOf(
                "🔥",
                "EDGY",
                "Bold, punchy, modern energy",
                "Imagine a persistent salesperson is DMing you. Shut down the conversation quickly, clearly, and confidently so they don't message you again. Write your exact reply.",
            ),
            listOf(
                "💬",
                StyleSamples.REPLY_KEY,
                "Smart Reply Settings",
                "Imagine a coworker sent a massive 4-paragraph idea. You agree, but are too busy to write a long response right now. Write a quick reply acknowledging it and suggesting you talk tomorrow.",
            ),
        )

        tones.forEach { toneData ->
            val emoji = toneData[0]
            val toneName = toneData[1]
            val desc = toneData[2]
            val question = toneData[3]
            var expanded by remember { mutableStateOf(false) }
            var draft by remember(toneName) {
                mutableStateOf(viewModel.getCustomInstruction(toneName))
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(emoji, fontSize = 24.sp)
                        Column {
                            Text(
                                toneName.lowercase().replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                desc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                            )
                        }
                    }
                    IconButton(onClick = { expanded = !expanded }) {
                        Text(if (expanded) "✕" else "✏️", fontSize = 18.sp)
                    }
                }

                AnimatedVisibility(visible = expanded) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = question,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                        OutlinedTextField(
                            value = draft,
                            onValueChange = { draft = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Your response") },
                            placeholder = { Text("Type exactly how you would naturally write it...") },
                            minLines = 3,
                            maxLines = 6,
                        )
                        Button(
                            onClick = {
                                viewModel.saveCustomInstruction(toneName, draft)
                                expanded = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Save")
                        }
                    }
                }

                if (tones.last()[1] != toneName) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusRow(label: String, ok: Boolean, text: String) {
    val color by animateColorAsState(
        targetValue = if (ok) Color(0xFF22C55E) else MaterialTheme.colorScheme.secondary,
        label = "status_color",
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color),
        )
        Column {
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
        }
    }
}

@Composable
private fun ReverbCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}
