package dev.hytalemodding.config;

import javax.annotation.Nonnull;

/**
 * Runtime tuning values applied on top of the base damage-driven particle formula.
 */
public record WeaponTuningProfile(float scaleMultiplier, float particleMultiplier) {

    public static final WeaponTuningProfile IDENTITY = new WeaponTuningProfile(1.0F, 1.0F);

    @Nonnull
    public WeaponTuningProfile multiply(@Nonnull WeaponTuningProfile other) {
        return new WeaponTuningProfile(
            this.scaleMultiplier * other.scaleMultiplier,
            this.particleMultiplier * other.particleMultiplier
        );
    }
}
