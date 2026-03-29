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
        registerModelPredicate(id -> modelAssetId.equals(id), particleSystemId);
    }

    public void registerModelPrefix(@Nonnull String prefix, @Nonnull String particleSystemId) {
        Objects.requireNonNull(prefix, "prefix");
        Objects.requireNonNull(particleSystemId, "particleSystemId");
        registerModelPredicate(id -> id != null && id.startsWith(prefix), particleSystemId);
    }

    public void registerModelContains(@Nonnull String needle, @Nonnull String particleSystemId) {
        Objects.requireNonNull(needle, "needle");
        Objects.requireNonNull(particleSystemId, "particleSystemId");
        registerModelPredicate(id -> id != null && id.contains(needle), particleSystemId);
    }

    public void registerModelPredicate(@Nonnull Predicate<String> predicate, @Nonnull String particleSystemId) {
        Objects.requireNonNull(predicate, "predicate");
        Objects.requireNonNull(particleSystemId, "particleSystemId");
        this.rules.add(new Rule(predicate, particleSystemId));
    }

    @Nonnull
    public String resolve(@Nullable String modelAssetId, @Nonnull String fallbackParticleSystemId) {
        for (Rule rule : this.rules) {
            if (rule.predicate.test(modelAssetId)) {
                return rule.particleSystemId;
            }
        }
        return fallbackParticleSystemId;
    }

    private record Rule(Predicate<String> predicate, String particleSystemId) {
    }
}

