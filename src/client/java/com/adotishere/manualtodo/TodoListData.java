package com.adotishere.manualtodo;

import java.util.ArrayList;
import java.util.List;

public final class TodoListData {
    public String title = "TODO LIST";
    public float horizontalPosition = 0.025F;
    public float verticalPosition = 0.16F;
    public float scale = 1.0F;
    public float fontScale = 1.0F;
    public int customWidth = 270;
    public int extraHeight = 0;
    public Integer accentColor = null;
    public boolean hidden = false;
    public List<TodoTask> tasks = new ArrayList<>();

    public void normalize() {
        title = title == null || title.isBlank() ? "TODO LIST" : title.strip();
        horizontalPosition = Math.clamp(horizontalPosition, 0.0F, 1.0F);
        verticalPosition = Math.clamp(verticalPosition, 0.0F, 1.0F);
        scale = Math.clamp(scale, 0.50F, 2.50F);
        fontScale = Math.clamp(fontScale, 0.60F, 1.80F);
        customWidth = Math.clamp(customWidth, 180, 800);
        extraHeight = Math.max(0, extraHeight);
        if (accentColor != null) {
            accentColor &= 0xFFFFFF;
        }
        if (tasks == null) {
            tasks = new ArrayList<>();
        }
        tasks.removeIf(task -> task == null || task.text == null || task.text.isBlank());
        tasks.forEach(TodoTask::normalize);
    }
}
