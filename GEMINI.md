# GEMINI.md — Project Guide for Google Antigravity

This is a native Android (Kotlin + Jetpack Compose + Material 3) project for **AL-ANHAR SCIENTIFIC JOURNAL** (`المجلة العلمية الرقمية – مستشفى الأنهار التخصصي`) and **AL-ANHAR CLINICAL INTELLIGENCE** at Al-Anhar Specialized Hospital.

## Project Identifiers
- **App Name**: `Al-Anhar Journal` (`app/src/main/res/values/strings.xml`, `settings.gradle.kts`, `metadata.json`)
- **Namespace**: `com.example` (do not rename `com/example/` package folders)
- **Application ID**: `com.aistudio.alanharjournal.qrxvkt` (`app/build.gradle.kts`)
- **Min / Target / Compile SDK**: `24` / `36` / `36`
- **Firebase Project**: `gen-lang-client-0956141942` (configured in `app/google-services.json`)
- **Named Firestore Database**: `ai-studio-android-alanharj-7e42f4bc-071d-4743-ac87-6ebf549dc215` (`R.string.firestore_database_id` in `app/src/main/res/values/firebase_applet_config.xml`)

## Critical Architectural Invariants
1. **Named Firestore Database Only**: Always resolve Firestore via `FirebaseFirestore.getInstance(context.getString(R.string.firestore_database_id))` (already wrapped in `FirestoreJournalRepository`). Never use parameterless `FirebaseFirestore.getInstance()` or `Firebase.firestore`.
2. **Firebase Auth**: Google Sign-In via Jetpack `CredentialManager` (`GetSignInWithGoogleOption` / `GetGoogleIdOption` in `core/firebase/AuthManager.kt`). Never add `signInAnonymously()` or email/password auth.
3. **Firebase AI Logic & App Check**: Uses `Firebase.ai(backend = GenerativeBackend.googleAI())` with `gemini-3.8-flash` (`data/repository/GeminiScientificAssistant.kt`). `configureAppCheck(this, intent)` in `MainActivity.onCreate()` must run before `setContent`.
4. **Firestore Models & Rules Parity**: `firestore.rules` validates fields strictly. Keep `@DocumentId` on `id` fields and do not put `"id"` into Firestore write maps. Keep `firestore.rules`, `firebase-blueprint.json`, and `FirestoreJournalRepository.kt` synchronized and validate changes with `firestore.test.js`.
5. **Hospital Admin Role**: Bootstrapped to verified email `ammaralqahwi2026@gmail.com` and `/admins/{uid}` documents. Admin-only features (logo upload via `PickVisualMedia`, editorial/local hospital protocol publishing, categories, featured articles) are enforced both in UI and in `firestore.rules`.
6. **Clinical Safety & Zero-PII Policy**:
   - **Al-Anhar Clinical Intelligence** is strictly an educational clinical knowledge-support system. Never generate autonomous diagnoses, medication orders, dosages, or treatment orders.
   - Case sessions (`ClinicalCaseInput` / `ClinicalCaseSession`) must remain de-identified (no names, MRNs, or phone numbers).
   - Always maintain clear visual and data separation between `INTERNATIONAL_GUIDELINE` and `LOCAL_HOSPITAL_PROTOCOL`.
7. **Academic Integrations**: Live REST clients in `data/remote/AcademicNetworkClients.kt` cover PubMed, Europe PMC, OpenAlex, Semantic Scholar, Crossref, DOAJ, and Unpaywall. Legal PDFs only; disable/hide PDF download actions when `pdfUrl` is blank.

## Package Structure (`app/src/main/java/com/example/`)
- `MainActivity.kt` — App Check init, Auth gate, RTL/LTR & theme binding, adaptive `NavigationBar`/`NavigationRail`, `NavHost`
- `core/firebase/` — `AppCheckHelper.kt`, `AuthManager.kt`, `FirestoreErrorHandler.kt`
- `core/i18n/` — `AppStrings.kt` (English & Arabic strings)
- `domain/model/` — `JournalModels.kt`, `ClinicalModels.kt`
- `domain/taxonomy/` — `MedicalTaxonomy.kt` (34 Respiratory Care + 24 Critical Care disciplines + reference guidelines)
- `data/local/` — Room `JournalDatabase.kt` (`CachedArticleEntity`, `SyncLogEntity`), `PreferencesManager.kt`
- `data/remote/` — `AcademicNetworkClients.kt`, `MultiDatabaseSearchService.kt`, `PdfDownloadService.kt`
- `data/repository/` — `FirestoreJournalRepository.kt`, `GeminiScientificAssistant.kt`, `ClinicalSafetyScreener.kt`, `ClinicalReportPdfExporter.kt`
- `worker/` — `ResearchMonitorWorker.kt` (WorkManager periodic literature alerts)
- `ui/theme/` — `Color.kt`, `Type.kt`, `Theme.kt`
- `ui/components/` — `CommonComponents.kt`
- `ui/viewmodel/` — `JournalViewModel.kt`, `LibraryAdminViewModel.kt`, `ClinicalIntelligenceViewModel.kt`
- `ui/screens/` — `AuthScreen.kt`, `HomeScreen.kt`, `DiscoverScreen.kt`, `ArticleDetailScreen.kt`, `SpecialtiesScreen.kt`, `LibraryScreen.kt`, `ClinicalIntelligenceScreen.kt`, `ClinicalIntelligenceExplorerTabs.kt`, `NotificationsScreen.kt`, `ProfileAdminScreen.kt`

## Build & Verification Commands
```bash
# Build debug APK
gradle :app:assembleDebug

# Run unit and Robolectric tests
gradle :app:testDebugUnitTest

# Run Firestore security rules tests (with Firebase CLI)
firebase emulators:exec --only firestore "node --test firestore.test.js"

# Deploy updated Firestore rules
firebase deploy --only firestore:rules
```
