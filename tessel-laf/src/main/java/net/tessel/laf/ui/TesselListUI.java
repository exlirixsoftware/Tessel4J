package net.tessel.laf.ui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.plaf.BorderUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicListUI;
import net.tessel.laf.Palette;
import net.tessel.laf.Tessel;

/**
 * Lists with rounded hover and selection highlights and an accent indicator on selected rows.
 * Highlights are painted by the delegate, so any non-opaque renderer gets them.
 */
public class TesselListUI extends BasicListUI {

    private static final String ROLLOVER = "Tessel.rolloverIndex";

    private final MouseAdapter rolloverHandler = new MouseAdapter() {
        @Override
        public void mouseMoved(MouseEvent e) {
            int index = list.locationToIndex(e.getPoint());
            if (index >= 0) {
                Rectangle r = list.getCellBounds(index, index);
                if (r == null || !r.contains(e.getPoint())) index = -1;
            }
            setRollover(index);
        }

        @Override
        public void mouseExited(MouseEvent e) {
            setRollover(-1);
        }
    };

    public static ComponentUI createUI(JComponent c) {
        return new TesselListUI();
    }

    @Override
    protected void installDefaults() {
        super.installDefaults();
        if (list.getBorder() == null || list.getBorder() instanceof UIResource) {
            list.setBorder(new BorderUIResource.EmptyBorderUIResource(4, 4, 4, 4));
        }
    }

    @Override
    protected void installListeners() {
        super.installListeners();
        list.addMouseListener(rolloverHandler);
        list.addMouseMotionListener(rolloverHandler);
    }

    @Override
    protected void uninstallListeners() {
        list.removeMouseListener(rolloverHandler);
        list.removeMouseMotionListener(rolloverHandler);
        super.uninstallListeners();
    }

    private void setRollover(int index) {
        Object old = list.getClientProperty(ROLLOVER);
        int oldIndex = old instanceof Integer ? (Integer) old : -1;
        if (oldIndex == index) return;
        list.putClientProperty(ROLLOVER, index);
        repaintCell(oldIndex);
        repaintCell(index);
    }

    private void repaintCell(int index) {
        if (index < 0 || index >= list.getModel().getSize()) return;
        Rectangle r = list.getCellBounds(index, index);
        if (r != null) list.repaint(r);
    }

    @Override
    protected void paintCell(Graphics g, int row, Rectangle rowBounds, ListCellRenderer<Object> cellRenderer,
                             ListModel<Object> dataModel, ListSelectionModel selModel, int leadIndex) {
        Palette p = Tessel.palette();
        JComboBox<?> combo = (JComboBox<?>) list.getClientProperty(TesselComboBoxUI.COMBO_KEY);
        boolean selected = selModel.isSelectedIndex(row);
        Object rollover = list.getClientProperty(ROLLOVER);
        boolean hover = rollover instanceof Integer && (Integer) rollover == row;

        // In a combo popup the list selection follows the mouse; the "current" item is the combo's value.
        boolean current = combo != null ? row == combo.getSelectedIndex() : selected;
        boolean highlight = combo != null ? selected : hover;

        if ((current || highlight) && list.isEnabled()) {
            Graphics2D g2 = UIUtils.create(g);
            try {
                g2.setColor(current ? p.subtle : p.controlHover);
                UIUtils.fillRound(g2, rowBounds.x, rowBounds.y + 1, rowBounds.width, rowBounds.height - 2, 4);
                if (current) {
                    g2.setColor(p.accent);
                    int h = Math.min(16, rowBounds.height - 8);
                    UIUtils.fillRound(g2, rowBounds.x + 1, rowBounds.y + (rowBounds.height - h) / 2.0, 3, h, 1.5);
                }
            } finally {
                g2.dispose();
            }
        }
        super.paintCell(g, row, rowBounds, cellRenderer, dataModel, selModel, leadIndex);
    }

    /** Default list renderer: padded, non-opaque; highlighting comes from {@link TesselListUI}. */
    public static class TesselListCellRenderer extends JLabel implements ListCellRenderer<Object>, UIResource {

        public TesselListCellRenderer() {
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        }

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            setComponentOrientation(list.getComponentOrientation());
            if (value instanceof Icon) {
                setIcon((Icon) value);
                setText("");
            } else {
                setIcon(null);
                setText(value == null ? "" : value.toString());
            }
            setFont(list.getFont());
            setForeground(isSelected ? list.getSelectionForeground() : list.getForeground());
            setEnabled(list.isEnabled());
            return this;
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            if (getText() == null || getText().isEmpty()) {
                d.height = Math.max(d.height, getFontMetrics(getFont()).getHeight() + 16);
            }
            return d;
        }
    }
}
