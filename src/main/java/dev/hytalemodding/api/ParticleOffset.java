package dev.hytalemodding.api;

public record ParticleOffset(float x, float y, float z) {
    public static final ParticleOffset ZERO = new ParticleOffset(0.0F, 0.0F, 0.0F);
}
