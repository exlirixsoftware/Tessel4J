package net.tessel.laf.components;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.font.TextAttribute;
import java.awt.geom.RoundRectangle2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import javax.swing.text.StyleConstants;
import net.tessel.laf.ButtonStyle;
import net.tessel.laf.Palette;
import net.tessel.laf.RichText;
import net.tessel.laf.Tessel;
import net.tessel.laf.TesselStrings;
import net.tessel.laf.icons.FontIcon;
import net.tessel.laf.icons.Symbol;
import net.tessel.laf.ui.UIUtils;

/**
 * A formatting tool bar for a {@code JTextPane} (or a {@code JEditorPane} with HTML or RTF content): undo and
 * redo, font and size, bold, italic, underline, strikethrough, text and highlight color, alignment, and clear
 * formatting. The buttons follow the caret, so they always show the format of the text being edited, and they
 * are disabled while the pane is read-only.
 *
 * <pre>{@code
 * JTextPane pane = new JTextPane();
 * panel.add(new RichTextToolBar(pane), BorderLayout.NORTH);
 * panel.add(new JScrollPane(pane), BorderLayout.CENTER);
 * }</pre>
 *
 * {@link RichTextEditor} puts both together. Like every {@code JToolBar}, it moves buttons that do not fit into
 * an overflow menu.
 */
public class RichTextToolBar extends JToolBar {

    private static final String[] SIZES = {"8", "9", "10", "11", "12", "14", "16", "18", "20", "24", "28", "32", "36", "48", "72"};
    private static final Color[] TEXT_COLORS = colors(
            0x000000, 0x6B7280, 0xDC2626, 0xEA580C, 0xCA8A04, 0x16A34A, 0x0891B2, 0x2563EB, 0x7C3AED, 0xDB2777, 0xFFFFFF, 0xB91C1C);
    private static final Color[] HIGHLIGHTS = colors(
            0xFEF08A, 0xFDE68A, 0xFED7AA, 0xFECACA, 0xFBCFE8, 0xE9D5FF, 0xBFDBFE, 0xA5F3FC, 0xBBF7D0, 0xD9F99D, 0xE5E7EB, 0xFFFFFF);

    private final JEditorPane pane;
    private final JButton undo;
    private final JButton redo;
    private final JButton clear;
    private final JComboBox<String> family;
    private final JComboBox<String> size;
    private final JToggleButton bold;
    private final JToggleButton italic;
    private final JToggleButton underline;
    private final JToggleButton strike;
    private final ColorButton textColor;
    private final ColorButton highlight;
    private final JToggleButton[] align = new JToggleButton[4];
    private final DocumentListener documentListener = new DocumentListener() {
        @Override
        public void insertUpdate(DocumentEvent e) {
            refreshLater();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            refreshLater();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            refreshLater();
        }
    };
    private Document observed;
    private boolean updating;
    private boolean refreshPending;

    public RichTextToolBar(JEditorPane pane) {
        this.pane = pane;
        setFloatable(false);

        undo = button(Symbol.UNDO, "↶", "undo", () -> RichText.undo(pane));
        redo = button(Symbol.REDO, "↷", "redo", () -> RichText.redo(pane));

        family = new JComboBox<>(families());
        family.setMaximumRowCount(14);
        family.setToolTipText(TesselStrings.get("fontFamily"));
        family.setPreferredSize(new Dimension(170, family.getPreferredSize().height));
        family.setMaximumSize(family.getPreferredSize());
        family.addActionListener(e -> {
            if (!updating && family.getSelectedItem() != null) {
                RichText.setFontFamily(pane, (String) family.getSelectedItem());
                pane.requestFocusInWindow();
            }
        });

        size = new JComboBox<>(SIZES);
        size.setEditable(true);
        size.setToolTipText(TesselStrings.get("fontSize"));
        size.setPreferredSize(new Dimension(76, size.getPreferredSize().height));
        size.setMaximumSize(size.getPreferredSize());
        size.addActionListener(e -> {
            if (updating) return;
            try {
                int points = Integer.parseInt(String.valueOf(size.getSelectedItem()).trim());
                if (points >= 1 && points <= 400) RichText.setFontSize(pane, points);
            } catch (NumberFormatException ex) {
                // fall through to the refresh below, which restores the real size
            }
            refresh();
            pane.requestFocusInWindow();
        });

        bold = toggle("B", Font.BOLD, null, "bold", "Ctrl+B", () -> RichText.toggleBold(pane));
        italic = toggle("I", Font.ITALIC, null, "italic", "Ctrl+I", () -> RichText.toggleItalic(pane));
        underline = toggle("U", Font.PLAIN, TextAttribute.UNDERLINE, "underline", "Ctrl+U", () -> RichText.toggleUnderline(pane));
        strike = toggle("S", Font.PLAIN, TextAttribute.STRIKETHROUGH, "strikethrough", null, () -> RichText.toggleStrikeThrough(pane));

        textColor = new ColorButton("A", "textColor", TEXT_COLORS, null, color -> RichText.setTextColor(pane, color),
                () -> RichText.characterColor(pane, false));
        highlight = new ColorButton("ab", "highlightColor", HIGHLIGHTS, new Color(0xFEF08A), color -> RichText.setHighlight(pane, color),
                () -> RichText.characterColor(pane, true));

        int[] alignments = {StyleConstants.ALIGN_LEFT, StyleConstants.ALIGN_CENTER, StyleConstants.ALIGN_RIGHT, StyleConstants.ALIGN_JUSTIFIED};
        String[] alignKeys = {"alignLeft", "alignCenter", "alignRight", "alignJustify"};
        ButtonGroup group = new ButtonGroup();
        for (int i = 0; i < align.length; i++) {
            int a = alignments[i];
            align[i] = new JToggleButton(new AlignIcon(a));
            Tessel.style(align[i], ButtonStyle.SUBTLE);
            describe(align[i], alignKeys[i], null);
            align[i].addActionListener(e -> {
                if (!updating) {
                    RichText.setAlignment(pane, a);
                    pane.requestFocusInWindow();
                }
            });
            group.add(align[i]);
        }
        clear = button(Symbol.CLEAR, "✕", "clearFormatting", () -> RichText.clearFormatting(pane));

        add(undo);
        add(redo);
        addSeparator();
        add(family);
        add(javax.swing.Box.createHorizontalStrut(4));
        add(size);
        addSeparator();
        add(bold);
        add(italic);
        add(underline);
        add(strike);
        add(textColor);
        add(highlight);
        addSeparator();
        for (JToggleButton a : align) add(a);
        addSeparator();
        add(clear);

        pane.addCaretListener(e -> refreshLater());
        pane.addPropertyChangeListener(e -> {
            String name = e.getPropertyName();
            if ("document".equals(name) || "editable".equals(name) || "enabled".equals(name) || "editorKit".equals(name)) {
                observe();
                refreshLater();
            }
        });
        observe();
        refresh();
    }

    /** The pane this tool bar formats. */
    public JEditorPane getTarget() {
        return pane;
    }

    // ------------------------------------------------------------------ state

    private void observe() {
        Document doc = pane.getDocument();
        if (doc == observed) return;
        if (observed != null) observed.removeDocumentListener(documentListener);
        observed = doc;
        if (doc != null) doc.addDocumentListener(documentListener);
    }

    private void refreshLater() {
        if (refreshPending) return;
        refreshPending = true;
        SwingUtilities.invokeLater(() -> {
            refreshPending = false;
            refresh();
        });
    }

    /** Brings the buttons in line with the pane; called automatically when the caret or the document change. */
    public void refresh() {
        boolean on = pane.isEditable() && pane.isEnabled() && isEnabled();
        updating = true;
        try {
            undo.setEnabled(on && RichText.canUndo(pane));
            redo.setEnabled(on && RichText.canRedo(pane));
            for (Component c : new Component[]{family, size, bold, italic, underline, strike, textColor, highlight, clear}) {
                c.setEnabled(on);
            }
            for (JToggleButton a : align) a.setEnabled(on);

            bold.setSelected(RichText.isBold(pane));
            italic.setSelected(RichText.isItalic(pane));
            underline.setSelected(RichText.isUnderline(pane));
            strike.setSelected(RichText.isStrikeThrough(pane));

            String fam = RichText.fontFamily(pane);
            if (fam == null && pane.getFont() != null) fam = pane.getFont().getFamily();
            if (fam != null && !fam.equals(family.getSelectedItem())) family.setSelectedItem(fam);
            int points = RichText.fontSize(pane);
            if (points <= 0 && pane.getFont() != null) points = pane.getFont().getSize();
            size.setSelectedItem(String.valueOf(points));

            int current = RichText.alignment(pane);
            align[0].setSelected(true);
            if (current == StyleConstants.ALIGN_CENTER) align[1].setSelected(true);
            else if (current == StyleConstants.ALIGN_RIGHT) align[2].setSelected(true);
            else if (current == StyleConstants.ALIGN_JUSTIFIED) align[3].setSelected(true);
            textColor.repaint();
            highlight.repaint();
        } finally {
            updating = false;
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (pane != null) refresh();
    }

    // ------------------------------------------------------------------ building blocks

    private JButton button(Symbol symbol, String fallback, String key, Runnable action) {
        JButton b = FontIcon.isAvailable() ? new JButton(new FontIcon(symbol, 16)) : new JButton(fallback);
        Tessel.style(b, ButtonStyle.ICON);
        describe(b, key, null);
        b.addActionListener(e -> {
            if (updating) return;
            action.run();
            refresh();
            pane.requestFocusInWindow();
        });
        return b;
    }

    private JToggleButton toggle(String text, int style, TextAttribute decoration, String key, String shortcut, Runnable action) {
        JToggleButton b = new JToggleButton(text);
        Font f = b.getFont().deriveFont(style);
        if (decoration != null) f = f.deriveFont(Collections.singletonMap(decoration, decoration == TextAttribute.UNDERLINE
                ? TextAttribute.UNDERLINE_ON : TextAttribute.STRIKETHROUGH_ON));
        b.setFont(f);
        Tessel.style(b, ButtonStyle.SUBTLE); // a selected toggle button turns accent-colored
        describe(b, key, shortcut);
        b.addActionListener(e -> {
            if (updating) return;
            action.run();
            refresh();
            pane.requestFocusInWindow();
        });
        return b;
    }

    private static void describe(AbstractButton b, String key, String shortcut) {
        String text = TesselStrings.get(key);
        b.setToolTipText(shortcut == null ? text : text + " (" + shortcut + ")");
        b.getAccessibleContext().setAccessibleName(text);
    }

    private static String[] families() {
        return GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames(Locale.getDefault());
    }

    private static Color[] colors(int... rgb) {
        Color[] out = new Color[rgb.length];
        for (int i = 0; i < rgb.length; i++) out[i] = new Color(rgb[i]);
        return out;
    }

    // ------------------------------------------------------------------ alignment icon

    /** Four lines of different lengths, drawn in the colors of the button. */
    private static final class AlignIcon implements Icon {
        private final int alignment;

        AlignIcon(int alignment) {
            this.alignment = alignment;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = UIUtils.create(g);
            try {
                Palette p = Tessel.palette();
                boolean selected = c instanceof AbstractButton && ((AbstractButton) c).isSelected();
                g2.setColor(selected ? p.onAccent : p.text);
                int[] widths = {14, 9, 14, 6};
                for (int i = 0; i < widths.length; i++) {
                    int w = alignment == StyleConstants.ALIGN_JUSTIFIED ? 14 : widths[i];
                    int lx = x + 1;
                    if (alignment == StyleConstants.ALIGN_CENTER) lx = x + 1 + (14 - w) / 2;
                    else if (alignment == StyleConstants.ALIGN_RIGHT) lx = x + 1 + 14 - w;
                    g2.fillRect(lx, y + 2 + i * 3, w, 1);
                }
            } finally {
                g2.dispose();
            }
        }

        @Override
        public int getIconWidth() {
            return 16;
        }

        @Override
        public int getIconHeight() {
            return 16;
        }
    }

    // ------------------------------------------------------------------ color buttons

    /** A split button: the icon applies the last chosen color, the arrow opens swatches. */
    private final class ColorButton extends SplitButton {

        private Color chosen;
        private final String label;

        ColorButton(String label, String key, Color[] swatches, Color initial, Consumer<Color> apply, Supplier<Color> current) {
            super(null, null);
            this.label = label;
            this.chosen = initial;
            setIcon(new LetterIcon());
            Tessel.style(this, ButtonStyle.SUBTLE);
            describe(this, key, null);
            addActionListener(e -> {
                if (chosen != null) {
                    apply.accept(chosen);
                    afterApply();
                }
            });

            JPopupMenu menu = getMenu();
            menu.add(new Swatches(swatches, color -> {
                menu.setVisible(false);
                chosen = color;
                apply.accept(color);
                afterApply();
            }));
            menu.addSeparator();
            JMenuItem more = new JMenuItem(TesselStrings.get("moreColors"));
            more.addActionListener(e -> {
                Color start = chosen != null ? chosen : current.get();
                Color picked = JColorChooser.showDialog(RichTextToolBar.this, TesselStrings.get(key), start == null ? Color.BLACK : start);
                if (picked != null) {
                    chosen = picked;
                    apply.accept(picked);
                    afterApply();
                }
            });
            menu.add(more);
            JMenuItem none = new JMenuItem(TesselStrings.get("noColor"));
            none.addActionListener(e -> {
                chosen = null;
                apply.accept(null);
                afterApply();
            });
            menu.add(none);
        }

        private void afterApply() {
            refresh();
            repaint();
            pane.requestFocusInWindow();
        }

        @Override
        public void updateUI() {
            super.updateUI();
            // Leave room for the arrow only; the default margin is made for text buttons.
            setMargin(new Insets(6, 6, 6, 30));
        }

        private final class LetterIcon implements Icon {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = UIUtils.create(g);
                try {
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    Palette p = Tessel.palette();
                    Font f = c.getFont().deriveFont(Font.BOLD, label.length() > 1 ? 12f : 14f);
                    g2.setFont(f);
                    g2.setColor(p.text);
                    int tw = g2.getFontMetrics().stringWidth(label);
                    g2.drawString(label, x + (16 - tw) / 2, y + 11);
                    Color bar = chosen != null ? chosen : p.border;
                    g2.setColor(bar);
                    g2.fill(new RoundRectangle2D.Double(x + 1, y + 13, 14, 3, 2, 2));
                    if (chosen == null) {
                        g2.setColor(p.borderStrong);
                        g2.drawLine(x + 1, y + 14, x + 14, y + 14);
                    }
                } finally {
                    g2.dispose();
                }
            }

            @Override
            public int getIconWidth() {
                return 16;
            }

            @Override
            public int getIconHeight() {
                return 16;
            }
        }
    }

    /** A grid of color squares inside a popup menu. */
    private static final class Swatches extends JPanel {

        Swatches(Color[] colors, Consumer<Color> picked) {
            super(new GridLayout(2, (colors.length + 1) / 2, 4, 4));
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
            for (Color c : colors) add(new Swatch(c, picked));
        }
    }

    private static final class Swatch extends JComponent {
        private final Color color;
        private boolean hover;

        Swatch(Color color, Consumer<Color> picked) {
            this.color = color;
            setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
            setToolTipText(String.format("#%06X", color.getRGB() & 0xFFFFFF));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (contains(e.getPoint())) picked.accept(color);
                }
            });
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(24, 24);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = UIUtils.create(g);
            try {
                Palette p = Tessel.palette();
                g2.setColor(color);
                UIUtils.fillRound(g2, 1, 1, getWidth() - 2, getHeight() - 2, 5);
                g2.setColor(hover ? p.accent : p.borderStrong);
                UIUtils.strokeRound(g2, 0, 0, getWidth(), getHeight(), 6, hover ? 2f : 1f);
            } finally {
                g2.dispose();
            }
        }
    }
}
