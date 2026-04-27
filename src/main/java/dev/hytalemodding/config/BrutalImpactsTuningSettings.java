package dev.hytalemodding.config;

import javax.annotation.Nonnull;

/**
 * User-facing runtime tuning for the damage-driven particle scaling formula.
 */
public record BrutalImpactsTuningSettings(
    float minScale,
    float maxScale,
    float scaleMultiplier,
    float particleMultiplier
) {

    public static final float DEFAULT_MIN_SCALE = 0.5F;
    public static final float DEFAULT_MAX_SCALE = 1.6F;
    public static final float DEFAULT_SCALE_MULTIPLIER = 1.0F;
    public static final float DEFAULT_PARTICLE_MULTIPLIER = 1.0F;

    public static final BrutalImpactsTuningSettings CLEAN =
        new BrutalImpactsTuningSettings(0.35F, 1.0F, 0.75F, 0.7F);
    public static final BrutalImpactsTuningSettings NORMAL =
        new BrutalImpactsTuningSettings(DEFAULT_MIN_SCALE, DEFAULT_MAX_SCALE, DEFAULT_SCALE_MULTIPLIER, DEFAULT_PARTICLE_MULTIPLIER);
    public static final BrutalImpactsTuningSettings BRUTAL =
        new BrutalImpactsTuningSettings(0.7F, 2.3F, 1.35F, 1.75F);

    @Nonnull
    public BrutalImpactsTuningSettings normalized() {
        float normalizedMinScale = clamp(this.minScale, 0.05F, 8.0F);
        float normalizedMaxScale = clamp(this.maxScale, normalizedMinScale, 8.0F);
        float normalizedScaleMultiplier = clamp(this.scaleMultiplier, 0.1F, 5.0F);
        float normalizedParticleMultiplier = clamp(this.particleMultiplier, 0.1F, 5.0F);

        return new BrutalImpactsTuningSettings(
            normalizedMinScale,
            normalizedMaxScale,
            normalizedScaleMultiplier,
            normalizedParticleMultiplier
        );
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
