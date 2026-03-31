package dev.hytalemodding.api;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

public final class HitParticleRegistry {

    private final CopyOnWriteArrayList<Rule> rules = new CopyOnWriteArrayList<>();

    public void registerModelExact(@Nonnull String modelAssetId, @Nonnull String particleSystemId) {
        Objects.requireNonNull(modelAssetId, "modelAssetId");
        Objects.requireNonNull(particleSystemId, "particleSystemId");
        registerModelPredicate(id -> modelAssetId.equals(id), HitParticleSpec.single(HitParticleEffect.of(particleSystemId)));
    }

    public void registerModelPrefix(@Nonnull String prefix, @Nonnull String particleSystemId) {
        Objects.requireNonNull(prefix, "prefix");
        Objects.requireNonNull(particleSystemId, "particleSystemId");
        registerModelPredicate(id -> id != null && id.startsWith(prefix), HitParticleSpec.single(HitParticleEffect.of(particleSystemId)));
    }

    public void registerModelContains(@Nonnull String needle, @Nonnull String particleSystemId) {
        Objects.requireNonNull(needle, "needle");
        Objects.requireNonNull(particleSystemId, "particleSystemId");
        registerModelPredicate(id -> id != null && id.contains(needle), HitParticleSpec.single(HitParticleEffect.of(particleSystemId)));
    }

    public void registerModelContainsTint(@Nonnull String needle, @Nonnull String particleSystemId, int r, int g, int b) {
        registerModelContainsTint(needle, particleSystemId, r, g, b, 1.0F);
    }

    public void registerModelContainsTint(@Nonnull String needle, @Nonnull String particleSystemId, int r, int g, int b, float scale) {
        Objects.requireNonNull(needle, "needle");
        Objects.requireNonNull(particleSystemId, "particleSystemId");
        registerModelPredicate(
            id -> id != null && id.contains(needle),
            HitParticleSpec.single(HitParticleEffect.tinted(particleSystemId, r, g, b, scale))
        );
    }

    public void registerModelContainsEffects(@Nonnull String needle, @Nonnull HitParticleEffect... effects) {
        Objects.requireNonNull(needle, "needle");
        Objects.requireNonNull(effects, "effects");
        registerModelPredicate(id -> id != null && id.contains(needle), new HitParticleSpec(java.util.List.of(effects)));
    }

    public void registerModelPredicate(@Nonnull Predicate<String> predicate, @Nonnull HitParticleSpec spec) {
        Objects.requireNonNull(predicate, "predicate");
        Objects.requireNonNull(spec, "spec");
        this.rules.add(new Rule(predicate, spec));
    }

    @Nonnull
    public HitParticleSpec resolveSpec(@Nullable String modelAssetId, @Nonnull String fallbackParticleSystemId) {
        for (Rule rule : this.rules) {
            if (rule.predicate.test(modelAssetId)) {
                return rule.spec;
            }
        }
        return HitParticleSpec.single(HitParticleEffect.of(fallbackParticleSystemId));
    }

    private record Rule(Predicate<String> predicate, HitParticleSpec spec) {
    }
}
