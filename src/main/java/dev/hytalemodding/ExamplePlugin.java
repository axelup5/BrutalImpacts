package dev.hytalemodding;

import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import dev.hytalemodding.api.BrutalImpactsApi;
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

        // Example hit-particle rules by target model asset id (tweak strings to match your actual assets).
        // More specific rules should be registered first.
        BrutalImpactsApi.hitParticles().registerModelContainsTint("Skeleton_Burnt", "BrutalImpacts_Hit_Blood_Default", 0, 0, 0);
        BrutalImpactsApi.hitParticles().registerModelContainsTint("Skeleton_Sand", "BrutalImpacts_Hit_Bone_Default", 0, 0, 0);
        BrutalImpactsApi.hitParticles().registerModelContains("Skeleton", "BrutalImpacts_Hit_Bone_Default");        
        BrutalImpactsApi.hitParticles().registerModelContainsTint("Spider", "BrutalImpacts_Hit_Blood_Default", 255, 255, 255);

        // Adds an extra impact particle system to all Damage events (without replacing existing ones).
        // Replace the id below with the id of your custom particle system asset.
        BrutalImpactParticlesSystem brutalParticles = new BrutalImpactParticlesSystem("BrutalImpacts_Hit_Blood_Default", 75.0, true);
        this.getEntityStoreRegistry().registerSystem(brutalParticles);
        this.getCommandRegistry().registerCommand(new BrutalImpactsCommand(brutalParticles));
    }
}
