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
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
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
import dev.hytalemodding.api.BrutalImpactsApi;
import dev.hytalemodding.api.HitParticleEffect;
import dev.hytalemodding.api.HitParticleSpec;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static com.hypixel.hytale.component.dependency.Order.BEFORE;
import static com.hypixel.hytale.component.dependency.OrderPriority.CLOSEST;

/**
 * Adds extra impact particles to {@link Damage#IMPACT_PARTICLES} without replacing existing ones.
 *
 * <p>This system resolves hit particle rules via {@link BrutalImpactsApi#hitParticles()} using the target's model
 * asset id, then appends the resulting world particles to the damage metadata.</p>
 *
 * <p>This relies on the vanilla {@link DamageSystems.ApplyParticles} system to actually broadcast/spawn the particles.</p>
 */
public class BrutalImpactsParticleSystem extends DamageEventSystem {

    private static final ComponentType<EntityStore, TransformComponent> TRANSFORM_COMPONENT_TYPE = TransformComponent.getComponentType();
    private static final ComponentType<EntityStore, ModelComponent> MODEL_COMPONENT_TYPE = ModelComponent.getComponentType();
    private static final Query<EntityStore> QUERY = Query.and(TRANSFORM_COMPONENT_TYPE);

    private volatile String particleSystemId;
    private volatile @Nullable Color defaultColor;
    private volatile float defaultScale = 1.0F;
    private final double defaultViewDistance;
    private final boolean debug;

    /**
     * @param particleSystemId Fallback particle system id used when no rule matches.
     * @param defaultViewDistance Minimum view distance for spawned particles.
     * @param debug Enables verbose console output and source-player chat notifications.
     */
    public BrutalImpactsParticleSystem(@Nonnull String particleSystemId, double defaultViewDistance, boolean debug) {
        this.particleSystemId = particleSystemId;
        this.defaultViewDistance = defaultViewDistance;
        this.debug = debug;
    }

    @Nullable
    public Color getDefaultColor() {
        return this.defaultColor;
    }

    public void setDefaultColor(@Nullable Color defaultColor) {
        this.defaultColor = defaultColor;
    }

    public float getDefaultScale() {
        return this.defaultScale;
    }

    public void setDefaultScale(float defaultScale) {
        this.defaultScale = defaultScale;
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
        String defaultParticleSystemIdSnapshot = this.particleSystemId;
        if (defaultParticleSystemIdSnapshot == null || defaultParticleSystemIdSnapshot.isBlank()) {
            return;
        }

        String modelAssetId = null;
        ModelComponent modelComponent = archetypeChunk.getComponent(index, MODEL_COMPONENT_TYPE);
        if (modelComponent != null && modelComponent.getModel() != null) {
            modelAssetId = modelComponent.getModel().getModelAssetId();
        }

        if (this.debug) {
            System.out.println("[BrutalImpacts] targetModelAssetId=" + modelAssetId);
        }

        HitParticleSpec spec = BrutalImpactsApi.hitParticles().resolveSpec(modelAssetId, defaultParticleSystemIdSnapshot);
        List<HitParticleEffect> effects = spec.effects();
        if (effects == null || effects.isEmpty()) {
            return;
        }

        WorldParticle[] extras = buildWorldParticles(effects);
        if (extras.length == 0) {
            return;
        }

        Damage.Particles particles = damage.getIfPresentMetaObject(Damage.IMPACT_PARTICLES);
        if (particles == null) {
            particles = new Damage.Particles(new ModelParticle[0], extras, this.defaultViewDistance);
            damage.putMetaObject(Damage.IMPACT_PARTICLES, particles);
            this.debugNotify(commandBuffer, damage, "Created IMPACT_PARTICLES + appended " + extras.length + " world particles");
            this.spawnForPredictingSourceIfNeeded(index, archetypeChunk, commandBuffer, damage, extras);
            return;
        }

        appendWorldParticles(particles, extras);

        if (particles.getViewDistance() < this.defaultViewDistance) {
            particles.setViewDistance(this.defaultViewDistance);
        }

        this.debugNotify(commandBuffer, damage, "Ensured " + extras.length + " world particles are present");
        this.spawnForPredictingSourceIfNeeded(index, archetypeChunk, commandBuffer, damage, extras);
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
        @Nonnull WorldParticle[] extras
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

        for (WorldParticle extra : extras) {
            ParticleUtil.spawnParticleEffect(extra, targetPosition, justSource, commandBuffer);
        }
        this.debugNotify(commandBuffer, damage, "Sent predicted-only particles to source (" + extras.length + ")");
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
    private static String formatColor(@Nullable Color color) {
        if (color == null) {
            return "null";
        }
        return "rgb(" + (color.red & 0xFF) + "," + (color.green & 0xFF) + "," + (color.blue & 0xFF) + ")";
    }

    @Nonnull
    private WorldParticle[] buildWorldParticles(@Nonnull List<HitParticleEffect> effects) {
        ArrayList<WorldParticle> out = new ArrayList<>(effects.size());
        for (HitParticleEffect effect : effects) {
            if (effect == null || effect.particleSystemId() == null || effect.particleSystemId().isBlank()) {
                continue;
            }

            Color colorToUse = effect.colorOverride() != null ? effect.colorOverride() : this.defaultColor;
            float scaleToUse = effect.scale() > 0.0F ? effect.scale() : this.defaultScale;

            out.add(
                new WorldParticle(
                    effect.particleSystemId(),
                    colorToUse,
                    scaleToUse,
                    new Vector3f(0.0F, 0.0F, 0.0F),
                    new Direction(0.0F, 0.0F, 0.0F)
                )
            );
        }

        if (this.debug) {
            StringBuilder sb = new StringBuilder();
            sb.append("[BrutalImpacts] resolved effects:");
            for (WorldParticle wp : out) {
                sb.append(" {id=").append(wp.getSystemId())
                    .append(", scale=").append(wp.getScale())
                    .append(", color=").append(formatColor(wp.getColor()))
                    .append("}");
            }
            System.out.println(sb);
        }

        return out.toArray(new WorldParticle[0]);
    }

    private static void appendWorldParticles(@Nonnull Damage.Particles particles, @Nonnull WorldParticle[] extras) {
        WorldParticle[] existing = particles.getWorldParticles();

        ArrayList<WorldParticle> combined = new ArrayList<>((existing == null ? 0 : existing.length) + extras.length);
        if (existing != null) {
            for (WorldParticle wp : existing) {
                if (wp != null) {
                    combined.add(wp);
                }
            }
        }

        for (WorldParticle extra : extras) {
            if (extra == null) {
                continue;
            }

            boolean alreadyPresent = false;
            for (WorldParticle wp : combined) {
                if (sameWorldParticle(wp, extra)) {
                    alreadyPresent = true;
                    break;
                }
            }

            if (!alreadyPresent) {
                combined.add(extra);
            }
        }

        particles.setWorldParticles(combined.toArray(new WorldParticle[0]));
    }

    private static boolean sameWorldParticle(@Nullable WorldParticle a, @Nullable WorldParticle b) {
        if (a == null || b == null) {
            return false;
        }
        return Objects.equals(a.getSystemId(), b.getSystemId())
            && Float.compare(a.getScale(), b.getScale()) == 0
            && Objects.equals(a.getColor(), b.getColor());
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
