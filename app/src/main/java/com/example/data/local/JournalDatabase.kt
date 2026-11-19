package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.domain.model.ResearchArticle
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "cached_articles")
data class CachedArticleEntity(
    @PrimaryKey val articleKey: String,
    val title: String,
    val authors: String,
    val journal: String,
    val publicationDate: String,
    val abstractText: String,
    val doi: String,
    val pmid: String,
    val pmcid: String,
    val publicationType: String,
    val sourceDatabase: String,
    val citationCount: Int,
    val publisherUrl: String,
    val openAccess: Boolean,
    val fullTextAvailable: Boolean,
    val pdfUrl: String,
    val localPdfPath: String,
    val preprint: Boolean,
    val retracted: Boolean,
    val retrievedAt: String,
    val specialtyTrack: String,
    val cachedAtMillis: Long
) {
    fun toResearchArticle(): ResearchArticle = ResearchArticle(
        articleKey = articleKey,
        title = title,
        authors = authors,
        journal = journal,
        publicationDate = publicationDate,
        abstractText = abstractText,
        doi = doi,
        pmid = pmid,
        pmcid = pmcid,
        publicationType = publicationType,
        sourceDatabase = sourceDatabase,
        contributingSources = listOf(sourceDatabase).filter { it.isNotBlank() },
        citationCount = citationCount,
        publisherUrl = publisherUrl,
        openAccess = openAccess,
        fullTextAvailable = fullTextAvailable,
        pdfUrl = pdfUrl,
        preprint = preprint,
        retracted = retracted,
        retrievedAt = retrievedAt,
        specialtyTrack = specialtyTrack
    )

    companion object {
        fun fromArticle(article: ResearchArticle, localPdfPath: String = ""): CachedArticleEntity =
            CachedArticleEntity(
                articleKey = article.articleKey,
                title = article.title,
                authors = article.authors,
                journal = article.journal,
                publicationDate = article.publicationDate,
                abstractText = article.abstractText,
                doi = article.doi,
                pmid = article.pmid,
                pmcid = article.pmcid,
                publicationType = article.publicationType,
                sourceDatabase = article.sourceDatabase,
                citationCount = article.citationCount,
                publisherUrl = article.publisherUrl,
                openAccess = article.openAccess,
                fullTextAvailable = article.fullTextAvailable,
                pdfUrl = article.pdfUrl,
                localPdfPath = localPdfPath,
                preprint = article.preprint,
                retracted = article.retracted,
                retrievedAt = article.retrievedAt,
                specialtyTrack = article.specialtyTrack,
                cachedAtMillis = System.currentTimeMillis()
            )
    }
}

@Entity(tableName = "sync_logs")
data class SyncLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val sourceName: String,
    val status: String,
    val message: String,
    val discoveredCount: Int,
    val timestampMillis: Long
)

@Dao
interface CachedArticleDao {
    @Query("SELECT * FROM cached_articles ORDER BY cachedAtMillis DESC LIMIT 100")
    fun observeCachedArticles(): Flow<List<CachedArticleEntity>>

    @Query("SELECT * FROM cached_articles WHERE localPdfPath != '' ORDER BY cachedAtMillis DESC")
    fun observeDownloadedPdfs(): Flow<List<CachedArticleEntity>>

    @Query("SELECT * FROM cached_articles WHERE articleKey = :key LIMIT 1")
    suspend fun getByKey(key: String): CachedArticleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<CachedArticleEntity>)

    @Query("UPDATE cached_articles SET localPdfPath = :localPath WHERE articleKey = :key")
    suspend fun updateLocalPdfPath(key: String, localPath: String)
}

@Dao
interface SyncLogDao {
    @Query("SELECT * FROM sync_logs ORDER BY timestampMillis DESC LIMIT 50")
    fun observeSyncLogs(): Flow<List<SyncLogEntity>>

    @Insert
    suspend fun insertLog(log: SyncLogEntity)

    @Query("DELETE FROM sync_logs")
    suspend fun clearLogs()
}

@Database(
    entities = [CachedArticleEntity::class, SyncLogEntity::class],
    version = 1,
    exportSchema = false
)
abstract class JournalDatabase : RoomDatabase() {
    abstract fun cachedArticleDao(): CachedArticleDao
    abstract fun syncLogDao(): SyncLogDao

    companion object {
        @Volatile
        private var INSTANCE: JournalDatabase? = null

        fun getInstance(context: Context): JournalDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    JournalDatabase::class.java,
                    "alanhar_journal_offline.db"
                ).fallbackToDestructiveMigration(true).build().also { INSTANCE = it }
            }
        }
    }
}
