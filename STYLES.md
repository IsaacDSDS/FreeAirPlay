# AirCast TV — Styles

Single source of truth for the receiver UI (Android TV, 10-foot experience, 1920×1080 @ 320 dpi → 960×540 dp).
Everything below is defined in `app/src/main/res/values/` — reuse these resources instead of hard-coding values.

## Colors (`values/colors.xml`)

The app is dark-only (TV screens, dimmed rooms, video letterboxing must blend in).

| Token | Value | Use |
|---|---|---|
| `bg` | `#0B0B0F` | Window and idle-screen background; video letterbox |
| `surface` | `#17171D` | Cards (instructions, now playing) |
| `surface_elevated` | `#22222A` | Step-number badges, progress track |
| `outline` | `#2E2E38` | 1 dp card stroke |
| `text_primary` | `#F5F5F7` | Titles, primary text |
| `text_secondary` | `#A1A1AA` | Supporting text |
| `text_tertiary` | `#6B6B76` | Footnotes (IP / port) |
| `accent` | `#0A84FF` | Brand icon, progress, focus, active pill dot |
| `accent_soft` | `#260A84FF` | Pill background when active |
| `success` | `#30D158` | "Ready" dot |
| `warning` | `#FF9F0A` | "Starting" dot |
| `error` | `#FF453A` | Error dot / message |
| `scrim` | `#B30B0B0F` | Overlays drawn on top of video (connection banner) |

## Typography (`values/styles.xml`, `TextAppearance.AirCast.*`)

System Roboto via font families; sizes are tuned for viewing from a couch.

| Style | Family | Size | Color |
|---|---|---|---|
| `TextAppearance.AirCast.Display` | `sans-serif-medium` | 44sp | `text_primary` |
| `TextAppearance.AirCast.Headline` | `sans-serif-medium` | 28sp | `text_primary` |
| `TextAppearance.AirCast.Title` | `sans-serif-medium` | 22sp | `text_primary` |
| `TextAppearance.AirCast.Body` | `sans-serif` | 20sp | `text_secondary` |
| `TextAppearance.AirCast.Caption` | `sans-serif` | 16sp | `text_tertiary` |
| `TextAppearance.AirCast.Label` | `sans-serif-medium` | 16sp | `text_primary` |

## Spacing & shape (`values/dimens.xml`)

- 4 dp grid: `space_xs` 4, `space_s` 8, `space_m` 16, `space_l` 24, `space_xl` 32, `space_xxl` 48.
- TV overscan safe area: `safe_horizontal` 48 dp, `safe_vertical` 27 dp — apply to every full-screen layout root.
- Radii: `radius_card` 20 dp, `radius_artwork` 16 dp, `radius_pill` 24 dp.
- Brand icon: `icon_hero` 96 dp; artwork: `artwork_size` 280 dp.

## Components

| Component | Where | Notes |
|---|---|---|
| `Widget.AirCast.Card` | `styles.xml` + `drawable/bg_card.xml` | `surface` fill, 1 dp `outline`, `radius_card`, `space_xl` padding |
| `Widget.AirCast.StatusPill` | `styles.xml` + `drawable/bg_pill.xml` | `accent_soft` fill, `Label` text; leading dot via `drawable/dot_status.xml` tinted with `success` / `warning` / `error` / `accent` |
| `Widget.AirCast.StepBadge` | `styles.xml` + `drawable/bg_step_badge.xml` | 32 dp circle, `surface_elevated`, `Label` text — numbered instruction steps |
| `Widget.AirCast.Progress` | `styles.xml` | Horizontal `ProgressBar`, 4 dp, `accent` on `surface_elevated` |
| `Widget.AirCast.Banner` | `styles.xml` + `drawable/bg_banner.xml` | Top-centered pill over video (`scrim` fill) for "connecting…" messages, auto-hides |
| Artwork | `ImageView` + `drawable/bg_artwork.xml` | `radius_artwork` with `clipToOutline`; placeholder = brand icon on `surface_elevated` |
| `AspectRatioFrameLayout` | `shared/ui/AspectRatioFrameLayout.kt` | Fits the mirrored `SurfaceView` to the stream's aspect ratio, centered, letterboxed with `bg` |
| Brand icon | `drawable/ic_aircast.xml` | Generic screen-with-signal glyph tinted `accent` (not Apple's AirPlay logo) |
| Launcher banner | `drawable-xhdpi/banner.png` (rendered by `scripts/render_banner.swift`) | 320×180 dp Leanback banner, brand icon + name on `bg` |

## Themes (`values/themes.xml`)

- `Theme.AirCast` — `android:Theme.DeviceDefault.NoActionBar.Fullscreen`, `windowBackground = bg`, no window animations on the video screen.
- `Theme.AirCast.Invisible` — fully transparent, no preview/animation/dim; only for `BootActivity`, which never draws UI.

## Rules

- Never hard-code colors, text sizes or paddings in layouts; add a token here first.
- All text must be legible from 3 m: nothing below 16sp.
- Focusable elements (if any are added) must show an `accent` focus state — the remote has no pointer.
