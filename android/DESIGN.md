# Swift Folio Android: design system

This is the design system the app actually implements. The base is Direction C, **"Console"**
(a dark, developer-grade glass console). The app also borrows the hairline rhythm and collapsing
folder headline from A ("Colophon"), and file-type tiles, the morph-to-check selection and the
one-tap region fix from B ("Squishy Bucket"). That choice is recorded in the design decision.
The code lives in `app/src/main/java/io/swiftfolio/app/ui/theme`.

## Color tokens (`ConsoleColors.kt`)

Glass tiers are pre-blended opaque values, so no alpha compositing happens where nothing shows
through. Dynamic color is off because the brand gradient is the identity.

| Token | Dark | Light | Use |
|---|---|---|---|
| canvas | `#07090E` | `#F3F5F9` | Background, with a fixed two-glow aurora (`Modifier.aurora()`) |
| glass1 / 2 / 3 / 4 | `#111318` `#181A1F` `#222429` `#2F3035` | `#FAFBFC` `#FCFCFE` `#FEFEFE` `#EBEFFE` | Pressed rows / cards and fields / bars, sheets and dialogs / selected rows |
| border / borderStrong | white 8% / 14% | ink 8% / 14% | 1dp hairlines / idle field outline |
| rim | white 18%→4% | white→ink 6% | Top-lit luminous rim on floating glass |
| textPrimary / Secondary / Tertiary | `#E8ECF4` `#A4AEC2` `#8590A6` | `#0B1220` `#465169` `#5A647A` | All pairings ≥ 4.5:1 (contrast figures computed in Direction C) |
| accent gradient | `#2E5BFF → #6E44FF` | same | Primary action, progress, selection count, checks only |
| accentText | `#7FA8FF` | `#2F5BEA` | Links, active path segment, folder tiles |
| success / warning / error | `#4ADE80` `#FBBF24` `#FB7185` | `#15803D` `#B45309` `#BE123C` | Status; the dark destructive button uses `#1A0006` text (about 8:1) |

Material 3 roles are mapped from the tokens (`primary = accentStart`, `surfaceContainer* = glass1..4`,
`outline = borderStrong`), so stock components inherit the theme.

## Type (`ConsoleType.kt`)

- **Inter** (OFL, bundled at weights 400/500/600/700) is used for anything a human would say: titles, buttons, explanations and errors.
- **JetBrains Mono** (OFL, weights 400/500) is used for machine identifiers and measurements: keys, prefixes,
  bucket, region, sizes, dates, counts and the credential inputs.
- Mono styles: `row` 14/20 500 (object names), `meta` 12/16 with `tnum` (size · date),
  `path` 13/18 500 (path pill), `input` 15/22 (fields), `caps` 11/14 500 +0.07em (chips).
- Every size is in `sp`. Rows use `heightIn(min)` and never a fixed height, so 200% font scale wraps instead of clipping.

## Shape, spacing and elevation (`ConsoleShapes.kt`, `GlassModifiers.kt`)

- Shapes: 6 / 10 (rows) / 14 (fields, cards) / 20 (command bar, sheets, dialogs) / 28. Pills are `CircleShape`.
  File tiles are 36dp with an 8dp radius.
- The spacing grid is 4dp, with a 16dp gutter and 48dp minimum touch targets. Folder rows are 56dp min and file rows 64dp min
  (Colophon). Rows have no dividers or cards; rhythm comes from spacing, and a row becomes a 10dp slab only when selected.
- Elevation: tonal elevation is 0 everywhere. In dark mode, depth comes from the glass tiers and the luminous rim. In light mode,
  floating chrome adds a soft blue-tinted shadow (ambient ink 6%, spot accent 10%).
- **Deviation:** Compose has no stable backdrop-blur API. Blurring the list behind the command bar would mean
  drawing the list twice, so every API level uses Direction C's documented "tier 2" fallback
  (`floatingGlass`: glass3 at 92% plus the rim).

## Components (`ui/components`, `ui/browser`)

- **Command bar:** a 64dp floating glass bar with three modes, modelled by the sealed `CommandMode`:
  - Browse: up, filter pill, new folder, and a gradient upload button that morphs into a progress chip while any
    upload or download runs (tapping it opens the transfer queue).
  - Search: close, auto-focused mono input, clear.
  - Select: Download, Copy keys, and Delete on an error-tinted slab.
- **Path pill:** a floating breadcrumb pill. It shows `bucket / … / parent / current` (`core.Breadcrumbs`), tapping a segment
  jumps there, `…` opens the path sheet, and a long press copies the `s3://` URI. It hides on scroll-down.
- **Collapsing top bar:** a `LargeTopAppBar` whose headline is the folder name (Colophon). In select mode it is replaced by
  a gradient `N SELECTED` chip with Select all / Clear.
- **File-type tiles** (`FileTypeStyles`): a table maps each `FileKind` to an icon, a tint per theme and a label,
  so type never depends on color alone. In select mode the tile crossfades to a check circle that
  pops on a spring (0.55 damping). That spring is the only bouncy motion in the app.
- **States:** skeleton rows with a diagonal shimmer, a 2dp indeterminate gradient activity line, and a quiet pull line
  (Colophon). Empty, no-results and error screens use a 72dp glass badge. Stale rows dim to 70% under a warning banner.
- **Sheets and dialogs:** opaque glass3 `ModalBottomSheet`s (sort, path, transfer queue for uploads and downloads,
  file details, new folder).
  The delete dialog uses hold-to-confirm (600ms fill) for 50 or more items or any folder, and exposes a direct
  accessibility action. Swipe-to-delete does not exist.
- **Connect:** a glass credential card with mono fields that validate after blur, an exposed region dropdown, and a Remember
  switch row. The gradient Connect button runs a live three-step checklist (resolving region → signing request →
  listing bucket). A wrong-region error shows a one-tap "Switch to <region>" (Squishy Bucket).

## Motion (`ConsoleMotion.kt`)

| Token | ms | Easing | Use |
|---|---|---|---|
| instant | 90 | LinearOutSlowIn | Press, chip toggle |
| quick | 160 | (0.2, 0, 0, 1) | Icon morphs, command-bar mode swap (fade + scale from 0.96) |
| standard | 220 | (0.2, 0, 0, 1) | Folder `cd` slide (1/16 width in, 1/32 out), screen fades |
| exit | 140 | (0.3, 0, 1, 1) | Anything leaving |
| shimmer | 1200 loop | linear | Skeletons |

When the system animator scale is 0, `reduced` motion turns slides into 90ms fades, freezes the
shimmer and activity line, and skips the check pop.

## Not implemented from the direction docs (scope cuts)

Real blur, predictive-back folder preview, drag-select, the odometer count, middle-ellipsis text, the region
search field, upload conflict ("Exists") chips and the "Mono file names" toggle.
