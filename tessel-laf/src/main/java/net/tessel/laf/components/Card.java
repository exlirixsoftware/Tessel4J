package net.tessel.laf.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.AbstractBorder;
import javax.swing.plaf.UIResource;
import net.tessel.laf.Palette;
import net.tessel.laf.Tessel;
import net.tessel.laf.TextStyle;
import net.tessel.laf.ui.UIUtils;

/**
 * A rounded surface that groups related content, with an optional header.
 * Uses a {@link BorderLayout}; {@link #setContent(Component)} places a component in the center.
 */
public class Card extends JPanel {

    static final int SHADOW = 6;

    private JLabel headerLabel;
    private boolean elevated;
    private int radius = 8;
    private Insets padding = new Insets(20, 20, 20, 20);

    public Card() {
        super(new BorderLayout());
        setOpaque(false);
        setBorder(new CardBorder());
    }

    public Card(String header) {
        this();
        setHeader(header);
    }

    public Card(String header, Component content) {
        this(header);
        setContent(content);
    }

    public void setContent(Component content) {
        add(content, BorderLayout.CENTER);
        revalidate();
    }

    public String getHeader() {
        return headerLabel == null ? null : headerLabel.getText();
    }

    public void setHeader(String header) {
        if (header == null) {
            if (headerLabel != null) remove(headerLabel);
            headerLabel = null;
        } else {
            if (headerLabel == null) {
                headerLabel = Tessel.label(header, TextStyle.HEADER);
                headerLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 14, 0));
                add(headerLabel, BorderLayout.NORTH);
            }
            headerLabel.setText(header);
        }
        revalidate();
        repaint();
    }

    public boolean isElevated() {
        return elevated;
    }

    /** Adds a soft drop shadow (reserves a few pixels around the card). */
    public void setElevated(boolean elevated) {
        this.elevated = elevated;
        revalidate();
        repaint();
    }

    public int getCornerRadius() {
        return radius;
    }

    public void setCornerRadius(int radius) {
        this.radius = radius;
        repaint();
    }

    public Insets getPadding() {
        return (Insets) padding.clone();
    }

    public void setPadding(Insets padding) {
        this.padding = (Insets) padding.clone();
        revalidate();
        repaint();
    }

    public void setPadding(int top, int left, int bottom, int right) {
        setPadding(new Insets(top, left, bottom, right));
    }

    /** The margin reserved for the shadow on each side. */
    Insets shadowInsets() {
        return elevated ? new Insets(SHADOW - 2, SHADOW, SHADOW + 2, SHADOW) : new Insets(0, 0, 0, 0);
    }

    Color surfaceColor() {
        Color bg = getBackground();
        return bg == null || bg instanceof UIResource ? Tessel.palette().surface : bg;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Insets s = shadowInsets();
        double x = s.left;
        double y = s.top;
        double w = getWidth() - s.left - s.right;
        double h = getHeight() - s.top - s.bottom;
        Graphics2D g2 = UIUtils.create(g);
        try {
            if (elevated) UIUtils.paintShadow(g2, x, y, w, h, radius, SHADOW - 1);
            g2.setColor(surfaceColor());
            UIUtils.fillRound(g2, x, y, w, h, radius);
        } finally {
            g2.dispose();
        }
    }

    /** Outline + padding (+ shadow margin). */
    private final class CardBorder extends AbstractBorder {
        @Override
        public Insets getBorderInsets(Component c, Insets i) {
            Insets s = shadowInsets();
            i.set(s.top + padding.top + 1, s.left + padding.left + 1, s.bottom + padding.bottom + 1, s.right + padding.right + 1);
            return i;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Insets s = shadowInsets();
            Palette p = Tessel.palette();
            Graphics2D g2 = UIUtils.create(g);
            try {
                g2.setColor(p.border);
                UIUtils.strokeRound(g2, x + s.left, y + s.top, w - s.left - s.right, h - s.top - s.bottom, radius, 1f);
            } finally {
                g2.dispose();
            }
        }
    }

    /** Convenience: a card around a component with no padding (e.g. media content). */
    public static Card wrap(JComponent content) {
        Card card = new Card();
        card.setPadding(0, 0, 0, 0);
        card.setContent(content);
        return card;
    }
}
