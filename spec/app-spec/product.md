# BulbMatch product contract

## Product promise

BulbMatch helps a non-expert turn the markings on an old replaceable lamp into a conservative shopping profile. It does not identify hidden fixture properties, certify an installation, or choose a commercial product.

The primary audience is renters, young adults, new homeowners, and anyone who buys lamps infrequently. The MVP serves English- and Russian-speaking users in regions whose normal supply is 220–240 V / 50 Hz. Android ships first; the same behavior is specified for iOS.

## Job to be done

When a lamp fails, help me capture or enter what is actually printed on it, make me verify uncertain recognition, and give me a short checklist I can carry to a shop without implying more certainty than the evidence supports.

## Safety posture

- A positive result means only that the confirmed markings form a supported replacement profile for the stated regional scope.
- Base and voltage are mandatory for a positive result. Frequency is optional, but when
  confirmed it must be exactly 50 Hz for the MVP electrical scope.
- Every OCR-derived field is untrusted until the user confirms, edits, or rejects it.
- A fixture's maximum wattage is a separate optional fact. It is never copied or inferred from the old lamp's wattage.
- Missing, unknown, contradictory, unsupported, or out-of-scope facts produce `Need clarification` or `Potential conflict`, never `Compatible`.
- Brightness is lumen-first. Watt equivalence is shown only when explicitly printed and confirmed, or when a reviewed rule has all required inputs and the UI labels the value as an estimate.
- The result always tells the user to switch power off, verify fixture markings, and seek a qualified person when damage, heat, wiring, moisture, enclosure, or dimmer compatibility is uncertain.
- Ads never split a warning from its explanation and never appear in capture, OCR review, or conflict resolution.

## MVP scope

### Included

- Match home with camera, system photo picker, and manual entry.
- On-device OCR for supported Latin and Cyrillic electrical, numeric, and lamp-base markings, used only as a drafting aid. Arbitrary Russian prose is outside the supported recognition promise.
- Per-field review of base, voltage, frequency, source-lamp power, explicitly printed equivalent power, luminous flux, color temperature, shape, and dimmability.
- Optional manually entered fixture maximum wattage.
- Deterministic three-way assessment and a safety-first result card.
- Explicit local save with optional name; local history, reopen, delete one, and clear all.
- Offline base reference with original line diagrams.
- Manual EN/RU language selection with English as the default; system/light/dark theme.
- Firebase Crashlytics only; no product analytics.
- Yandex inline, sticky, and conservatively capped interstitial advertising.

### Excluded

- Account, login, cloud backup, synchronization, catalog network updates, backend, community editing, store inventory, price comparison, affiliate links, barcode or QR product lookup, photo gallery, export, and sharing.
- Automated fixture recognition, electrical certification, wiring guidance, installation steps, or a promise that a lamp will physically fit an unknown enclosure.
- Other supply families, including 100–127 V / 60 Hz and low-voltage systems as recommendation targets. The app may recognize such markings only to stop with a conflict.
- Product analytics, attribution analytics, experiments, push messages, reminders, or background jobs.

## Navigation and information architecture

The root uses three bottom destinations:

1. **Match / Подбор** — the default destination and only place to start a new match.
2. **History / История** — explicitly saved results.
3. **Reference / Справочник** — common base families and identification help.

Settings opens from the top app bar. Root destinations retain their scroll position while the process lives. An unfinished image/OCR/manual draft is memory-only and disappears after process death; returning within the same process resumes it.

## Result information order

The result is an evidence ladder:

1. outcome and electrical-scope warning;
2. confirmed facts and why the outcome was chosen;
3. unresolved or conflicting checks;
4. replacement shopping profile;
5. fixture checklist and safety note;
6. Save and Match another actions;
7. one optional inline advertisement.

The first visual implementation should follow the locally researched “safety-first evidence ladder” direction. “Store ticket” and “fit passport” remain exploratory alternatives, not co-equal designs. The direct bulb-matching reference corpus was thin, so these are design hypotheses rather than measured performance claims.

## Visual language

- Material 3-based calm utility aesthetic.
- Neutral surfaces with a restrained warm-yellow brand accent.
- Semantic green, amber, and red are always paired with icon, heading, and text; color alone never carries status.
- System sans-serif font only. The template's handwriting font is not part of the product and should be removed during implementation if unused.
- Original simple line diagrams for bases and shapes; no manufacturer logos, copied packaging, or decorative stock photography.
- Light and dark palettes, safe-area-aware bottom navigation, minimum 48 dp touch targets, and layouts that tolerate 200% font scale.

## Localization

The first run uses English. Settings can switch between English and Russian without restart; no system-language option is exposed.

Units remain internationally recognizable: `V`, `Hz`, `W`, `lm`, and `K`. Numbers use locale-appropriate decimal separators, while catalog identifiers such as `E27`, `GU10`, and `R7s` are never translated. Search accepts Latin and Cyrillic aliases defined in the catalog.

Core terminology:

| Concept | English | Russian |
|---|---|---|
| Match | Match | Подбор |
| Base/cap | Base | Цоколь |
| Replacement profile | Replacement profile | Профиль замены |
| Compatible | Compatible profile | Совместимый профиль |
| Need clarification | Need clarification | Нужно уточнение |
| Potential conflict | Potential conflict | Возможен конфликт |
| Fixture maximum wattage | Fixture max wattage | Макс. мощность светильника |
| Advertisement | Advertisement | Реклама |

## Monetization contract

Yandex Mobile Ads Compose Multiplatform is the only ad provider in MVP. Production uses the eight approved ad unit IDs recorded in `app-spec.json`: Android/iOS multiplied by result inline, history sticky, reference sticky, and match-exit interstitial.

- `RESULT_INLINE`: only after the complete `Compatible` result, checklist, and actions. No slot on clarification or conflict outcomes.
- `HISTORY_STICKY`: anchored above bottom navigation and safe area; never covers list rows or destructive confirmation.
- `REFERENCE_STICKY`: same behavior as history; never covers a diagram or definition.
- `MATCH_EXIT_INTERSTITIAL`: eligible only after the second or later completed `Compatible` match, when the user explicitly leaves the result. At most one per three completed compatible matches and no more than one per ten minutes. Never show after a conflict, during saving, on app launch/resume, or when the next destination contains a pending destructive dialog.
- Failed or offline ads collapse to zero height. Ad loading never blocks product state.
- Preview, screenshot, unit, and UI-test builds do not initialize the SDK. Debug device builds use official test IDs. Release validation fails when a production key is missing or matches a known test ID.
- Every visible banner has the localized accessibility label “Advertisement / Реклама”.

No relevant utility-app A/B evidence was found for these placements. They are a conservative product hypothesis, subject to post-release revenue observation in the Yandex console without adding product analytics.

## Privacy position

- Photos are processed on device and discarded when the in-memory flow ends.
- Only fields the user explicitly confirms may enter a saved result. An unknown raw base string may be saved only with a `Need clarification` result.
- No gallery permission is requested because import uses the system photo picker.
- Camera permission is requested only after the user chooses Camera.
- Yandex advertising is configured conservatively: no ATT prompt, no advertising-ID permission on Android, no location signal, no custom targeting, and no affirmative personal-data consent passed to the SDK.
- Firebase Crashlytics receives platform crash data but no photo, OCR transcript, confirmed lamp values, result name, stable app-user ID, analytics breadcrumbs, or custom user content.
- The app is not directed to children.

## Success and guardrails

The primary success criterion is zero known false-positive compatible recommendations. The release-blocking guardrail is: if the app cannot establish a known base and in-scope voltage from user-confirmed data, it must not show `Compatible`.

Secondary outcomes are useful history reuse and ad revenue visible in Yandex reporting. Neither may weaken the safety guardrail.

## Resolved release ownership

- Final catalog/ruleset reviewer: Sergey V. Automated checks and multiple AI models may challenge rules and generate cases, but their output is advisory; Sergey V. records the final versioned approval.
- Publisher: Sergey V.
- Support: `info@sedsoftware.com`.
- Privacy policy: `https://sedsoftware.com/apps/bulbmatch/policy.html`.
- All previously identified product `openQuestions` are resolved; `app-spec.json` keeps an empty array for the contract.
