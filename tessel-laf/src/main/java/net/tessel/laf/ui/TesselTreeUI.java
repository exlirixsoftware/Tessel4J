package net.tessel.laf.ui;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JComponent;
import javax.swing.LookAndFeel;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicTreeUI;
import net.tessel.laf.Palette;
import net.tessel.laf.Tessel;

/** Trees with full-row rounded hover/selection highlights and chevron expand icons. */
public class TesselTreeUI extends BasicTreeUI {

    private int rolloverRow = -1;

    private final MouseAdapter rolloverHandler = new MouseAdapter() {
        @Override
        public void mouseMoved(MouseEvent e) {
            int row = tree.getClosestRowForLocation(e.getX(), e.getY());
            Rectangle r = row < 0 ? null : tree.getRowBounds(row);
            if (r == null || e.getY() < r.y || e.getY() >= r.y + r.height) row = -1;
            setRollover(row);
        }

        @Override
        public void mouseExited(MouseEvent e) {
            setRollover(-1);
        }
    };

    public static ComponentUI createUI(JComponent c) {
        return new TesselTreeUI();
    }

    @Override
    protected void installDefaults() {
        super.installDefaults();
        LookAndFeel.installProperty(tree, "rowHeight", 32);
    }

    @Override
    protected void installListeners() {
        super.installListeners();
        tree.addMouseListener(rolloverHandler);
        tree.addMouseMotionListener(rolloverHandler);
    }

    @Override
    protected void uninstallListeners() {
        tree.removeMouseListener(rolloverHandler);
        tree.removeMouseMotionListener(rolloverHandler);
        super.uninstallListeners();
    }

    private void setRollover(int row) {
        if (row == rolloverRow) return;
        repaintRow(rolloverRow);
        rolloverRow = row;
        repaintRow(row);
    }

    private void repaintRow(int row) {
        if (row < 0) return;
        Rectangle r = tree.getRowBounds(row);
        if (r != null) tree.repaint(0, r.y, tree.getWidth(), r.height);
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        Palette p = Tessel.palette();
        Graphics2D g2 = UIUtils.create(g);
        try {
            int w = tree.getWidth();
            if (rolloverRow >= 0 && !tree.isRowSelected(rolloverRow)) {
                Rectangle r = tree.getRowBounds(rolloverRow);
                if (r != null) {
                    g2.setColor(p.controlHover);
                    UIUtils.fillRound(g2, 2, r.y + 1, w - 4, r.height - 2, 4);
                }
            }
            int[] rows = tree.getSelectionRows();
            if (rows != null) {
                for (int row : rows) {
                    Rectangle r = tree.getRowBounds(row);
                    if (r == null) continue;
                    g2.setColor(p.subtle);
                    UIUtils.fillRound(g2, 2, r.y + 1, w - 4, r.height - 2, 4);
                    g2.setColor(p.accent);
                    UIUtils.fillRound(g2, 3, r.y + (r.height - 16) / 2.0, 3, 16, 1.5);
                }
            }
        } finally {
            g2.dispose();
        }
        super.paint(g, c);
    }
}
