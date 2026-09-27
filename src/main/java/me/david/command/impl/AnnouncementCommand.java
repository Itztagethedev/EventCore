package me.david.command.impl;

import me.david.EventCore;
import me.david.command.BukkitCommand;
import me.david.util.MessageUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
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
        super("annoucement", "event.command", "announce");
        this.plugin = plugin;
    }
    @Override
    public void onCommand(CommandSender sender, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(MessageUtil.getPrefix().append(MessageUtil.translateColorCodes("Usage: &c/announce <message>")));
            return;
        }
        String message = String.join(" ", args);
        var replacements = Map.of("%prefix%", MessageUtil.getPrefix(), "%message%", MessageUtil.translateColorCodes(message));
        Component chat = MessageUtil.format("Messages.AnnoucementCommand.MessageFormat", replacements);
        Bukkit.broadcastMessage(LegacyComponentSerializer.legacySection().serialize(chat));
        if (plugin.getConfig().getBoolean("Messages.AnnoucementCommand.Title.Enabled", true)) {
            Component title = MessageUtil.format("Messages.AnnoucementCommand.Title.Title", replacements);
            Component subtitle = MessageUtil.format("Messages.AnnoucementCommand.Title.SubTitle", replacements);
            String legacyTitle = LegacyComponentSerializer.legacySection().serialize(title);
            String legacySubtitle = LegacyComponentSerializer.legacySection().serialize(subtitle);
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.showTitle(Title.title(title, subtitle));
                player.sendTitle(legacyTitle, legacySubtitle, 10, 60, 10);
            }
        }
    }
    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, String[] args) {
        return new ArrayList<>();
    }
}