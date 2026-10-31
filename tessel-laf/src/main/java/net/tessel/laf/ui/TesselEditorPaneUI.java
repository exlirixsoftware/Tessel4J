package net.tessel.laf.ui;

import java.awt.Graphics;
import java.beans.PropertyChangeEvent;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicEditorPaneUI;

/**
 * Themed {@code JEditorPane}: placeholder text, an HTML style sheet that follows the palette, undo and redo,
 * a context menu and the formatting shortcuts (Ctrl+B / Ctrl+I / Ctrl+U). Put it in a scroll pane for the
 * rounded outline. See {@link net.tessel.laf.RichText} for the API.
 */
public class TesselEditorPaneUI extends BasicEditorPaneUI implements RichTextSupport.ViewHost {

    public static ComponentUI createUI(JComponent c) {
        return new TesselEditorPaneUI();
    }

    @Override
    protected void installDefaults() {
        super.installDefaults();
        TextSupport.installDefaults(getComponent());
        RichTextSupport.installDefaults((JEditorPane) getComponent());
    }

    @Override
    protected void installListeners() {
        super.installListeners();
        RichTextSupport.installListeners((JEditorPane) getComponent());
    }

    @Override
    protected void uninstallListeners() {
        RichTextSupport.uninstallListeners((JEditorPane) getComponent());
        super.uninstallListeners();
    }

    @Override
    protected void installKeyboardActions() {
        super.installKeyboardActions();
        RichTextSupport.installKeyboardActions((JEditorPane) getComponent());
    }

    @Override
    public void rebuildViews() {
        modelChanged();
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
        RichTextSupport.propertyChange(evt, (JEditorPane) getComponent(), this);
    }
}
