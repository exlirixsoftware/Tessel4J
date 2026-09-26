package net.tessel.laf.components;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.plaf.FontUIResource;
import net.tessel.laf.Fonts;
import net.tessel.laf.Palette;
import net.tessel.laf.Tessel;
import net.tessel.laf.ui.UIUtils;

/** A small pill-shaped status label ("New", "Beta", "3"). */
public class Badge extends JLabel {

    public enum Kind { NEUTRAL, ACCENT, SUCCESS, WARNING, DANGER, INFO }

    private Kind kind;
    private boolean solid;

    public Badge(String text) {
        this(text, Kind.NEUTRAL);
    }

    public Badge(String text, Kind kind) {
        this(text, kind, false);
    }

    public Badge(String text, Kind kind, boolean solid) {
        super(text, SwingConstants.CENTER);
        this.kind = kind;
        this.solid = solid;
        setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
        setOpaque(false);
    }

    @Override
    public void updateUI() {
        super.updateUI();
        setFont(new FontUIResource(Fonts.semibold(12)));
    }

    public Kind getKind() {
        return kind;
    }

    public void setKind(Kind kind) {
        this.kind = kind;
        repaint();
    }

    public boolean isSolid() {
        return solid;
    }

    public void setSolid(boolean solid) {
        this.solid = solid;
        repaint();
    }

    private Color strong(Palette p) {
        switch (kind) {
            case ACCENT: return p.accent;
            case SUCCESS: return p.success;
            case WARNING: return p.warning;
            case DANGER: return p.danger;
            case INFO: return p.info;
            default: return p.textSecondary;
        }
    }

    private Color soft(Palette p) {
        switch (kind) {
            case ACCENT: return p.accentSubtle;
            case SUCCESS: return p.successSubtle;
            case WARNING: return p.warningSubtle;
            case DANGER: return p.dangerSubtle;
            case INFO: return p.infoSubtle;
            default: return p.subtle;
        }
    }

    @Override
    public Color getForeground() {
        if (kind == null) return super.getForeground();
        Palette p = Tessel.palette();
        if (!solid) return strong(p);
        if (kind == Kind.ACCENT) return p.onAccent;
        if (kind == Kind.DANGER) return Color.WHITE;
        return p.surface;
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Palette p = Tessel.palette();
        Graphics2D g2 = UIUtils.create(g);
        try {
            g2.setColor(solid ? strong(p) : soft(p));
            UIUtils.fillRound(g2, 0, 0, getWidth(), getHeight(), getHeight() / 2.0);
        } finally {
            g2.dispose();
        }
        super.paintComponent(g);
    }
}
