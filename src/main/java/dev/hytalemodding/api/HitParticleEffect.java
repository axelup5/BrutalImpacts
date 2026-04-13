package dev.hytalemodding.api;

import com.hypixel.hytale.protocol.Color;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A single particle effect to spawn on hit.
 *
 * @param particleSystemId The particle system asset id (must be non-blank).
 * @param colorOverride Optional tint applied to the particle system. If {@code null}, the caller's default color
 *                      (if any) is used.
 * @param scale Scale multiplier. Values {@code <= 0} are treated as "use default" by the runtime system.
 */
public record HitParticleEffect(@Nonnull String particleSystemId, @Nullable Color colorOverride, float scale) {

    /**
     * Creates an effect with scale {@code 1.0} and no tint override.
     */
    public static HitParticleEffect of(@Nonnull String particleSystemId) {
        return new HitParticleEffect(particleSystemId, null, 1.0F);
    }

    /**
     * Creates an effect with no tint override.
     */
    public static HitParticleEffect of(@Nonnull String particleSystemId, float scale) {
        return new HitParticleEffect(particleSystemId, null, scale);
    }

    /**
     * Creates a tinted effect with scale {@code 1.0}.
     */
    public static HitParticleEffect tinted(@Nonnull String particleSystemId, int r, int g, int b) {
        return tinted(particleSystemId, r, g, b, 1.0F);
    }

    /**
     * Creates a tinted effect with an explicit scale.
     *
     * <p>RGB values are clamped to {@code [0, 255]}.</p>
     */
    public static HitParticleEffect tinted(@Nonnull String particleSystemId, int r, int g, int b, float scale) {
        return new HitParticleEffect(particleSystemId, new Color(toByte(r), toByte(g), toByte(b)), scale);
    }

    private static byte toByte(int value) {
        int clamped = Math.max(0, Math.min(255, value));
        return (byte) clamped;
    }
}
