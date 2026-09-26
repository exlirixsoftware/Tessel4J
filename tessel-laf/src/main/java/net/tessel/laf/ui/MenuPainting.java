package net.tessel.laf.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.ButtonModel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

/** Rounded highlight shared by the menu item delegates. */
final class MenuPainting {

    private MenuPainting() {
    }

    /** Gap between icon and text, and between text and accelerator. */
    static final int TEXT_ICON_GAP = 12;

    static void paintBackground(Graphics g, JMenuItem item, Color selection) {
        ButtonModel m = item.getModel();
        int w = item.getWidth();
        int h = item.getHeight();
        if (item.isOpaque()) {
            g.setColor(item.getBackground());
            g.fillRect(0, 0, w, h);
        }
        boolean armed = m.isArmed() || (item instanceof JMenu && m.isSelected());
        if (!armed) return;
        Graphics2D g2 = UIUtils.create(g);
        try {
            g2.setColor(selection);
            boolean topLevel = item instanceof JMenu && ((JMenu) item).isTopLevelMenu();
            if (topLevel) UIUtils.fillRound(g2, 2, 3, w - 4, h - 6, 4);
            else UIUtils.fillRound(g2, 0, 1, w, h - 2, 4);
        } finally {
            g2.dispose();
        }
    }
}
