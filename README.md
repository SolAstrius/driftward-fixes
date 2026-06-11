# Driftward Fixes

A small NeoForge **1.21.1** mixin mod carrying bug/compat fixes for the Driftward modpack.

It's a port of [vanutp's `liferooters-smp-fixes`](https://foxlab.dev/minecraft/liferooters-smp-fixes/)
(Fabric 1.20.1) to NeoForge 1.21.1, trimmed to the fixes whose target mods Driftward actually
ships.

## Fixes

| Mixin | Target mod | What it does |
|-------|-----------|--------------|
| `GrapplingHookEntityMixin` | Critters & Companions | `GrapplingHookEntity.tick()` calls `distanceToSqr(getOwner())` with no null-check; when the owner is gone (disconnect/death) it throws an NPE and can crash the server. This cancels the tick when there is no owner. *(Still present in C&C `neoforge-1.21.1-2.3.4`.)* |
| `BeeMovieMixin` (client) | Supplementaries | Strips leftover `§r`/`§0` formatting codes from the April-Fools "Bee Movie" easter-egg text so `Font.split` renders the scrolling text correctly. |

Not ported from the original: the Ashen Circlet heat-resistance patch (needs Spectrum + Trinkets,
neither in Driftward).

## Building

Requires JDK 21. The two target mods are compile-only dependencies and are **gitignored** — drop
their jars into `libs/` before building:

```
libs/crittersandcompanions.jar
libs/supplementaries.jar
```

Then:

```sh
./gradlew build
```

The mod jar lands at `build/libs/driftward-fixes-1.0.jar`.

No Mixin refmap is needed: NeoForge runs Mojang-mapped at runtime, so the mixin targets resolve
directly.

## Credits

Original fixes and mixin authorship: **vanutp** (`liferooters-smp-fixes`, MIT).
NeoForge 1.21.1 port: Sol Astrius. Licensed [MIT](LICENSE).
