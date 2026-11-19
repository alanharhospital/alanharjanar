package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.core.firebase.OperationType
import com.example.core.firebase.handleFirestoreError
import com.example.domain.model.ClinicalCaseSession
import com.example.domain.model.EditorialPost
import com.example.domain.model.GuidelineRecord
import com.example.domain.model.HospitalConfig
import com.example.domain.model.LibraryItem
import com.example.domain.model.MonitoredArticle
import com.example.domain.model.ResearchArticle
import com.example.domain.model.SavedClinicalTopic
import com.example.domain.model.ScientificCategory
import com.example.domain.model.UserCollection
import com.example.domain.model.UserNotification
import com.example.domain.model.UserProfile
import com.example.domain.taxonomy.MedicalTaxonomy
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

private const val TAG = "FirestoreJournalRepo"
private const val BOOTSTRAP_ADMIN_EMAIL = "ammaralqahwi2026@gmail.com"

class FirestoreJournalRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun sanitizeDocId(raw: String): String {
        val cleaned = raw.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(120)
        return cleaned.ifBlank { "doc_${System.currentTimeMillis()}" }
    }

    private fun <T> wrapSnapshotFlow(
        path: String,
        uidForGuard: String? = null,
        mapper: (Flow<T>) -> Flow<T> = { it },
        source: () -> Flow<T>
    ): Flow<T> = flow {
        val currentUid = requireUserId()
        if (uidForGuard != null && uidForGuard != currentUid) {
            throw IllegalStateException("Authenticated user mismatch.")
        }
        val flow = mapper(source()).catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val ex = firestoreError ?: (error as? Exception)
            if (ex != null && auth.currentUser?.uid == currentUid) {
                handleFirestoreError(ex, OperationType.LIST, path)
            }
            throw error
        }
        flow.collect { emit(it) }
    }

    // --- Admin Check ---

    suspend fun isUserAdmin(): Boolean {
        val user = auth.currentUser ?: return false
        if (user.isEmailVerified && user.email.equals(BOOTSTRAP_ADMIN_EMAIL, ignoreCase = true)) {
            return true
        }
        return try {
            val snap = db.collection("admins").document(user.uid).get().await()
            snap.exists()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Admin check returned false: ${e.message}")
            false
        }
    }

    // --- User Profile & Preferences ---

    fun observeUserProfile(userId: String): Flow<UserProfile?> =
        wrapSnapshotFlow("users/$userId", uidForGuard = userId) {
            db.collection("users").document(userId).snapshots()
                .map { it.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        }

    suspend fun ensureUserProfile(
        userId: String,
        displayName: String,
        email: String
    ): Result<Unit> = try {
        val uid = requireUserId()
        check(uid == userId)
        val docRef = db.collection("users").document(userId)
        val snap = docRef.get().await()
        if (!snap.exists()) {
            val payload = mapOf(
                "userId" to userId,
                "displayName" to displayName.ifBlank { "Healthcare Professional" }.take(160),
                "email" to email.take(200),
                "specialty" to "Respiratory Care",
                "language" to "en",
                "themeMode" to "system",
                "subscribedTopics" to listOf("Mechanical Ventilation", "ARDS", "Sepsis and Septic Shock"),
                "pushEnabled" to true,
                "emailDigestEnabled" to false,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
        }
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, "users/$userId")
        Result.failure(e)
    }

    suspend fun updateUserProfileFields(
        userId: String,
        language: String? = null,
        themeMode: String? = null,
        specialty: String? = null,
        subscribedTopics: List<String>? = null,
        pushEnabled: Boolean? = null,
        emailDigestEnabled: Boolean? = null
    ): Result<Unit> = try {
        val uid = requireUserId()
        check(uid == userId)
        val updates = mutableMapOf<String, Any>("updatedAt" to FieldValue.serverTimestamp())
        language?.let { updates["language"] = it }
        themeMode?.let { updates["themeMode"] = it }
        specialty?.let { updates["specialty"] = it.take(160) }
        subscribedTopics?.let { updates["subscribedTopics"] = it.take(50) }
        pushEnabled?.let { updates["pushEnabled"] = it }
        emailDigestEnabled?.let { updates["emailDigestEnabled"] = it }

        db.collection("users").document(userId).update(updates).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "users/$userId")
        Result.failure(e)
    }

    // --- Hospital Config ---

    fun observeHospitalConfig(): Flow<HospitalConfig> =
        wrapSnapshotFlow("hospital_config/branding") {
            db.collection("hospital_config").document("branding").snapshots()
                .map { snap ->
                    snap.toObject(HospitalConfig::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        ?: HospitalConfig()
                }
        }

    suspend fun updateHospitalConfig(config: HospitalConfig): Result<Unit> = try {
        val uid = requireUserId()
        val docRef = db.collection("hospital_config").document("branding")
        val existing = docRef.get().await()
        if (!existing.exists()) {
            val payload = mapOf(
                "hospitalNameEn" to config.hospitalNameEn.take(200),
                "hospitalNameAr" to config.hospitalNameAr.take(200),
                "tagline" to config.tagline.take(300),
                "logoDataUri" to config.logoDataUri.take(300000),
                "updateFrequencyHours" to config.updateFrequencyHours.coerceIn(1L, 168L),
                "unpaywallEmail" to config.unpaywallEmail.take(200),
                "enabledSources" to config.enabledSources.take(12),
                "updatedBy" to uid,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
        } else {
            val updates = mapOf(
                "hospitalNameEn" to config.hospitalNameEn.take(200),
                "hospitalNameAr" to config.hospitalNameAr.take(200),
                "tagline" to config.tagline.take(300),
                "logoDataUri" to config.logoDataUri.take(300000),
                "updateFrequencyHours" to config.updateFrequencyHours.coerceIn(1L, 168L),
                "unpaywallEmail" to config.unpaywallEmail.take(200),
                "enabledSources" to config.enabledSources.take(12),
                "updatedBy" to uid,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.update(updates).await()
        }
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.WRITE, "hospital_config/branding")
        Result.failure(e)
    }

    // --- Categories ---

    fun observeCategories(): Flow<List<ScientificCategory>> =
        wrapSnapshotFlow("categories") {
            db.collection("categories").snapshots().map { snap ->
                val remote = snap.toObjects(ScientificCategory::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                if (remote.isEmpty()) MedicalTaxonomy.allDefaultCategories
                else remote.sortedBy { it.sortOrder }
            }
        }

    suspend fun upsertCategory(category: ScientificCategory): Result<Unit> = try {
        requireUserId()
        val docId = sanitizeDocId(category.id.ifBlank { category.nameEn.lowercase() })
        val docRef = db.collection("categories").document(docId)
        val existing = docRef.get().await()
        if (!existing.exists()) {
            docRef.set(
                mapOf(
                    "nameEn" to category.nameEn.take(160),
                    "nameAr" to category.nameAr.take(160),
                    "parentTrack" to category.parentTrack,
                    "description" to category.description.take(1000),
                    "searchQueryTerms" to category.searchQueryTerms.ifBlank { category.nameEn }.take(400),
                    "sortOrder" to category.sortOrder.coerceIn(0L, 10000L),
                    "active" to category.active,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } else {
            docRef.update(
                mapOf(
                    "nameEn" to category.nameEn.take(160),
                    "nameAr" to category.nameAr.take(160),
                    "parentTrack" to category.parentTrack,
                    "description" to category.description.take(1000),
                    "searchQueryTerms" to category.searchQueryTerms.ifBlank { category.nameEn }.take(400),
                    "sortOrder" to category.sortOrder.coerceIn(0L, 10000L),
                    "active" to category.active,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.WRITE, "categories")
        Result.failure(e)
    }

    suspend fun deleteCategory(categoryId: String): Result<Unit> = try {
        requireUserId()
        db.collection("categories").document(sanitizeDocId(categoryId)).delete().await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.DELETE, "categories/$categoryId")
        Result.failure(e)
    }

    // --- Editorial Posts & Local Hospital Protocols ---

    fun observeEditorialPosts(): Flow<List<EditorialPost>> =
        wrapSnapshotFlow("editorial_posts") {
            db.collection("editorial_posts").snapshots().map { snap ->
                snap.toObjects(EditorialPost::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
        }

    suspend fun publishEditorialPost(post: EditorialPost): Result<Unit> = try {
        val uid = requireUserId()
        val docId = sanitizeDocId(post.id.ifBlank { "post_${System.currentTimeMillis()}" })
        val docRef = db.collection("editorial_posts").document(docId)
        val existing = docRef.get().await()
        if (!existing.exists()) {
            docRef.set(
                mapOf(
                    "title" to post.title.take(300),
                    "titleAr" to post.titleAr.take(300),
                    "summary" to post.summary.take(2000),
                    "body" to post.body.take(20000),
                    "specialty" to post.specialty.take(160),
                    "contentType" to post.contentType,
                    "authorName" to post.authorName.ifBlank { "Al-Anhar Editorial Board" }.take(160),
                    "hospitalProtocol" to post.hospitalProtocol,
                    "externalUrl" to post.externalUrl.take(500),
                    "doi" to post.doi.take(150),
                    "publicationDate" to post.publicationDate.take(40),
                    "verifiedDate" to post.verifiedDate.take(40),
                    "createdBy" to uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } else {
            docRef.update(
                mapOf(
                    "title" to post.title.take(300),
                    "titleAr" to post.titleAr.take(300),
                    "summary" to post.summary.take(2000),
                    "body" to post.body.take(20000),
                    "specialty" to post.specialty.take(160),
                    "contentType" to post.contentType,
                    "authorName" to post.authorName.take(160),
                    "hospitalProtocol" to post.hospitalProtocol,
                    "externalUrl" to post.externalUrl.take(500),
                    "doi" to post.doi.take(150),
                    "publicationDate" to post.publicationDate.take(40),
                    "verifiedDate" to post.verifiedDate.take(40),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.WRITE, "editorial_posts")
        Result.failure(e)
    }

    suspend fun deleteEditorialPost(postId: String): Result<Unit> = try {
        requireUserId()
        db.collection("editorial_posts").document(sanitizeDocId(postId)).delete().await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.DELETE, "editorial_posts/$postId")
        Result.failure(e)
    }

    // --- Monitored / Featured Articles ---

    fun observeMonitoredArticles(): Flow<List<MonitoredArticle>> =
        wrapSnapshotFlow("monitored_articles") {
            db.collection("monitored_articles").snapshots().map { snap ->
                snap.toObjects(MonitoredArticle::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
        }

    suspend fun featureResearchArticle(article: ResearchArticle, featured: Boolean = true): Result<Unit> = try {
        requireUserId()
        val docId = sanitizeDocId(article.articleKey)
        val docRef = db.collection("monitored_articles").document(docId)
        val existing = docRef.get().await()
        if (!existing.exists()) {
            docRef.set(
                mapOf(
                    "title" to article.title.take(600),
                    "authors" to article.authors.take(1000),
                    "journal" to article.journal.take(300),
                    "publicationDate" to article.publicationDate.take(50),
                    "abstractText" to article.abstractText.take(15000),
                    "doi" to article.doi.take(200),
                    "pmid" to article.pmid.take(40),
                    "publicationType" to article.publicationType.take(120),
                    "keywords" to article.keywords.take(25),
                    "sourceDatabase" to article.sourceDatabase.take(120),
                    "citationCount" to article.citationCount.toLong().coerceAtLeast(0L),
                    "publisherUrl" to article.publisherUrl.take(600),
                    "openAccess" to article.openAccess,
                    "fullTextAvailable" to article.fullTextAvailable,
                    "pdfUrl" to article.pdfUrl.take(600),
                    "preprint" to article.preprint,
                    "retracted" to article.retracted,
                    "featured" to featured,
                    "specialtyTrack" to article.specialtyTrack.ifBlank { "RESPIRATORY_CARE" },
                    "categoryName" to article.publicationType.take(160),
                    "retrievedAt" to article.retrievedAt.take(60),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } else {
            docRef.update(
                mapOf(
                    "featured" to featured,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.WRITE, "monitored_articles")
        Result.failure(e)
    }

    suspend fun deleteMonitoredArticle(articleId: String): Result<Unit> = try {
        requireUserId()
        db.collection("monitored_articles").document(sanitizeDocId(articleId)).delete().await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.DELETE, "monitored_articles/$articleId")
        Result.failure(e)
    }

    // --- Guidelines & Protocols ---

    fun observeGuidelineRecords(): Flow<List<GuidelineRecord>> =
        wrapSnapshotFlow("guideline_records") {
            db.collection("guideline_records").snapshots().map { snap ->
                val remote = snap.toObjects(GuidelineRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                val merged = (remote + MedicalTaxonomy.referenceGuidelinesAndProtocols)
                    .distinctBy { it.id.ifBlank { it.title } }
                merged.sortedByDescending { it.publicationYear }
            }
        }

    suspend fun upsertGuidelineRecord(record: GuidelineRecord): Result<Unit> = try {
        requireUserId()
        val docId = sanitizeDocId(record.id.ifBlank { "guide_${System.currentTimeMillis()}" })
        val docRef = db.collection("guideline_records").document(docId)
        val existing = docRef.get().await()
        if (!existing.exists()) {
            docRef.set(
                mapOf(
                    "title" to record.title.take(400),
                    "organization" to record.organization.take(160),
                    "specialtyTrack" to record.specialtyTrack,
                    "topic" to record.topic.take(200),
                    "publicationYear" to record.publicationYear.coerceIn(1990L, 2100L),
                    "latestUpdateDate" to record.latestUpdateDate.take(60),
                    "lastVerifiedDate" to record.lastVerifiedDate.take(60),
                    "recommendations" to record.recommendations.take(15000),
                    "evidenceCertainty" to record.evidenceCertainty.take(200),
                    "previousVersionSummary" to record.previousVersionSummary.take(5000),
                    "changesSummary" to record.changesSummary.take(5000),
                    "sourceUrl" to record.sourceUrl.take(600),
                    "fullTextAvailable" to record.fullTextAvailable,
                    "protocolScope" to record.protocolScope,
                    "doi" to record.doi.take(200),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } else {
            docRef.update(
                mapOf(
                    "title" to record.title.take(400),
                    "organization" to record.organization.take(160),
                    "specialtyTrack" to record.specialtyTrack,
                    "topic" to record.topic.take(200),
                    "publicationYear" to record.publicationYear.coerceIn(1990L, 2100L),
                    "latestUpdateDate" to record.latestUpdateDate.take(60),
                    "lastVerifiedDate" to record.lastVerifiedDate.take(60),
                    "recommendations" to record.recommendations.take(15000),
                    "evidenceCertainty" to record.evidenceCertainty.take(200),
                    "previousVersionSummary" to record.previousVersionSummary.take(5000),
                    "changesSummary" to record.changesSummary.take(5000),
                    "sourceUrl" to record.sourceUrl.take(600),
                    "fullTextAvailable" to record.fullTextAvailable,
                    "protocolScope" to record.protocolScope,
                    "doi" to record.doi.take(200),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.WRITE, "guideline_records")
        Result.failure(e)
    }

    // --- Personal Library ---

    fun observeLibrary(userId: String): Flow<List<LibraryItem>> =
        wrapSnapshotFlow("users/$userId/library", uidForGuard = userId) {
            db.collection("users").document(userId).collection("library").snapshots()
                .map { snap ->
                    snap.toObjects(LibraryItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.lastOpenedMillis }
                }
        }

    suspend fun saveOrUpdateLibraryItem(
        userId: String,
        article: ResearchArticle,
        saved: Boolean? = null,
        bookmarked: Boolean? = null,
        favorite: Boolean? = null,
        readingStatus: String? = null,
        personalNotes: String? = null,
        collectionIds: List<String>? = null,
        localPdfPath: String? = null,
        markOpened: Boolean = false
    ): Result<Unit> = try {
        val uid = requireUserId()
        check(uid == userId)
        val docId = sanitizeDocId(article.articleKey)
        val docRef = db.collection("users").document(userId).collection("library").document(docId)
        val existingSnap = docRef.get().await()
        val nowMillis = System.currentTimeMillis()

        if (!existingSnap.exists()) {
            val payload = mapOf(
                "userId" to userId,
                "articleKey" to article.articleKey.take(200),
                "title" to article.title.take(600),
                "authors" to article.authors.take(1000),
                "journal" to article.journal.take(300),
                "publicationDate" to article.publicationDate.take(50),
                "abstractText" to article.abstractText.take(15000),
                "doi" to article.doi.take(200),
                "pmid" to article.pmid.take(40),
                "publicationType" to article.publicationType.take(120),
                "sourceDatabase" to article.sourceDatabase.take(120),
                "publisherUrl" to article.publisherUrl.take(600),
                "pdfUrl" to article.pdfUrl.take(600),
                "openAccess" to article.openAccess,
                "fullTextAvailable" to article.fullTextAvailable,
                "preprint" to article.preprint,
                "retracted" to article.retracted,
                "saved" to (saved ?: true),
                "bookmarked" to (bookmarked ?: false),
                "favorite" to (favorite ?: false),
                "readingStatus" to (readingStatus ?: "UNREAD"),
                "personalNotes" to (personalNotes ?: "").take(10000),
                "collectionIds" to (collectionIds ?: emptyList()).take(25),
                "localPdfPath" to (localPdfPath ?: "").take(500),
                "lastOpenedMillis" to nowMillis,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
        } else {
            val updates = mutableMapOf<String, Any>("updatedAt" to FieldValue.serverTimestamp())
            saved?.let { updates["saved"] = it }
            bookmarked?.let { updates["bookmarked"] = it }
            favorite?.let { updates["favorite"] = it }
            readingStatus?.let { updates["readingStatus"] = it }
            personalNotes?.let { updates["personalNotes"] = it.take(10000) }
            collectionIds?.let { updates["collectionIds"] = it.take(25) }
            localPdfPath?.let { updates["localPdfPath"] = it.take(500) }
            if (markOpened) updates["lastOpenedMillis"] = nowMillis
            if (article.pdfUrl.isNotBlank()) updates["pdfUrl"] = article.pdfUrl.take(600)
            updates["openAccess"] = article.openAccess
            updates["fullTextAvailable"] = article.fullTextAvailable
            docRef.update(updates).await()
        }
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.WRITE, "users/$userId/library/${article.articleKey}")
        Result.failure(e)
    }

    suspend fun deleteLibraryItem(userId: String, articleKey: String): Result<Unit> = try {
        val uid = requireUserId()
        check(uid == userId)
        db.collection("users").document(userId).collection("library")
            .document(sanitizeDocId(articleKey)).delete().await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.DELETE, "users/$userId/library/$articleKey")
        Result.failure(e)
    }

    // --- Collections ---

    fun observeCollections(userId: String): Flow<List<UserCollection>> =
        wrapSnapshotFlow("users/$userId/collections", uidForGuard = userId) {
            db.collection("users").document(userId).collection("collections").snapshots()
                .map { snap ->
                    snap.toObjects(UserCollection::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.name }
                }
        }

    suspend fun ensureDefaultCollections(userId: String): Result<Unit> = try {
        val uid = requireUserId()
        check(uid == userId)
        val colRef = db.collection("users").document(userId).collection("collections")
        val existing = colRef.limit(1).get().await()
        if (existing.isEmpty) {
            for ((name, colorHex) in MedicalTaxonomy.defaultCollections) {
                val docId = sanitizeDocId("col_${name.lowercase()}")
                colRef.document(docId).set(
                    mapOf(
                        "userId" to userId,
                        "name" to name,
                        "description" to "Personal research folder for $name",
                        "colorHex" to colorHex,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        }
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, "users/$userId/collections")
        Result.failure(e)
    }

    suspend fun createCollection(userId: String, name: String, description: String): Result<Unit> = try {
        val uid = requireUserId()
        check(uid == userId)
        val docId = sanitizeDocId("col_${name.lowercase()}_${System.currentTimeMillis() % 10000}")
        db.collection("users").document(userId).collection("collections").document(docId).set(
            mapOf(
                "userId" to userId,
                "name" to name.trim().take(120),
                "description" to description.trim().take(500),
                "colorHex" to "#0D9488",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, "users/$userId/collections")
        Result.failure(e)
    }

    // --- Notifications ---

    fun observeNotifications(userId: String): Flow<List<UserNotification>> =
        wrapSnapshotFlow("users/$userId/notifications", uidForGuard = userId) {
            db.collection("users").document(userId).collection("notifications").snapshots()
                .map { snap ->
                    snap.toObjects(UserNotification::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
        }

    suspend fun addNotification(
        userId: String,
        title: String,
        body: String,
        type: String,
        articleKey: String = "",
        doi: String = ""
    ): Result<Unit> = try {
        val uid = requireUserId()
        check(uid == userId)
        val docId = sanitizeDocId("notif_${System.currentTimeMillis()}_${title.hashCode()}")
        db.collection("users").document(userId).collection("notifications").document(docId).set(
            mapOf(
                "userId" to userId,
                "title" to title.take(300),
                "body" to body.take(2000),
                "notificationType" to type.take(80),
                "articleKey" to articleKey.take(200),
                "doi" to doi.take(200),
                "read" to false,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, "users/$userId/notifications")
        Result.failure(e)
    }

    suspend fun markNotificationRead(userId: String, notificationId: String): Result<Unit> = try {
        val uid = requireUserId()
        check(uid == userId)
        db.collection("users").document(userId).collection("notifications")
            .document(sanitizeDocId(notificationId))
            .update(mapOf("read" to true, "updatedAt" to FieldValue.serverTimestamp()))
            .await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "users/$userId/notifications/$notificationId")
        Result.failure(e)
    }

    // --- Clinical Intelligence: Private De-identified Case Sessions & Saved Topics ---

    fun observeClinicalCases(userId: String): Flow<List<ClinicalCaseSession>> =
        wrapSnapshotFlow("users/$userId/clinical_cases", uidForGuard = userId) {
            db.collection("users").document(userId).collection("clinical_cases").snapshots()
                .map { snap ->
                    snap.toObjects(ClinicalCaseSession::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
        }

    suspend fun saveClinicalCase(userId: String, session: ClinicalCaseSession): Result<Unit> = try {
        val uid = requireUserId()
        check(uid == userId)
        val docId = sanitizeDocId(session.id.ifBlank { "case_${System.currentTimeMillis()}" })
        db.collection("users").document(userId).collection("clinical_cases").document(docId).set(
            mapOf(
                "userId" to userId,
                "caseTitle" to session.caseTitle.take(250),
                "preliminaryDiagnosis" to session.preliminaryDiagnosis.take(300),
                "specialtyTrack" to session.specialtyTrack.take(80),
                "demographicsSummary" to session.demographicsSummary.take(1000),
                "vitalsSummary" to session.vitalsSummary.take(1500),
                "respiratoryAbgSummary" to session.respiratoryAbgSummary.take(2000),
                "labsSummary" to session.labsSummary.take(2000),
                "imagingSummary" to session.imagingSummary.take(2000),
                "safetyAlertLevel" to session.safetyAlertLevel.take(40),
                "aiReportSummary" to session.aiReportSummary.take(25000),
                "missingChecklistSummary" to session.missingChecklistSummary.take(5000),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, "users/$userId/clinical_cases")
        Result.failure(e)
    }

    suspend fun deleteClinicalCase(userId: String, caseId: String): Result<Unit> = try {
        val uid = requireUserId()
        check(uid == userId)
        db.collection("users").document(userId).collection("clinical_cases")
            .document(sanitizeDocId(caseId)).delete().await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.DELETE, "users/$userId/clinical_cases/$caseId")
        Result.failure(e)
    }

    fun observeSavedClinicalTopics(userId: String): Flow<List<SavedClinicalTopic>> =
        wrapSnapshotFlow("users/$userId/saved_topics", uidForGuard = userId) {
            db.collection("users").document(userId).collection("saved_topics").snapshots()
                .map { snap ->
                    snap.toObjects(SavedClinicalTopic::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.lastViewedMillis }
                }
        }

    suspend fun recordClinicalTopic(
        userId: String,
        topicName: String,
        specialtyTrack: String,
        bookmarked: Boolean
    ): Result<Unit> = try {
        val uid = requireUserId()
        check(uid == userId)
        val docId = sanitizeDocId("topic_${topicName.lowercase()}")
        val docRef = db.collection("users").document(userId).collection("saved_topics").document(docId)
        val existing = docRef.get().await()
        val nowMillis = System.currentTimeMillis()
        if (!existing.exists()) {
            docRef.set(
                mapOf(
                    "userId" to userId,
                    "topicName" to topicName.take(200),
                    "specialtyTrack" to specialtyTrack.take(80),
                    "bookmarked" to bookmarked,
                    "lastViewedMillis" to nowMillis,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } else {
            docRef.update(
                mapOf(
                    "bookmarked" to bookmarked,
                    "lastViewedMillis" to nowMillis,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.WRITE, "users/$userId/saved_topics")
        Result.failure(e)
    }

    // --- Admin User List ---

    suspend fun listUsersForAdmin(): Result<List<UserProfile>> = try {
        requireUserId()
        val snap = db.collection("users").limit(100).get().await()
        Result.success(snap.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.LIST, "users")
        Result.failure(e)
    }
}
