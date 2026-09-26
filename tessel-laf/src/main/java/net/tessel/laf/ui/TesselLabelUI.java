package net.tessel.laf.ui;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.beans.PropertyChangeEvent;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicGraphicsUtils;
import javax.swing.plaf.basic.BasicLabelUI;
import net.tessel.laf.Fonts;
import net.tessel.laf.Tessel;
import net.tessel.laf.TextStyle;

/** Labels that honour {@link Tessel#TEXT_STYLE} and draw disabled text flat. */
public class TesselLabelUI extends BasicLabelUI {

    public static ComponentUI createUI(JComponent c) {
        return new TesselLabelUI();
    }

    @Override
    protected void installDefaults(JLabel c) {
        super.installDefaults(c);
        applyTextStyle(c);
    }

    @Override
    public void propertyChange(PropertyChangeEvent e) {
        super.propertyChange(e);
        if (Tessel.TEXT_STYLE.equals(e.getPropertyName()) && e.getSource() instanceof JLabel) {
            JLabel label = (JLabel) e.getSource();
            applyTextStyle(label);
            label.revalidate();
            label.repaint();
        }
    }

    /** Applies the label's text style unless the application set its own font/color. */
    public static void applyTextStyle(JComponent c) {
        Object style = c.getClientProperty(Tessel.TEXT_STYLE);
        if (!(style instanceof TextStyle)) return;
        TextStyle s = (TextStyle) style;
        if (c.getFont() == null || c.getFont() instanceof UIResource) {
            c.setFont(new FontUIResource(Fonts.forStyle(s)));
        }
        if (c.getForeground() == null || c.getForeground() instanceof UIResource) {
            c.setForeground(new ColorUIResource(s.isSecondary() ? Tessel.palette().textSecondary : Tessel.palette().text));
        }
    }

    @Override
    protected void paintDisabledText(JLabel l, Graphics g, String s, int textX, int textY) {
        g.setColor(Tessel.palette().textDisabled);
        BasicGraphicsUtils.drawStringUnderlineCharAt(l, (Graphics2D) g, s, l.getDisplayedMnemonicIndex(), textX, textY);
    }
}
