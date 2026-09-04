package com.adotishere.manualtodo;

public final class TodoTheme {
    public int background = 0x141517;
    public int accent = 0x94A3B8;
    public int normalTask = 0xE4E4E7;
    public int completedTask = 0x86EFAC;
    public int addButton = 0xD4D4D8;
    public int editButton = 0xA1A1AA;
    public int deleteButton = 0xF87171;
    public int mutedText = 0x71717A;

    public void normalize() {
        background &= 0xFFFFFF;
        accent &= 0xFFFFFF;
        normalTask &= 0xFFFFFF;
        completedTask &= 0xFFFFFF;
        addButton &= 0xFFFFFF;
        editButton &= 0xFFFFFF;
        deleteButton &= 0xFFFFFF;
        mutedText &= 0xFFFFFF;
    }
}
