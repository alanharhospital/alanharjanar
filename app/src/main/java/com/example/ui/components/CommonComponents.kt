package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.core.i18n.LocalAppStrings
import com.example.domain.model.EditorialPost
import com.example.domain.model.HospitalConfig
import com.example.domain.model.ResearchArticle
import com.example.ui.theme.ClinicalMonoFamily
import com.example.ui.theme.HospitalIndigo
import com.example.ui.theme.HospitalIndigoBg
import com.example.ui.theme.OpenAccessEmerald
import com.example.ui.theme.OpenAccessEmeraldBg
import com.example.ui.theme.PreprintAmber
import com.example.ui.theme.PreprintAmberBg
import com.example.ui.theme.UrgentCrimson
import com.example.ui.theme.UrgentCrimsonBg

fun openExternalUrl(context: Context, url: String) {
    if (url.isBlank()) return
    try {
        val uri = Uri.parse(url)
        CustomTabsIntent.Builder().setShowTitle(true).build().launchUrl(context, uri)
    } catch (_: Exception) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
        }
    }
}

fun shareArticleText(context: Context, article: ResearchArticle) {
    val doiLine = if (article.doi.isNotBlank()) "\nDOI: https://doi.org/${article.doi}" else ""
    val linkLine = if (article.publisherUrl.isNotBlank()) "\nSource: ${article.publisherUrl}" else ""
    val text = "${article.title}\n${article.authors}\n${article.journal} (${article.publicationDate})$doiLine$linkLine\n\nShared via Al-Anhar Scientific Journal"
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, article.title)
        putExtra(Intent.EXTRA_TEXT, text)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(Intent.createChooser(intent, "Share Scientific Publication"))
}

@Composable
fun HospitalLogoBadge(
    config: HospitalConfig,
    size: Dp = 44.dp,
    modifier: Modifier = Modifier
) {
    val decodedBitmap = remember(config.logoDataUri) {
        if (config.logoDataUri.startsWith("data:image")) {
            try {
                val b64 = config.logoDataUri.substringAfter("base64,", "")
                val bytes = Base64.decode(b64, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            } catch (_: Exception) {
                null
            }
        } else null
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (decodedBitmap != null) {
            Image(
                bitmap = decodedBitmap,
                contentDescription = config.hospitalNameEn,
                modifier = Modifier.fillMaxSize().padding(3.dp),
                contentScale = ContentScale.Fit
            )
        } else {
            Icon(
                imageVector = Icons.Filled.LocalHospital,
                contentDescription = config.hospitalNameEn,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(size * 0.58f)
            )
        }
    }
}

@Composable
fun StatusPill(
    text: String,
    containerColor: Color,
    contentColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(50),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(12.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ResearchArticleCard(
    article: ResearchArticle,
    isBookmarked: Boolean,
    onClick: () -> Unit,
    onToggleBookmark: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("article_card_${article.articleKey.take(24)}")
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (article.retracted || article.preprint) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    if (article.retracted) {
                        StatusPill(strings.retractedWarning, UrgentCrimsonBg, UrgentCrimson, Icons.Filled.Warning)
                    }
                    if (article.preprint) {
                        StatusPill(strings.preprintWarning, PreprintAmberBg, PreprintAmber, Icons.Filled.Warning)
                    }
                }
            }

            Text(
                text = article.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = article.authors,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = article.journal,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Text(
                    text = "• ${article.publicationDate}",
                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = ClinicalMonoFamily),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (article.abstractText.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = article.abstractText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(10.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                article.contributingSources.take(3).forEach { src ->
                    StatusPill(
                        text = src,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                if (article.openAccess) {
                    StatusPill(
                        text = strings.openAccess,
                        containerColor = OpenAccessEmeraldBg,
                        contentColor = OpenAccessEmerald,
                        icon = Icons.Filled.CheckCircle
                    )
                }
                if (article.pdfUrl.isNotBlank()) {
                    StatusPill(
                        text = strings.legalPdfAvailable,
                        containerColor = HospitalIndigoBg,
                        contentColor = HospitalIndigo,
                        icon = Icons.Filled.PictureAsPdf
                    )
                }
                if (article.citationCount > 0) {
                    StatusPill(
                        text = "${article.citationCount} citations",
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val idBadge = when {
                    article.doi.isNotBlank() -> "DOI: ${article.doi}"
                    article.pmid.isNotBlank() -> "PMID: ${article.pmid}"
                    else -> article.publicationType
                }
                Text(
                    text = idBadge,
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = ClinicalMonoFamily),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Row {
                    IconButton(
                        onClick = onToggleBookmark,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("bookmark_button_${article.articleKey.take(20)}")
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                            contentDescription = if (isBookmarked) "Remove bookmark" else "Bookmark article",
                            tint = if (isBookmarked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onShare,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("share_button_${article.articleKey.take(20)}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = "Share article",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EditorialPostCard(
    post: EditorialPost,
    isArabic: Boolean,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val badgeBg = if (post.hospitalProtocol) HospitalIndigoBg else MaterialTheme.colorScheme.secondaryContainer
    val badgeFg = if (post.hospitalProtocol) HospitalIndigo else MaterialTheme.colorScheme.onSecondaryContainer
    val badgeLabel = when {
        post.hospitalProtocol -> if (isArabic) "بروتوكول مستشفى الأنهار المحلي" else "Al-Anhar Local Hospital Protocol"
        post.contentType == "CLINICAL_GUIDELINE_UPDATE" -> if (isArabic) "تحديث إرشادات سريرية" else "Clinical Guideline Update"
        post.contentType == "RESEARCH_HIGHLIGHT" -> if (isArabic) "إضاءة بحثية" else "Research Highlight"
        else -> if (isArabic) "إعلان علمي تحريري" else "Hospital Editorial"
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusPill(badgeLabel, badgeBg, badgeFg, Icons.Filled.Verified)
                if (post.specialty.isNotBlank()) {
                    Text(
                        text = post.specialty,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (isArabic && post.titleAr.isNotBlank()) post.titleAr else post.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = post.summary.ifBlank { post.body },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${post.authorName} • ${post.publicationDate.ifBlank { post.verifiedDate }}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (post.externalUrl.isNotBlank()) {
                    IconButton(
                        onClick = { onOpenUrl(post.externalUrl) },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = "Open editorial link",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
