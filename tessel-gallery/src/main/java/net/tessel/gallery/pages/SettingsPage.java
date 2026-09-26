package net.tessel.gallery.pages;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.JToggleButton;
import net.tessel.gallery.Layouts;
import net.tessel.gallery.Page;
import net.tessel.laf.ButtonStyle;
import net.tessel.laf.Palette;
import net.tessel.laf.Severity;
import net.tessel.laf.Tessel;
import net.tessel.laf.TesselTheme;
import net.tessel.laf.TextStyle;
import net.tessel.laf.components.Badge;
import net.tessel.laf.components.Card;
import net.tessel.laf.components.SettingsCard;
import net.tessel.laf.components.Snackbar;
import net.tessel.laf.components.ToggleSwitch;
import net.tessel.laf.icons.Symbol;
import net.tessel.laf.ui.UIUtils;

public class SettingsPage extends Page {

    private static final int[] ACCENTS = {
            0x4F46E5, 0x0078D4, 0x0891B2, 0x059669, 0x65A30D, 0xD97706, 0xEA580C, 0xDC2626, 0xDB2777, 0x9333EA, 0x475569,
    };
    private static final String[] ACCENT_NAMES = {
            "Default (Indigo)", "Blue", "Cyan", "Emerald", "Lime", "Amber", "Orange", "Red", "Pink", "Purple", "Slate",
    };

    private final JRadioButton light = new JRadioButton("Light");
    private final JRadioButton dark = new JRadioButton("Dark");
    private final JRadioButton system = new JRadioButton("System");
    private final ButtonGroup accentGroup = new ButtonGroup();
    private boolean syncing;

    public SettingsPage() {
        super("Settings", "Theme and accent color are applied app-wide at runtime through Tessel.setTheme / Tessel.setAccent.");

        ButtonGroup themes = new ButtonGroup();
        themes.add(light);
        themes.add(dark);
        themes.add(system);
        light.addActionListener(e -> applyTheme(TesselTheme.LIGHT));
        dark.addActionListener(e -> applyTheme(TesselTheme.DARK));
        system.addActionListener(e -> applyTheme(TesselTheme.SYSTEM));
        syncThemeRadios();
        Tessel.addThemeListener(this::syncThemeRadios);

        add(Tessel.label("Appearance", TextStyle.BODY_STRONG));
        JPanel themeRow = Layouts.row(light, dark, system);
        themeRow.setPreferredSize(null);
        add(new SettingsCard("App theme", "Choose light, dark, or follow your system setting", Symbol.BRIGHTNESS, themeRow));

        JButton windowsAccent = new JButton("Use Windows accent");
        windowsAccent.addActionListener(e -> {
            Color c = Tessel.getSystemAccent();
            if (c == null) {
                Snackbar.show(this, "No system accent color found", null, Severity.WARNING);
                return;
            }
            accentGroup.clearSelection();
            Tessel.setAccent(c);
        });
        add(new SettingsCard("Accent color", "Used for primary buttons, selection and focus", Symbol.COLOR, windowsAccent));

        JPanel swatches = Layouts.row();
        for (int i = 0; i < ACCENTS.length; i++) {
            AccentSwatch s = new AccentSwatch(new Color(ACCENTS[i]), i == 0);
            s.setToolTipText(ACCENT_NAMES[i]);
            accentGroup.add(s);
            swatches.add(s);
        }
        Card swatchCard = new Card(null, swatches);
        swatchCard.setPadding(14, 60, 14, 16);
        add(swatchCard);

        add(Tessel.label("Preview", TextStyle.BODY_STRONG));
        JProgressBar progress = new JProgressBar(0, 100);
        progress.setValue(65);
        add(new Card(null, Layouts.stack(14,
                Layouts.row(Tessel.style(new JButton("Primary"), ButtonStyle.ACCENT), Tessel.style(new JButton("Outline"), ButtonStyle.OUTLINE),
                        new JCheckBox("Checked", true), new ToggleSwitch("On", true)),
                progress,
                new JSlider(0, 10, 4))));

        add(Tessel.label("About", TextStyle.BODY_STRONG));
        add(new SettingsCard("Tessel " + Tessel.VERSION, "A modern look and feel for Java Swing (Java 17+)", Symbol.INFO,
                new Badge("MIT", Badge.Kind.INFO)));

        for (java.awt.Component c : getComponents()) {
            if (c instanceof javax.swing.JLabel) ((javax.swing.JLabel) c).setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        }
    }

    private void applyTheme(TesselTheme theme) {
        if (!syncing) Tessel.setTheme(theme);
    }

    private void syncThemeRadios() {
        syncing = true;
        switch (Tessel.getTheme()) {
            case LIGHT: light.setSelected(true); break;
            case DARK: dark.setSelected(true); break;
            default: system.setSelected(true); break;
        }
        syncing = false;
    }

    /** A round color swatch that sets the accent when selected. */
    private static final class AccentSwatch extends JToggleButton {
        private final Color color;
        private final boolean builtIn;

        AccentSwatch(Color color, boolean builtIn) {
            this.color = color;
            this.builtIn = builtIn;
            setSelected(builtIn && Tessel.getAccent() == null);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addActionListener(e -> Tessel.setAccent(builtIn ? null : color));
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(36, 36);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Palette p = Tessel.palette();
            Graphics2D g2 = UIUtils.create(g);
            try {
                if (isSelected() || getModel().isRollover()) {
                    g2.setColor(isSelected() ? p.text : Palette.withAlpha(p.text, 100));
                    g2.setStroke(new java.awt.BasicStroke(2f));
                    g2.draw(new Ellipse2D.Double(1, 1, 34, 34));
                }
                g2.setColor(color);
                g2.fill(new Ellipse2D.Double(5, 5, 26, 26));
                if (isSelected()) {
                    g2.setColor(Color.WHITE);
                    g2.setStroke(new java.awt.BasicStroke(1.8f, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
                    Path2D check = new Path2D.Double();
                    check.moveTo(13, 18.5);
                    check.lineTo(16.5, 22);
                    check.lineTo(23, 14.5);
                    g2.draw(check);
                }
            } finally {
                g2.dispose();
            }
        }
    }
}
