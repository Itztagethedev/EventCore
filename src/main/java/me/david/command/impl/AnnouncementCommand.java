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

        String configuredFormat = plugin.getConfig().getString(
                "Messages.AnnoucementCommand.MessageFormat",
                "%prefix% %message%"
        );

        String prefix = plugin.getConfig().getString("Messages.Prefix", "");
        String formatted = configuredFormat
                .replace("%prefix%", prefix)
                .replace("%message%", message);

        Component announcement = MessageUtil.translateColorCodes(formatted);

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(announcement);
        }

        if (plugin.getConfig().getBoolean("Messages.AnnoucementCommand.Title.Enabled", true)) {
            String titleText = plugin.getConfig().getString(
                    "Messages.AnnoucementCommand.Title.Title", "");
            String subtitleText = plugin.getConfig().getString(
                    "Messages.AnnoucementCommand.Title.SubTitle", "");

            titleText = titleText.replace("%prefix%", prefix).replace("%message%", message);
            subtitleText = subtitleText.replace("%prefix%", prefix).replace("%message%", message);

            Component title = MessageUtil.translateColorCodes(titleText);
            Component subtitle = MessageUtil.translateColorCodes(subtitleText);
            Title titlePacket = Title.title(title, subtitle);

            for (Player player : Bukkit.getOnlinePlayers()) {
                player.showTitle(titlePacket);
            }
        }
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
