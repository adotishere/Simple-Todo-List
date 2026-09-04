package com.adotishere.manualtodo;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public final class TodoInteractionScreen extends Screen implements TodoScreenMarker {
    private TodoInlineEditor inlineEditor;
    private int draggingList = -1;
    private double dragOffsetX;
    private double dragOffsetY;

    // Resizing state
    private TodoHud.ResizeEdge resizingEdge = TodoHud.ResizeEdge.NONE;
    private int resizingList = -1;
    private double resizeStartMouseX;
    private double resizeStartMouseY;
    private int startWidth;
    private int startExtraHeight;
    private float startScale;
    private int startLayoutX;
    private int startLayoutY;

    // Increment amount inline editor state
    private EditBox incrementBox;
    private int incrementListIndex = -1;
    private int incrementTaskIndex = -1;

    // Color picker popup state
    private int colorPickerList = -1;
    private int colorPickerX = 0;
    private int colorPickerY = 0;

    public TodoInteractionScreen() {
        super(Component.translatable("screen.manual_todo_list.interact"));
    }

    @Override
    protected void init() {
        if (inlineEditor == null) {
            inlineEditor = new TodoInlineEditor(this);
        }
        closeIncrementEditor();
        colorPickerList = -1;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, width, height, 0xD010141A);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        inlineEditor.relayout();
        TodoHud.renderAll(graphics, mouseX, mouseY, TodoHud.Mode.EDITOR, inlineEditor);
        TodoTheme theme = TodoManager.config().theme;
        graphics.centeredText(font, Component.translatable("screen.manual_todo_list.editor.hint"),
                width / 2, 10, TodoHud.opaque(theme.normalTask));
        graphics.centeredText(font, Component.translatable("screen.manual_todo_list.editor.create_hint"),
                width / 2, 23, TodoHud.opaque(theme.mutedText));
        if (TodoManager.config().lists.isEmpty()) {
            graphics.centeredText(font, Component.translatable("screen.manual_todo_list.editor.no_lists"),
                    width / 2, height / 2, TodoHud.opaque(theme.addButton));
        }
        if (!inlineEditor.error().isEmpty()) {
            graphics.centeredText(font, inlineEditor.error(), width / 2, 36, TodoHud.opaque(theme.deleteButton));
        }
        drawHudToggleButton(graphics, mouseX, mouseY);

        // Render increment box if active
        if (incrementBox != null) {
            incrementBox.extractRenderState(graphics, mouseX, mouseY, delta);
        }

        // Highlight active resize edge if resizing
        if (resizingList >= 0 && resizingEdge != TodoHud.ResizeEdge.NONE) {
            TodoHud.Layout layout = TodoHud.layout(resizingList, width, height);
            drawResizeIndicator(graphics, layout, resizingEdge, 0xFFFFFFFF);
        } else {
            // Highlight hovered resize edge
            TodoHud.ResizeTarget hoverTarget = TodoHud.detectResizeEdge(mouseX, mouseY, width, height);
            if (hoverTarget.isResizing()) {
                TodoHud.Layout layout = TodoHud.layout(hoverTarget.listIndex(), width, height);
                drawResizeIndicator(graphics, layout, hoverTarget.edge(), 0xAA55B9FF);
            }
        }

        // Render color picker popup over everything
        if (colorPickerList >= 0) {
            drawColorPickerPopup(graphics, mouseX, mouseY);
        }

        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    private void drawResizeIndicator(GuiGraphicsExtractor graphics, TodoHud.Layout layout, TodoHud.ResizeEdge edge, int color) {
        int x = layout.x();
        int y = layout.y();
        int w = layout.scaledWidth();
        int h = layout.scaledHeight();
        switch (edge) {
            case LEFT -> graphics.fill(x - 2, y, x + 1, y + h, color);
            case RIGHT -> graphics.fill(x + w - 1, y, x + w + 2, y + h, color);
            case TOP -> graphics.fill(x, y - 2, x + w, y + 1, color);
            case BOTTOM -> graphics.fill(x, y + h - 1, x + w, y + h + 2, color);
            case TOP_LEFT -> {
                graphics.fill(x - 2, y - 2, x + 6, y + 1, color);
                graphics.fill(x - 2, y - 2, x + 1, y + 6, color);
            }
            case TOP_RIGHT -> {
                graphics.fill(x + w - 6, y - 2, x + w + 2, y + 1, color);
                graphics.fill(x + w - 1, y - 2, x + w + 2, y + 6, color);
            }
            case BOTTOM_LEFT -> {
                graphics.fill(x - 2, y + h - 1, x + 6, y + h + 2, color);
                graphics.fill(x - 2, y + h - 6, x + 1, y + h + 2, color);
            }
            case BOTTOM_RIGHT -> {
                graphics.fill(x + w - 6, y + h - 1, x + w + 2, y + h + 2, color);
                graphics.fill(x + w - 1, y + h - 6, x + w + 2, y + h + 2, color);
            }
            default -> {}
        }
    }

    private void drawColorPickerPopup(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int px = colorPickerX;
        int py = colorPickerY;
        int pw = 95;
        int ph = 46;
        if (px + pw > width) px = width - pw - 4;
        if (py + ph > height) py = height - ph - 4;

        // Background & outline
        graphics.fill(px, py, px + pw, py + ph, 0xF012151D);
        graphics.outline(px, py, pw, ph, 0xFF55B9FF);

        TodoListData list = TodoManager.list(colorPickerList);
        Integer currentAccent = list != null ? list.accentColor : null;
        int baseAccent = TodoManager.config().theme.accent;

        // 10 swatches in 2 rows of 5
        for (int i = 0; i < 10; i++) {
            int col = i % 5;
            int row = i / 5;
            int sx = px + 6 + col * 17;
            int sy = py + 6 + row * 18;

            int color = (i == 0) ? baseAccent : TodoManager.ACCENT_PALETTE[i - 1];
            boolean isSelected = (i == 0 && currentAccent == null) || (currentAccent != null && (currentAccent & 0xFFFFFF) == (color & 0xFFFFFF));
            boolean isHovered = mouseX >= sx && mouseX < sx + 14 && mouseY >= sy && mouseY < sy + 14;

            graphics.fill(sx, sy, sx + 14, sy + 14, TodoHud.opaque(color));
            if (i == 0) {
                // Draw a 'B' or indicator for base
                graphics.centeredText(font, "B", sx + 7, sy + 3, isDark(color) ? 0xFFFFFFFF : 0xFF000000);
            }
            if (isSelected) {
                graphics.outline(sx - 1, sy - 1, 16, 16, 0xFFFFFFFF);
            } else if (isHovered) {
                graphics.outline(sx - 1, sy - 1, 16, 16, 0xAAFFFFFF);
            } else {
                graphics.outline(sx, sy, 14, 14, 0x55000000);
            }
        }
    }

    private static boolean isDark(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return (r * 299 + g * 587 + b * 114) / 1000 < 128;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        // Handle color picker popup clicks
        if (colorPickerList >= 0) {
            int px = colorPickerX;
            int py = colorPickerY;
            int pw = 95;
            int ph = 46;
            if (px + pw > width) px = width - pw - 4;
            if (py + ph > height) py = height - ph - 4;

            if (event.x() >= px && event.x() < px + pw && event.y() >= py && event.y() < py + ph) {
                for (int i = 0; i < 10; i++) {
                    int col = i % 5;
                    int row = i / 5;
                    int sx = px + 6 + col * 17;
                    int sy = py + 6 + row * 18;
                    if (event.x() >= sx && event.x() < sx + 14 && event.y() >= sy && event.y() < sy + 14) {
                        if (i == 0) {
                            TodoManager.setListAccentColor(colorPickerList, null); // reset to base
                        } else {
                            TodoManager.setListAccentColor(colorPickerList, TodoManager.ACCENT_PALETTE[i - 1]);
                        }
                        colorPickerList = -1;
                        return true;
                    }
                }
                return true;
            } else {
                colorPickerList = -1;
            }
        }

        // Right-click creates a new todo list hud object so you don't accidentally make them
        if (event.button() == 1) {
            TodoHud.HitTarget hit = TodoHud.hitTarget(event.x(), event.y(), width, height, TodoHud.Mode.EDITOR, inlineEditor);
            if (hit.action() == TodoHud.Action.NONE && event.y() >= 43 && event.y() < height - 12) {
                TodoManager.addListAt(event.x(), event.y(), width, height);
                return true;
            }
            return super.mouseClicked(event, doubleClick);
        }

        if (event.button() != 0) {
            return super.mouseClicked(event, doubleClick);
        }

        // Handle increment box click
        if (incrementBox != null) {
            if (incrementBox.isMouseOver(event.x(), event.y())) {
                return incrementBox.mouseClicked(event, doubleClick);
            } else {
                saveIncrementEditor();
            }
        }

        if (inlineEditor.isOverField(event.x(), event.y())) {
            return super.mouseClicked(event, doubleClick);
        }
        if (insideHudToggle(event.x(), event.y())) {
            TodoManager.config().hudsVisible = !TodoManager.config().hudsVisible;
            TodoManager.save();
            return true;
        }

        // Check if clicking outside bounds on edge to resize
        TodoHud.ResizeTarget resizeTarget = TodoHud.detectResizeEdge(event.x(), event.y(), width, height);
        if (resizeTarget.isResizing()) {
            beginResizing(resizeTarget.listIndex(), resizeTarget.edge(), event.x(), event.y());
            return true;
        }

        TodoHud.HitTarget target = TodoHud.hitTarget(event.x(), event.y(), width, height,
                TodoHud.Mode.EDITOR, inlineEditor);

        if (inlineEditor.isActive()) {
            if (target.action() == TodoHud.Action.TOGGLE_INCREMENTAL) {
                inlineEditor.toggleIncremental();
                return true;
            }
            if (target.action() == TodoHud.Action.INCREMENT_CAP) {
                inlineEditor.incrementCap();
                return true;
            }
            if (target.action() == TodoHud.Action.DECREMENT_CAP) {
                inlineEditor.decrementCap();
                return true;
            }
            if (target.action() == TodoHud.Action.SAVE_INLINE) {
                inlineEditor.save();
                return true;
            }
            inlineEditor.save();
            return true;
        }

        switch (target.action()) {
            case TOGGLE_TASK -> TodoManager.toggleTask(target.listIndex(), target.taskIndex());
            case EDIT_TASK -> inlineEditor.start(target.listIndex(), target.taskIndex());
            case DELETE_TASK -> TodoManager.removeTask(target.listIndex(), target.taskIndex());
            case DECREMENT -> TodoManager.changeProgress(target.listIndex(), target.taskIndex(), -1);
            case INCREMENT -> TodoManager.changeProgress(target.listIndex(), target.taskIndex(), 1);
            case EDIT_INCREMENT -> startEditingIncrement(target.listIndex(), target.taskIndex());
            case ADD_TASK -> inlineEditor.start(target.listIndex(), -1);
            case COMPLETE_ALL_TASKS -> TodoManager.completeAllTasks(target.listIndex());
            case RESET_TASKS -> TodoManager.resetTasks(target.listIndex());
            case DELETE_LIST -> TodoManager.removeList(target.listIndex());
            case CHANGE_ACCENT_COLOR -> {
                colorPickerList = target.listIndex();
                colorPickerX = (int) Math.round(event.x());
                colorPickerY = (int) Math.round(event.y() + 8);
            }
            case TOGGLE_LIST_VISIBILITY -> TodoManager.toggleListHidden(target.listIndex());
            case DRAG_LIST -> beginDragging(target.listIndex(), event.x(), event.y());
            case BLOCK, NONE -> {
                return true;
            }
            default -> {
                return true;
            }
        }
        return true;
    }

    private void beginResizing(int listIndex, TodoHud.ResizeEdge edge, double mouseX, double mouseY) {
        TodoListData list = TodoManager.list(listIndex);
        if (list == null) return;
        TodoHud.Layout layout = TodoHud.layout(listIndex, width, height);
        resizingList = listIndex;
        resizingEdge = edge;
        resizeStartMouseX = mouseX;
        resizeStartMouseY = mouseY;
        startWidth = list.customWidth > 0 ? list.customWidth : TodoHud.BASE_WIDTH;
        startExtraHeight = list.extraHeight;
        startScale = list.scale;
        startLayoutX = layout.x();
        startLayoutY = layout.y();
    }

    private void startEditingIncrement(int listIndex, int taskIndex) {
        closeIncrementEditor();
        if (inlineEditor != null && inlineEditor.isActive()) {
            inlineEditor.cancel();
        }
        TodoTask task = TodoManager.task(listIndex, taskIndex);
        if (task == null) return;
        incrementListIndex = listIndex;
        incrementTaskIndex = taskIndex;

        TodoHud.Layout layout = TodoHud.layout(listIndex, width, height);
        int listWidth = layout.baseWidth();
        int rowY = TodoHud.HEADER_HEIGHT + taskIndex * TodoHud.ROW_HEIGHT;
        int boxX = layout.x() + Math.round((listWidth - 120) * layout.scale());
        int boxY = layout.y() + Math.round((rowY + 1) * layout.scale());
        int boxW = Math.max(44, Math.round(42 * layout.scale()));
        int boxH = Math.max(13, Math.round(13 * layout.scale()));

        incrementBox = new EditBox(font, boxX, boxY, boxW, boxH, Component.literal("Increment"));
        incrementBox.setMaxLength(10);
        incrementBox.setValue(""); // starts with an empty text box
        incrementBox.setHint(Component.literal("amount"));
        setFocused(incrementBox);
    }

    private void closeIncrementEditor() {
        if (incrementBox != null && getFocused() == incrementBox) {
            setFocused(null);
        }
        incrementBox = null;
        incrementListIndex = -1;
        incrementTaskIndex = -1;
    }

    private void saveIncrementEditor() {
        if (incrementBox == null) return;
        String val = incrementBox.getValue().trim();
        if (!val.isEmpty()) {
            TodoTask task = TodoManager.task(incrementListIndex, incrementTaskIndex);
            if (task != null) {
                if (val.contains("/")) {
                    String[] parts = val.split("/");
                    try {
                        int p = Integer.parseInt(parts[0].trim());
                        int g = Integer.parseInt(parts[1].trim());
                        TodoManager.setTaskIncrement(incrementListIndex, incrementTaskIndex, p, g);
                    } catch (Exception ignored) {}
                } else {
                    try {
                        int amount = Integer.parseInt(val);
                        if (amount > 0) {
                            if (task.progress == 0) {
                                TodoManager.setTaskIncrement(incrementListIndex, incrementTaskIndex, 0, amount);
                            } else if (amount > task.goal) {
                                TodoManager.setTaskIncrement(incrementListIndex, incrementTaskIndex, task.progress, amount);
                            } else {
                                TodoManager.setTaskIncrement(incrementListIndex, incrementTaskIndex, amount, task.goal);
                            }
                        }
                    } catch (Exception ignored) {}
                }
            }
        }
        closeIncrementEditor();
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (resizingList >= 0 && event.button() == 0) {
            TodoListData list = TodoManager.list(resizingList);
            if (list != null) {
                double dX = event.x() - resizeStartMouseX;
                double dY = event.y() - resizeStartMouseY;

                switch (resizingEdge) {
                    case RIGHT -> {
                        int newWidth = Math.clamp((int) Math.round(startWidth + dX / startScale), 180, 800);
                        list.customWidth = newWidth;
                    }
                    case BOTTOM -> {
                        int newExtraHeight = Math.max(0, (int) Math.round(startExtraHeight + dY / startScale));
                        list.extraHeight = newExtraHeight;
                    }
                    case LEFT -> {
                        int newWidth = Math.clamp((int) Math.round(startWidth - dX / startScale), 180, 800);
                        int widthDiff = newWidth - startWidth;
                        list.customWidth = newWidth;
                        int newX = startLayoutX - Math.round(widthDiff * startScale);
                        TodoHud.moveToPixels(resizingList, newX, startLayoutY, width, height);
                    }
                    case TOP -> {
                        int newExtraHeight = Math.max(0, (int) Math.round(startExtraHeight - dY / startScale));
                        int heightDiff = newExtraHeight - startExtraHeight;
                        list.extraHeight = newExtraHeight;
                        int newY = startLayoutY - Math.round(heightDiff * startScale);
                        TodoHud.moveToPixels(resizingList, startLayoutX, newY, width, height);
                    }
                    case BOTTOM_RIGHT -> {
                        int newWidth = Math.clamp((int) Math.round(startWidth + dX / startScale), 180, 800);
                        int newExtraHeight = Math.max(0, (int) Math.round(startExtraHeight + dY / startScale));
                        list.customWidth = newWidth;
                        list.extraHeight = newExtraHeight;
                    }
                    case TOP_LEFT -> {
                        int newWidth = Math.clamp((int) Math.round(startWidth - dX / startScale), 180, 800);
                        int newExtraHeight = Math.max(0, (int) Math.round(startExtraHeight - dY / startScale));
                        list.customWidth = newWidth;
                        list.extraHeight = newExtraHeight;
                        int newX = startLayoutX - Math.round((newWidth - startWidth) * startScale);
                        int newY = startLayoutY - Math.round((newExtraHeight - startExtraHeight) * startScale);
                        TodoHud.moveToPixels(resizingList, newX, newY, width, height);
                    }
                    case TOP_RIGHT -> {
                        int newWidth = Math.clamp((int) Math.round(startWidth + dX / startScale), 180, 800);
                        int newExtraHeight = Math.max(0, (int) Math.round(startExtraHeight - dY / startScale));
                        list.customWidth = newWidth;
                        list.extraHeight = newExtraHeight;
                        int newY = startLayoutY - Math.round((newExtraHeight - startExtraHeight) * startScale);
                        TodoHud.moveToPixels(resizingList, startLayoutX, newY, width, height);
                    }
                    case BOTTOM_LEFT -> {
                        int newWidth = Math.clamp((int) Math.round(startWidth - dX / startScale), 180, 800);
                        int newExtraHeight = Math.max(0, (int) Math.round(startExtraHeight + dY / startScale));
                        list.customWidth = newWidth;
                        list.extraHeight = newExtraHeight;
                        int newX = startLayoutX - Math.round((newWidth - startWidth) * startScale);
                        TodoHud.moveToPixels(resizingList, newX, startLayoutY, width, height);
                    }
                    default -> {}
                }
            }
            return true;
        }

        if (draggingList >= 0 && event.button() == 0) {
            TodoHud.moveToPixels(draggingList, event.x() - dragOffsetX, event.y() - dragOffsetY, width, height);
            return true;
        }
        return super.mouseDragged(event, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (resizingList >= 0 && event.button() == 0) {
            resizingList = -1;
            resizingEdge = TodoHud.ResizeEdge.NONE;
            TodoManager.save();
            return true;
        }
        if (draggingList >= 0 && event.button() == 0) {
            draggingList = -1;
            TodoManager.save();
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (inlineEditor.isActive() || incrementBox != null || colorPickerList >= 0 || scrollY == 0.0) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        int listIndex = TodoHud.listAt(mouseX, mouseY, width, height);
        TodoListData list = TodoManager.list(listIndex);
        if (list == null) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        float newFontScale = Math.clamp(list.fontScale + (scrollY > 0 ? 0.05F : -0.05F), 0.50F, 2.0F);
        list.fontScale = Math.round(newFontScale * 100.0F) / 100.0F;
        TodoManager.save();
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == 256 && colorPickerList >= 0) {
            colorPickerList = -1;
            return true;
        }
        if (incrementBox != null) {
            if (event.key() == 256) {
                closeIncrementEditor();
                return true;
            }
            if (event.key() == 257 || event.key() == 335) {
                saveIncrementEditor();
                return true;
            }
            if (incrementBox.keyPressed(event)) {
                return true;
            }
        }
        if (inlineEditor.handleKeyPress(event)) {
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        closeIncrementEditor();
        colorPickerList = -1;
        inlineEditor.cancel();
        TodoManager.save();
        minecraft.setScreen(null);
    }

    private void beginDragging(int listIndex, double mouseX, double mouseY) {
        TodoHud.Layout layout = TodoHud.layout(listIndex, width, height);
        draggingList = listIndex;
        dragOffsetX = mouseX - layout.x();
        dragOffsetY = mouseY - layout.y();
    }

    private void drawHudToggleButton(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TodoTheme theme = TodoManager.config().theme;
        int btnW = 76;
        int btnH = 18;
        int btnX = width - btnW - 8;
        int btnY = 8;
        boolean hovered = insideHudToggle(mouseX, mouseY);
        boolean visible = TodoManager.config().hudsVisible;

        graphics.fill(btnX, btnY, btnX + btnW, btnY + btnH,
                TodoHud.argb(hovered ? theme.accent : theme.background, hovered ? 180 : 220));
        graphics.outline(btnX, btnY, btnW, btnH, TodoHud.opaque(visible ? theme.completedTask : theme.deleteButton));
        int textColor = visible ? theme.completedTask : theme.mutedText;
        graphics.centeredText(font, "Hud toggle", btnX + btnW / 2, btnY + 5, TodoHud.opaque(textColor));
    }

    private boolean insideHudToggle(double mouseX, double mouseY) {
        int btnW = 76;
        int btnH = 18;
        int btnX = width - btnW - 8;
        int btnY = 8;
        return mouseX >= btnX && mouseX < btnX + btnW && mouseY >= btnY && mouseY < btnY + btnH;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean isInGameUi() {
        return true;
    }
}