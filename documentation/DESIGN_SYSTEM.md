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
| `MotoHubColors.TextSecondaryOnFill` | `#B6B6BC` | secondary text that sits on `Fill`: field label and placeholder, neutral chip |
| `MotoHubColors.TextTertiary` | `#6E6E74` | disabled and decorative text only (3.9:1, never a label someone must read) |
| `primary` | `#C8F240` (lime) | the primary button, switch on, live status, checkmarks, focused field label and cursor |
| `onPrimary` | `#0A0A0B` | text and spinner on lime |
| `primaryContainer` | `#252A12` | the subtle tint behind a live chip |
| `error` (`MotoHubColors.ErrorText`) | `#FF9994` | red text: destructive labels, field errors |
| `MotoHubColors.Error` | `#FF5A52` | red icons, dots and the error chip |
| `errorContainer` | `#2B1513` | behind an error banner or chip |
| `MotoHubColors.Warning` / `WarningContainer` | `#FFB340` / `#2B2111` | cautions |
| `scrim` | `#000000` at 60% | behind sheets and dialogs |

Each surface step is about 1.16:1 above the one below, so cards hold up in
sunlight without outlines, and `onSurfaceVariant` stays at 4.5:1 or better on
every surface (7.6 on background, 6.5 surface, 5.4 high, 4.6 highest).

`Fill` is translucent on purpose: it always reads one step lighter than whatever
it sits on. An opaque grey vanished on the surface that shared its value (a
secondary pill on a sheet was 1:1). The worst case for text on it is `Fill` over
a sheet, so the text colours that sit on `Fill` are chosen for that case:

| Text on `Fill` | on background | on surface | on a sheet |
|---|---|---|---|
| `onSurface` | 13.5 | 10.8 | 8.7 |
| `TextSecondaryOnFill` | 7.3 | 5.8 | 4.7 |
| `ErrorText` (destructive pill) | 7.2 | 5.7 | 4.6 |
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
| `displaySmall` | 28 / 34 | Bold | screen title (large, left-aligned) |
| `headlineMedium` | 22 / 28 | Bold | sheet title, hero name |
| `titleLarge` | 20 / 26 | SemiBold | card hero lines |
| `titleMedium` | 16 / 22 | Medium | list row title (SemiBold for the compact title in the top bar) |
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
| `MhPrimaryButton` | Lime pill, 56 dp, full width (`fillWidth = false` to wrap). `loading` keeps it lime with a dark spinner. One per layer. |
| `MhSecondaryButton` | `Fill` pill, 56 dp, full width (`fillWidth = false` to wrap), light text. `destructive = true` gives red text. |
| `MhTextButton` | Text only, 48 dp target, for "Details", "Skip". Never for back or close. |
| `MhIconButton` | 48 dp icon-only button with a plain 24 dp glyph. |
| `MhIconCircle` | Neutral icon in a `Fill` circle, 40 dp in rows, 56 dp in heroes. |
| `MhListGroup` | Rounded `surface` card holding rows, no dividers. |
| `MhListRow` | Leading icon circle (optional), title, subtitle (never truncated), trailing value, chevron, or a control. |
| `MhSwitchRow` / `MhSwitch` | An on/off setting. The whole row toggles and is the one control TalkBack announces, with its state. |
| `MhChoiceRow` | One of several exclusive choices; a lime check marks the chosen one. Put all the choices in one group, or directly on a sheet. |
| `MhSectionHeader` | Sentence-case `titleSmall` in secondary colour above a group. A heading for TalkBack. |
| `MhFootnote` | The standard info line under a group: the one sentence a setting needs and its row has no room for. |
| `MhTopBar` | The 56 dp bar of every non-tab screen: back or close top-left, an optional compact centred title, trailing `MhTopBarAction`s. Pads the status bar itself. Use it alone where `MhScreen` can't be used (camera scanner, Android Auto preview); it only draws, so pair it with a `BackHandler`. |
| `MhTopBarAction` | A trailing top-bar action: icon in a 40 dp `Fill` circle, 48 dp target. |
| `MhScreen` | Sub-screen scaffold: `MhTopBar`, large title, scrolling content, optional sticky bottom action that rides above the keyboard. The compact title fades in once the large one has scrolled away. System back calls `onBack`. Without a bottom action, the scroll ends clear of the navigation bar and the keyboard. `scrollable = false` gives the content the remaining height instead, for a LazyColumn. |
| `MhTabPage` | A tab's own page: the same bar slot (actions only, no back) and large title as `MhScreen`, so titles line up. |
| `HubBottomNavigation` | The dock. See Navigation chrome. `rideLive` puts a lime dot on Ride. |
| `MhSheet` | `ModalBottomSheet` with grabber, title, short body, optional content, then primary and secondary pills stacked. `primaryStyle` is LIME, NEUTRAL or DESTRUCTIVE. See Sheets below. |
| `MhDialog` | The blocking dialog (P3). Same API and contract as `MhSheet`, with an optional icon; stacked full-width pills, scrolling body, back and outside taps ignored unless `dismissible`. |
| `MhActionStyle` | LIME (the layer's one action), NEUTRAL (consent and trust answers, P4), DESTRUCTIVE (red text on a `Fill` pill; fires the confirm haptic itself). |
| `MhModals` | `MhModals.open` counts the kit sheets and dialogs on screen. Startup prompts wait for zero. Only the kit writes it. |
| `MotoHubDialogBody` | The scrolling, edge-faded body slot for any `AlertDialog` (`MhDialog` uses it). |
| `MotoHubSnackbar` | App-level Material `SnackbarHost`, restyled: a dark rounded card (20 dp), leading icon, auto-dismiss, optional action in `onSurface`. Tap to dismiss. The newest message replaces the one on screen. Falls back to a system toast when the app is not in front. |
| `MhStatusChip` | Pill with a dot: Live (lime), Connecting (pulsing), Offline (grey), Action needed (warning). Only Connecting pulses. |
| `MhBanner` | Inline card for a failure or caution: icon, one-line title, one or two lines of body, one action, "Details" for the rest. `onDismiss` adds a 48 dp close icon in the title row. |
| `MotoHubNotice` | Only for long runtime instructions (the Android Auto failure steps) until the Ride slice folds it into `MhBanner`. |
| `MhTextField` | Filled field on `Fill`, 12 dp corners, label, helper or error line (read by TalkBack), password visibility toggle. `monospace` also turns off autocorrect and capitalisation. |
| `MhEmptyState` | Icon circle, title, one line, one action (with `actionIcon`, so it matches the same action elsewhere). |

Legacy components are `@Deprecated` with a `ReplaceWith`: `MotoHubDetailScreen`,
`MotoHubActionRow`, `ToggleRow`, `MotoHubRadioRow`, `MonoLabel`,
`HeroPrimaryAction`, `HeroTile`, `HeroOptionRow`. Each slice migrates the call
sites it owns; do not add new ones.

Icons are Material Symbols Rounded (`material-icons-extended`, Rounded set).
They replace the hand-drawn Canvas glyphs.

## Navigation chrome

**Back and close are uniform.**
- Every non-tab screen uses `MhScreen`. A screen that can't (the camera
  scanner, the Android Auto preview, a LazyColumn body) uses `MhTopBar`.
- Back (or X, for full-screen flows and modal pages) is always top-left, at the
  same position and size: a plain 24 dp glyph with a 48 dp target, the glyph
  16 dp from the screen edge.
- No "‹ Back", "Close" or "Back" text buttons, and no back or close on the
  right. System back always does what the icon does (P9).
- Trailing actions are `MhTopBarAction`s: 40 dp `Fill` circles, the last one
  16 dp from the edge.
- The large title sits under the bar. When it scrolls away, a compact centred
  title fades into the bar in 220 ms (the Revolut collapsing header).

**The dock is Revolut's.**
- Full width on `surface`, running on under the gesture bar. The kit pads the
  inset inside the dock; callers add nothing.
- 56 dp of items, 24 dp icons: filled when selected, outlined otherwise.
- `labelSmall` labels: selected `onSurface` SemiBold, the rest `onSurfaceVariant`.
- No indicator pill, no top border, a bounded ripple per item.
- `rideLive = true` draws a 6 dp lime dot at the top end of the Ride icon.

## Usage notes

- **Errors.**
  - The UI maps the raw failure (which logic and the sister app match by string
    equality, so it never changes) to a presentation type.
  - Every failure and caution is an `MhBanner` (P5): a title from the glossary,
    one helpful line, and one action that fixes it. On Ride that action is
    never "Try again", because the hero button is the retry.
  - The raw message sits behind "Details".
  - `MotoHubNotice` is only for the long Android Auto instructions.
- **Destructive confirm (P2).** A sheet titled "<Verb> <thing>?" that names the
  thing, a one-line consequence, a DESTRUCTIVE primary and "Cancel". On
  success, a past-tense snackbar:
  ```kotlin
  val bike = removing ?: return
  MhSheet(
      onDismiss = { removing = null },
      title = motoHubText("Remove %1$s?", bike.name),
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
  - A snackbar raised while a sheet or dialog is open draws underneath it
    (they are separate windows), so close the sheet first (P10): inside
    `content`, `close { MotoHubSnackbar.success(context, motoHubText("Log copied")) }`.
    An error while the sheet stays open goes inline in the sheet.
- **Dialogs.** `MhDialog` has the same contract as `MhSheet`. Consent and
  trust answers use `primaryStyle = NEUTRAL`: two equal pills, no lime (P4).
- **Settings.**
  - Every choice is a row in a group. Exclusive choices show a trailing
    checkmark in one group.
  - Long explanations are cut to one sentence. A setting that genuinely needs
    more gets an `MhFootnote` under the group, not a paragraph in the row.
- **Haptics.** Only on Connect accepted, session started, Stop, and confirmed
  destructive actions:
  `LocalView.current.performHapticFeedback(HapticFeedbackConstants.CONFIRM)`.
  The destructive one comes from the kit (`MhActionStyle.DESTRUCTIVE`); don't
  add it yourself. No helper class.
- **Text.** Short, plain, rider words, from the glossary below. Every
  user-facing string goes through `motoHubText("literal")`, with the English
  text as a literal so the extractor can find it. Identical strings share one
  translation key, so use the glossary strings exactly. Slice teams do not
  edit `translations/`; the orchestrator updates the catalogues.

## Patterns: the same situation always gets the same treatment

| # | Situation | Pattern |
|---|---|---|
| P1 | A choice or picker | An `MhSheet` with a title and an optional one-line body. Rows sit **directly on the sheet** (no inner `MhListGroup`) and align with the title. A single choice closes on tap. There are no OK or Cancel pills: swipe, back or scrim cancels. This covers Ride options, connection type, import QR, motorcycle photo, the action picker, timing and the teach prerequisite. |
| P2 | Destructive confirm | An `MhSheet` titled "<Verb> <thing>?" that names the thing, with a one-line consequence. The primary uses `primaryStyle = DESTRUCTIVE` (red text on a Fill pill) and fires the haptic itself. The secondary is "Cancel". On success, a past-tense snackbar. This covers Remove motorcycle, Reset actions and Clear the log. "Remove photo" is a red row that acts at once with a snackbar, because it is already inside a sheet. |
| P3 | Blocking | Only three things block. Safety is a full-screen page. Crash consent and the unverified QR use `MhDialog`. Everything else is a sheet, a snackbar or inline. |
| P4 | Consent, trust and data answers | **No lime**: two equal neutral pills. This covers crash consent, the report notice, the unverified QR and the wire verdict. |
| P5 | Inline problem | `MhBanner`: a title from the glossary, one body line, one action (the fix, never "Try again" on Ride, where the hero button is the retry), then "Details". The raw text is shown as is. |
| P6 | Lime | One lime-filled **action** per layer. A sheet or dialog is its own layer, and the scrimmed screen behind it does not count. Lime as *state* is allowed: switch on, check mark, live dot, progress, success icon. The selected tab is white. The snackbar action is not lime. |
| P7 | Setup entry points | The same three rows with the same strings and icons in Ride PAIRING, the Ride options sheet and Garage. Pairing success returns to the tab it was launched from and does not auto-connect. Every save path ends with "Motorcycle saved". |
| P8 | Developer reach | App-wide tools live in Settings › Developer tools (the last group, no header). Per-motorcycle tools live in Motorcycle details › Advanced. The simulator profile stays in a "Developer" group at the end of Dashboard profile. Application logs live in Diagnostics › Support. |
| P9 | Back | System back always does what the back icon does. Nested Settings states go to their parent. Sheets and full-screen pages dismiss. Scanner and wizard exit through their cleanup callback. |
| P10 | Feedback order | A snackbar is emitted only after the sheet that caused it has closed. An error while a sheet stays open goes inline in the sheet. |
| P11 | Motion | 220 ms. Ride's state swaps crossfade; navigation slides. Haptics fire only on the four DS moments, and destructive haptics come from the kit. |
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

### Shared actions
| String | Where | Replaces |
|---|---|---|
| Scan QR code (icon QrCodeScanner) | Ride PAIRING primary · Connection options · Garage empty primary · Garage "Add a motorcycle" | Scan new QR code, Scan motorcycle QR code |
| Import QR code · From a photo or screenshot (icon Image) | Ride ×2 · Garage · import sheet title | Import QR, Import QR from a photo, Import QR from an image |
| Enter details manually · Wi-Fi name and password (icon Keyboard) | Ride ×2 · Garage · scanner button · manual screen title | Connect manually, No QR? Manual setup, No QR code? Type… |
| Take a photo · Choose from gallery · Browse files (Downloads, cloud drives and other folders) · Remove photo | photo sheet; import sheet (minus camera and remove) | "Pick one of your photos." subtitle |
| Android Auto on this phone · No motorcycle needed | Ride options · Controls prerequisite | Start on this phone (no T-Box), No bike needed. Runs on this screen. |
| Connect · Connect to %1$s · Connection options · Disconnect · Stop streaming | Ride, Controls | |
| Try again | Ride lime label in ERROR · snackbar action | Retry, Retry connection, Try another image |
| Show me how | opens "Android Auto won't start" | How to start Android Auto |
| Open Wi-Fi settings · Open hotspot settings · Open VPN settings · Open app settings · Open %1$s app settings · Open accessibility settings · Open Android Auto settings | buttons that leave the app | Open App info, Open %1$s app info, Open %1$s settings, Turn on (HID) |
| Turn on | changes a setting in place | Turn presses back on |
| Cancel | abandons something the rider started | |
| Don't send | consent decline | Not now (crash, notice) |
| Not now | only a true "ask me later" | |
| Remove · Reset · Clear | destructive confirm button = the title's verb | Clear log |
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
- Seamless resume needs “Display over other apps”

**Raw, shown unchanged:** phone-only Android Auto failure messages.

**Rules:**
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
| Pre-release | NEUTRAL |
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
- Your phone stopped the last session
- Button presses are off
- Accessibility service is off
- Switch greyed out?
- Logging is off
- Couldn't save the motorcycle
- Couldn't install the update
- This release has no APK to install
- Did you mean “%1$s”? (NEUTRAL)

### Screen titles and section names
A row's title is always the title of the screen it opens.

| Area | Names |
|---|---|
| Tabs | Ride · Garage · Settings |
| Ride | Connect your motorcycle · Show on the dashboard · Connection options |
| Garage | Garage · Current motorcycle · Other motorcycles · Add a motorcycle |
| Motorcycle details | Android Auto · Connection · Advanced · Display fit · Screen margins · Dashboard profile · Dashboard capabilities |
| Settings headers | On the motorcycle · Connection · Help · App |
| Settings rows and screens | Video quality · Android Auto · Handlebar buttons · Start automatically · Auto-connect and recovery · Dashboard clock · Android Auto won't start · Diagnostics · Language · Check for updates on launch · About MOTO-HUB · MOTO-HUB ADV-SOLO · Developer tools |
| Settings subscreens | Resolution · Interface size · Button mapping · How your data is handled · Application logs |
| Section headers | Screen margins · Controls · Support · Privacy · Logging · Timing · What it adds · Before you switch · Community · Maps and data |

Developer-lab body copy stays verbatim, because its audience is developers.
