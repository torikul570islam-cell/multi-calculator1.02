# Multi Calculator — Android Studio project

This project wraps the supplied `index.html` in a local Android WebView and uses the native Google Mobile Ads SDK for rewarded video ads. The HTML calculator UI and its existing calculator implementations are preserved.

## Important AdMob setup

The supplied HTML contains a **Rewarded Ad unit ID**:

`ca-app-pub-2754574676620387/6093293644`

It does **not** contain an AdMob **App ID** (the ID with `~` in it). The project therefore uses Google's official sample App ID by default so debug builds can be tested safely. Before a production build, get the App ID from your AdMob account and pass it as a Gradle property:

```bash
gradle assembleRelease -PADMOB_APP_ID=ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY
```

Do not publish a release APK until you have replaced the sample App ID with your own. The release variant uses the rewarded unit ID from the uploaded HTML. The debug variant uses Google's official test rewarded-ad unit ID to avoid invalid traffic during development.

## Build locally

1. Install Android Studio, Android SDK Platform 36, and JDK 17.
2. Open this folder (`MultiCalculatorAndroid`) in Android Studio.
3. Let Gradle sync, then run the `app` configuration on a device/emulator.
4. For a test APK, run `gradle assembleDebug` (or Android Studio: **Build > Build APK(s)**).
5. For a production APK, configure your own AdMob App ID as described above and run `gradle assembleRelease -PADMOB_APP_ID=...`.

The GitHub Actions workflow at `.github/workflows/android.yml` builds a debug APK and uploads it as a workflow artifact on pushes to `main`/`master` or when manually triggered.

## Reward/access behavior

- No ad is requested at app launch or merely when opening the Templates list.
- Selecting a calculator service checks the 7-day local access expiry.
- If access expired, the app requests a rewarded ad only after the user taps **Watch Ad & Unlock 7 Days**.
- Access is granted only by the native SDK's reward callback, not by closing/skipping/failing an ad.
- Access expiry is stored in WebView local storage. It is device-local and can be reset if app data is cleared or the app is reinstalled; it is not a tamper-proof server entitlement.

## Before publishing

- Replace the sample AdMob App ID with the exact App ID from your AdMob account.
- Verify the production rewarded ad unit belongs to that same AdMob app.
- Test on a real Android device using the debug build/test ad IDs first.
- Complete AdMob app setup, privacy policy, consent/UMP requirements where applicable, data-safety disclosures, store listing and release signing.
- Change `applicationId` in `app/build.gradle.kts` before the first release if you need a different package name. Do not change it after publishing an app update unless you intend to publish a separate app.

## Validation limits

This source package was reviewed and assembled, but an Android APK could not be compiled in this execution environment because Android SDK/Gradle tooling is not installed here. GitHub Actions or Android Studio must perform the actual Android build. No one can guarantee a literal zero build/deployment failure probability; run the CI build and test the resulting APK before release.
