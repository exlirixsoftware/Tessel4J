package net.tessel.laf.ui;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.LookAndFeel;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicScrollPaneUI;

/** Transparent scroll panes with a comfortable default wheel speed. */
public class TesselScrollPaneUI extends BasicScrollPaneUI {

    public static ComponentUI createUI(JComponent c) {
        return new TesselScrollPaneUI();
    }

    @Override
    protected void installDefaults(JScrollPane pane) {
        super.installDefaults(pane);
        LookAndFeel.installProperty(pane, "opaque", Boolean.FALSE);
        // Only used when the view is not Scrollable (a plain JPanel would otherwise scroll 1px per notch).
        if (pane.getVerticalScrollBar() != null) pane.getVerticalScrollBar().setUnitIncrement(16);
        if (pane.getHorizontalScrollBar() != null) pane.getHorizontalScrollBar().setUnitIncrement(16);
    }
}
