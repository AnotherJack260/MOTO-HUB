# MOTO-HUB design system

How the Android app looks and behaves. The values live in
`apps/android/app/src/main/java/io/motohub/android/ui/theme/MotoHubTheme.kt`.
The building blocks live in `ui/components/`. A screen that needs something this
document does not describe should add it here first.

The reference is Revolut's dark UI:
- quiet near-black canvas
- grouped rounded lists
- one obvious action per screen
- sheets for decisions
- short confirmations

The rider has gloves on, is in sunlight, and is in a hurry next to the bike.

## Principles

1. **One primary action per screen.** Only one lime-filled control is visible at
   a time. Every other action is secondary, tertiary, or a list row.
2. **Status over explanation.** Say what is happening in a few words. Put the why
   behind "Details", an info row, or the help guide. Never hide the one action
   that gets the rider unstuck.
3. **The lightest feedback that works.**
   - A finished action, or "couldn't open X", gets a snackbar.
   - A choice gets a bottom sheet.
   - Only three things block:
     - safety acknowledgement
     - crash-data consent
     - the unverified-QR warning
   - System permission prompts stay system dialogs.
4. **Riders first, developers second.** Diagnostics labs, protocol overrides and
   inspectors live under "Advanced" or "Developer tools", never in the main path.
5. **Glove-sized targets.** Primary buttons are 56 dp tall. Everything tappable
   is at least 48 dp.
6. **Calm motion.** Screen transitions take 200–240 ms. Nothing decorative or
   bouncing.

## Tokens

### Colour (dark only)

| Token | Value | Use |
|---|---|---|
| `background` | `#0A0A0B` | canvas, system bars |
| `surface` | `#161618` | cards, list groups |
| `surfaceContainerHigh` | `#202023` | sheets, dialogs, secondary buttons, pressed rows |
| `surfaceContainerHighest` | `#2A2A2E` | icon circles, text fields, switch tracks off |
| `outline` | `#2A2A2E` | the rare divider, field borders |
| `onSurface` | `#F5F5F7` | primary text |
| `onSurfaceVariant` | `#A0A0A6` | secondary text, inactive icons |
| `MotoHubColors.tertiaryText` | `#6E6E74` | disabled and decorative text only |
| `primary` | `#C8F240` (lime) | the primary button, selected tab, switch on, live status, checkmarks |
| `onPrimary` | `#0A0A0B` | text on lime |
| `primaryContainer` | `#252A12` | the subtle tint behind a selected row or live chip |
| `error` / `errorContainer` | `#FF5A52` / `#2B1513` | failures, destructive actions |
| `MotoHubColors.warning` / `warningContainer` | `#FFB340` / `#2B2111` | cautions |
| `scrim` | `#000000` at 60% | behind sheets and dialogs |

Lime is the single accent:
- No per-feature colours.
- List icons are neutral: a light glyph in a `surfaceContainerHighest` circle.
- A selection is shown with a lime checkmark, never with a glowing border.

### Type

System sans-serif everywhere. Monospace is only for machine values the rider may
compare character by character: SSID, support ID, version, hex.

| Style | Size / line | Weight | Use |
|---|---|---|---|
| `displaySmall` | 28 / 34 | Bold | screen title (large, left-aligned) |
| `headlineMedium` | 22 / 28 | Bold | sheet title, hero name |
| `titleLarge` | 20 / 26 | SemiBold | card hero lines |
| `titleMedium` | 16 / 22 | SemiBold | list row title |
| `titleSmall` | 15 / 20 | SemiBold | section header (sentence case, secondary colour) |
| `bodyLarge` | 16 / 24 | Regular | paragraphs, sheet body |
| `bodyMedium` | 14 / 20 | Regular | row subtitle (up to 2 lines), helper text |
| `bodySmall` | 13 / 18 | Regular | captions, footnotes |
| `labelLarge` | 16 / 20 | SemiBold | buttons |
| `labelMedium` | 13 / 16 | Medium | chips, values |
| `labelSmall` | 12 / 16 | Medium | tab labels |

Rules:
- No ALL CAPS anywhere. Sentence case for titles, headers and buttons.
- No trailing full stops on titles.

### Shape, spacing, elevation

- **Radius.**
  - `small` 12: chips, text fields
  - `medium` 16: tiles, banners
  - `large` 20: cards, list groups
  - `extraLarge` 28: sheet top corners
  - Buttons and status chips are full pills.
  - Icon containers are circles: 40 dp in rows, 56 dp in heroes.
- **Spacing.** 4 dp grid.
  - Screen gutter 16.
  - Card padding 16.
  - List row minimum height 64 (56 when there is no subtitle).
  - 24 between sections, 8 between a section header and its card.
- **Elevation.** None. Hierarchy comes from surface steps, not shadows.

## Components (`ui/components`)

| Component | What it is |
|---|---|
| `MhPrimaryButton` | Lime pill, 56 dp, full width. Has a loading state. One per screen. |
| `MhSecondaryButton` | `surfaceContainerHigh` pill, 48 dp, light text. `destructive = true` gives red text. |
| `MhTextButton` | Text only, 48 dp target, for "Cancel", "Details", "Skip". |
| `MhListGroup` | Rounded `surface` card holding rows, no dividers. |
| `MhListRow` | Leading icon circle (optional), title, subtitle (≤2 lines), trailing value, chevron, switch or checkmark. |
| `MhSectionHeader` | Sentence-case `titleSmall` in secondary colour above a group. |
| `MhScreen` | Sub-screen scaffold: 48 dp back icon, large title, scrolling content, optional sticky bottom action that rides above the keyboard. |
| `MhSheet` | `ModalBottomSheet` with grabber, title, short body, primary and secondary pills stacked. |
| `MotoHubSnackbar` | App-level Material `SnackbarHost`, restyled: dark floating pill, leading icon, auto-dismiss, optional action. Falls back to a system toast when the app is not in front. |
| `MhStatusChip` | Pill with a dot: Live (lime), Connecting (pulsing), Offline (grey), Action needed (warning). |
| `MhBanner` | Inline card for a failure or caution: icon, one-line title, one or two lines of body, one action, "Details" for the rest. |
| `MhTextField` | Filled field on `surfaceContainerHighest`, label, helper or error line, password visibility toggle. |
| `MhEmptyState` | Icon circle, title, one line, one action. |

Icons are Material Symbols Rounded (`material-icons-extended`, Rounded set).
They replace the hand-drawn Canvas glyphs.

## Patterns

- **Errors.**
  - The UI maps the raw failure (which logic and the sister app match by string
    equality, so it never changes) to a presentation type.
  - The banner shows a short title, one helpful line, and "Try again".
  - The raw message sits behind "Details".
- **Confirmations.** "Saved", "Copied" and "Couldn't open X" use snackbars.
  Removing something uses a sheet that names it, with a red explicit action
  and a neutral cancel.
- **Settings.**
  - Every choice is a row in a group. Exclusive choices show a trailing
    checkmark in one group.
  - Long explanations are cut to one sentence. A setting that genuinely needs
    more gets an info line under the group, not a paragraph in the row.
- **Haptics.** Only on Connect accepted, session started, Stop, and confirmed
  destructive actions.
- **Text.** Short, plain, rider words. Prefer "dashboard" over "T-Box" in
  rider-facing copy. Keep "T-Box" only where it names a technical setting.
  Every new string goes into all 11 catalogues in `translations/` in the same
  change.
