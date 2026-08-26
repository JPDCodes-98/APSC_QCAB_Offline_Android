# APSC QCAB Offline Android App

This is a native Android WebView wrapper around the complete APSC Mains PYQ QCAB generator.

## Offline features
- Entire APSC PYQ database is bundled inside the APK/project.
- The supplied answer-sheet template is embedded in the bundled QCAB generator.
- No INTERNET permission is requested.
- Subject / paper / year / topic / marks / keyword filters work offline.
- Select All Filtered and Deselect Filtered work offline.
- 10 marks -> 2 answer-sheet pages.
- 15 marks -> 4 answer-sheet pages.
- Question appears only on Page 1 for each selected question.
- Long questions wrap automatically.
- Generated PDF is saved to `Downloads/APSC_QCAB/` on Android 10+.
- Custom PYQ JSON import remains available via Android's file picker.

## Build an APK in Android Studio
1. Extract this ZIP.
2. Open the folder `APSC_QCAB_Offline_Android` in Android Studio.
3. Allow Gradle sync to finish.
4. Choose **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
5. Install the generated `app-debug.apk` on the Android phone.

The project deliberately contains no INTERNET permission, so once installed it does not require an internet connection.

## Package name
`com.apsc.qcab`

## Automatic APK build with GitHub
A GitHub Actions workflow is included at `.github/workflows/build-apk.yml`.
If this project is pushed to a GitHub repository, GitHub can compile the APK automatically. Open the repository's **Actions** tab, run **Build Offline Android APK**, then download the `APSC-QCAB-Offline-APK` artifact.
