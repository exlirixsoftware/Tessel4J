package net.tessel.laf.ui;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JComponent;
import javax.swing.JSeparator;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicToolBarSeparatorUI;
import net.tessel.laf.Tessel;

/** A thin divider line between tool bar groups. */
public class TesselToolBarSeparatorUI extends BasicToolBarSeparatorUI {

    public static ComponentUI createUI(JComponent c) {
        return new TesselToolBarSeparatorUI();
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        Graphics2D g2 = UIUtils.create(g);
        try {
            g2.setColor(Tessel.palette().border);
            if (((JSeparator) c).getOrientation() == JSeparator.VERTICAL) {
                g2.fillRect(c.getWidth() / 2, 5, 1, Math.max(0, c.getHeight() - 10));
            } else {
                g2.fillRect(5, c.getHeight() / 2, Math.max(0, c.getWidth() - 10), 1);
            }
        } finally {
            g2.dispose();
        }
    }

    @Override
    public Dimension getPreferredSize(JComponent c) {
        Dimension d = super.getPreferredSize(c);
        return d != null ? d : new Dimension(9, 24);
    }
}
