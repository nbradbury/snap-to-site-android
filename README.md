# Snap-to-Site

A camera-first WordPress site builder. Describe your business out loud, photograph your storefront, products, menu board, flyers and logo, and Claude turns it into a working site. After launch you keep it current the same way: say "we're closed Thursday for the holiday" and the hours and a banner update. Changes apply with Undo and an Activity log, or wait for your OK if you turn on "Review changes before applying".

<img width="3333" height="1373" alt="collage" src="https://github.com/user-attachments/assets/35541ae3-0073-4822-8004-9306e1a031a4" />

- **Claude** (`claude-opus-5-5` via the Anthropic Java SDK) reads the description and photos into a structured `SiteModel`: business info, menu, hours, events, contact, and which photos go where. Edits are model-to-model transforms with a one-line summary.
- **wordpress-rs** handles everything WordPress: API discovery, the application-password login flow, media uploads, pages and site settings.
- The app renders the model to core-block markup itself (`BlockRenderer`), so markup stays valid and the menu and hours stay structured. Each section is a group with a `snap-*` class.
- The site publishes as Home, Menu, Events and Visit pages; Menu and Events only appear once there's something on them. The app's preview renders the same model.
- Colors come from the logo (or storefront) photo on-device (`androidx.palette`) and are applied as block colors.
- While you talk, Gemini Nano (ML Kit GenAI) picks out the basic details on phones that have it; elsewhere Claude does it once you stop.
- The camera is CameraX; speech uses the platform `SpeechRecognizer` with live partial results.
- Icons come from Material Symbols Rounded, subset to the few the app uses by `tools/subset_symbols.py`. The site preview uses [Newsreader](https://github.com/google/fonts/tree/main/ofl/newsreader) (SIL Open Font License).

## Setup

1. A Claude API key, entered in the app. It's stored in app-private storage and backup is disabled. A shipped app would need a proxy instead.
2. A WordPress site with application passwords enabled (self-hosted or WordPress.com Atomic, over HTTPS). Use a block theme such as Twenty Twenty-Five; core REST can't switch themes, so the app styles whatever block theme is active.

## Limits

- The app is the source of truth for the pages it creates. If someone edits them in wp-admin, the next change warns before overwriting them. WordPress keeps revisions either way.
- The SDK derives structured-output schemas from classes using reflection APIs Android lacks, so `SchemaTest` derives them on the JVM and writes `app/src/main/assets/schemas/`. If you change `SiteModel`, run the tests twice: the first run regenerates the assets and fails, the second passes.
- The APK is large because wordpress-rs ships native libraries for four ABIs; an app bundle splits them per device.

## Building

```sh
./gradlew testDebugUnitTest assembleDebug
```
