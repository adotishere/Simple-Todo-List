package com.adotishere.manualtodo;

public final class TodoTask {
    public String text;
    public boolean completed;
    public boolean incremental;
    public int progress;
    public int goal = 1;

    public TodoTask(String text) {
        this(text, false);
    }

    public TodoTask(String text, boolean completed) {
        this.text = text;
        this.completed = completed;
    }

    public TodoTask(String text, boolean incremental, int goal) {
        this.text = text;
        this.incremental = incremental;
        this.goal = Math.max(1, goal);
    }

    public void normalize() {
        text = TodoManager.cleanTaskText(text);
        goal = Math.max(1, goal);
        progress = Math.max(0, Math.min(progress, goal));
        if (incremental) {
            completed = progress >= goal;
        } else {
            progress = 0;
        }
    }
}
