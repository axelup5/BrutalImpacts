package dev.hytalemodding.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.particle.config.ParticleSystem;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import dev.hytalemodding.systems.BrutalImpactParticlesSystem;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

public class BrutalImpactsCommand extends AbstractCommand {

    private final BrutalImpactParticlesSystem particlesSystem;
    private final OptionalArg<ParticleSystem> particleArg;

    public BrutalImpactsCommand(@Nonnull BrutalImpactParticlesSystem particlesSystem) {
        super("brutalimpacts", "Brutal Impacts settings.");
        this.particlesSystem = particlesSystem;

        this.particleArg = this.withOptionalArg(
            "particle",
            "Default particle system id to use on hit (e.g. BrutalImpacts_Hit_Blood_V3 / Impact_Sword_Bash)",
            ArgTypes.PARTICLE_SYSTEM
        );
    }

    @Nullable
    @Override
    protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
        ParticleSystem particleSystem = this.particleArg.get(context);
        if (particleSystem == null) {
            context.sendMessage(Message.raw("Current particle: " + this.particlesSystem.getParticleSystemId()));
            context.sendMessage(Message.raw("Usage: /brutalimpacts --particle <ParticleSystemId>"));
            return CompletableFuture.completedFuture(null);
        }

        this.particlesSystem.setParticleSystemId(particleSystem.getId());
        context.sendMessage(Message.raw("Updated hit particle to: " + particleSystem.getId()));
        return CompletableFuture.completedFuture(null);
    }
}

