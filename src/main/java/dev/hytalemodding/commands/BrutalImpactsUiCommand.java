package dev.hytalemodding.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hytalemodding.config.BrutalImpactsTuningStore;
import dev.hytalemodding.systems.BrutalImpactsParticleSystem;
import dev.hytalemodding.ui.BrutalImpactsSettingsPage;

import javax.annotation.Nonnull;

public final class BrutalImpactsUiCommand extends AbstractPlayerCommand {

    private final BrutalImpactsParticleSystem particleSystem;
    private final BrutalImpactsTuningStore tuningStore;
    private final BrutalImpactsCommand brutalImpactsCommand;

    public BrutalImpactsUiCommand(
        @Nonnull BrutalImpactsParticleSystem particleSystem,
        @Nonnull BrutalImpactsTuningStore tuningStore,
        @Nonnull BrutalImpactsCommand brutalImpactsCommand
    ) {
        super("brutalimpactsui", "Open the Brutal Impacts tuning interface.");
        this.requirePermission("axelup.brutalimpacts.command.brutalimpactsui");
        this.particleSystem = particleSystem;
        this.tuningStore = tuningStore;
        this.brutalImpactsCommand = brutalImpactsCommand;
    }

    @Override
    protected void execute(
        @Nonnull CommandContext context,
        @Nonnull Store<EntityStore> store,
        @Nonnull Ref<EntityStore> ref,
        @Nonnull PlayerRef playerRef,
        @Nonnull World world
    ) {
        var page = new BrutalImpactsSettingsPage(playerRef, this.particleSystem, this.tuningStore, this.brutalImpactsCommand);
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player != null) {
            player.getPageManager().openCustomPage(ref, store, page);
        }
    }
}
