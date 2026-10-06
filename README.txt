HOW TO GET THE APK (no Android Studio needed)

1. Make a free account at github.com and click New repository (any name, Public or Private). Create it.
2. In the new repo click "uploading an existing file" and drag in EVERYTHING from this folder
   (app/, .github/, build.gradle, settings.gradle, gradle.properties).
   - If the ".github" folder doesn't upload: click Add file > Create new file, type the name
     .github/workflows/build.yml  and paste the contents of build.yml. Commit.
3. Click the "Actions" tab. A build named "Build APK" starts automatically (3-5 min).
   If it doesn't: Actions > Build APK > Run workflow.
4. When it shows a green tick, open it, scroll to "Artifacts" and download ShortsBlocker-APK (zip).
   Unzip it to get app-debug.apk.
5. Send the APK to your phone, install it (allow "Install unknown apps").
6. Open Shorts Blocker and follow the on-screen setup.

TESTING: turn on "Show wall in red". You should see a red box over YouTube's Shorts tab.
Then turn it off to make it invisible.
