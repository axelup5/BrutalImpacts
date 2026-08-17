package dev.hytalemodding.events;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class ExampleEvent {

    public static void onPlayerReady(PlayerReadyEvent event) {
        Ref<EntityStore> playerEntityRef = event.getPlayerRef();
        if (playerEntityRef != null && playerEntityRef.isValid()) {
            PlayerRef playerRef = playerEntityRef.getStore().getComponent(playerEntityRef, PlayerRef.getComponentType());
            if (playerRef != null) {
                playerRef.sendMessage(Message.raw("Welcome " + playerRef.getUsername() + " to Brutal Impacts."));
            }
        }
    }

}
