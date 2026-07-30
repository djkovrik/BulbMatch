# BulbMatch source and licensing manifest

Manifest version: `1`  
Research/access date: `2026-07-28`  
Purpose: trace product rules, platform constraints, terminology, and design evidence. This file is not the production catalog itself.

## Intake rule

No fact enters the shipped catalog/ruleset until its record contains:

- stable source URL or document identifier;
- source authority and publication/version date when available;
- exact facts extracted and the transformation performed;
- license/usage assessment;
- named human reviewer and review date;
- target catalog/ruleset version and SHA-256 content hash.

If redistribution rights are unclear, retain only a non-copyrightable identifier/fact verified across permitted sources and create original prose/diagrams. Do not copy tables, standard sheets, packaging, logos, manufacturer photography, or long text.

Final review owner: **Sergey V.** Automated checks and multiple AI models may generate tests, cross-check sources, and flag inconsistencies. They are supporting evidence only; the final approval record is a human decision by Sergey V.

## Authoritative product-domain sources

| ID | Source | Intended use | License/usage decision |
|---|---|---|---|
| SRC-001 | [IEC 60061 database overview](https://webstore.iec.ch/en/iec_catalog/product/preview/?id=L3B1Yi9wZGYvcHJldmlldy9pbmZvX2llYzYwMDYxe2VkMS4wfWIucGRm) — “Lamp caps and holders together with gauges for interchangeability and safety” | Establish that base/cap identifiers and interchangeability belong to the IEC 60061 family; reviewer cross-check of canonical identifiers. | Subscription/end-user restrictions apply. Do not ship IEC standard sheets, gauges, dimensions, diagrams, or copied descriptive text. Record identifiers only after reviewer verification; draw original non-gauging identification diagrams. |
| SRC-002 | [Commission Regulation (EU) 2019/2020](https://eur-lex.europa.eu/legal-content/EN/TXT/?uri=CELEX%3A32019R2020) and current consolidated text | Terminology for light sources, declared power, luminous flux, control gear, and mains/non-mains distinctions; source review context. | EU legal text is referenced, not bundled. Verify the current consolidated version at catalog release. Do not present regulation-derived calculations as fixture compatibility rules. |
| SRC-003 | [Commission Delegated Regulation (EU) 2019/2015](https://eur-lex.europa.eu/legal-content/EN/ALL/?uri=CELEX%3A32019R2015) and current consolidated text | Energy-label vocabulary and the separation of lumens, on-mode power, and efficiency. | Reference/citation only. Do not reproduce label artwork. Check EUR-Lex reuse terms before any excerpt beyond short attribution. |
| SRC-004 | [European Commission light-sources overview](https://energy-efficient-products.ec.europa.eu/product-list/light-sources_en) | Consumer-facing confirmation that EPREL records include luminous flux, color temperature, and cap type; current regulation links. | European Commission reuse policy must be checked for copied material. BulbMatch uses original wording and no Commission images. |

## Platform and SDK sources

| ID | Source | Intended use | License/usage decision |
|---|---|---|---|
| SRC-010 | [ML Kit Text Recognition v2 overview](https://developers.google.com/ml-kit/vision/text-recognition/v2), [Android guide](https://developers.google.com/ml-kit/vision/text-recognition/v2/android), [iOS guide](https://developers.google.com/ml-kit/vision/text-recognition/v2/ios), and [supported languages](https://developers.google.com/ml-kit/vision/text-recognition/v2/languages) | On-device OCR capabilities, bundled Android model option, statically linked iOS assets, script/language limitations, target requirements. | Google developer documentation is CC BY 4.0 except noted code samples (Apache 2.0). AppSpec paraphrases; implementation uses SDK APIs under their licenses and verifies current versions. |
| SRC-011 | [Android Photo Picker](https://developer.android.com/training/data-storage/shared/photopicker) and [permission minimization](https://developer.android.com/privacy-and-security/minimize-permission-requests) | Single-image import without broad media access and least-permission behavior. | Android documentation terms apply; paraphrase only. |
| SRC-012 | [Apple privacy protection](https://developer.apple.com/documentation/uikit/protecting-the-user-s-privacy) and [PHPicker](https://developer.apple.com/documentation/photokit/phpickerviewcontroller) | Camera purpose string, system picker, and native permission lifecycle. | Apple documentation terms apply; no copied code/assets in AppSpec. |
| SRC-013 | [Yandex Mobile Ads Compose Multiplatform quick start](https://ads.yandex.com/helpcenter/en/dev/compose-multiplatform/quick-start), [SDK index](https://ads.yandex.com/helpcenter/en/dev/), and [GDPR guidance](https://ads.yandex.com/helpcenter/en/dev/compose-multiplatform/gdpr?lang=en) | Current KMP integration shape, formats, minimums, initialization order, privacy controls, test behavior. | Documentation referenced and paraphrased. Yandex trademarks/logos are not shipped. SDK license/terms and store disclosures must be reviewed at implementation/release. |
| SRC-014 | [Firebase Crashlytics Android setup](https://firebase.google.com/docs/crashlytics/android/get-started), [Apple setup](https://firebase.google.com/docs/crashlytics/ios/get-started), and [Android API reference](https://firebase.google.com/docs/reference/android/com/google/firebase/crashlytics/FirebaseCrashlytics) | Native host integration, validation test crash, data-collection and custom-key behavior. | Firebase documentation is generally CC BY 4.0 with Apache 2.0 code samples where stated. AppSpec paraphrases and intentionally excludes Analytics. |

## Design evidence and generated prototypes

Lazyweb research ran on `2026-07-28` for mobile scanner result cards, confirmation patterns, safety checklists, and monetization placements. The direct bulb-matching corpus was thin; evidence is cross-category and directional.

- Raw research plan/evidence stays under `.lazyweb/deep-design-research/bulbmatch-mobile-2026-07-28/` and is not a shipping asset.
- Individual Lazyweb screenshot URLs and provenance are recorded in its `work/report-data.json`.
- No third-party screenshot is copied into this AppSpec.
- `assets/design-research/*.jpg` are AI-generated exploratory prototypes created for this project. They contain no required production copy, logo, or third-party artwork and are not automatically shipping assets.
- The prototype hosting report was not published: Lazyweb first rejected image size, then required a non-null greenfield control, and finally rejected the corrected retry because the idempotency key was already bound to the previous input. Do not claim a hosted report exists.

## Production catalog source template

For every production entry, add a record equivalent to:

```text
entryId:
canonicalBaseCode:
facts:
primarySourceId:
secondaryVerificationUrl:
accessedAt:
transformation:
redistributionDecision:
reviewer:
reviewedAt:
reviewDecision:
aiAssistanceSummary:
catalogVersion:
contentHash:
```

Manufacturer datasheets may verify a product's printed facts but may not be generalized into universal base compatibility. Logos, product images, marketing descriptions, and proprietary drawings are excluded unless separately licensed.
