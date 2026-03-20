package dev.hytalemodding.systems;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.spatial.SpatialResource;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector4d;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.protocol.Color;
import com.hypixel.hytale.protocol.Direction;
import com.hypixel.hytale.protocol.Vector3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.particle.config.WorldParticle;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelParticle;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;

import it.unimi.dsi.fastutil.objects.ObjectList;

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
    private static final ComponentType<EntityStore, NetworkId> NETWORK_ID_COMPONENT_TYPE = NetworkId.getComponentType();
    private static final Query<EntityStore> QUERY = Query.and(TRANSFORM_COMPONENT_TYPE);

    private final String particleSystemId;
    private final double defaultViewDistance;
    private final boolean debug;

    public BrutalImpactParticlesSystem(@Nonnull String particleSystemId, double defaultViewDistance, boolean debug) {
        this.particleSystemId = particleSystemId;
        this.defaultViewDistance = defaultViewDistance;
        this.debug = debug;
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
        if (this.particleSystemId.isBlank()) {
            return;
        }

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
            this.debugNotify(commandBuffer, damage, "Created IMPACT_PARTICLES + appended " + this.particleSystemId);
            this.spawnDirectIfNeeded(index, archetypeChunk, commandBuffer, damage, extra, this.defaultViewDistance);
            return;
        }

        WorldParticle[] existing = particles.getWorldParticles();
        if (existing != null) {
            for (WorldParticle worldParticle : existing) {
                if (worldParticle != null && this.particleSystemId.equals(worldParticle.getSystemId())) {
                    return;
                }
            }
        }

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

        this.debugNotify(commandBuffer, damage, "Appended world particle " + this.particleSystemId);
        this.spawnDirectIfNeeded(index, archetypeChunk, commandBuffer, damage, extra, particles.getViewDistance());
    }

    private void spawnDirectIfNeeded(
        int index,
        @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull Damage damage,
        @Nonnull WorldParticle extra,
        double viewDistance
    ) {
        NetworkId targetNetworkId = archetypeChunk.getComponent(index, NETWORK_ID_COMPONENT_TYPE);

        boolean applyParticlesLikelyToRun = false;
        if (targetNetworkId != null && damage.getSource() instanceof Damage.EntitySource sourceEntity) {
            if (sourceEntity.getRef().isValid()) {
                TransformComponent sourceTransform = commandBuffer.getComponent(sourceEntity.getRef(), TransformComponent.getComponentType());
                applyParticlesLikelyToRun = sourceTransform != null;
            }
        }

        if (applyParticlesLikelyToRun && !this.debug) {
            return;
        }

        TransformComponent targetTransform = archetypeChunk.getComponent(index, TRANSFORM_COMPONENT_TYPE);
        if (targetTransform == null) {
            return;
        }

        Vector4d hitLocation = damage.getIfPresentMetaObject(Damage.HIT_LOCATION);
        Vector3d targetPosition = hitLocation == null
            ? targetTransform.getPosition()
            : new Vector3d(hitLocation.x, hitLocation.y, hitLocation.z);

        if (this.debug) {
            System.out.println(
                "[BrutalImpacts] direct spawn pos=" + targetPosition + " viewDistance=" + viewDistance + " targetHasNetworkId=" + (targetNetworkId != null)
            );
        }

        SpatialResource<Ref<EntityStore>, EntityStore> playerSpatialResource = commandBuffer.getResource(
            EntityModule.get().getPlayerSpatialResourceType()
        );
        ObjectList<Ref<EntityStore>> results = SpatialResource.getThreadLocalReferenceList();
        results.clear();
        playerSpatialResource.getSpatialStructure().collect(targetPosition, viewDistance, results);

        ParticleUtil.spawnParticleEffect(extra, targetPosition, results, commandBuffer);
        this.debugNotify(commandBuffer, damage, "Direct-spawn fallback ran for " + this.particleSystemId);
    }

    private void debugNotify(@Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull Damage damage, @Nonnull String msg) {
        if (!this.debug) {
            return;
        }

        String sourceType = damage.getSource() == null ? "null" : damage.getSource().getClass().getName();
        System.out.println("[BrutalImpacts] " + msg + " (source=" + sourceType + ")");

        if (damage.getSource() instanceof Damage.EntitySource sourceEntity) {
            Player player = commandBuffer.getComponent(sourceEntity.getRef(), Player.getComponentType());
            if (player != null) {
                player.sendMessage(Message.raw("[BrutalImpacts] " + msg + " (source=" + sourceType + ")"));
            }
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
