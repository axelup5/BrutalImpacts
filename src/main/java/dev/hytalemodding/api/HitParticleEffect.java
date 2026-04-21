package dev.hytalemodding.api;

import com.hypixel.hytale.protocol.Color;
import com.hypixel.hytale.protocol.Vector3f;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A single particle effect to spawn on hit.
 *
 * @param particleSystemId The particle system asset id (must be non-blank).
 * @param colorOverride Optional tint applied to the particle system.
 * @param scale Scale multiplier. Values {@code <= 0} are treated as "use default" by the runtime system.
 * @param inheritDefaultColor When {@code true} and {@code colorOverride} is {@code null}, the runtime uses its
 *                            configured default color (if any). When {@code false} and {@code colorOverride} is
 *                            {@code null}, the runtime uses {@code null} (no tint).
 * @param positionOffset Optional relative offset from the hit location.
 * @param fixedScale When {@code true}, the runtime keeps the authored scale and does not multiply it by damage.
 */
public record HitParticleEffect(
    @Nonnull String particleSystemId,
    @Nullable Color colorOverride,
    float scale,
    boolean inheritDefaultColor,
    @Nullable Vector3f positionOffset,
    boolean fixedScale
) {

    /**
     * Creates an effect with scale {@code 1.0} and no explicit tint override.
     *
     * <p>If the runtime has a default color configured, this effect will inherit it.</p>
     */
    public static HitParticleEffect of(@Nonnull String particleSystemId) {
        return new HitParticleEffect(particleSystemId, null, 1.0F, true, null, false);
    }

    /**
     * Creates an effect with no explicit tint override.
     *
     * <p>If the runtime has a default color configured, this effect will inherit it.</p>
     */
    public static HitParticleEffect of(@Nonnull String particleSystemId, float scale) {
        return new HitParticleEffect(particleSystemId, null, scale, true, null, false);
    }

    /**
     * Creates an effect that explicitly uses no tint, even if the runtime has a default color configured.
     */
    public static HitParticleEffect noTint(@Nonnull String particleSystemId, float scale) {
        return new HitParticleEffect(particleSystemId, null, scale, false, null, false);
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
        return new HitParticleEffect(particleSystemId, new Color(toByte(r), toByte(g), toByte(b)), scale, false, null, false);
    }

    public HitParticleEffect withOffset(@Nullable Vector3f positionOffset) {
        return new HitParticleEffect(this.particleSystemId, this.colorOverride, this.scale, this.inheritDefaultColor, positionOffset, this.fixedScale);
    }

    public HitParticleEffect withFixedScale(boolean fixedScale) {
        return new HitParticleEffect(this.particleSystemId, this.colorOverride, this.scale, this.inheritDefaultColor, this.positionOffset, fixedScale);
    }

    private static byte toByte(int value) {
        int clamped = Math.max(0, Math.min(255, value));
        return (byte) clamped;
    }
}
