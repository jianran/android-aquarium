# Android Aquarium 🐠

A full-screen animated aquarium for Android — pure Kotlin & Canvas, zero external dependencies.

## Features

- **8 cute fish types** — Clownfish, Blue Tang, Angelfish, Guppy, Pufferfish, Butterflyfish, Tetra, Goldfish
- Each fish has unique body shape, colour, fins, eye, smile and optional racing stripes
- Fish swim, bob, and occasionally flip direction naturally
- Swaying seaweed growing from a wavy sand bed with pebbles
- Rising bubbles with rim highlight and shine
- Animated shimmer light rays from the surface
- Rolling sand-ripple texture
- Hardware-accelerated Canvas at display refresh rate (~60 fps)
- Full-screen immersive mode — no status/nav bar

## Requirements

| Item | Value |
|------|-------|
| Min SDK | 26 (Android 8.0 Oreo) |
| Compile SDK | 34 |
| Language | Kotlin |
| Build system | Gradle 8.4 / AGP 8.2 |

## Build & install

```bash
# debug APK
./gradlew assembleDebug

# install on connected device / emulator
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Project structure

```
app/src/main/java/com/aquarium/app/
├── MainActivity.kt      — full-screen Activity
├── AquariumView.kt      — Canvas drawing + animation loop
├── Fish.kt              — Fish data class + FishType enum (colours, shapes)
├── Bubble.kt            — Bubble data class
└── Seaweed.kt           — Seaweed data class
```

## Customisation

| What | Where |
|------|-------|
| Number of fish | `initFish()` → `repeat(12)` |
| Fish size range | `28f + Random.nextFloat() * 38f` |
| Swim speed | `0.7f + Random.nextFloat() * 2.0f` |
| Bubble count | `initBubbles()` → `repeat(28)` |
| Seaweed count | `initSeaweed()` → `repeat(14)` |
| Add a new fish type | Add entry to `FishType` enum |

## License

MIT
