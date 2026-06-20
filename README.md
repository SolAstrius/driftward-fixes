# Driftward Fixes

A small NeoForge **1.21.1** mixin mod carrying bug/compat fixes and integrations for the Driftward
modpack.

It started as a port of [vanutp's `liferooters-smp-fixes`](https://foxlab.dev/minecraft/liferooters-smp-fixes/)
(Fabric 1.20.1) and has since grown its own Driftward-specific patches and cross-mod integrations.

## Fixes

| Mixin | Target mod(s) | What it does |
|-------|---------------|--------------|
| `GrapplingHookEntityMixin` | Critters & Companions | `GrapplingHookEntity.tick()` calls `distanceToSqr(getOwner())` with no null-check; when the owner is gone (disconnect/death) it throws an NPE and can crash the server. Cancels the tick when there is no owner. *(Still present in C&C `neoforge-1.21.1-2.3.4`.)* |
| `BeeMovieMixin` (client) | Supplementaries | Strips leftover `§r`/`§0` formatting codes from the April-Fools "Bee Movie" easter-egg text so `Font.split` renders the scrolling text correctly. |
| `BalloonTemperatureMixin` + `BalloonAccessor` | Create Aeronautics + Thermoo (+ Sable) | Makes hot-air/steam balloon lift react to the ambient Thermoo temperature where the airship actually is (resolved through Sable's sub-level pose): colder air = more lift, hotter = less. Clamped and configurable (see Config). Levitite uses a separate path and is untouched. |
| `ChatRelayMixin` + `TgbridgeChatRelay`/`TgbridgeChatForwarder` | tgbridge + StyledChat | StyledChat (via Sinytra Connector) redirects the chat decorator and shadows NeoForge's `ServerChatEvent`, so tgbridge's native listener never fires and player chat never reaches Telegram. Re-feeds public chat into tgbridge's public API from `broadcastChatMessage`. No-ops unless both mods are present (otherwise messages would be missing or doubled). |
| `CactusGlassPaneMixin` | vanilla | `CactusBlock.canSurvive()` pops the cactus if any horizontal neighbour is solid. Treats `#c:glass_panes` blocks as non-solid for that check, so a cactus can stand inside glass-pane walls (greenhouse builds). Every other solid neighbour, plus the sand/lava checks, are untouched. |
| `SpoutFillPurityMixin` | Create + Thirst Was Taken | TWT propagates water "purity" onto Create-basin-brewed tea fluids but not onto items filled by a Create Spout, so automated bottling silently resets purity. Stamps the source fluid's purity onto the resulting water-container item. |

Not ported from the original: the Ashen Circlet heat-resistance patch (needs Spectrum + Trinkets,
neither in Driftward).

## Mixin configs

The mixins are split across three configs so optional integrations don't fail the load when their
target mods are absent:

- `driftward-fixes.mixins.json` — **required**: `ChatRelayMixin`, `GrapplingHookEntityMixin`, `SpoutFillPurityMixin`, and (client) `BeeMovieMixin`.
- `driftward-fixes.aeronautics.mixins.json` — optional: `BalloonAccessor`, `BalloonTemperatureMixin`.
- `driftward-fixes.gameplay.mixins.json` — optional: `CactusGlassPaneMixin`.

## Config

Server config (`balloon_temperature` section) tunes the Create Aeronautics balloon-lift behaviour:
master toggle, reference temperature (multiplier = 1.0), sensitivity per °C, and min/max multiplier
clamps. Defaults: reference 20 °C, +1.25% lift per degree colder, clamped to 0.7×–1.5×.

## Building

Requires JDK 21. The target mods are compile-only dependencies and are **gitignored** — drop their
jars into `libs/` before building:

```
libs/crittersandcompanions.jar
libs/supplementaries.jar
libs/create.jar
libs/thirst.jar
libs/aeronautics.jar
libs/sable.jar
libs/sable-companion.jar
libs/thermoo.jar
libs/tgbridge.jar          # the NeoForge build (adventure relocated to tgbridge.shaded.*)
```

Then:

```sh
./gradlew build
```

The mod jar lands at `build/libs/driftward-fixes-1.8.jar`.

No Mixin refmap is needed: NeoForge runs Mojang-mapped at runtime, so the mixin targets resolve
directly. (Create/Thirst/tgbridge mixins use `remap = false` since their own names are already
stable at runtime.)

## Credits

Original fixes and mixin authorship: **vanutp** (`liferooters-smp-fixes`, MIT).
NeoForge 1.21.1 port and Driftward-specific patches: Sol Astrius. Licensed [MIT](LICENSE).
