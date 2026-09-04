package com.adotishere.manualtodo;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

public final class TodoSettingsScreen extends Screen implements TodoScreenMarker {
    private final @Nullable Screen parent;
    private final Map<ColorSlot, EditBox> colorFields = new EnumMap<>(ColorSlot.class);
    private EditBox opacityField;
    private String error = "";
    private static final int FIELD_WIDTH = 54;
    private static final int FIELD_HEIGHT = 18;
    private static final int SWATCH_SIZE = 12;
    private static final int ROW_SPACING = 24;

    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;

    // Palette popup state
    private @Nullable ColorSlot activePaletteSlot = null;
    private int paletteX = 0;
    private int paletteY = 0;

    public static final int[] SETTINGS_PALETTE = {
            0x94A3B8, 0x7DD3FC, 0x86EFAC, 0x6EE7B7, 0xFDE047,
            0xFDBA74, 0xFCA5A5, 0xF87171, 0xC4B5FD, 0xF472B6,
            0x5EEAD4, 0xF4F4F5, 0xA1A1AA, 0x27272A, 0x141517
    };

    private static final ColorSlot[] SLOTS = {
            ColorSlot.BACKGROUND,
            ColorSlot.ACCENT,
            ColorSlot.NORMAL_TASK,
            ColorSlot.COMPLETED_TASK,
            ColorSlot.INCREMENTAL_TEXT
    };

    public TodoSettingsScreen(@Nullable Screen parent) {
        super(Component.translatable("screen.manual_todo_list.settings"));
        this.parent = parent;
    }

    private int getRowY(int row) {
        return panelY + 34 + row * ROW_SPACING;
    }

    private int getFieldX() {
        return panelX + panelWidth - 18 - FIELD_WIDTH;
    }

    private int getPreviewX() {
        return getFieldX() - SWATCH_SIZE - 6;
    }

    @Override
    protected void init() {
        colorFields.clear();
        activePaletteSlot = null;
        panelWidth = Math.min(280, width - 20);
        panelHeight = 240;
        panelX = (width - panelWidth) / 2;
        panelY = Math.max(10, (height - panelHeight) / 2);

        // Row 0: Opacity %
        opacityField = makeField(0, 3, Integer.toString(TodoManager.config().backgroundOpacityPercent));
        opacityField.setResponder(val -> {
            try {
                if (!val.trim().isEmpty()) {
                    int op = Integer.parseInt(val.trim());
                    TodoManager.config().backgroundOpacityPercent = Math.clamp(op, 0, 100);
                }
            } catch (NumberFormatException ignored) {
            }
        });

        // Rows 1-5: Colors
        for (int i = 0; i < SLOTS.length; i++) {
            addColorField(SLOTS[i], i + 1);
        }

        // Row 6: Colored +/- Toggle
        int toggleWidth = 52;
        int toggleX = panelX + panelWidth - toggleWidth - 18;
        int toggleY = getRowY(SLOTS.length + 1);
        Button toggleBtn = Button.builder(
                getColoredToggleText(TodoManager.config().coloredIncrementButtons),
                button -> {
                    TodoConfig cfg = TodoManager.config();
                    cfg.coloredIncrementButtons = !cfg.coloredIncrementButtons;
                    button.setMessage(getColoredToggleText(cfg.coloredIncrementButtons));
                }
        ).bounds(toggleX, toggleY, toggleWidth, FIELD_HEIGHT).build();
        addRenderableWidget(toggleBtn);

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> saveAndClose())
                .bounds(panelX + panelWidth / 2 - 50, panelY + panelHeight - 26, 100, 18)
                .build());
    }

    private static Component getColoredToggleText(boolean enabled) {
        return Component.translatable(enabled ? "screen.manual_todo_list.colored_buttons_on" : "screen.manual_todo_list.colored_buttons_off");
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, width, height, 0xD010141A);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        TodoTheme theme = TodoManager.config().theme;
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, TodoHud.argb(theme.background, 239));
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + 2, TodoHud.opaque(theme.accent));
        graphics.centeredText(font, title, width / 2, panelY + 12, TodoHud.opaque(theme.accent));

        // Row 0: Opacity
        drawLabel(graphics, Component.translatable("screen.manual_todo_list.opacity_label"), 0, null, false);

        // Rows 1-5: Colors
        for (int i = 0; i < SLOTS.length; i++) {
            drawColorRow(graphics, SLOTS[i], i + 1, mouseX, mouseY);
        }

        // Row 6: Colored +/- buttons
        drawLabel(graphics, Component.translatable("screen.manual_todo_list.colored_increment_buttons"), SLOTS.length + 1, null, false);

        if (!error.isEmpty()) {
            graphics.centeredText(font, error, width / 2, panelY + panelHeight - 40, TodoHud.opaque(theme.deleteButton));
        }

        super.extractRenderState(graphics, mouseX, mouseY, delta);

        if (activePaletteSlot != null) {
            drawPaletteMenu(graphics, mouseX, mouseY);
        }
    }

    private void drawColorRow(GuiGraphicsExtractor graphics, ColorSlot slot, int row, int mouseX, int mouseY) {
        EditBox box = colorFields.get(slot);
        Integer preview = box != null ? tryParseColor(box.getValue()) : null;
        int px = getPreviewX();
        int py = getRowY(row) + (FIELD_HEIGHT - SWATCH_SIZE) / 2;
        boolean isHovered = mouseX >= px - 1 && mouseX < px + SWATCH_SIZE + 1 && mouseY >= py - 1 && mouseY < py + SWATCH_SIZE + 1;
        drawLabel(graphics, Component.translatable(slot.translationKey), row, preview, isHovered);
    }

    private void drawLabel(GuiGraphicsExtractor graphics, Component label, int row, @Nullable Integer preview, boolean isHovered) {
        TodoTheme theme = TodoManager.config().theme;
        int x = panelX + 18;
        int y = getRowY(row) + (FIELD_HEIGHT - 9) / 2;
        graphics.text(font, label, x, y, TodoHud.opaque(theme.normalTask), false);
        if (preview != null) {
            int px = getPreviewX();
            int py = getRowY(row) + (FIELD_HEIGHT - SWATCH_SIZE) / 2;
            graphics.fill(px, py, px + SWATCH_SIZE, py + SWATCH_SIZE, TodoHud.opaque(preview));
            graphics.outline(px - 1, py - 1, SWATCH_SIZE + 2, SWATCH_SIZE + 2, isHovered ? 0xFFFFFFFF : 0xFFAAAAAA);
        }
    }

    private void drawPaletteMenu(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int px = paletteX;
        int py = paletteY;
        int pw = 95;
        int ph = 64;
        if (px + pw > width) px = width - pw - 4;
        if (py + ph > height) py = height - ph - 4;

        graphics.fill(px, py, px + pw, py + ph, 0xF012151D);
        graphics.outline(px, py, pw, ph, 0xFF55B9FF);

        for (int i = 0; i < SETTINGS_PALETTE.length; i++) {
            int col = i % 5;
            int row = i / 5;
            int sx = px + 6 + col * 17;
            int sy = py + 6 + row * 18;
            int color = SETTINGS_PALETTE[i];
            boolean isHovered = mouseX >= sx && mouseX < sx + 14 && mouseY >= sy && mouseY < sy + 14;

            graphics.fill(sx, sy, sx + 14, sy + 14, TodoHud.opaque(color));
            if (isHovered) {
                graphics.outline(sx - 1, sy - 1, 16, 16, 0xFFFFFFFF);
            } else {
                graphics.outline(sx, sy, 14, 14, 0x55000000);
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (activePaletteSlot != null) {
            int px = paletteX;
            int py = paletteY;
            int pw = 95;
            int ph = 64;
            if (px + pw > width) px = width - pw - 4;
            if (py + ph > height) py = height - ph - 4;

            if (event.x() >= px && event.x() < px + pw && event.y() >= py && event.y() < py + ph) {
                for (int i = 0; i < SETTINGS_PALETTE.length; i++) {
                    int col = i % 5;
                    int row = i / 5;
                    int sx = px + 6 + col * 17;
                    int sy = py + 6 + row * 18;
                    if (event.x() >= sx && event.x() < sx + 14 && event.y() >= sy && event.y() < sy + 14) {
                        EditBox box = colorFields.get(activePaletteSlot);
                        if (box != null) {
                            box.setValue(hex(SETTINGS_PALETTE[i]));
                        }
                        activePaletteSlot = null;
                        return true;
                    }
                }
                return true;
            } else {
                activePaletteSlot = null;
            }
        }

        // Check if clicking preview square on any color slot
        ColorSlot clickedSlot = getSlotAtPreview(event.x(), event.y());
        if (clickedSlot != null) {
            activePaletteSlot = clickedSlot;
            paletteX = (int) Math.round(event.x());
            paletteY = (int) Math.round(event.y() + 8);
            return true;
        }

        return super.mouseClicked(event, doubleClick);
    }

    private @Nullable ColorSlot getSlotAtPreview(double mouseX, double mouseY) {
        int px = getPreviewX();
        for (int i = 0; i < SLOTS.length; i++) {
            int py = getRowY(i + 1) + (FIELD_HEIGHT - SWATCH_SIZE) / 2;
            if (mouseX >= px - 1 && mouseX < px + SWATCH_SIZE + 1 && mouseY >= py - 1 && mouseY < py + SWATCH_SIZE + 1) {
                return SLOTS[i];
            }
        }
        return null;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == 256) {
            if (activePaletteSlot != null) {
                activePaletteSlot = null;
                return true;
            }
            saveAndClose();
            return true;
        }
        if (event.key() == 257 || event.key() == 335) {
            saveAndClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        activePaletteSlot = null;
        save();
        minecraft.setScreenAndShow(parent);
    }

    private EditBox makeField(int row, int maxLength, String value) {
        int fieldX = getFieldX();
        int fieldY = getRowY(row);
        EditBox field = new EditBox(font, fieldX, fieldY, FIELD_WIDTH, FIELD_HEIGHT, Component.empty());
        field.setMaxLength(maxLength);
        field.setValue(value);
        addRenderableWidget(field);
        return field;
    }

    private void addColorField(ColorSlot slot, int row) {
        colorFields.put(slot, makeField(row, 7, hex(slot.read(TodoManager.config().theme))));
    }

    private boolean save() {
        try {
            String val = opacityField.getValue().trim();
            int opacity = val.isEmpty() ? 0 : Integer.parseInt(val);
            TodoManager.config().backgroundOpacityPercent = Math.clamp(opacity, 0, 100);
        } catch (NumberFormatException exception) {
            error = Component.translatable("screen.manual_todo_list.opacity_error").getString();
            return false;
        }

        Map<ColorSlot, Integer> parsed = new EnumMap<>(ColorSlot.class);
        for (ColorSlot slot : SLOTS) {
            EditBox box = colorFields.get(slot);
            if (box == null) continue;
            Integer color = tryParseColor(box.getValue());
            if (color == null) {
                error = Component.translatable("screen.manual_todo_list.color_error", Component.translatable(slot.translationKey)).getString();
                return false;
            }
            parsed.put(slot, color);
        }

        TodoConfig config = TodoManager.config();
        parsed.forEach((slot, value) -> slot.write(config.theme, value));
        TodoManager.save();
        return true;
    }

    private void saveAndClose() {
        if (save()) {
            minecraft.setScreenAndShow(parent);
        }
    }

    private static @Nullable Integer tryParseColor(String value) {
        String clean = value.trim();
        if (clean.startsWith("#")) {
            clean = clean.substring(1);
        }
        if (!clean.matches("(?i)[0-9a-f]{6}")) {
            return null;
        }
        return Integer.parseInt(clean, 16);
    }

    private static String hex(int color) {
        return String.format("%06X", color & 0xFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean isInGameUi() {
        return minecraft.level != null;
    }

    private enum ColorSlot {
        BACKGROUND("screen.manual_todo_list.color.background") {
            int read(TodoTheme theme) { return theme.background; }
            void write(TodoTheme theme, int value) { theme.background = value; }
        },
        ACCENT("screen.manual_todo_list.color.accent") {
            int read(TodoTheme theme) { return theme.accent; }
            void write(TodoTheme theme, int value) { theme.accent = value; }
        },
        NORMAL_TASK("screen.manual_todo_list.color.normal") {
            int read(TodoTheme theme) { return theme.normalTask; }
            void write(TodoTheme theme, int value) { theme.normalTask = value; }
        },
        COMPLETED_TASK("screen.manual_todo_list.color.completed") {
            int read(TodoTheme theme) { return theme.completedTask; }
            void write(TodoTheme theme, int value) { theme.completedTask = value; }
        },
        INCREMENTAL_TEXT("screen.manual_todo_list.color.incremental") {
            int read(TodoTheme theme) { return theme.incrementalText; }
            void write(TodoTheme theme, int value) { theme.incrementalText = value; }
        };

        private final String translationKey;

        ColorSlot(String translationKey) {
            this.translationKey = translationKey;
        }

        abstract int read(TodoTheme theme);
        abstract void write(TodoTheme theme, int value);
    }
}