package dev.hytalemodding.api;

import javax.annotation.Nonnull;
import java.util.List;

public record HitParticleSpec(@Nonnull List<HitParticleEffect> effects) {

    public HitParticleSpec {
        effects = List.copyOf(effects);
    }

    public static HitParticleSpec single(@Nonnull HitParticleEffect effect) {
        return new HitParticleSpec(List.of(effect));
    }
}

