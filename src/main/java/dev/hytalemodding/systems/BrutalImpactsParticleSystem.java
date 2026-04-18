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
import com.hypixel.hytale.math.util.TrigMathUtil;
import com.hypixel.hytale.protocol.Color;
import com.hypixel.hytale.protocol.Direction;
import com.hypixel.hytale.protocol.Vector3f;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.particle.config.WorldParticle;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
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
import java.util.Locale;
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

    private static final int MAX_EXTRA_WORLD_PARTICLES = 32;
    private static final float DEFAULT_SPREAD_DEGREES = 10.0F;

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

        TransformComponent targetTransform = archetypeChunk.getComponent(index, TRANSFORM_COMPONENT_TYPE);
        if (targetTransform == null) {
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

        Vector4d hitLocation = damage.getIfPresentMetaObject(Damage.HIT_LOCATION);
        Vector3d targetPosition = hitLocation == null
            ? targetTransform.getPosition()
            : new Vector3d(hitLocation.x, hitLocation.y, hitLocation.z);

        ImpactContext impact = resolveImpactContext(commandBuffer, damage, targetPosition);
        ImpactTuning tuning = ImpactTuning.from(damage, impact.weaponClass());

        WorldParticle[] extras = buildWorldParticles(effects, tuning, impact.rotation(), DEFAULT_SPREAD_DEGREES);
        if (extras.length == 0) {
            return;
        }

        Damage.Particles particles = damage.getIfPresentMetaObject(Damage.IMPACT_PARTICLES);
        if (particles == null) {
            particles = new Damage.Particles(new ModelParticle[0], extras, this.defaultViewDistance);
            damage.putMetaObject(Damage.IMPACT_PARTICLES, particles);
            this.debugNotify(commandBuffer, damage, "Created IMPACT_PARTICLES + appended " + extras.length + " world particles");
            this.spawnForPredictingSourceIfNeeded(commandBuffer, damage, targetPosition, impact, extras);
            return;
        }

        appendWorldParticles(particles, extras);

        if (particles.getViewDistance() < this.defaultViewDistance) {
            particles.setViewDistance(this.defaultViewDistance);
        }

        this.debugNotify(commandBuffer, damage, "Ensured " + extras.length + " world particles are present");
        this.spawnForPredictingSourceIfNeeded(commandBuffer, damage, targetPosition, impact, extras);
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
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull Damage damage,
        @Nonnull Vector3d targetPosition,
        @Nonnull ImpactContext impact,
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

        ObjectArrayList<Ref<EntityStore>> justSource = new ObjectArrayList<>(1);
        justSource.add(sourceRef);

        for (WorldParticle extra : extras) {
            ParticleUtil.spawnParticleEffect(
                extra,
                targetPosition,
                impact.rotation().yawToSource,
                0.0F,
                0.0F,
                null,
                justSource,
                commandBuffer
            );
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
    private WorldParticle[] buildWorldParticles(
        @Nonnull List<HitParticleEffect> effects,
        @Nonnull ImpactTuning tuning,
        @Nonnull ImpactRotation rotation,
        float spreadDegrees
    ) {
        int repeats = Math.max(1, tuning.repeats);
        float spreadRad = Math.max(0.0F, spreadDegrees) * TrigMathUtil.degToRad;

        int expected = Math.min(MAX_EXTRA_WORLD_PARTICLES, effects.size() * repeats);
        ArrayList<WorldParticle> out = new ArrayList<>(expected);

        for (HitParticleEffect effect : effects) {
            if (effect == null || effect.particleSystemId() == null || effect.particleSystemId().isBlank()) {
                continue;
            }

            Color colorToUse = resolveColor(effect);
            float baseScale = effect.scale() > 0.0F ? effect.scale() : this.defaultScale;
            float scaleToUse = clamp(baseScale * tuning.scaleMultiplier, 0.05F, 8.0F);

            int localRepeats = repeats;
            for (int i = 0; i < localRepeats && out.size() < MAX_EXTRA_WORLD_PARTICLES; i++) {
                float yawJitter = repeats <= 1 ? 0.0F : lerp(-spreadRad, spreadRad, (float) i / (float) (repeats - 1));
                float pitchJitter = repeats <= 1 ? 0.0F : (((i & 1) == 0) ? 0.35F : -0.35F) * (spreadRad * 0.5F);

                out.add(
                    new WorldParticle(
                        effect.particleSystemId(),
                        colorToUse,
                        scaleToUse,
                        new Vector3f(0.0F, 0.0F, 0.0F),
                        new Direction(rotation.yawOffset + yawJitter, rotation.pitchOffset + pitchJitter, 0.0F)
                    )
                );
            }
        }

        if (this.debug) {
            StringBuilder sb = new StringBuilder();
            sb.append("[BrutalImpacts] resolved effects:");
            for (WorldParticle wp : out) {
                sb.append(" {id=").append(wp.getSystemId())
                    .append(", scale=").append(wp.getScale())
                    .append(", color=").append(formatColor(wp.getColor()))
                    .append(", rotYaw=").append(wp.getRotationOffset() == null ? "null" : wp.getRotationOffset().yaw)
                    .append(", rotPitch=").append(wp.getRotationOffset() == null ? "null" : wp.getRotationOffset().pitch)
                    .append("}");
            }
            System.out.println(sb);
        }

        return out.toArray(new WorldParticle[0]);
    }

    @Nullable
    private Color resolveColor(@Nonnull HitParticleEffect effect) {
        if (effect.colorOverride() != null) {
            return effect.colorOverride();
        }
        return effect.inheritDefaultColor() ? this.defaultColor : null;
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
            && Objects.equals(a.getColor(), b.getColor())
            && Objects.equals(a.getPositionOffset(), b.getPositionOffset())
            && Objects.equals(a.getRotationOffset(), b.getRotationOffset());
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private record ImpactContext(@Nonnull WeaponClass weaponClass, @Nonnull ImpactRotation rotation) {
    }

    private record ImpactRotation(float yawToSource, float yawOffset, float pitchOffset) {
    }

    private record ImpactTuning(float scaleMultiplier, int repeats) {
        @Nonnull
        static ImpactTuning from(@Nonnull Damage damage, @Nonnull WeaponClass weaponClass) {
            float dmg = Math.max(0.0F, damage.getAmount());
            boolean isProjectile = damage.getSource() instanceof Damage.ProjectileSource;

            float scale = 0.85F + (dmg * 0.06F);
            int repeats = 1 + (int) Math.floor(dmg / 6.0F);

            if (isProjectile) {
                scale *= 0.85F;
                repeats = Math.max(1, repeats - 1);
            } else {
                scale *= 1.05F;
            }

            scale *= weaponClass.scaleMultiplier;
            repeats = Math.max(1, (int) Math.round(repeats * weaponClass.repeatMultiplier));

            scale = clamp(scale, 0.5F, 2.5F);
            repeats = Math.max(1, Math.min(5, repeats));
            return new ImpactTuning(scale, repeats);
        }
    }

    private enum WeaponClass {
        UNARMED(0.95F, 0.90F),
        SWORD(1.00F, 1.00F),
        AXE(1.05F, 1.05F),
        HAMMER(1.15F, 1.15F),
        SPEAR(1.00F, 1.05F),
        DAGGER(0.95F, 1.10F),
        BOW(0.90F, 0.85F),
        STAFF(0.95F, 0.95F),
        OTHER(1.00F, 1.00F);

        final float scaleMultiplier;
        final float repeatMultiplier;

        WeaponClass(float scaleMultiplier, float repeatMultiplier) {
            this.scaleMultiplier = scaleMultiplier;
            this.repeatMultiplier = repeatMultiplier;
        }
    }

    @Nonnull
    private static ImpactContext resolveImpactContext(
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull Damage damage,
        @Nonnull Vector3d targetPosition
    ) {
        WeaponClass weaponClass = WeaponClass.UNARMED;
        ImpactRotation rotation = new ImpactRotation(0.0F, 0.0F, 0.0F);

        if (damage.getSource() instanceof Damage.EntitySource sourceEntity) {
            Ref<EntityStore> sourceRef = sourceEntity.getRef();
            if (sourceRef != null && sourceRef.isValid()) {
                TransformComponent sourceTransform = commandBuffer.getComponent(sourceRef, TRANSFORM_COMPONENT_TYPE);
                if (sourceTransform != null) {
                    Vector3d sourcePos = sourceTransform.getPosition();
                    float yawToSource = TrigMathUtil.atan2(sourcePos.x - targetPosition.x, sourcePos.z - targetPosition.z);

                    double dx = targetPosition.x - sourcePos.x;
                    double dz = targetPosition.z - sourcePos.z;
                    double dy = targetPosition.y - sourcePos.y;
                    double horizontal = Math.sqrt((dx * dx) + (dz * dz));
                    float pitchAway = horizontal <= 1.0E-6 ? 0.0F : TrigMathUtil.atan2(dy, horizontal);

                    // Vanilla uses yawToSource for impact particles. To make "spray away from source", rotate by PI.
                    rotation = new ImpactRotation(yawToSource, TrigMathUtil.PI, pitchAway);
                }

                Player player = commandBuffer.getComponent(sourceRef, Player.getComponentType());
                if (player != null) {
                    ItemStack inHand = player.getInventory() == null ? null : player.getInventory().getItemInHand();
                    weaponClass = classifyWeapon(inHand);
                }
            }
        }

        return new ImpactContext(weaponClass, rotation);
    }


    @Nonnull
    private static WeaponClass classifyWeapon(@Nullable ItemStack inHand) {
        if (inHand == null || !inHand.isValid()) {
            return WeaponClass.UNARMED;
        }

        String itemId = inHand.getItemId();
        if (itemId == null || itemId.isBlank()) {
            return WeaponClass.UNARMED;
        }

        String id = itemId.toLowerCase(Locale.ROOT);
        if (id.contains("sword")) return WeaponClass.SWORD;
        if (id.contains("axe")) return WeaponClass.AXE;
        if (id.contains("hammer") || id.contains("mace")) return WeaponClass.HAMMER;
        if (id.contains("spear") || id.contains("pike")) return WeaponClass.SPEAR;
        if (id.contains("dagger") || id.contains("knife")) return WeaponClass.DAGGER;
        if (id.contains("bow") || id.contains("crossbow")) return WeaponClass.BOW;
        if (id.contains("staff") || id.contains("wand")) return WeaponClass.STAFF;
        return WeaponClass.OTHER;
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
