package dev.hytalemodding.api;

import javax.annotation.Nonnull;

/**
 * Public entry point for Brutal Impacts integration.
 *
 * <p>This class exposes a singleton {@link HitParticleRegistry} that is used by the mod to resolve which
 * particle effects should be appended for a given target model asset id.</p>
 *
 * <p>Server owners typically configure rules via JSON. Other mods/plugins can register rules at runtime
 * by calling {@link #hitParticles()} and using the registry's {@code register*} methods.</p>
 */
public final class BrutalImpactsApi {

    private static final HitParticleRegistry HIT_PARTICLES = new HitParticleRegistry();

    private BrutalImpactsApi() {
    }

    @Nonnull
    public static HitParticleRegistry hitParticles() {
        return HIT_PARTICLES;
    }
}
