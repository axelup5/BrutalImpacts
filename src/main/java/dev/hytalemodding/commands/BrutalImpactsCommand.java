package dev.hytalemodding.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.particle.config.ParticleSystem;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.DefaultArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.FlagArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import dev.hytalemodding.api.BrutalImpactsApi;
import dev.hytalemodding.config.BrutalImpactsFiles;
import dev.hytalemodding.systems.BrutalImpactParticlesSystem;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public class BrutalImpactsCommand extends AbstractCommand {

    private final BrutalImpactParticlesSystem particlesSystem;
    private final Class<?> pluginClass;
    private final Path dataDir;

    private final DefaultArg<String> actionArg;
    private final FlagArg reloadFlag;
    private final OptionalArg<ParticleSystem> particleArg;

    public BrutalImpactsCommand(@Nonnull BrutalImpactParticlesSystem particlesSystem, @Nonnull Class<?> pluginClass, @Nonnull Path dataDir) {
        super("brutalimpacts", "Brutal Impacts settings.");
        this.particlesSystem = particlesSystem;
        this.pluginClass = pluginClass;
        this.dataDir = dataDir;

        this.actionArg = this.withDefaultArg("action", "Action (reload)", ArgTypes.STRING, "", "Default: (none)");
        this.reloadFlag = this.withFlagArg("reload", "Reload hit particle JSON files");

        this.particleArg = this.withOptionalArg(
            "particle",
            "Default particle system id to use on hit (e.g. BrutalImpacts_Hit_Blood_V3 / Impact_Sword_Bash)",
            ArgTypes.PARTICLE_SYSTEM
        );
    }

    @Nullable
    @Override
    protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
        String action = this.actionArg.get(context);
        boolean wantsReload = (action != null && action.equalsIgnoreCase("reload")) || Boolean.TRUE.equals(this.reloadFlag.get(context));
        if (wantsReload) {
            try {
                BrutalImpactsFiles.ensureLayout(this.pluginClass, this.dataDir);
                var report = BrutalImpactsFiles.clearAndLoadAllHitParticleJsonReport(BrutalImpactsApi.hitParticles(), this.dataDir);
                context.sendMessage(
                    Message.raw(
                        "Reloaded hit particles: rules=" + report.rulesLoaded()
                            + ", filesOk=" + report.filesLoaded()
                            + ", filesFailed=" + report.filesFailed()
                    )
                );
                for (var err : report.errors()) {
                    context.sendMessage(Message.raw("JSON load failed: " + err.path().getFileName() + " (" + err.message() + ")"));
                }
            } catch (Exception e) {
                context.sendMessage(Message.raw("Reload failed: " + e.getMessage()));
            }
            return CompletableFuture.completedFuture(null);
        }

        ParticleSystem particleSystem = this.particleArg.get(context);
        if (particleSystem == null) {
            context.sendMessage(Message.raw("Current particle: " + this.particlesSystem.getParticleSystemId()));
            context.sendMessage(Message.raw("Usage: /brutalimpacts reload"));
            context.sendMessage(Message.raw("   or: /brutalimpacts --reload"));
            context.sendMessage(Message.raw("   or: /brutalimpacts --particle <ParticleSystemId>"));
            return CompletableFuture.completedFuture(null);
        }

        this.particlesSystem.setParticleSystemId(particleSystem.getId());
        context.sendMessage(Message.raw("Updated hit particle to: " + particleSystem.getId()));
        return CompletableFuture.completedFuture(null);
    }
}

