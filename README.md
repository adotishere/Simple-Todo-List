# Simple Todo List

A client-side Fabric mod for Minecraft. It adds compact todo lists to the HUD.

## Features

- Multiple independently positioned and scaled todo-list HUDs
- Persistent lists and tasks stored in `config/simple-todo-list.json`
- Click task text to toggle completion; completed tasks turn green
- Inline add/edit fields appear directly inside the selected task row
- Optional per-task incremental goals with a current/goal counter and `-`/`+` controls
- Non-pausing HUD editor that dims the game and unlocks the mouse
- Drag lists and drag sides/corners to resize them
- Right-click empty editor space to add a list; use its red `x` to delete it
- Per-list reset control that clears completion and counter progress without deleting tasks
- Global eye control that hides or shows lists
- Lists render above inventory/container screens and stay fully interactive
- Settings for background opacity and menu colors using six-digit hex values
- Two fully rebindable controls in Minecraft's normal Key Binds menu

## Default controls

- `O`: Open Todo Settings
- `P`: Edit / Interact With Todo Lists
- `/todolist`: Open the Todo HUD editor directly
- `/todolist settings`: Open the settings menu directly

Controls can be changed under **Options > Controls > Key Binds > Simple Todo List**.

## Installation

1. Install Fabric Loader (0.19.3 or newer) for your Minecraft version.
2. Install the matching version of Fabric API.
3. Download the release `.jar` for your Minecraft version from the [Releases](https://github.com/adotishere/Simple-Todo-List/releases) page and place it in your Minecraft `mods` folder.

This mod is client-side only and does not need to be installed on a server.

## Building

Building requires Java 25.

```bash
./gradlew build
```

The finished mod will be in `build/libs/`.
