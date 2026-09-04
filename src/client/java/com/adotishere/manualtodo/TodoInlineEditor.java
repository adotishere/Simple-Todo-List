package com.adotishere.manualtodo;

import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class TodoInlineEditor {
    private final Screen host;
    private int listIndex = -1;
    private int taskIndex = -1;
    private boolean active;
    private boolean incremental;
    private EditBox taskField;
    private EditBox goalField;
    private String error = "";

    public TodoInlineEditor(Screen host) {
        this.host = host;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isEditing(int candidateListIndex, int candidateTaskIndex) {
        return active && listIndex == candidateListIndex && taskIndex == candidateTaskIndex && taskIndex >= 0;
    }

    public boolean isAdding(int candidateListIndex) {
        return active && listIndex == candidateListIndex && taskIndex < 0;
    }

    public boolean isIncremental() {
        return incremental;
    }

    public String error() {
        return error;
    }

    public void start(int newListIndex, int newTaskIndex) {
        cancel();
        TodoListData list = TodoManager.list(newListIndex);
        if (list == null) {
            return;
        }

        listIndex = newListIndex;
        taskIndex = newTaskIndex;
        active = true;
        TodoTask existing = TodoManager.task(listIndex, taskIndex);
        incremental = existing != null && existing.incremental;
        int defaultGoal = existing == null ? 2 : (existing.goal <= 1 ? 2 : existing.goal);
        createFields(existing == null ? "" : existing.text, defaultGoal);
    }

    public void toggleIncremental() {
        if (!active) {
            return;
        }
        String text = taskField == null ? "" : taskField.getValue();
        int goal = readGoalOrDefault();
        if (goal <= 1) {
            goal = 2;
        }
        incremental = !incremental;
        removeFields();
        createFields(text, goal);
    }

    public void incrementCap() {
        if (!active || !incremental) return;
        int current = readGoalOrDefault();
        int next = Math.clamp(current + 1, 1, 9999);
        if (goalField != null) {
            goalField.setValue(Integer.toString(next));
        }
    }

    public void decrementCap() {
        if (!active || !incremental) return;
        int current = readGoalOrDefault();
        if (current <= 2) {
            toggleIncremental();
            return;
        }
        int next = Math.clamp(current - 1, 1, 9999);
        if (goalField != null) {
            goalField.setValue(Integer.toString(next));
        }
    }

    public boolean save() {
        if (!active || taskField == null) {
            return true;
        }
        String text = TodoManager.cleanTaskText(taskField.getValue());
        if (text.isEmpty()) {
            cancel();
            return true;
        }

        int goal = 1;
        if (incremental) {
            try {
                goal = Integer.parseInt(goalField.getValue().trim());
                if (goal < 1 || goal > 9999) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException exception) {
                error = Component.translatable("screen.manual_todo_list.goal_error").getString();
                return false;
            }
        }

        if (taskIndex < 0) {
            TodoManager.addTask(listIndex, text, incremental, goal);
        } else {
            TodoManager.editTask(listIndex, taskIndex, text, incremental, goal);
        }
        cancel();
        return true;
    }

    public void cancel() {
        removeFields();
        active = false;
        listIndex = -1;
        taskIndex = -1;
        incremental = false;
        error = "";
    }

    public boolean handleKeyPress(KeyEvent event) {
        if (!active) {
            return false;
        }
        if (event.key() == 257 || event.key() == 335) {
            save();
            return true;
        }
        if (event.key() == 256) {
            cancel();
            return true;
        }
        return false;
    }

    public boolean isOverField(double mouseX, double mouseY) {
        return active && ((taskField != null && taskField.isMouseOver(mouseX, mouseY))
                || (goalField != null && goalField.isMouseOver(mouseX, mouseY)));
    }

    public void relayout() {
        if (!active || taskField == null) {
            return;
        }
        if (!widgets().contains(taskField)) {
            widgets().add(taskField);
        }
        if (goalField != null && !widgets().contains(goalField)) {
            widgets().add(goalField);
        }
        TodoListData list = TodoManager.list(listIndex);
        if (list == null) {
            cancel();
            return;
        }
        TodoHud.Layout layout = TodoHud.layout(listIndex, host.width, host.height);
        int localY = taskIndex < 0
                ? TodoHud.HEADER_HEIGHT + list.tasks.size() * TodoHud.ROW_HEIGHT
                : TodoHud.HEADER_HEIGHT + taskIndex * TodoHud.ROW_HEIGHT;
        int fieldHeight = Math.max(12, Math.round(13 * layout.scale()));
        int listWidth = list.customWidth > 0 ? list.customWidth : 270;
        int taskFieldWidth = incremental ? (listWidth - 102) : (listWidth - 59);

        taskField.setRectangle(
                Math.max(50, Math.round(taskFieldWidth * layout.scale())),
                fieldHeight,
                layout.x() + Math.round(19 * layout.scale()),
                layout.y() + Math.round((localY + 1) * layout.scale())
        );
        if (goalField != null) {
            goalField.setRectangle(
                    Math.max(24, Math.round(30 * layout.scale())),
                    fieldHeight,
                    layout.x() + Math.round((listWidth - 54) * layout.scale()),
                    layout.y() + Math.round((localY + 1) * layout.scale())
            );
        }
    }

    public void renderFieldsOnTop(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        if (!active) {
            return;
        }
        relayout();
        if (taskField != null) {
            taskField.extractRenderState(graphics, mouseX, mouseY, delta);
        }
        if (goalField != null) {
            goalField.extractRenderState(graphics, mouseX, mouseY, delta);
        }
    }

    private void createFields(String text, int goal) {
        taskField = new EditBox(Screens.getFont(host), 0, 0, 100, 14,
                Component.translatable("screen.manual_todo_list.task_name"));
        taskField.setMaxLength(120);
        taskField.setValue(text);
        taskField.setHint(Component.translatable("screen.manual_todo_list.task_hint"));
        widgets().add(taskField);

        if (incremental) {
            goalField = new EditBox(Screens.getFont(host), 0, 0, 30, 14,
                    Component.translatable("screen.manual_todo_list.goal"));
            goalField.setMaxLength(4);
            goalField.setValue(Integer.toString(Math.max(1, goal)));
            goalField.setHint(Component.literal("?"));
            widgets().add(goalField);
        }

        relayout();
        host.setFocused(taskField);
        taskField.moveCursorToEnd(false);
    }

    private int readGoalOrDefault() {
        if (goalField == null) {
            return 1;
        }
        try {
            return Math.clamp(Integer.parseInt(goalField.getValue().trim()), 1, 9999);
        } catch (NumberFormatException exception) {
            return 1;
        }
    }

    private void removeFields() {
        List<AbstractWidget> widgets = widgets();
        if (taskField != null) {
            widgets.remove(taskField);
            if (host.getFocused() == taskField) {
                host.setFocused(null);
            }
        }
        if (goalField != null) {
            widgets.remove(goalField);
            if (host.getFocused() == goalField) {
                host.setFocused(null);
            }
        }
        taskField = null;
        goalField = null;
    }

    private List<AbstractWidget> widgets() {
        return Screens.getWidgets(host);
    }
}
