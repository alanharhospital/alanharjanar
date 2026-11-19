package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.core.firebase.authStateFlow
import com.example.core.firebase.configureAppCheck
import com.example.core.firebase.signOut
import com.example.core.i18n.ArabicStrings
import com.example.core.i18n.EnglishStrings
import com.example.core.i18n.LocalAppStrings
import com.example.data.local.JournalDatabase
import com.example.data.local.LocalAppPreferences
import com.example.data.local.PreferencesManager
import com.example.data.remote.AcademicNetworkClients
import com.example.data.remote.MultiDatabaseSearchService
import com.example.data.remote.PdfDownloadService
import com.example.data.repository.ClinicalReportPdfExporter
import com.example.data.repository.FirestoreJournalRepository
import com.example.data.repository.GeminiScientificAssistant
import com.example.domain.model.ResearchArticle
import com.example.domain.model.UiState
import com.example.ui.components.HospitalLogoBadge
import com.example.ui.screens.ArticleDetailScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ClinicalIntelligenceScreen
import com.example.ui.screens.DiscoverScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.ProfileAdminScreen
import com.example.ui.screens.SpecialtiesScreen
import com.example.ui.theme.AlAnharJournalTheme
import com.example.ui.viewmodel.ClinicalIntelligenceViewModel
import com.example.ui.viewmodel.JournalViewModel
import com.example.ui.viewmodel.LibraryAdminViewModel
import com.example.worker.ResearchMonitorWorker
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

const val ROUTE_HOME = "home"
const val ROUTE_DISCOVER = "discover"
const val ROUTE_CLINICAL_INTEL = "clinical_intelligence"
const val ROUTE_SPECIALTIES = "specialties"
const val ROUTE_LIBRARY = "library"
const val ROUTE_ARTICLE_DETAIL = "article_detail"
const val ROUTE_NOTIFICATIONS = "notifications"
const val ROUTE_PROFILE = "profile_admin"

private data class MainTabItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val testTag: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Must configure Firebase App Check before Compose UI or Firebase AI calls run
        configureAppCheck(this, intent)
        enableEdgeToEdge()

        val prefsManager = PreferencesManager(applicationContext)

        setContent {
            val currentUser by remember { Firebase.auth.authStateFlow() }
                .collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)
            val localPrefs by prefsManager.preferencesFlow
                .collectAsStateWithLifecycle(initialValue = LocalAppPreferences())
            val scope = rememberCoroutineScope()

            val isArabic = localPrefs.language == "ar"
            val strings = if (isArabic) ArabicStrings else EnglishStrings
            val layoutDirection = if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr

            CompositionLocalProvider(
                LocalAppStrings provides strings,
                LocalLayoutDirection provides layoutDirection
            ) {
                AlAnharJournalTheme(
                    themeMode = localPrefs.themeMode,
                    isArabic = isArabic
                ) {
                    val user = currentUser
                    if (user == null) {
                        AuthScreen(
                            isArabic = isArabic,
                            onToggleLanguage = {
                                scope.launch {
                                    prefsManager.setLanguage(if (isArabic) "en" else "ar")
                                }
                            },
                            onAuthSuccess = {}
                        )
                    } else {
                        key(user.uid) {
                            AuthenticatedJournalShell(
                                userId = user.uid,
                                prefsManager = prefsManager,
                                localPrefs = localPrefs
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedJournalShell(
    userId: String,
    prefsManager: PreferencesManager,
    localPrefs: LocalAppPreferences
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val strings = LocalAppStrings.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val db = remember(appContext) { JournalDatabase.getInstance(appContext) }
    val firestoreRepo = remember(appContext) { FirestoreJournalRepository(appContext) }
    val clients = remember { AcademicNetworkClients() }
    val searchService = remember(db) {
        MultiDatabaseSearchService(clients, db.cachedArticleDao(), db.syncLogDao())
    }
    val pdfService = remember(appContext) { PdfDownloadService(appContext) }
    val geminiAssistant = remember { GeminiScientificAssistant() }
    val pdfExporter = remember(appContext) { ClinicalReportPdfExporter(appContext) }

    val factory = remember(userId) {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return when (modelClass) {
                    JournalViewModel::class.java -> JournalViewModel(
                        firestoreRepo = firestoreRepo,
                        searchService = searchService,
                        geminiAssistant = geminiAssistant,
                        pdfService = pdfService,
                        cachedArticleDao = db.cachedArticleDao(),
                        preferencesManager = prefsManager,
                        currentUserId = userId
                    ) as T
                    LibraryAdminViewModel::class.java -> LibraryAdminViewModel(
                        firestoreRepo = firestoreRepo,
                        preferencesManager = prefsManager,
                        pdfService = pdfService,
                        localDb = db,
                        currentUserId = userId
                    ) as T
                    ClinicalIntelligenceViewModel::class.java -> ClinicalIntelligenceViewModel(
                        firestoreRepo = firestoreRepo,
                        searchService = searchService,
                        geminiAssistant = geminiAssistant,
                        pdfExporter = pdfExporter,
                        currentUserId = userId
                    ) as T
                    else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
        }
    }

    val journalVm: JournalViewModel = viewModel(factory = factory)
    val libraryAdminVm: LibraryAdminViewModel = viewModel(factory = factory)
    val clinicalVm: ClinicalIntelligenceViewModel = viewModel(factory = factory)

    LaunchedEffect(userId, localPrefs.syncFrequencyHours) {
        ResearchMonitorWorker.schedulePeriodicSync(appContext, localPrefs.syncFrequencyHours.toLong())
    }

    // Observed states
    val hospitalConfig by libraryAdminVm.hospitalConfig.collectAsStateWithLifecycle()
    val libraryState by libraryAdminVm.libraryItemsState.collectAsStateWithLifecycle()
    val collectionsState by libraryAdminVm.collectionsState.collectAsStateWithLifecycle()
    val notificationsState by libraryAdminVm.notificationsState.collectAsStateWithLifecycle()
    val userProfileState by libraryAdminVm.userProfileState.collectAsStateWithLifecycle()
    val isAdmin by libraryAdminVm.isAdmin.collectAsStateWithLifecycle()
    val adminUsers by libraryAdminVm.adminUsers.collectAsStateWithLifecycle()
    val syncLogs by libraryAdminVm.syncLogs.collectAsStateWithLifecycle()
    val statusBanner by libraryAdminVm.statusBanner.collectAsStateWithLifecycle()

    val homeFeedState by journalVm.homeFeedState.collectAsStateWithLifecycle()
    val editorialState by journalVm.editorialPostsState.collectAsStateWithLifecycle()
    val monitoredState by journalVm.monitoredArticlesState.collectAsStateWithLifecycle()
    val categoriesState by journalVm.categoriesState.collectAsStateWithLifecycle()
    val searchFilters by journalVm.searchFilters.collectAsStateWithLifecycle()
    val searchResultsState by journalVm.searchResultsState.collectAsStateWithLifecycle()
    val currentPage by journalVm.currentPage.collectAsStateWithLifecycle()
    val selectedArticle by journalVm.selectedArticle.collectAsStateWithLifecycle()
    val paperAiState by journalVm.paperAiState.collectAsStateWithLifecycle()
    val pdfDownloadStatus by journalVm.pdfDownloadStatus.collectAsStateWithLifecycle()
    val importedScholarMessage by journalVm.importedScholarMessage.collectAsStateWithLifecycle()

    val caseInput by clinicalVm.caseInput.collectAsStateWithLifecycle()
    val safetyScreen by clinicalVm.safetyScreen.collectAsStateWithLifecycle()
    val caseAnalysisState by clinicalVm.caseAnalysisState.collectAsStateWithLifecycle()
    val caseSupportingStudies by clinicalVm.caseSupportingStudies.collectAsStateWithLifecycle()
    val diagnosisReportState by clinicalVm.diagnosisReportState.collectAsStateWithLifecycle()
    val diagnosisStudies by clinicalVm.diagnosisStudies.collectAsStateWithLifecycle()
    val evidenceComparisonState by clinicalVm.evidenceComparisonState.collectAsStateWithLifecycle()
    val latestClinicalStudiesState by clinicalVm.latestClinicalStudiesState.collectAsStateWithLifecycle()
    val savedCasesState by clinicalVm.savedCasesState.collectAsStateWithLifecycle()
    val savedTopicsState by clinicalVm.savedTopicsState.collectAsStateWithLifecycle()
    val guidelines by clinicalVm.guidelinesState.collectAsStateWithLifecycle()
    val reportExportMessage by clinicalVm.reportExportMessage.collectAsStateWithLifecycle()

    val libraryItems = (libraryState as? UiState.Success)?.data.orEmpty()
    val collections = (collectionsState as? UiState.Success)?.data.orEmpty()
    val userProfile = (userProfileState as? UiState.Success)?.data
    val unreadNotifCount = (notificationsState as? UiState.Success)?.data?.count { !it.read } ?: 0

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(statusBanner) {
        val msg = statusBanner
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            libraryAdminVm.clearStatusBanner()
        }
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route

    val tabs = remember(strings) {
        listOf(
            MainTabItem(ROUTE_HOME, strings.navHome, Icons.Filled.Home, "nav_home"),
            MainTabItem(ROUTE_DISCOVER, strings.navDiscover, Icons.Filled.Search, "nav_discover"),
            MainTabItem(ROUTE_CLINICAL_INTEL, strings.navClinicalAi, Icons.Filled.MedicalServices, "nav_clinical"),
            MainTabItem(ROUTE_SPECIALTIES, strings.navSpecialties, Icons.Filled.Category, "nav_specialties"),
            MainTabItem(ROUTE_LIBRARY, strings.navLibrary, Icons.Filled.Bookmark, "nav_library")
        )
    }

    val isTopLevelRoute = tabs.any { it.route == currentRoute } || currentRoute == null

    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun openArticle(article: ResearchArticle) {
        journalVm.openArticleDetail(article)
        navController.navigate(ROUTE_ARTICLE_DETAIL) {
            launchSingleTop = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                if (isTopLevelRoute) {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                HospitalLogoBadge(config = hospitalConfig, size = 34.dp)
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (strings.isArabic) "مجلة الأنهار العلمية" else "AL-ANHAR JOURNAL",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (strings.isArabic) hospitalConfig.hospitalNameAr else hospitalConfig.hospitalNameEn,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = { navController.navigate(ROUTE_NOTIFICATIONS) { launchSingleTop = true } },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("topbar_notifications_btn")
                            ) {
                                BadgedBox(
                                    badge = {
                                        if (unreadNotifCount > 0) {
                                            Badge { Text(unreadNotifCount.toString()) }
                                        }
                                    }
                                ) {
                                    Icon(Icons.Filled.Notifications, contentDescription = strings.navNotifications)
                                }
                            }
                            IconButton(
                                onClick = { navController.navigate(ROUTE_PROFILE) { launchSingleTop = true } },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("topbar_profile_btn")
                            ) {
                                Icon(Icons.Filled.AccountCircle, contentDescription = strings.navProfile)
                            }
                        }
                    )
                }
            },
            bottomBar = {
                if (isTopLevelRoute && !useRail) {
                    NavigationBar {
                        tabs.forEach { item ->
                            val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTab(item.route) },
                                icon = { Icon(item.icon, contentDescription = item.label) },
                                label = { Text(item.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isTopLevelRoute && useRail) {
                    NavigationRail(modifier = Modifier.fillMaxHeight()) {
                        tabs.forEach { item ->
                            val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(item.route) },
                                icon = { Icon(item.icon, contentDescription = item.label) },
                                label = { Text(item.label, maxLines = 1) },
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = ROUTE_HOME,
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 900.dp)
                    ) {
                        composable(ROUTE_HOME) {
                            HomeScreen(
                                hospitalConfig = hospitalConfig,
                                homeFeedState = homeFeedState,
                                editorialState = editorialState,
                                monitoredState = monitoredState,
                                libraryItems = libraryItems,
                                onRefresh = { journalVm.refreshHomeFeeds() },
                                onOpenDiscover = { navigateToTab(ROUTE_DISCOVER) },
                                onOpenClinicalIntel = { navigateToTab(ROUTE_CLINICAL_INTEL) },
                                onOpenArticle = ::openArticle,
                                onToggleBookmark = { art, lib -> libraryAdminVm.toggleBookmark(art, lib) }
                            )
                        }

                        composable(ROUTE_DISCOVER) {
                            DiscoverScreen(
                                filters = searchFilters,
                                resultsState = searchResultsState,
                                currentPage = currentPage,
                                libraryItems = libraryItems,
                                scholarImportMessage = importedScholarMessage,
                                onUpdateFilters = { journalVm.updateFilters(it) },
                                onSearch = { journalVm.executeSearch(resetPage = true) },
                                onNextPage = { journalVm.nextPage() },
                                onPreviousPage = { journalVm.previousPage() },
                                onOpenArticle = ::openArticle,
                                onToggleBookmark = { art, lib -> libraryAdminVm.toggleBookmark(art, lib) },
                                onBuildGoogleScholarUrl = { q -> journalVm.getGoogleScholarUrl(q) },
                                onImportScholarAlert = { raw -> journalVm.importGoogleScholarEmailAlert(raw) },
                                onClearScholarMsg = { journalVm.clearScholarImportMessage() }
                            )
                        }

                        composable(ROUTE_CLINICAL_INTEL) {
                            ClinicalIntelligenceScreen(
                                caseInput = caseInput,
                                safetyScreen = safetyScreen,
                                caseAnalysisState = caseAnalysisState,
                                caseSupportingStudies = caseSupportingStudies,
                                diagnosisReportState = diagnosisReportState,
                                diagnosisStudies = diagnosisStudies,
                                evidenceComparisonState = evidenceComparisonState,
                                latestClinicalStudiesState = latestClinicalStudiesState,
                                savedCasesState = savedCasesState,
                                savedTopicsState = savedTopicsState,
                                guidelines = guidelines,
                                libraryItems = libraryItems,
                                reportExportMessage = reportExportMessage,
                                onUpdateCaseInput = { clinicalVm.updateCaseInput(it) },
                                onClearCaseInput = { clinicalVm.clearCaseInput() },
                                onAnalyzeCase = { clinicalVm.analyzeClinicalCase(strings.isArabic) },
                                onDeleteSavedCase = { id -> clinicalVm.deleteSavedCase(id) },
                                onExploreDiagnosis = { q, bm -> clinicalVm.exploreDiagnosis(q, strings.isArabic, bm) },
                                onToggleTopicBookmark = { name, track, bm -> clinicalVm.toggleTopicBookmark(name, track, bm) },
                                onFetchLatestEvidence = { q, tf -> clinicalVm.fetchLatestClinicalEvidence(q, tf) },
                                onCompareEvidence = { topic -> clinicalVm.compareEvidenceForTopic(topic, strings.isArabic) },
                                onExportReportPdf = { clinicalVm.exportAndShareClinicalReportPdf() },
                                onClearExportMessage = { clinicalVm.clearReportExportMessage() },
                                onOpenArticle = ::openArticle,
                                onToggleArticleBookmark = { art, lib -> libraryAdminVm.toggleBookmark(art, lib) }
                            )
                        }

                        composable(ROUTE_SPECIALTIES) {
                            SpecialtiesScreen(
                                categoriesState = categoriesState,
                                subscribedTopics = userProfile?.subscribedTopics.orEmpty(),
                                onSelectCategory = { cat ->
                                    journalVm.searchByCategory(cat)
                                    navigateToTab(ROUTE_DISCOVER)
                                },
                                onToggleSubscription = { topicName ->
                                    libraryAdminVm.toggleTopicSubscription(
                                        topicName,
                                        userProfile?.subscribedTopics.orEmpty()
                                    )
                                }
                            )
                        }

                        composable(ROUTE_LIBRARY) {
                            LibraryScreen(
                                libraryState = libraryState,
                                collectionsState = collectionsState,
                                onOpenArticle = ::openArticle,
                                onToggleBookmark = { art, lib -> libraryAdminVm.toggleBookmark(art, lib) },
                                onCreateCollection = { name, desc -> libraryAdminVm.createCollection(name, desc) },
                                onOpenLocalPdf = { path -> libraryAdminVm.openLocalPdf(path) },
                                onDeleteLocalPdf = { item -> libraryAdminVm.deleteDownloadedPdf(item) }
                            )
                        }

                        composable(ROUTE_ARTICLE_DETAIL) {
                            val art = selectedArticle
                            if (art == null) {
                                LaunchedEffect(Unit) { navController.popBackStack() }
                            } else {
                                val libItem = libraryItems.firstOrNull { it.articleKey == art.articleKey }
                                ArticleDetailScreen(
                                    article = art,
                                    libraryItem = libItem,
                                    collections = collections,
                                    paperAiState = paperAiState,
                                    pdfDownloadStatus = pdfDownloadStatus,
                                    isAdmin = isAdmin,
                                    onBack = { navController.popBackStack() },
                                    onToggleSave = { libraryAdminVm.toggleSaved(art, libItem) },
                                    onToggleBookmark = { libraryAdminVm.toggleBookmark(art, libItem) },
                                    onToggleFavorite = { libraryAdminVm.toggleFavorite(art, libItem) },
                                    onUpdateReadingStatus = { status -> libraryAdminVm.updateReadingStatus(art, status) },
                                    onSaveNotes = { notes -> libraryAdminVm.savePersonalNotes(art, notes) },
                                    onToggleCollection = { colId -> libraryAdminVm.toggleArticleInCollection(art, libItem, colId) },
                                    onDownloadPdf = { journalVm.downloadLegalPdfForSelectedArticle() },
                                    onOpenLocalPdf = { path -> journalVm.openLocalPdf(path) },
                                    onAnalyzeWithAi = { journalVm.analyzeSelectedArticleWithAi(strings.isArabic) },
                                    onFeatureArticle = { libraryAdminVm.featureArticleForJournal(art) }
                                )
                            }
                        }

                        composable(ROUTE_NOTIFICATIONS) {
                            NotificationsScreen(
                                notificationsState = notificationsState,
                                subscribedTopics = userProfile?.subscribedTopics.orEmpty(),
                                onBack = { navController.popBackStack() },
                                onCheckUpdatesNow = {
                                    libraryAdminVm.checkSubscribedUpdatesNow(
                                        appContext,
                                        userProfile?.subscribedTopics.orEmpty()
                                    )
                                },
                                onMarkRead = { id -> libraryAdminVm.markNotificationRead(id) }
                            )
                        }

                        composable(ROUTE_PROFILE) {
                            ProfileAdminScreen(
                                hospitalConfig = hospitalConfig,
                                localPrefs = localPrefs,
                                userProfileState = userProfileState,
                                isAdmin = isAdmin,
                                adminUsers = adminUsers,
                                syncLogs = syncLogs,
                                onBack = { navController.popBackStack() },
                                onSignOut = { signOut(credentialManager, scope) },
                                onSetLanguage = { lang -> libraryAdminVm.setLanguage(lang) },
                                onSetThemeMode = { mode -> libraryAdminVm.setThemeMode(mode) },
                                onUpdateNotificationPrefs = { push, digest ->
                                    libraryAdminVm.updateNotificationPrefs(push, digest)
                                },
                                onUpdateSyncSettings = { email, hrs ->
                                    libraryAdminVm.updateUnpaywallAndSyncSettings(appContext, email, hrs)
                                },
                                onUploadHospitalLogo = { uri -> libraryAdminVm.uploadOfficialHospitalLogo(appContext, uri) },
                                onPublishEditorial = { post -> libraryAdminVm.publishEditorialOrProtocol(post) },
                                onUpsertCategory = { cat -> libraryAdminVm.upsertAdminCategory(cat) },
                                onUpsertGuideline = { rec -> libraryAdminVm.upsertAdminGuideline(rec) },
                                onClearSyncLogs = { libraryAdminVm.clearSyncLogs() }
                            )
                        }
                    }
                }
            }
        }
    }
}
