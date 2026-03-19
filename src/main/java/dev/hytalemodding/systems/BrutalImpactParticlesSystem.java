package dev.hytalemodding.systems;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.protocol.Color;
import com.hypixel.hytale.protocol.Direction;
import com.hypixel.hytale.protocol.Vector3f;
import com.hypixel.hytale.server.core.asset.type.particle.config.WorldParticle;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelParticle;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;

import static com.hypixel.hytale.component.dependency.Order.BEFORE;
import static com.hypixel.hytale.component.dependency.OrderPriority.CLOSEST;

/**
 * Adds extra impact particles to {@link Damage#IMPACT_PARTICLES} without replacing existing ones.
 *
 * This relies on the vanilla {@link DamageSystems.ApplyParticles} system to actually broadcast/spawn the particles.
 */
public class BrutalImpactParticlesSystem extends DamageEventSystem {

    private static final ComponentType<EntityStore, TransformComponent> TRANSFORM_COMPONENT_TYPE = TransformComponent.getComponentType();
    private static final Query<EntityStore> QUERY = Query.and(TRANSFORM_COMPONENT_TYPE);

    private final String particleSystemId;
    private final double defaultViewDistance;

    public BrutalImpactParticlesSystem(@Nonnull String particleSystemId, double defaultViewDistance) {
        this.particleSystemId = particleSystemId;
        this.defaultViewDistance = defaultViewDistance;
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return QUERY;
    }

    @Override
    public void handle(
        int index,
        @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
        @Nonnull Store<EntityStore> store,
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull Damage damage
    ) {
        WorldParticle extra = new WorldParticle(
            this.particleSystemId,
            new Color((byte) 120, (byte) 0, (byte) 0),
            1.0F,
            new Vector3f(0.0F, 0.0F, 0.0F),
            new Direction(0.0F, 0.0F, 0.0F)
        );

        Damage.Particles particles = damage.getIfPresentMetaObject(Damage.IMPACT_PARTICLES);
        if (particles == null) {
            particles = new Damage.Particles(new ModelParticle[0], new WorldParticle[] { extra }, this.defaultViewDistance);
            damage.putMetaObject(Damage.IMPACT_PARTICLES, particles);
            return;
        }

        WorldParticle[] existing = particles.getWorldParticles();
        if (existing == null || existing.length == 0) {
            particles.setWorldParticles(new WorldParticle[] { extra });
        } else {
            WorldParticle[] combined = new WorldParticle[existing.length + 1];
            System.arraycopy(existing, 0, combined, 0, existing.length);
            combined[existing.length] = extra;
            particles.setWorldParticles(combined);
        }

        if (particles.getViewDistance() < this.defaultViewDistance) {
            particles.setViewDistance(this.defaultViewDistance);
        }
    }

    @Nonnull
    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return Set.of(new SystemDependency<>(BEFORE, DamageSystems.ApplyParticles.class, CLOSEST));
    }

    @Nullable
    @Override
    public SystemGroup<EntityStore> getGroup() {
        return DamageModule.get().getInspectDamageGroup();
    }
}
