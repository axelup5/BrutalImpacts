package dev.hytalemodding.api;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * A compiled "hit particle" result consisting of one or more {@link HitParticleEffect}s.
 *
 * <p>Most rules resolve to a single effect, but the JSON format also supports an {@code effects: []} array
 * to spawn multiple particle systems for the same match.</p>
 *
 * @param effects Ordered list of effects to spawn.
 */
public record HitParticleSpec(@Nonnull List<HitParticleEffect> effects) {

    public HitParticleSpec {
        effects = List.copyOf(effects);
    }

    /**
     * Convenience factory for a spec containing a single effect.
     */
    public static HitParticleSpec single(@Nonnull HitParticleEffect effect) {
        return new HitParticleSpec(List.of(effect));
    }
}

