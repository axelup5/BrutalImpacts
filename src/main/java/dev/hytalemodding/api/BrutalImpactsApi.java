package dev.hytalemodding.api;

import javax.annotation.Nonnull;

public final class BrutalImpactsApi {

    private static final HitParticleRegistry HIT_PARTICLES = new HitParticleRegistry();

    private BrutalImpactsApi() {
    }

    @Nonnull
    public static HitParticleRegistry hitParticles() {
        return HIT_PARTICLES;
    }
}

