package dev.hytalemodding.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractTargetEntityCommand;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import it.unimi.dsi.fastutil.objects.ObjectList;

public class ExampleTargetEntityCommand extends AbstractTargetEntityCommand {

    public ExampleTargetEntityCommand() {
        super("targetentity", "This is a target entity command");
    }

    protected void execute(CommandContext context, ObjectList<Ref<EntityStore>> ref, World world, Store<EntityStore> store) {

        EntityStatMap stats = store.getComponent(ref.getFirst(), EntityStatMap.getComponentType());

        if (stats == null) {
            context.sendMessage(Message.raw("This entity has no stats"));
            return;
        }

        int healthIdx = DefaultEntityStatTypes.getHealth();
        EntityStatValue health = stats.get(healthIdx);

        if (health == null) {
            context.sendMessage(Message.raw("This entity has no health."));
            return;
        }

        stats.addStatValue(healthIdx, 100);
    }
}
