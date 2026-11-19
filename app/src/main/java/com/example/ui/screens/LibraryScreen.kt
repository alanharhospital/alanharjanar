package com.example.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.core.i18n.LocalAppStrings
import com.example.domain.model.LibraryItem
import com.example.domain.model.ResearchArticle
import com.example.domain.model.UiState
import com.example.domain.model.UserCollection
import com.example.ui.components.ResearchArticleCard
import com.example.ui.components.StatusPill
import com.example.ui.components.shareArticleText
import com.example.ui.theme.OpenAccessEmerald
import com.example.ui.theme.OpenAccessEmeraldBg
import java.io.File

@Composable
fun LibraryScreen(
    libraryState: UiState<List<LibraryItem>>,
    collectionsState: UiState<List<UserCollection>>,
    onOpenArticle: (ResearchArticle) -> Unit,
    onToggleBookmark: (ResearchArticle, LibraryItem?) -> Unit,
    onCreateCollection: (String, String) -> Unit,
    onOpenLocalPdf: (String) -> Unit,
    onDeleteLocalPdf: (LibraryItem) -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current

    var selectedTab by remember { mutableIntStateOf(0) }
    var filterMode by remember { mutableStateOf("ALL_SAVED") }
    var selectedCollectionId by remember { mutableStateOf<String?>(null) }
    var showCreateColDialog by remember { mutableStateOf(false) }
    var newColName by remember { mutableStateOf("") }
    var newColDesc by remember { mutableStateOf("") }

    val items = (libraryState as? UiState.Success)?.data.orEmpty()
    val collections = (collectionsState as? UiState.Success)?.data.orEmpty()

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text(if (strings.isArabic) "المحفوظات" else "Saved") },
                icon = { Icon(Icons.Filled.Bookmark, contentDescription = null) },
                modifier = Modifier.testTag("library_tab_saved")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text(if (strings.isArabic) "المجلدات" else "Collections") },
                icon = { Icon(Icons.Filled.Folder, contentDescription = null) },
                modifier = Modifier.testTag("library_tab_collections")
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text(if (strings.isArabic) "السجل" else "History") },
                icon = { Icon(Icons.Filled.History, contentDescription = null) },
                modifier = Modifier.testTag("library_tab_history")
            )
            Tab(
                selected = selectedTab == 3,
                onClick = { selectedTab = 3 },
                text = { Text(if (strings.isArabic) "PDF الجهاز" else "Device PDFs") },
                icon = { Icon(Icons.Filled.PictureAsPdf, contentDescription = null) },
                modifier = Modifier.testTag("library_tab_pdfs")
            )
        }

        if (libraryState is UiState.Loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            when (selectedTab) {
                0 -> {
                    val filterOptions = listOf(
                        "ALL_SAVED" to (if (strings.isArabic) "الكل" else "All Saved"),
                        "BOOKMARKED" to (if (strings.isArabic) "الإشارات" else "Bookmarks"),
                        "FAVORITES" to (if (strings.isArabic) "المفضلة" else "Favorites"),
                        "UNREAD" to (if (strings.isArabic) "غير مقروء" else "Unread"),
                        "READING" to (if (strings.isArabic) "قيد القراءة" else "Reading"),
                        "READ" to (if (strings.isArabic) "مقروء" else "Read")
                    )
                    val filtered = remember(items, filterMode) {
                        items.filter { item ->
                            when (filterMode) {
                                "BOOKMARKED" -> item.bookmarked
                                "FAVORITES" -> item.favorite
                                "UNREAD" -> item.saved && item.readingStatus == "UNREAD"
                                "READING" -> item.saved && item.readingStatus == "READING"
                                "READ" -> item.saved && item.readingStatus == "READ"
                                else -> item.saved || item.bookmarked || item.favorite
                            }
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(filterOptions) { (code, label) ->
                                    FilterChip(
                                        selected = filterMode == code,
                                        onClick = { filterMode = code },
                                        label = { Text(label) }
                                    )
                                }
                            }
                        }
                        if (filtered.isEmpty()) {
                            item {
                                EmptyLibraryNote(
                                    if (strings.isArabic) "مكتبتك فارغة حالياً. احفظ الأبحاث من الرئيسية أو البحث لتظهر هنا حتى بدون إنترنت."
                                    else "No saved articles in this filter yet. Bookmark or save publications from Home or Discover."
                                )
                            }
                        } else {
                            items(filtered, key = { "lib_${it.articleKey}" }) { item ->
                                val art = item.toResearchArticle()
                                ResearchArticleCard(
                                    article = art,
                                    isBookmarked = item.bookmarked,
                                    onClick = { onOpenArticle(art) },
                                    onToggleBookmark = { onToggleBookmark(art, item) },
                                    onShare = { shareArticleText(context, art) }
                                )
                            }
                        }
                    }
                }
                1 -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Button(
                                onClick = { showCreateColDialog = true },
                                modifier = Modifier.fillMaxWidth().testTag("create_collection_button")
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text(if (strings.isArabic) "إنشاء مجلد جديد" else "Create New Collection")
                            }
                        }
                        items(collections, key = { it.id.ifBlank { it.name } }) { col ->
                            val count = items.count { col.id in it.collectionIds }
                            val expanded = selectedCollectionId == col.id
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedCollectionId = if (expanded) null else col.id },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(col.name, style = MaterialTheme.typography.titleSmall)
                                            if (col.description.isNotBlank()) {
                                                Text(col.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                        StatusPill("$count", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
                                    }
                                    if (expanded) {
                                        val colArticles = items.filter { col.id in it.collectionIds }
                                        Spacer(Modifier.height(10.dp))
                                        if (colArticles.isEmpty()) {
                                            Text(
                                                text = if (strings.isArabic) "لا توجد أبحاث في هذا المجلد." else "No articles assigned to this folder yet.",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        } else {
                                            colArticles.forEach { libItem ->
                                                Text(
                                                    text = "• ${libItem.title}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable { onOpenArticle(libItem.toResearchArticle()) }
                                                        .padding(vertical = 6.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    val history = items.filter { it.lastOpenedMillis > 0L }.sortedByDescending { it.lastOpenedMillis }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (history.isEmpty()) {
                            item {
                                EmptyLibraryNote(
                                    if (strings.isArabic) "سجل القراءة فارغ." else "Your reading history will appear here automatically as you open publications."
                                )
                            }
                        } else {
                            items(history, key = { "hist_${it.articleKey}" }) { item ->
                                val art = item.toResearchArticle()
                                ResearchArticleCard(
                                    article = art,
                                    isBookmarked = item.bookmarked,
                                    onClick = { onOpenArticle(art) },
                                    onToggleBookmark = { onToggleBookmark(art, item) },
                                    onShare = { shareArticleText(context, art) }
                                )
                            }
                        }
                    }
                }
                else -> {
                    val downloaded = items.filter { it.localPdfPath.isNotBlank() && File(it.localPdfPath).exists() }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                                Text(
                                    text = if (strings.isArabic)
                                        "مكتبة PDF المحلية: الملفات هنا محفوظة فعلياً على جهازك للوصول دون اتصال، بخلاف الإشارات المرجعية عبر الإنترنت."
                                    else
                                        "On-Device PDF Library: Files listed here are legally downloaded and stored on your device for offline reading (distinct from online bookmarks).",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        }
                        if (downloaded.isEmpty()) {
                            item {
                                EmptyLibraryNote(
                                    if (strings.isArabic) "لم تقم بتنزيل أي ملفات PDF قانونية بعد."
                                    else "No downloaded PDFs on device yet. Open an Open Access article with a verified PDF link to download."
                                )
                            }
                        } else {
                            items(downloaded, key = { "pdf_${it.articleKey}" }) { item ->
                                val file = File(item.localPdfPath)
                                val sizeKb = (file.length() / 1024L).coerceAtLeast(1L)
                                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Filled.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                                        Spacer(Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(item.title, style = MaterialTheme.typography.titleSmall, maxLines = 2)
                                            Spacer(Modifier.height(4.dp))
                                            StatusPill("Stored on Device • ${sizeKb} KB", OpenAccessEmeraldBg, OpenAccessEmerald)
                                        }
                                        IconButton(onClick = { onOpenLocalPdf(item.localPdfPath) }, modifier = Modifier.minimumInteractiveComponentSize()) {
                                            Icon(Icons.Filled.PictureAsPdf, contentDescription = "Open PDF", tint = MaterialTheme.colorScheme.primary)
                                        }
                                        IconButton(onClick = { onDeleteLocalPdf(item) }, modifier = Modifier.minimumInteractiveComponentSize()) {
                                            Icon(Icons.Filled.Delete, contentDescription = "Delete PDF", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateColDialog) {
        AlertDialog(
            onDismissRequest = { showCreateColDialog = false },
            title = { Text(if (strings.isArabic) "مجلد بحثي جديد" else "New Research Collection") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newColName,
                        onValueChange = { newColName = it },
                        label = { Text(if (strings.isArabic) "اسم المجلد" else "Folder Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newColDesc,
                        onValueChange = { newColDesc = it },
                        label = { Text(if (strings.isArabic) "الوصف" else "Description") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    onCreateCollection(newColName, newColDesc)
                    newColName = ""
                    newColDesc = ""
                    showCreateColDialog = false
                }) {
                    Text(if (strings.isArabic) "إنشاء" else "Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateColDialog = false }) {
                    Text(if (strings.isArabic) "إلغاء" else "Cancel")
                }
            }
        )
    }
}

@Composable
private fun EmptyLibraryNote(message: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Text(message, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(18.dp))
    }
}
