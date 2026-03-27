package dev.hytalemodding.commands;

/*import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.spatial.SpatialResource;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.particle.config.ParticleSystem;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.DefaultArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import it.unimi.dsi.fastutil.objects.ObjectList;

import javax.annotation.Nonnull;

public class SpawnParticleSystemCommand extends AbstractPlayerCommand {

    private final RequiredArg<ParticleSystem> particleSystemArg;
    private final DefaultArg<Float> distanceArg;

    public SpawnParticleSystemCommand() {
        super("spawnparticlesystem", "Spawns a particle system at your position (useful to test your asset pack).");

        this.particleSystemArg = this.withRequiredArg(
            "particle",
            "Particle system to spawn (by id)",
            ArgTypes.PARTICLE_SYSTEM
        );

        this.distanceArg = this.withDefaultArg(
            "distance",
            "View distance for receivers",
            ArgTypes.FLOAT,
            75.0F,
            "Default: 75"
        );
    }

    @Override
    protected void execute(
        @Nonnull CommandContext context,
        @Nonnull Store<EntityStore> store,
        @Nonnull Ref<EntityStore> ref,
        @Nonnull PlayerRef playerRef,
        @Nonnull World world
    ) {
        ParticleSystem system = this.particleSystemArg.get(context);
        if (system == null) {
            context.sendMessage(Message.raw("Missing particle system argument."));
            return;
        }

        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) {
            context.sendMessage(Message.raw("Missing TransformComponent."));
            return;
        }

        SpatialResource<Ref<EntityStore>, EntityStore> playerSpatialResource = store.getResource(
            EntityModule.get().getPlayerSpatialResourceType()
        );
        ObjectList<Ref<EntityStore>> results = SpatialResource.getThreadLocalReferenceList();
        results.clear();

        float distance = this.distanceArg.get(context);
        playerSpatialResource.getSpatialStructure().collect(transform.getPosition(), distance, results);

        ParticleUtil.spawnParticleEffect(system.getId(), transform.getPosition(), transform.getRotation(), results, store);
        context.sendMessage(Message.raw("Spawned particle system: " + system.getId()));
    }
}*/

