package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.core.i18n.LocalAppStrings
import com.example.domain.model.LibraryItem
import com.example.domain.model.ResearchArticle
import com.example.domain.model.SearchFilters
import com.example.domain.model.SortOption
import com.example.domain.model.StudyTypeFilter
import com.example.domain.model.TimeFilter
import com.example.domain.model.UiState
import com.example.ui.components.ResearchArticleCard
import com.example.ui.components.openExternalUrl
import com.example.ui.components.shareArticleText

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DiscoverScreen(
    filters: SearchFilters,
    resultsState: UiState<List<ResearchArticle>>,
    currentPage: Int,
    libraryItems: List<LibraryItem>,
    scholarImportMessage: String?,
    onUpdateFilters: ((SearchFilters) -> SearchFilters) -> Unit,
    onSearch: () -> Unit,
    onNextPage: () -> Unit,
    onPreviousPage: () -> Unit,
    onOpenArticle: (ResearchArticle) -> Unit,
    onToggleBookmark: (ResearchArticle, LibraryItem?) -> Unit,
    onBuildGoogleScholarUrl: (String) -> String,
    onImportScholarAlert: (String) -> Unit,
    onClearScholarMsg: () -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val bookmarkedMap = remember(libraryItems) { libraryItems.associateBy { it.articleKey } }

    var showAdvancedFilters by remember { mutableStateOf(false) }
    var showScholarImportDialog by remember { mutableStateOf(false) }
    var scholarEmailDraft by remember { mutableStateOf("") }

    val allDatabases = listOf("PubMed", "Europe PMC", "OpenAlex", "Semantic Scholar", "Crossref", "DOAJ")

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = filters.query,
                    onValueChange = { q -> onUpdateFilters { it.copy(query = q) } },
                    label = { Text(strings.searchPlaceholder) },
                    singleLine = true,
                    trailingIcon = {
                        IconButton(
                            onClick = onSearch,
                            modifier = Modifier.minimumInteractiveComponentSize().testTag("discover_search_submit")
                        ) {
                            Icon(Icons.Filled.Search, contentDescription = "Search")
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("discover_search_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onSearch,
                        modifier = Modifier.weight(1f).testTag("discover_run_search_button")
                    ) {
                        Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (strings.isArabic) "بحث علمي" else "Search")
                    }
                    OutlinedButton(
                        onClick = { showAdvancedFilters = !showAdvancedFilters },
                        modifier = Modifier.testTag("discover_toggle_filters_button")
                    ) {
                        Icon(Icons.Filled.FilterList, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (strings.isArabic) "الفلاتر" else "Filters")
                    }
                }

                // External Google Scholar Integration + Email Alert Import
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { openExternalUrl(context, onBuildGoogleScholarUrl(filters.query)) },
                        modifier = Modifier.weight(1f).testTag("google_scholar_external_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Google Scholar", maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = { showScholarImportDialog = true },
                        modifier = Modifier.weight(1f).testTag("google_scholar_import_button")
                    ) {
                        Icon(Icons.Filled.Email, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (strings.isArabic) "استيراد تنبيه" else "Import Alert", maxLines = 1)
                    }
                }

                if (scholarImportMessage != null) {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(scholarImportMessage, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                            TextButton(onClick = onClearScholarMsg) { Text("OK") }
                        }
                    }
                }
            }
        }

        // Quick Toggles (OA, Free Full Text, Free PDF)
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = filters.openAccessOnly,
                        onClick = {
                            onUpdateFilters { it.copy(openAccessOnly = !it.openAccessOnly) }
                            onSearch()
                        },
                        label = { Text(strings.openAccess) }
                    )
                }
                item {
                    FilterChip(
                        selected = filters.freeFullTextOnly,
                        onClick = {
                            onUpdateFilters { it.copy(freeFullTextOnly = !it.freeFullTextOnly) }
                            onSearch()
                        },
                        label = { Text(if (strings.isArabic) "نص كامل مجاني" else "Free Full Text") }
                    )
                }
                item {
                    FilterChip(
                        selected = filters.freePdfOnly,
                        onClick = {
                            onUpdateFilters { it.copy(freePdfOnly = !it.freePdfOnly) }
                            onSearch()
                        },
                        label = { Text(if (strings.isArabic) "PDF متاح" else "Free PDF") }
                    )
                }
                items(SortOption.entries) { sort ->
                    FilterChip(
                        selected = filters.sortOption == sort,
                        onClick = {
                            onUpdateFilters { it.copy(sortOption = sort) }
                            onSearch()
                        },
                        label = { Text(if (strings.isArabic) sort.labelAr else sort.labelEn) }
                    )
                }
            }
        }

        if (showAdvancedFilters) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = if (strings.isArabic) "فلاتر البحث المتقدم" else "Advanced Discovery Filters",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = filters.authorFilter,
                                onValueChange = { v -> onUpdateFilters { it.copy(authorFilter = v) } },
                                label = { Text(if (strings.isArabic) "المؤلف" else "Author") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = filters.journalFilter,
                                onValueChange = { v -> onUpdateFilters { it.copy(journalFilter = v) } },
                                label = { Text(if (strings.isArabic) "المجلة" else "Journal") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        OutlinedTextField(
                            value = filters.yearFilter,
                            onValueChange = { v -> onUpdateFilters { it.copy(yearFilter = v) } },
                            label = { Text(if (strings.isArabic) "سنة النشر (مثلاً 2025)" else "Publication Year (e.g. 2025)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(if (strings.isArabic) "النطاق الزمني" else "Publication Window", style = MaterialTheme.typography.labelLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            TimeFilter.entries.forEach { tf ->
                                FilterChip(
                                    selected = filters.timeFilter == tf,
                                    onClick = { onUpdateFilters { it.copy(timeFilter = tf) } },
                                    label = { Text(if (strings.isArabic) tf.labelAr else tf.labelEn) }
                                )
                            }
                        }

                        Text(if (strings.isArabic) "نوع الدراسة" else "Study Type", style = MaterialTheme.typography.labelLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            StudyTypeFilter.entries.forEach { st ->
                                FilterChip(
                                    selected = filters.studyTypeFilter == st,
                                    onClick = { onUpdateFilters { it.copy(studyTypeFilter = st) } },
                                    label = { Text(if (strings.isArabic) st.labelAr else st.labelEn) }
                                )
                            }
                        }

                        Text(if (strings.isArabic) "قواعد البيانات الأكاديمية" else "Connected Databases", style = MaterialTheme.typography.labelLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            allDatabases.forEach { dbName ->
                                val active = dbName in filters.enabledDatabases
                                FilterChip(
                                    selected = active,
                                    onClick = {
                                        onUpdateFilters {
                                            val next = it.enabledDatabases.toMutableSet()
                                            if (active && next.size > 1) next.remove(dbName) else next.add(dbName)
                                            it.copy(enabledDatabases = next)
                                        }
                                    },
                                    label = { Text(dbName) }
                                )
                            }
                        }

                        Button(
                            onClick = {
                                showAdvancedFilters = false
                                onSearch()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (strings.isArabic) "تطبيق الفلاتر والبحث" else "Apply Filters & Search")
                        }
                    }
                }
            }
        }

        // Results
        when (resultsState) {
            is UiState.Loading -> {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = if (strings.isArabic) "جاري البحث في قواعد البيانات الطبية..." else "Querying academic databases & deduplicating...",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
            is UiState.Error -> {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Text(
                            text = resultsState.message,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
            is UiState.Success -> {
                val list = resultsState.data
                item {
                    Text(
                        text = if (strings.isArabic) "تم العثور على ${list.size} بحث فريد (صفحة ${currentPage + 1})"
                        else "${list.size} deduplicated publications • Page ${currentPage + 1}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (list.isEmpty()) {
                    item {
                        Card {
                            Text(
                                text = if (strings.isArabic) "لم يتم العثور على نتائج مطابقة. جرب توسيع الفلاتر أو البحث بالإنجليزية."
                                else "No matching publications found. Try broadening your date range or removing strict PDF/OA filters.",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                } else {
                    items(list, key = { "disc_${it.articleKey}" }) { article ->
                        val lib = bookmarkedMap[article.articleKey]
                        ResearchArticleCard(
                            article = article,
                            isBookmarked = lib?.bookmarked == true,
                            onClick = { onOpenArticle(article) },
                            onToggleBookmark = { onToggleBookmark(article, lib) },
                            onShare = { shareArticleText(context, article) }
                        )
                    }
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            OutlinedButton(
                                onClick = onPreviousPage,
                                enabled = currentPage > 0,
                                modifier = Modifier.testTag("discover_prev_page")
                            ) {
                                Text(if (strings.isArabic) "السابق" else "Previous Page")
                            }
                            Button(
                                onClick = onNextPage,
                                modifier = Modifier.testTag("discover_next_page")
                            ) {
                                Text(if (strings.isArabic) "الصفحة التالية" else "Next Page")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showScholarImportDialog) {
        AlertDialog(
            onDismissRequest = { showScholarImportDialog = false },
            title = { Text(if (strings.isArabic) "استيراد تنبيه بريد Google Scholar" else "Import Google Scholar Alert Email") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (strings.isArabic) "الصق نص رسالة تنبيه Google Scholar المصرح بها لاستيراد الأبحاث بشكل قانوني."
                        else "Paste the text of your user-authorized Google Scholar Alert email to parse citations into your research feed without scraping.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = scholarEmailDraft,
                        onValueChange = { scholarEmailDraft = it },
                        label = { Text(if (strings.isArabic) "محتوى التنبيه" else "Alert Email Content") },
                        minLines = 5,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onImportScholarAlert(scholarEmailDraft)
                        showScholarImportDialog = false
                    }
                ) {
                    Text(if (strings.isArabic) "استيراد" else "Import")
                }
            },
            dismissButton = {
                TextButton(onClick = { showScholarImportDialog = false }) {
                    Text(if (strings.isArabic) "إلغاء" else "Cancel")
                }
            }
        )
    }
}
