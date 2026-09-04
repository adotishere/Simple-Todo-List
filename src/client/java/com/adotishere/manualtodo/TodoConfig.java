package com.adotishere.manualtodo;

import java.util.ArrayList;
import java.util.List;

public final class TodoConfig {
    public int backgroundOpacityPercent = 68;
    public boolean hudsVisible = true;
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
        backgroundOpacityPercent = Math.max(0, Math.min(100, backgroundOpacityPercent));
        if (theme == null) {
            theme = new TodoTheme();
        }
        if (theme.background == 0x0B1018 && theme.accent == 0x55B9FF) {
            theme.background = 0x141517;
            theme.accent = 0x94A3B8;
            theme.normalTask = 0xE4E4E7;
            theme.completedTask = 0x86EFAC;
            theme.addButton = 0xD4D4D8;
            theme.editButton = 0xA1A1AA;
            theme.deleteButton = 0xF87171;
            theme.mutedText = 0x71717A;
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
