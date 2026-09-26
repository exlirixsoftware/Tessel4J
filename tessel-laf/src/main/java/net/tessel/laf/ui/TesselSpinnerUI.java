package net.tessel.laf.ui;

import java.awt.BasicStroke;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.geom.Line2D;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JSpinner;
import javax.swing.LookAndFeel;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicSpinnerUI;
import net.tessel.laf.Palette;
import net.tessel.laf.Tessel;

/** Number-box style spinners: rounded input with "−" and "+" buttons on the right. */
public class TesselSpinnerUI extends BasicSpinnerUI {

    public static ComponentUI createUI(JComponent c) {
        return new TesselSpinnerUI();
    }

    @Override
    protected void installDefaults() {
        super.installDefaults();
        LookAndFeel.installProperty(spinner, "opaque", Boolean.FALSE);
    }

    @Override
    protected void installListeners() {
        super.installListeners();
        UIUtils.installHover(spinner);
    }

    @Override
    protected void uninstallListeners() {
        UIUtils.uninstallHover(spinner);
        super.uninstallListeners();
    }

    @Override
    protected Component createNextButton() {
        SpinButton b = new SpinButton(true);
        b.setName("Spinner.nextButton");
        installNextButtonListeners(b);
        return b;
    }

    @Override
    protected Component createPreviousButton() {
        SpinButton b = new SpinButton(false);
        b.setName("Spinner.previousButton");
        installPreviousButtonListeners(b);
        return b;
    }

    @Override
    protected LayoutManager createLayout() {
        return new SpinnerLayout();
    }

    @Override
    protected JComponent createEditor() {
        JComponent editor = super.createEditor();
        configureEditor(editor);
        return editor;
    }

    @Override
    protected void replaceEditor(JComponent oldEditor, JComponent newEditor) {
        super.replaceEditor(oldEditor, newEditor);
        configureEditor(newEditor);
    }

    private static void configureEditor(JComponent editor) {
        editor.setOpaque(false);
        editor.setBorder(BorderFactory.createEmptyBorder());
        if (editor instanceof JSpinner.DefaultEditor) {
            JFormattedTextField field = ((JSpinner.DefaultEditor) editor).getTextField();
            field.setOpaque(false);
            field.setBorder(BorderFactory.createEmptyBorder());
            field.setHorizontalAlignment(javax.swing.SwingConstants.LEADING);
            UIUtils.installHover(field);
        }
    }

    @Override
    public void update(Graphics g, JComponent c) {
        Graphics2D g2 = UIUtils.create(g);
        try {
            UIUtils.applyEnabledAlpha(g2, c);
            java.awt.Color bg = c.getBackground();
            g2.setColor(bg == null || bg instanceof UIResource ? Tessel.palette().inputBackground : bg);
            UIUtils.fillRound(g2, 0, 0, c.getWidth(), c.getHeight(), TesselInputBorder.RADIUS);
        } finally {
            g2.dispose();
        }
        paint(g, c);
    }

    /** Flat "+" / "−" button. */
    private static final class SpinButton extends JButton implements UIResource {
        private final boolean plus;

        SpinButton(boolean plus) {
            this.plus = plus;
            setFocusable(false);
            setRequestFocusEnabled(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder());
            setRolloverEnabled(true);
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(28, 26);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Palette p = Tessel.palette();
            Graphics2D g2 = UIUtils.create(g);
            try {
                boolean pressed = getModel().isArmed() && getModel().isPressed();
                boolean hover = getModel().isRollover() && isEnabled();
                if (pressed || hover) {
                    g2.setColor(pressed ? p.controlPressed : p.controlHover);
                    UIUtils.fillRound(g2, 0, 0, getWidth(), getHeight(), 4);
                }
                double cx = getWidth() / 2.0;
                double cy = getHeight() / 2.0;
                g2.setColor(hover ? p.text : p.textSecondary);
                g2.setStroke(new BasicStroke(1.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(new Line2D.Double(cx - 5, cy, cx + 5, cy));
                if (plus) g2.draw(new Line2D.Double(cx, cy - 5, cx, cy + 5));
            } finally {
                g2.dispose();
            }
        }
    }

    /** Editor on the left, "−" then "+" on the right. */
    private static final class SpinnerLayout implements LayoutManager {
        private Component next;
        private Component previous;
        private Component editor;

        @Override
        public void addLayoutComponent(String name, Component c) {
            if ("Next".equals(name)) next = c;
            else if ("Previous".equals(name)) previous = c;
            else if ("Editor".equals(name)) editor = c;
        }

        @Override
        public void removeLayoutComponent(Component c) {
            if (c == next) next = null;
            else if (c == previous) previous = null;
            else if (c == editor) editor = null;
        }

        @Override
        public Dimension preferredLayoutSize(Container parent) {
            Insets in = parent.getInsets();
            Dimension e = editor == null ? new Dimension(0, 0) : editor.getPreferredSize();
            int w = in.left + e.width + 8 + 2 * 28 + in.right;
            int h = in.top + Math.max(e.height, 30) + in.bottom;
            return new Dimension(Math.max(w, 120), Math.max(h, 32));
        }

        @Override
        public Dimension minimumLayoutSize(Container parent) {
            return preferredLayoutSize(parent);
        }

        @Override
        public void layoutContainer(Container parent) {
            Insets in = parent.getInsets();
            int h = parent.getHeight() - in.top - in.bottom;
            int x = parent.getWidth() - in.right - 28;
            int by = in.top + (h - 26) / 2;
            if (next != null) next.setBounds(x, by, 28, 26);
            if (previous != null) previous.setBounds(x - 28, by, 28, 26);
            if (editor != null) editor.setBounds(in.left, in.top, Math.max(0, x - 28 - 4 - in.left), h);
        }
    }
}
