package dev.hytalemodding.api;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

public final class HitParticleRegistry {

    /**
     * Registration order is used to preserve "first match wins" semantics while still allowing indexed lookups
     * for large rule sets (e.g. thousands of exact model ids).
     */
    private final AtomicInteger nextOrder = new AtomicInteger(0);

    private final ConcurrentHashMap<String, OrderedSpec> exactRules = new ConcurrentHashMap<>();

    private final Object snapshotLock = new Object();
    private volatile boolean snapshotsDirty = false;

    private final ArrayList<OrderedStringRule> prefixRules = new ArrayList<>();
    private final ArrayList<OrderedStringRule> containsRules = new ArrayList<>();
    private final ArrayList<OrderedPredicateRule> predicateRules = new ArrayList<>();

    private volatile OrderedStringRule[] prefixRulesSnapshot = new OrderedStringRule[0];
    private volatile OrderedStringRule[] containsRulesSnapshot = new OrderedStringRule[0];
    private volatile OrderedPredicateRule[] predicateRulesSnapshot = new OrderedPredicateRule[0];

    /**
     * Per-model cache for maximum in-game performance.
     *
     * Cache entries store only "matched spec" vs "no match". The {@code fallbackParticleSystemId} is applied
     * dynamically so changing the default particle system does not require invalidating this cache.
     */
    private final ConcurrentHashMap<String, ResolvedCacheEntry> resolvedCache = new ConcurrentHashMap<>();

    public void clear() {
        this.nextOrder.set(0);
        this.exactRules.clear();

        synchronized (this.snapshotLock) {
            this.prefixRules.clear();
            this.containsRules.clear();
            this.predicateRules.clear();
            this.prefixRulesSnapshot = new OrderedStringRule[0];
            this.containsRulesSnapshot = new OrderedStringRule[0];
            this.predicateRulesSnapshot = new OrderedPredicateRule[0];
            this.snapshotsDirty = false;
        }

        this.resolvedCache.clear();
    }

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
        registerModelExactSpec(modelAssetId, new HitParticleSpec(java.util.List.of(effects)));
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
        registerModelPrefixSpec(prefix, new HitParticleSpec(java.util.List.of(effects)));
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
        registerModelContainsSpec(needle, new HitParticleSpec(java.util.List.of(effects)));
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
        int order = this.nextOrder.getAndIncrement();
        synchronized (this.snapshotLock) {
            this.predicateRules.add(new OrderedPredicateRule(order, predicate, spec));
            this.snapshotsDirty = true;
        }
        this.resolvedCache.clear();
    }

    public void registerModelPredicate(@Nonnull Predicate<String> predicate, @Nonnull HitParticleEffect... effects) {
        Objects.requireNonNull(predicate, "predicate");
        Objects.requireNonNull(effects, "effects");
        registerModelPredicate(predicate, new HitParticleSpec(java.util.List.of(effects)));
    }

    @Nonnull
    public HitParticleSpec resolveSpec(@Nullable String modelAssetId, @Nonnull String fallbackParticleSystemId) {
        Objects.requireNonNull(fallbackParticleSystemId, "fallbackParticleSystemId");
        if (modelAssetId == null) {
            return HitParticleSpec.single(HitParticleEffect.of(fallbackParticleSystemId));
        }

        ResolvedCacheEntry cached = this.resolvedCache.get(modelAssetId);
        if (cached != null) {
            return cached.matchedSpec != null
                ? cached.matchedSpec
                : HitParticleSpec.single(HitParticleEffect.of(fallbackParticleSystemId));
        }

        ResolvedCacheEntry computed = this.resolvedCache.computeIfAbsent(modelAssetId, id -> new ResolvedCacheEntry(resolveMatchedSpec(id)));
        return computed.matchedSpec != null
            ? computed.matchedSpec
            : HitParticleSpec.single(HitParticleEffect.of(fallbackParticleSystemId));
    }

    @Nullable
    private HitParticleSpec resolveMatchedSpec(@Nonnull String modelAssetId) {
        ensureSnapshots();

        OrderedSpec exact = this.exactRules.get(modelAssetId);

        int bestOrder = exact == null ? Integer.MAX_VALUE : exact.order;
        HitParticleSpec bestSpec = exact == null ? null : exact.spec;
        if (bestOrder == 0) {
            return bestSpec;
        }

        for (OrderedStringRule rule : this.prefixRulesSnapshot) {
            if (modelAssetId.startsWith(rule.needle) && rule.order < bestOrder) {
                bestOrder = rule.order;
                bestSpec = rule.spec;
                if (bestOrder == 0) {
                    return bestSpec;
                }
            }
        }

        for (OrderedStringRule rule : this.containsRulesSnapshot) {
            if (modelAssetId.contains(rule.needle) && rule.order < bestOrder) {
                bestOrder = rule.order;
                bestSpec = rule.spec;
                if (bestOrder == 0) {
                    return bestSpec;
                }
            }
        }

        for (OrderedPredicateRule rule : this.predicateRulesSnapshot) {
            if (rule.predicate.test(modelAssetId) && rule.order < bestOrder) {
                bestOrder = rule.order;
                bestSpec = rule.spec;
                if (bestOrder == 0) {
                    return bestSpec;
                }
            }
        }

        return bestSpec;
    }

    private static HitParticleEffect effectFrom(@Nonnull String particleSystemId, int r, int g, int b, float scale) {
        if (r == 0 && g == 0 && b == 0) {
            return HitParticleEffect.of(particleSystemId, scale);
        }
        return HitParticleEffect.tinted(particleSystemId, r, g, b, scale);
    }

    private void registerModelExactSpec(@Nonnull String modelAssetId, @Nonnull HitParticleSpec spec) {
        int order = this.nextOrder.getAndIncrement();
        // Preserve "first match wins" for duplicate exact ids.
        this.exactRules.putIfAbsent(modelAssetId, new OrderedSpec(order, spec));
        this.resolvedCache.clear();
    }

    private void registerModelPrefixSpec(@Nonnull String prefix, @Nonnull HitParticleSpec spec) {
        int order = this.nextOrder.getAndIncrement();
        synchronized (this.snapshotLock) {
            this.prefixRules.add(new OrderedStringRule(order, prefix, spec));
            this.snapshotsDirty = true;
        }
        this.resolvedCache.clear();
    }

    private void registerModelContainsSpec(@Nonnull String needle, @Nonnull HitParticleSpec spec) {
        int order = this.nextOrder.getAndIncrement();
        synchronized (this.snapshotLock) {
            this.containsRules.add(new OrderedStringRule(order, needle, spec));
            this.snapshotsDirty = true;
        }
        this.resolvedCache.clear();
    }

    private void ensureSnapshots() {
        if (!this.snapshotsDirty) {
            return;
        }
        synchronized (this.snapshotLock) {
            if (!this.snapshotsDirty) {
                return;
            }
            this.prefixRulesSnapshot = this.prefixRules.toArray(new OrderedStringRule[0]);
            this.containsRulesSnapshot = this.containsRules.toArray(new OrderedStringRule[0]);
            this.predicateRulesSnapshot = this.predicateRules.toArray(new OrderedPredicateRule[0]);
            this.snapshotsDirty = false;
        }
    }

    private record OrderedSpec(int order, HitParticleSpec spec) {
    }

    private record OrderedStringRule(int order, String needle, HitParticleSpec spec) {
    }

    private record OrderedPredicateRule(int order, Predicate<String> predicate, HitParticleSpec spec) {
    }

    private static final class ResolvedCacheEntry {
        private final @Nullable HitParticleSpec matchedSpec;

        private ResolvedCacheEntry(@Nullable HitParticleSpec matchedSpec) {
            this.matchedSpec = matchedSpec;
        }
    }
}
