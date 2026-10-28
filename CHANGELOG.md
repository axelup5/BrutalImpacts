# Brutal Impacts v2.5.0

## Hytale targets

Verified against the official Maven metadata on 2026-10-27:

| Git branch | Patchline | Server dependency | Manifest compatibility |
| --- | --- | --- | --- |
| `dev` | `release` | `0.7.4` | `>=0.7.4 <0.8.0` |
| `pre-release` | `pre-release` | `0.8.0-pre.2.1` | `>=0.8.0-pre.2.1 <0.8.0` |

Dependencies are pinned in `settings.gradle.kts` instead of using `latest`, so rebuilding a version does not silently change its server API. The pre-release range deliberately excludes the final 0.8.0 release, which needs its own compatibility check.

## Changes

- Updated the project name and plugin manifest to 2.5.0 for both patchlines.
- Updated the default model rule from `Spectre_Void` to `Void_Spectre`, matching Hytale's renamed model. Existing effect colors, scales and offsets are preserved.
- Enabled compiler diagnostics for both deprecated APIs and APIs marked for removal.
- Preserved explicit command permissions, strict JSON parsing, particle prediction handling, UI settings and persistent versioned JAR archives.
- Preserved each branch's public offset API: `dev` uses JOML `Vector3f`; `pre-release` retains its immutable `ParticleOffset` record.

## Local and GitHub reconciliation

Before this update, local `dev` was three commits ahead and two commits behind `origin/dev`. The local sources already included the remote 2.4.0 update, plus newer command permissions, a bounded server range and persistent release archives. The remaining remote additions were generated `bin/` output. A merge preserves both histories and the newer local source tree, with `bin/` still excluded from version control.

Local `pre-release` already contained `origin/pre-release` and an additional release-archive commit. Its offset API is kept when applying the 2.5.0 migration.

## API and asset migration review

The migration checks the actual server artifacts with Java 25 and `-Xlint:deprecation -Xlint:removal`, including damage metadata, `WorldParticle`, predicted particle delivery, ECS system dependencies, `InventoryComponent`, `PlayerRef`, command permissions and custom UI codecs. This is a check of APIs used by this mod, not a guarantee about future Hytale releases.

Hytale's Update 7 and Update 8 notes also describe chunk/section API removals, world access changes and a new abstract method for custom permission providers. Brutal Impacts does not implement those providers or use the removed chunk/world methods, so those changes do not require adapters here.

The existing `Goblin_Hermit` and `Goblin_Ogre` default rules are retained for custom packs; neither model is present in the installed 0.7.4 vanilla assets. `Goblin_Miner` remains a valid model ID: the similarly named attitude role was renamed, not this model.

Personal `USER_*.json` files are never overwritten. If a personal rule targets `Spectre_Void`, change its match value to `Void_Spectre`. The bundled read-only defaults refresh when the updated mod starts.

## Build and validation

Use Java 25 and the repository's Gradle wrapper from the desired branch:

```powershell
.\gradlew.bat --gradle-user-home .gradle-user-home --no-daemon --console=plain clean build
```

The main JAR is copied to `releases/` and survives `clean`. Release and pre-release JARs have distinct names. Install only the JAR matching the server patchline.

The repository has no existing automated test suite. A successful `build` verifies compilation and packaging; Gradle's `test NO-SOURCE` does not mean gameplay tests ran. Combat effects, attacker prediction and UI behavior still require an in-game smoke test with a matching client and server. The locally installed pre-release server was 0.7.0-pre.2 during the migration and must be updated separately before testing the new pre-release build.

The release JAR additionally passed a Java smoke check against the actual 0.7.4 server artifact: 266 hit rules and 17 weapon/source rules loaded; the renamed spectre retained its effects and offset; personal files and override priority survived a layout refresh; runtime tuning round-tripped; malformed JSON was rejected; Hytale's own SemVer parser accepted 0.7.4 and rejected 0.7.3 and 0.8.0. All 19 custom particle systems, 39 spawners, texture references and packaged UI resources resolved against the mod JAR and installed 0.7.4 assets. No Hytale server classes were bundled in the mod JAR.

## Official references

- [Release Maven metadata](https://maven.hytale.com/release/com/hypixel/hytale/Server/maven-metadata.xml)
- [Pre-release Maven metadata](https://maven.hytale.com/pre-release/com/hypixel/hytale/Server/maven-metadata.xml)
- [Update 7 modder-facing patch notes](https://hytale.com/news/2026/10/modder-facing-patch-notes-update-7)
- [Update 8 pre-release patch notes](https://hytale.com/news/2026/10/pre-release-patch-notes-update-8)
