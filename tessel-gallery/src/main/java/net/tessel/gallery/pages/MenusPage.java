package net.tessel.gallery.pages;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import net.tessel.gallery.Layouts;
import net.tessel.gallery.Page;
import net.tessel.laf.Tessel;
import net.tessel.laf.TextStyle;
import net.tessel.laf.icons.FontIcon;
import net.tessel.laf.icons.Symbol;
import net.tessel.laf.ui.UIUtils;

public class MenusPage extends Page {

    public MenusPage() {
        super("Menus & Tooltips", "Menu bars, context menus, sub-menus, checkable items, tool bars and tooltips.");

        section("Menu bar", menuBar());

        JPanel area = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = UIUtils.create(g);
                try {
                    g2.setColor(Tessel.palette().subtle);
                    UIUtils.fillRound(g2, 0, 0, getWidth(), getHeight(), 8);
                    g2.setColor(Tessel.palette().borderStrong);
                    UIUtils.strokeRound(g2, 0, 0, getWidth(), getHeight(), 8, 1f);
                } finally {
                    g2.dispose();
                }
            }
        };
        area.setOpaque(false);
        area.setPreferredSize(new Dimension(100, 120));
        javax.swing.JLabel hint = Tessel.label("Right-click anywhere in this area", TextStyle.SECONDARY);
        hint.setHorizontalAlignment(SwingConstants.CENTER);
        area.add(hint);
        area.setComponentPopupMenu(contextMenu());
        hint.setInheritsPopupMenu(true);
        section("Context menu", area);

        JButton simple = new JButton("Hover me");
        simple.setToolTipText("A simple text tooltip");
        JButton rich = new JButton("Rich tooltip");
        rich.setToolTipText("<html><b>Keyboard shortcut</b><br>Press Ctrl+K to open the command palette.</html>");
        JButton help = Tessel.style(new JButton(new FontIcon(Symbol.HELP)), net.tessel.laf.ButtonStyle.ICON);
        help.setToolTipText("Tooltips use the surface color and a thin border.");
        section("Tooltips", Layouts.row(simple, rich, help));

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.add(toolButton(Symbol.ADD, "New"));
        toolBar.add(toolButton(Symbol.FOLDER, "Open"));
        toolBar.add(toolButton(Symbol.SAVE, "Save"));
        toolBar.addSeparator();
        toolBar.add(toolButton(Symbol.UNDO, "Undo"));
        toolBar.add(toolButton(Symbol.REDO, "Redo"));
        toolBar.addSeparator();
        JButton share = new JButton("Share", new FontIcon(Symbol.SHARE));
        toolBar.add(share);
        section("Tool bar", toolBar);

        section("Tool bar overflow", overflowDemo());
    }

    /** A tool bar in a box of adjustable width: what does not fit moves into the "more" menu. */
    private static JPanel overflowDemo() {
        JToolBar bar = new JToolBar();
        bar.setFloatable(false);
        bar.add(toolButton(Symbol.ADD, "New"));
        bar.add(toolButton(Symbol.FOLDER, "Open"));
        bar.add(toolButton(Symbol.SAVE, "Save"));
        bar.addSeparator();
        bar.add(toolButton(Symbol.CUT, "Cut"));
        bar.add(toolButton(Symbol.COPY, "Copy"));
        bar.add(toolButton(Symbol.PASTE, "Paste"));
        bar.addSeparator();
        JToggleButton bold = new JToggleButton(new FontIcon(Symbol.FONT));
        bold.setToolTipText("Bold");
        bar.add(bold);
        bar.add(toolButton(Symbol.UNDO, "Undo"));
        bar.add(toolButton(Symbol.REDO, "Redo"));
        bar.addSeparator();
        bar.add(new JButton("Share", new FontIcon(Symbol.SHARE)));
        bar.add(new JButton("Print", new FontIcon(Symbol.PRINT)));

        JPanel box = new JPanel(new BorderLayout()) {
            @Override
            public Dimension getMaximumSize() {
                return getPreferredSize();
            }
        };
        box.setOpaque(false);
        box.add(bar, BorderLayout.CENTER);
        box.setPreferredSize(new Dimension(620, 48));

        javax.swing.JSlider width = new javax.swing.JSlider(140, 760, 620);
        width.addChangeListener(e -> {
            box.setPreferredSize(new Dimension(width.getValue(), 48));
            box.revalidate();
            box.getParent().repaint();
        });
        return Layouts.stack(12,
                Layouts.row(Tessel.label("Available width", TextStyle.BODY), width),
                Layouts.row(box),
                Tessel.label("Buttons that do not fit move into the overflow menu; widen the box to bring them back.", TextStyle.CAPTION));
    }

    private static JButton toolButton(Symbol symbol, String tooltip) {
        JButton b = new JButton(new FontIcon(symbol));
        b.setToolTipText(tooltip);
        return b;
    }

    private static JMenuBar menuBar() {
        JMenuBar bar = new JMenuBar();

        JMenu file = new JMenu("File");
        file.setMnemonic(KeyEvent.VK_F);
        file.add(item("New", Symbol.ADD, KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_DOWN_MASK)));
        file.add(item("Open...", Symbol.FOLDER, KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK)));
        file.add(item("Save", Symbol.SAVE, KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK)));
        file.addSeparator();
        JMenu recent = new JMenu("Recent");
        recent.add(new JMenuItem("Report.docx"));
        recent.add(new JMenuItem("Budget.xlsx"));
        recent.add(new JMenuItem("Slides.pptx"));
        file.add(recent);
        file.addSeparator();
        file.add(new JMenuItem("Exit"));

        JMenu edit = new JMenu("Edit");
        edit.setMnemonic(KeyEvent.VK_E);
        edit.add(item("Undo", Symbol.UNDO, KeyStroke.getKeyStroke(KeyEvent.VK_Z, KeyEvent.CTRL_DOWN_MASK)));
        edit.add(item("Redo", Symbol.REDO, KeyStroke.getKeyStroke(KeyEvent.VK_Y, KeyEvent.CTRL_DOWN_MASK)));
        edit.addSeparator();
        edit.add(item("Cut", Symbol.CUT, KeyStroke.getKeyStroke(KeyEvent.VK_X, KeyEvent.CTRL_DOWN_MASK)));
        edit.add(item("Copy", Symbol.COPY, KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_DOWN_MASK)));
        JMenuItem paste = item("Paste", Symbol.PASTE, KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.CTRL_DOWN_MASK));
        paste.setEnabled(false);
        edit.add(paste);

        JMenu view = new JMenu("View");
        view.setMnemonic(KeyEvent.VK_V);
        view.add(new JCheckBoxMenuItem("Status bar", true));
        view.add(new JCheckBoxMenuItem("Word wrap"));
        view.addSeparator();
        ButtonGroup density = new ButtonGroup();
        JRadioButtonMenuItem compact = new JRadioButtonMenuItem("Compact");
        JRadioButtonMenuItem comfortable = new JRadioButtonMenuItem("Comfortable", true);
        density.add(compact);
        density.add(comfortable);
        view.add(compact);
        view.add(comfortable);
        view.addSeparator();
        JMenu zoom = new JMenu("Zoom");
        zoom.add(item("Zoom in", null, KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, KeyEvent.CTRL_DOWN_MASK)));
        zoom.add(item("Zoom out", null, KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, KeyEvent.CTRL_DOWN_MASK)));
        zoom.add(item("Reset", null, KeyStroke.getKeyStroke(KeyEvent.VK_0, KeyEvent.CTRL_DOWN_MASK)));
        view.add(zoom);

        bar.add(file);
        bar.add(edit);
        bar.add(view);
        bar.add(new JMenu("Help"));
        return bar;
    }

    private static JMenuItem item(String text, Symbol icon, KeyStroke accelerator) {
        JMenuItem item = new JMenuItem(text, icon == null ? null : new FontIcon(icon, 14));
        item.setAccelerator(accelerator);
        return item;
    }

    private static JPopupMenu contextMenu() {
        JPopupMenu menu = new JPopupMenu();
        menu.add(item("Open", Symbol.PAGE, null));
        menu.add(item("Rename", Symbol.RENAME, KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0)));
        JMenu share = new JMenu("Share");
        share.setIcon(new FontIcon(Symbol.SHARE, 14));
        share.add(item("Email", Symbol.MAIL, null));
        share.add(item("Copy link", Symbol.LINK, null));
        menu.add(share);
        menu.addSeparator();
        menu.add(new JCheckBoxMenuItem("Pin to start"));
        menu.add(item("Delete", Symbol.DELETE, KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0)));
        return menu;
    }
}
