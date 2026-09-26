package net.tessel.laf.ui;

import java.awt.Dimension;
import javax.swing.JComponent;
import javax.swing.LookAndFeel;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicTableUI;

/** Tables with 36px rows, horizontal grid lines only and accent-tinted selection. */
public class TesselTableUI extends BasicTableUI {

    public static ComponentUI createUI(JComponent c) {
        return new TesselTableUI();
    }

    @Override
    protected void installDefaults() {
        super.installDefaults();
        LookAndFeel.installProperty(table, "rowHeight", 36);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setFillsViewportHeight(true);
    }
}
