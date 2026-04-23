package dev.hytalemodding.events;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;

public class ExampleEvent {

    public static void onPlayerReady(PlayerReadyEvent event) {
        Player player = event.getPlayer();
        if (player.getPlayerRef() != null) {
            player.getPlayerRef().sendMessage(Message.raw("Welcome " + player.getPlayerRef().getUsername() + " to Brutal Impacts."));
        }
    }

}
