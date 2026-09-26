package net.tessel.laf.components;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.Timer;
import net.tessel.laf.ButtonStyle;
import net.tessel.laf.Fonts;
import net.tessel.laf.Palette;
import net.tessel.laf.Tessel;
import net.tessel.laf.TextStyle;
import net.tessel.laf.icons.FontIcon;
import net.tessel.laf.icons.Symbol;
import net.tessel.laf.ui.UIUtils;

/**
 * App navigation: a collapsible side pane with menu and footer items and a content area.
 * Pages are created lazily from a {@link Supplier} and cached.
 *
 * <pre>{@code
 * NavigationView nav = new NavigationView("My App");
 * nav.addItem("Home", Symbol.HOME, HomePage::new);
 * nav.addHeader("Section");
 * nav.addFooterItem("Settings", Symbol.SETTINGS, SettingsPage::new);
 * nav.select(0);
 * }</pre>
 */
public class NavigationView extends JPanel {

    private final List<Item> items = new ArrayList<>();
    private final Map<Item, JComponent> pageCache = new HashMap<>();
    private final List<Consumer<Item>> selectionListeners = new ArrayList<>();

    private final Pane pane = new Pane();
    private final JPanel menuPanel = new JPanel();
    private final JPanel footerPanel = new JPanel();
    private final JPanel paneHeaderHolder = new JPanel(new BorderLayout());
    private final JButton toggleButton = new JButton();
    private final javax.swing.JLabel titleLabel = Tessel.label("", TextStyle.BODY_STRONG);
    private final ContentHost contentHost = new ContentHost();

    private Item selected;
    private boolean paneOpen = true;
    private int openPaneLength = 260;
    private int compactPaneLength = 56;

    public NavigationView() {
        this(null);
    }

    public NavigationView(String paneTitle) {
        super(new BorderLayout());
        setOpaque(false);

        menuPanel.setOpaque(false);
        menuPanel.setLayout(new BoxLayout(menuPanel, BoxLayout.Y_AXIS));
        footerPanel.setOpaque(false);
        footerPanel.setLayout(new BoxLayout(footerPanel, BoxLayout.Y_AXIS));
        footerPanel.setBorder(BorderFactory.createEmptyBorder(4, 0, 8, 0));

        toggleButton.setIcon(new FontIcon(Symbol.GLOBAL_NAV, 16));
        toggleButton.setToolTipText("Toggle navigation");
        Tessel.style(toggleButton, ButtonStyle.ICON);
        toggleButton.setPreferredSize(new Dimension(40, 36));
        toggleButton.addActionListener(e -> setPaneOpen(!paneOpen));

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.X_AXIS));
        top.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        top.add(toggleButton);
        top.add(Box.createHorizontalStrut(12));
        top.add(titleLabel);
        top.add(Box.createHorizontalGlue());

        paneHeaderHolder.setOpaque(false);
        paneHeaderHolder.setBorder(BorderFactory.createEmptyBorder(4, 12, 8, 12));
        paneHeaderHolder.setVisible(false);

        JPanel north = new JPanel(new BorderLayout());
        north.setOpaque(false);
        north.add(top, BorderLayout.NORTH);
        north.add(paneHeaderHolder, BorderLayout.CENTER);

        JPanel menuWrapper = new JPanel(new BorderLayout());
        menuWrapper.setOpaque(false);
        menuWrapper.add(menuPanel, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(menuWrapper, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(javax.swing.BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);

        pane.add(north, BorderLayout.NORTH);
        pane.add(scroll, BorderLayout.CENTER);
        pane.add(footerPanel, BorderLayout.SOUTH);

        add(pane, BorderLayout.WEST);
        add(contentHost, BorderLayout.CENTER);

        setPaneTitle(paneTitle);

        // Cached pages that are not on screen miss SwingUtilities.updateComponentTreeUI; refresh them too.
        Tessel.addThemeListener(() -> {
            for (JComponent page : pageCache.values()) {
                if (page != null && page.getParent() == null) javax.swing.SwingUtilities.updateComponentTreeUI(page);
            }
        });
    }

    // ------------------------------------------------------------------ items

    public Item addItem(String text, Symbol icon, Supplier<? extends JComponent> page) {
        return addItem(text, icon == null ? null : new FontIcon(icon, 16), page);
    }

    public Item addItem(String text, Icon icon, Supplier<? extends JComponent> page) {
        Item item = new Item(text, icon, page);
        items.add(item);
        menuPanel.add(item);
        menuPanel.revalidate();
        return item;
    }

    public Item addFooterItem(String text, Symbol icon, Supplier<? extends JComponent> page) {
        Item item = new Item(text, icon == null ? null : new FontIcon(icon, 16), page);
        items.add(item);
        footerPanel.add(item);
        footerPanel.revalidate();
        return item;
    }

    /** Adds a non-selectable section header to the menu. */
    public void addHeader(String text) {
        menuPanel.add(new Header(text));
        menuPanel.revalidate();
    }

    public List<Item> getItems() {
        return Collections.unmodifiableList(items);
    }

    public Item getSelectedItem() {
        return selected;
    }

    public void select(int index) {
        select(items.get(index));
    }

    public void select(Item item) {
        if (item == selected) return;
        Item old = selected;
        selected = item;
        if (old != null) old.repaint();
        if (item != null) {
            item.repaint();
            JComponent page = pageCache.computeIfAbsent(item, i -> i.page == null ? null : i.page.get());
            if (page != null) contentHost.show(page);
        }
        for (Consumer<Item> l : selectionListeners) l.accept(item);
    }

    /** Shows a page that has no menu item. */
    public void navigate(JComponent page) {
        contentHost.show(page);
    }

    public void addSelectionListener(Consumer<Item> listener) {
        selectionListeners.add(listener);
    }

    // ------------------------------------------------------------------ pane

    public void setPaneTitle(String title) {
        titleLabel.setText(title == null ? "" : title);
    }

    /** A component shown under the pane title while the pane is open (e.g. a search box). */
    public void setPaneHeader(JComponent header) {
        paneHeaderHolder.removeAll();
        if (header != null) paneHeaderHolder.add(header);
        paneHeaderHolder.setVisible(header != null && paneOpen);
        pane.revalidate();
    }

    public boolean isPaneOpen() {
        return paneOpen;
    }

    public void setPaneOpen(boolean open) {
        paneOpen = open;
        titleLabel.setVisible(open);
        paneHeaderHolder.setVisible(open && paneHeaderHolder.getComponentCount() > 0);
        for (Item item : items) item.setToolTipText(open ? null : item.text);
        pane.revalidate();
        revalidate();
        repaint();
    }

    public void setOpenPaneLength(int length) {
        openPaneLength = length;
        revalidate();
    }

    public void setCompactPaneLength(int length) {
        compactPaneLength = length;
        revalidate();
    }

    // ------------------------------------------------------------------ parts

    private final class Pane extends JPanel {
        Pane() {
            super(new BorderLayout());
            setOpaque(false);
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(paneOpen ? openPaneLength : compactPaneLength, super.getPreferredSize().height);
        }

        @Override
        public Dimension getMinimumSize() {
            return getPreferredSize();
        }
    }

    /** A selectable navigation entry. */
    public final class Item extends JComponent {
        private final String text;
        private final Icon icon;
        private final Supplier<? extends JComponent> page;
        private boolean hover;

        Item(String text, Icon icon, Supplier<? extends JComponent> page) {
            this.text = text;
            this.icon = icon;
            this.page = page;
            setFocusable(true);
            setAlignmentX(0f);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    select(Item.this);
                }

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
            });
            getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "select");
            getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "select");
            getActionMap().put("select", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    select(Item.this);
                }
            });
        }

        public String getText() {
            return text;
        }

        public boolean isSelected() {
            return selected == this;
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(openPaneLength, 44);
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, 44);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Palette p = Tessel.palette();
            Graphics2D g2 = UIUtils.create(g);
            try {
                double x = 8;
                double y = 2;
                double w = getWidth() - 16;
                double h = getHeight() - 4;
                boolean sel = isSelected();
                if (sel || hover) {
                    g2.setColor(sel ? p.controlPressed : p.controlHover);
                    UIUtils.fillRound(g2, x, y, w, h, 6);
                }
                if (sel) {
                    g2.setColor(p.accent);
                    UIUtils.fillRound(g2, x, y + (h - 16) / 2, 3, 16, 1.5);
                }
                if (icon != null) {
                    int ix = (int) (x + (40 - icon.getIconWidth()) / 2.0);
                    int iy = (int) (y + (h - icon.getIconHeight()) / 2.0);
                    if (icon instanceof FontIcon) ((FontIcon) icon).paintIcon(this, g2, ix, iy, sel ? p.accent : p.text);
                    else icon.paintIcon(this, g2, ix, iy);
                }
                if (paneOpen && text != null) {
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    g2.setFont(Fonts.regular(14));
                    g2.setColor(p.text);
                    FontMetrics fm = g2.getFontMetrics();
                    int tx = (int) x + 44;
                    int maxW = (int) (x + w) - tx - 8;
                    String s = text;
                    if (fm.stringWidth(s) > maxW) {
                        while (s.length() > 1 && fm.stringWidth(s + "…") > maxW) s = s.substring(0, s.length() - 1);
                        s = s + "…";
                    }
                    g2.drawString(s, tx, (int) (y + (h - fm.getHeight()) / 2 + fm.getAscent()));
                }
                if (UIUtils.isFocusVisible(this)) UIUtils.paintFocusRing(g2, x, y, w, h, 6);
            } finally {
                g2.dispose();
            }
        }
    }

    /** A section header ("Controls", "Design"); a line when the pane is compact. */
    private final class Header extends JComponent {
        private final String text;

        Header(String text) {
            this.text = text;
            setAlignmentX(0f);
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(openPaneLength, 36);
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, 36);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Palette p = Tessel.palette();
            Graphics2D g2 = UIUtils.create(g);
            try {
                if (!paneOpen) {
                    g2.setColor(p.border);
                    g2.fillRect(16, 23, getWidth() - 32, 1);
                    return;
                }
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setFont(Fonts.semibold(12));
                g2.setColor(p.textSecondary);
                g2.drawString(text, 20, 28);
            } finally {
                g2.dispose();
            }
        }
    }

    /** Content area with a rounded top-left corner and a slide/fade transition. */
    private static final class ContentHost extends JPanel {
        private float progress = 1f;
        private long start;
        private final Timer timer = new Timer(15, e -> step());

        ContentHost() {
            super(new BorderLayout());
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(1, 1, 0, 0));
        }

        void show(JComponent page) {
            removeAll();
            add(page, BorderLayout.CENTER);
            revalidate();
            repaint();
            progress = 0f;
            start = System.currentTimeMillis();
            timer.start();
        }

        private void step() {
            progress = Math.min(1f, (System.currentTimeMillis() - start) / 220f);
            if (progress >= 1f) timer.stop();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Palette p = Tessel.palette();
            Graphics2D g2 = UIUtils.create(g);
            try {
                int w = getWidth();
                int h = getHeight();
                double r = 8;
                Path2D shape = new Path2D.Double();
                shape.moveTo(0, h);
                shape.lineTo(0, r);
                shape.quadTo(0, 0, r, 0);
                shape.lineTo(w, 0);
                shape.lineTo(w, h);
                shape.closePath();
                g2.setColor(p.surfaceAlt);
                g2.fill(shape);

                Path2D edge = new Path2D.Double();
                edge.moveTo(0.5, h);
                edge.lineTo(0.5, r);
                edge.quadTo(0.5, 0.5, r, 0.5);
                edge.lineTo(w, 0.5);
                g2.setColor(p.border);
                g2.setStroke(new java.awt.BasicStroke(1f));
                g2.draw(edge);
            } finally {
                g2.dispose();
            }
        }

        @Override
        protected void paintChildren(Graphics g) {
            if (progress >= 1f) {
                super.paintChildren(g);
                return;
            }
            float eased = 1f - (1f - progress) * (1f - progress) * (1f - progress);
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setComposite(AlphaComposite.SrcOver.derive(eased));
                g2.translate(0, Math.round(16 * (1 - eased)));
                super.paintChildren(g2);
            } finally {
                g2.dispose();
            }
        }

        @Override
        public boolean isOptimizedDrawingEnabled() {
            return false;
        }

        @SuppressWarnings("unused")
        Component current() {
            return getComponentCount() == 0 ? null : getComponent(0);
        }
    }
}
