package me.david.listener;

import me.david.EventCore;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

public class PlayerInteractListener implements Listener {

    @EventHandler
    public void onPlayerInteractEvent(PlayerInteractEvent event) {
        final Player player = event.getPlayer();

        if (player.hasPermission("event.bypass")) {
            event.setCancelled(false);
            return;
        }

        // Players can use doors, chests, buttons, levers, crafting tables,
        // beds and other normal interactions once the event has started.
        event.setCancelled(!EventCore.getInstance().getGameManager().isRunning());
    }

}
