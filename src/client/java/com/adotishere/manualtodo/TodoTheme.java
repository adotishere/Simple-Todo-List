package com.adotishere.manualtodo;

public final class TodoTheme {
    public int background = 0x141517;
    public int accent = 0x55B9FF;
    public int normalTask = 0xE4E4E7;
    public int completedTask = 0x86EFAC;
    public int addButton = 0x75F09A;
    public int editButton = 0xFFA23A;
    public int deleteButton = 0xF87171;
    public int mutedText = 0x71717A;
    public int incrementalText = 0xD4D4D8;

    public void normalize() {
        background &= 0xFFFFFF;
        accent &= 0xFFFFFF;
        normalTask &= 0xFFFFFF;
        completedTask &= 0xFFFFFF;
        addButton &= 0xFFFFFF;
        editButton &= 0xFFFFFF;
        deleteButton &= 0xFFFFFF;
        mutedText &= 0xFFFFFF;
        if (incrementalText == 0 || incrementalText == 0x71717A) {
            incrementalText = 0xD4D4D8;
        }
        incrementalText &= 0xFFFFFF;
    }
}
