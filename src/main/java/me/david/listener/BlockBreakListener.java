package me.david.listener;

import me.david.EventCore;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class BlockBreakListener implements Listener {

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        final Player player = event.getPlayer();

        if (player.hasPermission("event.bypass")) {
            event.setCancelled(false);
            return;
        }

        // The waiting area is protected. Once the event starts, normal
        // survival block breaking is enabled.
        event.setCancelled(!EventCore.getInstance().getGameManager().isRunning());
    }

}
