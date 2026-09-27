package me.david.listener;

import me.david.EventCore;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

public class BlockPlaceListener implements Listener {

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        final Player player = event.getPlayer();

        if (player.hasPermission("event.bypass")) {
            event.setCancelled(false);
            return;
        }

        // No building before the event starts.
        if (!EventCore.getInstance().getGameManager().isRunning()) {
            event.setCancelled(true);
            return;
        }

        // A configured value of 0 or less means there is no custom height limit.
        long maxBuildHeight = EventCore.getInstance().getConfig()
                .getLong("Settings.MaxBuildHeight", 0L);

        if (maxBuildHeight > 0 && event.getBlock().getY() > maxBuildHeight) {
            event.setCancelled(true);
        }
    }

}
