package net.tessel.laf.ui;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.LookAndFeel;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicCheckBoxUI;

/** Check boxes with a rounded accent box; supports {@link net.tessel.laf.Tessel#INDETERMINATE}. */
public class TesselCheckBoxUI extends BasicCheckBoxUI {

    public static ComponentUI createUI(JComponent c) {
        return new TesselCheckBoxUI();
    }

    @Override
    protected void installDefaults(AbstractButton b) {
        super.installDefaults(b);
        LookAndFeel.installProperty(b, "opaque", Boolean.FALSE);
        LookAndFeel.installProperty(b, "rolloverEnabled", Boolean.TRUE);
        LookAndFeel.installProperty(b, "iconTextGap", 10);
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        Graphics2D g2 = ButtonPainter.begin(g, c);
        try {
            super.paint(g2, c);
        } finally {
            g2.dispose();
        }
    }

    @Override
    protected void paintText(Graphics g, AbstractButton b, Rectangle textRect, String text) {
        ButtonPainter.paintText(g, b, textRect, text, null, getTextShiftOffset());
    }

    @Override
    protected void paintFocus(Graphics g, Rectangle textRect, java.awt.Dimension size) {
        // The icon draws the focus ring.
    }
}
