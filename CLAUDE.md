# Currency Converter (Currency-26)

Android currency converter: Kotlin, Jetpack Compose (Material3) and Glance home-screen widgets.
Application id `com.ohav.currency`; code namespace `com.example`. The owner writes in Hebrew: answer in
Hebrew, and show rendered previews of visual changes (see `tools/preview`).

## Workflow
- Work on the session branch and merge to `main` through a PR (GitHub MCP tools; `merge_pull_request`
  needs the full 40-character `expectedHeadSha`).
- `.github/workflows/android.yml` builds every branch push (assembleDebug, assembleRelease,
  testDebugUnitTest). There is no Android SDK in the container, so CI is the only real Android build:
  push, then poll `https://api.github.com/repos/ohavs/Currency-26/actions/runs?head_sha=<sha>`.
- **Every push to `main` is a release**: `release.yml` publishes `v5.0.<run>` with
  `currency-26-5.0.N.apk`, and the app's in-app updater installs it. For docs/tooling-only merges put
  `[skip ci]` in the merge commit title so no empty release goes out.
- After a release, download the APK and check it: version in the manifest
  (`pyaxmlparser` or a UTF-16 search), new strings in `resources.arsc`.
- Latest release: 5.0.16. History and decisions: `docs/PLAY_LAUNCH_PLAN.md` (Hebrew).

## Where things are
- `MainActivity`, `CurrencyApp` (runs `AppLanguage.initialize` first), `QuickConvertActivity` (bubble for
  text selected in other apps: `PROCESS_TEXT` and `SEND`).
- `data/`: `CurrencyRepository` - Room rates per USD from open.er-api.com, SharedPreferences
  `currency_prefs` for everything else.
- `ui/`: `CalculatorScreen` (banners, cards, keypad), `SettingsScreen`, `CurrencyPicker`, `FeatureTour`
  and `Spotlight`, `QuickConvertSheet`; `theme/Color.kt` (palettes) and `theme/PaletteTexture.kt`.
- `utils/`: `PriceParser` and `QuickConvert` (prices in free text), `CurrencySearch`, `AppLanguage`,
  `CountryLanguage`, `DeviceCountry`, `FeatureTourSchedule`, amount formatting, currency info.
- `widget/`: Large/Small Glance widgets, `WidgetSizing` (pure, unit-tested), `WidgetColors` and
  `WidgetTexture`, `WidgetUpdater`.
- `update/`: `AppUpdater` (GitHub releases), `LegacyApp` (offers to remove the old AI Studio build).

## Generated files - edit the generator, never the output
- Strings (13 languages): `tools/i18n/base.py` (key, he, en, es) and `translations.py` (ar ru fr de pt
  it tr zh ja hi), then `python3 tools/i18n/generate.py app/src/main/res && rm -rf tools/i18n/__pycache__`.
  It checks placeholders and writes every `values*/strings.xml`, including both `values-iw` and `values-he`.
- Icons: `tools/icons/make_icons.mjs` (one look per palette; the launcher icon is always "sage").
- Textures: `tools/textures/make_textures.mjs` (tiles in `drawable-xxhdpi`, `drawable/texture_<key>.xml`
  for widgets; the "rose" sun rays are a vector mirrored in `PaletteTexture.kt`).
- Run both with `PATH=/opt/node22/bin:$PATH NODE_PATH=$(npm root -g) node <script>` (global Playwright
  and its Chromium); both take `--preview out.png`.

## Things learned the hard way
- Palette keys are not their names: sage=Classic, ocean=Sea, sunset=Sand, rose=Sunset, lavender=Night,
  gold=Gold.
- Language: new installs follow the device ("system", English fallback); installs from before the setting
  stay Hebrew. Activities wrap their context in `attachBaseContext`. Labels shown inside other apps (the
  selection menu, the share sheet) follow the device language, not the in-app one.
- The feature tour must never pop up by itself: its sheet covers the keypad and its buttons sit on the
  `. 0 00` row, so people typing dismissed it by accident. It is offered by a banner instead.
- Samsung One UI 7/8 hides other apps in the text-selection menu, so quick convert also registers as a
  share target.
- Glance: keep layout nesting under 10 levels. Image tints must be opaque (texture strength is baked into
  the PNG alpha) because API < 31 tints with SRC_ATOP.
- RTL: amounts use `TextDirection.Ltr`; `Modifier.offset` mirrors in RTL, `absoluteOffset` does not.

## Next
- The owner is checking 5.0.16 on a Samsung (share -> "Convert currency", tour banner).
- Then more features of the owner's choosing, and later monetization and Google Play publishing as planned
  in `docs/PLAY_LAUNCH_PLAN.md`.
