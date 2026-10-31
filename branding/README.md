# DriveCare identity

Original editable vector artwork, created for this application on 2026-10-25.
No external logo, stock asset or image-generation service was used.

The protective shield carries a D-shaped road, three lane markings and a mint care
check. Palette: royal blue `#2563EB`, deep blue `#1D4ED8`, mint `#75E2CA`, navy `#10254B`.

| Asset | Format / dimensions | Purpose |
| --- | --- | --- |
| `drivecare-logo.svg` | SVG, 512×512, transparent | Editable master symbol |
| `drivecare-app-icon.svg` | SVG, 512×512 | Rounded preview composition, navy background |
| `drivecare-app-icon-preview.png` | PNG, 512×512, alpha | Quick Look render of the SVG, reviewed visually |
| `drivecare-loading-preview.png` | PNG, 1080×2424 | Actual light-theme emulator frame from the verified startup recording |
| `../app/src/main/res/drawable/drivecare_logo.xml` | VectorDrawable, viewport 128×128 | Logo inside the loading screen |
| `../app/src/main/res/drawable/drivecare_launcher_foreground.xml` | VectorDrawable, viewport 108×108 | Adaptive foreground; central symbol bounded to 64×64 |
| `../app/src/main/res/drawable/drivecare_launcher_monochrome.xml` | VectorDrawable, viewport 108×108 | One-color themed icon |

Adaptive launcher XML supplies separate background/foreground/monochrome layers.
The rounded preview is not used as a flattened adaptive foreground. Existing legacy
bitmap resources remain preserved; API 26+ uses adaptive resources. Store artwork
has not been uploaded or validated by Google Play.

Brand loading uses the same VectorDrawable with a subtle breathing scale and a
rotating progress arc in Compose. The system launch window is themed on API 26+;
API 31+ also receives standard native splash attributes. There is one Activity,
no splash Activity or external animation dependency. The intro lasts at least 650 ms
on a new application flow and waits for local state, not for network requests.
Rotation retains the intro state. Actual loading failures expose a retry action.
