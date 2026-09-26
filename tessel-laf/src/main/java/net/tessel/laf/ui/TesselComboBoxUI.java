package net.tessel.laf.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import javax.swing.BorderFactory;
import javax.swing.ComboBoxEditor;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.LookAndFeel;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicComboBoxEditor;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.BasicGraphicsUtils;
import javax.swing.plaf.basic.ComboPopup;
import net.tessel.laf.Palette;
import net.tessel.laf.Tessel;
import net.tessel.laf.icons.TesselIcons;

/** Rounded combo boxes with a chevron, a padded popup and placeholder support. */
public class TesselComboBoxUI extends javax.swing.plaf.basic.BasicComboBoxUI {

    /** List client property linking a popup list to its combo box. */
    static final String COMBO_KEY = "Tessel.comboBox";

    public static ComponentUI createUI(JComponent c) {
        return new TesselComboBoxUI();
    }

    @Override
    protected void installDefaults() {
        super.installDefaults();
        LookAndFeel.installProperty(comboBox, "opaque", Boolean.FALSE);
    }

    @Override
    protected void installListeners() {
        super.installListeners();
        UIUtils.installHover(comboBox);
    }

    @Override
    protected void uninstallListeners() {
        UIUtils.uninstallHover(comboBox);
        super.uninstallListeners();
    }

    @Override
    public void configureArrowButton() {
        super.configureArrowButton();
        if (arrowButton != null) UIUtils.installHover(arrowButton);
    }

    @Override
    protected void configureEditor() {
        super.configureEditor();
        if (editor instanceof JComponent) UIUtils.installHover((JComponent) editor);
    }

    @Override
    protected JButton createArrowButton() {
        return new ArrowButton();
    }

    @Override
    protected ComboBoxEditor createEditor() {
        return new BasicComboBoxEditor.UIResource() {
            @Override
            protected JTextField createEditorComponent() {
                JTextField field = super.createEditorComponent();
                field.setOpaque(false);
                field.setBorder(BorderFactory.createEmptyBorder());
                return field;
            }
        };
    }

    @Override
    protected ListCellRenderer<Object> createRenderer() {
        return new TesselComboBoxRenderer();
    }

    @Override
    protected ComboPopup createPopup() {
        return new BasicComboPopup(comboBox) {
            @Override
            protected void configurePopup() {
                super.configurePopup();
                setBorder(new TesselBorders.PopupBorder(0, 0, 0, 0));
            }

            @Override
            protected void configureList() {
                super.configureList();
                list.putClientProperty(COMBO_KEY, comboBox);
                list.setBackground(new javax.swing.plaf.ColorUIResource(Tessel.palette().surface));
            }

            @Override
            protected Rectangle computePopupBounds(int px, int py, int pw, int ph) {
                return super.computePopupBounds(px, py + 4, pw, ph);
            }
        };
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        Palette p = Tessel.palette();
        Graphics2D g2 = UIUtils.create(g);
        try {
            UIUtils.applyEnabledAlpha(g2, c);
            boolean hover = UIUtils.isHover(c) || isPopupVisible(comboBox);
            Color bg;
            if (comboBox.isEditable()) {
                bg = p.inputBackground;
            } else {
                Color user = comboBox.getBackground();
                bg = user != null && !(user instanceof UIResource) ? user : hover && c.isEnabled() ? p.controlHover : p.controlBackground;
            }
            g2.setColor(bg);
            UIUtils.fillRound(g2, 0, 0, c.getWidth(), c.getHeight(), TesselInputBorder.RADIUS);
            if (!comboBox.isEditable()) paintCurrentValue(g2, rectangleForCurrentValue(), false);
        } finally {
            g2.dispose();
        }
    }

    @Override
    public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
        // Background is painted in paint().
    }

    @Override
    public void paintCurrentValue(Graphics g, Rectangle bounds, boolean hasFocus) {
        Object value = comboBox.getSelectedItem();
        Object placeholder = comboBox.getClientProperty(Tessel.PLACEHOLDER);
        if (value == null && placeholder instanceof String) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setFont(comboBox.getFont());
                g2.setColor(Tessel.palette().textTertiary);
                FontMetrics fm = comboBox.getFontMetrics(comboBox.getFont());
                int y = bounds.y + (bounds.height - fm.getHeight()) / 2 + fm.getAscent();
                BasicGraphicsUtils.drawString(comboBox, g2, (String) placeholder, bounds.x, y);
            } finally {
                g2.dispose();
            }
            return;
        }

        ListCellRenderer<Object> renderer = comboBox.getRenderer();
        Component comp = renderer.getListCellRendererComponent(listBox, value, -1, false, false);
        comp.setFont(comboBox.getFont());
        comp.setForeground(comboBox.getForeground());
        boolean wasOpaque = false;
        if (comp instanceof JComponent) {
            wasOpaque = comp.isOpaque();
            ((JComponent) comp).setOpaque(false);
        }
        currentValuePane.paintComponent(g, comp, comboBox, bounds.x, bounds.y, bounds.width, bounds.height,
                comp instanceof javax.swing.JPanel);
        if (comp instanceof JComponent) ((JComponent) comp).setOpaque(wasOpaque);
    }

    @Override
    public Dimension getMinimumSize(JComponent c) {
        Dimension d = super.getMinimumSize(c);
        return new Dimension(Math.max(d.width, 80), Math.max(d.height, 32));
    }

    @Override
    public Dimension getPreferredSize(JComponent c) {
        Dimension d = super.getPreferredSize(c);
        return new Dimension(Math.max(d.width, 120), Math.max(d.height, 32));
    }

    /** The chevron button of the combo box. */
    static final class ArrowButton extends JButton implements UIResource {
        private final javax.swing.Icon chevron = TesselIcons.chevron(TesselIcons.Direction.DOWN, 16);

        ArrowButton() {
            setName("ComboBox.arrowButton");
            setFocusable(false);
            setRequestFocusEnabled(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder());
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(32, 30);
        }

        @Override
        protected void paintComponent(Graphics g) {
            chevron.paintIcon(this, g, (getWidth() - 16) / 2, (getHeight() - 16) / 2);
        }
    }

    /** Renders combo box items with padding; highlighting is painted by the list delegate. */
    public static class TesselComboBoxRenderer extends javax.swing.JLabel implements ListCellRenderer<Object>, UIResource {

        public TesselComboBoxRenderer() {
            setOpaque(false);
        }

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            if (value instanceof javax.swing.Icon) {
                setIcon((javax.swing.Icon) value);
                setText("");
            } else {
                setIcon(null);
                setText(value == null ? " " : value.toString());
            }
            if (list != null) {
                setFont(list.getFont());
                setForeground(list.getForeground());
            }
            setBorder(index < 0 ? BorderFactory.createEmptyBorder() : BorderFactory.createEmptyBorder(7, 10, 7, 10));
            setOpaque(false);
            return this;
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            if (getText() == null || getText().isEmpty()) d.height = Math.max(d.height, getFontMetrics(getFont()).getHeight());
            return d;
        }
    }
}
