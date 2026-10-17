# Farazi Advertising integration notes

This calculator source is connected to the same Firebase project used by the Farazi Central Ads Admin Panel (`farazi-advertising`).

## What was aligned
- The app creates and saves a stable `registered_apps` document ID in local storage and registers `Multi Calculator` in Firestore.
- The Admin Panel's existing App-Wise Statistics fields (`totalImpressions`, `totalClicks`) are incremented when a Farazi ad is displayed/clicked.
- Ad-level `impressions` and `clicks` are updated in the same Firestore batch as the app-level totals.
- Banner rotation runs when the user opens Templates: ten Start.io banner placements, then one Farazi ad attempt.
- Ad targeting uses `All Apps` or the generated app document ID, matching the Admin Panel dropdown.

## Important testing notes
- The Start.io banner is a native Android SDK view; it will not display when `index.html` is opened in ordinary Chrome.
- The Farazi ad needs a published Banner/Image ad in Firestore, with a valid image URL and target `All Apps` or this app's generated ID.
- Firebase Firestore rules must permit this app's registration and counter writes. Client-side Firebase configuration alone does not guarantee permissions.
- No live Firebase test or Android Gradle build was run in this environment. Test a debug APK and verify the `registered_apps` document and App-Wise Statistics in the Admin Panel.
