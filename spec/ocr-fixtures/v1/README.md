# OCR fixture corpus v1

This versioned corpus contains 96 synthetic, project-owned PNG images. It does
not contain user photos or third-party photography. The cases cover supported
Latin and Cyrillic base aliases and electrical units, plus no-text and
mixed-script confusable negatives. Each source marking is rendered as clean,
rotated, low-contrast, small, curved, and blurred text.

`manifest.json` is generated with the images and records the expected candidate
fields, transformation tag, and SHA-256 for each PNG. OCR output remains
untrusted: fixture expectations validate recognition and parser inputs, not
automatic user confirmation.

Regenerate on the pinned fixture-authoring environment (JDK 17 on Windows) with:

```powershell
java spec/ocr-fixtures/v1/tools/GenerateOcrFixtures.java
```

Do not add production-catalog aliases from OCR mistakes. New real-world images
must have an explicit redistribution/license record and must never be copied
from user captures.
