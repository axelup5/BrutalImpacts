package dev.hytalemodding.systems;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector4d;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.protocol.Color;
import com.hypixel.hytale.protocol.Direction;
import com.hypixel.hytale.protocol.Vector3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.particle.config.WorldParticle;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelParticle;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

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

    private volatile String particleSystemId;
    private final double defaultViewDistance;
    private final boolean debug;

    public BrutalImpactParticlesSystem(@Nonnull String particleSystemId, double defaultViewDistance, boolean debug) {
        this.particleSystemId = particleSystemId;
        this.defaultViewDistance = defaultViewDistance;
        this.debug = debug;
    }

    @Nonnull
    public String getParticleSystemId() {
        return this.particleSystemId;
    }

    public void setParticleSystemId(@Nonnull String particleSystemId) {
        this.particleSystemId = particleSystemId;
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
        String particleSystemIdSnapshot = this.particleSystemId;
        if (particleSystemIdSnapshot == null || particleSystemIdSnapshot.isBlank()) {
            return;
        }

        WorldParticle extra = new WorldParticle(
            particleSystemIdSnapshot,
            new Color((byte) 120, (byte) 0, (byte) 0),
            1.0F,
            new Vector3f(0.0F, 0.0F, 0.0F),
            new Direction(0.0F, 0.0F, 0.0F)
        );

        Damage.Particles particles = damage.getIfPresentMetaObject(Damage.IMPACT_PARTICLES);
        if (particles == null) {
            particles = new Damage.Particles(new ModelParticle[0], new WorldParticle[] { extra }, this.defaultViewDistance);
            damage.putMetaObject(Damage.IMPACT_PARTICLES, particles);
            this.debugNotify(commandBuffer, damage, "Created IMPACT_PARTICLES + appended " + particleSystemIdSnapshot);
            this.spawnForPredictingSourceIfNeeded(index, archetypeChunk, commandBuffer, damage, extra);
            return;
        }

        WorldParticle[] existing = particles.getWorldParticles();
        if (existing != null) {
            for (WorldParticle worldParticle : existing) {
                if (worldParticle != null && particleSystemIdSnapshot.equals(worldParticle.getSystemId())) {
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

        this.debugNotify(commandBuffer, damage, "Appended world particle " + particleSystemIdSnapshot);
        this.spawnForPredictingSourceIfNeeded(index, archetypeChunk, commandBuffer, damage, extra);
    }

    /**
     * If the damage can be predicted, vanilla {@link DamageSystems.ApplyParticles} intentionally does NOT send
     * world-particle packets to the predicting source (it passes {@code particleSource=sourceRef}).
     *
     * That means when you're alone, you might not see particles at all.
     *
     * This method sends the extra particle effect ONLY to the predicting player so:
     * - The attacker sees the effect
     * - Other nearby players still receive it from vanilla (no duplicates)
     */
    private void spawnForPredictingSourceIfNeeded(
        int index,
        @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull Damage damage,
        @Nonnull WorldParticle extra
    ) {
        boolean canBePredicted = damage.getMetaStore().getMetaObject(Damage.CAN_BE_PREDICTED);
        if (!canBePredicted) {
            return;
        }

        if (!(damage.getSource() instanceof Damage.EntitySource sourceEntity)) {
            return;
        }

        Ref<EntityStore> sourceRef = sourceEntity.getRef();
        if (!sourceRef.isValid()) {
            return;
        }

        // Safety: ParticleUtil asserts that every targetRef has a PlayerRef component.
        PlayerRef sourcePlayerRef = commandBuffer.getComponent(sourceRef, PlayerRef.getComponentType());
        if (sourcePlayerRef == null) {
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

        ObjectArrayList<Ref<EntityStore>> justSource = new ObjectArrayList<>(1);
        justSource.add(sourceRef);

        ParticleUtil.spawnParticleEffect(extra, targetPosition, justSource, commandBuffer);
        this.debugNotify(commandBuffer, damage, "Sent predicted-only particle to source " + extra.getSystemId());
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
