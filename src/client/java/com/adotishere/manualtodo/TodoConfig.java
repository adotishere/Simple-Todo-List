package com.adotishere.manualtodo;

import java.util.ArrayList;
import java.util.List;

public final class TodoConfig {
    public int backgroundOpacityPercent = 50;
    public boolean hudsVisible = true;
    public boolean coloredIncrementButtons = false;
    public TodoTheme theme = new TodoTheme();
    public List<TodoListData> lists = new ArrayList<>();

    // Version 1 fields. They remain readable only so existing tasks can be migrated.
    public Float horizontalPosition;
    public Float verticalPosition;
    public Float scale;
    public List<TodoTask> tasks;

    public static TodoConfig createDefault() {
        return new TodoConfig();
    }

    public void normalize() {
        if (backgroundOpacityPercent == 68) {
            backgroundOpacityPercent = 50;
        }
        backgroundOpacityPercent = Math.max(0, Math.min(100, backgroundOpacityPercent));
        if (theme == null) {
            theme = new TodoTheme();
        }
        if (theme.addButton == 0xD4D4D8) {
            theme.addButton = 0x75F09A;
        }
        if (theme.editButton == 0xA1A1AA) {
            theme.editButton = 0xFFA23A;
        }
        if (theme.accent == 0x94A3B8) {
            theme.accent = 0x55B9FF;
        }
        theme.normalize();
        if (lists == null) {
            lists = new ArrayList<>();
        }

        if (tasks != null) {
            TodoListData migrated = new TodoListData();
            migrated.horizontalPosition = horizontalPosition == null ? 0.025F : horizontalPosition;
            migrated.verticalPosition = verticalPosition == null ? 0.16F : verticalPosition;
            migrated.scale = scale == null ? 1.0F : scale;
            migrated.tasks = tasks;
            lists.add(migrated);
            horizontalPosition = null;
            verticalPosition = null;
            scale = null;
            tasks = null;
        }

        lists.removeIf(list -> list == null);
        lists.forEach(TodoListData::normalize);
    }
}
