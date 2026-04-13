# Brutal Impacts

Brutal Impacts is a Hytale server mod that adds **extra impact particles** when entities take damage.
It does **not** replace vanilla particles; it appends additional world particles to the damage event.

## Configuration (Hit Particles JSON)

On startup, the mod creates/uses a data directory named `BrutalImpacts/` **next to the mod JAR** and loads hit particle rules from `*.json` files inside it.

You can override the base directory with the JVM system property:

`-Dbrutalimpacts.dir=<path>` → data dir becomes `<path>/BrutalImpacts/`

### Files

- `DefaultHitParticles_ReadOnly.json`
  - Bundled defaults (read-only example).
  - Overwritten when the mod updates.
- `USER_HitParticles.json`
  - Your personal overrides.
  - Created once and never overwritten on updates.
- Any other `*.json` file (e.g. `MoreAnimals.json`)
  - Additional rules (useful for modpacks / servers).

### Priority (override order)

When multiple JSON files define rules for the same model id, priority is:

1. `USER_HitParticles.json` (highest)
2. Other `*.json` (sorted by filename)
3. `DefaultHitParticles_ReadOnly.json` (lowest)

Tip: if you need one modder file to win over another, prefix the filename (e.g. `00_MoreAnimals.json`).

### Hot reload

After editing any JSON file, reload rules in-game:

- `/brutalimpacts --reload`

### Default Particles

The default particles (the blood splatter) can be replaced using:

- `/brutalimpacts --particle <ParticleSystemId>`

## JSON format

The bundled README shipped into the data directory includes the full JSON schema + examples:

- `BrutalImpacts/README.md` (generated at runtime)
- `src/main/resources/brutalimpacts/README.md` (source copy in this repo)

## Developer API (optional)

Other mods/plugins can register rules at runtime via:

- `dev.hytalemodding.api.BrutalImpactsApi.hitParticles()`

## Resources

- Hytale Modding Guides: https://hytalemodding.dev
- Hytale Modding Discord: https://discord.gg/hytalemodding
- ScaffoldIt Plugin Docs: https://scaffoldit.dev
