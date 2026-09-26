package net.tessel.gallery.pages;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.tessel.gallery.GalleryFrame;
import net.tessel.gallery.Layouts;
import net.tessel.gallery.Page;
import net.tessel.laf.Fonts;
import net.tessel.laf.Palette;
import net.tessel.laf.Tessel;
import net.tessel.laf.TextStyle;
import net.tessel.laf.components.Card;
import net.tessel.laf.components.TextBlock;
import net.tessel.laf.icons.FontIcon;
import net.tessel.laf.icons.Symbol;
import net.tessel.laf.ui.UIUtils;

public class HomePage extends Page {

    public HomePage(GalleryFrame frame) {
        super("Welcome", null);
        remove(0); // the hero replaces the page header

        add(hero(frame));

        add(Layouts.columns(16,
                feature(Symbol.COLOR, "Theming", "Light, dark or system theme with any accent color, switchable at runtime."),
                feature(Symbol.VIEW_ALL, "40+ styled components", "Every standard Swing control restyled - install one look and feel."),
                feature(Symbol.CODE, "Drop-in", "Pure Java 17+, no dependencies. Works with any Swing application.")));

        JPanel started = Layouts.stack(8,
                new TextBlock("1. Add tessel-laf-1.0.0.jar to your classpath and install the look and feel on the EDT:", TextStyle.SECONDARY),
                code("SwingUtilities.invokeLater(() -> {\n"
                        + "    Tessel.setup(TesselTheme.SYSTEM);\n"
                        + "    new MyFrame().setVisible(true);\n"
                        + "});"),
                new TextBlock("2. Optionally extend TesselFrame for the custom title bar, and switch themes at runtime:", TextStyle.SECONDARY),
                code("public class MyFrame extends TesselFrame { ... }\n\n"
                        + "Tessel.setTheme(TesselTheme.DARK);\n"
                        + "Tessel.setAccent(new Color(0x0078D4));"));
        section("Getting started", started);
    }

    private static JComponent hero(GalleryFrame frame) {
        JPanel hero = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Palette p = Tessel.palette();
                Graphics2D g2 = UIUtils.create(g);
                try {
                    g2.setPaint(new GradientPaint(0, 0, p.accent, getWidth(), getHeight(), new Color(0x7C3AED)));
                    UIUtils.fillRound(g2, 0, 0, getWidth(), getHeight(), 12);
                } finally {
                    g2.dispose();
                }
            }
        };
        hero.setOpaque(false);
        hero.setBorder(BorderFactory.createEmptyBorder(32, 36, 32, 36));

        JLabel eyebrow = new JLabel("TESSEL FOR JAVA SWING");
        eyebrow.setFont(Fonts.semibold(13));
        eyebrow.setForeground(new Color(255, 255, 255, 204));

        JLabel title = new JLabel("Build beautiful desktop apps.");
        title.setFont(Fonts.forStyle(TextStyle.DISPLAY));
        title.setForeground(Color.WHITE);

        TextBlock text = new TextBlock("A look and feel with light & dark themes, custom accent colors, restyled standard "
                + "controls and modern components - windows, navigation, dialogs, notifications and more.", TextStyle.BODY);
        text.setFont(Fonts.regular(15));
        text.setForeground(new Color(255, 255, 255, 230));

        JButton explore = new JButton("Explore controls", new FontIcon(Symbol.FORWARD, 16));
        explore.setBackground(Color.WHITE);
        explore.setForeground(new Color(0x1B1F24));
        explore.addActionListener(e -> frame.navigateTo("Buttons"));

        JButton settings = new JButton("Theme settings", new FontIcon(Symbol.COLOR, 16));
        settings.setBackground(new Color(255, 255, 255, 38));
        settings.setForeground(Color.WHITE);
        settings.addActionListener(e -> frame.navigateTo("Settings"));

        JPanel buttons = Layouts.row(explore, settings);
        buttons.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));

        JPanel content = Layouts.stack(8, eyebrow, title, text, buttons);
        hero.add(content, BorderLayout.CENTER);
        return hero;
    }

    private static JComponent feature(Symbol symbol, String title, String text) {
        JLabel icon = new JLabel(FontIcon.themed(symbol, 24, p -> p.accent));
        JPanel body = Layouts.stack(4, icon, Tessel.label(title, TextStyle.BODY_STRONG), new TextBlock(text, TextStyle.SECONDARY));
        icon.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        Card card = new Card(null, body);
        card.setElevated(true);
        return card;
    }

    static JComponent code(String source) {
        JPanel box = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = UIUtils.create(g);
                try {
                    g2.setColor(Tessel.palette().subtle);
                    UIUtils.fillRound(g2, 0, 0, getWidth(), getHeight(), 6);
                } finally {
                    g2.dispose();
                }
            }
        };
        box.setOpaque(false);
        box.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        TextBlock code = new TextBlock(source, TextStyle.CODE);
        code.setLineWrap(false);
        box.add(code);
        return box;
    }
}
