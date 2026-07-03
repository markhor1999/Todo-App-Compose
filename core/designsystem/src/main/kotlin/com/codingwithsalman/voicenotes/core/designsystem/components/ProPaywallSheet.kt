package com.codingwithsalman.voicenotes.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.codingwithsalman.voicenotes.core.designsystem.theme.VnTheme

/**
 * The one place Pro is sold. Pure UI — callers own billing state and actions.
 * Design: benefits-first, two price cards (lifetime visually preferred for the
 * anti-subscription audience the listing attracts), restore + privacy reassurance.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProPaywallSheet(
    isPro: Boolean,
    monthlyPrice: String?,
    lifetimePrice: String?,
    onBuyMonthly: () -> Unit,
    onBuyLifetime: () -> Unit,
    onRestore: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 20.dp),
        ) {
            if (isPro) {
                Text(
                    text = "You're Pro. Thank you. 🖤",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Unlimited on-device transcription is active on this phone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(16.dp))
                return@Column
            }

            Text(
                text = "Murmur Pro",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Free gives you unlimited recording, forever. Pro removes the one limit that exists:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(16.dp))

            BenefitRow("∞", "Unlimited transcription", "Free: 10 minutes a day. Pro: no meter, ever.")
            BenefitRow("🌍", "Every model, every language", "Including the 99-language HD model — all offline.")
            BenefitRow("🔒", "Still 100% private", "Pro changes what you can do, never where your data lives.")
            BenefitRow("🌱", "Built by one person", "Your purchase directly funds development.")

            Spacer(modifier = Modifier.height(20.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PriceCard(
                    modifier = Modifier.weight(1f),
                    title = "Monthly",
                    price = monthlyPrice ?: "—",
                    caption = "cancel anytime",
                    highlighted = false,
                    onClick = onBuyMonthly,
                )
                PriceCard(
                    modifier = Modifier.weight(1f),
                    title = "Lifetime",
                    price = lifetimePrice ?: "—",
                    caption = "pay once · founder price",
                    highlighted = true,
                    onClick = onBuyLifetime,
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onRestore) {
                    Text("Restore purchase", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    text = "Billed by Google Play",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun BenefitRow(emoji: String, title: String, body: String) {
    Row(modifier = Modifier.padding(vertical = 7.dp)) {
        Text(text = emoji, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PriceCard(
    modifier: Modifier,
    title: String,
    price: String,
    caption: String,
    highlighted: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier
            .border(
                border = BorderStroke(
                    width = if (highlighted) 2.dp else 1.dp,
                    color = if (highlighted) MaterialTheme.colorScheme.primary
                    else VnTheme.extended.cardStroke,
                ),
                shape = RoundedCornerShape(18.dp),
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = if (highlighted) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (highlighted) {
                Text(
                    text = "BEST VALUE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = price,
                style = MaterialTheme.typography.headlineSmall,
                color = if (highlighted) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = caption,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
