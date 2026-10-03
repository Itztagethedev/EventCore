package me.david.manager;

import lombok.Getter;
import me.david.EventCore;
import me.david.api.events.game.GameStartEvent;
import me.david.api.events.game.GameStopEvent;
import me.david.api.events.game.GameTimerTickEvent;
import me.david.api.events.game.InGameTimerTickEvent;
import me.david.util.BorderUtil;
import me.david.util.MessageUtil;
import me.david.util.PlayerUtil;
import me.david.util.folia.FoliaScheduler;
import me.david.util.folia.TaskWrapper;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
public class GameManager implements me.david.api.manager.GameManager {

    private boolean running = false;
    private volatile boolean timerRunning = false;

    private TaskWrapper startTask;
    private TaskWrapper autoStopTask;
    private TaskWrapper autoDropTask;
    private TaskWrapper timerTask;

    private AtomicInteger timer;
    private long inGameTimer;
    private boolean autoDropped = false;

    private String message(String path, String fallback, String... replacements) {
        String value = EventCore.getInstance().getConfig().getString(path, fallback);
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            value = value.replace(replacements[i], replacements[i + 1]);
        }
        return value;
    }

    private void showTitle(String titleText, String subtitleText) {
        Component title = MessageUtil.translateColorCodes(titleText);
        Component subtitle = MessageUtil.translateColorCodes(subtitleText);
        Title packet = Title.title(title, subtitle);

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.showTitle(packet);
        }
    }

    private void broadcastMessage(String text) {
        Component component = MessageUtil.getPrefix().append(MessageUtil.translateColorCodes(text));
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(component);
        }
    }

    private void dispatchConfiguredCommand(String command) {
        String finalCommand = command == null ? "" : command.trim();
        if (finalCommand.startsWith("/")) {
            finalCommand = finalCommand.substring(1);
        }

        if (!finalCommand.isEmpty()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand);
        }
    }

    public void start() {
        if (timerRunning || running) return;

        stopAllTimers();
        running = false;
        autoDropped = false;
        timerRunning = true;

        timer = new AtomicInteger(EventCore.getInstance().getConfig().getInt("Messages.StartTimer.Timer", 5));

        final GameStartEvent gameStartEvent = new GameStartEvent(timer.get());
        Bukkit.getPluginManager().callEvent(gameStartEvent);

        if (gameStartEvent.isCancelled()) {
            timerRunning = false;
            return;
        }

        startTask = FoliaScheduler.getGlobalRegionScheduler().runAtFixedRate(EventCore.getInstance(), o -> {
            if (!timerRunning || running) return;

            int current = timer.get();
            Bukkit.getPluginManager().callEvent(new GameTimerTickEvent(current));

            if (current > 0) {
                String timerMessage = message(
                        "Messages.StartTimer.Message",
                        "The game starts in &c%timer% seconds!",
                        "%timer%", String.valueOf(current)
                );

                String title = message(
                        "Messages.StartTimer.Title",
                        "%timer%",
                        "%timer%", String.valueOf(current)
                );

                String subtitle = message(
                        "Messages.StartTimer.SubTitle",
                        "",
                        "%timer%", String.valueOf(current)
                );

                String color = EventCore.getInstance().getConfig().getString(
                        "Messages.StartTimer.Colors." + current + "sec", ""
                );

                if (!color.isEmpty()) {
                    timerMessage = color + timerMessage;
                    title = color + title;
                    subtitle = color + subtitle;
                }

                Component chatMessage = MessageUtil.getPrefix().append(
                        MessageUtil.translateColorCodes(timerMessage)
                );
                Title titlePacket = Title.title(
                        MessageUtil.translateColorCodes(title),
                        MessageUtil.translateColorCodes(subtitle)
                );

                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.sendMessage(chatMessage);
                    player.showTitle(titlePacket);
                }
            } else {
                String startMessage = EventCore.getInstance().getConfig().getString(
                        "Messages.Start.Message",
                        "&aThe game starts now! Good luck!"
                );
                String startTitle = EventCore.getInstance().getConfig().getString(
                        "Messages.Start.Title",
                        "&aStart!"
                );
                String startSubtitle = EventCore.getInstance().getConfig().getString(
                        "Messages.Start.SubTitle",
                        ""
                );

                Component chatMessage = MessageUtil.getPrefix().append(
                        MessageUtil.translateColorCodes(startMessage)
                );
                Title titlePacket = Title.title(
                        MessageUtil.translateColorCodes(startTitle),
                        MessageUtil.translateColorCodes(startSubtitle)
                );

                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.sendMessage(chatMessage);
                    player.showTitle(titlePacket);
                }

                running = true;
                timerRunning = false;

                for (World world : Bukkit.getWorlds()) {
                    world.setDifficulty(Difficulty.HARD);
                }

                if (EventCore.getInstance().getConfig().getBoolean("Settings.IngameTimer.Enabled")
                        && !EventCore.getInstance().getConfig().getBoolean("Messages.Actionbar.Enabled")) {
                    startInGameTimer();
                }

                EventCore.getInstance().getConfig().getStringList("Settings.Start.CustomCommands")
                        .forEach(command -> FoliaScheduler.getGlobalRegionScheduler().execute(
                                EventCore.getInstance(),
                                () -> dispatchConfiguredCommand(command)
                        ));

                if (startTask != null) {
                    startTask.cancel();
                    startTask = null;
                }
            }

            if (timerRunning && timer.get() > 0) {
                timer.decrementAndGet();
            }
        }, 0, 20);

        if (EventCore.getInstance().getConfig().getBoolean("Settings.AutoStop1Player")) {
            autoStopTask = FoliaScheduler.getGlobalRegionScheduler().runAtFixedRate(EventCore.getInstance(), o -> {
                if (running && PlayerUtil.getAlive() == 1) {
                    running = false;

                    String winner = Bukkit.getOnlinePlayers().stream()
                            .filter(player -> player.getGameMode() == GameMode.SURVIVAL)
                            .findFirst()
                            .map(Player::getName)
                            .orElse("Unknown");

                    FoliaScheduler.getGlobalRegionScheduler().execute(
                            EventCore.getInstance(),
                            () -> stop(winner)
                    );
                }
            }, 0, 20);
        }

        if (EventCore.getInstance().getConfig().getBoolean("Settings.DropOnPlayerCount.Enabled")) {
            autoDropTask = FoliaScheduler.getGlobalRegionScheduler().runAtFixedRate(EventCore.getInstance(), o -> {
                if (running
                        && PlayerUtil.getAlive() <= EventCore.getInstance().getConfig().getLong("Settings.DropOnPlayerCount.Count")
                        && !autoDropped) {
                    autoDropped = true;
                    EventCore.getInstance().getMapManager().drop();
                }
            }, 0, 20);
        }
    }

    public void stop(final String winner) {
        final String winnerName = winner == null || winner.trim().isEmpty() ? "Unknown" : winner.trim();

        final GameStopEvent gameStopEvent = new GameStopEvent(winnerName);
        Bukkit.getPluginManager().callEvent(gameStopEvent);

        if (gameStopEvent.isCancelled()) {
            return;
        }

        running = false;
        timerRunning = false;
        BorderUtil.lastOptimal = 200;

        stopInGameTimer();
        stopAllTimers();

        if (EventCore.getInstance().getConfig().getBoolean("Messages.Stop.Enabled", true)) {
            String stopMessage = message(
                    "Messages.Stop.Message",
                    "&cThe game has ended! The winner is %winner%",
                    "%winner%", winnerName
            );
            String stopTitle = message(
                    "Messages.Stop.Title",
                    "&c%winner%",
                    "%winner%", winnerName
            );
            String stopSubtitle = message(
                    "Messages.Stop.SubTitle",
                    "&7is the winner",
                    "%winner%", winnerName
            );

            Component winnerMessage = MessageUtil.getPrefix().append(
                    MessageUtil.translateColorCodes(stopMessage)
            );
            Title winnerTitle = Title.title(
                    MessageUtil.translateColorCodes(stopTitle),
                    MessageUtil.translateColorCodes(stopSubtitle)
            );

            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendMessage(winnerMessage);
                player.showTitle(winnerTitle);
                PlayerUtil.cleanPlayer(player);
            }
        } else {
            for (Player player : Bukkit.getOnlinePlayers()) {
                PlayerUtil.cleanPlayer(player);
            }
        }

        for (World world : Bukkit.getWorlds()) {
            world.setDifficulty(Difficulty.PEACEFUL);
            world.getWorldBorder().setSize(BorderUtil.borderDefault);
        }

        EventCore.getInstance().getConfig().getStringList("Settings.Stop.CustomCommands")
                .forEach(command -> FoliaScheduler.getGlobalRegionScheduler().execute(
                        EventCore.getInstance(),
                        () -> dispatchConfiguredCommand(command)
                ));

        if (EventCore.getInstance().getConfig().getBoolean("Settings.MapReset.AutoReset")) {
            EventCore.getInstance().getMapManager().reset();
        }
    }

    public void startInGameTimer() {
        inGameTimer = 0;

        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }

        timerTask = FoliaScheduler.getGlobalRegionScheduler().runAtFixedRate(EventCore.getInstance(), o -> {
            inGameTimer++;

            Bukkit.getPluginManager().callEvent(new InGameTimerTickEvent(inGameTimer));

            String raw = Objects.requireNonNull(
                    EventCore.getInstance().getConfig().getString(
                            "Settings.IngameTimer.Format",
                            "&8» &chh:mm:ss &8«"
                    )
            )
                    .replace("hh", String.format("%02d", (inGameTimer / 3600)))
                    .replace("mm", String.format("%02d", ((inGameTimer % 3600) / 60)))
                    .replace("ss", String.format("%02d", (inGameTimer % 60)));

            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendActionBar(MessageUtil.translateColorCodes(raw));
            }
        }, 0, 20);
    }

    public void stopInGameTimer() {
        inGameTimer = 0;
        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }
    }

    private void stopAllTimers() {
        timerRunning = false;

        if (startTask != null) { startTask.cancel(); startTask = null; }
        if (autoStopTask != null) { autoStopTask.cancel(); autoStopTask = null; }
        if (autoDropTask != null) { autoDropTask.cancel(); autoDropTask = null; }
        if (timerTask != null) { timerTask.cancel(); timerTask = null; }
    }
}
