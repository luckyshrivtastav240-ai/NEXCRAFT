# NEXCRAFT — Complete Android Launcher Repair & Mobile Redesign

Repository: https://github.com/maxtroitofficial/NEXCRAFT  
Working branch: `nexcraft/mobile-layout-and-ci`

## Important
Work on this existing project. Do not create a separate project, static mockup, or fake demo.

## 1. Inspect first
- Inspect repository structure, current code, recent commits, and the latest GitHub Actions results.
- Identify current launcher architecture, navigation, Minecraft launch flow, Gradle configuration, bugs, and incomplete features.
- Preserve working functionality, project history, license obligations, and third-party attribution.

## 2. Full mobile-first redesign
Redesign the actual Android app for portrait phones and touch use. Update Intro, Home/Play, Versions, Mods, Accounts/Login, Wardrobe, Settings, Renderer/Controls, Updates, About/Credits.
- Professional Minecraft-inspired dark navy/charcoal design with restrained cyan accents.
- Consistent typography, spacing, icons, and navigation.
- Correct scrolling, safe areas, keyboard handling, and Android back navigation.
- Clear loading, empty, progress, success, and error states.
- Complete Hindi translations where supported.
- Do not redesign only one card or change only colors.

## 3. Repair real functionality
Audit and fix version selection/installation, supported mod-loader installation, mod management, account/authentication flow, download progress/errors, persistent settings, update flow, and existing features. No dead buttons, fake success states, or mock data presented as real functionality.

## 4. Launcher icons and cosmetics
Inspect AndroidManifest.xml, activity aliases, icon resources, and selection logic. Fix mappings so each icon option uses the correct artwork. Verify behavior on supported Android versions and explain OS limitations honestly. Do not claim capes or head accessories appear in gameplay unless implementation and compatibility are verified.

## 5. Real 3D home background
Check whether a real, lightweight 3D Minecraft-style scene is feasible in this architecture. Do not substitute a static image and call it 3D. Consider device performance, APK size, compatibility, and licensing; provide a fallback if needed.

## 6. Actual in-game AI companion
Investigate whether an actual in-game entity can be implemented for supported Minecraft versions/mod loaders. A chatbot or decorative character does not count. If a separate Minecraft mod is required, explain that and implement it only if feasible. Never label unimplemented functionality as complete.

## 7. Build and verify
- Run available tests and Gradle checks.
- Fix compilation, resource, manifest, and relevant lint errors.
- Run GitHub Actions and inspect the final result.
- Generate an installable ARM64 APK if the workflow supports it.
- Provide an APK link only after confirming the artifact exists.
- Do not claim physical-device testing unless it was performed.
- Pending, cancelled, or failed builds are not successful builds.

## 8. Work safely and incrementally
Work on the branch above, commit changes in stages with clear messages, and never expose keys, passwords, tokens, or secrets. Do not delete working features merely to make the build pass.

## 9. Final report
List changed files and real improvements, remaining limitations, exact test/build status, successful Actions run link, and APK artifact link if one was generated.

## Start now
Inspect the repository and latest build failures, then make and verify real changes. Do not stop after a plan or cosmetic-only edits.
