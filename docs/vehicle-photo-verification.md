# Vehicle photo presentation

Verified on 2026-10-26 using the existing Pixel_10a emulator and debug APK.

- Vehicle photos use a full-width 4:3 preview with 24 dp rounded corners and
  centered cropping. Height is limited on large or short windows.
- The shared presentation appears in Overview, Garage and the vehicle editor.
- Tapping a photo opens the complete original image with proportional fitting.
- The original attachment and persistence format are unchanged.

Manual checks completed using a WebP illustration already in emulator Downloads:

- Selected the file through the system document picker and inspected the large,
  rounded preview in the editor.
- Saved the vehicle and confirmed the photo appeared in Garage.
- Opened the full image and visually confirmed it was shown without cropping.
- Closed the viewer with system Back and returned to Garage.
- Force-stopped and relaunched the app; confirmed the saved photo appeared in
  Overview alongside the existing 10,450 km reading.

`./gradlew spotlessCheck :app:assembleDebug :app:lintDebug` passed.
Lint reported 0 errors and 20 warnings. All three locales have the same 320 string
keys. No automated tests were added, changed or run. No physical device data was
modified. Portrait was checked manually; landscape, large fonts and TalkBack
were not exercised in this pass.
