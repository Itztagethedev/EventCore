package me.david.command.impl;

import me.david.EventCore;
import me.david.command.BukkitCommand;
import me.david.util.MessageUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class AnnouncementCommand extends BukkitCommand {

    private final EventCore plugin;

    public AnnouncementCommand(EventCore plugin) {
        super("announce", "event.command");
        this.plugin = plugin;
    }

    @Override
    public void onCommand(CommandSender sender, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(MessageUtil.getPrefix().append(
                    MessageUtil.translateColorCodes("&cUsage: /announce <message>")));
            return;
        }

        String message = String.join(" ", args);

        // Display announcement with new color code and format
        String announcementTitle = MessageUtil.translateColorCodes("&#88C0EC&lANNOUNCEMENT");
        Component announcement = announcementTitle;

        // Send title screen announcement to all online players
        for (Player player : Bukkit.getOnlinePlayers()) {
            // Show title on title screen
            Component titleComponent = MessageUtil.translateColorCodes("&#88C0EC&lANNOUNCEMENT");
            Component subtitleComponent = MessageUtil.translateColorCodes(message);
            Title titlePacket = Title.title(titleComponent, subtitleComponent);
            player.showTitle(titlePacket);
        }

        // Also send chat message to console
        sender.sendMessage(MessageUtil.getPrefix().append(
                MessageUtil.translateColorCodes("&#88C0EC&l&o✓ Announcement sent to all players")));
    }

    @Override
    public @NotNull List<String> tabComplete(
            @NotNull CommandSender sender,
            @NotNull String alias,
            String[] args
    ) {
        return new ArrayList<>();
    }
}
