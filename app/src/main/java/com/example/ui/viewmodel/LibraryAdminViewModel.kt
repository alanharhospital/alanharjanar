package com.example.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.JournalDatabase
import com.example.data.local.LocalAppPreferences
import com.example.data.local.PreferencesManager
import com.example.data.local.SyncLogEntity
import com.example.data.remote.AcademicNetworkClients
import com.example.data.remote.PdfDownloadService
import com.example.data.repository.FirestoreJournalRepository
import com.example.domain.model.EditorialPost
import com.example.domain.model.GuidelineRecord
import com.example.domain.model.HospitalConfig
import com.example.domain.model.LibraryItem
import com.example.domain.model.ResearchArticle
import com.example.domain.model.ScientificCategory
import com.example.domain.model.SearchFilters
import com.example.domain.model.TimeFilter
import com.example.domain.model.UiState
import com.example.domain.model.UserCollection
import com.example.domain.model.UserNotification
import com.example.domain.model.UserProfile
import com.example.worker.ResearchMonitorWorker
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

private const val TAG = "LibraryAdminVM"

class LibraryAdminViewModel(
    private val firestoreRepo: FirestoreJournalRepository,
    private val preferencesManager: PreferencesManager,
    private val pdfService: PdfDownloadService,
    private val localDb: JournalDatabase,
    private val currentUserId: String
) : ViewModel() {

    val localPrefs: StateFlow<LocalAppPreferences> = preferencesManager.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), LocalAppPreferences())

    val userProfileState: StateFlow<UiState<UserProfile?>> = firestoreRepo.observeUserProfile(currentUserId)
        .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
        .catch { error -> emit(unwrapError(error)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val hospitalConfig: StateFlow<HospitalConfig> = firestoreRepo.observeHospitalConfig()
        .catch { emit(HospitalConfig()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), HospitalConfig())

    val libraryItemsState: StateFlow<UiState<List<LibraryItem>>> = firestoreRepo.observeLibrary(currentUserId)
        .map<List<LibraryItem>, UiState<List<LibraryItem>>> { UiState.Success(it) }
        .catch { error -> emit(unwrapError(error)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val collectionsState: StateFlow<UiState<List<UserCollection>>> = firestoreRepo.observeCollections(currentUserId)
        .map<List<UserCollection>, UiState<List<UserCollection>>> { UiState.Success(it) }
        .catch { error -> emit(unwrapError(error)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val notificationsState: StateFlow<UiState<List<UserNotification>>> = firestoreRepo.observeNotifications(currentUserId)
        .map<List<UserNotification>, UiState<List<UserNotification>>> { UiState.Success(it) }
        .catch { error -> emit(unwrapError(error)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val syncLogs: StateFlow<List<SyncLogEntity>> = localDb.syncLogDao().observeSyncLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    private val _adminUsers = MutableStateFlow<List<UserProfile>>(emptyList())
    val adminUsers: StateFlow<List<UserProfile>> = _adminUsers.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    init {
        viewModelScope.launch {
            val authUser = Firebase.auth.currentUser
            firestoreRepo.ensureUserProfile(
                userId = currentUserId,
                displayName = authUser?.displayName ?: "Clinician",
                email = authUser?.email ?: ""
            )
            firestoreRepo.ensureDefaultCollections(currentUserId)
            val adminFlag = firestoreRepo.isUserAdmin()
            _isAdmin.value = adminFlag
            if (adminFlag) {
                firestoreRepo.listUsersForAdmin().onSuccess { _adminUsers.value = it }
            }
        }
    }

    fun clearStatusBanner() {
        _statusBanner.value = null
    }

    // --- Library Operations ---

    fun toggleSaved(article: ResearchArticle, current: LibraryItem?) {
        viewModelScope.launch {
            val next = !(current?.saved ?: false)
            firestoreRepo.saveOrUpdateLibraryItem(currentUserId, article, saved = next)
                .onFailure { _statusBanner.value = it.localizedMessage }
        }
    }

    fun toggleBookmark(article: ResearchArticle, current: LibraryItem?) {
        viewModelScope.launch {
            val next = !(current?.bookmarked ?: false)
            firestoreRepo.saveOrUpdateLibraryItem(currentUserId, article, saved = true, bookmarked = next)
                .onFailure { _statusBanner.value = it.localizedMessage }
        }
    }

    fun toggleFavorite(article: ResearchArticle, current: LibraryItem?) {
        viewModelScope.launch {
            val next = !(current?.favorite ?: false)
            firestoreRepo.saveOrUpdateLibraryItem(currentUserId, article, saved = true, favorite = next)
                .onFailure { _statusBanner.value = it.localizedMessage }
        }
    }

    fun updateReadingStatus(article: ResearchArticle, status: String) {
        viewModelScope.launch {
            firestoreRepo.saveOrUpdateLibraryItem(currentUserId, article, saved = true, readingStatus = status)
                .onFailure { _statusBanner.value = it.localizedMessage }
        }
    }

    fun savePersonalNotes(article: ResearchArticle, notes: String) {
        viewModelScope.launch {
            firestoreRepo.saveOrUpdateLibraryItem(currentUserId, article, saved = true, personalNotes = notes)
                .onSuccess { _statusBanner.value = "Notes saved to personal library." }
                .onFailure { _statusBanner.value = it.localizedMessage }
        }
    }

    fun toggleArticleInCollection(article: ResearchArticle, current: LibraryItem?, collectionId: String) {
        viewModelScope.launch {
            val existing = current?.collectionIds.orEmpty().toMutableList()
            if (collectionId in existing) existing.remove(collectionId) else existing.add(collectionId)
            firestoreRepo.saveOrUpdateLibraryItem(currentUserId, article, saved = true, collectionIds = existing)
                .onFailure { _statusBanner.value = it.localizedMessage }
        }
    }

    fun createCollection(name: String, description: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            firestoreRepo.createCollection(currentUserId, name, description)
                .onSuccess { _statusBanner.value = "Collection \"$name\" created." }
                .onFailure { _statusBanner.value = it.localizedMessage }
        }
    }

    fun deleteDownloadedPdf(item: LibraryItem) {
        viewModelScope.launch {
            if (item.localPdfPath.isNotBlank()) {
                pdfService.deleteLocalPdf(item.localPdfPath)
                localDb.cachedArticleDao().updateLocalPdfPath(item.articleKey, "")
                firestoreRepo.saveOrUpdateLibraryItem(
                    userId = currentUserId,
                    article = item.toResearchArticle(),
                    localPdfPath = ""
                )
                _statusBanner.value = "Removed downloaded PDF from device storage."
            }
        }
    }

    fun openLocalPdf(path: String) {
        pdfService.openLocalPdf(path).onFailure { _statusBanner.value = it.localizedMessage }
    }

    // --- Topic Subscriptions & Automatic Research Updates ---

    fun toggleTopicSubscription(topicName: String, currentTopics: List<String>) {
        viewModelScope.launch {
            val updated = currentTopics.toMutableList()
            if (topicName in updated) updated.remove(topicName) else updated.add(topicName)
            firestoreRepo.updateUserProfileFields(currentUserId, subscribedTopics = updated)
                .onFailure { _statusBanner.value = it.localizedMessage }
        }
    }

    fun checkSubscribedUpdatesNow(context: Context, topics: List<String>) {
        viewModelScope.launch {
            _statusBanner.value = "Checking PubMed & Europe PMC for subscribed topics..."
            val clients = AcademicNetworkClients()
            val dao = localDb.cachedArticleDao()
            var count = 0
            for (topic in topics.ifEmpty { listOf("Mechanical Ventilation", "ARDS") }.take(4)) {
                try {
                    val found = clients.searchPubMed(
                        SearchFilters(query = topic, timeFilter = TimeFilter.LAST_7_DAYS),
                        page = 0,
                        pageSize = 2
                    )
                    for (art in found) {
                        if (dao.getByKey(art.articleKey) == null) {
                            firestoreRepo.addNotification(
                                userId = currentUserId,
                                title = "[$topic] ${art.title.take(120)}",
                                body = "${art.journal} (${art.publicationDate}) • ${art.sourceDatabase}",
                                type = if (art.openAccess) "NEW_OPEN_ACCESS" else "SUBSCRIBED_SPECIALTY",
                                articleKey = art.articleKey,
                                doi = art.doi
                            )
                            count++
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Manual sync warning for $topic: ${e.message}")
                }
            }
            preferencesManager.setLastSyncMillis(System.currentTimeMillis())
            if (count > 0) {
                ResearchMonitorWorker.postLocalNotification(
                    context,
                    "Al-Anhar Scientific Journal",
                    "$count new research publications discovered."
                )
            }
            _statusBanner.value = if (count > 0) "Discovered $count new publications." else "All subscribed specialties are up to date."
        }
    }

    fun markNotificationRead(notificationId: String) {
        viewModelScope.launch {
            firestoreRepo.markNotificationRead(currentUserId, notificationId)
        }
    }

    // --- User Preferences ---

    fun setLanguage(language: String) {
        viewModelScope.launch {
            preferencesManager.setLanguage(language)
            firestoreRepo.updateUserProfileFields(currentUserId, language = language)
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            preferencesManager.setThemeMode(mode)
            firestoreRepo.updateUserProfileFields(currentUserId, themeMode = mode)
        }
    }

    fun updateNotificationPrefs(pushEnabled: Boolean, emailDigestEnabled: Boolean) {
        viewModelScope.launch {
            firestoreRepo.updateUserProfileFields(
                currentUserId,
                pushEnabled = pushEnabled,
                emailDigestEnabled = emailDigestEnabled
            )
        }
    }

    fun updateUnpaywallAndSyncSettings(context: Context, email: String, hours: Int) {
        viewModelScope.launch {
            preferencesManager.setUnpaywallEmail(email)
            preferencesManager.setSyncFrequencyHours(hours)
            ResearchMonitorWorker.schedulePeriodicSync(context, hours.toLong())
            if (_isAdmin.value) {
                val cfg = hospitalConfig.value.copy(
                    unpaywallEmail = email,
                    updateFrequencyHours = hours.toLong()
                )
                firestoreRepo.updateHospitalConfig(cfg)
            }
            _statusBanner.value = "Sync settings updated (every ${hours}h)."
        }
    }

    // --- Admin Actions (strictly verified on backend by firestore.rules isAdmin()) ---

    fun uploadOfficialHospitalLogo(context: Context, imageUri: Uri) {
        if (!_isAdmin.value) return
        viewModelScope.launch {
            try {
                val dataUri = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(imageUri)?.use { stream ->
                        val orig = BitmapFactory.decodeStream(stream) ?: return@use ""
                        val maxDim = 256
                        val scale = minOf(maxDim.toFloat() / orig.width, maxDim.toFloat() / orig.height, 1f)
                        val scaled = Bitmap.createScaledBitmap(
                            orig,
                            (orig.width * scale).toInt().coerceAtLeast(1),
                            (orig.height * scale).toInt().coerceAtLeast(1),
                            true
                        )
                        val out = ByteArrayOutputStream()
                        scaled.compress(Bitmap.CompressFormat.PNG, 90, out)
                        "data:image/png;base64," + Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
                    }.orEmpty()
                }
                if (dataUri.isBlank()) {
                    _statusBanner.value = "Could not decode selected logo image."
                    return@launch
                }
                val updated = hospitalConfig.value.copy(logoDataUri = dataUri)
                firestoreRepo.updateHospitalConfig(updated)
                    .onSuccess { _statusBanner.value = "Official hospital logo updated." }
                    .onFailure { _statusBanner.value = it.localizedMessage }
            } catch (e: Exception) {
                _statusBanner.value = e.localizedMessage ?: "Logo upload failed."
            }
        }
    }

    fun publishEditorialOrProtocol(post: EditorialPost) {
        if (!_isAdmin.value) return
        viewModelScope.launch {
            firestoreRepo.publishEditorialPost(post)
                .onSuccess { _statusBanner.value = "Published to Al-Anhar Digital Journal." }
                .onFailure { _statusBanner.value = it.localizedMessage }
        }
    }

    fun upsertAdminCategory(category: ScientificCategory) {
        if (!_isAdmin.value) return
        viewModelScope.launch {
            firestoreRepo.upsertCategory(category)
                .onSuccess { _statusBanner.value = "Scientific category saved." }
                .onFailure { _statusBanner.value = it.localizedMessage }
        }
    }

    fun upsertAdminGuideline(record: GuidelineRecord) {
        if (!_isAdmin.value) return
        viewModelScope.launch {
            firestoreRepo.upsertGuidelineRecord(record)
                .onSuccess { _statusBanner.value = "Protocol/Guideline saved." }
                .onFailure { _statusBanner.value = it.localizedMessage }
        }
    }

    fun featureArticleForJournal(article: ResearchArticle) {
        if (!_isAdmin.value) return
        viewModelScope.launch {
            firestoreRepo.featureResearchArticle(article, featured = true)
                .onSuccess { _statusBanner.value = "Article added to Featured Research." }
                .onFailure { _statusBanner.value = it.localizedMessage }
        }
    }

    fun clearSyncLogs() {
        viewModelScope.launch {
            localDb.syncLogDao().clearLogs()
        }
    }

    private fun <T> unwrapError(error: Throwable): UiState<T> {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        return UiState.Error(firestoreError?.message ?: error.message ?: "Firestore sync error")
    }
}
