# NEXCRAFT AI Player — companion foundation

This is a separate Fabric mod for **Minecraft Java 1.20.1**. It is not yet bundled into the Android launcher.

## Implemented in this build
- `/nexcraftbot spawn`: spawns a named companion near the command user.
- `/nexcraftbot follow`: switches the companion to follow mode.
- `/nexcraftbot stay`: stops navigation and keeps the companion at its current location.
- `/nexcraftbot rename <name>`: renames the companion (1–32 characters).
- `/nexcraftbot status`: reports whether the companion is active and its current mode.
- `/nexcraftbot stop`: removes the companion.
- Follow navigation is updated periodically; Stay mode halts its navigation.

## Important limitations — not a finished AI player
The current companion is still a named **villager entity**, not a player-model NPC with an imported skin. Mining, gathering, crafting, building, farming, combat, Hindi/English voice, natural-language AI planning, and the Android launcher bridge are **not implemented**. Do not advertise these as working. This module currently supports Minecraft Java 1.20.1 with Fabric only.

## Build
From the repository root:
```bash
./gradlew -p ai-player-mod build
```
The resulting mod JAR is under `ai-player-mod/build/libs/`.

Install Fabric Loader and Fabric API for Minecraft 1.20.1, then place the JAR in that instance's `mods` folder. In a single-player world with cheats enabled, use the commands above.
