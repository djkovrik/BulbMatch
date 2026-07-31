# BulbMatch visual asset manifest

## Shipping asset policy

Production visuals are original Compose/vector line art, localized text, and the app icon. Do not ship manufacturer logos, bulb packaging, stock photography, IEC standard sheets, or Lazyweb screenshots.

Every diagram must:

- use a stable asset ID independent of filename;
- show only visually distinguishing identification features, not certified dimensions or gauges;
- include an EN/RU text alternative;
- remain legible in light/dark theme and high contrast;
- work at 200% font scale without being the sole source of information;
- be reviewed against the catalog entry and source manifest.

## Planned original diagrams

The following are candidates, not approved catalog contents:

| Asset ID | Candidate | Required alternative-text focus |
|---|---|---|
| `base_e27` | E27 | Screw base; canonical E27 code |
| `base_e14` | E14 | Smaller screw base; canonical E14 code |
| `base_b22d` | B22d | Bayonet base with two side pins and two contacts |
| `base_gu10` | GU10 | Two short twist-lock pins |
| `base_g9` | G9 | Two looped wire contacts |
| `base_g4` | G4 | Two straight closely spaced pins |
| `base_gu5_3` | GU5.3 | Two straight pins; do not imply voltage |
| `base_g13` | G13 | Two-pin linear-lamp end |
| `base_r7s` | R7s | Double-ended linear contact form |
| `base_2g11` | 2G11 | Four-pin compact-lamp base |

Production filenames and inclusion follow reviewer approval. Diagram geometry must not be used for automated photo classification in MVP.

## Exploratory design references

Files under `assets/design-research/` are non-shipping visual directions:

- `prototype-safety-ladder.jpg` — recommended result information hierarchy;
- `prototype-store-ticket.jpg` — runner-up compact shopping artifact;
- `prototype-fit-passport.jpg` — runner-up paired comparison concept.

Generated text may be imperfect. Product copy and component geometry come from the screen contracts, not pixels in these prototypes.

## Template cleanup for implementation

The existing template contains `IndieFlower-Regular.ttf` and generic demo icons. They are not BulbMatch assets. Remove unused template resources during implementation after confirming there are no references; do not carry the handwriting font into the product.
