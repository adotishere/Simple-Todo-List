package com.adotishere.manualtodo;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;

import java.util.Map;
import java.util.WeakHashMap;

public final class InventoryTodoOverlay {
    private static final Map<Screen, Controller> CONTROLLERS = new WeakHashMap<>();

    private InventoryTodoOverlay() {
    }

    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof AbstractContainerScreen<?>)) {
                return;
            }

            Controller previous = CONTROLLERS.remove(screen);
            if (previous != null) {
                previous.close();
            }
            Controller controller = new Controller(screen);
            CONTROLLERS.put(screen, controller);

            ScreenEvents.afterExtract(screen).register(controller::afterExtract);
            ScreenMouseEvents.allowMouseClick(screen).register(controller::allowMouseClick);
            ScreenEvents.remove(screen).register(removed -> controller.close());
        });
    }

    private static final class Controller {
        private final Screen screen;
        private boolean active = true;

        private Controller(Screen screen) {
            this.screen = screen;
        }

        private void afterExtract(Screen ignored, GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
            if (!active) {
                return;
            }
            TodoHud.renderAll(graphics, mouseX, mouseY, TodoHud.Mode.INVENTORY, null);
        }

        private boolean allowMouseClick(Screen ignored, MouseButtonEvent event) {
            if (!active || event.button() != 0 || !TodoManager.config().hudsVisible) {
                return true;
            }

            TodoHud.HitTarget target = TodoHud.hitTarget(event.x(), event.y(), screen.width, screen.height,
                    TodoHud.Mode.INVENTORY, null);

            switch (target.action()) {
                case NONE -> {
                    return true;
                }
                case TOGGLE_TASK -> {
                    TodoManager.toggleTask(target.listIndex(), target.taskIndex());
                    return false;
                }
                case INCREMENT -> {
                    TodoManager.changeProgress(target.listIndex(), target.taskIndex(), 1);
                    return false;
                }
                case DECREMENT -> {
                    TodoManager.changeProgress(target.listIndex(), target.taskIndex(), -1);
                    return false;
                }
                case COMPLETE_ALL_TASKS -> {
                    TodoManager.completeAllTasks(target.listIndex());
                    return false;
                }
                case RESET_TASKS -> {
                    TodoManager.resetTasks(target.listIndex());
                    return false;
                }
                default -> {
                    // Consume click on HUD so underlying inventory slots aren't accidentally triggered,
                    // but do not allow editing, deleting, hiding, adding, etc. in inventory mode.
                    return false;
                }
            }
        }

        private void close() {
            active = false;
        }
    }
}
