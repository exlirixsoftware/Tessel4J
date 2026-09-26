package net.tessel.laf.ui;

import java.awt.Dimension;
import java.awt.Graphics;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicSeparatorUI;
import net.tessel.laf.Tessel;

/** A single 1px line; used for plain separators and popup menu separators. */
public class TesselSeparatorUI extends BasicSeparatorUI {

    public static ComponentUI createUI(JComponent c) {
        return new TesselSeparatorUI();
    }

    private static boolean inPopup(JComponent c) {
        return c instanceof JPopupMenu.Separator;
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        JSeparator s = (JSeparator) c;
        int inset = inPopup(c) ? 4 : 0;
        g.setColor(Tessel.palette().border);
        if (s.getOrientation() == SwingConstants.VERTICAL) {
            g.fillRect(c.getWidth() / 2, inset, 1, c.getHeight() - 2 * inset);
        } else {
            g.fillRect(inset, c.getHeight() / 2, c.getWidth() - 2 * inset, 1);
        }
    }

    @Override
    public Dimension getPreferredSize(JComponent c) {
        int size = inPopup(c) ? 9 : 17;
        return ((JSeparator) c).getOrientation() == SwingConstants.VERTICAL ? new Dimension(size, 0) : new Dimension(0, size);
    }

    @Override
    public Dimension getMaximumSize(JComponent c) {
        Dimension d = getPreferredSize(c);
        return ((JSeparator) c).getOrientation() == SwingConstants.VERTICAL
                ? new Dimension(d.width, Integer.MAX_VALUE) : new Dimension(Integer.MAX_VALUE, d.height);
    }
}
