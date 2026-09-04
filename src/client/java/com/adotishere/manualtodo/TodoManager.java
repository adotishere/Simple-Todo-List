package com.adotishere.manualtodo;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class TodoManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("simple-todo-list.json");
    private static final Path LEGACY_CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("manual-todo-list.json");
    private static TodoConfig config = TodoConfig.createDefault();

    private TodoManager() {
    }

    public static TodoConfig config() {
        return config;
    }

    public static void load() {
        Path path = Files.exists(CONFIG_PATH) ? CONFIG_PATH : (Files.exists(LEGACY_CONFIG_PATH) ? LEGACY_CONFIG_PATH : null);
        if (path == null) {
            config = TodoConfig.createDefault();
            save();
            return;
        }

        try {
            TodoConfig loaded = GSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), TodoConfig.class);
            config = loaded == null ? TodoConfig.createDefault() : loaded;
            config.normalize();
            save();
            if (path.equals(LEGACY_CONFIG_PATH)) {
                try {
                    Files.deleteIfExists(LEGACY_CONFIG_PATH);
                } catch (Exception ignored) {
                }
            }
        } catch (Exception exception) {
            ManualTodoListClient.LOGGER.error("Could not read {}. Using default todo settings.", path, exception);
            config = TodoConfig.createDefault();
        }
    }

    public static void save() {
        config.normalize();
        Path temporaryPath = CONFIG_PATH.resolveSibling(CONFIG_PATH.getFileName() + ".tmp");
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(temporaryPath, GSON.toJson(config), StandardCharsets.UTF_8);
            try {
                Files.move(temporaryPath, CONFIG_PATH, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException ignored) {
                Files.move(temporaryPath, CONFIG_PATH, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            ManualTodoListClient.LOGGER.error("Could not save {}.", CONFIG_PATH, exception);
        }
    }

    public static void addTask(int listIndex, String text, boolean incremental, int goal) {
        TodoListData list = list(listIndex);
        String cleanText = cleanTaskText(text);
        if (list != null && !cleanText.isEmpty()) {
            list.tasks.add(new TodoTask(cleanText, incremental, goal));
            save();
        }
    }

    public static void editTask(int listIndex, int taskIndex, String text, boolean incremental, int goal) {
        TodoTask task = task(listIndex, taskIndex);
        if (task == null) {
            return;
        }
        String cleanText = cleanTaskText(text);
        if (!cleanText.isEmpty()) {
            boolean wasIncremental = task.incremental;
            task.text = cleanText;
            task.incremental = incremental;
            task.goal = Math.max(1, goal);
            if (!incremental) {
                task.progress = 0;
            } else if (!wasIncremental) {
                task.progress = 0;
            }
            task.normalize();
            save();
        }
    }

    public static void removeTask(int listIndex, int taskIndex) {
        TodoListData list = list(listIndex);
        if (list != null && taskIndex >= 0 && taskIndex < list.tasks.size()) {
            list.tasks.remove(taskIndex);
            save();
        }
    }

    public static void moveTask(int listIndex, int fromIndex, int toIndex) {
        TodoListData list = list(listIndex);
        if (list == null || fromIndex < 0 || fromIndex >= list.tasks.size() || toIndex < 0 || toIndex >= list.tasks.size() || fromIndex == toIndex) {
            return;
        }
        TodoTask task = list.tasks.remove(fromIndex);
        list.tasks.add(toIndex, task);
        save();
    }

    public static void toggleTask(int listIndex, int taskIndex) {
        TodoTask task = task(listIndex, taskIndex);
        if (task != null) {
            task.completed = !task.completed;
            if (task.incremental) {
                task.progress = task.completed ? task.goal : 0;
            }
            save();
        }
    }

    public static final int[] ACCENT_PALETTE = {
        0x94A3B8, // Slate
        0x7DD3FC, // Soft Ice Blue
        0x86EFAC, // Sage Green
        0xFDE047, // Soft Sand
        0xFCA5A5, // Muted Coral
        0xC4B5FD, // Soft Lavender
        0xF472B6, // Soft Rose
        0x5EEAD4, // Soft Mint
        0xF4F4F5  // Crisp Off-White
    };

    public static void changeProgress(int listIndex, int taskIndex, int amount) {
        TodoTask task = task(listIndex, taskIndex);
        if (task == null) {
            return;
        }
        if (!task.incremental) {
            if (amount > 0) {
                task.incremental = true;
                task.progress = 0;
                task.goal = 2;
                task.completed = false;
                save();
            }
            return;
        }
        if (amount < 0 && task.progress == 0 && task.goal <= 2) {
            task.incremental = false;
            task.goal = 1;
            task.progress = 0;
            task.completed = false;
            save();
            return;
        }
        task.progress = Math.clamp(task.progress + amount, 0, task.goal);
        task.completed = task.progress >= task.goal;
        save();
    }

    public static void setTaskIncrement(int listIndex, int taskIndex, int progress, int goal) {
        TodoTask task = task(listIndex, taskIndex);
        if (task != null) {
            task.incremental = true;
            task.goal = Math.clamp(goal, 1, 9999);
            task.progress = Math.clamp(progress, 0, task.goal);
            task.completed = task.progress >= task.goal;
            save();
        }
    }

    public static void cycleListAccentColor(int listIndex) {
        TodoListData list = list(listIndex);
        if (list == null) {
            return;
        }
        if (list.accentColor == null) {
            list.accentColor = ACCENT_PALETTE[0];
        } else {
            int currentIndex = -1;
            for (int i = 0; i < ACCENT_PALETTE.length; i++) {
                if (ACCENT_PALETTE[i] == (list.accentColor & 0xFFFFFF)) {
                    currentIndex = i;
                    break;
                }
            }
            if (currentIndex == -1 || currentIndex == ACCENT_PALETTE.length - 1) {
                list.accentColor = null;
            } else {
                list.accentColor = ACCENT_PALETTE[currentIndex + 1];
            }
        }
        save();
    }

    public static void setListAccentColor(int listIndex, Integer color) {
        TodoListData list = list(listIndex);
        if (list != null) {
            list.accentColor = color == null ? null : (color & 0xFFFFFF);
            save();
        }
    }

    public static void toggleListHidden(int listIndex) {
        TodoListData list = list(listIndex);
        if (list != null) {
            list.hidden = !list.hidden;
            save();
        }
    }

    public static void completeAllTasks(int listIndex) {
        TodoListData list = list(listIndex);
        if (list == null) {
            return;
        }
        for (TodoTask task : list.tasks) {
            task.completed = true;
            if (task.incremental) {
                task.progress = task.goal;
            }
        }
        save();
    }

    public static void resetTasks(int listIndex) {
        TodoListData list = list(listIndex);
        if (list == null) {
            return;
        }
        for (TodoTask task : list.tasks) {
            task.completed = false;
            if (task.incremental) {
                task.progress = 0;
            }
        }
        save();
    }

    public static void addListAt(double pixelX, double pixelY, int screenWidth, int screenHeight) {
        TodoListData list = new TodoListData();
        config.lists.add(list);
        TodoHud.moveToPixels(config.lists.size() - 1, pixelX, pixelY, screenWidth, screenHeight);
        save();
    }

    public static void removeList(int listIndex) {
        if (listIndex >= 0 && listIndex < config.lists.size()) {
            config.lists.remove(listIndex);
            save();
        }
    }

    public static TodoListData list(int index) {
        return index >= 0 && index < config.lists.size() ? config.lists.get(index) : null;
    }

    public static TodoTask task(int listIndex, int taskIndex) {
        TodoListData list = list(listIndex);
        return list != null && taskIndex >= 0 && taskIndex < list.tasks.size()
                ? list.tasks.get(taskIndex)
                : null;
    }

    public static String cleanTaskText(String text) {
        if (text == null) {
            return "";
        }
        String cleaned = text.strip().replace('\n', ' ').replace('\r', ' ');
        return cleaned.length() > 120 ? cleaned.substring(0, 120) : cleaned;
    }

}
