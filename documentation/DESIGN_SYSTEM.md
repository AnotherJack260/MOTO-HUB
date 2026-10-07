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

1. **One lime action per layer** (P6). A screen, a sheet and a dialog each get
   at most one lime-filled action. Every other action is secondary, tertiary,
   or a list row.
2. **Status over explanation.** Say what is happening in a few words. Put the why
   behind "Details", an info row, or the help guide. Never hide the one action
   that gets the rider unstuck.
3. **The lightest feedback that works.**
   - A finished action, or "couldn't open X", gets a snackbar.
   - A choice gets a bottom sheet.
   - Only three things block (P3): the safety acknowledgement (a full-screen
     page), crash-data consent and the unverified-QR warning (both `MhDialog`).
   - System permission prompts stay system dialogs.
4. **Riders first, developers second.** Diagnostics labs, protocol overrides and
   inspectors live under "Advanced" or "Developer tools", never in the main path.
5. **Glove-sized targets.** Primary buttons are 56 dp tall. Everything tappable
   is at least 48 dp. A compact pill (inside a banner or at the end of a row)
   draws 40 dp inside a 48 dp target.
6. **Purposeful motion.** Motion answers "what just changed?" or "did it hear
   me?", or it doesn't ship. An untouched screen is still; one change moves on
   one 220 ms clock; only a confirmation glyph overshoots; a live stream moves
   only when its state changes. See Motion below.

## Tokens

### Colour (dark only)

| Token | Value | Use |
|---|---|---|
| `background` | `#0A0A0B` | canvas, system bars, splash |
| `surface` | `#1C1C1E` | cards, list groups, empty states |
| `surfaceContainerHigh` | `#2C2C2E` | sheets, dialogs |
| `surfaceContainerHighest` | `#363638` | the snackbar pill, disabled primary button |
| `MotoHubColors.Fill` | white at 12% | controls on any surface: secondary buttons, icon circles, text fields, switch track off, sheet grabber, neutral and progress chips; a banner on a sheet or dialog |
| `outline` | `#363638` | the rare divider |
| `onSurface` | `#F5F5F7` | primary text, selected tab |
| `onSurfaceVariant` | `#A0A0A6` | secondary text, idle tab, inactive icons |
| `MotoHubColors.TextSecondaryOnFill` | `#B6B6BC` | secondary text that sits on `Fill`: field label and placeholder, neutral chip |
| `MotoHubColors.TextTertiary` | `#6E6E74` | disabled and decorative text only (3.9:1, never a label someone must read) |
| `primary` | `#C8F240` (lime) | the primary button, switch on, live status, checkmarks, focused field label and cursor |
| `onPrimary` | `#0A0A0B` | text and spinner on lime |
| `primaryContainer` | `#252A12` | the subtle tint behind a live chip |
| `error` (`MotoHubColors.ErrorText`) | `#FF9994` | red text: destructive labels, field errors |
| `MotoHubColors.Error` | `#FF5A52` | red icons, dots and the error chip |
| `errorContainer` | `#2B1513` | behind an error chip only (a banner is a plain card) |
| `MotoHubColors.Error` at 24% | translucent | the destructive confirm pill on a sheet or dialog (`MhActionStyle.DESTRUCTIVE`) |
| `MotoHubColors.Warning` / `WarningContainer` | `#FFB340` / `#2B2111` | cautions: the warning glyph, a row's warning subtitle / behind a warning chip only |
| `scrim` | `#000000` at 80% | behind sheets and dialogs (`MhDialog` sets its window dim to match). At 60% the lime action behind still read as an olive pill |

Each surface step is about 1.16:1 above the one below, so cards hold up in
sunlight without outlines, and `onSurfaceVariant` stays at 4.5:1 or better on
every surface (7.6 on background, 6.5 surface, 5.4 high, 4.6 highest).

`Fill` is translucent on purpose: it always reads one step lighter than whatever
it sits on. An opaque grey vanished on the surface that shared its value (a
secondary pill on a sheet was 1:1). The worst case for text on it is `Fill` over
a sheet, so the text colours that sit on `Fill` are chosen for that case. The
destructive confirm's red tint is translucent for the same reason: the opaque
`errorContainer` is darker than a sheet and read as a hole, not a button.

| Text on `Fill` | on background | on surface | on a sheet |
|---|---|---|---|
| `onSurface` | 13.5 | 10.8 | 8.7 |
| `TextSecondaryOnFill` | 7.3 | 5.8 | 4.7 |
| `ErrorText` (destructive pill) | 7.2 | 5.7 | 4.6 |
| `ErrorText` on the 24% destructive tint | | | 4.9 |
| `onSurfaceVariant` (do not use on `Fill`) | 5.7 | 4.5 | 3.6 |
| `Error` (icons only) | 4.8 | 3.8 | 3.1 |

A field's helper and error lines sit below the field, on the surface itself,
where `onSurfaceVariant` is 5.4:1 even on a sheet.

**Lime (P6).** One lime-filled *action* per layer. A sheet or dialog is its own
layer; the scrimmed screen behind it does not count. Lime as *state* is
allowed: switch on, check mark, live dot, progress, success icon, a focused
field's label. Otherwise:
- No per-feature colours.
- List icons are neutral: a light glyph in a `Fill` circle.
- A selection is shown with a lime checkmark, never with a glowing border.
- The selected tab is white, not lime. The snackbar action is not lime.
- Consent, trust and data answers get no lime at all (P4).

### Type

System sans-serif everywhere. Monospace is only for machine values the rider may
compare character by character: SSID, support ID, version, hex.

| Style | Size / line | Weight | Use |
|---|---|---|---|
| `displaySmall` | 32 / 38 | Bold, −0.5 | screen title (large, left-aligned, wraps; twice the row text), the Ride hero name |
| `headlineMedium` | 22 / 28 | Bold | sheet title |
| `titleLarge` | 20 / 26 | SemiBold | card hero lines |
| `titleMedium` | 16 / 22 | Medium | list row title (SemiBold for the compact title in the top bar) |
| `titleSmall` | 15 / 20 | SemiBold | snackbar text, small titles |
| section header | 17 / 22 | SemiBold | `MhSectionHeader`: `titleSmall` at 17 sp in `onSurface`, white, sentence case. SemiBold against the rows' Medium keeps the two apart |
| `bodyLarge` | 16 / 24 | Regular | paragraphs, sheet body |
| `bodyMedium` | 14 / 20 | Regular | row subtitle (up to 2 lines), helper text |
| `bodySmall` | 13 / 18 | Regular | captions, footnotes |
| `labelLarge` | 16 / 20 | SemiBold | buttons |
| `labelMedium` | 13 / 16 | Medium | chips, values |
| `labelSmall` | 12 / 16 | Medium | tab labels |

Rules:
- No ALL CAPS anywhere. Sentence case for titles, headers and buttons.
- The screen title, section headers and footnotes start on the card edge (the
  16 dp gutter), with no extra inset, and never truncate: a long translation
  wraps.
- Titles hyphenate. `displaySmall` and `headlineMedium` carry
  `hyphens = Hyphens.Auto` and `lineBreak = LineBreak.Heading` in the theme, so
  every screen title, hero name and sheet title breaks a long word (nl
  "Dashboardmogelijkheden", a rider's own motorcycle name) with a hyphen and
  balances its lines. Don't override either on a copy of those styles.
- No trailing full stops on titles.

### Shape, spacing, elevation

- **Radius.**
  - `small` 12: text fields
  - `medium` 16: tiles, tab press highlight
  - `large` 20: cards, list groups, banners (the snackbar is a full pill)
  - `extraLarge` 28: sheet top corners
  - Buttons and status chips are full pills: 56 dp, or 40 dp for a compact
    secondary pill.
  - Icon containers are circles: 40 dp in rows, 56 dp in an empty state.
- **Spacing.** 4 dp grid.
  - Screen gutter 16. Title, headers and footnotes sit on it too, not 4 dp in.
  - Card padding 16.
  - List row minimum height 64 (56 when there is no subtitle).
  - 24 between sections, 8 between a section header and its card.
- **Elevation.** None. Hierarchy comes from surface steps, not shadows.
- **States.** Pills and cards dip to 98% on press (the ripple still shows);
  rows, icon buttons and the dock keep the ripple alone. A switch that is off
  shows a full-size white thumb on `Fill`, as live a choice as on; only a
  disabled one greys its thumb. A disabled row fades to 45%;
  a disabled button turns grey with tertiary text. A loading button keeps its
  colours and shows a spinner in the label colour. A focused field shows a lime
  label (the cursor is lime too); no border.

## Components (`ui/components`)

| Component | What it is |
|---|---|
| `MhPrimaryButton` | Lime pill, 56 dp, full width (`fillWidth = false` to wrap). `loading` keeps it lime with a dark spinner. One per layer. Like every kit pill, it dips under the finger and ignores a second tap within 500 ms. |
| `MhSecondaryButton` | `Fill` pill, 56 dp, full width (`fillWidth = false` to wrap), light text. `destructive = true` gives red text. `size = MhButtonSize.COMPACT` draws 40 dp in a 48 dp target, 16 dp side padding, 15 sp label, and wraps its label by default: the action inside a banner, or a row's `trailing` action ("Use"). Never compact for a screen's or a sheet's own actions. |
| `MhTextButton` | Text only, 48 dp target, for "Cancel", "Skip", "Not now". Never for back or close. |
| `MhIconButton` | 48 dp icon-only button with a plain 24 dp glyph. |
| `MhIconCircle` | Neutral icon in a `Fill` circle, 40 dp in rows, 56 dp in an empty state. |
| `MhListGroup` | Rounded `surface` card holding rows, no dividers. |
| `MhListRow` | Leading icon circle (optional), title, subtitle (never truncated), trailing value, chevron, or a control (a compact `MhSecondaryButton` fits). `leading` draws instead of the icon circle, for a 40 dp avatar (a motorcycle's photo). The value is one short word or number, never a compound like "A · B": the column is 140 dp and two lines, and a translation breaks at the separator. A compound state belongs on the screen the row opens. A trailing slot holds one thing too; a status ("Pre-release") goes in the subtitle, not a chip beside the value. `subtitleColor = MotoHubColors.Warning` (with no value) flags a parent row whose screen has a broken prerequisite ("Accessibility service is off"). The chevron means "opens a screen": `showChevron` defaults to `!LocalMhInSheet.current`, so rows on a sheet have none. |
| `MhSwitchRow` / `MhSwitch` | An on/off setting. The whole row toggles and is the one control TalkBack announces, with its state. On: black thumb on lime. Off: full-size white thumb on `Fill` (Material's small grey dot read as disabled). |
| `MhChoiceRow` | One of several exclusive choices; a lime check marks the chosen one and pops in (`MhPop`) when it moves. Put all the choices in one group, or directly on a sheet. Takes `leading` like `MhListRow` (the Switch motorcycle sheet shows each photo). |
| `MhSectionHeader` | Sentence-case, white, 17 sp SemiBold above a group, on the card edge. A heading for TalkBack. |
| `MhFootnote` | The standard info line under a group, on the card edge: the one sentence a setting needs and its row has no room for. |
| `MhTopBar` | The 56 dp bar of every screen: back or close top-left (none on a tab page), an optional compact centred title, trailing `MhTopBarAction`s. Pads the status bar itself. Use it alone where `MhScreen` can't be used (camera scanner, Android Auto preview); it only draws, so pair it with a `BackHandler`. |
| `MhTopBarAction` | A trailing top-bar action: icon in a 40 dp `Fill` circle, 48 dp target. |
| `MhScreen` | Sub-screen scaffold: `MhTopBar`, large title, scrolling content, optional sticky bottom action that rides above the keyboard. The title collapses into the bar as it scrolls (see Navigation chrome). System back calls `onBack`. Without a bottom action, the scroll ends clear of the navigation bar and the keyboard. `scrollable = false` gives the content the remaining height instead, for a LazyColumn. `header` replaces the large title with something full-bleed (a photo) that starts at the top of the window under a floating bar: back sits in a 40 dp `Fill` circle over a dark wash, the header fades as it scrolls, and the bar turns solid as the compact `title` fades in. Draw the screen's name inside the header as a heading; `subtitle` isn't shown. Pass a `header` only when there is a real photo: never an empty media slab, so without one the screen keeps the standard large title, collapse and back glyph. The `bottomBar` reserves snackbar clearance. |
| `MhTabPage` | A tab's own page: the same bar slot (actions only, no back) and large title as `MhScreen`, so titles line up. Its title collapses into the bar the same way, so a scrolled Settings still says "Settings". |
| `HubBottomNavigation` | The dock. See Navigation chrome. `rideLive` puts a lime dot on Ride. |
| `MhSheet` | `ModalBottomSheet` with grabber, title, short body, optional content, then primary and secondary pills stacked. `primaryStyle` is LIME, NEUTRAL or DESTRUCTIVE. Provides `LocalMhInSheet = true` to its content. See Sheets below. |
| `LocalMhInSheet` | `true` inside an `MhSheet`'s or `MhDialog`'s content. `MhListRow` reads it to drop the chevron, `MhBanner` to sit on `Fill`; read it for any other "on a sheet" difference instead of adding a parameter. |
| `MhDialog` | The blocking dialog (P3). Same API and contract as `MhSheet`, with an optional icon; stacked full-width pills, scrolling body, back and outside taps ignored unless `dismissible`. Dims the screen behind to the `scrim` token (80%), like a sheet. |
| `MhActionStyle` | LIME (the layer's one action), NEUTRAL (consent and trust answers, P4), DESTRUCTIVE (red text on a red-tinted pill, `Error` at 24%, so the confirm outweighs the grey Cancel; fires the confirm haptic itself). |
| `MhModals` | `MhModals.open` counts the kit sheets and dialogs on screen. Startup prompts wait for zero. Only the kit writes it. |
| `MotoHubDialogBody` | The scrolling, edge-faded body slot for any `AlertDialog` (`MhDialog` uses it). |
| `MotoHubSnackbar` | App-level Material `SnackbarHost`, restyled: a centred pill as wide as its words (min 52 dp tall, 26 dp radius, `SurfaceHighest`), a 20 dp leading icon, `titleSmall` text that wraps and is never cut, auto-dismiss, optional action in `onSurface`. Tap to dismiss. The newest message replaces the one on screen. Falls back to a system toast when the app is not in front. A success message's check pops in. The host keeps messages above `bottomClearance`. |
| `Modifier.reserveSnackbarClearance()` / `MotoHubSnackbar.bottomClearance` | A pinned bottom action reports its height (inside the navigation-bar and keyboard insets) while it is on screen; `bottomClearance` is the tallest one, or 0. `MhScreen`'s `bottomBar` already does it; Ride's pinned slot applies the modifier itself. |
| `MhStatusChip` | Pill with a dot: Live (lime), Connecting (pulsing), Offline (grey), Action needed (warning). Only Connecting pulses; with animations off the dot rests fully lit. A change sweeps the colours, crossfades the word and lets the width follow, on the 220 ms clock. |
| `MhMotion` | The motion tokens (see Motion) and `fadeThrough(animateHeight)`, `foldIn` / `foldOut`. |
| `Modifier.mhPressable(shape, enabled, role, onClick)` | Clickable, ripple and the 98% dip, clipped to `shape`: a custom card that acts like a pill (the Garage card, the Ride hero). First in the chain, before the background. |
| `MhPop(visible) { }` | A confirmation glyph that pops in (0.8 to 1 on `pop()`, with a fade) when `visible` turns true, and fades out. No pop on first composition. Give it a fixed-size parent. |
| `ScreenCrossfade` | Swaps a screen's content with `MhMotion.fadeThrough`, keeping each one's saved state. `animateHeight = true` makes the height follow on the same clock (Ride's states, Garage); leave it off for full-screen swaps (the tabs). `ScreenSlideTransition` / `HubScreenTransition` are the navigation slides. |
| `MhBanner` | Inline card for a failure or caution: icon, one-line title, one or two lines of body, one action (a compact pill, so the banner never outweighs the screen's lime button), "Details" for the rest. A plain `surface` card like the groups around it (`Fill` on a sheet or dialog); the colour is only on its filled 22 dp glyph: Error red, Warning amber, Info grey. No tinted slab. "Details" is plain text in a 48 dp target that starts on the body's line. The action row is a `FlowRow`: "Details" sits 20 dp after the pill and drops under it, onto the body's line, when the two don't fit; it never wraps. An action label is at most 26 characters in every language, so the pill stays one line; a translation may use a noun phrase ("Battery settings") to fit. An action inside "Details" is compact too, never a 56 dp pill. `onDismiss` adds a 48 dp close icon in the title row. |
| `MotoHubNotice` | Only for long runtime instructions (the Android Auto failure steps) until the Ride slice folds it into `MhBanner`. A plain `surface` card in every tone; the tone is the label's colour. |
| `MhTextField` | Filled field on `Fill`, 12 dp corners, label, helper or error line (read by TalkBack), password visibility toggle. `monospace` also turns off autocorrect and capitalisation. `placeholder` is what an empty field stands for ("My motorcycle"): grey under the label, focused or not, while the value stays empty. While focused with text, a clear (×) button empties it; a password field keeps its eye instead. |
| `MhEmptyState` | Icon circle, title, one line, one action (with `actionIcon`, so it matches the same action elsewhere). |

Legacy components are `@Deprecated` with a `ReplaceWith`: `MotoHubDetailScreen`,
`MotoHubActionRow`, `ToggleRow`, `MotoHubRadioRow`, `MonoLabel`,
`HeroPrimaryAction`, `HeroTile`, `HeroOptionRow`. Each slice migrates the call
sites it owns; do not add new ones.

Icons are Material Symbols Rounded (`material-icons-extended`, Rounded set).
They replace the hand-drawn Canvas glyphs.

There is no `MhSlider` yet: the one slider (Music volume) is Material's. When a
second slider appears, add `MhSlider` (24 dp white round thumb, 4 dp track, no
stop indicator) and move both to it.

## Navigation chrome

**Back and close are uniform.**
- Every non-tab screen uses `MhScreen`. A screen that can't (the camera
  scanner, the Android Auto preview, a LazyColumn body) uses `MhTopBar`.
- Back (or X, for full-screen flows and modal pages) is always top-left, at the
  same position and size: a plain 24 dp glyph with a 48 dp target, the glyph
  16 dp from the screen edge.
- No "‹ Back", "Close" or "Back" text buttons, and no back or close on the
  right. System back always does what the icon does (P9).
- Predictive back is on (`android:enableOnBackInvokedCallback="true"`): the
  back-to-home preview on the tab roots and Material's back gesture on sheets.
  Handle back only with Compose's `BackHandler`; never override `onBackPressed`
  or listen for `KEYCODE_BACK`, which Android 13+ no longer delivers.
- Trailing actions are `MhTopBarAction`s: 40 dp `Fill` circles, the last one
  16 dp from the edge.
- The large title sits under the bar, on `MhScreen` and `MhTabPage` alike. As it
  scrolls under the bar it fades out, so no half-cut glyphs hang there, and a
  compact centred title fades into the bar over the last 24 dp before it is
  gone (the Revolut collapsing header). Both follow the scroll, not a timer.

**The dock is Revolut's.**
- It shows only on the three tab roots. Every pushed screen covers it.
- Full width on `surface`, running on under the gesture bar. The kit pads the
  inset inside the dock; callers add nothing.
- 56 dp of items, 24 dp icons: filled when selected, outlined otherwise.
  Garage uses the Warehouse glyph: Material's Garage has a car parked in it,
  and none has a motorcycle.
- `labelSmall` labels: selected `onSurface` SemiBold, the rest `onSurfaceVariant`.
- No indicator pill, no top border, a bounded ripple per item.
- `rideLive = true` draws a 6 dp lime dot at the top end of the Ride icon. It
  fades in and out (`FAST`); the icon swap itself snaps, as Revolut's does.

## Motion

Motion explains a change; it never decorates. Every effect must answer "what
just changed?" or "did it hear me?". The tokens live in `MhMotion`
(`ui/components/MhMotion.kt`); don't write a duration or an easing by hand.

| Token | Value | Use |
|---|---|---|
| `MhMotion.FAST` | 120 ms | exits, icon and label swaps, small colour changes |
| `MhMotion.BASE` | 220 ms | state swaps, heights, expand and collapse, screen slides (`MOTION_MILLIS` is the same value) |
| `Standard` / `Enter` / `Exit` | M3 standard / emphasized decelerate / emphasized accelerate | changing in place / arriving / leaving |
| `press()` | spring, damping 1, stiffness 1500 | the press dip and its release |
| `pop()` | spring, damping 0.6, stiffness 800 (about 9% overshoot) | confirmation glyphs only |
| `fadeThrough(animateHeight)` | old out in 80 ms, new in over 160 ms from 60 ms; height on a 220 ms `Standard` tween, or not animated | a state swap in place (`ScreenCrossfade`, `AnimatedContent`) |
| `foldIn` / `foldOut` | fade plus grow from the top edge on `BASE`; out: fade `FAST`, shrink `BASE` | anything folding open under what is above it: banner details, a banner arriving or leaving, a footnote. Keep the last non-null value for the exit |
| press scale | 98% | pills (the kit does it) and cards (`mhPressable`); never rows, icons or the dock |

**Rules.**
- **Acknowledge the touch on the frame it lands.** Pills and cards dip; rows
  ripple. The dip is the tap's feedback, so a tap on its own never vibrates.
- **The glove guard.** Every kit pill ignores a second tap within 500 ms of the
  one it let through (sheet and dialog actions included). It is a time window,
  not a "busy" flag, so nothing has to clear it. A custom tappable that starts
  something (Connect, Stop, Save) uses a kit pill rather than its own button.
- **One change, one clock.** Everything that moves for one state change shares
  `BASE` and one easing, heights included. That is why heights are 220 ms
  tweens and never the default spring.
- **Never slower to use.** Nothing blocks input while it animates and nothing
  runs past 220 ms. The only dwells are the QR "got it" hold (250 ms) and the
  teach wizard's (700 ms), and both carry information.
- **State swaps fade through; navigation slides.** A pushed screen slides in
  over a still, dimmed base, with no parallax. Nothing slides that isn't a
  screen.
- **Overshoot only on confirmation glyphs** (`MhPop`): a check, a success icon.
  Layout, sheets, cards, text and navigation never bounce.
- **Read continuous values in the draw phase** (`graphicsLayer {}`,
  `drawBehind {}`), never as composition parameters. The status chip's word
  colour is the one accepted exception (220 ms per change).
- **Still while streaming.** In Mirroring the phone screen is the video, and in
  Android Auto a running animation holds the display at its high refresh rate.
  A live session moves only when its state changes.
- **Animations off means instant, never broken.** Compose follows the system
  animator scale, so most effects need nothing. An infinite transition jumps
  to its *target* at scale 0, so a loop's target must be its resting look (the
  chip's dot pulses 0.25 to 1, not 1 to 0.25). An effect that must take a
  different path when animations are off (a shake) checks
  `ValueAnimator.areAnimatorsEnabled()` when it starts; add a `reducedMotion()`
  helper to the kit with its first caller. Haptics stay.

**Never animate.**
- Anything on a live session except a state change, and nothing at the moment
  streaming starts. Nothing painted into the streamed picture
  (`HandlebarPressHud`, the dashboard renderer); no animating overlay on the
  Android Auto preview.
- Loops beyond real progress: the PROGRESS chip dot and Material's spinner are
  the only ones. No breathing live dot, viewfinder pulse, shimmer or skeleton.
- Entrances: screens, tabs, empty states and lists don't animate in. No
  stagger, no hero zoom.
- Bounce or overshoot outside confirmation glyphs; parallax.
- Rolling numbers, row values changing, the collapsing title sliding, the dock
  icons (no crossfade, no bounce).
- Shared-element morphs (pill to steps, mode row to session card).
- A global `animateContentSize` on `MhSheet`: animate only known, bounded swaps
  inside a sheet.
- Not built until there is a caller: a text-field shake (with REJECT, skipped
  when animations are off), swipe-to-dismiss on the snackbar, a custom in-app
  predictive back.

## Usage notes

- **Errors.**
  - The UI maps the raw failure (which logic and the sister app match by string
    equality, so it never changes) to a presentation type.
  - Every failure and caution is an `MhBanner` (P5): a title from the glossary,
    one helpful line, and one action that fixes it, as a compact pill. On Ride that action is
    never "Try again", because the pinned lime button is the retry.
  - The raw message sits behind "Details".
  - `MotoHubNotice` is only for the long Android Auto instructions.
- **Destructive confirm (P2).** A sheet titled "<Verb> <thing>?" that names the
  thing, a one-line consequence, a DESTRUCTIVE primary (red on a red tint, so
  it outweighs Cancel) and "Cancel". On
  success, a past-tense snackbar:
  ```kotlin
  val bike = removing ?: return
  MhSheet(
      onDismiss = { removing = null },
      title = motoHubText("Remove “%1$s”?", bike.name),
      body = motoHubText("Its QR code and photo are deleted from this phone."),
      primaryLabel = motoHubText("Remove"),
      onPrimary = { garage.remove(bike) },
      secondaryLabel = motoHubText("Cancel"),
      primaryStyle = MhActionStyle.DESTRUCTIVE
  )
  ```
- **Sheets.**
  - `onDismiss` means the sheet is gone, by any path (scrim, back, swipe, any
    button), and runs once. It only clears your `showX` flag. Put Cancel's own
    logic in `onSecondary`.
  - Actions run after the sheet has slid away and after `onDismiss`. So an
    action captures what it needs in a local `val` first (as `bike` above), and
    never reads state that its `onDismiss` clears.
  - The content slot is inset 4 dp, so `MhListRow` and `MhChoiceRow` line up
    with the 20 dp title. Rows sit directly on the sheet, with no inner group
    (P1). Anything else in the slot pads itself 16 dp.
  - A row on a sheet acts (picks, imports, removes); it never navigates, so it
    has no chevron. The kit drops it (`LocalMhInSheet`), so don't pass
    `showChevron = false` there.
  - A snackbar never covers the screen's pinned action. `MhScreen`'s
    `bottomBar` reports its height; anything else pinned to the bottom (Ride's
    action slot) applies `Modifier.reserveSnackbarClearance()` inside the
    navigation-bar insets. The host (`MotoHubSnackbar.Host`) adds
    `bottomClearance` itself, so its caller pads only for the dock: 72 dp while
    the dock shows, 16 dp on a pushed screen, plus the navigation bar.
  - A snackbar raised while a sheet or dialog is open draws underneath it
    (they are separate windows), so close the sheet first (P10): inside
    `content`, `close { MotoHubSnackbar.success(context, motoHubText("Log copied")) }`.
    An error while the sheet stays open goes inline in the sheet.
- **Dialogs.** `MhDialog` has the same contract as `MhSheet`. Consent and
  trust answers use `primaryStyle = NEUTRAL`: two equal pills, no lime (P4).
- **Fields.** An empty field that stands for a default (an unnamed motorcycle is
  "My motorcycle") shows that default as `placeholder`, never a bare label.
- **Motorcycle photos.** `MotorcyclePhoto` crops the photo to its shape.
  - Without a photo, an avatar (40 dp in a row or the Switch motorcycle sheet)
    shows `TwoWheeler` on `Fill` at the icon circle's 0.55 glyph ratio, the
    same motorcycle everywhere.
  - `AddAPhoto` appears only where a tap adds a photo: the "Add photo" row in
    Motorcycle details and in the Ride hero's "Motorcycle options" sheet. Both
    open the same photo sheet (`MotorcyclePhotoSheet`).
- **Ride.** One composition in every state: hero card, then the state's content,
  then the pinned action slot above the dock.
  - The hero is a 208 dp card (it grows only for a name that wraps) in the 16 dp
    gutter, 20 dp corners. With a photo: the photo edge to edge, cropped, under a
    vertical black scrim from 15% to 75%. Without one: a tonal gradient from
    `SurfaceHigh` (top-left) to `background` and a 120 dp `TwoWheeler`
    watermark at 8% white bleeding off the end. A restrained lime radial bloom
    is added only while connected or live; offline stays neutral, so lime never
    claims a link that isn't there. Bottom-left, 16 dp in: the name in
    `displaySmall` (wraps, never cut), then the status chip and the monospaced
    Wi-Fi name. With no motorcycle the same card says "Connect your
    motorcycle" and one line of guidance, with no chip.
  - At rest and when connected the hero is a button (`mhPressable`, a "more"
    glyph top-right) that opens the "Motorcycle options" sheet (P1): "Switch
    motorcycle" (only with two or more saved), "Add photo" / "Change photo"
    (the photo sheet) and "Motorcycle details". Connecting or streaming it does
    nothing and the glyph fades out.
  - Between the hero and the slot: the failure banner right under the hero,
    the delivery warning, the system-kill notice (at rest only), then the
    state's content (the two setup rows with no motorcycle, the timeline, the
    mode rows, the session controls) and the promo (rest only, P12).
  - The pinned slot keeps one place for the thing to press, with snackbar
    clearance and the 400 ms tap guard: lime "Scan QR code" with no
    motorcycle; at rest and after a failure lime "Connect" / "Try again" with
    a neutral "Connection options" pill directly under it (the P7 sheet);
    "Cancel" while connecting, "Disconnect" in mode selection, "Stop
    streaming" (red text) while live. The slot's height follows its content on
    the 220 ms clock.
- **Settings.**
  - Every choice is a row in a group. Exclusive choices show a trailing
    checkmark in one group.
  - Long explanations are cut to one sentence. A setting that genuinely needs
    more gets an `MhFootnote` under the group, not a paragraph in the row.
- **Haptics.** A haptic means the motorcycle (or the system) answered, not
  that a finger touched glass. Call
  `LocalView.current.performHapticFeedback(...)` directly; no helper class.
  - `CONFIRM`, it worked: connected (CONNECTING to mode selection or a
    session), picture on the dashboard, Stop (on tap), a destructive action
    confirmed, a QR code read, a teach press captured. The destructive one
    comes from the kit (`MhActionStyle.DESTRUCTIVE`); don't add it yourself.
  - `REJECT`, it failed: a connection failed (once per attempt), a wrong QR
    code (once per distinct code, at most every 2 s), the teach wizard got the
    wrong press (once per gesture).
  - Never: taps on their own, navigation, tabs, toggles, choices, copies,
    scrolling, "Action needed".
- **Text.** Short, plain, rider words, from the glossary below. Every
  user-facing string goes through `motoHubText("literal")`, with the English
  text as a literal so the extractor can find it. Identical strings share one
  translation key, so use the glossary strings exactly. Slice teams do not
  edit `translations/`; the orchestrator updates the catalogues.

## Patterns: the same situation always gets the same treatment

| # | Situation | Pattern |
|---|---|---|
| P1 | A choice or picker | An `MhSheet` with a title and an optional one-line body. Rows sit **directly on the sheet** (no inner `MhListGroup`) and align with the title, with no chevron. A single choice closes on tap. There are no OK or Cancel pills: swipe, back or scrim cancels. This covers Connection options, Motorcycle options, connection type, import QR, motorcycle photo, the action picker, timing and the teach prerequisite. |
| P2 | Destructive confirm | An `MhSheet` titled "<Verb> <thing>?" that names the thing, with a one-line consequence. The primary uses `primaryStyle = DESTRUCTIVE` (red text on a red-tinted pill, heavier than the grey Cancel) and fires the haptic itself. The secondary is "Cancel". On success, a past-tense snackbar. This covers Remove motorcycle, Reset actions and Clear the log. "Remove photo" is a red row that acts at once with a snackbar, because it is already inside a sheet. |
| P3 | Blocking | Only three things block. Safety is a full-screen page. Crash consent and the unverified QR use `MhDialog`. Everything else is a sheet, a snackbar or inline. |
| P4 | Consent, trust and data answers | **No lime**: two equal neutral pills. This covers crash consent, the report notice, the unverified QR and the wire verdict. |
| P5 | Inline problem | `MhBanner`: a title from the glossary, one body line, one compact action (the fix, never "Try again" on Ride, where the pinned lime button is the retry), then "Details". The raw text is shown as is. |
| P6 | Lime | One lime-filled **action** per layer. A sheet or dialog is its own layer, and the scrimmed screen behind it does not count. Lime as *state* is allowed: switch on, check mark, live dot, progress, success icon. The selected tab is white. The snackbar action is not lime. |
| P7 | Setup entry points | The same three rows with the same strings and icons in Ride PAIRING (lime "Scan QR code" plus two rows), the Ride "Connection options" sheet (the neutral pill under Connect) and Garage. Pairing success returns to the tab it was launched from and does not auto-connect. Every save path ends with "Motorcycle saved". |
| P8 | Developer reach | App-wide tools live in Settings › Developer tools (the last group, no header). Per-motorcycle tools live in Motorcycle details › Advanced. The simulator profile stays in a "Developer" group at the end of Dashboard profile, reachable in every build (owner decision: dev tools stay reachable). Application logs live in Diagnostics › Support. |
| P9 | Back | System back always does what the back icon does. Nested Settings states go to their parent. Sheets and full-screen pages dismiss. Scanner and wizard exit through their cleanup callback. |
| P10 | Feedback order | A snackbar is emitted only after the sheet that caused it has closed. An error while a sheet stays open goes inline in the sheet. |
| P11 | Motion | 220 ms on one clock, heights included. State swaps fade through (`ScreenCrossfade`, `MhMotion.fadeThrough`); navigation slides. Pills and cards dip on press. Haptics: CONFIRM = it worked (connected, picture on the dashboard, Stop, destructive confirmed, QR read, press captured); REJECT = it failed (connection failed, wrong QR, wrong press). Nothing else vibrates. See Motion. |
| P12 | Promo | One `AdvancedPromoRow` (owned by Overlays) on Ride PAIRING, Ride CONNECTION and Settings › App. Nowhere during connecting or riding. No red. |

## Copy glossary (canonical English)

### Vocabulary rules
| Use | Never (in rider copy) | Notes |
|---|---|---|
| **motorcycle** | bike, moto | Applies everywhere: titles, rows, bodies, buttons and snackbars. |
| **dashboard** | dash, TFT, T-Box, head unit | "T-Box" is allowed only in Developer tools, inspector rows and raw strings kept verbatim. |
| **QR code** | pairing code, QR (bare), T-Box QR | |
| **Wi-Fi name** | SSID, network name | The manual form label is "Wi-Fi name (SSID)". |
| **streaming** | session, projection | "Stop streaming", "Stop streaming first". |
| **Mirroring / Android Auto / External display** | Mirror, Auto, External | These are the mode names (nouns) everywhere. |
| **app settings** | App info, app info | Android's per-app page. |
| **report** | diagnostics (as a verb object), log (for the upload) | The screen name "Diagnostics" stays, because legal text cites it. |
| **Media controls / Bluetooth keyboard** | Media keys, Keyboard remote, AVRCP, HID | The two Handlebar buttons types. "Bluetooth keyboard" is for remotes paired as a Bluetooth keyboard. |
| **and** | & | |

### Form rules
- Sentence case everywhere.
- **No trailing full stop** on titles, section headers, row titles, row subtitles, buttons, chips or snackbars.
- **Full stops** on footnotes and on sheet, dialog and banner bodies.
- A subtitle that needs two sentences is too long: move the second sentence to a footnote.
- Phrase patterns:
  - failure: "Couldn't <verb> <object>"
  - state: "<Thing> is off"
  - permission: "Allow <thing>"
  - confirm title: "<Verb> <object>?", with the button being the bare verb
  - success: "<Object> <past participle>"
- Progress text uses the single character "…".
- Curly quotes “ ” go only around a typed or unknown name, or a setting's name, inside a sentence.
- Placeholders are `%1$s` / `%1$d`. Never build a sentence from fragments.
- The Language list names each language in its own script with no region: "한국어", not "한국어 (대한민국)".

### Shared actions
| String | Where | Replaces |
|---|---|---|
| Scan QR code (icon QrCodeScanner) | Ride PAIRING primary · Connection options · Garage empty primary · Garage "Add a motorcycle" | Scan new QR code, Scan motorcycle QR code |
| Import QR code · From a photo or screenshot (icon Image) | Ride ×2 · Garage · import sheet title | Import QR, Import QR from a photo, Import QR from an image |
| Enter details manually · Wi-Fi name and password (icon Keyboard) | Ride ×2 · Garage · scanner button · manual screen title | Connect manually, No QR? Manual setup, No QR code? Type… |
| Take a photo · Choose from gallery · Browse files (Downloads, cloud drives and other folders) · Remove photo | photo sheet; import sheet (minus camera and remove) | "Pick one of your photos." subtitle |
| Android Auto on this phone · No motorcycle needed | Connection options · Controls prerequisite | Start on this phone (no T-Box), No bike needed. Runs on this screen. |
| Connect · Connect to %1$s · Connection options · Disconnect · Stop streaming | Ride, Controls | |
| Try again | Ride lime label in ERROR · snackbar action | Retry, Retry connection, Try another image |
| Show me how | opens "Android Auto won't start" | How to start Android Auto |
| Open Wi-Fi settings · Open hotspot settings · Open VPN settings · Open app settings · Open %1$s app settings · Open accessibility settings · Open Android Auto settings | buttons that leave the app | Open App info, Open %1$s app info, Open %1$s settings, Turn on (HID) |
| Turn on | changes a setting in place | Turn presses back on |
| Cancel | abandons something the rider started | |
| Don't send | consent decline | Not now (crash, notice) |
| Not now | only a true "ask me later" | |
| Remove · Reset · Clear | destructive confirm button = the title's verb | |
| Clear log | red row after the log in Application logs; opens "Clear the log?" | a red "Clear" pill above the log |
| Details · Hide details | MhBanner | |
| Save · Done · Got it · I understand · Use · Skip · Skip this version | | Finish, I understand and continue |
| Send report · Send reports · Send a report now · Send reports automatically | crash · notice · Diagnostics row · switches (crash, Diagnostics, trial keep sheet) | Send diagnostics now/automatically, Send my log now, Send logs automatically from now on |

### Snackbars
**Success:**
- Motorcycle saved
- Motorcycle removed
- Photo removed
- Actions reset
- Log cleared
- Log copied
- Support ID copied
- Report sent
- You have the latest version
- Prototype unlocked (K)
- Stopped · %1$s · %2$s (Ride-end receipt: the mode, then "<1 min", "%d min" or "%d h %d min")

**Error:**
- Couldn't open Wi-Fi settings / hotspot settings / VPN settings / app settings / Android Auto / GitHub / Discord / ADV-SOLO / the browser
- Couldn't save changes
- Couldn't save the motorcycle
- Couldn't save the photo
- Couldn't remove the motorcycle
- Couldn't switch motorcycles
- Couldn't create the log file
- Couldn't send the report
- Couldn't check for updates [Try again]
- Couldn't install the update
- Stop streaming first
- Allow the camera to scan QR codes [Settings]
- Allow the camera to take a photo [Settings]
- Allow the microphone to start Android Auto [Settings]
- Seamless resume needs “Display over other apps”

**Raw, shown unchanged:** phone-only Android Auto failure messages.

**Rules:**
- "Support ID copied" and "Log copied" show on Android 12L and lower only. On
  13+ the system shows its own clipboard confirmation, and a second one
  repeats it.
- A snackbar action is one or two words: "Try again", "Settings".
- The action text is onSurface, not lime.

### Chips (MhStatusChip)
| Chip | Tone |
|---|---|
| Not connected | NEUTRAL |
| Connecting | PROGRESS |
| Connected | LIVE |
| Live | LIVE |
| Starting | PROGRESS |
| Action needed | WARNING |
| Stopped (Android Auto preview) | NEUTRAL |
| Failed | ERROR |
| Installed | NEUTRAL |
| Listening | PROGRESS |
| Got it | LIVE |
| Passed / Failed / Running / Skipped / Not run (labs) | LIVE / ERROR / PROGRESS / NEUTRAL / NEUTRAL |

### Banner titles (MhBanner)
- Wi-Fi is off
- Hotspot is off
- Android Auto needs a setting
- A VPN is blocking the dashboard
- %1$s is using the dashboard / Another app is using the dashboard
- Couldn't join the dashboard's Wi-Fi
- Dashboard not found
- Allow nearby devices
- Allow notifications
- Connection failed
- Your dashboard isn't showing the picture
- Your phone closed MOTO-HUB
- Button presses are off
- Accessibility service is off
- Switch greyed out?
- Logging is off
- Couldn't save the motorcycle
- Couldn't install the update
- This release has no APK to install
- Did you mean “%1$s”? (NEUTRAL)

### Row warnings (MhListRow subtitle in Warning)
A parent row whose screen has a broken prerequisite says what is off, with no
value, using the state pattern:
- Accessibility service is off
- Bluetooth access is off

### Screen titles and section names
A row's title is always the title of the screen it opens.

| Area | Names |
|---|---|
| Tabs | Ride · Garage · Settings |
| Ride | Connect your motorcycle · Show on the dashboard · Connection options · Motorcycle options (sheet: Switch motorcycle · Add photo / Change photo · Motorcycle details) · Switch motorcycle (sheet) · Allow notifications / Allow the microphone / Allow notifications and the microphone (permission sheet before the first stream, rows "Notifications · Keep streaming visible and let you stop it" and "Microphone · For Google Assistant and calls") |
| Garage | Garage · Current motorcycle · Other motorcycles · Add a motorcycle |
| Motorcycle details | Name (row and its sheet) · Android Auto · Connection · Advanced · Display fit · Screen margins · Dashboard profile · Dashboard capabilities |
| Settings headers | On the motorcycle · Connection · Help · App |
| Settings rows and screens | Video quality · Android Auto · Handlebar buttons · Start automatically · Auto-connect and recovery · Dashboard clock · Android Auto won't start · Diagnostics · Language · Check for updates on launch · About MOTO-HUB · MOTO-HUB ADV-SOLO · Developer tools |
| Settings subscreens | Resolution · Interface size · Button mapping · How your data is handled · Application logs · Legal (About) |
| Video quality choices | Picture: Lighter · Balanced · Sharper (not "Smoother": a frame-rate subtitle already says "Smoothest"). Frame rate: Auto · 30 fps · 24 fps · 20 fps |
| Button type choices | Media controls (Most dashboards) · Bluetooth keyboard (For remotes paired as a Bluetooth keyboard) |
| Section headers | Screen margins · Controls · Support · Privacy · Logging · Timing · What it adds · Before you switch · Community · Maps and data · Frame rate (Video quality) · Button type (Handlebar buttons) |

Developer-lab body copy stays verbatim, because its audience is developers.
