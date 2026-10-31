package net.tessel.gallery.pages;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import net.tessel.gallery.Layouts;
import net.tessel.gallery.Page;
import net.tessel.laf.ButtonStyle;
import net.tessel.laf.Severity;
import net.tessel.laf.Tessel;
import net.tessel.laf.TextStyle;
import net.tessel.laf.components.ContentDialog;
import net.tessel.laf.components.Snackbar;
import net.tessel.laf.components.TabView;
import net.tessel.laf.components.ToggleSwitch;
import net.tessel.laf.icons.FontIcon;
import net.tessel.laf.icons.Symbol;
import net.tessel.laf.ui.UIUtils;

public class TabsPage extends Page {

    private int documents;
    private int windows;
    private final JLabel tabCount = Tessel.label("", TextStyle.SECONDARY);

    public TabsPage() {
        super("Tabs & Windows", "TabView for documents, and internal frames inside a desktop pane.");

        // TabView
        TabView tabs = new TabView();
        tabs.setPreferredSize(new Dimension(600, 330));
        tabs.addTab("Readme.md", new FontIcon(Symbol.DOCUMENT, 16), editor("# Tessel4J\n\nA modern look and feel for Swing.\n\n"
                + "Drag a tab to reorder it, middle-click or press Ctrl+W to close it,\nand use + to open a new one."));
        tabs.addTab("Notes.txt", new FontIcon(Symbol.DOCUMENT, 16), editor("Closing a tab that has text asks for confirmation."));
        tabs.addTab("Scratch", editor(""));
        tabs.setSelectedIndex(0);
        tabs.setAddTabHandler(() -> {
            TabView.Tab tab = tabs.addTab("Untitled " + (++documents), new FontIcon(Symbol.DOCUMENT, 16), editor(""));
            tab.setTooltip("A new, empty document");
        });
        tabs.addTabViewListener(new TabView.TabViewListener() {
            @Override
            public boolean tabClosing(TabView view, TabView.Tab tab) {
                JTextArea area = (JTextArea) ((JScrollPane) tab.getContent()).getViewport().getView();
                if (area.getText().trim().isEmpty()) return true;
                ContentDialog d = new ContentDialog("Close " + tab.getTitle() + "?", "The text in this tab will be lost.");
                d.setPrimaryButtonText("Close tab");
                d.setPrimaryDestructive(true);
                d.setCloseButtonText("Keep");
                return d.showModal(TabsPage.this) == ContentDialog.Result.PRIMARY;
            }

            @Override
            public void tabClosed(TabView view, TabView.Tab tab) {
                updateCount(view);
            }

            @Override
            public void selectionChanged(TabView view, TabView.Tab oldTab, TabView.Tab newTab) {
                updateCount(view);
            }
        });
        updateCount(tabs);

        ToggleSwitch reorderable = new ToggleSwitch("Reorderable", true);
        reorderable.addItemListener(e -> tabs.setReorderable(reorderable.isSelected()));
        ToggleSwitch addButton = new ToggleSwitch("Add button", true);
        addButton.addItemListener(e -> tabs.setAddButtonVisible(addButton.isSelected()));
        JButton many = new JButton("Add 8 tabs");
        many.addActionListener(e -> {
            for (int i = 0; i < 8; i++) tabs.addTab("Document " + (++documents), new FontIcon(Symbol.DOCUMENT, 16), editor("Document " + documents));
        });
        JButton rename = new JButton("Rename selected");
        rename.addActionListener(e -> {
            TabView.Tab t = tabs.getSelectedTab();
            if (t != null) t.setTitle(t.getTitle().endsWith("*") ? t.getTitle() : t.getTitle() + "*");
        });
        section("TabView", Layouts.stack(12, tabs, Layouts.row(reorderable, addButton, many, rename, tabCount)));

        // Internal frames
        JDesktopPane desktop = new JDesktopPane();
        desktop.setOpaque(false);
        newWindow(desktop, 24, 24, "Document");
        newWindow(desktop, 150, 70, "Inspector");

        JPanel stage = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = UIUtils.create(g);
                try {
                    g2.setColor(Tessel.palette().subtle);
                    UIUtils.fillRound(g2, 0, 0, getWidth(), getHeight(), 8);
                    g2.setColor(Tessel.palette().border);
                    UIUtils.strokeRound(g2, 0, 0, getWidth(), getHeight(), 8, 1f);
                } finally {
                    g2.dispose();
                }
            }
        };
        stage.setOpaque(false);
        stage.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        stage.add(desktop, BorderLayout.CENTER);
        stage.setPreferredSize(new Dimension(600, 380));

        JButton add = Tessel.style(new JButton("New window", new FontIcon(Symbol.ADD)), ButtonStyle.ACCENT);
        add.addActionListener(e -> newWindow(desktop, 30 + (windows % 8) * 28, 30 + (windows % 8) * 24, "Window"));
        JButton minimize = new JButton("Minimize all");
        minimize.addActionListener(e -> {
            for (JInternalFrame f : desktop.getAllFrames()) {
                try {
                    f.setIcon(true);
                } catch (java.beans.PropertyVetoException ignored) {
                    // ignore
                }
            }
        });
        JButton restore = new JButton("Restore all");
        restore.addActionListener(e -> {
            for (JInternalFrame f : desktop.getAllFrames()) {
                try {
                    f.setIcon(false);
                } catch (java.beans.PropertyVetoException ignored) {
                    // ignore
                }
            }
        });
        JButton closeAll = new JButton("Close all");
        closeAll.addActionListener(e -> {
            for (JInternalFrame f : desktop.getAllFrames()) f.dispose();
            Snackbar.show(this, "All windows closed", null, Severity.INFORMATIONAL);
        });
        section("Internal frames", Layouts.stack(12, stage, Layouts.row(add, minimize, restore, closeAll)));
    }

    private void updateCount(TabView view) {
        TabView.Tab selected = view.getSelectedTab();
        tabCount.setText(view.getTabCount() + " tab(s)" + (selected == null ? "" : ", selected: " + selected.getTitle()));
    }

    private static JScrollPane editor(String text) {
        JTextArea area = new JTextArea(text);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setOpaque(false);
        area.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        JScrollPane scroll = new JScrollPane(area);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setOpaque(false);
        scroll.setOpaque(false);
        return scroll;
    }

    private void newWindow(JDesktopPane desktop, int x, int y, String title) {
        JInternalFrame frame = new JInternalFrame(title + " " + (++windows), true, true, true, true);
        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setOpaque(false);
        body.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        body.add(Tessel.label("Drag the title bar, resize from any edge, or use the buttons.", TextStyle.SECONDARY), BorderLayout.NORTH);
        JButton ok = Tessel.style(new JButton("OK"), ButtonStyle.ACCENT);
        ok.addActionListener(e -> frame.dispose());
        JPanel south = Layouts.row(ok);
        body.add(south, BorderLayout.SOUTH);
        frame.setContentPane(body);
        frame.setFrameIcon(new FontIcon(Symbol.PAGE, 16));
        frame.setBounds(x, y, 340, 200);
        frame.setVisible(true);
        desktop.add(frame);
        desktop.getDesktopManager().activateFrame(frame);
        try {
            frame.setSelected(true);
        } catch (java.beans.PropertyVetoException ignored) {
            // ignore
        }
    }
}
