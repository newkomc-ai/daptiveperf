package com.example.adaptiveperf;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Налаштування мода. Файл: config/adaptiveperf.json */
public class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("adaptiveperf.json");

    /** Увімкнено чи ні (перемикається клавішею K). */
    public boolean enabled = true;
    /** Цільовий FPS. Став нижче свого ліміту FPS (наприклад, ліміт 144 -> ціль 60). */
    public int targetFps = 60;
    /** Мінімальна дальність промальовки, нижче якої мод не опускається. */
    public int minRenderDistance = 6;
    /** На скільки чанків змінювати дальність за один крок. */
    public int renderDistanceStep = 2;
    /** Чи зменшувати частинки, коли дальності вже мінімальна, а FPS усе ще низький. */
    public boolean adjustParticles = true;
    /** Як часто міряти FPS (у тіках; 20 тіків = 1 секунда). */
    public int checkIntervalTicks = 20;
    /** Пауза після кожної зміни, щоб гра встигла стабілізуватися (у тіках). */
    public int cooldownTicks = 100;
    /** Знижувати, якщо середній FPS < targetFps * lowFactor. */
    public double lowFactor = 0.9;
    /** Повертати якість, якщо середній FPS > targetFps * recoverFactor. */
    public double recoverFactor = 1.3;
    /** Показувати повідомлення над хотбаром при змінах. */
    public boolean showMessages = true;

    public static Config load() {
        Config cfg = new Config();
        try {
            if (Files.exists(PATH)) {
                Config read = GSON.fromJson(Files.readString(PATH), Config.class);
                if (read != null) cfg = read;
            }
        } catch (Exception e) {
            AdaptivePerfClient.LOGGER.warn("Не вдалося прочитати конфіг, беру стандартний", e);
        }
        cfg.sanitize();
        cfg.save(); // дописує нові поля, якщо файл старий
        return cfg;
    }

    public void save() {
        try {
            Files.createDirectories(PATH.getParent());
            Files.writeString(PATH, GSON.toJson(this));
        } catch (IOException e) {
            AdaptivePerfClient.LOGGER.warn("Не вдалося зберегти конфіг", e);
        }
    }

    private void sanitize() {
        targetFps = Math.max(15, targetFps);
        minRenderDistance = Math.max(2, minRenderDistance);
        renderDistanceStep = Math.max(1, renderDistanceStep);
        checkIntervalTicks = Math.max(5, checkIntervalTicks);
        cooldownTicks = Math.max(20, cooldownTicks);
        lowFactor = Math.min(1.0, Math.max(0.3, lowFactor));
        recoverFactor = Math.max(1.05, recoverFactor);
    }
}
