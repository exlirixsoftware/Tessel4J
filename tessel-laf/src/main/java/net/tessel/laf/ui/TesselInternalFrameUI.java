package net.tessel.laf.ui;

import java.beans.PropertyChangeListener;
import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.LookAndFeel;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicInternalFrameUI;

/** Internal frames with a rounded, softly shadowed surface and a Fluent-style title bar. */
public class TesselInternalFrameUI extends BasicInternalFrameUI {

    public TesselInternalFrameUI(JInternalFrame frame) {
        super(frame);
    }

    public static ComponentUI createUI(JComponent c) {
        return new TesselInternalFrameUI((JInternalFrame) c);
    }

    @Override
    public void installUI(JComponent c) {
        super.installUI(c);
        // The rounded surface comes from the border; BasicInternalFrameUI forces opaque after installDefaults.
        LookAndFeel.installProperty(c, "opaque", Boolean.FALSE);
    }

    @Override
    protected javax.swing.JComponent createNorthPane(JInternalFrame w) {
        titlePane = new TesselInternalFrameTitlePane(w);
        return titlePane;
    }

    @Override
    protected PropertyChangeListener createPropertyChangeListener() {
        PropertyChangeListener base = super.createPropertyChangeListener();
        return e -> {
            base.propertyChange(e);
            if (JInternalFrame.IS_SELECTED_PROPERTY.equals(e.getPropertyName())) frame.repaint();
        };
    }
}
