package com.adotishere.manualtodo;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

public final class TodoHud {
    public static final int BASE_WIDTH = 270;
    public static final int HEADER_HEIGHT = 21;
    public static final int ROW_HEIGHT = 15;
    public static final int FOOTER_HEIGHT = 19;

    private TodoHud() {
    }

    public static void extractHud(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gui.screen() instanceof TodoScreenMarker
                || minecraft.gui.screen() instanceof AbstractContainerScreen<?>
                || minecraft.gui.hud.isHidden()) {
            return;
        }
        renderAll(graphics, -1, -1, Mode.HUD, null);
    }

    public static int minimumBaseHeight() {
        return HEADER_HEIGHT + ROW_HEIGHT + FOOTER_HEIGHT;
    }

    public static Layout layout(int listIndex, int screenWidth, int screenHeight) {
        TodoListData list = TodoManager.list(listIndex);
        if (list == null) {
            return new Layout(0, 0, 0, 0, minimumBaseHeight(), 1.0F, BASE_WIDTH);
        }
        int width = list.customWidth > 0 ? list.customWidth : BASE_WIDTH;
        int rowCount = Math.max(1, list.tasks.size());
        int baseHeight = HEADER_HEIGHT + rowCount * ROW_HEIGHT + FOOTER_HEIGHT + Math.max(0, list.extraHeight);
        int scaledWidth = Math.round(width * list.scale);
        int scaledHeight = Math.round(baseHeight * list.scale);
        // x is independent of scaledWidth so left edge never slides when resizing right edge!
        int x = Math.round(list.horizontalPosition * screenWidth);
        int y = Math.round(list.verticalPosition * screenHeight);
        return new Layout(x, y, scaledWidth, scaledHeight, baseHeight, list.scale, width);
    }

    public static void renderAll(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            Mode mode,
            TodoInlineEditor editor
    ) {
        renderAll(graphics, mouseX, mouseY, mode, editor, -1, -1);
    }

    public static void renderAll(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            Mode mode,
            TodoInlineEditor editor,
            int draggedListIndex,
            int draggedTaskIndex
    ) {
        TodoConfig config = TodoManager.config();
        if (!config.hudsVisible && mode != Mode.EDITOR) {
            return;
        }
        for (int listIndex = 0; listIndex < config.lists.size(); listIndex++) {
            TodoListData list = TodoManager.list(listIndex);
            if (list != null && list.hidden && mode != Mode.EDITOR) {
                continue;
            }
            renderList(graphics, listIndex, mouseX, mouseY, mode, editor, draggedListIndex, draggedTaskIndex);
        }
    }

    private static void renderList(
            GuiGraphicsExtractor graphics,
            int listIndex,
            int mouseX,
            int mouseY,
            Mode mode,
            TodoInlineEditor editor,
            int draggedListIndex,
            int draggedTaskIndex
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        TodoConfig config = TodoManager.config();
        TodoTheme theme = config.theme;
        TodoListData list = TodoManager.list(listIndex);
        if (list == null) {
            return;
        }

        int accent = list.accentColor != null ? list.accentColor : theme.accent;
        Layout layout = layout(listIndex, graphics.guiWidth(), graphics.guiHeight());
        int listWidth = layout.baseWidth();
        float localMouseX = (mouseX - layout.x) / layout.scale;
        float localMouseY = (mouseY - layout.y) / layout.scale;

        graphics.pose().pushMatrix();
        graphics.pose().translate(layout.x, layout.y);
        graphics.pose().scale(layout.scale, layout.scale);

        int alpha = Math.round(config.backgroundOpacityPercent * 2.55F);
        if (list.hidden && mode == Mode.EDITOR) {
            alpha = Math.round(alpha * 0.55F);
        }
        int headerAlpha = Math.min(255, Math.round(alpha * 1.08F));
        graphics.fill(0, HEADER_HEIGHT, listWidth, layout.baseHeight, argb(theme.background, alpha));
        graphics.fill(0, 0, listWidth, HEADER_HEIGHT, argb(theme.background, headerAlpha));
        int dividerAlpha = Math.round(22 * (alpha / 255.0F));
        graphics.fill(8, HEADER_HEIGHT - 1, listWidth - 8, HEADER_HEIGHT, argb(0xFFFFFF, dividerAlpha));

        int titleX = 8;
        if (mode == Mode.EDITOR) {
            graphics.fill(6, 6, 15, 15, opaque(accent));
            graphics.outline(5, 5, 11, 11, opaque(theme.background));
            if (inside(localMouseX, localMouseY, 4, 4, 17, 17)) {
                graphics.outline(4, 4, 13, 13, 0xFFFFFFFF);
            }
            titleX = 19;
        }

        drawScaledText(graphics, font, list.title, titleX, 7, opaque(accent), true, list.fontScale);
        int completed = (int) list.tasks.stream().filter(task -> task.completed).count();
        String count = completed + "/" + list.tasks.size();
        int countRight = (mode == Mode.EDITOR) ? listWidth - 36 : listWidth - 7;
        drawScaledText(graphics, font, count, countRight - Math.round(font.width(count) * list.fontScale), 7, opaque(theme.incrementalText), true, list.fontScale);

        // Header buttons: only in EDITOR mode (not in inventory or HUD)
        if (mode == Mode.EDITOR) {
            boolean eyeHovered = inside(localMouseX, localMouseY, listWidth - 33, 3, listWidth - 19, 18);
            if (eyeHovered) {
                graphics.fill(listWidth - 33, 3, listWidth - 19, 18, argb(accent, 72));
            }
            drawEyeIcon(graphics, listWidth - 31, 7, list.hidden ? theme.mutedText : accent, list.hidden);

            boolean closeHovered = inside(localMouseX, localMouseY, listWidth - 17, 3, listWidth - 3, 18);
            if (closeHovered) {
                graphics.fill(listWidth - 18, 3, listWidth - 3, 18, argb(theme.deleteButton, 72));
            }
            graphics.text(font, "x", listWidth - 13, 6, opaque(theme.deleteButton), true);
        }

        if (list.tasks.isEmpty()) {
            if (editor == null || !editor.isAdding(listIndex)) {
                drawScaledText(graphics, font, "No tasks yet", 19, HEADER_HEIGHT + 4, opaque(theme.mutedText), false, list.fontScale);
                drawSquare(graphics, 8, HEADER_HEIGHT + 5, theme.mutedText, false);
            }
        } else {
            for (int taskIndex = 0; taskIndex < list.tasks.size(); taskIndex++) {
                TodoTask task = list.tasks.get(taskIndex);
                int rowY = HEADER_HEIGHT + taskIndex * ROW_HEIGHT;
                boolean editing = editor != null && editor.isEditing(listIndex, taskIndex);
                boolean isBeingDragged = (listIndex == draggedListIndex && taskIndex == draggedTaskIndex);
                boolean rowHovered = (mode == Mode.EDITOR) && inside(localMouseX, localMouseY, 5, rowY + 1, listWidth - 5, rowY + ROW_HEIGHT);
                if (isBeingDragged) {
                    graphics.fill(5, rowY + 1, listWidth - 5, rowY + ROW_HEIGHT, argb(accent, 85));
                    graphics.outline(5, rowY + 1, listWidth - 10, ROW_HEIGHT - 1, opaque(accent));
                } else if (rowHovered || editing) {
                    graphics.fill(5, rowY + 1, listWidth - 5, rowY + ROW_HEIGHT, argb(0xFFFFFF, editing ? 35 : 18));
                }

                if (editing) {
                    drawInlineControls(graphics, font, rowY, editor.isIncremental(), theme, accent, listWidth, config.coloredIncrementButtons);
                    continue;
                }

                int taskColor = task.completed ? theme.completedTask : theme.normalTask;
                drawSquare(graphics, 8, rowY + 5, taskColor, task.completed);

                int textStartX = 19;
                if (mode == Mode.EDITOR) {
                    boolean gripHovered = inside(localMouseX, localMouseY, 16, rowY + 1, 26, rowY + ROW_HEIGHT);
                    if (gripHovered || isBeingDragged) {
                        graphics.fill(16, rowY + 2, 26, rowY + ROW_HEIGHT - 1, argb(0xFFFFFF, 35));
                    }
                    drawGripIcon(graphics, 18, rowY + 5, (isBeingDragged || gripHovered) ? 0xFFFFFF : theme.mutedText);
                    textStartX = 28;
                }

                if (task.incremental) {
                    String progress = task.progress + "/" + task.goal;
                    int progressWidth = Math.round(font.width(progress) * list.fontScale);
                    int plusColor = config.coloredIncrementButtons ? opaque(theme.addButton) : 0xFFD4D4D8;
                    int minusColor = config.coloredIncrementButtons ? opaque(theme.editButton) : 0xFFA1A1AA;

                    if (mode == Mode.EDITOR) {
                        int maxTextWidth = Math.max(30, listWidth - 151);
                        drawScaledText(graphics, font, abbreviate(font, task.text, Math.round(maxTextWidth / list.fontScale)), textStartX, rowY + 4, opaque(taskColor), true, list.fontScale);

                        int progressX = (listWidth - 78) - progressWidth;
                        boolean progressHovered = inside(localMouseX, localMouseY, progressX - 2, rowY + 1, listWidth - 76, rowY + ROW_HEIGHT);
                        if (progressHovered) {
                            graphics.fill(progressX - 2, rowY + 2, listWidth - 76, rowY + ROW_HEIGHT - 1, argb(0xFFFFFF, 25));
                        }
                        drawScaledText(graphics, font, progress, progressX, rowY + 4, opaque(theme.incrementalText), true, list.fontScale);

                        boolean minusHovered = inside(localMouseX, localMouseY, listWidth - 76, rowY + 1, listWidth - 64, rowY + ROW_HEIGHT);
                        boolean plusHovered = inside(localMouseX, localMouseY, listWidth - 64, rowY + 1, listWidth - 50, rowY + ROW_HEIGHT);
                        if (minusHovered) {
                            graphics.fill(listWidth - 75, rowY + 2, listWidth - 65, rowY + ROW_HEIGHT - 1, argb(minusColor, 35));
                        }
                        if (plusHovered) {
                            graphics.fill(listWidth - 63, rowY + 2, listWidth - 51, rowY + ROW_HEIGHT - 1, argb(plusColor, 35));
                        }
                        graphics.text(font, "-", listWidth - 74, rowY + 4, minusColor, true);
                        graphics.text(font, "+", listWidth - 62, rowY + 4, plusColor, true);

                        boolean pencilHovered = inside(localMouseX, localMouseY, listWidth - 50, rowY + 1, listWidth - 28, rowY + ROW_HEIGHT);
                        boolean delHovered = inside(localMouseX, localMouseY, listWidth - 28, rowY + 1, listWidth, rowY + ROW_HEIGHT);
                        if (pencilHovered) {
                            graphics.fill(listWidth - 48, rowY + 2, listWidth - 32, rowY + ROW_HEIGHT - 1, argb(theme.editButton, 35));
                        }
                        if (delHovered) {
                            graphics.fill(listWidth - 27, rowY + 2, listWidth - 2, rowY + ROW_HEIGHT - 1, argb(theme.deleteButton, 45));
                        }
                        drawPencil(graphics, listWidth - 45, rowY + 3, theme.editButton);
                        graphics.text(font, "del-", listWidth - 26, rowY + 4, opaque(theme.deleteButton), true);
                    } else {
                        // Outside edit view (HUD and INVENTORY): controls positioned at the far right!
                        int progressX = (listWidth - 32) - progressWidth;
                        int maxTextWidth = Math.max(30, progressX - 24);
                        drawScaledText(graphics, font, abbreviate(font, task.text, Math.round(maxTextWidth / list.fontScale)), 19, rowY + 4, opaque(taskColor), true, list.fontScale);

                        drawScaledText(graphics, font, progress, progressX, rowY + 4, opaque(theme.incrementalText), true, list.fontScale);

                        if (mode == Mode.INVENTORY) {
                            boolean minusHovered = inside(localMouseX, localMouseY, listWidth - 30, rowY + 1, listWidth - 18, rowY + ROW_HEIGHT);
                            boolean plusHovered = inside(localMouseX, localMouseY, listWidth - 18, rowY + 1, listWidth - 4, rowY + ROW_HEIGHT);
                            if (minusHovered) {
                                graphics.fill(listWidth - 29, rowY + 2, listWidth - 19, rowY + ROW_HEIGHT - 1, argb(minusColor, 35));
                            }
                            if (plusHovered) {
                                graphics.fill(listWidth - 17, rowY + 2, listWidth - 5, rowY + ROW_HEIGHT - 1, argb(plusColor, 35));
                            }
                        }
                        graphics.text(font, "-", listWidth - 26, rowY + 4, minusColor, true);
                        graphics.text(font, "+", listWidth - 14, rowY + 4, plusColor, true);
                    }
                } else {
                    int maxTextWidth = mode == Mode.EDITOR ? Math.max(30, listWidth - 63) : Math.max(30, listWidth - 24);
                    drawScaledText(graphics, font, abbreviate(font, task.text, Math.round(maxTextWidth / list.fontScale)), textStartX, rowY + 4, opaque(taskColor), true, list.fontScale);

                    if (mode == Mode.EDITOR) {
                        boolean pencilHovered = inside(localMouseX, localMouseY, listWidth - 50, rowY + 1, listWidth - 28, rowY + ROW_HEIGHT);
                        boolean delHovered = inside(localMouseX, localMouseY, listWidth - 28, rowY + 1, listWidth, rowY + ROW_HEIGHT);
                        if (pencilHovered) {
                            graphics.fill(listWidth - 48, rowY + 2, listWidth - 32, rowY + ROW_HEIGHT - 1, argb(theme.editButton, 35));
                        }
                        if (delHovered) {
                            graphics.fill(listWidth - 27, rowY + 2, listWidth - 2, rowY + ROW_HEIGHT - 1, argb(theme.deleteButton, 45));
                        }
                        drawPencil(graphics, listWidth - 45, rowY + 3, theme.editButton);
                        graphics.text(font, "del-", listWidth - 26, rowY + 4, opaque(theme.deleteButton), true);
                    }
                }
            }
        }

        // When adding a task, draw the input row and its controls on the new task row!
        if (editor != null && editor.isAdding(listIndex)) {
            int addRowY = HEADER_HEIGHT + list.tasks.size() * ROW_HEIGHT;
            graphics.fill(5, addRowY + 1, listWidth - 5, addRowY + ROW_HEIGHT, argb(accent, 78));
            drawInlineControls(graphics, font, addRowY, editor.isIncremental(), theme, accent, listWidth, config.coloredIncrementButtons);
        }

        // Footer is positioned at bottom
        int footerY = layout.baseHeight - FOOTER_HEIGHT;
        graphics.fill(8, footerY, listWidth - 8, footerY + 1, argb(0xFFFFFF, dividerAlpha));

        // Footer buttons in INVENTORY mode (done and reset)
        if (mode == Mode.INVENTORY) {
            boolean doneHovered = inside(localMouseX, localMouseY, 5, footerY + 1, 44, footerY + FOOTER_HEIGHT);
            boolean resetHovered = inside(localMouseX, localMouseY, 46, footerY + 1, 88, footerY + FOOTER_HEIGHT);
            if (doneHovered) {
                graphics.fill(5, footerY + 2, 44, footerY + FOOTER_HEIGHT - 2, argb(theme.completedTask, 45));
            }
            if (resetHovered) {
                graphics.fill(46, footerY + 2, 88, footerY + FOOTER_HEIGHT - 2, argb(accent, 45));
            }
            graphics.text(font, "done", 8, footerY + 5, opaque(theme.completedTask), true);
            graphics.text(font, "reset", 48, footerY + 5, opaque(accent), true);
        }

        // Footer buttons in EDITOR mode (add+, done, reset)
        if (mode == Mode.EDITOR && (editor == null || !editor.isAdding(listIndex))) {
            boolean addHovered = inside(localMouseX, localMouseY, 5, footerY + 1, 44, footerY + FOOTER_HEIGHT);
            boolean doneHovered = inside(localMouseX, localMouseY, 46, footerY + 1, 84, footerY + FOOTER_HEIGHT);
            boolean resetHovered = inside(localMouseX, localMouseY, 86, footerY + 1, 128, footerY + FOOTER_HEIGHT);
            if (addHovered) {
                graphics.fill(5, footerY + 2, 44, footerY + FOOTER_HEIGHT - 2, argb(theme.addButton, 45));
            }
            if (doneHovered) {
                graphics.fill(46, footerY + 2, 84, footerY + FOOTER_HEIGHT - 2, argb(theme.completedTask, 45));
            }
            if (resetHovered) {
                graphics.fill(86, footerY + 2, 128, footerY + FOOTER_HEIGHT - 2, argb(accent, 45));
            }
            graphics.text(font, "add+", 8, footerY + 5, opaque(theme.addButton), true);
            graphics.text(font, "done", 48, footerY + 5, opaque(theme.completedTask), true);
            graphics.text(font, "reset", 88, footerY + 5, opaque(accent), true);
        }

        int outlineColor = (list.hidden && mode == Mode.EDITOR) ? argb(accent, 140) : opaque(accent);
        graphics.outline(0, 0, listWidth, layout.baseHeight, outlineColor);
        if (mode == Mode.EDITOR) {
            graphics.fill(listWidth - 6, layout.baseHeight - 6, listWidth, layout.baseHeight, outlineColor);
        }

        graphics.pose().popMatrix();
    }

    private static void drawInlineControls(GuiGraphicsExtractor graphics, Font font, int rowY, boolean incremental, TodoTheme theme, int accent, int listWidth, boolean coloredIncrement) {
        int plusColor = coloredIncrement ? opaque(theme.addButton) : 0xFFD4D4D8;
        int minusColor = coloredIncrement ? opaque(theme.editButton) : 0xFFA1A1AA;
        if (incremental) {
            graphics.text(font, "-", listWidth - 78, rowY + 4, minusColor, true);
            graphics.text(font, "+", listWidth - 66, rowY + 4, plusColor, true);
        } else {
            graphics.text(font, "+", listWidth - 36, rowY + 4, plusColor, true);
        }
        graphics.text(font, "ok", listWidth - 19, rowY + 4, opaque(theme.addButton), true);
    }

    public static void drawScaledText(GuiGraphicsExtractor graphics, Font font, String text, float x, float y, int color, boolean shadow, float fontScale) {
        if (Math.abs(fontScale - 1.0F) < 0.01F) {
            graphics.text(font, text, Math.round(x), Math.round(y), color, shadow);
            return;
        }
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y + 4.5F * (1.0F - fontScale));
        graphics.pose().scale(fontScale, fontScale);
        graphics.text(font, text, 0, 0, color, shadow);
        graphics.pose().popMatrix();
    }

    public static HitTarget hitTarget(
            double mouseX,
            double mouseY,
            int screenWidth,
            int screenHeight,
            Mode mode,
            TodoInlineEditor editor
    ) {
        TodoConfig config = TodoManager.config();
        if (!config.hudsVisible && mode != Mode.EDITOR) {
            return new HitTarget(Action.NONE, -1, -1);
        }
        for (int listIndex = config.lists.size() - 1; listIndex >= 0; listIndex--) {
            TodoListData list = config.lists.get(listIndex);
            if (list == null || (list.hidden && mode != Mode.EDITOR)) {
                continue;
            }
            Layout layout = layout(listIndex, screenWidth, screenHeight);
            if (!layout.contains(mouseX, mouseY)) {
                continue;
            }
            int listWidth = layout.baseWidth();
            double x = (mouseX - layout.x) / layout.scale;
            double y = (mouseY - layout.y) / layout.scale;

            // INVENTORY MODE: strictly allows toggling tasks, incrementing/decrementing, and footer done/reset
            if (mode == Mode.INVENTORY) {
                for (int taskIndex = 0; taskIndex < list.tasks.size(); taskIndex++) {
                    TodoTask task = list.tasks.get(taskIndex);
                    int rowY = HEADER_HEIGHT + taskIndex * ROW_HEIGHT;
                    if (!inside(x, y, 5, rowY + 1, listWidth - 4, rowY + ROW_HEIGHT)) {
                        continue;
                    }
                    if (task.incremental && inside(x, y, listWidth - 30, rowY + 1, listWidth - 18, rowY + ROW_HEIGHT)) {
                        return new HitTarget(Action.DECREMENT, listIndex, taskIndex);
                    }
                    if (task.incremental && inside(x, y, listWidth - 18, rowY + 1, listWidth - 4, rowY + ROW_HEIGHT)) {
                        return new HitTarget(Action.INCREMENT, listIndex, taskIndex);
                    }
                    if (inside(x, y, 6, rowY + 1, task.incremental ? (listWidth - 32) : listWidth - 5, rowY + ROW_HEIGHT)) {
                        return new HitTarget(Action.TOGGLE_TASK, listIndex, taskIndex);
                    }
                    return new HitTarget(Action.BLOCK, listIndex, taskIndex);
                }

                // Check footer buttons in INVENTORY mode (done and reset)
                int footerY = layout.baseHeight - FOOTER_HEIGHT;
                if (inside(x, y, 5, footerY + 1, listWidth - 4, footerY + FOOTER_HEIGHT)) {
                    if (inside(x, y, 5, footerY + 1, 44, footerY + FOOTER_HEIGHT)) {
                        return new HitTarget(Action.COMPLETE_ALL_TASKS, listIndex, -1);
                    }
                    if (inside(x, y, 46, footerY + 1, 88, footerY + FOOTER_HEIGHT)) {
                        return new HitTarget(Action.RESET_TASKS, listIndex, -1);
                    }
                    return new HitTarget(Action.BLOCK, listIndex, -1);
                }

                return new HitTarget(Action.BLOCK, listIndex, -1);
            }

            // EDITOR MODE
            if (inside(x, y, 4, 4, 17, 17)) {
                return new HitTarget(Action.CHANGE_ACCENT_COLOR, listIndex, -1);
            }
            if (inside(x, y, listWidth - 33, 2, listWidth - 19, 19)) {
                return new HitTarget(Action.TOGGLE_LIST_VISIBILITY, listIndex, -1);
            }
            if (inside(x, y, listWidth - 19, 2, listWidth - 2, 19)) {
                return new HitTarget(Action.DELETE_LIST, listIndex, -1);
            }

            // Task editing row
            for (int taskIndex = 0; taskIndex < list.tasks.size(); taskIndex++) {
                TodoTask task = list.tasks.get(taskIndex);
                int rowY = HEADER_HEIGHT + taskIndex * ROW_HEIGHT;
                if (!inside(x, y, 5, rowY + 1, listWidth - 4, rowY + ROW_HEIGHT)) {
                    continue;
                }
                if (editor != null && editor.isEditing(listIndex, taskIndex)) {
                    if (editor.isIncremental()) {
                        if (inside(x, y, listWidth - 82, rowY, listWidth - 70, rowY + ROW_HEIGHT)) {
                            return new HitTarget(Action.DECREMENT_CAP, listIndex, taskIndex);
                        }
                        if (inside(x, y, listWidth - 70, rowY, listWidth - 58, rowY + ROW_HEIGHT)) {
                            return new HitTarget(Action.INCREMENT_CAP, listIndex, taskIndex);
                        }
                    } else {
                        if (inside(x, y, listWidth - 40, rowY, listWidth - 24, rowY + ROW_HEIGHT)) {
                            return new HitTarget(Action.TOGGLE_INCREMENTAL, listIndex, taskIndex);
                        }
                    }
                    if (inside(x, y, listWidth - 22, rowY, listWidth, rowY + ROW_HEIGHT)) {
                        return new HitTarget(Action.SAVE_INLINE, listIndex, taskIndex);
                    }
                    return new HitTarget(Action.BLOCK, listIndex, taskIndex);
                }

                if (mode == Mode.EDITOR && inside(x, y, 16, rowY + 1, 26, rowY + ROW_HEIGHT)) {
                    return new HitTarget(Action.DRAG_TASK, listIndex, taskIndex);
                }

                int progressEndX = listWidth - 78;
                if (task.incremental && inside(x, y, progressEndX - 42, rowY + 1, progressEndX, rowY + ROW_HEIGHT)) {
                    return new HitTarget(Action.EDIT_INCREMENT, listIndex, taskIndex);
                }

                if (inside(x, y, 6, rowY + 1, task.incremental ? (progressEndX - 42) : (listWidth - 48), rowY + ROW_HEIGHT)) {
                    return new HitTarget(Action.TOGGLE_TASK, listIndex, taskIndex);
                }
                if (task.incremental && inside(x, y, listWidth - 76, rowY + 1, listWidth - 64, rowY + ROW_HEIGHT)) {
                    return new HitTarget(Action.DECREMENT, listIndex, taskIndex);
                }
                if (task.incremental && inside(x, y, listWidth - 64, rowY + 1, listWidth - 50, rowY + ROW_HEIGHT)) {
                    return new HitTarget(Action.INCREMENT, listIndex, taskIndex);
                }
                if (inside(x, y, listWidth - 50, rowY + 1, listWidth - 28, rowY + ROW_HEIGHT)) {
                    return new HitTarget(Action.EDIT_TASK, listIndex, taskIndex);
                }
                if (inside(x, y, listWidth - 28, rowY + 1, listWidth, rowY + ROW_HEIGHT)) {
                    return new HitTarget(Action.DELETE_TASK, listIndex, taskIndex);
                }
                return new HitTarget(Action.BLOCK, listIndex, taskIndex);
            }

            // Check adding task row
            if (editor != null && editor.isAdding(listIndex)) {
                int addRowY = HEADER_HEIGHT + list.tasks.size() * ROW_HEIGHT;
                if (inside(x, y, 5, addRowY, listWidth, addRowY + ROW_HEIGHT)) {
                    if (editor.isIncremental()) {
                        if (inside(x, y, listWidth - 82, addRowY, listWidth - 70, addRowY + ROW_HEIGHT)) {
                            return new HitTarget(Action.DECREMENT_CAP, listIndex, -1);
                        }
                        if (inside(x, y, listWidth - 70, addRowY, listWidth - 58, addRowY + ROW_HEIGHT)) {
                            return new HitTarget(Action.INCREMENT_CAP, listIndex, -1);
                        }
                    } else {
                        if (inside(x, y, listWidth - 40, addRowY, listWidth - 24, addRowY + ROW_HEIGHT)) {
                            return new HitTarget(Action.TOGGLE_INCREMENTAL, listIndex, -1);
                        }
                    }
                    if (inside(x, y, listWidth - 22, addRowY, listWidth, addRowY + ROW_HEIGHT)) {
                        return new HitTarget(Action.SAVE_INLINE, listIndex, -1);
                    }
                    return new HitTarget(Action.BLOCK, listIndex, -1);
                }
            }

            // Footer controls (only in EDITOR mode)
            int footerY = layout.baseHeight - FOOTER_HEIGHT;
            if (inside(x, y, 5, footerY + 1, listWidth - 4, footerY + FOOTER_HEIGHT)) {
                if (editor == null || !editor.isAdding(listIndex)) {
                    if (inside(x, y, 5, footerY + 1, 44, footerY + FOOTER_HEIGHT)) {
                        return new HitTarget(Action.ADD_TASK, listIndex, -1);
                    }
                    if (inside(x, y, 46, footerY + 1, 84, footerY + FOOTER_HEIGHT)) {
                        return new HitTarget(Action.COMPLETE_ALL_TASKS, listIndex, -1);
                    }
                    if (inside(x, y, 86, footerY + 1, 128, footerY + FOOTER_HEIGHT)) {
                        return new HitTarget(Action.RESET_TASKS, listIndex, -1);
                    }
                }
                return new HitTarget(Action.BLOCK, listIndex, -1);
            }

            return new HitTarget(Action.DRAG_LIST, listIndex, -1);
        }
        return new HitTarget(Action.NONE, -1, -1);
    }

    public static int listAt(double mouseX, double mouseY, int screenWidth, int screenHeight) {
        for (int index = TodoManager.config().lists.size() - 1; index >= 0; index--) {
            if (layout(index, screenWidth, screenHeight).contains(mouseX, mouseY)) {
                return index;
            }
        }
        return -1;
    }

    public static void moveToPixels(int listIndex, double x, double y, int screenWidth, int screenHeight) {
        TodoListData list = TodoManager.list(listIndex);
        if (list == null) {
            return;
        }
        list.horizontalPosition = Math.clamp((float) x / Math.max(1, screenWidth), 0.0F, 1.0F);
        list.verticalPosition = Math.clamp((float) y / Math.max(1, screenHeight), 0.0F, 1.0F);
    }

    public static void drawEyeIcon(GuiGraphicsExtractor graphics, int x, int y, int rgb, boolean slashed) {
        int color = opaque(rgb);
        graphics.fill(x + 2, y, x + 9, y + 1, color);
        graphics.fill(x + 2, y + 6, x + 9, y + 7, color);
        graphics.fill(x + 1, y + 1, x + 2, y + 2, color);
        graphics.fill(x, y + 2, x + 1, y + 5, color);
        graphics.fill(x + 1, y + 5, x + 2, y + 6, color);

        graphics.fill(x + 9, y + 1, x + 10, y + 2, color);
        graphics.fill(x + 10, y + 2, x + 11, y + 5, color);
        graphics.fill(x + 9, y + 5, x + 10, y + 6, color);

        if (!slashed) {
            graphics.fill(x + 4, y + 2, x + 7, y + 5, color);
        } else {
            graphics.fill(x + 1, y, x + 3, y + 1, 0xFFFF5555);
            graphics.fill(x + 3, y + 1, x + 5, y + 3, 0xFFFF5555);
            graphics.fill(x + 5, y + 3, x + 7, y + 5, 0xFFFF5555);
            graphics.fill(x + 7, y + 5, x + 9, y + 7, 0xFFFF5555);
            graphics.fill(x + 9, y + 6, x + 11, y + 7, 0xFFFF5555);
        }
    }

    public enum ResizeEdge {
        NONE,
        LEFT,
        RIGHT,
        TOP,
        BOTTOM,
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT
    }

    public record ResizeTarget(ResizeEdge edge, int listIndex) {
        public boolean isResizing() {
            return edge != ResizeEdge.NONE && listIndex >= 0;
        }
    }

    public static ResizeTarget detectResizeEdge(double mouseX, double mouseY, int screenWidth, int screenHeight) {
        TodoConfig config = TodoManager.config();
        final int MARGIN = 6;
        for (int listIndex = config.lists.size() - 1; listIndex >= 0; listIndex--) {
            Layout layout = layout(listIndex, screenWidth, screenHeight);
            int left = layout.x();
            int top = layout.y();
            int right = layout.x() + layout.scaledWidth();
            int bottom = layout.y() + layout.scaledHeight();

            // CRITICAL: If mouse is inside the HUD object bounds, NEVER trigger edge resize!
            if (mouseX >= left && mouseX < right && mouseY >= top && mouseY < bottom) {
                continue;
            }

            // Must be within outer margin
            if (mouseX < left - MARGIN || mouseX > right + MARGIN || mouseY < top - MARGIN || mouseY > bottom + MARGIN) {
                continue;
            }

            boolean onLeft = mouseX < left && mouseX >= left - MARGIN;
            boolean onRight = mouseX >= right && mouseX <= right + MARGIN;
            boolean onTop = mouseY < top && mouseY >= top - MARGIN;
            boolean onBottom = mouseY >= bottom && mouseY <= bottom + MARGIN;

            if (onLeft && onTop) return new ResizeTarget(ResizeEdge.TOP_LEFT, listIndex);
            if (onRight && onTop) return new ResizeTarget(ResizeEdge.TOP_RIGHT, listIndex);
            if (onLeft && onBottom) return new ResizeTarget(ResizeEdge.BOTTOM_LEFT, listIndex);
            if (onRight && onBottom) return new ResizeTarget(ResizeEdge.BOTTOM_RIGHT, listIndex);
            if (onLeft) return new ResizeTarget(ResizeEdge.LEFT, listIndex);
            if (onRight) return new ResizeTarget(ResizeEdge.RIGHT, listIndex);
            if (onTop) return new ResizeTarget(ResizeEdge.TOP, listIndex);
            if (onBottom) return new ResizeTarget(ResizeEdge.BOTTOM, listIndex);
        }
        return new ResizeTarget(ResizeEdge.NONE, -1);
    }

    private static void drawSquare(GuiGraphicsExtractor graphics, int x, int y, int rgb, boolean filled) {
        if (filled) {
            graphics.outline(x, y, 7, 7, opaque(rgb));
            graphics.fill(x + 2, y + 2, x + 5, y + 5, opaque(rgb));
        } else {
            graphics.outline(x, y, 7, 7, argb(rgb, 160));
        }
    }

    private static void drawGripIcon(GuiGraphicsExtractor graphics, int x, int y, int rgb) {
        int color = opaque(rgb);
        graphics.fill(x, y, x + 7, y + 1, color);
        graphics.fill(x, y + 2, x + 7, y + 3, color);
        graphics.fill(x, y + 4, x + 7, y + 5, color);
    }

    private static void drawPencil(GuiGraphicsExtractor graphics, int x, int y, int rgb) {
        int color = opaque(rgb);
        graphics.fill(x + 2, y + 7, x + 4, y + 9, color);
        graphics.fill(x + 3, y + 5, x + 6, y + 8, color);
        graphics.fill(x + 5, y + 3, x + 8, y + 6, color);
        graphics.fill(x + 7, y + 2, x + 9, y + 4, opaque(mix(rgb, 0xFFFFFF, 0.45F)));
    }

    private static String abbreviate(Font font, String text, int maximumWidth) {
        if (font.width(text) <= maximumWidth) {
            return text;
        }
        String suffix = "...";
        int suffixWidth = font.width(suffix);
        int end = text.length();
        while (end > 0 && font.width(text.substring(0, end)) + suffixWidth > maximumWidth) {
            end--;
        }
        return text.substring(0, end) + suffix;
    }

    public static int opaque(int rgb) {
        return 0xFF000000 | (rgb & 0xFFFFFF);
    }

    public static int argb(int rgb, int alpha) {
        return (Math.clamp(alpha, 0, 255) << 24) | (rgb & 0xFFFFFF);
    }

    private static int mix(int first, int second, float amount) {
        int red = Math.round(((first >> 16) & 0xFF) * (1.0F - amount) + ((second >> 16) & 0xFF) * amount);
        int green = Math.round(((first >> 8) & 0xFF) * (1.0F - amount) + ((second >> 8) & 0xFF) * amount);
        int blue = Math.round((first & 0xFF) * (1.0F - amount) + (second & 0xFF) * amount);
        return (red << 16) | (green << 8) | blue;
    }

    private static boolean inside(double x, double y, double left, double top, double right, double bottom) {
        return x >= left && x < right && y >= top && y < bottom;
    }

    public enum Mode {
        HUD,
        EDITOR,
        INVENTORY
    }

    public enum Action {
        NONE,
        BLOCK,
        TOGGLE_TASK,
        DRAG_TASK,
        EDIT_TASK,
        DELETE_TASK,
        DECREMENT,
        INCREMENT,
        ADD_TASK,
        COMPLETE_ALL_TASKS,
        RESET_TASKS,
        TOGGLE_INCREMENTAL,
        SAVE_INLINE,
        DELETE_LIST,
        DRAG_LIST,
        CHANGE_ACCENT_COLOR,
        TOGGLE_LIST_VISIBILITY,
        EDIT_INCREMENT,
        INCREMENT_CAP,
        DECREMENT_CAP
    }

    public record HitTarget(Action action, int listIndex, int taskIndex) {
    }

    public record Layout(int x, int y, int scaledWidth, int scaledHeight, int baseHeight, float scale, int baseWidth) {
        public boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + scaledWidth && mouseY >= y && mouseY < y + scaledHeight;
        }
    }
}