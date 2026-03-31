package dev.hytalemodding.api;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

public final class HitParticleRegistry {

    private final CopyOnWriteArrayList<Rule> rules = new CopyOnWriteArrayList<>();

    public void registerModelExact(@Nonnull String modelAssetId, @Nonnull String particleSystemId) {
        registerModelExact(modelAssetId, particleSystemId, 0, 0, 0, 1.0F);
    }

    public void registerModelExact(@Nonnull String modelAssetId, @Nonnull String particleSystemId, int r, int g, int b) {
        registerModelExact(modelAssetId, particleSystemId, r, g, b, 1.0F);
    }

    public void registerModelExact(@Nonnull String modelAssetId, @Nonnull String particleSystemId, int r, int g, int b, float scale) {
        Objects.requireNonNull(modelAssetId, "modelAssetId");
        Objects.requireNonNull(particleSystemId, "particleSystemId");
        registerModelExact(modelAssetId, effectFrom(particleSystemId, r, g, b, scale));
    }

    public void registerModelExact(@Nonnull String modelAssetId, @Nonnull HitParticleEffect... effects) {
        Objects.requireNonNull(modelAssetId, "modelAssetId");
        Objects.requireNonNull(effects, "effects");
        registerModelPredicate(id -> modelAssetId.equals(id), effects);
    }

    public void registerModelPrefix(@Nonnull String prefix, @Nonnull String particleSystemId) {
        registerModelPrefix(prefix, particleSystemId, 0, 0, 0, 1.0F);
    }

    public void registerModelPrefix(@Nonnull String prefix, @Nonnull String particleSystemId, int r, int g, int b) {
        registerModelPrefix(prefix, particleSystemId, r, g, b, 1.0F);
    }

    public void registerModelPrefix(@Nonnull String prefix, @Nonnull String particleSystemId, int r, int g, int b, float scale) {
        Objects.requireNonNull(prefix, "prefix");
        Objects.requireNonNull(particleSystemId, "particleSystemId");
        registerModelPrefix(prefix, effectFrom(particleSystemId, r, g, b, scale));
    }

    public void registerModelPrefix(@Nonnull String prefix, @Nonnull HitParticleEffect... effects) {
        Objects.requireNonNull(prefix, "prefix");
        Objects.requireNonNull(effects, "effects");
        registerModelPredicate(id -> id != null && id.startsWith(prefix), effects);
    }

    public void registerModelContains(@Nonnull String needle, @Nonnull String particleSystemId) {
        registerModelContains(needle, particleSystemId, 0, 0, 0, 1.0F);
    }

    public void registerModelContains(@Nonnull String needle, @Nonnull String particleSystemId, int r, int g, int b) {
        registerModelContains(needle, particleSystemId, r, g, b, 1.0F);
    }

    public void registerModelContains(@Nonnull String needle, @Nonnull String particleSystemId, int r, int g, int b, float scale) {
        Objects.requireNonNull(needle, "needle");
        Objects.requireNonNull(particleSystemId, "particleSystemId");
        registerModelContains(needle, effectFrom(particleSystemId, r, g, b, scale));
    }

    public void registerModelContains(@Nonnull String needle, @Nonnull HitParticleEffect... effects) {
        Objects.requireNonNull(needle, "needle");
        Objects.requireNonNull(effects, "effects");
        registerModelPredicate(id -> id != null && id.contains(needle), effects);
    }

    /**
     * @deprecated Use {@link #registerModelContains(String, String, int, int, int)}.
     */
    @Deprecated
    public void registerModelContainsTint(@Nonnull String needle, @Nonnull String particleSystemId, int r, int g, int b) {
        registerModelContains(needle, particleSystemId, r, g, b);
    }

    /**
     * @deprecated Use {@link #registerModelContains(String, String, int, int, int, float)}.
     */
    @Deprecated
    public void registerModelContainsTint(@Nonnull String needle, @Nonnull String particleSystemId, int r, int g, int b, float scale) {
        registerModelContains(needle, particleSystemId, r, g, b, scale);
    }

    /**
     * @deprecated Use {@link #registerModelContains(String, HitParticleEffect...)}.
     */
    @Deprecated
    public void registerModelContainsEffects(@Nonnull String needle, @Nonnull HitParticleEffect... effects) {
        registerModelContains(needle, effects);
    }

    public void registerModelPredicate(@Nonnull Predicate<String> predicate, @Nonnull HitParticleSpec spec) {
        Objects.requireNonNull(predicate, "predicate");
        Objects.requireNonNull(spec, "spec");
        this.rules.add(new Rule(predicate, spec));
    }

    public void registerModelPredicate(@Nonnull Predicate<String> predicate, @Nonnull HitParticleEffect... effects) {
        Objects.requireNonNull(predicate, "predicate");
        Objects.requireNonNull(effects, "effects");
        registerModelPredicate(predicate, new HitParticleSpec(java.util.List.of(effects)));
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

    private static HitParticleEffect effectFrom(@Nonnull String particleSystemId, int r, int g, int b, float scale) {
        if (r == 0 && g == 0 && b == 0) {
            return HitParticleEffect.of(particleSystemId, scale);
        }
        return HitParticleEffect.tinted(particleSystemId, r, g, b, scale);
    }
}
