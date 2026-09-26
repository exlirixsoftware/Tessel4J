package net.tessel.laf.ui;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicButtonUI;

/** Rounded buttons with the {@link net.tessel.laf.ButtonStyle} variants. */
public class TesselButtonUI extends BasicButtonUI {

    private ButtonPainter.Colors colors;

    public static ComponentUI createUI(JComponent c) {
        return new TesselButtonUI();
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
        // Pressed state is part of paintBackground.
    }

    @Override
    protected void paintIcon(Graphics g, JComponent c, Rectangle iconRect) {
        ButtonPainter.paintIcon(g, (AbstractButton) c, iconRect, colors);
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
