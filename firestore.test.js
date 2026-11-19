const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const ADMIN_EMAIL = "ammaralqahwi2026@gmail.com";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: denied across all collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("categories").get());
  await assertFails(unauthDb.collection("editorial_posts").get());
  await assertFails(unauthDb.collection("monitored_articles").get());
  await assertFails(unauthDb.collection("guideline_records").get());
  await assertFails(unauthDb.collection("hospital_config").doc("branding").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("library").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("clinical_cases").get());
});

test("User profile: owner CRUD, PII isolation, and privilege escalation rejection", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@hospital.org", email_verified: true }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@hospital.org", email_verified: true }).firestore();
  const now = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Dr. Alice",
      email: "alice@hospital.org",
      specialty: "Respiratory Care",
      language: "en",
      themeMode: "light",
      subscribedTopics: ["Mechanical Ventilation", "ARDS"],
      pushEnabled: true,
      emailDigestEnabled: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Partial update allowed
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      language: "ar",
      subscribedTopics: ["Mechanical Ventilation", "ARDS", "Sepsis and Septic Shock"],
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Ghost/shadow field rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      role: "admin",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Cross-user PII read/write rejected
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).update({ displayName: "Hacked" }));
  await assertFails(bobDb.collection("users").get());
});

test("User library, collections, notifications, and clinical_cases: strict owner isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 5000);

  // Library create (NO id field)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("library").doc("pmid_12345").set({
      userId: ALICE_UID,
      articleKey: "pmid_12345",
      title: "Lung Protective Ventilation in ARDS",
      authors: "Brower RG et al.",
      journal: "NEJM",
      publicationDate: "2024-05-01",
      abstractText: "Low tidal volume ventilation...",
      doi: "10.1056/NEJMoa0000",
      pmid: "12345",
      publicationType: "Randomized Controlled Trial",
      sourceDatabase: "PubMed",
      publisherUrl: "https://pubmed.ncbi.nlm.nih.gov/12345/",
      pdfUrl: "",
      openAccess: true,
      fullTextAvailable: true,
      preprint: false,
      retracted: false,
      saved: true,
      bookmarked: true,
      favorite: false,
      readingStatus: "UNREAD",
      personalNotes: "Check PEEP titration table",
      collectionIds: ["col_mv"],
      localPdfPath: "",
      lastOpenedMillis: Date.now(),
      createdAt: now,
      updatedAt: now,
    })
  );

  // Reject @DocumentId id leakage
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("library").doc("pmid_999").set({
      id: "pmid_999",
      userId: ALICE_UID,
      articleKey: "pmid_999",
      title: "Bad payload",
      saved: true,
      bookmarked: false,
      favorite: false,
      readingStatus: "UNREAD",
      collectionIds: [],
      lastOpenedMillis: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Partial update on library item
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("library").doc("pmid_12345").update({
      readingStatus: "READING",
      personalNotes: "Updated bedside note",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Clinical case session: owner only
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("clinical_cases").doc("case_01").set({
      userId: ALICE_UID,
      caseTitle: "Hypoxemic Respiratory Failure Assessment",
      preliminaryDiagnosis: "Suspected ARDS",
      specialtyTrack: "RESPIRATORY_CARE",
      demographicsSummary: "Adult, 58y",
      vitalsSummary: "RR 28, SpO2 86%",
      respiratoryAbgSummary: "pH 7.31, PaO2 54, FiO2 0.8",
      labsSummary: "Lactate 2.8",
      imagingSummary: "Bilateral alveolar infiltrates",
      safetyAlertLevel: "URGENT",
      aiReportSummary: "Educational synthesis...",
      missingChecklistSummary: "Plateau pressure missing",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Cross-user denial
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("library").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("clinical_cases").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("clinical_cases").doc("case_01").get());

  // Collection & notification CRUD
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("collections").doc("col_mv").set({
      userId: ALICE_UID,
      name: "Mechanical Ventilation",
      description: "ICU vent strategies",
      colorHex: "#0B3C5D",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("notifications").doc("notif_1").set({
      userId: ALICE_UID,
      title: "New ARDS Trial",
      body: "Published in Intensive Care Medicine",
      notificationType: "SUBSCRIBED_SPECIALTY",
      articleKey: "pmid_12345",
      doi: "10.1056/NEJMoa0000",
      read: false,
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Admin RBAC & email spoofing protection", async () => {
  const spoofedAdminDb = testEnv.authenticatedContext("spoof_uid", {
    email: ADMIN_EMAIL,
    email_verified: false,
  }).firestore();
  const realAdminDb = testEnv.authenticatedContext("admin_uid", {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();
  const regularDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@hospital.org",
    email_verified: true,
  }).firestore();
  const now = new Date(Date.now() - 5000);

  const catPayload = {
    nameEn: "Mechanical Ventilation",
    nameAr: "التهوية الميكانيكية",
    parentTrack: "RESPIRATORY_CARE",
    description: "Invasive and non-invasive ventilatory support",
    searchQueryTerms: "mechanical ventilation ICU",
    sortOrder: 12,
    active: true,
    createdAt: now,
    updatedAt: now,
  };

  // Spoofed unverified email and ordinary user must be rejected
  await assertFails(spoofedAdminDb.collection("categories").doc("cat_mv").set(catPayload));
  await assertFails(regularDb.collection("categories").doc("cat_mv").set(catPayload));

  // Verified admin succeeds
  await assertSucceeds(realAdminDb.collection("categories").doc("cat_mv").set(catPayload));

  // Ordinary user can read categories, editorial_posts, guideline_records, hospital_config
  await assertSucceeds(regularDb.collection("categories").get());
  await assertSucceeds(regularDb.collection("editorial_posts").get());
  await assertSucceeds(regularDb.collection("guideline_records").get());

  // Admin can publish local hospital protocol & hospital branding
  await assertSucceeds(
    realAdminDb.collection("hospital_config").doc("branding").set({
      hospitalNameEn: "Al-Anhar Specialized Hospital",
      hospitalNameAr: "مستشفى الأنهار التخصصي",
      tagline: "ADVANCING RESPIRATORY & CRITICAL CARE THROUGH SCIENTIFIC EVIDENCE.",
      logoDataUri: "",
      updateFrequencyHours: 12,
      unpaywallEmail: "research@alanharhospital.org",
      enabledSources: ["PubMed", "Europe PMC", "OpenAlex"],
      updatedBy: "admin_uid",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    realAdminDb.collection("guideline_records").doc("ards_ats_2024").set({
      title: "Update on Management of Adult Patients with ARDS",
      organization: "ATS",
      specialtyTrack: "RESPIRATORY_CARE",
      topic: "ARDS",
      publicationYear: 2024,
      latestUpdateDate: "2024-01-01",
      lastVerifiedDate: "2026-11-19",
      recommendations: "Lung-protective ventilation with low tidal volume 4-8 mL/kg PBW.",
      evidenceCertainty: "Strong recommendation, moderate certainty",
      previousVersionSummary: "2017 ATS/ESICM/SCCM ARDS Guideline",
      changesSummary: "Updated corticosteroid and neuromuscular blockade guidance.",
      sourceUrl: "https://www.thoracic.org",
      fullTextAvailable: true,
      protocolScope: "INTERNATIONAL_GUIDELINE",
      doi: "10.1164/rccm.202311-2011ST",
      createdAt: now,
      updatedAt: now,
    })
  );
});
