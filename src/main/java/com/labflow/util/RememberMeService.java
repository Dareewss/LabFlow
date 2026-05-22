package com.labflow.util;

import java.util.OptionalInt;
import java.util.prefs.Preferences;

public class RememberMeService {
    private static final Preferences PREFERENCES = Preferences.userNodeForPackage(RememberMeService.class);
    private static final String REMEMBER_ENABLED = "rememberEnabled";
    private static final String USER_ID = "rememberedUserId";

    public static void rememberUser(int userId) {
        PREFERENCES.putBoolean(REMEMBER_ENABLED, true);
        PREFERENCES.putInt(USER_ID, userId);
    }

    public static void clear() {
        PREFERENCES.putBoolean(REMEMBER_ENABLED, false);
        PREFERENCES.remove(USER_ID);
    }

    public static boolean isRememberEnabled() {
        return PREFERENCES.getBoolean(REMEMBER_ENABLED, false);
    }

    public static OptionalInt getRememberedUserId() {
        if (!isRememberEnabled()) {
            return OptionalInt.empty();
        }
        int userId = PREFERENCES.getInt(USER_ID, -1);
        return userId > 0 ? OptionalInt.of(userId) : OptionalInt.empty();
    }
}
