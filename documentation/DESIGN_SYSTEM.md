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
6. **Calm motion.** One duration, 220 ms (`MOTION_MILLIS`): screen slides, tab
   crossfades, a banner folding open. A screen slides in over a still, slightly
   dimmed base; no parallax. Nothing decorative or bouncing. Only a status chip
   in progress ("Connecting") pulses.

## Tokens

### Colour (dark only)

| Token | Value | Use |
|---|---|---|
| `background` | `#0A0A0B` | canvas, system bars, splash |
| `surface` | `#1C1C1E` | cards, list groups, empty states |
| `surfaceContainerHigh` | `#2C2C2E` | sheets, dialogs |
| `surfaceContainerHighest` | `#363638` | snackbar, disabled primary button |
| `MotoHubColors.Fill` | white at 12% | controls on any surface: secondary buttons, icon circles, text fields, switch track off, sheet grabber, neutral and progress chips |
| `outline` | `#363638` | the rare divider |
| `onSurface` | `#F5F5F7` | primary text, selected tab |
| `onSurfaceVariant` | `#A0A0A6` | secondary text, idle tab, inactive icons |
| `MotoHubColors.TextTertiary` | `#6E6E74` | disabled and decorative text only (3.9:1, never a label someone must read) |
| `primary` | `#C8F240` (lime) | the primary button, switch on, live status, checkmarks, focused field label and cursor |
| `onPrimary` | `#0A0A0B` | text and spinner on lime |
| `primaryContainer` | `#252A12` | the subtle tint behind a live chip |
| `error` / `errorContainer` | `#FF5A52` / `#2B1513` | failures, destructive actions |
| `MotoHubColors.Warning` / `WarningContainer` | `#FFB340` / `#2B2111` | cautions |
| `scrim` | `#000000` at 60% | behind sheets and dialogs |

Each surface step is about 1.16:1 above the one below, so cards hold up in
sunlight without outlines, and `onSurfaceVariant` stays at 4.5:1 or better on
every surface (7.6 on background, 6.5 surface, 5.4 high, 4.6 highest).

`Fill` is translucent on purpose: it always reads one step lighter than whatever
it sits on. An opaque grey vanished on the surface that shared its value (a
secondary pill on a sheet was 1:1). Known limit: on a sheet, text on `Fill`
drops to 3.7:1 for `onSurfaceVariant` and 3.1:1 for `error`.

Lime is the single accent:
- No per-feature colours.
- List icons are neutral: a light glyph in a `Fill` circle.
- A selection is shown with a lime checkmark, never with a glowing border.
- The selected tab is white, not lime.

### Type

System sans-serif everywhere. Monospace is only for machine values the rider may
compare character by character: SSID, support ID, version, hex.

| Style | Size / line | Weight | Use |
|---|---|---|---|
| `displaySmall` | 28 / 34 | Bold | screen title (large, left-aligned) |
| `headlineMedium` | 22 / 28 | Bold | sheet title, hero name |
| `titleLarge` | 20 / 26 | SemiBold | card hero lines |
| `titleMedium` | 16 / 22 | Medium | list row title |
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
  - `small` 12: text fields
  - `medium` 16: tiles, tab press highlight
  - `large` 20: cards, list groups, banners, the snackbar
  - `extraLarge` 28: sheet top corners
  - Buttons and status chips are full pills.
  - Icon containers are circles: 40 dp in rows, 56 dp in heroes.
- **Spacing.** 4 dp grid.
  - Screen gutter 16.
  - Card padding 16.
  - List row minimum height 64 (56 when there is no subtitle).
  - 24 between sections, 8 between a section header and its card.
- **Elevation.** None. Hierarchy comes from surface steps, not shadows.
- **States.** Pressed is the ripple, nothing else. A disabled row fades to 45%;
  a disabled button turns grey with tertiary text. A loading button keeps its
  colours and shows a spinner in the label colour. A focused field shows a lime
  label (the cursor is lime too); no border.

## Components (`ui/components`)

| Component | What it is |
|---|---|
| `MhPrimaryButton` | Lime pill, 56 dp, full width (`fillWidth = false` to wrap). `loading` keeps it lime with a dark spinner. One per screen. |
| `MhSecondaryButton` | `Fill` pill, 56 dp, full width (`fillWidth = false` to wrap), light text. `destructive = true` gives red text. |
| `MhTextButton` | Text only, 48 dp target, for "Cancel", "Details", "Skip". |
| `MhIconButton` | 48 dp icon-only button: back, close, info. |
| `MhIconCircle` | Neutral icon in a `Fill` circle, 40 dp in rows, 56 dp in heroes. |
| `MhListGroup` | Rounded `surface` card holding rows, no dividers. |
| `MhListRow` | Leading icon circle (optional), title, subtitle (never truncated), trailing value, chevron, or a control. |
| `MhSwitchRow` / `MhSwitch` | An on/off setting. The whole row toggles and is the one control TalkBack announces, with its state. |
| `MhChoiceRow` | One of several exclusive choices; a lime check marks the chosen one. Put all the choices in one group. |
| `MhSectionHeader` | Sentence-case `titleSmall` in secondary colour above a group. A heading for TalkBack. |
| `MhFootnote` | The standard info line under a group: the one sentence a setting needs and its row has no room for. |
| `MhScreen` | Sub-screen scaffold: 48 dp back icon, large title, scrolling content, optional sticky bottom action that rides above the keyboard. System back calls `onBack`. Without a bottom action, the scroll ends clear of the navigation bar and the keyboard. |
| `MhTabPage` | A tab's own page: large title, scrolling content, no back button. |
| `MhSheet` | `ModalBottomSheet` with grabber, title, short body, primary and secondary pills stacked. See Sheets below. |
| `MhDialog` | The blocking dialog, for the three cases in principle 3 only. Kit buttons, scrolling body, back and outside taps ignored unless `dismissible`. |
| `MotoHubDialogBody` | The scrolling, edge-faded body slot for any `AlertDialog` (`MhDialog` uses it). |
| `MotoHubSnackbar` | App-level Material `SnackbarHost`, restyled: a dark rounded card (20 dp), leading icon, auto-dismiss, optional action. Tap to dismiss. The newest message replaces the one on screen. Falls back to a system toast when the app is not in front. |
| `MhStatusChip` | Pill with a dot: Live (lime), Connecting (pulsing), Offline (grey), Action needed (warning). Only Connecting pulses. |
| `MhBanner` | Inline card for a failure or caution: icon, one-line title, one or two lines of body, one action, "Details" for the rest. |
| `MotoHubNotice` | Only for long runtime instructions (the Android Auto failure steps) until the Ride slice folds it into `MhBanner`. |
| `MhTextField` | Filled field on `Fill`, 12 dp corners, label, helper or error line (read by TalkBack), password visibility toggle. `monospace` also turns off autocorrect and capitalisation. |
| `MhEmptyState` | Icon circle, title, one line, one action. |

Legacy components are `@Deprecated` with a `ReplaceWith`: `MotoHubDetailScreen`,
`MotoHubActionRow`, `ToggleRow`, `MotoHubRadioRow`, `MonoLabel`,
`HeroPrimaryAction`, `HeroTile`, `HeroOptionRow`. Each slice migrates the call
sites it owns; do not add new ones.

Icons are Material Symbols Rounded (`material-icons-extended`, Rounded set).
They replace the hand-drawn Canvas glyphs.

## Patterns

- **Errors.**
  - The UI maps the raw failure (which logic and the sister app match by string
    equality, so it never changes) to a presentation type.
  - Every failure and caution is an `MhBanner`: a short title, one helpful
    line, and one action such as "Try again".
  - The raw message sits behind "Details".
  - `MotoHubNotice` is only for the long Android Auto instructions.
- **Confirmations.** "Saved", "Copied" and "Couldn't open X" use snackbars.
  Removing something uses a sheet that names it, with a red explicit action
  and a neutral cancel:
  ```kotlin
  MhSheet(
      onDismiss = { confirmRemove = false },
      title = motoHubText("Remove this motorcycle?"),
      primaryLabel = motoHubText("Remove"),
      onPrimary = { garage.remove(bike) },
      secondaryLabel = motoHubText("Cancel"),
      destructivePrimary = true
  )
  ```
- **Sheets.** `onDismiss` means the sheet is gone, by any path (scrim, back,
  swipe, any button), and runs once. Clear your `showX` flag there and nowhere
  else; put Cancel's own logic in `onSecondary`. Actions run after the sheet has
  slid away. A snackbar raised while a sheet or dialog is open draws underneath
  it (they are separate windows), so close the sheet first: inside `content`,
  `close { MotoHubSnackbar.success(context, motoHubText("Saved")) }`.
- **Blocking dialogs.** Only the safety acknowledgement, crash-data consent and
  the unverified-QR warning, and each uses `MhDialog`. Everything else is a
  sheet or a snackbar.
- **Settings.**
  - Every choice is a row in a group. Exclusive choices show a trailing
    checkmark in one group.
  - Long explanations are cut to one sentence. A setting that genuinely needs
    more gets an `MhFootnote` under the group, not a paragraph in the row.
- **Haptics.** Only on Connect accepted, session started, Stop, and confirmed
  destructive actions:
  `LocalView.current.performHapticFeedback(HapticFeedbackConstants.CONFIRM)`.
  No helper class.
- **Text.** Short, plain, rider words. Prefer "dashboard" over "T-Box" in
  rider-facing copy. Keep "T-Box" only where it names a technical setting.
  Every user-facing string goes through `motoHubText("literal")`, with the
  English text as a literal so the extractor can find it. Slice teams do not
  edit `translations/`; the orchestrator updates the catalogues.
