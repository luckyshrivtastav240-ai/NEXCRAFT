# NEXCRAFT AI Player — implementation specification

## Product goal

NEXCRAFT will offer two distinct ways to use AI while playing Minecraft Java:

1. **Control My Player** — the user keeps their current Minecraft character; an approved command planner translates voice/text commands into bounded in-game actions for that character.
2. **Spawn AI Player** — a separate named, skinnable companion appears in the loaded world and behaves like a player-like NPC. The user's own character remains under normal control.

These are separate modes and must not be presented as the same feature.

## User experience

- Launcher entry: **NEXCRAFT AI**.
- In-world companion action: **Spawn AI Player**.
- Companion default name: **NEXCRAFT BOT**; user can rename it and select/import a skin.
- Modes: Follow, Stay, Gather, Mine, Build, Farm, Defend, Fight, Explore, and Stop.
- Voice and text commands should support Hindi and English. Push-to-talk is the default; no always-on microphone.
- Show a visible status (Listening, Planning, Working, Paused, Needs help) and a one-tap Stop control.
- Ask for confirmation before destructive actions such as dropping/overwriting valuable items, changing world files, or risky commands.
- Preserve normal manual controls. The user can take control at any time.

## Required technical architecture

### A. Launcher-side Android integration

- Add an AI Player screen and settings entry to the NEXCRAFT launcher.
- Provide push-to-talk speech input using Android speech recognition and spoken responses using Android TextToSpeech.
- Provide a local command layer for simple actions and a configurable remote AI provider for natural-language planning.
- Never ship a private AI API key inside the APK. Remote inference must use a protected user-provided key or a separately operated backend.
- Keep voice capture opt-in and visibly active only while the user is holding/tapping the microphone.
- The launcher must report whether the companion mod is installed and compatible with the selected Minecraft instance. Do not show Spawn as available when the mod is absent or incompatible.

### B. Minecraft in-game companion mod

- A separate Fabric mod is required for the first implementation. It must be installed into the selected Minecraft instance like any other mod.
- The mod owns in-world spawning, companion rendering/skin/name, navigation, interaction, inventory rules, and action execution.
- The first single-player implementation may use a player-like companion NPC controlled by the mod. It must not falsely claim to be a real authenticated multiplayer player.
- A true server-side player bot for multiplayer is a separate server mod/service and must respect server permissions and anti-cheat rules.
- The mod exposes a small, versioned local command/status bridge to the launcher. Validate every incoming command and never execute arbitrary code from AI output.
- Use a strict allowlist of actions (move, follow, stop, inspect, gather, mine, craft, build, farm, defend, attack) with range, inventory, and world-state checks.
- Pathfinding must avoid hazards where possible and stop safely when no valid route/action exists.

### C. AI planning and execution

- Convert speech/text into structured intents, not direct game code.
- Planner returns typed action plans; the mod validates and executes one bounded step at a time.
- The game state supplied to the planner should be minimal and limited to relevant nearby blocks/entities, player state, inventory summary, and current task.
- Add timeouts, cancellation, action budgets, and a global emergency Stop.
- Basic commands should work locally where possible; natural-language planning may require internet.
- Do not claim that the AI learns permanently from gameplay unless an explicit, user-controlled memory system is implemented.

## Delivery phases

1. **Foundation and compatibility:** choose the first supported Minecraft Java version and loader; add the mod project, metadata, build workflow, compatibility detection, and launcher-side status model.
2. **Spawn MVP:** spawn/despawn one player-like companion in single-player; name and skin; follow/stay/stop; safe navigation.
3. **Player actions:** inspect, gather, mine, collect, craft, and basic inventory management.
4. **Survival behavior:** health/food awareness, retreat, defend, and fight with clear user settings.
5. **Build and farming:** bounded blueprints, block placement validation, crop care, and resource planning.
6. **Voice and natural-language planning:** launcher push-to-talk, Hindi/English command parsing, confirmation UI, and task progress.
7. **Optional multiplayer server companion:** separate server-side implementation after single-player stability and server permission design.

## Acceptance criteria

- The companion visibly spawns in the same loaded single-player world, can be despawned, and does not replace or hijack the user's character.
- Follow and Stop work reliably; Stop interrupts any active task immediately.
- The companion cannot modify blocks or inventory outside its approved action/task bounds.
- Unsupported Minecraft versions or missing mod dependencies produce a clear compatibility message instead of a non-working Spawn button.
- Launcher and mod communicate through a documented, versioned interface.
- Automated build and tests pass before an APK/mod artifact is called ready.

## Important constraints

- NEXCRAFT is an Android launcher, not the Minecraft game engine. The launcher alone cannot create an in-world player.
- Fabric/Forge/NeoForge mods are tied to Minecraft versions and loader APIs. Supporting every Minecraft version at once is not realistic; compatibility must be expanded and tested version by version.
- A visual NPC is not automatically a genuine multiplayer player. Multiplayer support requires server-side code and server authorization.
- Do not bundle API secrets in the APK, promise always-on background play, or claim untested gameplay behavior.
