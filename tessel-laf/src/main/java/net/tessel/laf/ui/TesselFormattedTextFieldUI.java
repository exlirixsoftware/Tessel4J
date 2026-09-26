package net.tessel.laf.ui;

import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;

/** Formatted text fields share the text field look. */
public class TesselFormattedTextFieldUI extends TesselTextFieldUI {

    public static ComponentUI createUI(JComponent c) {
        return new TesselFormattedTextFieldUI();
    }

    @Override
    protected String getPropertyPrefix() {
        return "FormattedTextField";
    }
}
