# Brutal Impacts

## Hit particles config

This mod reads hit particle rules from JSON files in its data directory:

- By default: a `BrutalImpacts/` folder next to the mod JAR (commonly under your Hytale `UserData/Mods/` directory).
- Optional override: `-Dbrutalimpacts.dir=<path>` → data dir becomes `<path>/BrutalImpacts/`.

### Files

- `DefaultHitParticles_ReadOnly.json`
  - Bundled defaults for vanilla mobs (read-only example).
  - This file is overwritten when the mod updates.
- `USER_HitParticles.json`
  - Your personal overrides.
  - This file is created once and will NOT be overwritten on updates.
- `Weapons_ReadOnly.json`
  - Bundled defaults for source/weapon multipliers (read-only example).
  - This file is overwritten when the mod updates.
- `USER_Weapons.json`
  - Your personal weapon/source overrides.
  - This file is created once and will NOT be overwritten on updates.
- Any other `*.json` file (e.g. `MoreAnimals.json`, `CombatPack.json`)
  - For modders (or advanced users) to add/override hit-particle rules, weapon/source tuning, or both.

### Priority (override order)

When multiple JSON files define rules for the same mob/model id, priority is:

1. `USER_HitParticles.json` (highest)
2. Other `*.json` (sorted by filename)
3. `DefaultHitParticles_ReadOnly.json` (lowest)

Tip: if you need a modder file to win over another modder file, prefix the filename (e.g. `00_MoreAnimals.json`).

Weapon/source tuning follows the same priority, but uses the `sources` and `weapons` sections instead of `rules`.

### Hot reload

After editing any JSON file, you can reload rules in-game with:

`/brutalimpacts --reload`

### Default fallback particle (optional)

If no rule matches a model id, the mod uses a fallback particle system id.
You can change it in-game with:

`/brutalimpacts --particle <ParticleSystemId>`

### JSON format

```json
{
  "rules": [
    {
      "match": { "type": "exact|prefix|contains", "value": "Skeleton" },
      "particleSystemId": "BrutalImpacts_Hit_Bone_Default",
      "color": { "r": 235, "g": 224, "b": 192 },
      "scale": 1.0
    },
    {
      "match": { "type": "exact", "value": "Zombie" },
      "effects": [
        { 
          "particleSystemId": "BrutalImpacts_Hit_Blood_Default",
          "color": { "r": 255, "g": 0, "b": 0 },
          "scale": 2.0 
        },
        {
          "particleSystemId": "BrutalImpacts_Hit_Bone_Default",
          "color": { "r": 0, "g": 80, "b": 255 },
          "scale": 1.0 
        }
      ]
    }
  ]
}
```

#### Color rules

- If `"color"` is omitted: the effect inherits the runtime default color (if configured).
- If `"color": null`: the effect uses no tint (even if a runtime default color exists).
- If `"color": { "r": ..., "g": ..., "b": ... }`: the effect uses that RGB tint.

## Weapon/source tuning config

These values multiply the base runtime formula that already reacts to damage amount.

### Weapons JSON format

```json
{
  "sources": [
    { "kind": "projectile", "scaleMultiplier": 0.85, "particleMultiplier": 0.85 },
    { "kind": "melee", "scaleMultiplier": 1.05, "particleMultiplier": 1.0 },
    { "kind": "unarmed", "scaleMultiplier": 0.95, "particleMultiplier": 0.9 }
  ],
  "weapons": [
    {
      "match": { "type": "contains", "value": "sword" },
      "scaleMultiplier": 1.0,
      "particleMultiplier": 1.0
    }
  ]
}
```

### Source kinds

- `projectile`: projectile hits
- `melee`: entity hits with a valid `itemId` in hand
- `unarmed`: entity hits without a valid item in hand
- `other`: non-entity or fallback cases

### Matching rules

- `sources` and `weapons` are resolved independently, then multiplied together.
- `weapons.match.type` supports `exact`, `prefix`, and `contains`.
- Extra modder JSON files can include only `rules`, only `sources`/`weapons`, or both in the same file.
