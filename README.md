# AL-ANHAR SCIENTIFIC JOURNAL & CLINICAL INTELLIGENCE
**المجلة العلمية الرقمية – مستشفى الأنهار التخصصي**

Native Android application (Kotlin + Jetpack Compose + Material 3) for **Al-Anhar Specialized Hospital**, combining a multi-database Respiratory Care & Critical Care digital research journal with the **Al-Anhar Clinical Intelligence** knowledge-support module.

---

## Key Features
- **7-Database Research Discovery**: Federated live search and deduplication across PubMed (NCBI), Europe PMC, OpenAlex, Semantic Scholar, Crossref, DOAJ, and Unpaywall, plus external Google Scholar search and alert-email citation import.
- **58 Medical Disciplines**: 34 Respiratory Care + 24 Critical Care specialties with topic alert subscriptions and WorkManager background monitoring.
- **Personal Scientific Library & Legal PDFs**: Per-user Firestore sync for Saved/Bookmarked/Favorite papers, reading progress, personal collections, private notes, and legal open-access PDF downloads.
- **Gemini AI Paper Assistant**: Structured 10-point paper breakdown and respiratory/ICU parameter extraction via Firebase AI Logic (`gemini-3.8-flash`) + App Check.
- **Al-Anhar Clinical Intelligence**: De-identified Clinical Case Analyzer with deterministic physiological safety red-flag screening, Missing Information Detector, 17-point Diagnosis Explorer, Guideline & Protocol Library (distinguishing International Guidelines from Local Hospital Protocols), Evidence Comparison, and De-Identified Clinical Report PDF export.
- **Bilingual & Adaptive**: Full English (LTR) + Arabic (RTL), Light & ICU Dark modes, and responsive phone/tablet layouts.
- **Hospital Admin Dashboard**: Gated to verified hospital admins (`ammaralqahwi2026@gmail.com` or `/admins/{uid}`) for uploading the official hospital logo, publishing editorials/local protocols, curating featured studies, and inspecting sync logs.

---

## Importing & Running in Google Antigravity

### 1. Export & Open in Google Antigravity
1. In AI Studio, choose **Export as ZIP** (or **Push to GitHub**).
2. Extract/clone the repository locally and open the root folder in **Google Antigravity** (or Android Studio).
3. Antigravity will automatically read `GEMINI.md` for architecture, Firebase rules, and clinical safety conventions.

### 2. Local Android SDK & Gradle Setup
- **Android SDK**: Install **Android SDK Platform 36** and **JDK 21** (or 17).
- **SDK Path**: Set `ANDROID_HOME` / `ANDROID_SDK_ROOT`, or let Android Studio create `local.properties` (`sdk.dir=...`). Do not commit `local.properties` to Git.
- **Gradle Wrapper (Optional locally)**: This template uses system `gradle` (`9.3.1` per `gradle/wrapper/gradle-wrapper.properties`). If your local environment prefers `./gradlew`, run `gradle wrapper` locally outside AI Studio, or run `gradle :app:assembleDebug` directly.

### 3. Signing Keystore for Google Sign-In & Firebase Auth
The Firebase project (`gen-lang-client-0956141942`) has this project's debug keystore SHA-1/SHA-256 registered in `app/google-services.json`.
To sign debug APKs in Google Antigravity with the **exact matching fingerprint** so Google Sign-In and App Check work immediately:
```bash
# macOS / Linux
base64 -d debug.keystore.base64 > debug.keystore

# Windows (PowerShell)
[IO.File]::WriteAllBytes("debug.keystore", [Convert]::FromBase64String((Get-Content debug.keystore.base64 -Raw)))
```
*(Do not commit `debug.keystore` to Git; it is already listed in `.gitignore`.)*

### 4. Build & Test Commands
```bash
# Compile & assemble Debug APK
gradle :app:assembleDebug

# Run local JVM unit & Robolectric tests
gradle :app:testDebugUnitTest

# Test Firestore Security Rules locally (requires Node.js + Firebase CLI)
firebase emulators:exec --only firestore "node --test firestore.test.js"

# Deploy Firestore Security Rules (requires Firebase CLI login to gen-lang-client-0956141942)
firebase deploy --only firestore:rules
```

---

## تجهيز المشروع للعمل في Google Antigravity (ملخص سريع بالعربية)
1. **تنزيل المشروع**: قم بتنزيل الملفات كملف مضغوط (ZIP) من AI Studio أو دفعها إلى GitHub ثم افتح المجلد الرئيسي داخل **Google Antigravity**.
2. **ملف السياق التلقائي**: تم إضافة ملف `GEMINI.md` لكي يتعرف وكيل الذكاء الاصطناعي في Antigravity تلقائياً على بنية التطبيق، قواعد Firestore، الهوية البصرية، وضوابط السلامة السريرية.
3. **مفتاح التوقيع (`debug.keystore`)**: لتجنب إعادة تسجيل بصمة SHA-1 لخدمة تسجيل الدخول بحساب Google، فك ترميز ملف `debug.keystore.base64` إلى `debug.keystore` في المجلد الرئيسي للمشروع عبر الأمر الموضح أعلاه.
4. **البناء والاختبار**: تأكد من تثبيت Android SDK 36 وJDK 21 ثم استخدم `gradle :app:assembleDebug` و`gradle :app:testDebugUnitTest`.
