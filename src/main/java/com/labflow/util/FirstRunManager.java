package com.labflow.util;

import com.labflow.dao.PreferencesDAO;

public final class FirstRunManager {
    private static final String ONBOARDING_COMPLETED = "onboarding_completed";
    private static final String TUTORIAL_SEEN_PREFIX = "tutorial_seen_user_";
    private static final PreferencesDAO PREFERENCES = new PreferencesDAO();

    private FirstRunManager() {
    }

    public static boolean isFirstRun() {
        return !"true".equalsIgnoreCase(PREFERENCES.get(ONBOARDING_COMPLETED, "false"));
    }

    public static void markOnboardingComplete() {
        PREFERENCES.set(ONBOARDING_COMPLETED, "true");
    }

    public static boolean shouldShowTutorial(int userId) {
        if (userId <= 0) {
            return false;
        }
        return !"true".equalsIgnoreCase(PREFERENCES.get(TUTORIAL_SEEN_PREFIX + userId, "false"));
    }

    public static void markTutorialSeen(int userId) {
        if (userId > 0) {
            PREFERENCES.set(TUTORIAL_SEEN_PREFIX + userId, "true");
        }
    }
}
