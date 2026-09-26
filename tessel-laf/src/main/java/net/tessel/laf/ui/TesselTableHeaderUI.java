package net.tessel.laf.ui;

import java.awt.Component;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicTableHeaderUI;
import javax.swing.table.DefaultTableCellRenderer;
import net.tessel.laf.Tessel;

/** Left-aligned, small semibold column headers with a chevron sort indicator. */
public class TesselTableHeaderUI extends BasicTableHeaderUI {

    public static ComponentUI createUI(JComponent c) {
        return new TesselTableHeaderUI();
    }

    @Override
    protected void installDefaults() {
        super.installDefaults();
        if (header.getDefaultRenderer() instanceof UIResource) {
            header.setDefaultRenderer(new HeaderRenderer());
        }
    }

    /** Default header renderer. */
    public static class HeaderRenderer extends DefaultTableCellRenderer implements UIResource {

        public HeaderRenderer() {
            setHorizontalAlignment(SwingConstants.LEADING);
            setHorizontalTextPosition(SwingConstants.LEADING);
            setIconTextGap(8);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, false, false, row, column);
            setFont(UIManager.getFont("TableHeader.font"));
            setForeground(Tessel.palette().textSecondary);
            setOpaque(false);
            setBorder(UIManager.getBorder("TableHeader.cellBorder"));
            setIcon(null);
            if (table != null) {
                RowSorter<?> sorter = table.getRowSorter();
                List<? extends RowSorter.SortKey> keys = sorter == null ? null : sorter.getSortKeys();
                if (keys != null && !keys.isEmpty() && keys.get(0).getColumn() == table.convertColumnIndexToModel(column)) {
                    SortOrder order = keys.get(0).getSortOrder();
                    if (order == SortOrder.ASCENDING) setIcon(UIManager.getIcon("Table.ascendingSortIcon"));
                    else if (order == SortOrder.DESCENDING) setIcon(UIManager.getIcon("Table.descendingSortIcon"));
                }
            }
            return this;
        }
    }
}
