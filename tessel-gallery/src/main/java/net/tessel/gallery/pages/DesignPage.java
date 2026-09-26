package net.tessel.gallery.pages;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.util.function.Function;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.tessel.gallery.Layouts;
import net.tessel.gallery.Page;
import net.tessel.laf.Palette;
import net.tessel.laf.Tessel;
import net.tessel.laf.TextStyle;
import net.tessel.laf.ui.UIUtils;

public class DesignPage extends Page {

    public DesignPage() {
        super("Colors & Typography", "Design tokens available from Tessel.palette() and as UIManager keys (\"Tessel.accent\", ...).");

        JPanel type = Layouts.stack(4,
                Tessel.label("Display", TextStyle.DISPLAY),
                Tessel.label("Title", TextStyle.TITLE),
                Tessel.label("Subtitle", TextStyle.SUBTITLE),
                Tessel.label("Header", TextStyle.HEADER),
                Tessel.label("Body strong", TextStyle.BODY_STRONG),
                Tessel.label("Body — The quick brown fox jumps over the lazy dog.", TextStyle.BODY),
                Tessel.label("Secondary — Supporting text with lower emphasis.", TextStyle.SECONDARY),
                Tessel.label("Caption — Small print and metadata.", TextStyle.CAPTION),
                Tessel.label("Code — Palette p = Tessel.palette();", TextStyle.CODE));
        section("Typography", type);

        section("Surfaces & text", grid(
                swatch("background", p -> p.background), swatch("surface", p -> p.surface),
                swatch("surfaceAlt", p -> p.surfaceAlt), swatch("subtle", p -> p.subtle),
                swatch("borderStrong", p -> p.borderStrong), swatch("text", p -> p.text),
                swatch("textSecondary", p -> p.textSecondary), swatch("textTertiary", p -> p.textTertiary),
                swatch("controlHover", p -> p.controlHover), swatch("controlPressed", p -> p.controlPressed)));

        section("Accent & semantic", grid(
                swatch("accent", p -> p.accent), swatch("success", p -> p.success),
                swatch("warning", p -> p.warning), swatch("danger", p -> p.danger),
                swatch("info", p -> p.info), swatch("accentSubtle", p -> p.accentSubtle),
                swatch("successSubtle", p -> p.successSubtle), swatch("warningSubtle", p -> p.warningSubtle),
                swatch("dangerSubtle", p -> p.dangerSubtle), swatch("infoSubtle", p -> p.infoSubtle)));
    }

    private static JPanel grid(JComponent... swatches) {
        JPanel grid = Layouts.panel(new GridLayout(0, 5, 12, 16));
        for (JComponent s : swatches) grid.add(s);
        return grid;
    }

    private static JComponent swatch(String name, Function<Palette, Color> color) {
        JComponent chip = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Palette p = Tessel.palette();
                Graphics2D g2 = UIUtils.create(g);
                try {
                    g2.setColor(color.apply(p));
                    UIUtils.fillRound(g2, 0, 0, getWidth(), getHeight(), 6);
                    g2.setColor(p.border);
                    UIUtils.strokeRound(g2, 0, 0, getWidth(), getHeight(), 6, 1f);
                } finally {
                    g2.dispose();
                }
            }
        };
        chip.setPreferredSize(new Dimension(100, 56));
        JLabel label = Tessel.label(name, TextStyle.CAPTION);
        label.setFont(net.tessel.laf.Fonts.mono(12));
        JPanel cell = Layouts.panel(new BorderLayout(0, 6));
        cell.add(chip, BorderLayout.CENTER);
        cell.add(label, BorderLayout.SOUTH);
        return cell;
    }
}
