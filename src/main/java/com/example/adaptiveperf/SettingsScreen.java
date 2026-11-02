package com.example.adaptiveperf;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;

/** Меню налаштувань (відкривається клавішею K). */
public class SettingsScreen extends Screen {
    private final Screen parent;
    private final Config config;
    private final AdaptiveController controller;

    public SettingsScreen(Screen parent, Config config, AdaptiveController controller) {
        super(Text.literal("Adaptive Performance"));
        this.parent = parent;
        this.config = config;
        this.controller = controller;
    }

    @Override
    protected void init() {
        int w = 220;
        int x = this.width / 2 - w / 2;
        int y = this.height / 6;
        int step = 24;

        addDrawableChild(toggleButton(x, y, w, "Мод", () -> config.enabled, () -> {
            controller.setEnabled(this.client, !config.enabled);
        }));
        y += step;

        addDrawableChild(new IntSlider(x, y, w, 20, "Цільовий FPS", 20, 240, config.targetFps,
                v -> config.targetFps = v));
        y += step;

        addDrawableChild(new IntSlider(x, y, w, 20, "Мін. дальність (чанки)", 2, 16, config.minRenderDistance,
                v -> config.minRenderDistance = v));
        y += step;

        addDrawableChild(toggleButton(x, y, w, "Зменшувати частинки", () -> config.adjustParticles, () -> {
            config.adjustParticles = !config.adjustParticles;
        }));
        y += step;

        addDrawableChild(toggleButton(x, y, w, "Повідомлення над хотбаром", () -> config.showMessages, () -> {
            config.showMessages = !config.showMessages;
        }));
        y += step + 12;

        addDrawableChild(ButtonWidget.builder(Text.literal("Готово"), b -> close())
                .dimensions(x, y, w, 20).build());
    }

    private ButtonWidget toggleButton(int x, int y, int w, String label, BooleanSupplier state, Runnable onClick) {
        return ButtonWidget.builder(labelFor(label, state.getAsBoolean()), b -> {
            onClick.run();
            b.setMessage(labelFor(label, state.getAsBoolean()));
        }).dimensions(x, y, w, 20).build();
    }

    private Text labelFor(String label, boolean on) {
        return Text.literal(label + ": " + (on ? "ТАК" : "НІ"));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 15, 0xFFFFFF);
    }

    @Override
    public void removed() {
        config.save();
    }

    @Override
    public void close() {
        this.client.setScreen(parent);
    }

    /** Повзунок із цілими значеннями. */
    private static class IntSlider extends SliderWidget {
        private final String label;
        private final int min;
        private final int max;
        private final IntConsumer onChange;

        IntSlider(int x, int y, int w, int h, String label, int min, int max, int value, IntConsumer onChange) {
            super(x, y, w, h, Text.empty(), (double) (Math.min(max, Math.max(min, value)) - min) / (max - min));
            this.label = label;
            this.min = min;
            this.max = max;
            this.onChange = onChange;
            updateMessage();
        }

        private int current() {
            return min + (int) Math.round(this.value * (max - min));
        }

        @Override
        protected void updateMessage() {
            if (label == null) return;
            setMessage(Text.literal(label + ": " + current()));
        }

        @Override
        protected void applyValue() {
            onChange.accept(current());
        }
    }
}
