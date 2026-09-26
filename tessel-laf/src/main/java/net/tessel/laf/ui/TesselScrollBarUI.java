package net.tessel.laf.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.LookAndFeel;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicScrollBarUI;
import net.tessel.laf.Palette;
import net.tessel.laf.Tessel;

/** Thin scroll bars with a rounded thumb and no arrow buttons. */
public class TesselScrollBarUI extends BasicScrollBarUI {

    public static ComponentUI createUI(JComponent c) {
        return new TesselScrollBarUI();
    }

    @Override
    protected void installDefaults() {
        super.installDefaults();
        LookAndFeel.installProperty(scrollbar, "opaque", Boolean.FALSE);
    }

    @Override
    protected JButton createDecreaseButton(int orientation) {
        return new ZeroButton();
    }

    @Override
    protected JButton createIncreaseButton(int orientation) {
        return new ZeroButton();
    }

    @Override
    protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
        // Transparent track.
    }

    @Override
    protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
        if (r.isEmpty() || !scrollbar.isEnabled()) return;
        Palette p = Tessel.palette();
        int alpha = isDragging ? 255 : isThumbRollover() ? 210 : 130;
        Graphics2D g2 = UIUtils.create(g);
        try {
            g2.setColor(new Color(p.textTertiary.getRed(), p.textTertiary.getGreen(), p.textTertiary.getBlue(), alpha));
            if (scrollbar.getOrientation() == JScrollBar.VERTICAL) {
                UIUtils.fillRound(g2, r.x + 3, r.y + 2, r.width - 6, r.height - 4, 3);
            } else {
                UIUtils.fillRound(g2, r.x + 2, r.y + 3, r.width - 4, r.height - 6, 3);
            }
        } finally {
            g2.dispose();
        }
    }

    @Override
    protected Dimension getMinimumThumbSize() {
        return scrollbar.getOrientation() == JScrollBar.VERTICAL ? new Dimension(12, 32) : new Dimension(32, 12);
    }

    private static final class ZeroButton extends JButton {
        ZeroButton() {
            setFocusable(false);
            setRequestFocusEnabled(false);
            setBorder(null);
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(0, 0);
        }

        @Override
        public Dimension getMinimumSize() {
            return new Dimension(0, 0);
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(0, 0);
        }
    }
}
