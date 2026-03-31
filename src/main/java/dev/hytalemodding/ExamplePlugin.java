package dev.hytalemodding;

import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import dev.hytalemodding.api.BrutalImpactsApi;
import dev.hytalemodding.api.HitParticleRulesJson;
import dev.hytalemodding.commands.BrutalImpactsCommand;
import dev.hytalemodding.commands.ExampleCommand;
//import dev.hytalemodding.commands.ExamplePlayerCommand;
//import dev.hytalemodding.commands.ExampleTargetEntityCommand;
import dev.hytalemodding.commands.HealPlayerCommand;
import dev.hytalemodding.commands.ServerRulesCommand;
//import dev.hytalemodding.commands.SpawnParticleSystemCommand;
import dev.hytalemodding.events.ExampleEvent;
import dev.hytalemodding.systems.BrutalImpactParticlesSystem;

import javax.annotation.Nonnull;
import java.nio.file.Files;
import java.nio.file.Path;

public class ExamplePlugin extends JavaPlugin {

    public ExamplePlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        this.getCommandRegistry().registerCommand(new ExampleCommand("brutalhello", "Hello command from Brutal Impacts."));
        //this.getCommandRegistry().registerCommand(new ExamplePlayerCommand());
        this.getCommandRegistry().registerCommand(new ServerRulesCommand());
        //this.getCommandRegistry().registerCommand(new ExampleTargetEntityCommand());
        this.getCommandRegistry().registerCommand(new HealPlayerCommand());
        //this.getCommandRegistry().registerCommand(new SpawnParticleSystemCommand());
        this.getEventRegistry().registerGlobal(PlayerReadyEvent.class, ExampleEvent::onPlayerReady);

        // Hit particle rules are loaded from JSON (supports thousands of mobs efficiently).
        // External override (no rebuild): config/brutalimpacts/hit_particles.json
        // Fallback: bundled resource /hit_particles.json
        int loadedRules = 0;
        try {
            Path external = Path.of("config", "brutalimpacts", "hit_particles.json");
            if (Files.exists(external)) {
                loadedRules = HitParticleRulesJson.clearAndLoadFromFile(BrutalImpactsApi.hitParticles(), external);
                System.out.println("[BrutalImpacts] Loaded hit particle rules from " + external + " (" + loadedRules + ")");
            } else {
                loadedRules = HitParticleRulesJson.clearAndLoadFromResource(BrutalImpactsApi.hitParticles(), ExamplePlugin.class, "/hit_particles.json");
                System.out.println("[BrutalImpacts] Loaded hit particle rules from bundled /hit_particles.json (" + loadedRules + ")");
            }
        } catch (Exception e) {
            System.out.println("[BrutalImpacts] Failed to load hit particle rules JSON: " + e.getMessage());
            e.printStackTrace();
        }

        // Adds an extra impact particle system to all Damage events (without replacing existing ones).
        // Replace the id below with the id of your custom particle system asset.
        BrutalImpactParticlesSystem brutalParticles = new BrutalImpactParticlesSystem("BrutalImpacts_Hit_Blood_Default", 75.0, true);
        brutalParticles.setDefaultColor(new com.hypixel.hytale.protocol.Color((byte) 150, (byte) 0, (byte) 0));
        this.getEntityStoreRegistry().registerSystem(brutalParticles);
        this.getCommandRegistry().registerCommand(new BrutalImpactsCommand(brutalParticles));
    }
}
