package com.adotishere.manualtodo;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ManualTodoListClient implements ClientModInitializer {
    public static final String MOD_ID = "manual_todo_list";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(id("controls"));
    private static KeyMapping settingsKey;
    private static KeyMapping interactKey;

    @Override
    public void onInitializeClient() {
        TodoManager.load();
        InventoryTodoOverlay.register();

        settingsKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.manual_todo_list.settings",
                InputConstants.Type.KEYSYM,
                InputConstants.KEY_O,
                CATEGORY
        ));
        interactKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.manual_todo_list.interact",
                InputConstants.Type.KEYSYM,
                InputConstants.KEY_P,
                CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (settingsKey.consumeClick()) {
                client.setScreen(new TodoSettingsScreen(client.screen));
            }
            while (interactKey.consumeClick()) {
                if (client.level != null) {
                    client.setScreen(new TodoInteractionScreen());
                }
            }
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommands.literal("todolist")
                    .executes(context -> {
                        Minecraft client = Minecraft.getInstance();
                        client.execute(() -> client.setScreen(new TodoInteractionScreen()));
                        return 1;
                    })
                    .then(ClientCommands.literal("settings")
                            .executes(context -> {
                                Minecraft client = Minecraft.getInstance();
                                client.execute(() -> client.setScreen(new TodoSettingsScreen(null)));
                                return 1;
                            }))
            );
        });

        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                id("todo_hud"),
                TodoHud::extractHud
        );
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
