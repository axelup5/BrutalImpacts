package dev.hytalemodding.api;

import com.hypixel.hytale.protocol.Color;

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
        registerModelPredicate(id -> modelAssetId.equals(id), new HitParticleSpec(particleSystemId, null));
    }

    public void registerModelPrefix(@Nonnull String prefix, @Nonnull String particleSystemId) {
        Objects.requireNonNull(prefix, "prefix");
        Objects.requireNonNull(particleSystemId, "particleSystemId");
        registerModelPredicate(id -> id != null && id.startsWith(prefix), new HitParticleSpec(particleSystemId, null));
    }

    public void registerModelContains(@Nonnull String needle, @Nonnull String particleSystemId) {
        Objects.requireNonNull(needle, "needle");
        Objects.requireNonNull(particleSystemId, "particleSystemId");
        registerModelPredicate(id -> id != null && id.contains(needle), new HitParticleSpec(particleSystemId, null));
    }

    public void registerModelContainsTint(@Nonnull String needle, @Nonnull String particleSystemId, int r, int g, int b) {
        Objects.requireNonNull(needle, "needle");
        Objects.requireNonNull(particleSystemId, "particleSystemId");
        registerModelPredicate(
            id -> id != null && id.contains(needle),
            new HitParticleSpec(particleSystemId, new Color(toByte(r), toByte(g), toByte(b)))
        );
    }

    public void registerModelPredicate(@Nonnull Predicate<String> predicate, @Nonnull HitParticleSpec spec) {
        Objects.requireNonNull(predicate, "predicate");
        Objects.requireNonNull(spec, "spec");
        this.rules.add(new Rule(predicate, spec));
    }

    @Nonnull
    public String resolve(@Nullable String modelAssetId, @Nonnull String fallbackParticleSystemId) {
        return resolveSpec(modelAssetId, fallbackParticleSystemId).particleSystemId();
    }

    @Nonnull
    public HitParticleSpec resolveSpec(@Nullable String modelAssetId, @Nonnull String fallbackParticleSystemId) {
        for (Rule rule : this.rules) {
            if (rule.predicate.test(modelAssetId)) {
                return rule.spec;
            }
        }
        return new HitParticleSpec(fallbackParticleSystemId, null);
    }

    private static byte toByte(int value) {
        int clamped = Math.max(0, Math.min(255, value));
        return (byte) clamped;
    }

    private record Rule(Predicate<String> predicate, HitParticleSpec spec) {
    }
}