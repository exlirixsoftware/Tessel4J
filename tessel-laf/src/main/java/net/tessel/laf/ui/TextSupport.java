package net.tessel.laf.ui;

import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import javax.swing.LookAndFeel;
import javax.swing.plaf.basic.BasicGraphicsUtils;
import javax.swing.plaf.basic.BasicTextUI;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import javax.swing.text.Position;
import net.tessel.laf.Tessel;

/** Behaviour shared by the Tessel text component delegates. */
final class TextSupport {

    private TextSupport() {
    }

    static void installDefaults(JTextComponent c) {
        if (c.getBorder() instanceof TesselInputBorder) {
            LookAndFeel.installProperty(c, "opaque", Boolean.FALSE);
        }
    }

    static void installListeners(JTextComponent c) {
        UIUtils.installHover(c);
    }

    static void uninstallListeners(JTextComponent c) {
        UIUtils.uninstallHover(c);
    }

    /** Rounded background for components with the Tessel input border. */
    static void paintBackground(Graphics g, JTextComponent c) {
        if (c.isOpaque() || !(c.getBorder() instanceof TesselInputBorder)) return;
        Graphics2D g2 = UIUtils.create(g);
        try {
            g2.setColor(c.getBackground());
            UIUtils.fillRound(g2, 0, 0, c.getWidth(), c.getHeight(), TesselInputBorder.RADIUS);
        } finally {
            g2.dispose();
        }
    }

    static void paintPlaceholder(Graphics g, JTextComponent c, BasicTextUI ui) {
        Object text = c.getClientProperty(Tessel.PLACEHOLDER);
        if (!(text instanceof String) || ((String) text).isEmpty() || c.getDocument().getLength() > 0) return;
        Rectangle2D r;
        try {
            r = ui.modelToView2D(c, 0, Position.Bias.Forward);
        } catch (BadLocationException | RuntimeException e) {
            return;
        }
        if (r == null) return;

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            Insets in = c.getInsets();
            g2.clipRect(in.left, in.top, c.getWidth() - in.left - in.right, c.getHeight() - in.top - in.bottom);
            g2.setFont(c.getFont());
            g2.setColor(Tessel.palette().textTertiary);
            FontMetrics fm = c.getFontMetrics(c.getFont());
            float y = (float) (r.getY() + (r.getHeight() - fm.getHeight()) / 2.0 + fm.getAscent());
            BasicGraphicsUtils.drawString(c, g2, (String) text, (float) r.getX(), y);
        } finally {
            g2.dispose();
        }
    }

    static void propertyChange(PropertyChangeEvent e, JTextComponent c) {
        String name = e.getPropertyName();
        if (Tessel.PLACEHOLDER.equals(name)) {
            c.repaint();
        } else if (Tessel.LEADING_ICON.equals(name)) {
            c.revalidate();
            c.repaint();
        } else if ("editable".equals(name) || "enabled".equals(name)) {
            c.repaint();
        }
    }
}
