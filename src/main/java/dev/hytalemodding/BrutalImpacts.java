package dev.hytalemodding;

import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import dev.hytalemodding.api.BrutalImpactsApi;
import dev.hytalemodding.commands.BrutalImpactsCommand;
//import dev.hytalemodding.commands.ExampleCommand;
//import dev.hytalemodding.commands.ExamplePlayerCommand;
//import dev.hytalemodding.commands.ExampleTargetEntityCommand;
//import dev.hytalemodding.commands.HealPlayerCommand;
//import dev.hytalemodding.commands.ServerRulesCommand;
//import dev.hytalemodding.commands.SpawnParticleSystemCommand;
import dev.hytalemodding.config.BrutalImpactsFiles;
import dev.hytalemodding.events.ExampleEvent;
import dev.hytalemodding.systems.BrutalImpactsParticleSystem;

import javax.annotation.Nonnull;
import java.nio.file.Path;

/**
 * Main plugin entry point for Brutal Impacts.
 *
 * <p>Responsibilities:</p>
 * <ul>
 *   <li>Ensure the configuration folder exists and copy bundled resources (README + default JSON)</li>
 *   <li>Load hit particle rules from JSON into {@link BrutalImpactsApi}</li>
 *   <li>Register {@link BrutalImpactsParticleSystem} to append extra impact particles on damage events</li>
 *   <li>Register {@code /brutalimpacts} command for reload / runtime tweaking</li>
 * </ul>
 */
public class BrutalImpacts extends JavaPlugin {

    public BrutalImpacts(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        // Example scaffolding commands/events (kept as references while developing):
        // this.getCommandRegistry().registerCommand(new ExampleCommand("brutalhello", "Hello command from Brutal Impacts."));
        // this.getCommandRegistry().registerCommand(new ExamplePlayerCommand());
        // this.getCommandRegistry().registerCommand(new ServerRulesCommand());
        // this.getCommandRegistry().registerCommand(new ExampleTargetEntityCommand());
        // this.getCommandRegistry().registerCommand(new HealPlayerCommand());
        // this.getCommandRegistry().registerCommand(new SpawnParticleSystemCommand());
        this.getEventRegistry().registerGlobal(PlayerReadyEvent.class, ExampleEvent::onPlayerReady);

        // Hit particle rules are loaded from JSON in the BrutalImpacts data directory.
        // You can override the base directory with: -Dbrutalimpacts.dir=<path>
        Path dataDir = BrutalImpactsFiles.resolveDataDir(BrutalImpacts.class);
        try {
            BrutalImpactsFiles.ensureLayout(BrutalImpacts.class, dataDir);
            var report = BrutalImpactsFiles.clearAndLoadAllHitParticleJsonReport(BrutalImpactsApi.hitParticles(), dataDir);
            System.out.println("[BrutalImpacts] Loaded hit particle rules (" + report.rulesLoaded() + ") from " + dataDir
                + " [files ok=" + report.filesLoaded() + ", failed=" + report.filesFailed() + "]");
            for (var err : report.errors()) {
                System.out.println("[BrutalImpacts] JSON load failed: " + err.path() + " (" + err.message() + ")");
            }
        } catch (Exception e) {
            System.out.println("[BrutalImpacts] Failed to load hit particle rules JSON: " + e.getMessage());
            e.printStackTrace();
        }

        // Adds an extra impact particle system to all Damage events (without replacing existing ones).
        // Replace the id below with the id of your custom particle system asset.
        BrutalImpactsParticleSystem brutalParticles = new BrutalImpactsParticleSystem("BrutalImpacts_Hit_Blood_Default", 75.0, false);
        brutalParticles.setDefaultColor(new com.hypixel.hytale.protocol.Color((byte) 150, (byte) 0, (byte) 0));
        this.getEntityStoreRegistry().registerSystem(brutalParticles);
        this.getCommandRegistry().registerCommand(new BrutalImpactsCommand(brutalParticles, BrutalImpacts.class, dataDir));
    }
}
