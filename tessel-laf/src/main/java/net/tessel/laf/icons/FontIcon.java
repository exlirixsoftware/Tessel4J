package net.tessel.laf.icons;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.function.Function;
import javax.swing.Icon;
import net.tessel.laf.Fonts;
import net.tessel.laf.Palette;
import net.tessel.laf.Tessel;

/**
 * An icon drawn from the Segoe Fluent Icons font (Windows 11) or Segoe MDL2 Assets (Windows 10).
 * When neither font is installed the icon paints nothing but keeps its size.
 *
 * <p>By default the glyph uses the foreground color of the component it is painted on;
 * use {@link #themed(Symbol, float, Function)} for a palette color that follows theme changes.
 */
public class FontIcon implements Icon {

    private final String glyph;
    private final float size;
    private final Function<Palette, Color> color;

    public FontIcon(Symbol symbol) {
        this(symbol.glyph(), 16, null);
    }

    public FontIcon(Symbol symbol, float size) {
        this(symbol.glyph(), size, null);
    }

    public FontIcon(Symbol symbol, float size, Color color) {
        this(symbol.glyph(), size, color == null ? null : p -> color);
    }

    public FontIcon(String glyph, float size, Function<Palette, Color> color) {
        this.glyph = glyph;
        this.size = size;
        this.color = color;
    }

    /** An icon whose color comes from the active palette, e.g. {@code p -> p.accent}. */
    public static FontIcon themed(Symbol symbol, float size, Function<Palette, Color> color) {
        return new FontIcon(symbol.glyph(), size, color);
    }

    /** True when an icon font is installed (Windows 10/11). */
    public static boolean isAvailable() {
        return Fonts.hasIconFont();
    }

    public String getGlyph() {
        return glyph;
    }

    public float getSize() {
        return size;
    }

    public boolean hasOwnColor() {
        return color != null;
    }

    public FontIcon withSize(float newSize) {
        return new FontIcon(glyph, newSize, color);
    }

    public FontIcon withColor(Function<Palette, Color> newColor) {
        return new FontIcon(glyph, size, newColor);
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        paintIcon(c, g, x, y, null);
    }

    /** Paints with {@code override} as color when the icon has no color of its own. */
    public void paintIcon(Component c, Graphics g, int x, int y, Color override) {
        Font font = Fonts.icon(size);
        if (font == null) return;

        Color col = color != null ? color.apply(Tessel.palette())
                : override != null ? override
                : c != null ? c.getForeground()
                : Tessel.palette().text;

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            g2.setFont(font);
            g2.setColor(col);
            FontMetrics fm = g2.getFontMetrics();
            float w = getIconWidth();
            float h = getIconHeight();
            float gx = x + (w - (float) fm.getStringBounds(glyph, g2).getWidth()) / 2f;
            float gy = y + (h - (fm.getAscent() + fm.getDescent())) / 2f + fm.getAscent();
            g2.drawString(glyph, gx, gy);
        } finally {
            g2.dispose();
        }
    }

    @Override
    public int getIconWidth() {
        return Math.round(size);
    }

    @Override
    public int getIconHeight() {
        return Math.round(size);
    }
}
