package com.labflow.util;

import javafx.scene.Scene;
import javafx.scene.control.DialogPane;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;

public class ThemeManager {
    public enum Theme {
        DARK,
        LIGHT
    }

    public enum ColorPalette {
        RED("Red"),
        GREEN("Green"),
        PURPLE("Purple"),
        BLUE("Blue");

        private final String displayName;

        ColorPalette(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private static final Preferences PREFERENCES = Preferences.userNodeForPackage(ThemeManager.class);
    private static final String THEME_KEY = "theme";
    private static final String COLOR_PALETTE_KEY = "colorPalette";
    private static final String ACCENT_KEY = "accentColor";
    private static final String ACCENT_LAB_PREFIX = "accentColor.lab.";
    private static final String DEFAULT_ACCENT = "#6B0F1A";
    private static final String FONT_STYLE = "-fx-font-family: 'Montserrat', 'Segoe UI', Arial, sans-serif;";
    private static final List<Consumer<Theme>> LISTENERS = new CopyOnWriteArrayList<>();
    private static final Pattern HEX_COLOR = Pattern.compile("^#[0-9a-fA-F]{6}$");

    public static Theme getTheme() {
        try {
            return Theme.valueOf(PREFERENCES.get(THEME_KEY, Theme.DARK.name()));
        } catch (Exception e) {
            return Theme.DARK;
        }
    }

    public static void setTheme(Theme theme) {
        PREFERENCES.put(THEME_KEY, theme.name());
        for (Consumer<Theme> listener : LISTENERS) {
            listener.accept(theme);
        }
    }

    public static boolean isLight() {
        return getTheme() == Theme.LIGHT;
    }

    public static Theme toggle() {
        Theme next = isLight() ? Theme.DARK : Theme.LIGHT;
        setTheme(next);
        return next;
    }

    public static ColorPalette getColorPalette() {
        ColorPalette labPalette = currentLabPalette();
        if (labPalette != null) {
            return labPalette;
        }
        return ColorPalette.RED;
    }

    public static void setColorPalette(ColorPalette palette) {
        PREFERENCES.put(COLOR_PALETTE_KEY, (palette == null ? ColorPalette.RED : palette).name());
        for (Consumer<Theme> listener : LISTENERS) {
            listener.accept(getTheme());
        }
    }

    public static void setCurrentLabColorPalette(ColorPalette palette) {
        ColorPalette safePalette = palette == null ? ColorPalette.RED : palette;
        if (SessionManager.getInstance().getCurrentLab() != null) {
            SessionManager.getInstance().getCurrentLab().setColorPalette(safePalette.name());
        } else {
            PREFERENCES.put(COLOR_PALETTE_KEY, safePalette.name());
        }
        for (Consumer<Theme> listener : LISTENERS) {
            listener.accept(getTheme());
        }
    }

    public static String getAccentColor() {
        return colors().accent().toUpperCase();
    }

    public static List<String> getChartColors() {
        PaletteColors colors = colors();
        return List.of(
                colors.accent(),
                colors.accent2(),
                colors.accent3(),
                colors.accent4(),
                colors.strongAccent(),
                colors.info(),
                colors.success(),
                colors.warning()
        );
    }

    public static List<String> getPalettePreviewColors(ColorPalette palette) {
        PaletteColors colors = colors(palette, Theme.LIGHT);
        return List.of(colors.strongAccent(), colors.accent(), colors.accent2(), colors.accent3(), colors.accent4());
    }

    public static void setAccentColor(String hexColor, boolean labScoped) {
        String normalized = normalizeHex(hexColor);
        int labId = SessionManager.getInstance().getCurrentLabId();
        if (labScoped && labId > 0) {
            PREFERENCES.put(ACCENT_LAB_PREFIX + labId, normalized);
        } else {
            PREFERENCES.put(ACCENT_KEY, normalized);
        }
    }

    public static boolean isValidHex(String value) {
        return value != null && HEX_COLOR.matcher(value.trim()).matches();
    }

    public static String normalizeHex(String value) {
        String hex = value == null ? "" : value.trim();
        if (!hex.startsWith("#")) {
            hex = "#" + hex;
        }
        if (!isValidHex(hex)) {
            throw new IllegalArgumentException("Use a valid hex color like #2F80ED.");
        }
        return hex.toUpperCase();
    }

    public static void applyTo(Scene scene) {
        if (scene == null) {
            return;
        }
        scene.getStylesheets().clear();
        scene.getStylesheets().add(currentStylesheet());
        scene.setFill(Color.web(transitionColor(isLight())));
        if (scene.getRoot() != null) {
            scene.getRoot().setStyle(themeStyle());
        }
    }

    public static void applyTo(Scene scene, ColorPalette paletteOverride) {
        if (scene == null) {
            return;
        }
        scene.getStylesheets().clear();
        scene.getStylesheets().add(currentStylesheet());
        scene.setFill(Color.web(transitionColor(isLight(), paletteOverride)));
        if (scene.getRoot() != null) {
            scene.getRoot().setStyle(themeStyle(paletteOverride));
        }
    }

    public static void applyTo(DialogPane dialogPane) {
        if (dialogPane == null) {
            return;
        }
        dialogPane.getStylesheets().clear();
        dialogPane.getStylesheets().add(currentStylesheet());
        dialogPane.setStyle(themeStyle());
    }

    public static void addThemeListener(Consumer<Theme> listener) {
        LISTENERS.add(listener);
    }

    private static String currentStylesheet() {
        String stylesheet = isLight() ? "/css/light-theme.css" : "/css/dark-theme.css";
        return ThemeManager.class.getResource(stylesheet).toExternalForm();
    }

    private static String withFont(String style) {
        if (style != null && style.contains("-fx-font-family")) {
            return style;
        }
        return (style == null || style.isBlank()) ? FONT_STYLE : style + " " + FONT_STYLE;
    }

    private static String themeStyle() {
        PaletteColors colors = colors();
        String accent = colors.accent();
        String hover = shiftColor(accent, isLight() ? -0.12 : 0.18);
        String soft = withAlpha(accent, isLight() ? 0.14 : 0.26);
        String faint = withAlpha(accent, isLight() ? 0.07 : 0.14);
        return FONT_STYLE
                + " -labflow-bg: " + colors.background() + ";"
                + " -labflow-surface: " + colors.surface() + ";"
                + " -labflow-surface-alt: " + colors.surfaceAlt() + ";"
                + " -labflow-sidebar: " + colors.sidebar() + ";"
                + " -labflow-text: " + colors.text() + ";"
                + " -labflow-muted: " + colors.muted() + ";"
                + " -labflow-accent: " + accent + ";"
                + " -labflow-accent-2: " + colors.accent2() + ";"
                + " -labflow-accent-3: " + colors.accent3() + ";"
                + " -labflow-accent-4: " + colors.accent4() + ";"
                + " -labflow-strong-accent: " + colors.strongAccent() + ";"
                + " -labflow-accent-hover: " + hover + ";"
                + " -labflow-accent-soft: " + soft + ";"
                + " -labflow-accent-faint: " + faint + ";"
                + " -labflow-on-accent: " + colors.onAccent() + ";"
                + " -labflow-border: " + colors.border() + ";"
                + " -labflow-row-alt: " + colors.rowAlt() + ";"
                + " -labflow-shadow: " + colors.shadow() + ";"
                + " -labflow-success: " + colors.success() + ";"
                + " -labflow-warning: " + colors.warning() + ";"
                + " -labflow-danger: " + colors.danger() + ";"
                + " -labflow-info: " + colors.info() + ";";
    }

    public static String transitionColor(boolean light) {
        return colors(getColorPalette(), light ? Theme.LIGHT : Theme.DARK).background();
    }

    public static String transitionColor(boolean light, ColorPalette paletteOverride) {
        return colors(paletteOverride == null ? getColorPalette() : paletteOverride, light ? Theme.LIGHT : Theme.DARK).background();
    }

    private static PaletteColors colors() {
        return colors(getColorPalette(), getTheme());
    }

    private static String themeStyle(ColorPalette paletteOverride) {
        PaletteColors colors = colors(paletteOverride == null ? getColorPalette() : paletteOverride, getTheme());
        String accent = colors.accent();
        String hover = shiftColor(accent, isLight() ? -0.12 : 0.18);
        String soft = withAlpha(accent, isLight() ? 0.14 : 0.26);
        String faint = withAlpha(accent, isLight() ? 0.07 : 0.14);
        return FONT_STYLE
                + " -labflow-bg: " + colors.background() + ";"
                + " -labflow-surface: " + colors.surface() + ";"
                + " -labflow-surface-alt: " + colors.surfaceAlt() + ";"
                + " -labflow-sidebar: " + colors.sidebar() + ";"
                + " -labflow-text: " + colors.text() + ";"
                + " -labflow-muted: " + colors.muted() + ";"
                + " -labflow-accent: " + accent + ";"
                + " -labflow-accent-2: " + colors.accent2() + ";"
                + " -labflow-accent-3: " + colors.accent3() + ";"
                + " -labflow-accent-4: " + colors.accent4() + ";"
                + " -labflow-strong-accent: " + colors.strongAccent() + ";"
                + " -labflow-accent-hover: " + hover + ";"
                + " -labflow-accent-soft: " + soft + ";"
                + " -labflow-accent-faint: " + faint + ";"
                + " -labflow-on-accent: " + colors.onAccent() + ";"
                + " -labflow-border: " + colors.border() + ";"
                + " -labflow-row-alt: " + colors.rowAlt() + ";"
                + " -labflow-shadow: " + colors.shadow() + ";"
                + " -labflow-success: " + colors.success() + ";"
                + " -labflow-warning: " + colors.warning() + ";"
                + " -labflow-danger: " + colors.danger() + ";"
                + " -labflow-info: " + colors.info() + ";";
    }

    private static ColorPalette currentLabPalette() {
        String value = SessionManager.getInstance().getCurrentLab() == null
                ? null
                : SessionManager.getInstance().getCurrentLab().getColorPalette();
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return ColorPalette.valueOf(value);
        } catch (Exception e) {
            return ColorPalette.RED;
        }
    }

    private static PaletteColors colors(ColorPalette palette, Theme theme) {
        boolean light = theme == Theme.LIGHT;
        return switch (palette == null ? ColorPalette.RED : palette) {
            case RED -> light
                    ? new PaletteColors("#FFF7F8", "#FFFFFF", "#FFF0F2", "#FFFFFF", "#2A0716", "#8C5968", "#6B0F1A", "#31081F", "#9C2532", "#F2A7B1", "#FFD9DF", "#FFFFFF", "rgba(49, 8, 31, 0.10)", "#FFFBFC", "rgba(49, 8, 31, 0.045)", "#0F9F6E", "#B77900", "#B4232F", "#9C2532")
                    : new PaletteColors("#10070B", "#181014", "#21161B", "#130B0F", "#F8FAFC", "#A7AFBA", "#E54863", "#6B0F1A", "#C0394A", "#F05A76", "#F2A7B1", "#FFFFFF", "rgba(248, 250, 252, 0.10)", "#1C1317", "rgba(0, 0, 0, 0.24)", "#54D6A1", "#F2B84B", "#FF7A8C", "#E54863");
            case GREEN -> light
                    ? new PaletteColors("#F2F8F3", "#FFFFFF", "#EAF5EE", "#F7FBF8", "#061B08", "#536A59", "#139A43", "#053B06", "#0DAB76", "#0B5D1E", "#22C877", "#FFFFFF", "rgba(5, 59, 6, 0.14)", "#F8FCF9", "rgba(5, 59, 6, 0.055)", "#139A43", "#8A6500", "#A8323E", "#0DAB76")
                    : new PaletteColors("#07110A", "#101A13", "#17251A", "#0B160E", "#F8FAFC", "#A7B7AD", "#0DAB76", "#053B06", "#139A43", "#0B5D1E", "#22C877", "#031B08", "rgba(248, 250, 252, 0.10)", "#131F16", "rgba(0, 0, 0, 0.24)", "#0DAB76", "#F2B84B", "#FF7A8C", "#22C877");
            case PURPLE -> light
                    ? new PaletteColors("#F8F3FA", "#FFFFFF", "#F1EAF7", "#FAF7FC", "#120B22", "#665B77", "#7353BA", "#2F195F", "#FAA6FF", "#0F1020", "#B780E1", "#FFFFFF", "rgba(47, 25, 95, 0.14)", "#FCFAFF", "rgba(47, 25, 95, 0.055)", "#18794E", "#8A6500", "#B4232F", "#7353BA")
                    : new PaletteColors("#0D0E18", "#151421", "#1E1A2D", "#11101C", "#F8FAFC", "#AAA6BA", "#B992FF", "#2F195F", "#7353BA", "#FAA6FF", "#4D2F93", "#0F1020", "rgba(248, 250, 252, 0.10)", "#1A1726", "rgba(0, 0, 0, 0.24)", "#63E6A8", "#F2B84B", "#FF7A8C", "#B992FF");
            case BLUE -> light
                    ? new PaletteColors("#F2F8FD", "#FFFFFF", "#EAF4FB", "#F8FBFE", "#041A34", "#58697B", "#1768AC", "#03256C", "#2541B2", "#06BEE1", "#FFFFFF", "#FFFFFF", "rgba(3, 37, 108, 0.14)", "#FBFDFF", "rgba(3, 37, 108, 0.055)", "#167D4E", "#8A6500", "#B4232F", "#06BEE1")
                    : new PaletteColors("#07111D", "#101927", "#172337", "#0B1421", "#F8FAFC", "#A5B3C3", "#06BEE1", "#03256C", "#2541B2", "#1768AC", "#FFFFFF", "#03256C", "rgba(248, 250, 252, 0.10)", "#132033", "rgba(0, 0, 0, 0.24)", "#63DFA0", "#F2B84B", "#FF7A8C", "#06BEE1");
        };
    }

    private record PaletteColors(
            String background,
            String surface,
            String surfaceAlt,
            String sidebar,
            String text,
            String muted,
            String accent,
            String strongAccent,
            String accent2,
            String accent3,
            String accent4,
            String onAccent,
            String border,
            String rowAlt,
            String shadow,
            String success,
            String warning,
            String danger,
            String info
    ) {
    }

    private static String shiftColor(String hex, double amount) {
        int r = Integer.parseInt(hex.substring(1, 3), 16);
        int g = Integer.parseInt(hex.substring(3, 5), 16);
        int b = Integer.parseInt(hex.substring(5, 7), 16);
        return "#%02X%02X%02X".formatted(shift(r, amount), shift(g, amount), shift(b, amount));
    }

    private static int shift(int value, double amount) {
        double next = amount >= 0 ? value + (255 - value) * amount : value * (1 + amount);
        return Math.max(0, Math.min(255, (int) Math.round(next)));
    }

    private static String withAlpha(String hex, double alpha) {
        int r = Integer.parseInt(hex.substring(1, 3), 16);
        int g = Integer.parseInt(hex.substring(3, 5), 16);
        int b = Integer.parseInt(hex.substring(5, 7), 16);
        return "rgba(%d, %d, %d, %.3f)".formatted(r, g, b, alpha);
    }

    private static String readableText(String hex) {
        int r = Integer.parseInt(hex.substring(1, 3), 16);
        int g = Integer.parseInt(hex.substring(3, 5), 16);
        int b = Integer.parseInt(hex.substring(5, 7), 16);
        double luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0;
        return luminance > 0.62 ? "#31081F" : "#F7F4EE";
    }
}
