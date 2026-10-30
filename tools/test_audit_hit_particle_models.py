"""Regression tests for coverage priority, inheritance suggestions and auxiliaries."""

import argparse
import json
import tempfile
import unittest
from pathlib import Path
from zipfile import ZipFile

from audit_hit_particle_models import audit, match_rule, read_rules


class ModelAuditTests(unittest.TestCase):
    def setUp(self):
        cache = Path(__file__).resolve().parents[1] / ".cache"
        cache.mkdir(exist_ok=True)
        self.temp = tempfile.TemporaryDirectory(dir=cache)
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.defaults = self.root / "DefaultHitParticles_ReadOnly.json"
        self.user = self.root / "USER_HitParticles.json"
        self.defaults.write_text(json.dumps({"rules": [{"match": {"type": "exact", "value": "Goblin"}, "particleSystemId": "Blood"}]}))
        self.user.write_text('{"rules": []}')
        self.assets = self.root / "Assets.zip"
        models = {
            "Goblin": {"Model": "Goblin.blockymodel", "Texture": "Skin.png", "MinScale": 1, "MaxScale": 1},
            "Coffer_Goblin": {"Parent": "Goblin"},
            "Voidtaken_Goblin": {"Parent": "Goblin", "Texture": "Voidtaken.png"},
            "Tentacle": {"Model": "Tentacle.blockymodel", "Texture": "Tentacle.png"},
            "Tentacle_Marker": {"Model": "NPC/MISC/Empty.blockymodel", "Texture": "Tentacle.png"},
        }
        with ZipFile(self.assets, "w") as archive:
            for mid, data in models.items():
                archive.writestr(f"Server/Models/Void/{mid}.json", json.dumps(data))
                archive.writestr(f"Server/NPC/Roles/{mid}.json", json.dumps({"Type": "Variant", "Modify": {"Appearance": mid}}))
            archive.writestr("Server/Models/Projectiles/Test_Projectile.json", json.dumps({"Model": "Projectile.blockymodel"}))
        self.log = self.root / "client.log"
        self.log.write_text("targetModelAssetId=Coffer_Goblin\ntargetModelAssetId=Coffer_Goblin\ntargetModelAssetId=Modded_Creature\n")

    def run_audit(self):
        return audit(argparse.Namespace(defaults=self.defaults, user=self.user, data_dir=None, assets=self.assets, log=[self.log]))

    def test_parent_is_not_coverage_but_identical_child_is_copy_candidate(self):
        report, candidates = self.run_audit()
        records = {r["id"]: r for r in report["models"]}
        self.assertTrue(records["Goblin"]["covered"])
        self.assertFalse(records["Coffer_Goblin"]["covered"])
        self.assertTrue(records["Coffer_Goblin"]["copy_candidate"])
        self.assertEqual(candidates["rules"], [{"match": {"type": "exact", "value": "Coffer_Goblin"}, "particleSystemId": "Blood"}])
        self.assertFalse(records["Voidtaken_Goblin"]["copy_candidate"])
        self.assertEqual(records["Voidtaken_Goblin"]["suggested_base"], "Goblin")

    def test_marker_and_projectile_are_separate_from_creatures(self):
        report, _ = self.run_audit()
        records = {r["id"]: r for r in report["models"]}
        self.assertEqual(records["Tentacle_Marker"]["category"], "auxiliar")
        self.assertEqual(records["Test_Projectile"]["category"], "proyectil")

    def test_user_prefix_wins_over_later_default_exact(self):
        self.user.write_text(json.dumps({"rules": [{"match": {"type": "prefix", "value": "Gob"}, "particleSystemId": "Custom"}]}))
        rules = read_rules(self.user) + read_rules(self.defaults)
        self.assertEqual(match_rule("Goblin", rules)["rule"]["particleSystemId"], "Custom")
        self.assertIsNone(match_rule("goblin", rules))

    def test_log_counts_and_unknown_modded_models_are_retained(self):
        report, _ = self.run_audit()
        coffer = next(r for r in report["models"] if r["id"] == "Coffer_Goblin")
        self.assertEqual(coffer["observed_debug_messages"], 2)
        self.assertEqual(report["observed_models_absent_from_assets"], {"Modded_Creature": 1})

    def test_inheritance_cycle_does_not_silently_produce_suggestions(self):
        with ZipFile(self.assets, "a") as archive:
            archive.writestr("Server/Models/Void/Loop.json", json.dumps({"Parent": "Loop"}))
        with self.assertRaisesRegex(ValueError, "Cyclic model inheritance"):
            self.run_audit()

    def test_additional_files_are_loaded_between_user_and_defaults(self):
        extra = self.root / "00_Custom.json"
        extra.write_text(json.dumps({"rules": [{"match": {"type": "exact", "value": "Coffer_Goblin"}, "particleSystemId": "Extra"}]}))
        report, candidates = audit(argparse.Namespace(defaults=self.defaults, user=self.user, data_dir=self.root, assets=self.assets, log=[]))
        coffer = next(r for r in report["models"] if r["id"] == "Coffer_Goblin")
        self.assertTrue(coffer["covered"])
        self.assertEqual(Path(coffer["matching_rule"]["file"]), extra)
        self.assertNotIn("Coffer_Goblin", [r["match"]["value"] for r in candidates["rules"]])


if __name__ == "__main__":
    unittest.main()
