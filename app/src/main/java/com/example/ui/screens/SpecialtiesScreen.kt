package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.core.i18n.LocalAppStrings
import com.example.domain.model.ScientificCategory
import com.example.domain.model.UiState
import com.example.domain.taxonomy.MedicalTaxonomy

@Composable
fun SpecialtiesScreen(
    categoriesState: UiState<List<ScientificCategory>>,
    subscribedTopics: List<String>,
    onSelectCategory: (ScientificCategory) -> Unit,
    onToggleSubscription: (String) -> Unit
) {
    val strings = LocalAppStrings.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var filterText by remember { mutableStateOf("") }

    val allCats = (categoriesState as? UiState.Success)?.data ?: MedicalTaxonomy.allDefaultCategories
    val activeTrack = if (selectedTab == 0) "RESPIRATORY_CARE" else "CRITICAL_CARE"
    val filtered = remember(allCats, activeTrack, filterText) {
        allCats.filter { cat ->
            cat.active && cat.parentTrack == activeTrack &&
                (filterText.isBlank() ||
                    cat.nameEn.contains(filterText, ignoreCase = true) ||
                    cat.nameAr.contains(filterText, ignoreCase = true))
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("${strings.respiratoryCare} (34)") },
                icon = { Icon(Icons.Filled.Air, contentDescription = null) },
                modifier = Modifier.testTag("tab_respiratory_care")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("${strings.criticalCare} (24)") },
                icon = { Icon(Icons.Filled.MonitorHeart, contentDescription = null) },
                modifier = Modifier.testTag("tab_critical_care")
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                OutlinedTextField(
                    value = filterText,
                    onValueChange = { filterText = it },
                    label = {
                        Text(if (strings.isArabic) "تصفية التخصصات العلمية..." else "Filter scientific disciplines...")
                    },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("specialties_filter_input")
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (strings.isArabic)
                        "اضغط على أي تخصص لاستكشاف أحدث الأبحاث، أو فعّل الجرس للاشتراك في التحديثات التلقائية."
                    else
                        "Tap any discipline to query indexed publications, or tap the bell to subscribe to automatic research updates.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(filtered, key = { it.id.ifBlank { it.nameEn } }) { cat ->
                val isSubscribed = cat.nameEn in subscribedTopics
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("category_card_${cat.id}")
                        .clickable { onSelectCategory(cat) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (strings.isArabic && cat.nameAr.isNotBlank()) cat.nameAr else cat.nameEn,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = if (strings.isArabic) cat.nameEn else cat.nameAr,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (cat.description.isNotBlank()) {
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = cat.description,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2
                                )
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        IconButton(
                            onClick = { onToggleSubscription(cat.nameEn) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("subscribe_category_${cat.id}")
                        ) {
                            Icon(
                                imageVector = if (isSubscribed) Icons.Filled.NotificationsActive else Icons.Filled.NotificationsNone,
                                contentDescription = if (isSubscribed) "Unsubscribe from topic" else "Subscribe to topic",
                                tint = if (isSubscribed) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
