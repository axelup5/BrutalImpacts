"""Read installed Hytale assets and report hit-particle coverage without changing configs.

Only Python's standard library is required. Outputs stay under the mod project.
Model inheritance is evidence for suggestions; it never counts as a matching mod rule.
"""

from __future__ import annotations

import argparse
import copy
import hashlib
import json
import re
from collections import Counter, defaultdict
from datetime import datetime
from pathlib import Path
from zipfile import ZipFile


ROOT = Path(__file__).resolve().parents[1]


def load_json(path: Path):
    return json.loads(path.read_text(encoding="utf-8-sig"))


def read_rules(path: Path):
    result = []
    for index, rule in enumerate(load_json(path).get("rules", []), 1):
        match = rule.get("match", {})
        kind = str(match.get("type", "")).strip().lower()
        value = str(match.get("value", "")).strip()
        effects = rule.get("effects") or [rule]
        if kind not in {"exact", "prefix", "contains"} or not value:
            raise ValueError(f"Invalid match in {path}, rule {index}")
        if not any(effect.get("particleSystemId") for effect in effects):
            raise ValueError(f"No particleSystemId in {path}, rule {index}")
        result.append({"rule": rule, "kind": kind, "value": value, "file": str(path), "index": index})
    return result


def match_rule(model_id, rules):
    for record in rules:
        value, kind = record["value"], record["kind"]
        if ((kind == "exact" and model_id == value)
                or (kind == "prefix" and model_id.startswith(value))
                or (kind == "contains" and value in model_id)):
            return record
    return None


def appearances(obj, path=()):
    if isinstance(obj, dict):
        for key, value in obj.items():
            if key not in {"Description", "Compute"}:
                yield from appearances(value, path + (key,))
    elif isinstance(obj, list):
        for index, value in enumerate(obj):
            yield from appearances(value, path + (str(index),))
    elif isinstance(obj, str) and any("appearance" in key.lower() for key in path):
        yield obj, ".".join(path)


def audit(args):
    default_rules = read_rules(args.defaults)
    user_rules = read_rules(args.user) if args.user else []
    extras = []
    if args.data_dir:
        excluded = {args.defaults.name, "DefaultHitParticles_ReadOnly.json", "USER_HitParticles.json"}
        for path in sorted(args.data_dir.glob("*.json"), key=lambda p: p.name.lower()):
            if path.name not in excluded:
                extras += read_rules(path)
    rules = user_rules + extras + default_rules
    observed = Counter()
    for path in args.log:
        for line in path.read_text(encoding="utf-8-sig", errors="replace").splitlines():
            observed.update(re.findall(r"targetModelAssetId=([A-Za-z0-9_]+)", line))
    with ZipFile(args.assets) as archive:
        models = {}
        for name in archive.namelist():
            if name.startswith("Server/Models/") and name.endswith(".json"):
                model_id = Path(name).stem
                if model_id in models:
                    raise ValueError(f"Duplicate model ID: {model_id}")
                models[model_id] = {"path": name, "data": json.loads(archive.read(name))}
        role_refs = defaultdict(list)
        role_count = 0
        for name in archive.namelist():
            if name.startswith("Server/NPC/Roles/") and name.endswith(".json"):
                role_count += 1
                role = json.loads(archive.read(name))
                for model_id, field in appearances(role):
                    if model_id in models:
                        role_refs[model_id].append({"role": Path(name).stem, "type": role.get("Type"), "path": name, "field": field})

    def lineage(model_id):
        result, seen = [], set()
        current = model_id
        while current in models:
            if current in seen:
                raise ValueError(f"Cyclic model inheritance: {model_id}")
            seen.add(current)
            result.append(current)
            current = models[current]["data"].get("Parent")
        return result

    def inherited(model_id, key):
        for member in lineage(model_id):
            if key in models[member]["data"]:
                return models[member]["data"][key]
        return None

    surfaces = {mid: (inherited(mid, "Model"), inherited(mid, "Texture")) for mid in models}

    def category(model_id):
        path = models[model_id]["path"]
        mesh = surfaces[model_id][0] or ""
        if "/Projectiles/" in path:
            return "proyectil"
        if ("marker" in model_id.lower() or "preview" in model_id.lower()
                or mesh.lower().endswith("/empty.blockymodel") or model_id in {"Empty", "Empty_Model"}):
            return "auxiliar"
        if (model_id.startswith(("Cinematic_", "Test_", "PlayerTestModel_", "Showcase_"))
                or model_id.endswith("_Test") or "/_Debug/" in path or "/Tests/" in path):
            return "cinematica_o_prueba"
        if "/Deployables/" in path:
            return "desplegable"
        return "criatura" if role_refs[model_id] else "sin_referencia_npc_directa"

    records = []
    for mid in sorted(models):
        actual_rule = match_rule(mid, rules)
        default_rule = match_rule(mid, default_rules)
        ancestors = lineage(mid)[1:]
        suggested_base = next((ancestor for ancestor in ancestors if match_rule(ancestor, rules)), None)
        evidence = "ancestro_configurado" if suggested_base else None
        if all(surfaces[mid]) and (not suggested_base or surfaces[suggested_base] != surfaces[mid]):
            same_surface_base = next((other for other in sorted(models)
                                      if other != mid and surfaces[other] == surfaces[mid] and match_rule(other, rules)), None)
            if same_surface_base:
                suggested_base = same_surface_base
                evidence = "misma_geometria_y_textura"
        same_surface = bool(suggested_base and all(surfaces[mid]) and surfaces[mid] == surfaces[suggested_base])
        cat = category(mid)
        reusable = not actual_rule and same_surface and cat == "criatura"
        records.append({
            "id": mid, "asset_path": models[mid]["path"], "category": cat,
            "observed_debug_messages": observed[mid],
            "covered": actual_rule is not None, "covered_by_defaults": default_rule is not None,
            "matching_rule": {"file": actual_rule["file"], "index": actual_rule["index"], "match": actual_rule["rule"]["match"]} if actual_rule else None,
            "parent": models[mid]["data"].get("Parent"), "ancestors": ancestors,
            "geometry": surfaces[mid][0], "texture": surfaces[mid][1],
            "min_scale": inherited(mid, "MinScale"), "max_scale": inherited(mid, "MaxScale"),
            "npc_appearance_references": role_refs[mid],
            "suggested_base": suggested_base, "suggestion_evidence": evidence,
            "same_geometry_and_texture_as_base": same_surface,
            "copy_candidate": reusable,
            "scale_differs_from_base": bool(suggested_base and any(inherited(mid, k) != inherited(suggested_base, k) for k in ("MinScale", "MaxScale"))),
        })

    groups = defaultdict(list)
    for record in records:
        if record["category"] in {"criatura", "sin_referencia_npc_directa"} and record["geometry"] and record["texture"]:
            groups[(record["geometry"], record["texture"])].append(record)
    families = [{"geometry": surface[0], "texture": surface[1], "models": [r["id"] for r in members],
                 "uncovered": [r["id"] for r in members if not r["covered"]]}
                for surface, members in sorted(groups.items()) if len(members) > 1 and any(not r["covered"] for r in members)]
    candidates = []
    for record in records:
        if record["copy_candidate"]:
            rule = copy.deepcopy(match_rule(record["suggested_base"], rules)["rule"])
            rule["match"] = {"type": "exact", "value": record["id"]}
            candidates.append(rule)

    version = "desconocida"
    server_jar = args.assets.parent / "Server" / "HytaleServer.jar"
    if server_jar.exists():
        with ZipFile(server_jar) as archive:
            for line in archive.read("META-INF/MANIFEST.MF").decode().splitlines():
                if line.startswith("Implementation-Version: "):
                    version = line.split(": ", 1)[1]
    installed_diff = None
    if args.data_dir:
        installed = args.data_dir / "DefaultHitParticles_ReadOnly.json"
        if installed.exists():
            installed_rules = read_rules(installed)
            installed_diff = {"path": str(installed), "same_rules_as_project": [r["rule"] for r in installed_rules] == [r["rule"] for r in default_rules]}
    return {
        "generated_at": datetime.now().astimezone().isoformat(timespec="seconds"),
        "server_version": version, "assets": str(args.assets), "defaults": str(args.defaults),
        "data_dir": str(args.data_dir) if args.data_dir else None,
        "user": str(args.user) if args.user else None, "default_sha256": hashlib.sha256(args.defaults.read_bytes()).hexdigest(),
        "logs": [str(path) for path in args.log],
        "observed_models_absent_from_assets": {mid: count for mid, count in observed.items() if mid not in models},
        "default_rules": len(default_rules), "user_rules": len(user_rules), "extra_rules": len(extras),
        "npc_role_assets": role_count, "model_count": len(records),
        "covered_count": sum(r["covered"] for r in records),
        "missing_by_category": dict(Counter(r["category"] for r in records if not r["covered"])),
        "default_exact_ids_absent_from_assets": sorted({r["value"] for r in default_rules if r["kind"] == "exact"} - models.keys()),
        "installed_defaults": installed_diff, "families": families, "models": records,
    }, {"rules": candidates}


def markdown(report, candidates):
    missing = [r for r in report["models"] if not r["covered"]]
    def ps_quote(value):
        return "'" + value.replace("'", "''") + "'"
    command = ["python tools/audit_hit_particle_models.py", f"  --assets {ps_quote(report['assets'])}", f"  --defaults {ps_quote(report['defaults'])}"]
    for option in ("user", "data_dir"):
        if report[option]:
            command.append(f"  --{option.replace('_', '-')} {ps_quote(report[option])}")
    command += [f"  --log {ps_quote(path)}" for path in report["logs"]]
    lines = ["# Auditoría de partículas por modelo", "", f"Generada: {report['generated_at']}. Servidor instalado: **{report['server_version']}**.", "",
             f"Se leyeron **{report['model_count']} modelos**, **{report['npc_role_assets']} roles NPC**, **{report['default_rules']} reglas por defecto**, **{report['user_rules']} reglas de usuario** y **{report['extra_rules']} reglas adicionales**.", "",
             f"**{report['covered_count']} modelos cubiertos; {len(missing)} sin regla personalizada**. Sin regla se usa el fallback; no significa ausencia total de partículas.", "",
             "## Cómo interpretar este informe", "",
             "- La cobertura usa el orden real: USER, otros JSON por nombre y defaults; la primera coincidencia gana. No se da prioridad automática a `exact` sobre `prefix`/`contains`.",
             "- El `Parent` de un modelo de Hytale **no hereda reglas del mod**. El modelo derivado necesita una coincidencia propia.",
             "- Las referencias de `Appearance` de roles son evidencia de uso NPC; no demuestran que se pueda dañar ni que aparezca normalmente. Incluyen templates abstractos.",
             "- Misma geometría/textura corporal es una propuesta para reutilizar efectos, no una garantía: revisar attachments, escala, offsets y el estilo deseado en el juego.",
             "- Los modelos sin referencias directas no se descartan: un plugin o un asset puede usarlos dinámicamente. Marcadores, previews, proyectiles y cinemáticas se listan aparte.",
             "- Esta es una foto de los assets instalados; no demuestra en qué actualización se añadió cada modelo.", "",
             "## Fuentes", "", f"- ZIP oficial leído: `{report['assets']}`.",
             "- Defaults: [DefaultHitParticles_ReadOnly.json](../src/main/resources/brutalimpacts/DefaultHitParticles_ReadOnly.json).",
             f"- USER leído: `{report['user'] or 'ninguno'}`.",
             "- [El JSON de auditoría](particle-model-audit.json) incluye la ruta dentro del ZIP, padres, geometría, textura y campos `Appearance` para comprobar cada caso.", ""]
    if report["installed_defaults"]:
        lines += [f"Defaults instalados y del proyecto: **{'iguales' if report['installed_defaults']['same_rules_as_project'] else 'diferentes'}** al comparar las reglas.", ""]
    lines += ["## Resumen de pendientes", "", "| Tipo | Sin regla |", "| --- | ---: |"]
    for category, count in sorted(report["missing_by_category"].items()):
        lines.append(f"| {category} | {count} |")
    observed_missing = sorted((r for r in missing if r["observed_debug_messages"]), key=lambda r: (-r["observed_debug_messages"], r["id"]))
    if report["logs"]:
        lines += ["", "## Pendientes observados en tus pruebas", "", "Se contaron únicamente mensajes `targetModelAssetId` en los logs indicados. La cantidad mide apariciones en el log, no daño ni eventos únicos.", "",
                  "| targetModelAssetId | Mensajes | Tipo |", "| --- | ---: | --- |"]
        for record in observed_missing:
            lines.append(f"| `{record['id']}` | {record['observed_debug_messages']} | {record['category']} |")
        if report["observed_models_absent_from_assets"]:
            lines += ["", "IDs observados que no existen en este ZIP: " + ", ".join(f"`{mid}`" for mid in report["observed_models_absent_from_assets"])]
    lines += ["", "## Casos que conviene priorizar", "",
              "- `Bear_Grizzly_Voidtaken` y `Bear_Grizzly_Voidtaken2` heredan de `Bear_Grizzly`, pero usan texturas Voidtaken diferentes. La receta del oso común es una base estructural; el color del pelo y el efecto de corrupción necesitan decisión visual.",
              "- `Void_Tentacle`, `Bear_Voidtaken_Tentacle` y `Bear_Voidtaken_Tentacle_Model` comparten geometría y textura. Diseñar una receta de tentáculo y duplicarla con matches exactos permite cubrir los tres; revisar la escala menor de las variantes del oso.",
              "- `Bear_Voidtaken_Tentacle_Marker` y `Bear_Voidtaken_Tentacle_Marker_Model` usan `Empty.blockymodel`. Son auxiliares y no deben recibir automáticamente la receta del tentáculo por compartir el prefijo.",
              "- `Coffer_Goblin_Lobber`, `Coffer_Goblin_Miner` y `Coffer_Goblin_Scrapper` heredan la geometría/textura de sus correspondientes goblins ya configurados. Son los primeros candidatos para copiar una receta existente.",
              "- `Goblin_Burner`/`Coffer_Goblin_Burner`, `Goblin_Guardian`/`Coffer_Goblin_Guardian`/`Dungeon_Goblin_Mace`, y `Goblin_Glutton`/`Coffer_Goblin_Glutton` necesitan recetas base nuevas. `Dungeon_Feastmaster_Boss` deriva de Glutton, con otra textura y escala.",
              "- Las fases y transiciones del Gobliterator comparten su cuerpo mecánico. Sus piezas, barril y piloto merecen revisión separada; una regla general `contains: Goblin` también alcanzaría esos IDs.",
              "- `Dungeon_Goblin_Lobber` es un rol cuya `Modify.Appearance` apunta a `Goblin_Lobber`; ese rol ya usa un ID configurado. `Coffer_Goblin_Lobber`, en cambio, sí es un modelo diferente que requiere otra coincidencia.", "",
              "## Familias con la misma geometría y textura corporal", "", "Una receta nueva puede servir de base para varias reglas exactas de estas familias. Se incluyen también modelos sin referencia NPC directa; su aparición en el juego debe comprobarse.", "", "| Familia de modelos | Sin regla |", "| --- | --- |"]
    for group in report["families"]:
        lines.append("| " + ", ".join(f"`{mid}`" for mid in group["models"]) + " | " + ", ".join(f"`{mid}`" for mid in group["uncovered"]) + " |")
    for category in ("criatura", "sin_referencia_npc_directa", "desplegable", "auxiliar", "cinematica_o_prueba", "proyectil"):
        members = [r for r in missing if r["category"] == category]
        if not members:
            continue
        lines += ["", f"## Pendientes: {category} ({len(members)})", "", "| targetModelAssetId | Parent | Base sugerida | Revisión |", "| --- | --- | --- | --- |"]
        for record in members:
            if record["copy_candidate"]:
                review = "Copiar como punto de partida; misma geometría/textura"
            elif record["suggested_base"]:
                review = "Diseñar o ajustar: cambia geometría o textura"
            else:
                review = "Diseño nuevo; sin base configurada verificable"
            if record["scale_differs_from_base"]:
                review += "; cambia escala del modelo"
            lines.append(f"| `{record['id']}` | `{record['parent'] or '—'}` | `{record['suggested_base'] or '—'}` | {review} |")
    lines += ["", "## Reglas del mod cuyo ID exacto no está en los assets", "", ", ".join(f"`{mid}`" for mid in report["default_exact_ids_absent_from_assets"]) or "Ninguna.", "",
              "Pueden pertenecer a packs antiguos o personalizados; no se borran automáticamente.", "", "## Probar sin tocar los defaults", "",
              f"[USER_HitParticles.candidates.json](USER_HitParticles.candidates.json) contiene **{len(candidates['rules'])} reglas exactas propuestas** copiadas de bases ya configuradas con la misma geometría/textura corporal. Está fuera del directorio de datos y no se carga automáticamente.", "",
              "Combina solo las reglas que quieras probar con tu USER existente. No reemplaces el archivo entero ni dupliques un match que ya estés afinando: la primera regla gana. Los osos Voidtaken y los tentáculos requieren diseño y no reciben una receta inventada en este archivo.", "",
              "Después de guardar el USER real de tu instalación, ejecuta `/brutalimpacts --reload`. Puedes activar el debug con `/brutalimpacts --debug` para confirmar `targetModelAssetId`. Cuando una receta esté aprobada, se podrá trasladar a los defaults del proyecto y retirar su override de USER.", "",
              "El USER ya generado no se reemplaza al instalar otro JAR: cambiar el USER incluido en el proyecto solo afecta a instalaciones donde todavía no existe. Los defaults generados sí se refrescan al arrancar y al recargar; edita USER durante las pruebas.", "",
              "## Repetir la auditoría", "", "Desde la raíz del mod, usando Python 3.10 o posterior:", "", "```powershell",
              *[line + (' `' if index < len(command) - 1 else '') for index, line in enumerate(command)],
              "```", ""]
    return "\n".join(lines)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--assets", type=Path, required=True)
    parser.add_argument("--defaults", type=Path, default=ROOT / "src/main/resources/brutalimpacts/DefaultHitParticles_ReadOnly.json")
    parser.add_argument("--user", type=Path)
    parser.add_argument("--data-dir", type=Path)
    parser.add_argument("--log", type=Path, action="append", default=[])
    parser.add_argument("--output-dir", type=Path, default=ROOT / "docs")
    args = parser.parse_args()
    args.output_dir = args.output_dir.resolve()
    if not args.output_dir.is_relative_to(ROOT):
        parser.error("Outputs must stay inside the BrutalImpacts project")
    report, candidates = audit(args)
    args.output_dir.mkdir(parents=True, exist_ok=True)
    for name, content in (("particle-model-audit.json", report), ("USER_HitParticles.candidates.json", candidates)):
        (args.output_dir / name).write_text(json.dumps(content, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    (args.output_dir / "particle-model-audit.md").write_text(markdown(report, candidates), encoding="utf-8")
    print(json.dumps({k: report[k] for k in ("server_version", "model_count", "covered_count", "missing_by_category", "default_exact_ids_absent_from_assets")}, ensure_ascii=False))
    print(f"Candidate exact rules: {len(candidates['rules'])}")


if __name__ == "__main__":
    main()
