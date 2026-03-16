package dev.hytalemodding.commands;

import java.util.concurrent.CompletableFuture;

import javax.annotation.Nonnull;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;

public class ServerRulesCommand extends AbstractAsyncCommand {
    
    //Constructor
    public ServerRulesCommand () {
        super("rules", "Lists the server rules.");
    }

    @Override
    protected CompletableFuture<Void> executeAsync(@Nonnull CommandContext context) {
        context.sendMessage(Message.raw("The only rule is there are no rules."));
        return CompletableFuture.completedFuture(null);
    } 
}
