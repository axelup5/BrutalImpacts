package dev.hytalemodding;

import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import dev.hytalemodding.commands.ExampleCommand;
import dev.hytalemodding.commands.ExamplePlayerCommand;
import dev.hytalemodding.commands.ExampleTargetEntityCommand;
import dev.hytalemodding.commands.HealPlayerCommand;
import dev.hytalemodding.commands.ServerRulesCommand;
import dev.hytalemodding.commands.SpawnParticleSystemCommand;
import dev.hytalemodding.events.ExampleEvent;
import dev.hytalemodding.systems.BrutalImpactParticlesSystem;

import javax.annotation.Nonnull;

public class ExamplePlugin extends JavaPlugin {

    public ExamplePlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        this.getCommandRegistry().registerCommand(new ExampleCommand("brutalimpacts", "An example command from de Brutal Impacts Mod."));
        this.getCommandRegistry().registerCommand(new ExamplePlayerCommand());
        this.getCommandRegistry().registerCommand(new ServerRulesCommand());
        this.getCommandRegistry().registerCommand(new ExampleTargetEntityCommand());
        this.getCommandRegistry().registerCommand(new HealPlayerCommand());
        this.getCommandRegistry().registerCommand(new SpawnParticleSystemCommand());
        this.getEventRegistry().registerGlobal(PlayerReadyEvent.class, ExampleEvent::onPlayerReady);

        // Adds an extra impact particle system to all Damage events (without replacing existing ones).
        // Replace the id below with the id of your custom particle system asset.
        this.getEntityStoreRegistry().registerSystem(
            new BrutalImpactParticlesSystem("BrutalImpacts_Blood", 75.0, true)
        );
    }
}
