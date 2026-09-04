package com.adotishere.manualtodo.compat;

import com.adotishere.manualtodo.TodoSettingsScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public final class ModMenuCompat implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return TodoSettingsScreen::new;
    }
}
