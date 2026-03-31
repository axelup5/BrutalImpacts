package dev.hytalemodding.api;

import com.hypixel.hytale.protocol.Color;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public record HitParticleEffect(@Nonnull String particleSystemId, @Nullable Color colorOverride, float scale) {

    public static HitParticleEffect of(@Nonnull String particleSystemId) {
        return new HitParticleEffect(particleSystemId, null, 1.0F);
    }

    public static HitParticleEffect tinted(@Nonnull String particleSystemId, int r, int g, int b) {
        return tinted(particleSystemId, r, g, b, 1.0F);
    }

    public static HitParticleEffect tinted(@Nonnull String particleSystemId, int r, int g, int b, float scale) {
        return new HitParticleEffect(particleSystemId, new Color(toByte(r), toByte(g), toByte(b)), scale);
    }

    private static byte toByte(int value) {
        int clamped = Math.max(0, Math.min(255, value));
        return (byte) clamped;
    }
}
