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

import java.util.Map;
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

    public void start() {
        if (EventCore.getInstance().getMapManager().getSpawnLocation() == null) {
            Bukkit.getServer().broadcast(MessageUtil.getPrefix().append(
                    MessageUtil.translateColorCodes("&cThe event cannot start because no spawn location is configured. Use /event setSpawn.")));
            return;
        }

        if (timerRunning) {
            Bukkit.getServer().broadcast(MessageUtil.getPrefix().append(
                    MessageUtil.translateColorCodes("&cAn event countdown is already running.")));
            return;
        }

        stopAllTimers();
        running = false;
        autoDropped = false;
        timerRunning = true;
        timer = new AtomicInteger(Math.max(1,
                EventCore.getInstance().getConfig().getInt("Messages.StartTimer.Timer", 5)));

        GameStartEvent gameStartEvent = new GameStartEvent(timer.get());
        Bukkit.getPluginManager().callEvent(gameStartEvent);

        if (gameStartEvent.isCancelled()) {
            timerRunning = false;
            return;
        }

        Component countdownStarted = MessageUtil.getPrefix().append(
                MessageUtil.translateColorCodes("&7Event countdown started."));
        Bukkit.getServer().broadcast(countdownStarted);

        startTask = FoliaScheduler.getGlobalRegionScheduler().runAtFixedRate(EventCore.getInstance(), task -> {
            if (!timerRunning || running) return;

            int current = timer.get();
            Bukkit.getPluginManager().callEvent(new GameTimerTickEvent(current));

            String configuredColor = EventCore.getInstance().getConfig()
                    .getString("Messages.StartTimer.Colors." + current + "sec", "&c");

            String timerText = MessageUtil.translateColorCodes(configuredColor + current + "&7");
            Map<String, Component> replacements = Map.of(
                    "%timer%", timerText,
                    "%prefix%", MessageUtil.getPrefix()
            );

            for (Player player : Bukkit.getOnlinePlayers()) {
                if (current > 0) {
                    player.sendMessage(MessageUtil.getPrefix().append(
                            MessageUtil.format("Messages.StartTimer.Message", replacements)));

                    player.showTitle(Title.title(
                            MessageUtil.format("Messages.StartTimer.Title", replacements),
                            MessageUtil.format("Messages.StartTimer.SubTitle", replacements)));
                } else {
                    player.sendMessage(MessageUtil.getPrefix().append(
                            MessageUtil.get("Messages.Start.Message")));

                    player.showTitle(Title.title(
                            MessageUtil.get("Messages.Start.Title"),
                            MessageUtil.get("Messages.Start.SubTitle")));
                }
            }

            if (current <= 0) {
                for (World world : Bukkit.getWorlds()) {
                    world.setDifficulty(Difficulty.HARD);
                }

                if (EventCore.getInstance().getConfig().getBoolean("Settings.IngameTimer.Enabled")
                        && !EventCore.getInstance().getConfig().getBoolean("Messages.Actionbar.Enabled")) {
                    startInGameTimer();
                }

                EventCore.getInstance().getConfig().getStringList("Settings.Start.CustomCommands")
                        .forEach(command -> FoliaScheduler.getGlobalRegionScheduler().execute(EventCore.getInstance(),
                                () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                                        command.startsWith("/") ? command.substring(1) : command)));

                running = true;
                timerRunning = false;

                if (startTask != null) {
                    startTask.cancel();
                    startTask = null;
                }
            } else {
                timer.decrementAndGet();
            }
        }, 1, 20);

        if (EventCore.getInstance().getConfig().getBoolean("Settings.AutoStop1Player")) {
            autoStopTask = FoliaScheduler.getGlobalRegionScheduler().runAtFixedRate(EventCore.getInstance(), task -> {
                if (running && PlayerUtil.getAlive() == 1) {
                    running = false;
                    FoliaScheduler.getGlobalRegionScheduler().execute(EventCore.getInstance(), () -> stop(
                            Bukkit.getOnlinePlayers().stream()
                                    .filter(player -> player.getGameMode() == GameMode.SURVIVAL)
                                    .findFirst()
                                    .map(Player::getName)
                                    .orElse("Unknown")));
                }
            }, 20, 20);
        }

        if (EventCore.getInstance().getConfig().getBoolean("Settings.DropOnPlayerCount.Enabled")) {
            autoDropTask = FoliaScheduler.getGlobalRegionScheduler().runAtFixedRate(EventCore.getInstance(), task -> {
                if (running && PlayerUtil.getAlive() <= EventCore.getInstance().getConfig()
                        .getLong("Settings.DropOnPlayerCount.Count") && !autoDropped) {
                    autoDropped = true;
                    EventCore.getInstance().getMapManager().drop();
                }
            }, 20, 20);
        }
    }

    public void stop(final String winner) {
        final GameStopEvent gameStopEvent = new GameStopEvent(winner);
        Bukkit.getPluginManager().callEvent(gameStopEvent);

        if (gameStopEvent.isCancelled()) return;

        running = false;
        timerRunning = false;
        BorderUtil.lastOptimal = 200;
        stopInGameTimer();
        stopAllTimers();

        final String safeWinner = winner == null || winner.isBlank() ? "Unknown" : winner;
        Map<String, Component> replacements = Map.of(
                "%winner%", MessageUtil.translateColorCodes(safeWinner),
                "%prefix%", MessageUtil.getPrefix()
        );

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(MessageUtil.getPrefix().append(
                    MessageUtil.format("Messages.Stop.Message", replacements)));
            player.showTitle(Title.title(
                    MessageUtil.format("Messages.Stop.Title", replacements),
                    MessageUtil.format("Messages.Stop.SubTitle", replacements)));
            PlayerUtil.cleanPlayer(player);
        }

        for (World world : Bukkit.getWorlds()) {
            world.setDifficulty(Difficulty.PEACEFUL);
            world.getWorldBorder().setSize(BorderUtil.borderDefault);
        }

        EventCore.getInstance().getConfig().getStringList("Settings.Stop.CustomCommands")
                .forEach(command -> FoliaScheduler.getGlobalRegionScheduler().execute(EventCore.getInstance(),
                        () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                                command.startsWith("/") ? command.substring(1) : command)));

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

        timerTask = FoliaScheduler.getGlobalRegionScheduler().runAtFixedRate(EventCore.getInstance(), task -> {
            inGameTimer++;
            Bukkit.getPluginManager().callEvent(new InGameTimerTickEvent(inGameTimer));

            String raw = Objects.requireNonNull(EventCore.getInstance().getConfig()
                    .getString("Settings.IngameTimer.Format"))
                    .replace("hh", String.format("%02d", inGameTimer / 3600))
                    .replace("mm", String.format("%02d", (inGameTimer % 3600) / 60))
                    .replace("ss", String.format("%02d", inGameTimer % 60));

            for (org.bukkit.entity.Player player : Bukkit.getOnlinePlayers()) {
                player.sendActionBar(MessageUtil.translateColorCodes(raw));
            }
        }, 1, 20);
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
