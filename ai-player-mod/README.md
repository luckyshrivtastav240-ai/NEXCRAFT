# NEXCRAFT AI Player — early prototype

This is a separate Fabric mod for **Minecraft Java 1.20.1**. It is not yet bundled into the Android launcher.

## Current prototype scope
- `/nexcraftbot spawn`: spawns a named villager companion near the command user.
- `/nexcraftbot status`: reports whether the companion is active.
- `/nexcraftbot stop`: removes the companion.
- The companion attempts to navigate toward its owner periodically.

**Honest limitation:** this first scaffold uses a villager entity as a visible companion placeholder. It is not yet a player-model NPC, does not mine/build/fight on command, and has no voice/AI brain or launcher bridge. Those are subsequent implementation milestones.

## Build
From the repository root:
```bash
./gradlew -p ai-player-mod build
```
The resulting mod JAR is under `ai-player-mod/build/libs/`.
Install Fabric Loader and Fabric API for Minecraft 1.20.1, then place the JAR in that instance's `mods` folder.
