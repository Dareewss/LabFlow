package com.labflow.util;

import javafx.application.Platform;

public final class DesktopRefreshBus {
    private static Runnable refreshHandler;

    private DesktopRefreshBus() {
    }

    public static void setRefreshHandler(Runnable handler) {
        refreshHandler = handler;
    }

    public static void clearRefreshHandler(Runnable handler) {
        if (refreshHandler == handler) {
            refreshHandler = null;
        }
    }

    public static void requestRefresh() {
        Runnable handler = refreshHandler;
        if (handler == null) {
            return;
        }
        if (Platform.isFxApplicationThread()) {
            handler.run();
        } else {
            Platform.runLater(handler);
        }
    }
}
