# Brutal Impacts v2.5.0

## Hytale targets

Verified against the official Maven metadata and installed servers on 2026-10-30:

| Git branch | Patchline | Server dependency | Manifest compatibility |
| --- | --- | --- | --- |
| `dev` | `release` | `0.7.5` | `>=0.7.5 <0.8.0` |
| `pre-release` | `pre-release` | `0.8.0-pre.3` | `>=0.8.0-pre.3 <0.8.0` |

Dependencies are pinned in `settings.gradle.kts` instead of using `latest`, so rebuilding a version does not silently change its server API. The pre-release range deliberately excludes the final 0.8.0 release, which needs its own compatibility check.

## Changes

- Updated the project name and plugin manifest to 2.5.0 for both patchlines.
- Updated the default model rule from `Spectre_Void` to `Void_Spectre`, matching Hytale's renamed model. Existing effect colors, scales and offsets are preserved.
- Enabled compiler diagnostics for both deprecated APIs and APIs marked for removal.
- Preserved explicit command permissions, JSON parsing, particle prediction handling, UI settings and persistent versioned JAR archives.
- Preserved each branch's public offset API: `dev` uses JOML `Vector3f`; `pre-release` retains its immutable `ParticleOffset` record.

## Particle rules promoted on 2026-10-30

Promoted the 24 completed exact-match rules from the installed `USER_HitParticles.json` into the bundled defaults on both branches. This adds 23 model IDs and replaces the complete `Goblin_Miner` rule with the user's latest values. Defaults now contain 288 unique model rules, sorted alphabetically by ID. Existing rules outside that selection keep their original values.

The imported rules cover Voidtaken bears and tentacles, Coffer goblins, Feastmaster, Gobliterator phases, Guardian, Burner, Spectator, Skeleton Elite, cave spiderlings and Void Anomaly crystals. Colors, scales, offsets, fixed-scale settings and effect order are copied exactly; no inferred family rules are added. The installed user files remain untouched.

The source USER snapshot has SHA-256 `17518c1cb1fce38fa0c75d0ff84dd7ffd5bef3b885c2ef0adebf14325cdac86f`. Both distributable JARs contain identical defaults. The mod remains version 2.5.0.

## Local and GitHub reconciliation

Before this update, local `dev` was three commits ahead and two commits behind `origin/dev`. The local sources already included the remote 2.4.0 update, plus newer command permissions, a bounded server range and persistent release archives. The remaining remote additions were generated `bin/` output. A merge preserves both histories and the newer local source tree, with `bin/` still excluded from version control.

Local `pre-release` already contained `origin/pre-release` and an additional release-archive commit. Its offset API is kept when applying the 2.5.0 migration.

## API and asset migration review

The migration checks the actual server artifacts with Java 25 and `-Xlint:deprecation -Xlint:removal`, including damage metadata, `WorldParticle`, predicted particle delivery, ECS system dependencies, `InventoryComponent`, `PlayerRef`, command permissions and custom UI codecs. This is a check of APIs used by this mod, not a guarantee about future Hytale releases.

Hytale's Update 7 and Update 8 notes also describe chunk/section API removals, world access changes and a new abstract method for custom permission providers. Brutal Impacts does not implement those providers or use the removed chunk/world methods, so those changes do not require adapters here.

The existing `Goblin_Hermit` and `Goblin_Ogre` default rules are retained for custom packs. `Goblin_Miner` remains a valid model ID: the similarly named attitude role was renamed, not this model.

Personal `USER_*.json` files are never overwritten. If a personal rule targets `Spectre_Void`, change its match value to `Void_Spectre`. The bundled read-only defaults refresh when the updated mod starts.

## Build and validation

Use Java 25 and the repository's Gradle wrapper from the desired branch:

```powershell
.\gradlew.bat --gradle-user-home .gradle-user-home --no-daemon --console=plain clean build
```

The main JAR is copied to `releases/` and survives `clean`. Release and pre-release JARs have distinct names. Install only the JAR matching the server patchline.

Both branches passed `clean build` against their pinned server artifacts with no deprecation or removal warnings. Gradle's `test NO-SOURCE` does not mean gameplay tests ran.

Each JAR passed a Java smoke check against its own API and asset pack: all 288 defaults were unique and sorted; all 24 promoted JSON objects and their runtime effects matched the source USER snapshot, including RGB, scale, offset and fixed-scale values. A clean configuration loaded 289 hit rules (288 defaults plus the bundled USER example) and 17 weapon/source rules. Personal files and override priority survived a layout refresh, runtime tuning round-tripped, and structurally malformed JSON was rejected.

Both channels resolved all 19 custom particle systems, 39 spawners, texture references and packaged UI resources against their matching installed assets. No Hytale server classes were bundled in the mod JARs. Hytale's SemVer parser accepted each target and rejected the previous target and the next stable 0.8.0 release.

Both official servers (0.7.5 and 0.8.0-pre.3) completed isolated loading with their corresponding mod JAR and asset pack, exiting with code 0. These runs used `--bare --shutdown-after-validate`, with no listening ports or worlds. The servers emitted warnings about vanilla assets. This checks plugin setup and normal asset loading, rather than exhaustive validation of the chunks in every bundled vanilla instance. Combat visuals, attacker prediction and UI interaction still require an in-game check with a matching client and server.

## Official references

- [Release Maven metadata](https://maven.hytale.com/release/com/hypixel/hytale/Server/maven-metadata.xml)
- [Pre-release Maven metadata](https://maven.hytale.com/pre-release/com/hypixel/hytale/Server/maven-metadata.xml)
- [Update 7 modder-facing patch notes](https://hytale.com/news/2026/10/modder-facing-patch-notes-update-7)
- [Update 8 pre-release patch notes](https://hytale.com/news/2026/10/pre-release-patch-notes-update-8)
