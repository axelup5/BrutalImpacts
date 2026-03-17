package dev.hytalemodding.commands;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.DefaultArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.FlagArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractTargetPlayerCommand;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class HealPlayerCommand extends AbstractTargetPlayerCommand {
    private final DefaultArg<Float> healthArg;
    private final OptionalArg<String> messageArg;
    private final FlagArg debugArg;

    public HealPlayerCommand() {
        super("healplayer", "Heals a player for an <input> amount of HP. (default: 100)");

        this.healthArg = this.withDefaultArg("health", "Amount to heal player", ArgTypes.FLOAT, (float)100, "Default: 100");

        this.messageArg = this.withOptionalArg("message", "Message to print while healing", ArgTypes.STRING);

        this.debugArg = this.withFlagArg("debug", "Add debug logs");
    }

    @Override
    protected void execute(@NonNullDecl CommandContext context, @NullableDecl Ref<EntityStore> ref, @NonNullDecl Ref<EntityStore> ref1, @NonNullDecl PlayerRef playerRef, @NonNullDecl World world, @NonNullDecl Store<EntityStore> store) {
        if (this.debugArg.get(context) == true) {
            context.sendMessage(Message.raw("We are debugging."));
        }

        EntityStatMap stats = store.getComponent(ref, EntityStatMap.getComponentType());
        int healthIdx = DefaultEntityStatTypes.getHealth();
        EntityStatValue health = stats.get(healthIdx);

        float missing = health.getMax() - health.get();

        if (this.debugArg.get(context) == true) {
            context.sendMessage(Message.raw("Missing: " + missing + " health."));
            context.sendMessage(Message.raw("Adding: " + healthArg.get(context) + " health to "));
            context.sendMessage(Message.raw(messageArg.get(context)));
            context.sendMessage(Message.raw("Imput value: " + healthArg.get(context) + " Default"));
            context.sendMessage(Message.raw("Default health value: " + healthArg.getDefaultValue()));
        }

        stats.addStatValue(healthIdx, healthArg.get(context));
    }
}
