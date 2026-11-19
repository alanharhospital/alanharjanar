package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.i18n.LocalAppStrings
import com.example.domain.model.EditorialPost
import com.example.domain.model.HospitalConfig
import com.example.domain.model.LibraryItem
import com.example.domain.model.MonitoredArticle
import com.example.domain.model.ResearchArticle
import com.example.domain.model.UiState
import com.example.ui.components.EditorialPostCard
import com.example.ui.components.HospitalLogoBadge
import com.example.ui.components.ResearchArticleCard
import com.example.ui.components.openExternalUrl
import com.example.ui.components.shareArticleText
import com.example.ui.theme.MedicalNavy
import com.example.ui.theme.PureWhite
import com.example.ui.viewmodel.HomeJournalFeed

@Composable
fun HomeScreen(
    hospitalConfig: HospitalConfig,
    homeFeedState: UiState<HomeJournalFeed>,
    editorialState: UiState<List<EditorialPost>>,
    monitoredState: UiState<List<MonitoredArticle>>,
    libraryItems: List<LibraryItem>,
    onRefresh: () -> Unit,
    onOpenDiscover: () -> Unit,
    onOpenClinicalIntel: () -> Unit,
    onOpenArticle: (ResearchArticle) -> Unit,
    onToggleBookmark: (ResearchArticle, LibraryItem?) -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val bookmarkedMap = remember(libraryItems) { libraryItems.associateBy { it.articleKey } }

    var selectedSectionIndex by remember { mutableIntStateOf(0) }
    val sectionLabels = if (strings.isArabic) {
        listOf("أحدث المنشورات", "الأكثر صلة", "الرعاية التنفسية", "العناية المركزة", "وصول مفتوح", "إرشادات سريرية", "مراجعات منهجية")
    } else {
        listOf("Latest", "Most Relevant", "Respiratory", "Critical Care", "Open Access", "Guidelines", "Systematic Reviews")
    }

    val featuredFromAdmin = (monitoredState as? UiState.Success)?.data
        ?.filter { it.featured }
        ?.map { it.toResearchArticle() }
        .orEmpty()

    val editorialPosts = (editorialState as? UiState.Success)?.data.orEmpty()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Journal Masthead Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_hero_journal),
                    contentDescription = "Al-Anhar Scientific Journal Banner",
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(MedicalNavy.copy(alpha = 0.90f), MedicalNavy.copy(alpha = 0.72f))
                            )
                        )
                        .padding(18.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            HospitalLogoBadge(config = hospitalConfig, size = 48.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (strings.isArabic) hospitalConfig.hospitalNameAr else hospitalConfig.hospitalNameEn,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = PureWhite.copy(alpha = 0.9f)
                                )
                                Text(
                                    text = strings.journalTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = PureWhite,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            IconButton(
                                onClick = onRefresh,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("home_refresh_button")
                            ) {
                                Icon(Icons.Filled.Refresh, contentDescription = strings.refresh, tint = PureWhite)
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = if (strings.isArabic) strings.tagline else hospitalConfig.tagline,
                            style = MaterialTheme.typography.bodySmall,
                            color = PureWhite.copy(alpha = 0.92f)
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = onOpenDiscover,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PureWhite,
                                    contentColor = MedicalNavy
                                ),
                                modifier = Modifier.testTag("home_discover_cta")
                            ) {
                                Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(strings.navDiscover)
                            }
                            OutlinedButton(
                                onClick = onOpenClinicalIntel,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = PureWhite),
                                modifier = Modifier.testTag("home_clinical_cta")
                            ) {
                                Icon(Icons.Filled.MedicalServices, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(strings.navClinicalAi)
                            }
                        }
                    }
                }
            }
        }

        // 2. AL-ANHAR CLINICAL INTELLIGENCE Quick Launch Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_clinical_intelligence_card")
                    .clickable(onClick = onOpenClinicalIntel),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondary,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondary,
                            modifier = Modifier.padding(10.dp).size(24.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.clinicalTitle,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = strings.clinicalSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Open Clinical Intelligence",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        // 3. Featured Research / Editor's Selection (if admin featured any articles)
        if (featuredFromAdmin.isNotEmpty()) {
            item {
                Text(
                    text = strings.featuredResearch,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            items(featuredFromAdmin, key = { "feat_${it.articleKey}" }) { article ->
                val lib = bookmarkedMap[article.articleKey]
                ResearchArticleCard(
                    article = article,
                    isBookmarked = lib?.bookmarked == true,
                    onClick = { onOpenArticle(article) },
                    onToggleBookmark = { onToggleBookmark(article, lib) },
                    onShare = { shareArticleText(context, article) }
                )
            }
        }

        // 4. Hospital Editorial & Local Announcements Section (strictly distinguished from external research)
        if (editorialPosts.isNotEmpty()) {
            item {
                Column {
                    Text(
                        text = strings.editorialSection,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = strings.editorialDisclaimer,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            items(editorialPosts.take(5), key = { "ed_${it.id}" }) { post ->
                EditorialPostCard(
                    post = post,
                    isArabic = strings.isArabic,
                    onOpenUrl = { openExternalUrl(context, it) }
                )
            }
        }

        // 5. Journal Section Selector Chips
        item {
            Column {
                Text(
                    text = strings.latestPublications,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = strings.editorialDisclaimer,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(sectionLabels.size) { index ->
                        FilterChip(
                            selected = selectedSectionIndex == index,
                            onClick = { selectedSectionIndex = index },
                            label = { Text(sectionLabels[index]) },
                            modifier = Modifier.testTag("home_section_chip_$index")
                        )
                    }
                }
            }
        }

        // 6. Live Feed Results
        when (homeFeedState) {
            is UiState.Loading -> {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
            is UiState.Error -> {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = homeFeedState.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = onRefresh) {
                                Text(strings.refresh)
                            }
                        }
                    }
                }
            }
            is UiState.Success -> {
                val feed = homeFeedState.data
                val articles = when (selectedSectionIndex) {
                    0 -> feed.latestPublications
                    1 -> feed.mostRelevant
                    2 -> feed.newRespiratory
                    3 -> feed.newCriticalCare
                    4 -> feed.openAccessList
                    5 -> feed.clinicalGuidelinesPapers
                    else -> feed.systematicReviews
                }
                if (articles.isEmpty()) {
                    item {
                        Card {
                            Text(
                                text = if (strings.isArabic) "لا توجد نتائج في هذا القسم حالياً. اضغط تحديث أو استخدم محرك البحث."
                                else "No publications matched this section window yet. Tap Refresh or search across all 7 databases in Discover.",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                } else {
                    items(articles, key = { "feed_${selectedSectionIndex}_${it.articleKey}" }) { article ->
                        val lib = bookmarkedMap[article.articleKey]
                        ResearchArticleCard(
                            article = article,
                            isBookmarked = lib?.bookmarked == true,
                            onClick = { onOpenArticle(article) },
                            onToggleBookmark = { onToggleBookmark(article, lib) },
                            onShare = { shareArticleText(context, article) }
                        )
                    }
                }
            }
        }
    }
}
