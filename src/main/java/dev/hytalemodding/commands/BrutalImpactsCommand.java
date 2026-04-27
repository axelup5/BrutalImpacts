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
import dev.hytalemodding.config.BrutalImpactsTuningStore;
import dev.hytalemodding.config.WeaponTuningRegistry;
import dev.hytalemodding.systems.BrutalImpactsParticleSystem;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * Server command for managing Brutal Impacts at runtime.
 *
 * <p>Supported actions:</p>
 * <ul>
 *   <li>Reload JSON rule files from disk: {@code /brutalimpacts reload} or {@code /brutalimpacts --reload}</li>
 *   <li>Set the fallback particle system id (used when no rule matches): {@code /brutalimpacts --particle <id>}</li>
 * </ul>
 *
 * <p>Note: the fallback particle system id is kept in memory and is not persisted to disk.</p>
 */
public class BrutalImpactsCommand extends AbstractCommand {

    private final BrutalImpactsParticleSystem particlesSystem;
    private final WeaponTuningRegistry weaponTuningRegistry;
    private final BrutalImpactsTuningStore tuningStore;
    private final Class<?> pluginClass;
    private final Path dataDir;

    private final DefaultArg<String> actionArg;
    private final DefaultArg<String> valueArg;
    private final FlagArg reloadFlag;
    private final OptionalArg<ParticleSystem> particleArg;

    public BrutalImpactsCommand(
        @Nonnull BrutalImpactsParticleSystem particlesSystem,
        @Nonnull WeaponTuningRegistry weaponTuningRegistry,
        @Nonnull BrutalImpactsTuningStore tuningStore,
        @Nonnull Class<?> pluginClass,
        @Nonnull Path dataDir
    ) {
        super("brutalimpacts", "Manage Brutal Impacts (reload JSON rules / set fallback particle).");
        this.particlesSystem = particlesSystem;
        this.weaponTuningRegistry = weaponTuningRegistry;
        this.tuningStore = tuningStore;
        this.pluginClass = pluginClass;
        this.dataDir = dataDir;

        this.actionArg = this.withDefaultArg("action", "Action (reload)", ArgTypes.STRING, "", "Default: (none)");
        this.valueArg = this.withDefaultArg("value", "Action value (on/off/toggle)", ArgTypes.STRING, "", "Default: (none)");
        this.reloadFlag = this.withFlagArg("reload", "Reload hit particle JSON files");

        this.particleArg = this.withOptionalArg(
            "particle",
            "Default particle system id to use on hit (e.g. BrutalImpacts_Hit_Blood_Default / Impact_Sword_Bash / BrutalImpacts_Hit_Fire)",
            ArgTypes.PARTICLE_SYSTEM
        );
    }

    @Nullable
    @Override
    protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
        String action = this.actionArg.get(context);
        String value = this.valueArg.get(context);
        boolean wantsReload = (action != null && action.equalsIgnoreCase("reload")) || Boolean.TRUE.equals(this.reloadFlag.get(context));
        if (wantsReload) {
            context.sendMessage(Message.raw(this.reloadAll()));
            return CompletableFuture.completedFuture(null);
        }

        if (action != null && action.equalsIgnoreCase("debug")) {
            String normalized = value == null ? "" : value.trim().toLowerCase();
            boolean enabled;
            switch (normalized) {
                case "", "toggle" -> enabled = this.particlesSystem.toggleDebug();
                case "on", "true", "1" -> {
                    this.particlesSystem.setDebugEnabled(true);
                    enabled = true;
                }
                case "off", "false", "0" -> {
                    this.particlesSystem.setDebugEnabled(false);
                    enabled = false;
                }
                default -> {
                    context.sendMessage(Message.raw("Usage: /brutalimpacts debug [on|off|toggle]"));
                    return CompletableFuture.completedFuture(null);
                }
            }

            context.sendMessage(Message.raw("Brutal Impacts debug mode: " + (enabled ? "ON" : "OFF")));
            return CompletableFuture.completedFuture(null);
        }

        ParticleSystem particleSystem = this.particleArg.get(context);
        if (particleSystem == null) {
            context.sendMessage(Message.raw("Current default particle: " + this.particlesSystem.getParticleSystemId()));
            context.sendMessage(Message.raw("Debug mode: " + (this.particlesSystem.isDebugEnabled() ? "ON" : "OFF")));
            context.sendMessage(Message.raw("Runtime tuning: min=" + this.tuningStore.get().minScale()
                + ", max=" + this.tuningStore.get().maxScale()
                + ", scaleMul=" + this.tuningStore.get().scaleMultiplier()
                + ", particleMul=" + this.tuningStore.get().particleMultiplier()));
            context.sendMessage(Message.raw("Usage: /brutalimpacts reload"));
            context.sendMessage(Message.raw("   or: /brutalimpacts --reload"));
            context.sendMessage(Message.raw("   or: /brutalimpacts debug [on|off|toggle]"));
            context.sendMessage(Message.raw("   or: /brutalimpacts --particle <ParticleSystemId>"));
            context.sendMessage(Message.raw("   or: /brutalimpactsui"));
            return CompletableFuture.completedFuture(null);
        }

        this.particlesSystem.setParticleSystemId(particleSystem.getId());
        context.sendMessage(Message.raw("Updated fallback hit particle to: " + particleSystem.getId()));
        return CompletableFuture.completedFuture(null);
    }

    @Nonnull
    public String reloadAll() {
        try {
            BrutalImpactsFiles.ensureLayout(this.pluginClass, this.dataDir);
            var hitParticlesReport = BrutalImpactsFiles.clearAndLoadAllHitParticleJsonReport(BrutalImpactsApi.hitParticles(), this.dataDir);
            var weaponsReport = BrutalImpactsFiles.clearAndLoadAllWeaponTuningJsonReport(this.weaponTuningRegistry, this.dataDir);
            this.particlesSystem.applyTuningSettings(this.tuningStore.load());

            StringBuilder message = new StringBuilder("Reloaded Brutal Impacts config: hitRules=")
                .append(hitParticlesReport.rulesLoaded())
                .append(", weaponRules=")
                .append(weaponsReport.rulesLoaded())
                .append(", failed=")
                .append(hitParticlesReport.filesFailed() + weaponsReport.filesFailed());

            for (var err : hitParticlesReport.errors()) {
                message.append(" | Hit JSON failed: ").append(err.path().getFileName()).append(" (").append(err.message()).append(")");
            }
            for (var err : weaponsReport.errors()) {
                message.append(" | Weapons JSON failed: ").append(err.path().getFileName()).append(" (").append(err.message()).append(")");
            }
            return message.toString();
        } catch (Exception e) {
            return "Reload failed: " + e.getMessage();
        }
    }
}
