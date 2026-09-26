package net.tessel.gallery.pages;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.tessel.gallery.Layouts;
import net.tessel.laf.Severity;
import net.tessel.laf.Tessel;
import net.tessel.laf.TextStyle;
import net.tessel.laf.components.Snackbar;
import net.tessel.laf.components.TextBlock;
import net.tessel.laf.icons.FontIcon;
import net.tessel.laf.icons.Symbol;

/** All {@link Symbol} glyphs with a filter; clicking copies the Java expression. */
public class IconsPage extends JPanel {

    private static final Symbol[] ALL = Arrays.stream(Symbol.values())
            .sorted(Comparator.comparing(Enum::name)).toArray(Symbol[]::new);

    private final DefaultListModel<Symbol> model = new DefaultListModel<>();

    public IconsPage() {
        super(new BorderLayout(0, 16));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(28, 36, 24, 36));

        JTextField filter = Tessel.leadingIcon(Tessel.placeholder(new JTextField(), "Filter icons"), new FontIcon(Symbol.FILTER, 14));
        filter.setPreferredSize(new Dimension(360, 32));
        JPanel filterRow = Layouts.panel(new BorderLayout());
        filterRow.add(filter, BorderLayout.WEST);

        String note = FontIcon.isAvailable()
                ? "Symbols from Segoe Fluent Icons. Click an icon to copy its Java expression."
                : "Segoe Fluent Icons is not installed on this system, so the glyphs are blank.";
        JPanel header = Layouts.stack(4, Tessel.label("Icons", TextStyle.TITLE), new TextBlock(note, TextStyle.SECONDARY), filterRow);
        filterRow.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));

        JList<Symbol> list = new JList<>(model);
        list.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        list.setVisibleRowCount(-1);
        list.setFixedCellWidth(116);
        list.setFixedCellHeight(96);
        list.setOpaque(false);
        list.setBorder(BorderFactory.createEmptyBorder());
        list.setCellRenderer(new SymbolRenderer());
        list.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting() || list.getSelectedValue() == null) return;
            String text = "new FontIcon(Symbol." + list.getSelectedValue().name() + ")";
            try {
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
                Snackbar.show(this, "Copied to clipboard", text, Severity.SUCCESS, 2000);
            } catch (IllegalStateException ex) {
                // Clipboard busy.
            }
        });

        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(javax.swing.BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);

        add(header, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);

        filter.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                apply(filter.getText());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                apply(filter.getText());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                apply(filter.getText());
            }
        });
        apply("");
    }

    private void apply(String query) {
        String q = query.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
        model.clear();
        for (Symbol s : ALL) {
            if (q.isEmpty() || s.name().toLowerCase(Locale.ROOT).contains(q)) model.addElement(s);
        }
    }

    private static final class SymbolRenderer extends JPanel implements ListCellRenderer<Symbol> {
        private final JLabel icon = new JLabel();
        private final JLabel name = Tessel.label("", TextStyle.CAPTION);

        SymbolRenderer() {
            super(new BorderLayout(0, 8));
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(18, 6, 12, 6));
            icon.setHorizontalAlignment(SwingConstants.CENTER);
            name.setHorizontalAlignment(SwingConstants.CENTER);
            add(icon, BorderLayout.CENTER);
            add(name, BorderLayout.SOUTH);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends Symbol> list, Symbol value, int index, boolean isSelected, boolean cellHasFocus) {
            icon.setIcon(new FontIcon(value, 24));
            icon.setForeground(Tessel.palette().text);
            name.setText(value.name().toLowerCase(Locale.ROOT).replace('_', ' '));
            return this;
        }
    }
}
