package me.david.util;

import me.david.EventCore;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

public class AutoBroadcast implements Runnable {

    private final List<String> messages;
    private int index = 0;

    public AutoBroadcast() {
        messages = EventCore.getInstance().getConfig().getStringList("AutoBroadcast.Messages");
    }

    @Override
    public void run() {
        if (!(EventCore.getInstance().getConfig().getBoolean("AutoBroadcast.Enabled")) || messages.isEmpty()) {
            return;
        }

        if (index >= messages.size()) {
            index = 0;
        }

        String message = messages.get(index);
        if (EventCore.getInstance().getConfig().getBoolean("AutoBroadcast.UseBroadcastCommand")) {
            String command = EventCore.getInstance().getConfig().getString("AutoBroadcast.BroadcastCommand", "").replace("%message%", message);
            if (command.startsWith("/")) command = command.substring(1);
            final String finalCommand = command;
            me.david.util.folia.FoliaScheduler.getGlobalRegionScheduler().execute(EventCore.getInstance(), () ->
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand));
        } else {
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendMessage(Component.empty());
                player.sendMessage(Component.empty());
                player.sendMessage(MessageUtil.translateColorCodes(message));
                player.sendMessage(Component.empty());
                player.sendMessage(Component.empty());
            }
        }

        index++;
    }

}
