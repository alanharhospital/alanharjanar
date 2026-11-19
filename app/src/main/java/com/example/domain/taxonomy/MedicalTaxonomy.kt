package com.example.domain.taxonomy

import com.example.domain.model.GuidelineRecord
import com.example.domain.model.ScientificCategory

object MedicalTaxonomy {

    val respiratoryCareCategories: List<ScientificCategory> = listOf(
        cat("rc_01", "Respiratory Care Fundamentals", "أساسيات الرعاية التنفسية", "RESPIRATORY_CARE", "respiratory care fundamentals clinical practice", 1),
        cat("rc_02", "Respiratory Anatomy and Physiology", "تشريح وفسيولوجيا الجهاز التنفسي", "RESPIRATORY_CARE", "respiratory physiology pulmonary mechanics gas exchange", 2),
        cat("rc_03", "Respiratory Assessment", "التقييم التنفسي السريري", "RESPIRATORY_CARE", "respiratory physical assessment dyspnea work of breathing", 3),
        cat("rc_04", "Pulmonary Function Testing", "اختبارات وظائف الرئة", "RESPIRATORY_CARE", "pulmonary function testing spirometry DLCO", 4),
        cat("rc_05", "Arterial Blood Gas Analysis", "تحليل غازات الدم الشرياني", "RESPIRATORY_CARE", "arterial blood gas analysis acid base hypoxemia", 5),
        cat("rc_06", "Oxygen Therapy", "العلاج بالأكسجين", "RESPIRATORY_CARE", "oxygen therapy target SpO2 hypoxemia", 6),
        cat("rc_07", "Aerosol and Nebulizer Therapy", "العلاج بالرذاذ والاستنشاق", "RESPIRATORY_CARE", "aerosol nebulizer bronchodilator inhalation therapy", 7),
        cat("rc_08", "Airway Management", "إدارة مجرى الهواء", "RESPIRATORY_CARE", "airway management intubation difficult airway ICU", 8),
        cat("rc_09", "Artificial Airways", "الممرات الهوائية الاصطناعية", "RESPIRATORY_CARE", "artificial airway cuff pressure airway care", 9),
        cat("rc_10", "Endotracheal Tubes", "الأنابيب الرغامية", "RESPIRATORY_CARE", "endotracheal tube intubation subglottic suction", 10),
        cat("rc_11", "Tracheostomy Management", "العناية بفتح القصبة الهوائية", "RESPIRATORY_CARE", "tracheostomy management decannulation ICU", 11),
        cat("rc_12", "Mechanical Ventilation", "التهوية الميكانيكية", "RESPIRATORY_CARE", "mechanical ventilation ICU protective ventilation", 12),
        cat("rc_13", "Invasive Mechanical Ventilation", "التهوية الميكانيكية الغازية", "RESPIRATORY_CARE", "invasive mechanical ventilation tidal volume PEEP", 13),
        cat("rc_14", "Non-Invasive Ventilation", "التهوية غير الغازية", "RESPIRATORY_CARE", "noninvasive ventilation NIV BiPAP CPAP", 14),
        cat("rc_15", "Ventilator Modes", "أنماط التهوية الميكانيكية", "RESPIRATORY_CARE", "ventilator modes volume control pressure control APRV", 15),
        cat("rc_16", "Ventilator Graphics and Waveforms", "منحنيات ومخططات التنفس الاصطناعي", "RESPIRATORY_CARE", "ventilator waveforms asynchrony driving pressure", 16),
        cat("rc_17", "Ventilator Monitoring", "مراقبة جهاز التنفس الاصطناعي", "RESPIRATORY_CARE", "ventilator monitoring plateau pressure esophageal pressure", 17),
        cat("rc_18", "Ventilator Troubleshooting", "استكشاف أعطال التهوية وإصلاحها", "RESPIRATORY_CARE", "ventilator alarms troubleshooting auto-PEEP barotrauma", 18),
        cat("rc_19", "Ventilator Weaning", "الفطام عن التهوية الميكانيكية", "RESPIRATORY_CARE", "ventilator weaning spontaneous breathing trial", 19),
        cat("rc_20", "Extubation", "نزع الأنبوب الرغامي", "RESPIRATORY_CARE", "extubation failure post-extubation respiratory support", 20),
        cat("rc_21", "High-Flow Nasal Cannula", "القنية الأنفية عالية التدفق", "RESPIRATORY_CARE", "high flow nasal cannula HFNC respiratory failure", 21),
        cat("rc_22", "Acute Respiratory Failure", "الفشل التنفسي الحاد", "RESPIRATORY_CARE", "acute respiratory failure hypoxemic hypercapnic", 22),
        cat("rc_23", "ARDS", "متلازمة الضائقة التنفسية الحادة (ARDS)", "RESPIRATORY_CARE", "acute respiratory distress syndrome ARDS prone positioning", 23),
        cat("rc_24", "COPD", "مرض الانسداد الرئوي المزمن (COPD)", "RESPIRATORY_CARE", "COPD exacerbation GOLD chronic obstructive pulmonary", 24),
        cat("rc_25", "Asthma", "الربو الشعبي", "RESPIRATORY_CARE", "acute severe asthma GINA status asthmaticus", 25),
        cat("rc_26", "Pneumonia", "الالتهاب الرئوي", "RESPIRATORY_CARE", "community acquired ventilator associated pneumonia", 26),
        cat("rc_27", "Pulmonary Embolism", "الانسداد الرئوي", "RESPIRATORY_CARE", "acute pulmonary embolism ICU thrombolysis anticoagulation", 27),
        cat("rc_28", "Interstitial Lung Diseases", "أمراض الرئة الخلالية", "RESPIRATORY_CARE", "interstitial lung disease pulmonary fibrosis exacerbation", 28),
        cat("rc_29", "Sleep-Related Respiratory Disorders", "اضطرابات التنفس أثناء النوم", "RESPIRATORY_CARE", "obstructive sleep apnea hypoventilation CPAP", 29),
        cat("rc_30", "Pulmonary Rehabilitation", "إعادة التأهيل الرئوي", "RESPIRATORY_CARE", "pulmonary rehabilitation dyspnea exercise capacity", 30),
        cat("rc_31", "Neonatal Respiratory Care", "الرعاية التنفسية لحديثي الولادة", "RESPIRATORY_CARE", "neonatal respiratory distress syndrome surfactant CPAP", 31),
        cat("rc_32", "Pediatric Respiratory Care", "الرعاية التنفسية للأطفال", "RESPIRATORY_CARE", "pediatric acute respiratory failure bronchiolitis ventilation", 32),
        cat("rc_33", "Respiratory Infection Control", "مكافحة العدوى التنفسية", "RESPIRATORY_CARE", "ventilator associated pneumonia prevention infection control", 33),
        cat("rc_34", "Evidence-Based Respiratory Care", "الرعاية التنفسية المبنية على الدليل", "RESPIRATORY_CARE", "evidence based respiratory therapy clinical practice guideline", 34)
    )

    val criticalCareCategories: List<ScientificCategory> = listOf(
        cat("cc_01", "Critical Care Medicine", "طب العناية المركزة", "CRITICAL_CARE", "critical care medicine intensive care outcomes", 1),
        cat("cc_02", "ICU Patient Assessment", "تقييم مريض العناية المركزة", "CRITICAL_CARE", "ICU patient assessment SOFA APACHE scoring", 2),
        cat("cc_03", "Intensive Care Monitoring", "المراقبة في العناية المركزة", "CRITICAL_CARE", "intensive care monitoring capnography telemetry", 3),
        cat("cc_04", "Hemodynamic Monitoring", "المراقبة الديناميكية الدموية", "CRITICAL_CARE", "hemodynamic monitoring arterial line cardiac output", 4),
        cat("cc_05", "Cardiovascular Critical Care", "العناية القلبية الحرجة", "CRITICAL_CARE", "cardiogenic shock cardiac ICU acute heart failure", 5),
        cat("cc_06", "Shock and Hemodynamic Instability", "الصدمة وعدم الاستقرار الديناميكي الدموي", "CRITICAL_CARE", "circulatory shock vasopressors lactate resuscitation", 6),
        cat("cc_07", "Sepsis and Septic Shock", "الإنتان والصدمة الإنتانية", "CRITICAL_CARE", "sepsis septic shock surviving sepsis campaign", 7),
        cat("cc_08", "Neurological Critical Care", "العناية العصبية الحرجة", "CRITICAL_CARE", "neurocritical care intracranial pressure status epilepticus", 8),
        cat("cc_09", "Renal Critical Care", "العناية الكلوية الحرجة", "CRITICAL_CARE", "renal critical care fluid balance nephrotoxicity", 9),
        cat("cc_10", "Acute Kidney Injury", "إصابة الكلى الحادة", "CRITICAL_CARE", "acute kidney injury KDIGO ICU biomarkers", 10),
        cat("cc_11", "Renal Replacement Therapy", "العلاج بالاستبدال الكلوي", "CRITICAL_CARE", "continuous renal replacement therapy CRRT ICU", 11),
        cat("cc_12", "Fluid and Electrolyte Management", "إدارة السوائل والشوارد", "CRITICAL_CARE", "balanced crystalloids ICU fluid resuscitation", 12),
        cat("cc_13", "Acid-Base Disorders", "اضطرابات التوازن الحمضي القاعدي", "CRITICAL_CARE", "metabolic acidosis respiratory acidosis Stewart ICU", 13),
        cat("cc_14", "Multiorgan Dysfunction Syndrome", "متلازمة خلل الأعضاء المتعدد", "CRITICAL_CARE", "multiple organ dysfunction syndrome MODS ICU", 14),
        cat("cc_15", "ICU Infection Prevention", "الوقاية من العدوى في العناية المركزة", "CRITICAL_CARE", "CLABSI CAUTI VAP prevention ICU bundle", 15),
        cat("cc_16", "Sedation and Analgesia", "التهدئة والتسكين في العناية المركزة", "CRITICAL_CARE", "PADIS ICU sedation analgesia delirium dexmedetomidine", 16),
        cat("cc_17", "Critical Care Pharmacology", "علم الأدوية في العناية الحرجة", "CRITICAL_CARE", "critical care pharmacology pharmacokinetics vasopressors antibiotics", 17),
        cat("cc_18", "ICU Nutrition", "التغذية في العناية المركزة", "CRITICAL_CARE", "enteral parenteral nutrition critically ill ESPEN ASPEN", 18),
        cat("cc_19", "Emergency and Resuscitation", "الطوارئ والإنعاش", "CRITICAL_CARE", "cardiac arrest resuscitation ACLS post-resuscitation", 19),
        cat("cc_20", "ICU Patient Safety", "سلامة المرضى في العناية المركزة", "CRITICAL_CARE", "ICU patient safety early mobilization checklist", 20),
        cat("cc_21", "Evidence-Based Critical Care", "العناية الحرجة المبنية على الدليل", "CRITICAL_CARE", "evidence based intensive care randomized trial", 21),
        cat("cc_22", "Pediatric Intensive Care", "العناية المركزة للأطفال", "CRITICAL_CARE", "pediatric intensive care PICU PALICC sepsis", 22),
        cat("cc_23", "Neonatal Intensive Care", "العناية المركزة لحديثي الولادة", "CRITICAL_CARE", "neonatal intensive care NICU hemodynamics ventilation", 23),
        cat("cc_24", "Postoperative Critical Care", "العناية المركزة بعد الجراحة", "CRITICAL_CARE", "postoperative ICU cardiac surgery thoracic recovery", 24)
    )

    val allDefaultCategories: List<ScientificCategory> = respiratoryCareCategories + criticalCareCategories

    val defaultCollections: List<Pair<String, String>> = listOf(
        "Mechanical Ventilation" to "#0B3C5D",
        "ARDS" to "#0284C7",
        "ABG Interpretation" to "#0D9488",
        "PFT" to "#0F4C81",
        "ICU Guidelines" to "#4338CA",
        "Critical Care Research" to "#059669"
    )

    val guidelineSocieties: List<String> = listOf(
        "ATS", "ERS", "SCCM", "ESICM", "AARC", "GOLD", "GINA", "WHO", "NICE"
    )

    val protocolTopicsRespiratory: List<String> = listOf(
        "Mechanical Ventilation", "ARDS", "Oxygen Therapy", "NIV", "HFNC",
        "Airway Management", "Ventilator Weaning", "Extubation", "ABG Assessment",
        "Pulmonary Function Testing"
    )

    val protocolTopicsCriticalCare: List<String> = listOf(
        "Sepsis", "Shock", "Hemodynamic Monitoring", "Sedation",
        "ICU Infection Prevention", "Acute Kidney Injury", "ICU Nutrition",
        "Neurological Monitoring"
    )

    /**
     * Authoritative reference index of major published international guidelines
     * (clearly tagged INTERNATIONAL_GUIDELINE — never presented as local hospital protocols).
     */
    val referenceGuidelinesAndProtocols: List<GuidelineRecord> = listOf(
        GuidelineRecord(
            id = "ref_ats_ards_2024",
            title = "An Official ATS Clinical Practice Guideline: Update on Management of Adult Patients with ARDS",
            organization = "ATS (American Thoracic Society)",
            specialtyTrack = "RESPIRATORY_CARE",
            topic = "ARDS",
            publicationYear = 2024L,
            latestUpdateDate = "2024-01-01",
            lastVerifiedDate = "2026-11-19",
            recommendations = "Strong recommendation for low tidal volume ventilation (4–8 mL/kg PBW) and plateau pressure <= 30 cmH2O; conditional recommendation for systemic corticosteroids and neuromuscular blockade in severe ARDS; prone positioning > 12h/day for severe ARDS; VV-ECMO in selected severe refractory ARDS.",
            evidenceCertainty = "Strong / Conditional (GRADE)",
            previousVersionSummary = "2017 ATS/ESICM/SCCM Clinical Practice Guideline on Mechanical Ventilation in Adult Patients with ARDS",
            changesSummary = "2024 update incorporated new RCT evidence on systemic corticosteroids, VV-ECMO (EOLIA/CESAR), and refined PEEP/recruitment maneuver guidance.",
            sourceUrl = "https://www.atsjournals.org/doi/10.1164/rccm.202311-2011ST",
            fullTextAvailable = true,
            protocolScope = "INTERNATIONAL_GUIDELINE",
            doi = "10.1164/rccm.202311-2011ST"
        ),
        GuidelineRecord(
            id = "ref_esicm_ards_2023",
            title = "ESICM Guidelines on Acute Respiratory Distress Syndrome: Definition, Phenotyping and Respiratory Support Strategies",
            organization = "ESICM (European Society of Intensive Care Medicine)",
            specialtyTrack = "RESPIRATORY_CARE",
            topic = "ARDS",
            publicationYear = 2023L,
            latestUpdateDate = "2023-06-16",
            lastVerifiedDate = "2026-11-19",
            recommendations = "Addresses HFNC vs NIV in acute hypoxemic respiratory failure, low tidal volume ventilation, prone positioning, and ECMO criteria.",
            evidenceCertainty = "GRADE Methodology",
            previousVersionSummary = "2012 Berlin Definition & 2017 ESICM/ATS/SCCM Guideline",
            changesSummary = "Expanded phenotyping and non-invasive respiratory support guidance including HFNC.",
            sourceUrl = "https://link.springer.com/article/10.1007/s00134-023-07050-7",
            fullTextAvailable = true,
            protocolScope = "INTERNATIONAL_GUIDELINE",
            doi = "10.1007/s00134-023-07050-7"
        ),
        GuidelineRecord(
            id = "ref_ssc_sepsis_2021",
            title = "Surviving Sepsis Campaign: International Guidelines for Management of Sepsis and Septic Shock 2021",
            organization = "SCCM & ESICM",
            specialtyTrack = "CRITICAL_CARE",
            topic = "Sepsis",
            publicationYear = 2021L,
            latestUpdateDate = "2021-10-02",
            lastVerifiedDate = "2026-11-19",
            recommendations = "Immediate sepsis screening, balanced crystalloids over normal saline, norepinephrine as first-line vasopressor, dynamic fluid responsiveness assessment, early antimicrobials, and ICU follow-up.",
            evidenceCertainty = "Strong / Weak (GRADE)",
            previousVersionSummary = "2016 Surviving Sepsis Campaign Guidelines",
            changesSummary = "2021 shifted 30 mL/kg fluid bolus from strong to weak recommendation, favored balanced crystalloids, and recommended HFNC over NIV for sepsis-induced hypoxemic respiratory failure.",
            sourceUrl = "https://link.springer.com/article/10.1007/s00134-021-06506-y",
            fullTextAvailable = true,
            protocolScope = "INTERNATIONAL_GUIDELINE",
            doi = "10.1007/s00134-021-06506-y"
        ),
        GuidelineRecord(
            id = "ref_ers_hfnc_2022",
            title = "ERS Clinical Practice Guidelines: High-Flow Nasal Cannula in Acute Respiratory Failure",
            organization = "ERS (European Respiratory Society)",
            specialtyTrack = "RESPIRATORY_CARE",
            topic = "HFNC",
            publicationYear = 2022L,
            latestUpdateDate = "2022-04-01",
            lastVerifiedDate = "2026-11-19",
            recommendations = "Conditional recommendation for HFNC over conventional oxygen therapy in hypoxemic acute respiratory failure and post-extubation low-risk patients; trial of NIV prior to HFNC in COPD with acute hypercapnic respiratory failure.",
            evidenceCertainty = "Conditional (GRADE)",
            previousVersionSummary = "Pre-2022 conventional oxygen therapy standards",
            changesSummary = "Established structured comparative indications for HFNC vs COT and NIV across hypoxemic and hypercapnic respiratory failure.",
            sourceUrl = "https://erj.ersjournals.com/content/59/4/2101574",
            fullTextAvailable = true,
            protocolScope = "INTERNATIONAL_GUIDELINE",
            doi = "10.1183/13993003.01574-2021"
        ),
        GuidelineRecord(
            id = "ref_gold_copd_2025",
            title = "Global Strategy for Prevention, Diagnosis and Management of COPD (GOLD Report)",
            organization = "GOLD",
            specialtyTrack = "RESPIRATORY_CARE",
            topic = "COPD",
            publicationYear = 2025L,
            latestUpdateDate = "2024-11-11",
            lastVerifiedDate = "2026-11-19",
            recommendations = "ABE assessment framework, spirometric confirmation (post-bronchodilator FEV1/FVC < 0.7), NIV indications for acute hypercapnic respiratory acidosis, and exacerbation risk reduction.",
            evidenceCertainty = "Evidence Levels A–D",
            previousVersionSummary = "GOLD 2023/2024 Reports",
            changesSummary = "Refined ABE exacerbation classification, blood eosinophil thresholds, and cardiovascular risk screening.",
            sourceUrl = "https://goldcopd.org/",
            fullTextAvailable = true,
            protocolScope = "INTERNATIONAL_GUIDELINE",
            doi = ""
        ),
        GuidelineRecord(
            id = "ref_aarc_weaning_2023",
            title = "AARC Clinical Practice Guideline: Spontaneous Breathing Trials and Ventilator Liberation",
            organization = "AARC (American Association for Respiratory Care)",
            specialtyTrack = "RESPIRATORY_CARE",
            topic = "Ventilator Weaning",
            publicationYear = 2023L,
            latestUpdateDate = "2023-09-01",
            lastVerifiedDate = "2026-11-19",
            recommendations = "Daily readiness screening, standardized spontaneous breathing trials (SBT), cuff leak assessment in high-risk airway edema, and post-extubation NIV/HFNC prophylaxis in high-risk ICU patients.",
            evidenceCertainty = "GRADE Evidence Summary",
            previousVersionSummary = "2017 ATS/CHEST Ventilator Liberation Guideline",
            changesSummary = "Operationalized respiratory therapist-driven weaning protocols and post-extubation support pathways.",
            sourceUrl = "https://rc.rcjournal.com/",
            fullTextAvailable = false,
            protocolScope = "INSTITUTIONAL_PROTOCOL",
            doi = ""
        ),
        GuidelineRecord(
            id = "ref_sccm_padis_2018",
            title = "Clinical Practice Guidelines for Prevention and Management of Pain, Agitation/Sedation, Delirium, Immobility, and Sleep Disruption (PADIS)",
            organization = "SCCM (Society of Critical Care Medicine)",
            specialtyTrack = "CRITICAL_CARE",
            topic = "Sedation",
            publicationYear = 2018L,
            latestUpdateDate = "2018-09-01",
            lastVerifiedDate = "2026-11-19",
            recommendations = "Assessment-driven analgesia-first sedation, light sedation targets (RASS -2 to 0), propofol or dexmedetomidine over benzodiazepines, routine delirium screening (CAM-ICU / ICDSC), and early rehabilitation.",
            evidenceCertainty = "GRADE Recommendations",
            previousVersionSummary = "2013 SCCM PAD Guidelines",
            changesSummary = "Added Immobility and Sleep Disruption domains to the ICU Liberation bundle.",
            sourceUrl = "https://journals.lww.com/ccmjournal/",
            fullTextAvailable = true,
            protocolScope = "INTERNATIONAL_GUIDELINE",
            doi = "10.1097/CCM.0000000000003299"
        ),
        GuidelineRecord(
            id = "ref_kdigo_aki_summary",
            title = "KDIGO Clinical Practice Guideline for Acute Kidney Injury & Critical Care CRRT Overview",
            organization = "KDIGO / Critical Care Consensus",
            specialtyTrack = "CRITICAL_CARE",
            topic = "Acute Kidney Injury",
            publicationYear = 2020L,
            latestUpdateDate = "2020-04-01",
            lastVerifiedDate = "2026-11-19",
            recommendations = "KDIGO serum creatinine & urine output staging (Stages 1–3), nephrotoxin stewardship, dynamic hemodynamic optimization, and timing/modality considerations for RRT in ICU.",
            evidenceCertainty = "Educational Summary of KDIGO",
            previousVersionSummary = "2012 KDIGO AKI Guideline",
            changesSummary = "Clarified RRT initiation trials (AKIKI, IDEAL-ICU, STARRT-AKI) supporting individualized timing without routine early initiation absent urgent indications.",
            sourceUrl = "https://kdigo.org/guidelines/acute-kidney-injury/",
            fullTextAvailable = true,
            protocolScope = "EDUCATIONAL_SUMMARY",
            doi = ""
        )
    )

    private fun cat(
        id: String,
        nameEn: String,
        nameAr: String,
        track: String,
        terms: String,
        order: Long
    ) = ScientificCategory(
        id = id,
        nameEn = nameEn,
        nameAr = nameAr,
        parentTrack = track,
        description = "$nameEn ($nameAr) — indexed international peer-reviewed research.",
        searchQueryTerms = terms,
        sortOrder = order,
        active = true
    )
}
