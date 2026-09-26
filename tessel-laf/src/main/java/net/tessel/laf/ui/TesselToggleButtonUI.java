package net.tessel.laf.ui;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicToggleButtonUI;

/** Toggle buttons: standard look when off, accent when on. */
public class TesselToggleButtonUI extends BasicToggleButtonUI {

    private ButtonPainter.Colors colors;

    public static ComponentUI createUI(JComponent c) {
        return new TesselToggleButtonUI();
    }

    @Override
    protected void installDefaults(AbstractButton b) {
        super.installDefaults(b);
        ButtonPainter.installDefaults(b);
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        AbstractButton b = (AbstractButton) c;
        Graphics2D g2 = ButtonPainter.begin(g, c);
        try {
            colors = ButtonPainter.colors(b);
            ButtonPainter.paintBackground(g2, b, colors);
            super.paint(g2, c);
        } finally {
            g2.dispose();
        }
    }

    @Override
    protected void paintButtonPressed(Graphics g, AbstractButton b) {
        // Pressed and selected states are part of paintBackground.
    }

    @Override
    protected void paintIcon(Graphics g, AbstractButton b, Rectangle iconRect) {
        ButtonPainter.paintIcon(g, b, iconRect, colors);
    }

    @Override
    protected void paintText(Graphics g, AbstractButton b, Rectangle textRect, String text) {
        ButtonPainter.paintText(g, b, textRect, text, colors, getTextShiftOffset());
    }

    @Override
    protected void paintFocus(Graphics g, AbstractButton b, Rectangle viewRect, Rectangle textRect, Rectangle iconRect) {
        // Focus ring is part of paintBackground (keyboard focus only).
    }

    @Override
    public Dimension getPreferredSize(JComponent c) {
        return ButtonPainter.preferredSize((AbstractButton) c, super.getPreferredSize(c));
    }
}
