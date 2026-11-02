package com.example.adaptiveperf;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdaptivePerfClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("adaptiveperf");

    private static KeyBinding toggleKey;

    @Override
    public void onInitializeClient() {
        Config config = Config.load();
        AdaptiveController controller = new AdaptiveController(config);

        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.adaptiveperf.menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                "category.adaptiveperf"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleKey.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new SettingsScreen(null, config, controller));
                }
            }
            controller.tick(client);
        });

        // Повертаємо налаштування гравця, щоб вони не збереглися в options.txt зниженими
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> controller.restore(client));
        ClientLifecycleEvents.CLIENT_STOPPING.register(controller::restore);

        LOGGER.info("Adaptive Performance запущено (ціль {} FPS)", config.targetFps);
    }
}
