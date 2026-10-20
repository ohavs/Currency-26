# Desktop preview

Renders the app's Compose screens on the desktop (Compose Desktop + Skia), without an Android device or
emulator: the calculator, settings, picker, tour, quick-convert bubble and mock widgets, in any language,
palette and light/dark. It also runs the pure unit tests (`utils/`, widget sizing) and a few gesture checks.

```sh
cd tools/preview
./sync.sh                         # copy ui/, utils/ and tests in from the app; write R stand-ins
ONLY=v15_quick gradle -q run      # render (ONLY filters by name; omit for everything) -> out/*.png
gradle -q test                    # utils and widget-sizing tests
gradle -q dragCheck               # swap/drag, spotlight, tour and keypad tap checks
gradle -q vmCheck                 # the real MainViewModel: calculator, history
python3 montage.py sheet.png 900 out/a.png out/b.png   # side-by-side sheet at 900px high
```

- Needs Gradle 8.x and JDK 21; dependencies come from Maven Central (no Google Maven, so androidx
  lifecycle/annotation are stubbed in `stubs/`).
- `stubs/` stands in for Android-only pieces: string and drawable resources (read straight from
  `app/src/main/res`), fonts, the repository, the updater, `BackHandler`.
- Glance widgets can't run here; `render/WidgetMock2.kt` mirrors their layout math (`WidgetSizing.kt`
  is shared). Keep it in step when the widgets change.
- When app code gains a new Android dependency in `ui/` or `utils/`, add a stub or the compile fails.
