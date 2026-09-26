package net.tessel.laf;

import java.awt.Color;
import java.awt.Window;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

/**
 * Entry point of the Tessel look and feel.
 *
 * <pre>{@code
 * SwingUtilities.invokeLater(() -> {
 *     Tessel.setup(TesselTheme.SYSTEM);
 *     new MyFrame().setVisible(true);
 * });
 * }</pre>
 *
 * Theme and accent can be changed at runtime with {@link #setTheme(TesselTheme)} and {@link #setAccent(Color)};
 * all open windows are updated.
 */
public final class Tessel {

    public static final String VERSION = "1.0.0";

    /** Client property: {@link ButtonStyle} (or its name) of a button. */
    public static final String BUTTON_STYLE = "Tessel.buttonStyle";
    /** Client property: hint text shown in an empty text field, text area or combo box. */
    public static final String PLACEHOLDER = "Tessel.placeholder";
    /** Client property: {@link Icon} shown at the start of a text field. */
    public static final String LEADING_ICON = "Tessel.leadingIcon";
    /** Client property: {@link TextStyle} of a label. */
    public static final String TEXT_STYLE = "Tessel.textStyle";
    /** Client property: Boolean, draws a check box in the indeterminate (mixed) state. */
    public static final String INDETERMINATE = "Tessel.indeterminate";

    private static TesselTheme theme = TesselTheme.LIGHT;
    private static Color accent;
    private static Palette palette = new Palette(false, null);
    private static final List<Runnable> LISTENERS = new CopyOnWriteArrayList<>();

    private Tessel() {
    }

    // ------------------------------------------------------------------ setup & theming

    /** Installs the look and feel following the system theme. Call on the event dispatch thread. */
    public static void setup() {
        setup(TesselTheme.SYSTEM, null);
    }

    /** Installs the look and feel with the given theme. Call on the event dispatch thread. */
    public static void setup(TesselTheme theme) {
        setup(theme, null);
    }

    /** Installs the look and feel with a theme and a custom accent (null = built-in). */
    public static void setup(TesselTheme theme, Color accent) {
        Tessel.theme = theme == null ? TesselTheme.SYSTEM : theme;
        Tessel.accent = accent;
        install(false);
    }

    /** Switches the theme and updates every open window. */
    public static void setTheme(TesselTheme theme) {
        Tessel.theme = theme == null ? TesselTheme.SYSTEM : theme;
        install(true);
    }

    /** Sets a custom accent color (null = built-in indigo) and updates every open window. */
    public static void setAccent(Color accent) {
        Tessel.accent = accent;
        install(true);
    }

    /** Uses the operating system accent color when one is available. */
    public static void useSystemAccent() {
        Color c = getSystemAccent();
        if (c != null) setAccent(c);
    }

    public static TesselTheme getTheme() {
        return theme;
    }

    public static Color getAccent() {
        return accent;
    }

    /** The palette of the installed theme. */
    public static Palette palette() {
        return palette;
    }

    public static boolean isDark() {
        return palette.dark;
    }

    /** Registers a callback invoked after the theme or accent changes. */
    public static void addThemeListener(Runnable listener) {
        LISTENERS.add(listener);
    }

    public static void removeThemeListener(Runnable listener) {
        LISTENERS.remove(listener);
    }

    private static void install(boolean updateWindows) {
        boolean dark = theme == TesselTheme.DARK || (theme == TesselTheme.SYSTEM && isSystemDark());
        try {
            UIManager.setLookAndFeel(new TesselLookAndFeel(new Palette(dark, accent)));
        } catch (UnsupportedLookAndFeelException e) {
            throw new IllegalStateException(e);
        }
        if (updateWindows) {
            for (Window w : Window.getWindows()) {
                SwingUtilities.updateComponentTreeUI(w);
                w.repaint();
            }
        }
        for (Runnable r : LISTENERS) r.run();
    }

    /** Called by {@link TesselLookAndFeel#initialize()}. */
    static void onInstalled(TesselLookAndFeel laf) {
        palette = laf.getPalette();
        if (theme != TesselTheme.SYSTEM) theme = palette.dark ? TesselTheme.DARK : TesselTheme.LIGHT;
    }

    // ------------------------------------------------------------------ component helpers

    /** Applies a {@link ButtonStyle} to a button. */
    public static <T extends AbstractButton> T style(T button, ButtonStyle style) {
        button.putClientProperty(BUTTON_STYLE, style);
        if (style == ButtonStyle.LINK) button.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        button.revalidate();
        button.repaint();
        return button;
    }

    /** Sets the placeholder (hint) text of a text component or combo box. */
    public static <T extends JComponent> T placeholder(T component, String text) {
        component.putClientProperty(PLACEHOLDER, text);
        component.repaint();
        return component;
    }

    /** Shows an icon at the start of a text field. */
    public static <T extends JComponent> T leadingIcon(T component, Icon icon) {
        component.putClientProperty(LEADING_ICON, icon);
        component.revalidate();
        component.repaint();
        return component;
    }

    /** Applies a {@link TextStyle} (font and color) to a label; follows theme changes. */
    public static <T extends JComponent> T textStyle(T component, TextStyle style) {
        component.putClientProperty(TEXT_STYLE, style);
        return component;
    }

    /** Creates a label with a {@link TextStyle}. */
    public static JLabel label(String text, TextStyle style) {
        return textStyle(new JLabel(text), style);
    }

    public static java.awt.Font font(TextStyle style) {
        return Fonts.forStyle(style);
    }

    // ------------------------------------------------------------------ system integration

    /** Detects a dark system theme on Windows, macOS and GNOME. Returns false when unknown. */
    public static boolean isSystemDark() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        try {
            if (os.contains("win")) {
                String out = run("reg", "query", "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize", "/v", "AppsUseLightTheme");
                return out != null && out.contains("0x0") && !out.contains("0x1");
            }
            if (os.contains("mac")) {
                String out = run("defaults", "read", "-g", "AppleInterfaceStyle");
                return out != null && out.trim().equalsIgnoreCase("Dark");
            }
            String out = run("gsettings", "get", "org.gnome.desktop.interface", "color-scheme");
            return out != null && out.contains("dark");
        } catch (Exception e) {
            return false;
        }
    }

    /** Reads the Windows accent color, or null when unavailable. */
    public static Color getSystemAccent() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (!os.contains("win")) return null;
        try {
            String out = run("reg", "query", "HKCU\\Software\\Microsoft\\Windows\\DWM", "/v", "AccentColor");
            if (out == null) return null;
            int i = out.indexOf("0x");
            if (i < 0) return null;
            String hex = out.substring(i + 2).trim().split("\\s+")[0];
            long abgr = Long.parseLong(hex, 16);
            return new Color((int) (abgr & 0xFF), (int) ((abgr >> 8) & 0xFF), (int) ((abgr >> 16) & 0xFF));
        } catch (Exception e) {
            return null;
        }
    }

    private static String run(String... command) throws Exception {
        Process p = new ProcessBuilder(command).redirectErrorStream(true).start();
        StringBuilder sb = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) sb.append(line).append('\n');
        }
        if (!p.waitFor(3, TimeUnit.SECONDS)) {
            p.destroy();
            return null;
        }
        return p.exitValue() == 0 ? sb.toString() : null;
    }
}
