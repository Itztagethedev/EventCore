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
import java.util.Map;

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
        Map<String, Component> replacements = Map.of(
                "%prefix%", MessageUtil.getPrefix(),
                "%message%", MessageUtil.translateColorCodes(message)
        );

        Component chat = MessageUtil.format("Messages.AnnoucementCommand.MessageFormat", replacements);
        Bukkit.getServer().broadcast(chat);

        if (plugin.getConfig().getBoolean("Messages.AnnoucementCommand.Title.Enabled", true)) {
            Component title = MessageUtil.format("Messages.AnnoucementCommand.Title.Title", replacements);
            Component subtitle = MessageUtil.format("Messages.AnnoucementCommand.Title.SubTitle", replacements);

            Title titlePacket = Title.title(title, subtitle);
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.showTitle(titlePacket);
            }
        }
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, String[] args) {
        return new ArrayList<>();
    }
}
