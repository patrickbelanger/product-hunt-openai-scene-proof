# Demo film

Status: specification only. No demo media, seed command, cached analysis or live demo
is installed by the foundation. The product does not display a non-working demo action.

Target project: **Between the Line — Continuity Study**, a fictional generated metro
and apartment sequence, using original or licensed assets without importing proprietary
project code. A minimal original test sequence is required during the first P0 slice.

Reference Bible: Patrick's face and short hairstyle; fitted black blazer with narrow
lapels and black shirt; black phone and its screen; metro geography and apartment.
Sequence target: about 10–12 shots, one short video plus corresponding references.

| Case | Intended judgment |
| --- | --- |
| Facial/hairstyle drift | Character issue with specific visual evidence |
| Blazer changes navy with wider lapels within metro | Wardrobe issue |
| Phone shape or state changes between adjacent shots | Prop issue |
| Unmotivated screen-direction reversal | Spatial/direction issue |
| Phone display text changes unexpectedly | Text/UI issue |
| Warmer lighting after entering apartment | Intentional; do not flag |
| Phone switches hand while opening door | Intentional with explicit scoped context |

Exact shot IDs and expected findings must be established from actual assets, not
invented before media exists. Keep model uncertainty visible; use expected outcomes
as evaluation targets, never injected model results.

Recording: open demo → import/select sequence → real extraction and analysis →
select wardrobe finding → compare evidence → copy correction → explain intentional
jacket change after transition → re-analyze → show updated finding state/history.

Before launch implement isolated demo sessions, deterministic reset without deleting
another visitor's data, a bounded server-side run quota and a concrete approved spend
budget. A cached first analysis must be labeled and derived from a real recorded run;
at least one central analysis/steering interaction must genuinely use Astra.
