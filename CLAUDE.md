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
    settings, Premium, a rewarded test video, the quick-convert bubble and its currency picker, the tour's
    "Show me how" to the Settings sample price, a long press on it and "Convert currency" from that menu) and
    fail on a crash of the app (uiautomator itself sometimes dies mid-run: printed, not counted). Artifacts can't
    be downloaded from the container (blob storage is blocked), so the screenshots are printed into the last
    step's log as `SHOT <name> <base64 chunk>` lines (read them with `get_job_logs` and join the chunks).
- **Every push to `main` is a release**: `release.yml` publishes `v5.0.<run>` with
  `currency-26-5.0.N.apk` (github flavor; the in-app updater installs it), and its `play` job builds the AAB
  with the same version once the `UPLOAD_KEYSTORE_*` secrets exist, uploading it to Play's internal track
  (status `PLAY_STATUS`, draft until the app's first review) once `PLAY_SERVICE_ACCOUNT_JSON` exists.
  Pushes that only touch notes, `docs/`, `store/`, `tools/` or the other workflows don't release
  (`paths-ignore`), so no `[skip ci]` is needed.
- The Claude GitHub integration can't start workflows by hand (`run_workflow` -> 403): trigger them with a
  push. `release.yml` has a manual
  "Play bundle only" mode (`github_release` off) for the owner to run from the Actions tab.
- `android.yml` opens the upload key from the secrets on every branch build and prints its fingerprints
  (expected SHA1 `A0:25:F0:85:BF:B1:22:62:53:5F:7B:95:90:16:31:26:82:AC:56:4B`).
- `store-assets.yml`: on changes under `store/` packs `store-images` (per language) and `store-videos` (the 13
  promo mp4s) as artifacts for the owner to download from the run page.
- `play-listing.yml` (run by hand) uploads `store/listing` to the Play store page.
- The public site is its own PUBLIC repo `ohavs/ohavs.github.io` (GitHub Pages from main, no workflow; add it to
  the session with `add_repo`): `privacy.html` (`AppLinks.PRIVACY_POLICY`, contact ohavstudio.contact@gmail.com),
  `app-ads.txt` at the root for AdMob, and `Currency-26/` redirects from the policy's old address (builds up to
  5.0.25 link there). The container can't reach github.io: check deploys with the actions tools
  ("pages build and deployment").
- After a release, download the APK and check it: version in the manifest
  (`pyaxmlparser` or a UTF-16 search), new strings in `resources.arsc`.
- When a task is done, save what the next session needs (this file, `docs/PLAY_LAUNCH_PLAN.md`), merge it,
  and tell the owner it is safe to `/clear`. The owner asked for this every time.
- Latest release: 5.0.28 (PR #48, run 40532720077: the widget works in the Play build (WorkManager/R8), tour hand,
  Premium crown, test tools; its Play bundle (test ads) is the one to upload to the closed test). 5.0.27 (PR #44: Play bundle with test ads, translation review). 5.0.26 (PR #40: the privacy policy link moved to https://ohavs.github.io/privacy.html; checked
  in the APK). 5.0.25 (PR #35) brought the real AdMob ids to the store build; its run (40078765153) built the signed Play
  bundle with the real ads (artifact `currency-26-play-bundle`; the log prints the AdMob app id), the one for the
  owner's first manual upload (plan step 3.3). GitHub step 1 and AdMob step 2 are done; in Play Console the app exists and
  the owner uploaded the first bundle by hand (2026-10-26, step 3.3); the rest of step 3 is next. History, decisions and the Play launch steps: `docs/PLAY_LAUNCH_PLAN.md` (Hebrew).

## Where things are
- `MainActivity`, `CurrencyApp` (runs `AppLanguage.initialize` first), `QuickConvertActivity` (bubble for
  text selected in other apps: `PROCESS_TEXT` and `SEND`).
- `data/`: `CurrencyRepository` - Room rates per USD from open.er-api.com, SharedPreferences
  `currency_prefs` for everything else.
- `ui/`: `CalculatorScreen` (banners, cards), `Keypad` (calculator keys), `HistoryPanel`,
  `SettingsScreen`, `CurrencyPicker` (`CurrencyChooser` is its body, reused by the bubble), `FeatureTour` and
  `Spotlight`, `QuickConvertSheet`, `SelectionMenuDemo` (the looping demo; `DemoTimeline` and, on Samsung, `SetupTimeline` are its pure scripts),
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
- Store: `store/listing/<locale>` (fastlane layout), `tools/store/`; the privacy policy is in `ohavs/ohavs.github.io`.

## Generated files - edit the generator, never the output
- Strings (13 languages): `tools/i18n/base.py` (key, he, en, es) and `translations.py` (ar ru fr de pt
  it tr zh ja hi), then `python3 tools/i18n/generate.py app/src/main/res && rm -rf tools/i18n/__pycache__`.
  It checks placeholders and writes every `values*/strings.xml`, including both `values-iw` and `values-he`.
- Icons: `tools/icons/make_icons.mjs` (one look per palette; the launcher icon is always "sage").
- Store listing: texts in `tools/store/listing.py` (`python3 tools/store/listing.py store/listing`, checks the
  30/80/4000 limits, writes the icon); images: `cd tools/preview && ./sync.sh && ONLY=store_ gradle -q run`,
  then `node tools/store/screenshots.mjs` (headlines and stickers in its COPY table, 13 languages; fonts from npm).
  Style (owner's pick, 2026-10-27, after rejecting "generic" white/bento looks): brutalist, one flat colour per
  screenshot from the launcher icon of the colour set its screen shows, slate outlines and hard shadows, Hebrew in
  Secular One, Latin/Cyrillic in Rubik. The owner reviews every language in the artifact
  https://claude.ai/artifact/KeKXVxz9ajFXA9YTavwjwF (codes like `de-3`); republish it after changes.
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
- Samsung's "Manage apps" panel in the setup demo (5.0.24) is drawn from the step texts ("turn on ... and tap
  Done"); no source documents the real screen. If the owner sends a screenshot, match it.
- `Spotlight` without `onTargetTap` lets touches through its hole (four blocks around it close it): the
  sample-price guide needs the long press to reach the real TextView. `SelectablePrice` must not reset its
  text on recomposition, or the selection the guide leads to is dropped.
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
- Ads and Premium (owner, 2026-10-27, asked again and confirmed): a trial or a video unlock opens the features
  but keeps the ads; only the purchase removes them (the Banner is gone entirely then). While Premium is on the
  Settings button wears a crown (`PremiumCrown`, a drawn `CrownIcon`) and the ad strip stays empty until its ad
  comes (the "Premium: no ads" line is only for users without Premium).
- Test-ads builds (Google's publisher id in the banner id) have `Monetization.testTools`: seven taps on the version
  in Settings > About show "end the trial" (also skips the first day without full-screen ads) and "restart the trial".
  They vanish by themselves once `PLAY_REAL_ADS: true`.
- Tour demos draw the finger as the selection-menu demo does (`TourFinger`: the TouchApp hand, tip at the spot),
  never a dot; small glyphs like "⇅" never centre in a box, use an icon.
- R8 removed the no-arg constructor of WorkManager's `OverwritingInputMerger` (made by reflection): in the Play
  build no work ran, so widgets stayed "Can't load widget" (Glance renders in a worker) and the rate refresh and
  Premium expiry workers never ran (5.0.27). Kept in `proguard-rules.pro`. Widgets also need `android:initialLayout`
  (Glance's `glance_default_loading_layout`).
  smoke.yml adds the widget from Settings and prints the app's log.
- History panel (5.0.19): the header is just the clock icon (the link already says "History"); the clear key
  is icon-only and exactly the size of the keypad key beside it, widening to "Clear all?" on the first tap.
  The owner notices mismatched button sizes: keep neighbouring buttons the same height and shape.

## Next
- **Open as of 2026-10-26 (owner's wishes, next session starts here):**
  - 2026-10-27: Store settings and the privacy URL fix are done (owner). The closed test ("Alpha") was SENT FOR REVIEW
    on 2026-10-27: 5.0.27 (versionCode 527, run 40501759554, test ads), list "צוות בודקים" (17 people), 177 countries +
    rest of world. Along the way the owner filled the en-US default store listing by hand (texts and images from
    `store/listing/en-US`, AI declaration "Don't label assets") and the Advertising ID declaration (Yes; advertising +
    analytics). The Dashboard shows "0 testers opted-in" of 12. Next: when Google approves (mail/bell), the owner copies
    "Join on the web" from Closed testing > Alpha > Testers and sends it; the 14 days count once 12 have opted in. Internal testing doesn't count. Play bundles now carry Google's TEST ads (`PLAY_REAL_ADS: false` in
    `release.yml`) for the whole test period: the owner doesn't trust testers not to tap ads. Set it to `true` and build
    a new bundle before applying for production. The 5.0.25 bundle the testers got has real ads: the owner replaces it
    with a "Play bundle only" run. Next: service account (steps given to the owner, plan step 3.6).
  - Play Console: the first 9 "App content" forms are done (answers in the plan). Left: Store settings (Finance,
    contact email ohavstudio.contact@gmail.com, website `https://ohavs.github.io`: needed for app-ads.txt; the
    owner wants ALL ad buyers, so app-ads.txt is not optional) and the store listing (via the service account +
    `play-listing.yml`, not by hand). The service account key is not created yet. App content > Privacy policy
    must be changed to `https://ohavs.github.io/privacy.html` (it was `/Currency-26/privacy.html`).
    AdMob consent messages were published only on 2026-10-26 ("Currency GDPR", 9 European languages, no "Do not
    consent" button; and a US states opt-out message): until then UMP had no form to show. AdMob has no privacy
    URL field; it takes the app's details from the Play listing once linked.
    Completed Play checklist items are edited from the left menu (Policy and programs > App content; Store presence
    > Store settings), not from the dashboard checklist.
  - The owner wants the code private: the site moved to the public `ohavs/ohavs.github.io` (2026-10-26, the owner
    accepted a public repo holding only public files). Next the owner makes `ohavs/Currency-26` private (Settings >
    Danger zone). Consequences: the github build's in-app updater reads public releases (breaks; the owner moves
    to the Play test build), the unauthenticated api.github.com polling breaks (use the MCP actions tools), Actions
    get 2,000 free minutes/month, and the old Pages project site goes away (the redirect in ohavs.github.io takes
    over). After the app is live and linked in AdMob, check that app-ads.txt shows "Verified"; if github.io isn't
    accepted, the owner buys a domain and we attach it to the same repo (custom domain).
  - Store images and video (2026-10-27, merged): the owner approved the images; a Spanish speaker found mistakes,
    so all 12 non-Hebrew languages were reviewed (app strings, store texts, screenshot headlines) and the screenshots
    re-rendered. Open question for the owner: amounts are formatted US-style in every language ("$4,980.42";
    Locale.US in `AmountFormat.kt`), which a native speaker may read as a mistake. Promo video v6
    (`store/video/promo-<lang>.mp4`, Hebrew `promo-he.mp4`; source `tools/video/promo`, README) is APPROVED by the
    owner: SFX Kit effects + "Upbeat" by AtlasAudio (Pixabay, commercial use, no credit; the mp3 is NOT in git, the
    owner has it), punch-hole camera, translucent phone shadow, the bubble on the page without the grey scrim.
    Music licenses: check commercial/advertising use first (freetouse.com's free license forbids it; that track was
    dropped; both mp3s the owner uploaded are still in git history, one more reason to make the repo private). The video still has to be uploaded to YouTube and linked in the store listing (owner).
  - Store images/videos for the owner: `store-assets.yml` run 40528996051 (artifacts `store-images`, `store-videos`).
    The other languages' images go up with `play-listing.yml` once the service account exists (or by hand under
    Main store listing > Manage translations). Videos: one YouTube link per language (unlisted is fine), at least
    en and he.
- Google Play launch: the code and the store page are ready; the owner's steps are listed in order at the top
  of `docs/PLAY_LAUNCH_PLAN.md` (secrets, AdMob ids, Play Console, service account, closed
  test with 12 testers for 14 days). AdMob is done (2026-10-24): the real ids are in `gradle.properties`
  and only the store build uses them; smoke and branch builds pass `-PadmobTestAds=true` (Google's test ads,
  since real ads on an emulator are invalid traffic). Each build logs the AdMob app id it carries. When the service account secret
  exists, the owner runs `play-listing.yml` from the Actions tab (we can't dispatch); read its log. After the first production review, set `PLAY_STATUS: completed`.
- The Play upload key was generated in session 2026-10-20 and sent to the owner (not in git).
