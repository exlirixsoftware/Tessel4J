package net.tessel.laf;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Font helpers. Uses Segoe UI / Segoe Fluent Icons on Windows and sensible fallbacks elsewhere. */
public final class Fonts {

    private static volatile Set<String> families;
    private static final Map<Float, Font> ICON_FONTS = new ConcurrentHashMap<>();
    private static String uiFamily;
    private static String semiboldFamily;
    private static String monoFamily;
    private static String iconFamily;
    private static boolean resolved;

    private Fonts() {
    }

    /** True when a font family with this name is installed. */
    public static boolean isInstalled(String family) {
        if (families == null) {
            synchronized (Fonts.class) {
                if (families == null) {
                    String[] names = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames(Locale.ROOT);
                    families = new HashSet<>(Arrays.asList(names));
                }
            }
        }
        return families.contains(family);
    }

    private static synchronized void resolve() {
        if (resolved) return;
        uiFamily = first("Segoe UI", "SF Pro Text", "Helvetica Neue", "Inter", "Cantarell", "Ubuntu", "Noto Sans", "DejaVu Sans");
        if (uiFamily == null) uiFamily = Font.DIALOG;
        semiboldFamily = first("Segoe UI Semibold", "Inter SemiBold");
        monoFamily = first("Cascadia Mono", "Consolas", "JetBrains Mono", "SF Mono", "Menlo", "DejaVu Sans Mono");
        if (monoFamily == null) monoFamily = Font.MONOSPACED;
        iconFamily = first("Segoe Fluent Icons", "Segoe MDL2 Assets");
        resolved = true;
    }

    private static String first(String... names) {
        for (String n : names) {
            if (isInstalled(n)) return n;
        }
        return null;
    }

    public static String uiFamily() {
        resolve();
        return uiFamily;
    }

    public static Font regular(float size) {
        resolve();
        return new Font(uiFamily, Font.PLAIN, 1).deriveFont(size);
    }

    public static Font semibold(float size) {
        resolve();
        if (semiboldFamily != null) return new Font(semiboldFamily, Font.PLAIN, 1).deriveFont(size);
        return new Font(uiFamily, Font.BOLD, 1).deriveFont(size);
    }

    public static Font mono(float size) {
        resolve();
        return new Font(monoFamily, Font.PLAIN, 1).deriveFont(size);
    }

    /** The icon font (Segoe Fluent Icons / MDL2 Assets), or null when not installed. */
    public static Font icon(float size) {
        resolve();
        if (iconFamily == null) return null;
        return ICON_FONTS.computeIfAbsent(size, s -> new Font(iconFamily, Font.PLAIN, 1).deriveFont(s));
    }

    public static boolean hasIconFont() {
        resolve();
        return iconFamily != null;
    }

    public static Font forStyle(TextStyle style) {
        if (style == TextStyle.CODE) return mono(style.size);
        return style.semibold ? semibold(style.size) : regular(style.size);
    }
}
