# Auditoría de partículas por modelo

Generada: 2026-10-27T23:50:25-03:00. Servidor instalado: **0.7.4**.

Se leyeron **504 modelos**, **1148 roles NPC**, **265 reglas por defecto**, **1 reglas de usuario** y **0 reglas adicionales**.

**263 modelos cubiertos; 241 sin regla personalizada**. Sin regla se usa el fallback; no significa ausencia total de partículas.

## Cómo interpretar este informe

- La cobertura usa el orden real: USER, otros JSON por nombre y defaults; la primera coincidencia gana. No se da prioridad automática a `exact` sobre `prefix`/`contains`.
- El `Parent` de un modelo de Hytale **no hereda reglas del mod**. El modelo derivado necesita una coincidencia propia.
- Las referencias de `Appearance` de roles son evidencia de uso NPC; no demuestran que se pueda dañar ni que aparezca normalmente. Incluyen templates abstractos.
- Misma geometría/textura corporal es una propuesta para reutilizar efectos, no una garantía: revisar attachments, escala, offsets y el estilo deseado en el juego.
- Los modelos sin referencias directas no se descartan: un plugin o un asset puede usarlos dinámicamente. Marcadores, previews, proyectiles y cinemáticas se listan aparte.
- Esta es una foto de los assets instalados; no demuestra en qué actualización se añadió cada modelo.

## Fuentes

- ZIP oficial leído: `C:\Users\axelu\AppData\Roaming\Hytale\install\release\package\game\latest\Assets.zip`.
- Defaults: [DefaultHitParticles_ReadOnly.json](../src/main/resources/brutalimpacts/DefaultHitParticles_ReadOnly.json).
- USER leído: `C:\Users\axelu\AppData\Roaming\Hytale\UserData\Mods\BrutalImpacts\USER_HitParticles.json`.
- [El JSON de auditoría](particle-model-audit.json) incluye la ruta dentro del ZIP, padres, geometría, textura y campos `Appearance` para comprobar cada caso.

Defaults instalados y del proyecto: **iguales** al comparar las reglas.

## Resumen de pendientes

| Tipo | Sin regla |
| --- | ---: |
| auxiliar | 7 |
| cinematica_o_prueba | 22 |
| criatura | 51 |
| desplegable | 5 |
| proyectil | 107 |
| sin_referencia_npc_directa | 49 |

## Pendientes observados en tus pruebas

Se contaron únicamente mensajes `targetModelAssetId` en los logs indicados. La cantidad mide apariciones en el log, no daño ni eventos únicos.

| targetModelAssetId | Mensajes | Tipo |
| --- | ---: | --- |
| `Goblin_Gobliterator_Phase2` | 7 | criatura |
| `Goblin_Gobliterator_Phase1` | 5 | criatura |
| `Bear_Voidtaken_Tentacle` | 4 | criatura |
| `Coffer_Goblin_Guardian` | 4 | criatura |
| `Coffer_Goblin_Lobber` | 4 | criatura |
| `Goblin_Glutton` | 4 | criatura |
| `Goblin_Guardian` | 4 | criatura |
| `Coffer_Goblin_Glutton` | 3 | criatura |
| `Goblin_Gobliterator_Phase3` | 3 | criatura |
| `Bear_Grizzly_Voidtaken` | 1 | criatura |
| `Bear_Voidtaken_Tentacle_Marker` | 1 | auxiliar |
| `Coffer_Goblin_Miner` | 1 | criatura |
| `Coffer_Goblin_Scrapper` | 1 | criatura |
| `Dungeon_Feastmaster_Boss` | 1 | criatura |
| `Void_Tentacle` | 1 | criatura |
| `Voidtaken_Goblin` | 1 | criatura |

## Casos que conviene priorizar

- `Bear_Grizzly_Voidtaken` y `Bear_Grizzly_Voidtaken2` heredan de `Bear_Grizzly`, pero usan texturas Voidtaken diferentes. La receta del oso común es una base estructural; el color del pelo y el efecto de corrupción necesitan decisión visual.
- `Void_Tentacle`, `Bear_Voidtaken_Tentacle` y `Bear_Voidtaken_Tentacle_Model` comparten geometría y textura. Diseñar una receta de tentáculo y duplicarla con matches exactos permite cubrir los tres; revisar la escala menor de las variantes del oso.
- `Bear_Voidtaken_Tentacle_Marker` y `Bear_Voidtaken_Tentacle_Marker_Model` usan `Empty.blockymodel`. Son auxiliares y no deben recibir automáticamente la receta del tentáculo por compartir el prefijo.
- `Coffer_Goblin_Lobber`, `Coffer_Goblin_Miner` y `Coffer_Goblin_Scrapper` heredan la geometría/textura de sus correspondientes goblins ya configurados. Son los primeros candidatos para copiar una receta existente.
- `Goblin_Burner`/`Coffer_Goblin_Burner`, `Goblin_Guardian`/`Coffer_Goblin_Guardian`/`Dungeon_Goblin_Mace`, y `Goblin_Glutton`/`Coffer_Goblin_Glutton` necesitan recetas base nuevas. `Dungeon_Feastmaster_Boss` deriva de Glutton, con otra textura y escala.
- Las fases y transiciones del Gobliterator comparten su cuerpo mecánico. Sus piezas, barril y piloto merecen revisión separada; una regla general `contains: Goblin` también alcanzaría esos IDs.
- `Dungeon_Goblin_Lobber` es un rol cuya `Modify.Appearance` apunta a `Goblin_Lobber`; ese rol ya usa un ID configurado. `Coffer_Goblin_Lobber`, en cambio, sí es un modelo diferente que requiere otra coincidencia.

## Familias con la misma geometría y textura corporal

Una receta nueva puede servir de base para varias reglas exactas de estas familias. Se incluyen también modelos sin referencia NPC directa; su aparición en el juego debe comprobarse.

| Familia de modelos | Sin regla |
| --- | --- |
| `Boy_Trail`, `NPC_Elf`, `NPC_Santa`, `Player`, `Warrior_Quest` | `Boy_Trail`, `NPC_Elf`, `NPC_Santa`, `Player`, `Warrior_Quest` |
| `NPC_Sound_Shoe`, `Outlander`, `Outlander_Berserker`, `Outlander_Cultist`, `Outlander_Hunter`, `Outlander_Marauder`, `Outlander_Peon`, `Outlander_Priest`, `Outlander_Sorcerer`, `Outlander_Stalker` | `NPC_Sound_Shoe`, `Outlander` |
| `Larva_Void`, `Wurmling_Frost` | `Wurmling_Frost` |
| `Goblin_Gobliterator_Phase1`, `Goblin_Gobliterator_Phase2`, `Goblin_Gobliterator_Phase3`, `Goblin_Gobliterator_SawTransition`, `Goblin_Gobliterator_Transition_Phase3` | `Goblin_Gobliterator_Phase1`, `Goblin_Gobliterator_Phase2`, `Goblin_Gobliterator_Phase3`, `Goblin_Gobliterator_SawTransition`, `Goblin_Gobliterator_Transition_Phase3` |
| `Bramblekin`, `Bramblekin_Shaman` | `Bramblekin`, `Bramblekin_Shaman` |
| `Feran`, `Feran_Civilian`, `Feran_Sharptooth` | `Feran` |
| `Coffer_Goblin_Scrapper`, `Dungeon_Goblin_Food_Carrier`, `Dungeon_Goblin_Scrapper_Enrage`, `Dungeon_Goblin_Shroom_Carrier`, `Goblin`, `Goblin_Lab_Scrapper`, `Goblin_Merchant`, `Goblin_Scrapper`, `Goblin_Spectator`, `Temple_Goblin_Scrapper` | `Coffer_Goblin_Scrapper`, `Dungeon_Goblin_Food_Carrier`, `Dungeon_Goblin_Scrapper_Enrage`, `Dungeon_Goblin_Shroom_Carrier`, `Goblin_Lab_Scrapper`, `Goblin_Merchant`, `Goblin_Spectator`, `Temple_Goblin_Scrapper` |
| `Coffer_Goblin_Miner`, `Goblin_Miner` | `Coffer_Goblin_Miner` |
| `Coffer_Goblin_Burner`, `Goblin_Burner` | `Coffer_Goblin_Burner`, `Goblin_Burner` |
| `Coffer_Goblin_Lobber`, `Goblin_Lobber` | `Coffer_Goblin_Lobber` |
| `Coffer_Goblin_Guardian`, `Dungeon_Goblin_Mace`, `Goblin_Guardian` | `Coffer_Goblin_Guardian`, `Dungeon_Goblin_Mace`, `Goblin_Guardian` |
| `Coffer_Goblin_Glutton`, `Goblin_Glutton` | `Coffer_Goblin_Glutton`, `Goblin_Glutton` |
| `Klops`, `Klops_Merchant`, `Klops_Miner` | `Klops` |
| `Kweebec_Sapling`, `Kweebec_Sapling_Christmas_Blue`, `Kweebec_Sapling_Orange` | `Kweebec_Sapling_Christmas_Blue` |
| `Kweebec_Sapling_Christmas_Pink`, `Kweebec_Sapling_Pink` | `Kweebec_Sapling_Christmas_Pink` |
| `Kweebec_Sapling_Christmas_Green`, `Kweebec_Sapling_HardHat`, `Kweebec_Sapling_Razorleaf`, `Kweebec_Sapling_Yellow` | `Kweebec_Sapling_Christmas_Green`, `Kweebec_Sapling_HardHat`, `Kweebec_Sapling_Yellow` |
| `Saurian`, `Saurian_Warrior` | `Saurian`, `Saurian_Warrior` |
| `Slothian`, `Slothian_Villager` | `Slothian`, `Slothian_Villager` |
| `Trork_Christmas`, `Trork_Shaman` | `Trork_Christmas` |
| `Tuluk`, `Tuluk_Fisherman` | `Tuluk`, `Tuluk_Fisherman` |
| `Horse`, `Horse_Tamed` | `Horse_Tamed` |
| `Void_Anomaly_Crystal_Large`, `Void_Anomaly_Crystal_Medium`, `Void_Anomaly_Crystal_Small`, `Void_Anomaly_Stutter` | `Void_Anomaly_Crystal_Large`, `Void_Anomaly_Crystal_Medium`, `Void_Anomaly_Crystal_Small`, `Void_Anomaly_Stutter` |
| `Bear_Voidtaken_Tentacle`, `Bear_Voidtaken_Tentacle_Model`, `Void_Tentacle` | `Bear_Voidtaken_Tentacle`, `Bear_Voidtaken_Tentacle_Model`, `Void_Tentacle` |
| `Deer_Doe`, `Model_Deer_Stag`, `Reindeer_Christmas` | `Reindeer_Christmas` |

## Pendientes: criatura (51)

| targetModelAssetId | Parent | Base sugerida | Revisión |
| --- | --- | --- | --- |
| `Bear_Grizzly_Voidtaken` | `Bear_Grizzly` | `Bear_Grizzly` | Diseñar o ajustar: cambia geometría o textura; cambia escala del modelo |
| `Bear_Voidtaken_Tentacle` | `Void_Tentacle` | `—` | Diseño nuevo; sin base configurada verificable |
| `Bear_Voidtaken_Tentacle_Model` | `Void_Tentacle` | `—` | Diseño nuevo; sin base configurada verificable |
| `Boy_Trail` | `Player` | `—` | Diseño nuevo; sin base configurada verificable |
| `Bramblekin` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Bramblekin_Shaman` | `Bramblekin` | `—` | Diseño nuevo; sin base configurada verificable |
| `Coffer_Goblin_Burner` | `Goblin_Burner` | `Goblin` | Diseñar o ajustar: cambia geometría o textura; cambia escala del modelo |
| `Coffer_Goblin_Glutton` | `Goblin_Glutton` | `—` | Diseño nuevo; sin base configurada verificable |
| `Coffer_Goblin_Guardian` | `Goblin_Guardian` | `Goblin` | Diseñar o ajustar: cambia geometría o textura; cambia escala del modelo |
| `Coffer_Goblin_Lobber` | `Goblin_Lobber` | `Goblin_Lobber` | Copiar como punto de partida; misma geometría/textura |
| `Coffer_Goblin_Miner` | `Goblin_Miner` | `Goblin_Miner` | Copiar como punto de partida; misma geometría/textura |
| `Coffer_Goblin_Scrapper` | `Goblin_Scrapper` | `Goblin_Scrapper` | Copiar como punto de partida; misma geometría/textura |
| `Corgi` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Dragon_Fire` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Dragon_Frost` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Dungeon_Feastmaster_Boss` | `Goblin_Glutton` | `—` | Diseño nuevo; sin base configurada verificable |
| `Dungeon_Goblin_Food_Carrier` | `Goblin_Scrapper` | `Goblin_Scrapper` | Copiar como punto de partida; misma geometría/textura |
| `Dungeon_Goblin_Mace` | `Goblin_Guardian` | `Goblin` | Diseñar o ajustar: cambia geometría o textura; cambia escala del modelo |
| `Dungeon_Goblin_Scrapper_Enrage` | `Goblin_Scrapper` | `Goblin_Scrapper` | Copiar como punto de partida; misma geometría/textura |
| `Dungeon_Goblin_Shroom_Carrier` | `Goblin_Scrapper` | `Goblin_Scrapper` | Copiar como punto de partida; misma geometría/textura |
| `Eel_Moray` | `Bluegill` | `Bluegill` | Diseñar o ajustar: cambia geometría o textura; cambia escala del modelo |
| `Goblin_Burner` | `Goblin` | `Goblin` | Diseñar o ajustar: cambia geometría o textura; cambia escala del modelo |
| `Goblin_Glutton` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Goblin_Gobliterator_Bottom_Phase3` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Goblin_Gobliterator_Explosive_Barrel` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Goblin_Gobliterator_Phase1` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Goblin_Gobliterator_Phase2` | `Goblin_Gobliterator_Phase1` | `—` | Diseño nuevo; sin base configurada verificable |
| `Goblin_Gobliterator_Phase3` | `Goblin_Gobliterator_Phase1` | `—` | Diseño nuevo; sin base configurada verificable |
| `Goblin_Gobliterator_SawTransition` | `Goblin_Gobliterator_Phase1` | `—` | Diseño nuevo; sin base configurada verificable |
| `Goblin_Gobliterator_Transition_Phase3` | `Goblin_Gobliterator_Phase2` | `—` | Diseño nuevo; sin base configurada verificable |
| `Goblin_Guardian` | `Goblin` | `Goblin` | Diseñar o ajustar: cambia geometría o textura; cambia escala del modelo |
| `Goblin_Lab_Scrapper` | `Goblin_Scrapper` | `Goblin_Scrapper` | Copiar como punto de partida; misma geometría/textura |
| `Goblin_Pilot` | `Goblin` | `Goblin` | Diseñar o ajustar: cambia geometría o textura |
| `Goblin_Scientist` | `Goblin` | `Goblin` | Diseñar o ajustar: cambia geometría o textura |
| `Goblin_Spectator` | `Goblin` | `Goblin` | Copiar como punto de partida; misma geometría/textura; cambia escala del modelo |
| `Golem_Guardian_Void` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Player` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Scarak_Broodmother_Young` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Skeleton_Elite` | `Player` | `—` | Diseño nuevo; sin base configurada verificable |
| `Slothian` | `Player` | `—` | Diseño nuevo; sin base configurada verificable |
| `Spider_Cave_Spiderling` | `Spider` | `Spider` | Diseñar o ajustar: cambia geometría o textura |
| `Temple_Goblin_Scrapper` | `Goblin_Scrapper` | `Goblin_Scrapper` | Copiar como punto de partida; misma geometría/textura |
| `Temple_Mithril_Guard` | `Player` | `—` | Diseño nuevo; sin base configurada verificable |
| `Tuluk_Fisherman` | `Tuluk` | `—` | Diseño nuevo; sin base configurada verificable |
| `Void_Anomaly_Crystal_Large` | `Void_Anomaly_Crystal_Medium` | `—` | Diseño nuevo; sin base configurada verificable |
| `Void_Anomaly_Crystal_Medium` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Void_Anomaly_Crystal_Small` | `Void_Anomaly_Crystal_Medium` | `—` | Diseño nuevo; sin base configurada verificable |
| `Void_Anomaly_Stutter` | `Void_Anomaly_Crystal_Small` | `—` | Diseño nuevo; sin base configurada verificable |
| `Void_Tentacle` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Voidtaken_Goblin` | `Goblin_Scrapper` | `Goblin_Scrapper` | Diseñar o ajustar: cambia geometría o textura |
| `Warrior_Quest` | `Player` | `—` | Diseño nuevo; sin base configurada verificable |

## Pendientes: sin_referencia_npc_directa (49)

| targetModelAssetId | Parent | Base sugerida | Revisión |
| --- | --- | --- | --- |
| `Bear_Grizzly_Voidtaken2` | `Bear_Grizzly` | `Bear_Grizzly` | Diseñar o ajustar: cambia geometría o textura; cambia escala del modelo |
| `Boat` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Dragon_Void` | `Dragon_Frost` | `—` | Diseño nuevo; sin base configurada verificable |
| `Feran` | `Player` | `Feran_Civilian` | Diseñar o ajustar: cambia geometría o textura |
| `Goblin_Boss` | `Goblin` | `Goblin` | Diseñar o ajustar: cambia geometría o textura |
| `Goblin_Merchant` | `Goblin` | `Goblin` | Diseñar o ajustar: cambia geometría o textura |
| `Grooble` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Horse_Tamed` | `—` | `Horse` | Diseñar o ajustar: cambia geometría o textura |
| `Kitten` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Klops` | `Player` | `Klops_Merchant` | Diseñar o ajustar: cambia geometría o textura |
| `Kweebec_Sapling_Brown` | `Kweebec_Sapling` | `Kweebec_Sapling` | Diseñar o ajustar: cambia geometría o textura |
| `Kweebec_Sapling_Christmas_Blue` | `Kweebec_Sapling` | `Kweebec_Sapling` | Diseñar o ajustar: cambia geometría o textura |
| `Kweebec_Sapling_Christmas_Green` | `Kweebec_Sapling` | `Kweebec_Sapling_Razorleaf` | Diseñar o ajustar: cambia geometría o textura |
| `Kweebec_Sapling_Christmas_Pink` | `Kweebec_Sapling` | `Kweebec_Sapling_Pink` | Diseñar o ajustar: cambia geometría o textura |
| `Kweebec_Sapling_Green` | `Kweebec_Sapling` | `Kweebec_Sapling` | Diseñar o ajustar: cambia geometría o textura |
| `Kweebec_Sapling_HardHat` | `Kweebec_Sapling` | `Kweebec_Sapling_Razorleaf` | Diseñar o ajustar: cambia geometría o textura |
| `Kweebec_Sapling_Red` | `Kweebec_Sapling` | `Kweebec_Sapling` | Diseñar o ajustar: cambia geometría o textura |
| `Kweebec_Sapling_Yellow` | `Kweebec_Sapling` | `Kweebec_Sapling_Razorleaf` | Diseñar o ajustar: cambia geometría o textura |
| `Kweebec_Sproutling_Blue` | `Kweebec_Sapling` | `Kweebec_Sapling` | Diseñar o ajustar: cambia geometría o textura |
| `Kweebec_Sproutling_Lime` | `Kweebec_Sapling` | `Kweebec_Sapling` | Diseñar o ajustar: cambia geometría o textura |
| `Minecart` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Model_Bee_Swarm` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Mushee` | `Cactee` | `Cactee` | Diseñar o ajustar: cambia geometría o textura |
| `NPC_Elf` | `Player` | `—` | Diseño nuevo; sin base configurada verificable |
| `NPC_Santa` | `Player` | `—` | Diseño nuevo; sin base configurada verificable |
| `NPC_Sound_Shoe` | `Outlander` | `Outlander_Berserker` | Diseñar o ajustar: cambia geometría o textura |
| `Necromancer_Void` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Outlander` | `Player` | `Outlander_Berserker` | Diseñar o ajustar: cambia geometría o textura |
| `Reindeer_Christmas` | `Model_Deer_Stag` | `Model_Deer_Stag` | Diseñar o ajustar: cambia geometría o textura |
| `Saurian` | `Spawn_Void` | `Spawn_Void` | Diseñar o ajustar: cambia geometría o textura |
| `Saurian_Hunter` | `Saurian` | `Spawn_Void` | Diseñar o ajustar: cambia geometría o textura |
| `Saurian_Rogue` | `Saurian` | `Spawn_Void` | Diseñar o ajustar: cambia geometría o textura |
| `Saurian_Warrior` | `Saurian` | `Spawn_Void` | Diseñar o ajustar: cambia geometría o textura |
| `Shrouded_Figure` | `Player` | `—` | Diseño nuevo; sin base configurada verificable |
| `Slothian_Elder` | `Player` | `—` | Diseño nuevo; sin base configurada verificable |
| `Slothian_Kid` | `Player` | `—` | Diseño nuevo; sin base configurada verificable |
| `Slothian_Monk` | `Slothian` | `—` | Diseño nuevo; sin base configurada verificable |
| `Slothian_Scout` | `Slothian` | `—` | Diseño nuevo; sin base configurada verificable |
| `Slothian_Villager` | `Slothian` | `—` | Diseño nuevo; sin base configurada verificable |
| `Slothian_Warrior` | `Slothian` | `—` | Diseño nuevo; sin base configurada verificable |
| `Swarm_Bees` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Tadpole_Rhino` | `Frog_Green` | `Frog_Green` | Diseñar o ajustar: cambia geometría o textura |
| `Tank` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Toad_Rhino_Green` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Trork_Christmas` | `Trork` | `Trork_Shaman` | Diseñar o ajustar: cambia geometría o textura; cambia escala del modelo |
| `Tuluk` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Warp` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Wurmling_Frost` | `Larva_Silk` | `Larva_Void` | Diseñar o ajustar: cambia geometría o textura |
| `Zombie_Werewolf` | `Zombie` | `Zombie` | Diseñar o ajustar: cambia geometría o textura |

## Pendientes: desplegable (5)

| targetModelAssetId | Parent | Base sugerida | Revisión |
| --- | --- | --- | --- |
| `Crossbow_Turret` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Deployable_Fire_Trap` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Goblin_Launcher` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Healing_Totem` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Slowness_Totem` | `—` | `—` | Diseño nuevo; sin base configurada verificable |

## Pendientes: auxiliar (7)

| targetModelAssetId | Parent | Base sugerida | Revisión |
| --- | --- | --- | --- |
| `Bear_Voidtaken_Tentacle_Marker` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Bear_Voidtaken_Tentacle_Marker_Model` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Deployable_Fire_Trap_Preview` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Encounter_Marker` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `NPC_Path_Marker` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `NPC_Spawn_Marker` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Objective_Location_Marker` | `—` | `—` | Diseño nuevo; sin base configurada verificable |

## Pendientes: cinematica_o_prueba (22)

| targetModelAssetId | Parent | Base sugerida | Revisión |
| --- | --- | --- | --- |
| `Cinematic_Dragon_Void` | `Dragon_Frost` | `—` | Diseño nuevo; sin base configurada verificable |
| `Cinematic_Shrouded_Figure` | `Player` | `—` | Diseño nuevo; sin base configurada verificable |
| `PlayerTestModel_G` | `Player` | `—` | Diseño nuevo; sin base configurada verificable |
| `PlayerTestModel_V` | `Player` | `—` | Diseño nuevo; sin base configurada verificable |
| `Showcase_Cobalt_Gear` | `Mannequin` | `Mannequin` | Diseñar o ajustar: cambia geometría o textura |
| `Showcase_Copper_Gear` | `Mannequin` | `Mannequin` | Diseñar o ajustar: cambia geometría o textura |
| `Showcase_Iron_Gear` | `Mannequin` | `Mannequin` | Diseñar o ajustar: cambia geometría o textura |
| `Showcase_Iron_TargetDummy_1` | `Mannequin` | `Mannequin` | Diseñar o ajustar: cambia geometría o textura |
| `Showcase_Mannequin_Heal` | `Mannequin` | `Mannequin` | Diseñar o ajustar: cambia geometría o textura |
| `Showcase_Mannequin_Inv_Portal` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Showcase_Mannequin_Inv_Sphere` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Showcase_Mannequin_Lightning` | `Mannequin` | `Mannequin` | Diseñar o ajustar: cambia geometría o textura |
| `Showcase_Mannequin_Sitting` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Showcase_Onyxium_Gear` | `Mannequin` | `Mannequin` | Diseñar o ajustar: cambia geometría o textura |
| `Showcase_Prisma_Gear` | `Mannequin` | `Mannequin` | Diseñar o ajustar: cambia geometría o textura |
| `Showcase_Skeleton_Assasin` | `Skeleton` | `Skeleton_Burnt_Alchemist` | Diseñar o ajustar: cambia geometría o textura |
| `Showcase_Skeleton_Dead` | `Skeleton` | `Skeleton_Burnt_Alchemist` | Diseñar o ajustar: cambia geometría o textura |
| `Showcase_Skeleton_Guard` | `Skeleton` | `Skeleton_Burnt_Alchemist` | Diseñar o ajustar: cambia geometría o textura |
| `Showcase_Skeleton_Tank` | `Skeleton` | `Skeleton_Burnt_Alchemist` | Diseñar o ajustar: cambia geometría o textura |
| `Showcase_Wooden_Gear` | `Mannequin` | `Mannequin` | Diseñar o ajustar: cambia geometría o textura |
| `Sword_Charged_Test` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Test_Platform` | `—` | `—` | Diseño nuevo; sin base configurada verificable |

## Pendientes: proyectil (107)

| targetModelAssetId | Parent | Base sugerida | Revisión |
| --- | --- | --- | --- |
| `Ability_ChargedShot_Bolt` | `Ice_Ball` | `—` | Diseño nuevo; sin base configurada verificable |
| `Ability_Fireball` | `Fireball` | `—` | Diseño nuevo; sin base configurada verificable |
| `Arrow_Crossbow_Signature` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Arrow_Crude` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Arrow_Fire` | `Arrow_Crude` | `—` | Diseño nuevo; sin base configurada verificable |
| `Arrow_Frost` | `Arrow_Crude` | `—` | Diseño nuevo; sin base configurada verificable |
| `Arrow_Iron` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Arrow_Ricochet` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Arrow_Ricochet_Signature` | `Arrow_Ricochet` | `—` | Diseño nuevo; sin base configurada verificable |
| `Arrow_Shortbow_Signature` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Arrow_Vamp` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Arrow_Vamp_Signature` | `Arrow_Vamp` | `—` | Diseño nuevo; sin base configurada verificable |
| `Axe_Bone` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Axe_Stone_Trork` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Bangstick` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Bomb` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Bomb_Fire_Goblin` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Bomb_Fire_Goblin_Dud` | `Bomb_Fire_Goblin` | `—` | Diseño nuevo; sin base configurada verificable |
| `Bomb_Large_Fire_Goblin` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Bomb_Popberry` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Bomb_Potion_Poison` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Bullet_Blunderbuss` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Cactee_Spike` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Crossbow_Turret_Item_Projectile` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Dagger_Adamantite` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Dagger_Bone` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Dagger_Bronze` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Dagger_Bronze_Ancient` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Dagger_Cobalt` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Dagger_Copper` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Dagger_Crude` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Dagger_Doomed` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Dagger_Fang_Doomed` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Dagger_Iron` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Dagger_Mithril` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Dagger_Onyxium` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Dagger_Stone_Trork` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Dagger_Thorium` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Debug_Chain_Hook` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Debug_Charged_Bolt` | `Arrow_Crude` | `—` | Diseño nuevo; sin base configurada verificable |
| `Debug_Earth_Wave` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Debug_Wind_Wave` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Dungeon_Cauldron_Goo_Bomb` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Dungeon_Food_Throw` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Dungeon_Shroom_Throw` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Egg` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Explosive_Barrel` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Eye_Void_Blast` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Feran_Windwalker_Wind_Burst` | `Tornado` | `—` | Diseño nuevo; sin base configurada verificable |
| `Feran_Windwalker_Wind_Vortex` | `Tornado` | `—` | Diseño nuevo; sin base configurada verificable |
| `Fireball` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Goblin_Pot` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Goblin_Rocket` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Goblin_Trash` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Ground_Slam` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Healing_Totem_Projectile` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Health_Pack_Potion` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Hookshot_Hook` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Ice_Ball` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Ice_Bolt` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Ingredient_Poop` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Invisible_Projectile` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Kunai` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Projectile` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Projectile_Debug_Scale` | `Arrow_Crude` | `—` | Diseño nuevo; sin base configurada verificable |
| `Rubble_Aqua` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Rubble_Basalt` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Rubble_Calcite` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Rubble_Default` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Rubble_Ice` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Rubble_Marble` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Rubble_Quartzite` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Rubble_Sandstone` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Rubble_Sandstone_Red` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Rubble_Sandstone_White` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Rubble_Shale` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Rubble_Slate` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Rubble_Stone` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Rubble_Stone_Mossy` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Rubble_Volcanic` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Sawblade` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Scarak_Seeker_Spitball` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Scrap_Drop_Can_Projectile` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Scrap_Drop_Projectile` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Skeleton_Mage_Corruption_Orb` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Slowness_Totem_Projectile` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Spear_Adamantite` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Spear_Adamantite_Saurian` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Spear_Bone` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Spear_Bronze` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Spear_Cobalt` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Spear_Copper` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Spear_Crude` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Spear_Double_Incandescent` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Spear_Fishbone` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Spear_Iron` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Spear_Leaf` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Spear_Mithril` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Spear_Onyxium` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Spear_Scrap` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Spear_Stone_Trork` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Spear_Thorium` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Spear_Tribal` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Stalactite_Projectile` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Tornado` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Trash` | `—` | `—` | Diseño nuevo; sin base configurada verificable |
| `Wind_Strike` | `—` | `—` | Diseño nuevo; sin base configurada verificable |

## Reglas del mod cuyo ID exacto no está en los assets

`Goblin_Hermit`, `Goblin_Ogre`

Pueden pertenecer a packs antiguos o personalizados; no se borran automáticamente.

## Probar sin tocar los defaults

[USER_HitParticles.candidates.json](USER_HitParticles.candidates.json) contiene **9 reglas exactas propuestas** copiadas de bases ya configuradas con la misma geometría/textura corporal. Está fuera del directorio de datos y no se carga automáticamente.

Combina solo las reglas que quieras probar con tu USER existente. No reemplaces el archivo entero ni dupliques un match que ya estés afinando: la primera regla gana. Los osos Voidtaken y los tentáculos requieren diseño y no reciben una receta inventada en este archivo.

Después de guardar el USER real de tu instalación, ejecuta `/brutalimpacts --reload`. Puedes activar el debug con `/brutalimpacts --debug` para confirmar `targetModelAssetId`. Cuando una receta esté aprobada, se podrá trasladar a los defaults del proyecto y retirar su override de USER.

El USER ya generado no se reemplaza al instalar otro JAR: cambiar el USER incluido en el proyecto solo afecta a instalaciones donde todavía no existe. Los defaults generados sí se refrescan al arrancar y al recargar; edita USER durante las pruebas.

## Repetir la auditoría

Desde la raíz del mod, usando Python 3.10 o posterior:

```powershell
python tools/audit_hit_particle_models.py `
  --assets 'C:\Users\axelu\AppData\Roaming\Hytale\install\release\package\game\latest\Assets.zip' `
  --defaults 'C:\Hytale\MODDING\MyFirstMod\BrutalImpacts\src\main\resources\brutalimpacts\DefaultHitParticles_ReadOnly.json' `
  --user 'C:\Users\axelu\AppData\Roaming\Hytale\UserData\Mods\BrutalImpacts\USER_HitParticles.json' `
  --data-dir 'C:\Users\axelu\AppData\Roaming\Hytale\UserData\Mods\BrutalImpacts' `
  --log 'C:\Users\axelu\AppData\Roaming\Hytale\UserData\Logs\2026-10-27_22-47-47_client.log'
```
