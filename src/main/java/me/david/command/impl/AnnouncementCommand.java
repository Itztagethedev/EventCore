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
        super("announce", "event.command", "announcement");
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

        String messageFormat = plugin.getConfig().getString(
                "Messages.AnnoucementCommand.MessageFormat",
                "%prefix% %message%"
        );
        String titleText = plugin.getConfig().getString(
                "Messages.AnnoucementCommand.Title.Title",
                ""
        );
        String subtitleText = plugin.getConfig().getString(
                "Messages.AnnoucementCommand.Title.SubTitle",
                ""
        );

        String prefix = plugin.getConfig().getString("Messages.Prefix", "");

        String formattedMessage = messageFormat
                .replace("%prefix%", prefix)
                .replace("%message%", message);

        String formattedTitle = titleText
                .replace("%prefix%", prefix)
                .replace("%message%", message);

        String formattedSubtitle = subtitleText
                .replace("%prefix%", prefix)
                .replace("%message%", message);

        Component announcementMessage = MessageUtil.translateColorCodes(formattedMessage);

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(announcementMessage);

            if (plugin.getConfig().getBoolean(
                    "Messages.AnnoucementCommand.Title.Enabled", true)) {
                Component title = MessageUtil.translateColorCodes(formattedTitle);
                Component subtitle = MessageUtil.translateColorCodes(formattedSubtitle);
                player.showTitle(Title.title(title, subtitle));
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
