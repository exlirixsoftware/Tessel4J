package net.tessel.laf.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicOptionPaneUI;
import net.tessel.laf.ButtonStyle;
import net.tessel.laf.Palette;
import net.tessel.laf.Tessel;

/**
 * Option panes in the style of {@code ContentDialog}: a surface-colored message area with the icon at the
 * start, and a tinted footer with right-aligned buttons whose default button uses the accent style.
 */
public class TesselOptionPaneUI extends BasicOptionPaneUI {

    public static ComponentUI createUI(JComponent c) {
        return new TesselOptionPaneUI();
    }

    @Override
    protected Container createMessageArea() {
        Container area = super.createMessageArea();
        makeTransparent(area);
        return area;
    }

    /** The panels the Basic delegate creates are opaque; only those (not the application's message components) are changed. */
    private static void makeTransparent(Container c) {
        if (!(c instanceof JPanel)) return;
        String name = c.getName();
        if (name != null && !name.startsWith("OptionPane.")) return;
        ((JPanel) c).setOpaque(false);
        for (Component child : c.getComponents()) {
            if (child instanceof Container) makeTransparent((Container) child);
        }
    }

    @Override
    protected Container createButtonArea() {
        Container buttons = super.createButtonArea();
        if (buttons instanceof JComponent) {
            ((JComponent) buttons).setOpaque(false);
            ((JComponent) buttons).setBorder(BorderFactory.createEmptyBorder());
        }
        Footer footer = new Footer();
        footer.add(buttons, BorderLayout.CENTER);
        return footer;
    }

    @Override
    protected void addButtonComponents(Container container, Object[] buttons, int initialIndex) {
        super.addButtonComponents(container, buttons, initialIndex);
        Component[] created = container.getComponents();
        if (initialIndex >= 0 && initialIndex < created.length && created[initialIndex] instanceof JButton) {
            Tessel.style((JButton) created[initialIndex], ButtonStyle.ACCENT);
        }
    }

    /** The tinted strip below the message with a divider line on top. */
    private static final class Footer extends JPanel {
        Footer() {
            super(new BorderLayout());
            setOpaque(false);
            setName("OptionPane.buttonArea");
            setBorder(BorderFactory.createEmptyBorder(15, 24, 15, 24));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Palette p = Tessel.palette();
            Graphics2D g2 = UIUtils.create(g);
            try {
                g2.setColor(p.surfaceAlt);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(p.border);
                g2.fillRect(0, 0, getWidth(), 1);
            } finally {
                g2.dispose();
            }
        }
    }
}
