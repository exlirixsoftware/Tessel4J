package net.tessel.laf.ui;

import java.awt.Graphics;
import java.beans.PropertyChangeEvent;
import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicTextAreaUI;

/** Text areas with placeholder support. Put them in a scroll pane for the rounded outline. */
public class TesselTextAreaUI extends BasicTextAreaUI {

    public static ComponentUI createUI(JComponent c) {
        return new TesselTextAreaUI();
    }

    @Override
    protected void installDefaults() {
        super.installDefaults();
        TextSupport.installDefaults(getComponent());
    }

    @Override
    protected void paintSafely(Graphics g) {
        TextSupport.paintBackground(g, getComponent());
        // Opaque components fill their background in super.paintSafely, so draw the hint afterwards.
        boolean opaque = getComponent().isOpaque();
        if (!opaque) TextSupport.paintPlaceholder(g, getComponent(), this);
        super.paintSafely(g);
        if (opaque) TextSupport.paintPlaceholder(g, getComponent(), this);
    }

    @Override
    protected void propertyChange(PropertyChangeEvent evt) {
        super.propertyChange(evt);
        TextSupport.propertyChange(evt, getComponent());
    }
}
