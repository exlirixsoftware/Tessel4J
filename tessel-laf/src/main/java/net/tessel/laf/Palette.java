package net.tessel.laf;

import java.awt.Color;

/**
 * The colors of a Tessel theme. The values match the Tessel.NET design tokens.
 * Obtain the active palette with {@link Tessel#palette()}.
 */
public final class Palette {

    public final boolean dark;

    public final Color background;
    public final Color surface;
    public final Color surfaceAlt;
    public final Color subtle;
    public final Color border;
    public final Color borderStrong;

    public final Color text;
    public final Color textSecondary;
    public final Color textTertiary;
    public final Color textDisabled;

    public final Color controlBackground;
    public final Color controlHover;
    public final Color controlPressed;
    public final Color inputBackground;
    public final Color overlay;

    public final Color accent;
    public final Color accentHover;
    public final Color accentPressed;
    public final Color accentSubtle;
    public final Color onAccent;

    public final Color success;
    public final Color successSubtle;
    public final Color warning;
    public final Color warningSubtle;
    public final Color danger;
    public final Color dangerHover;
    public final Color dangerSubtle;
    public final Color info;
    public final Color infoSubtle;

    /** Base color of drop shadows (includes alpha). */
    public final Color shadow;

    /**
     * @param dark         true for the dark palette
     * @param customAccent a custom accent color, or null for the built-in indigo accent
     */
    public Palette(boolean dark, Color customAccent) {
        this.dark = dark;

        background = rgb(dark ? 0x0F1115 : 0xF4F5F7);
        surface = rgb(dark ? 0x181B21 : 0xFFFFFF);
        surfaceAlt = rgb(dark ? 0x1D2027 : 0xF9FAFB);
        subtle = rgb(dark ? 0x242832 : 0xEEF0F3);
        border = rgb(dark ? 0x2C313B : 0xE1E4E8);
        borderStrong = rgb(dark ? 0x434B58 : 0xC4CAD3);

        text = rgb(dark ? 0xE8EAED : 0x1B1F24);
        textSecondary = rgb(dark ? 0xA3ACB9 : 0x58626F);
        textTertiary = rgb(dark ? 0x7A8494 : 0x8A93A0);
        textDisabled = rgb(dark ? 0x565E6B : 0xA8AFB9);

        controlBackground = rgb(dark ? 0x20242C : 0xFFFFFF);
        controlHover = rgb(dark ? 0x292E38 : 0xF2F3F5);
        controlPressed = rgb(dark ? 0x313743 : 0xE7E9ED);
        inputBackground = rgb(dark ? 0x14171C : 0xFFFFFF);
        overlay = dark ? new Color(0, 0, 0, 0x99) : new Color(0x10, 0x13, 0x18, 0x66);

        success = rgb(dark ? 0x4ADE80 : 0x15803D);
        successSubtle = rgb(dark ? 0x15291E : 0xE3F8EA);
        warning = rgb(dark ? 0xFBBF24 : 0xB45309);
        warningSubtle = rgb(dark ? 0x33280F : 0xFEF4DC);
        danger = rgb(dark ? 0xEF4444 : 0xDC2626);
        dangerHover = rgb(dark ? 0xF26363 : 0xB91C1C);
        dangerSubtle = rgb(dark ? 0x361A1C : 0xFDE8E8);
        info = rgb(dark ? 0x38BDF8 : 0x0369A1);
        infoSubtle = rgb(dark ? 0x122838 : 0xE3F2FC);

        shadow = new Color(0, 0, 0, dark ? 110 : 30);

        if (customAccent == null) {
            accent = rgb(dark ? 0x6366F1 : 0x4F46E5);
            accentHover = rgb(dark ? 0x7C7FF4 : 0x4338CA);
            accentPressed = rgb(dark ? 0x5457D6 : 0x3730A3);
            accentSubtle = rgb(dark ? 0x272A52 : 0xEEF0FF);
            onAccent = Color.WHITE;
        } else {
            Color c = new Color(customAccent.getRGB() & 0xFFFFFF);
            accent = c;
            accentHover = dark ? mix(c, Color.WHITE, 0.14) : mix(c, Color.BLACK, 0.12);
            accentPressed = dark ? mix(c, Color.BLACK, 0.15) : mix(c, Color.BLACK, 0.24);
            accentSubtle = mix(c, surface, dark ? 0.78 : 0.88);
            onAccent = luminance(c) > 0.5 ? rgb(0x111418) : Color.WHITE;
        }
    }

    /** Linear blend from {@code from} to {@code to}; {@code amount} 0 = from, 1 = to. */
    public static Color mix(Color from, Color to, double amount) {
        return new Color(
                (int) Math.round(from.getRed() + (to.getRed() - from.getRed()) * amount),
                (int) Math.round(from.getGreen() + (to.getGreen() - from.getGreen()) * amount),
                (int) Math.round(from.getBlue() + (to.getBlue() - from.getBlue()) * amount));
    }

    /** The same color with a different alpha (0-255). */
    public static Color withAlpha(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, alpha)));
    }

    /** WCAG relative luminance (0 = black, 1 = white). */
    public static double luminance(Color c) {
        return 0.2126 * channel(c.getRed()) + 0.7152 * channel(c.getGreen()) + 0.0722 * channel(c.getBlue());
    }

    private static double channel(int v) {
        double s = v / 255.0;
        return s <= 0.03928 ? s / 12.92 : Math.pow((s + 0.055) / 1.055, 2.4);
    }

    private static Color rgb(int rgb) {
        return new Color(rgb);
    }
}
