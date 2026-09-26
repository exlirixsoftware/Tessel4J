package net.tessel.laf.ui;

import java.awt.Graphics;
import java.beans.PropertyChangeEvent;
import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicPasswordFieldUI;

/** Password fields share the text field look. */
public class TesselPasswordFieldUI extends BasicPasswordFieldUI {

    public static ComponentUI createUI(JComponent c) {
        return new TesselPasswordFieldUI();
    }

    @Override
    protected void installDefaults() {
        super.installDefaults();
        TextSupport.installDefaults(getComponent());
    }

    @Override
    protected void installListeners() {
        super.installListeners();
        TextSupport.installListeners(getComponent());
    }

    @Override
    protected void uninstallListeners() {
        TextSupport.uninstallListeners(getComponent());
        super.uninstallListeners();
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
