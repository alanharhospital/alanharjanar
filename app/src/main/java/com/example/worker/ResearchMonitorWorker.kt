package com.example.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.R
import com.example.data.local.CachedArticleEntity
import com.example.data.local.JournalDatabase
import com.example.data.local.PreferencesManager
import com.example.data.remote.AcademicNetworkClients
import com.example.data.repository.FirestoreJournalRepository
import com.example.domain.model.SearchFilters
import com.example.domain.model.TimeFilter
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

private const val TAG = "ResearchMonitorWorker"
private const val CHANNEL_ID = "al_anhar_research_updates"
private const val WORK_NAME = "al_anhar_periodic_research_monitor"

class ResearchMonitorWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val currentUser = Firebase.auth.currentUser ?: return Result.success()
        val uid = currentUser.uid

        return try {
            val db = JournalDatabase.getInstance(applicationContext)
            val dao = db.cachedArticleDao()
            val prefs = PreferencesManager(applicationContext)
            val repo = FirestoreJournalRepository(applicationContext)
            val clients = AcademicNetworkClients()

            val profile = repo.observeUserProfile(uid).first()
            val topics = profile?.subscribedTopics?.ifEmpty {
                listOf("Mechanical Ventilation", "ARDS")
            } ?: listOf("Mechanical Ventilation", "ARDS")

            var newlyDiscovered = 0
            for (topic in topics.take(5)) {
                val results = try {
                    clients.searchPubMed(
                        SearchFilters(query = topic, timeFilter = TimeFilter.LAST_7_DAYS),
                        page = 0,
                        pageSize = 3
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "PubMed check failed for $topic: ${e.message}")
                    emptyList()
                }
                for (article in results) {
                    val existing = dao.getByKey(article.articleKey)
                    if (existing == null) {
                        dao.upsertAll(listOf(CachedArticleEntity.fromArticle(article)))
                        val notifType = if (article.openAccess) "NEW_OPEN_ACCESS" else "SUBSCRIBED_SPECIALTY"
                        repo.addNotification(
                            userId = uid,
                            title = "[$topic] ${article.title.take(120)}",
                            body = "${article.journal} (${article.publicationDate}) — Indexed from ${article.sourceDatabase}",
                            type = notifType,
                            articleKey = article.articleKey,
                            doi = article.doi
                        )
                        newlyDiscovered++
                    }
                }
            }

            prefs.setLastSyncMillis(System.currentTimeMillis())

            if (newlyDiscovered > 0 && (profile?.pushEnabled != false)) {
                postLocalNotification(
                    applicationContext,
                    title = "Al-Anhar Scientific Journal",
                    body = "$newlyDiscovered new publications found in your subscribed specialties."
                )
            }
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Background research check error", e)
            Result.retry()
        }
    }

    companion object {
        fun schedulePeriodicSync(context: Context, frequencyHours: Long) {
            val interval = frequencyHours.coerceIn(1L, 168L)
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = PeriodicWorkRequestBuilder<ResearchMonitorWorker>(interval, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        fun postLocalNotification(context: Context, title: String, body: String) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Al-Anhar Research Updates",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Alerts for new publications and clinical guidelines in subscribed specialties"
                }
                manager.createNotificationChannel(channel)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                if (!granted) return
            }
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()
            NotificationManagerCompat.from(context).notify((System.currentTimeMillis() % 10000).toInt(), notification)
        }
    }
}
