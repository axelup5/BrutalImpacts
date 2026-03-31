package dev.hytalemodding.api;

import com.hypixel.hytale.protocol.Color;

import javax.annotation.Nullable;

public record HitParticleSpec(String particleSystemId, @Nullable Color colorOverride) {
}