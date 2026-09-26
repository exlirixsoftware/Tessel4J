package net.tessel.laf.ui;

import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JComponent;
import javax.swing.JSplitPane;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import net.tessel.laf.Tessel;

/** Split panes with a thin line divider that turns accent-colored on hover. */
public class TesselSplitPaneUI extends BasicSplitPaneUI {

    public static ComponentUI createUI(JComponent c) {
        return new TesselSplitPaneUI();
    }

    @Override
    public BasicSplitPaneDivider createDefaultDivider() {
        return new LineDivider(this);
    }

    private static final class LineDivider extends BasicSplitPaneDivider {
        private boolean hover;

        LineDivider(BasicSplitPaneUI ui) {
            super(ui);
            setBorder(null);
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }
            });
        }

        @Override
        public void paint(Graphics g) {
            int size = hover ? 2 : 1;
            g.setColor(hover ? Tessel.palette().accent : Tessel.palette().border);
            if (orientation == JSplitPane.HORIZONTAL_SPLIT) g.fillRect((getWidth() - size) / 2, 0, size, getHeight());
            else g.fillRect(0, (getHeight() - size) / 2, getWidth(), size);
            super.paint(g);
        }
    }
}
