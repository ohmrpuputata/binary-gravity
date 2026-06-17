# Visual Identity — “Incursion Report”

> The design philosophy behind the repository banner, README, and key art.
> One named aesthetic, held with discipline. Every mark earns its place.

## The movement

**Incursion Report** treats the project's surface as a recovered document — a
classified field study of something that should not be studyable. The invasion
is not illustrated; it is *catalogued*. We borrow the visual grammar of orbital
surveillance and hazard cartography: coordinate ticks, registration brackets,
monospace annotations, a single descent trajectory plotted across the void. The
result should read like an artifact that proves a living catastrophe can be
measured, mapped, and — eventually — ended.

Space is mostly emptiness. The composition breathes around one bright signal
against a near-black field, the way a single contaminated reading dominates an
otherwise dead instrument. Density is reserved, not sprayed: clusters of fine
detail (ticks, labels, the timeline) sit inside vast calm so the eye is led, never
crowded. White space here is not absence — it is the void the swarm crosses.

Color is a warning system, not decoration. One dominant **signal green** carries
almost all meaning; one **hazard amber** is spent only on genuine danger — the
day-8 marker, the threat crest. Nothing competes with these two. The neutral is
a cold, deep black-blue that lets the signal glow as if lit from within the page.

Type is instrumentation. Monospace annotation does the technical talking —
small, precise, evenly tracked, the voice of a readout. The wordmark stands
apart as the one heavy, human gesture, so the brand reads instantly while the
surrounding labels keep their clinical calm. Text is sparse and structural;
paragraphs never clutter the artwork.

This identity must look **meticulously crafted** — the product of deep
expertise, with painstaking attention to alignment, optical spacing, and
restraint. Master-level execution means nothing is approximate: every tick lands
on the grid, every glow is intentional, every label is legible. If a flourish
does not serve the thesis of *a hazard measured in the dark*, it is removed.

The quiet conceptual thread: an eight-day countdown rendered as an orbital
trajectory, where the final node is not a destination but a confrontation. Those
who play will feel it; everyone else simply sees a precise, ominous diagram.

## Design tokens

| Token | Value | Role |
| --- | --- | --- |
| `--void` | `#05070C` | Primary background, deepest field |
| `--void-panel` | `#0B121C` | Raised panels, chips |
| `--signal` | `#6BFF93` | Dominant — life-sign green, the one bright voice |
| `--signal-deep` | `#1F9E5F` → `#128A4B` | Gradient core, atmosphere rim |
| `--signal-dim` | `#2E5C44` | Grid, ticks, quiet structure |
| `--hazard` | `#FFB020` | Accent — danger only (day-8, threat crest) |
| `--bone` | `#E9F5EC` | Primary text |
| `--bone-dim` | `#8AA596` | Secondary/label text |

> `--signal-deep` is a gradient from `#1F9E5F` to `#128A4B`; the table cell above
> is shorthand. Use the two stops directly in SVG.

## Typography

- **Wordmark:** a heavy grotesque — `"Arial Black", "Helvetica Neue", system-ui, sans-serif` — tight tracking, the single human gesture.
- **Instrumentation:** monospace — `"SFMono-Regular", "Cascadia Code", Consolas, "Roboto Mono", monospace` — small, uppercase, generously letter-spaced.

## Motif

- **The sigil:** a targeting reticle fused with three swarm lobes and a downward
  strike chevron — biohazard meets orbital lock.
- **The trajectory:** a single dashed descent arc with node pips; the eighth node
  is amber and larger — the boss.
- **Registration marks & coordinate ticks** frame the field as a measured document.

## Usage rules

- Two accents maximum on any surface: `--signal` always, `--hazard` only for danger.
- Keep the void dominant. Resist filling negative space.
- Monospace for data and labels; the heavy wordmark only for the name.
- Banners live in `docs/assets/`. English: `repo-banner.svg`. Russian: `repo-banner-ru.svg`.
