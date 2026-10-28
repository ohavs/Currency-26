package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.premium.Access
import com.example.premium.PremiumReason
import com.example.premium.PremiumRules
import com.example.premium.QuickConvertAllowance
import com.example.premium.VideoStatus
import com.example.ui.theme.LocalPalette
import com.example.utils.formatUntil
import kotlinx.coroutines.delay

/**
 * Premium: what it opens (the feature the user reached for comes first), the free way in - a couple of short
 * videos for a day - and the one-time purchase, which also removes the ads. The free part of the app is
 * named at the bottom, so nobody thinks converting is locked.
 */
@Composable
fun PremiumSheet(
    reason: PremiumReason,
    access: Access,
    videosNeeded: Int,
    price: String?,
    video: VideoStatus,
    purchasePending: Boolean,
    onWatch: () -> Unit,
    onBuy: () -> Unit,
    onRestore: () -> Unit,
    onDismiss: () -> Unit,
    now: Long = System.currentTimeMillis(),
) {
    val palette = LocalPalette.current
    val appear = remember { Animatable(0f) }
    // Same guard as the tour: a tap meant for what was under the finger must not hit a button here.
    var settled by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        appear.animateTo(1f, tween(340, easing = FastOutSlowInEasing))
        delay(350)
        settled = true
    }
    BackHandler(onBack = onDismiss)
    fun guarded(action: () -> Unit): () -> Unit = { if (settled) action() }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = appear.value }
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = guarded(onDismiss))
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .safeDrawingPadding()
                .padding(12.dp)
                .graphicsLayer {
                    translationY = (1f - appear.value) * size.height * 0.6f
                    alpha = appear.value
                }
                .clip(RoundedCornerShape(32.dp))
                .background(palette.background)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(palette.accent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.WorkspacePremium, contentDescription = null, tint = palette.onAccent, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.premium), style = MaterialTheme.typography.titleLarge, color = palette.ink)
                    Text(stringResource(R.string.premium_subtitle), style = MaterialTheme.typography.bodyMedium, color = palette.inkMuted)
                }
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.close),
                    tint = palette.inkMuted,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(role = Role.Button, onClick = guarded(onDismiss))
                        .padding(6.dp)
                        .size(20.dp)
                )
            }

            val status = premiumStatus(access, purchasePending, now)
            if (status != null) {
                Spacer(Modifier.height(14.dp))
                Text(
                    status,
                    style = MaterialTheme.typography.labelLarge,
                    color = palette.ink,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(palette.highlight)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }

            Spacer(Modifier.height(16.dp))
            val features = listOf(
                PremiumReason.WIDGET to (Icons.Rounded.Widgets to R.string.premium_feature_widget),
                PremiumReason.COLORS to (Icons.Rounded.Palette to R.string.premium_feature_colors),
                PremiumReason.CALCULATOR to (Icons.Rounded.Calculate to R.string.premium_feature_calculator),
                PremiumReason.EXTRA_CURRENCY to (Icons.AutoMirrored.Rounded.PlaylistAdd to R.string.premium_feature_extra),
                PremiumReason.QUICK_CONVERT to (Icons.Rounded.TouchApp to R.string.premium_feature_quick),
            ).sortedByDescending { it.first == reason }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                features.forEach { (key, feature) ->
                    FeatureRow(feature.first, stringResource(feature.second), highlighted = key == reason)
                }
            }

            if (access.purchased) {
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.premium_owned), style = MaterialTheme.typography.bodyLarge, color = palette.ink)
            } else {
                Spacer(Modifier.height(18.dp))
                VideoButton(videosNeeded, video, onClick = guarded(onWatch))
                val note = when {
                    video == VideoStatus.UNAVAILABLE -> stringResource(R.string.premium_video_unavailable)
                    access.unlocked && !access.trial && videosNeeded == PremiumRules.VIDEOS_PER_UNLOCK ->
                        stringResource(R.string.premium_extend, PremiumRules.VIDEOS_PER_UNLOCK, PremiumRules.REWARD_HOURS)
                    else -> null
                }
                if (note != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(note, style = MaterialTheme.typography.bodySmall, color = palette.inkMuted, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                }

                Spacer(Modifier.height(10.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .border(2.dp, palette.accent, RoundedCornerShape(24.dp))
                        .clickable(enabled = !purchasePending, role = Role.Button, onClick = guarded(onBuy))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        if (price != null) stringResource(R.string.premium_buy, price) else stringResource(R.string.premium_buy_plain),
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.ink,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        stringResource(R.string.premium_buy_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.inkMuted,
                        textAlign = TextAlign.Center
                    )
                }
                Text(
                    stringResource(R.string.premium_restore),
                    style = MaterialTheme.typography.labelLarge,
                    color = palette.inkMuted,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 6.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(role = Role.Button, onClick = guarded(onRestore))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }

            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.premium_free_note, QuickConvertAllowance.FREE_PER_WEEK),
                style = MaterialTheme.typography.bodySmall,
                color = palette.inkMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/** "Free trial until Thu 14:30", "Unlocked until today 18:00", "waiting for the payment"; null when locked. */
@Composable
internal fun premiumStatus(access: Access, purchasePending: Boolean, now: Long = System.currentTimeMillis()): String? {
    val until = access.until
    return when {
        access.purchased -> null
        purchasePending -> stringResource(R.string.premium_pending)
        until == null -> null
        else -> {
            val time = formatUntil(
                until, now, LocalAppLocale.current,
                todayFormat = stringResource(R.string.today_at),
                tomorrowFormat = stringResource(R.string.tomorrow_at)
            )
            stringResource(if (access.trial) R.string.premium_trial_until else R.string.premium_until, time)
        }
    }
}

@Composable
private fun FeatureRow(icon: ImageVector, text: String, highlighted: Boolean) {
    val palette = LocalPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (highlighted) palette.highlight else palette.card)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = palette.accent, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge, color = palette.ink)
    }
}

/** "24 hours free · Watch 2 short videos", then "Video 2 of 2" with the progress dots, or loading. */
@Composable
private fun VideoButton(videosNeeded: Int, video: VideoStatus, onClick: () -> Unit) {
    val palette = LocalPalette.current
    val total = PremiumRules.VIDEOS_PER_UNLOCK
    val watched = total - videosNeeded
    val busy = video == VideoStatus.LOADING || video == VideoStatus.SHOWING
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp)
            .clip(CircleShape)
            .background(palette.accent)
            .clickable(enabled = !busy, role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        AnimatedContent(
            targetState = busy,
            transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(100)) },
            label = "videoButton"
        ) { loading ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = palette.onAccent, strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(stringResource(R.string.premium_video_loading), style = MaterialTheme.typography.titleMedium, color = palette.onAccent)
                } else {
                    Icon(Icons.Rounded.PlayCircle, contentDescription = null, tint = palette.onAccent, modifier = Modifier.size(26.dp))
                    Spacer(Modifier.width(12.dp))
                    // Not started: "24 hours free" over "Watch 2 short videos"; halfway: "Video 2 of 2" over the hours.
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            if (watched == 0) {
                                stringResource(R.string.premium_free_hours, PremiumRules.REWARD_HOURS)
                            } else {
                                stringResource(R.string.premium_watch_next, watched + 1, total)
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.onAccent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            if (watched == 0) {
                                stringResource(R.string.premium_watch, total)
                            } else {
                                stringResource(R.string.premium_free_hours, PremiumRules.REWARD_HOURS)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.onAccent.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (watched > 0) {
                        Spacer(Modifier.width(12.dp))
                        repeat(total) { index ->
                            Box(
                                Modifier
                                    .padding(horizontal = 2.dp)
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (index < watched) palette.onAccent else palette.onAccent.copy(alpha = 0.35f))
                            )
                        }
                    }
                }
            }
        }
    }
}

/** A small lock on a Premium key or tile while it is locked. */
@Composable
internal fun BoxScope.LockBadge(modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    Box(
        modifier = modifier
            .align(Alignment.TopEnd)
            .padding(5.dp)
            .size(18.dp)
            .clip(CircleShape)
            .background(palette.accent),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Rounded.Lock, contentDescription = stringResource(R.string.premium_locked), tint = palette.onAccent, modifier = Modifier.size(11.dp))
    }
}

/** The app's own line in the ad strip until an ad arrives (or when none does): what Premium gives. */
@Composable
fun PremiumPromo(onClick: () -> Unit) {
    val palette = LocalPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .clip(RoundedCornerShape(16.dp))
            .background(palette.cardSoft)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Rounded.WorkspacePremium, contentDescription = null, tint = palette.accent, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            stringResource(R.string.premium_promo),
            style = MaterialTheme.typography.labelLarge,
            color = palette.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
