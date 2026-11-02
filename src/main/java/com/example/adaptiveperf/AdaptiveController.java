package com.example.adaptiveperf;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.ParticlesMode;
import net.minecraft.text.Text;

/**
 * Логіка адаптації. Кожні checkIntervalTicks тіків міряємо FPS, беремо середнє з останніх
 * вимірів і:
 *  - якщо FPS низький: спочатку зменшуємо дальність промальовки, потім частинки;
 *  - якщо FPS з запасом: спочатку повертаємо частинки, потім дальність (але не вище, ніж поставив сам гравець).
 */
public class AdaptiveController {
    private static final int SAMPLES_NEEDED = 3;

    private final Config config;

    // Що гравець виставив сам (до чого повертаємось)
    private int baseView = -1;
    private ParticlesMode baseParticles = null;
    // Що ми виставили востаннє (щоб помітити ручну зміну гравцем)
    private int expectedView = -1;
    private ParticlesMode expectedParticles = null;

    private final int[] samples = new int[SAMPLES_NEEDED];
    private int sampleCount = 0;
    private int tickCounter = 0;
    private int cooldown = 0;
    private boolean wasIdle = true;

    public AdaptiveController(Config config) {
        this.config = config;
    }

    public void tick(MinecraftClient mc) {
        boolean idle = !config.enabled || mc.world == null || mc.player == null
                || mc.currentScreen != null || mc.isPaused();
        if (idle) {
            wasIdle = true;
            return;
        }
        if (wasIdle) {
            // Щойно повернулись у гру (меню, пауза, вхід у світ): FPS у цей момент не показовий
            wasIdle = false;
            resetSamples();
            cooldown = Math.max(cooldown, 40);
        }

        GameOptions opts = mc.options;
        syncBaseline(opts);

        if (cooldown > 0) {
            cooldown--;
            return;
        }
        if (++tickCounter < config.checkIntervalTicks) return;
        tickCounter = 0;

        samples[sampleCount++ % SAMPLES_NEEDED] = mc.getCurrentFps();
        if (sampleCount < SAMPLES_NEEDED) return;

        double avg = 0;
        for (int s : samples) avg += s;
        avg /= SAMPLES_NEEDED;

        boolean changed = false;
        if (avg < config.targetFps * config.lowFactor) {
            changed = degrade(mc, opts, avg);
        } else if (avg > config.targetFps * config.recoverFactor) {
            changed = improve(mc, opts, avg);
        }

        if (changed) {
            resetSamples();
            cooldown = config.cooldownTicks;
        }
    }

    /** Якщо гравець сам змінив налаштування, запам'ятовуємо їх як нову "базу". */
    private void syncBaseline(GameOptions opts) {
        int view = opts.getViewDistance().getValue();
        if (view != expectedView) {
            baseView = view;
            expectedView = view;
        }
        ParticlesMode particles = opts.getParticles().getValue();
        if (particles != expectedParticles) {
            baseParticles = particles;
            expectedParticles = particles;
        }
    }

    private boolean degrade(MinecraftClient mc, GameOptions opts, double avg) {
        int view = opts.getViewDistance().getValue();
        if (view > config.minRenderDistance) {
            int next = Math.max(config.minRenderDistance, view - config.renderDistanceStep);
            setView(opts, next);
            notify(mc, "Низький FPS (" + Math.round(avg) + "): дальність " + view + " → " + next);
            return true;
        }
        if (config.adjustParticles) {
            ParticlesMode p = opts.getParticles().getValue();
            if (p.ordinal() < ParticlesMode.MINIMAL.ordinal()) {
                ParticlesMode next = ParticlesMode.values()[p.ordinal() + 1];
                setParticles(opts, next);
                notify(mc, "Низький FPS (" + Math.round(avg) + "): частинки → " + next.name());
                return true;
            }
        }
        return false;
    }

    private boolean improve(MinecraftClient mc, GameOptions opts, double avg) {
        ParticlesMode p = opts.getParticles().getValue();
        if (config.adjustParticles && baseParticles != null && p.ordinal() > baseParticles.ordinal()) {
            ParticlesMode next = ParticlesMode.values()[p.ordinal() - 1];
            setParticles(opts, next);
            notify(mc, "FPS у нормі (" + Math.round(avg) + "): частинки → " + next.name());
            return true;
        }
        int view = opts.getViewDistance().getValue();
        if (baseView != -1 && view < baseView) {
            int next = Math.min(baseView, view + config.renderDistanceStep);
            setView(opts, next);
            notify(mc, "FPS у нормі (" + Math.round(avg) + "): дальність " + view + " → " + next);
            return true;
        }
        return false;
    }

    private void setView(GameOptions opts, int value) {
        opts.getViewDistance().setValue(value);
        expectedView = opts.getViewDistance().getValue();
    }

    private void setParticles(GameOptions opts, ParticlesMode value) {
        opts.getParticles().setValue(value);
        expectedParticles = opts.getParticles().getValue();
    }

    /** Повертає налаштування гравця (при виході зі світу, вимкненні мода, закритті гри). */
    public void restore(MinecraftClient mc) {
        GameOptions opts = mc.options;
        if (opts == null) return;
        if (baseView != -1 && opts.getViewDistance().getValue() == expectedView) {
            opts.getViewDistance().setValue(baseView);
        }
        if (baseParticles != null && opts.getParticles().getValue() == expectedParticles) {
            opts.getParticles().setValue(baseParticles);
        }
        baseView = -1;
        expectedView = -1;
        baseParticles = null;
        expectedParticles = null;
        resetSamples();
    }

    public void toggle(MinecraftClient mc) {
        config.enabled = !config.enabled;
        config.save();
        if (!config.enabled) restore(mc);
        if (mc.player != null) {
            mc.player.sendMessage(Text.literal("Adaptive Performance: " + (config.enabled ? "увімкнено" : "вимкнено")), true);
        }
    }

    private void resetSamples() {
        sampleCount = 0;
        tickCounter = 0;
    }

    private void notify(MinecraftClient mc, String msg) {
        AdaptivePerfClient.LOGGER.info(msg);
        if (config.showMessages && mc.player != null) {
            mc.player.sendMessage(Text.literal(msg), true);
        }
    }
}
