# Currency Converter (Currency-26)

Android currency converter: Kotlin, Jetpack Compose (Material3) and Glance home-screen widgets.
Application id `com.ohav.currency`; code namespace `com.example`. The owner writes in Hebrew: answer in
Hebrew, and show rendered previews of visual changes (see `tools/preview`).

## Workflow
- Work on the session branch and merge to `main` through a PR (GitHub MCP tools; `merge_pull_request`
  needs the full 40-character `expectedHeadSha`).
- **Two builds (flavors):** `github` = the owner's APK on GitHub Releases (in-app updater, everything unlocked,
  no ads, shared debug key, no R8); `play` = Google Play (ads, Premium, Billing, UMP, R8, upload key, AAB).
  Gradle tasks carry the flavor: `assembleGithubRelease`, `bundlePlayRelease`, `testGithubDebugUnitTest`...
- CI (no Android SDK in the container, and dl.google.com is blocked, so CI is the only real Android build;
  push, then poll `https://api.github.com/repos/ohavs/Currency-26/actions/runs?head_sha=<sha>`):
  - `android.yml`: every branch push builds both flavors, prints each APK's permissions and size, runs tests.
  - `smoke.yml`: app changes on a branch run the minified Play APK on an emulator (start, calculator,
    settings, Premium, a rewarded test video, the quick-convert bubble and its currency picker, a long press
    on the Settings sample price and "Convert currency" from that menu) and fail on a crash. Artifacts can't
    be downloaded from the container (blob storage is blocked), so the screenshots are printed into the last
    step's log as `SHOT <name> <base64 chunk>` lines (read them with `get_job_logs` and join the chunks).
- **Every push to `main` is a release**: `release.yml` publishes `v5.0.<run>` with
  `currency-26-5.0.N.apk` (github flavor; the in-app updater installs it), and its `play` job builds the AAB
  with the same version once the `UPLOAD_KEYSTORE_*` secrets exist, uploading it to Play's internal track
  (status `PLAY_STATUS`, draft until the app's first review) once `PLAY_SERVICE_ACCOUNT_JSON` exists.
  Pushes that only touch notes, `docs/`, `site/`, `store/`, `tools/` or the other workflows don't release
  (`paths-ignore`), so no `[skip ci]` is needed (it would also stop `pages.yml`).
- The Claude GitHub integration can't start workflows by hand (`run_workflow` -> 403): trigger them with a
  push (`pages.yml` runs when `site/` or the file itself changes on main). `release.yml` has a manual
  "Play bundle only" mode (`github_release` off) for the owner to run from the Actions tab.
- `android.yml` opens the upload key from the secrets on every branch build and prints its fingerprints
  (expected SHA1 `A0:25:F0:85:BF:B1:22:62:53:5F:7B:95:90:16:31:26:82:AC:56:4B`).
- `play-listing.yml` (run by hand) uploads `store/listing` to the Play store page; `pages.yml` publishes
  `site/` (privacy policy) to https://ohavs.github.io/Currency-26/.
- After a release, download the APK and check it: version in the manifest
  (`pyaxmlparser` or a UTF-16 search), new strings in `resources.arsc`.
- When a task is done, save what the next session needs (this file, `docs/PLAY_LAUNCH_PLAN.md`), merge it,
  and tell the owner it is safe to `/clear`. The owner asked for this every time.
- Latest release: 5.0.23 (PR #29: an animated demo of the selection menu in Settings > "Convert from any app").
  Its run built the first signed Play bundle (artifact `currency-26-play-bundle`, run 39430944465), ready for
  the owner's first manual upload (plan step 3.3). Upload key secrets are in and verified. GitHub Pages is on
  (source "GitHub Actions"); the owner's manual Pages run on 2026-10-20 published the privacy policy at
  https://ohavs.github.io/Currency-26/privacy.html (step 1 is done). History, decisions and the Play launch
  steps: `docs/PLAY_LAUNCH_PLAN.md` (Hebrew).

## Where things are
- `MainActivity`, `CurrencyApp` (runs `AppLanguage.initialize` first), `QuickConvertActivity` (bubble for
  text selected in other apps: `PROCESS_TEXT` and `SEND`).
- `data/`: `CurrencyRepository` - Room rates per USD from open.er-api.com, SharedPreferences
  `currency_prefs` for everything else.
- `ui/`: `CalculatorScreen` (banners, cards), `Keypad` (calculator keys), `HistoryPanel`,
  `SettingsScreen`, `CurrencyPicker` (`CurrencyChooser` is its body, reused by the bubble), `FeatureTour` and
  `Spotlight`, `QuickConvertSheet`, `SelectionMenuDemo` (the looping demo; `DemoTimeline` is its pure script),
  `SelectionMenu` (a real TextView to long-press, and the menu labels in the
  phone's language; the preview has a stand-in in `tools/preview/stubs`); `theme/Color.kt` (palettes) and
  `theme/PaletteTexture.kt`.
- `utils/`: `Calculator` (expression keys, parsing, `%` rules), `CalcHistory`, `PriceParser`
  and `QuickConvert` (prices in free text), `CurrencySearch`, `AppLanguage`, `CountryLanguage`,
  `DeviceCountry`, `FeatureTourSchedule`, amount formatting, currency info.
- `widget/`: Large/Small Glance widgets, `WidgetSizing` (pure, unit-tested), `WidgetColors` and
  `WidgetTexture`, `WidgetUpdater`.
- `premium/`: `Premium.kt` (pure rules: `PremiumState`, `Access`, trial, 2 videos -> 24h, `InterstitialPolicy`,
  `ReviewPolicy`; tested), `PremiumStore` (prefs "premium"), `Monetization` (interface). Implementations:
  `src/play/.../PlayMonetization.kt` (Billing, AdMob, UMP, review) and `src/github/.../CreateMonetization.kt`
  (everything open). Premium = widget, colour sets, calculator; the rest is free (see the plan doc).
- `update/` lives in `src/github` (`AppUpdater`, `LegacyApp`); `src/play` has no-op stand-ins.
- `ui/PremiumSheet.kt` (sheet, `PremiumPromo` for the banner slot, `LockBadge`), `widget/WidgetLocked.kt`,
  `widget/WidgetText.kt` (`AppFontText`: Rubik text as a tinted bitmap), `workers/PremiumExpiryWorker`.
- Store: `store/listing/<locale>` (fastlane layout), `site/` (privacy policy), `tools/store/`.

## Generated files - edit the generator, never the output
- Strings (13 languages): `tools/i18n/base.py` (key, he, en, es) and `translations.py` (ar ru fr de pt
  it tr zh ja hi), then `python3 tools/i18n/generate.py app/src/main/res && rm -rf tools/i18n/__pycache__`.
  It checks placeholders and writes every `values*/strings.xml`, including both `values-iw` and `values-he`.
- Icons: `tools/icons/make_icons.mjs` (one look per palette; the launcher icon is always "sage").
- Store listing: texts in `tools/store/listing.py` (`python3 tools/store/listing.py store/listing`, checks the
  30/80/4000 limits, writes the icon); images: `cd tools/preview && ./sync.sh && ONLY=store_ gradle -q run`,
  then `node tools/store/screenshots.mjs` (captions reuse the app's tour titles; Noto fonts from npm).
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
- Samsung One UI 7/8 hides other apps in the text-selection menu until they are turned on under ⋮ >
  "Manage apps", so quick convert also registers as a share target. That screen is part of the selection menu
  and has no intent: Settings > "Convert from any app" offers a sample price to long-press instead (5.0.22).
  Apps can't move their item up the selection menu: system actions come first, the rest overflows into ⋮,
  and Android ignores intent-filter priority for regular apps.
- Quick-convert bubble (5.0.22): tapping the converted card (or the chip over a list) picks another currency
  inside the bubble. The pick is kept (`quick_target`) until the app's own target changes, and "open in app"
  carries it over (`QuickConvert.appCurrencies`).
- Glance: keep layout nesting under 10 levels. Image tints must be opaque (texture strength is baked into
  the PNG alpha) because API < 31 tints with SRC_ATOP.
- Widgets can't use font resources (launchers inflate them in a restricted context) and Glance's "medium"
  weight looks regular on Samsung: text in the app's font goes through `AppFontText` (a white bitmap, tinted).
- R8 (play release): OkHttp needs `-dontwarn` for its optional TLS providers; widget and worker classes are
  kept by name (Glance and WorkManager create them by reflection).
- Desktop preview renders use the system sans font for anything without Rubik (the widget mocks): install
  Roboto (fontconfig alias) so they look like a phone; `npm pack roboto-fontface` + fontTools woff->ttf.
- RTL: amounts use `TextDirection.Ltr`; `Modifier.offset` mirrors in RTL, `absoluteOffset` does not. A
  calculation inside Hebrew/Arabic prose needs LRM marks around it, or "120 + 15%" shows as "15% + 120".
- Calculator (5.0.17): "00" became the calculator key; it shows "=" only while there is something to compute.
  The amount in prefs follows the live result (the widget reads it). Keys never change under a finger that
  may still be typing.
- Calculator display (5.0.18, the owner's feedback): a calculation carries no currency symbol ("90 × 3 Ft"
  read as if only the 3 were forints); the symbol goes with the result under it, and the cards show codes
  instead of names while the calculator is open. Quick amounts were dropped; the history must not take a
  row of its own, so it is a link opposite "+ add currency" under the cards (in calculator mode the
  keypad's top row is full).
- History panel (5.0.19): the header is just the clock icon (the link already says "History"); the clear key
  is icon-only and exactly the size of the keypad key beside it, widening to "Clear all?" on the first tap.
  The owner notices mismatched button sizes: keep neighbouring buttons the same height and shape.

## Next
- Google Play launch: the code and the store page are ready; the owner's steps are listed in order at the top
  of `docs/PLAY_LAUNCH_PLAN.md` (GitHub Pages + secrets, AdMob ids, Play Console, service account, closed
  test with 12 testers for 14 days). When the AdMob ids arrive, put them in `gradle.properties`
  (`admobAppId`, `admobBanner`, `admobInterstitial`, `admobRewarded`). When the service account secret
  exists, run `play-listing.yml`. After the first production review, set `PLAY_STATUS: completed`.
- The Play upload key was generated in session 2026-10-20 and sent to the owner (not in git).
