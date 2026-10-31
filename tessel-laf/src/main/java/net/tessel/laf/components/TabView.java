package net.tessel.laf.components;

import java.awt.AWTKeyStroke;
import java.awt.BasicStroke;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.EventListenerList;
import net.tessel.laf.Fonts;
import net.tessel.laf.Palette;
import net.tessel.laf.Tessel;
import net.tessel.laf.icons.TesselIcons;
import net.tessel.laf.ui.UIUtils;

/**
 * Closable, reorderable tabs with a "+" button, like the tabs of a browser or an editor.
 *
 * <pre>{@code
 * TabView tabs = new TabView();
 * tabs.addTab("Readme.md", new FontIcon(Symbol.DOCUMENT, 16), new JTextArea());
 * tabs.setAddTabHandler(() -> tabs.addTab("Untitled", new JTextArea()));
 * tabs.addTabViewListener(new TabViewListener() {
 *     public boolean tabClosing(TabView view, Tab tab) { return confirmClose(tab); }
 * });
 * }</pre>
 *
 * Tabs shrink to fit and scroll when there are too many. Drag a tab to reorder it, middle-click or press
 * Ctrl+W to close it, Ctrl+PageUp / Ctrl+PageDown to switch (Ctrl+Tab too while the tab strip has focus).
 * The last tab can be closed unless {@link #setCloseLastTabAllowed(boolean)} says otherwise.
 */
public class TabView extends JPanel {

    /** One tab: a title, an optional icon and the component shown when it is selected. */
    public static final class Tab {
        private String title;
        private Icon icon;
        private String tooltip;
        private boolean closable = true;
        private final Component content;
        private Object data;
        private TabView owner;

        Tab(String title, Icon icon, Component content) {
            this.title = title == null ? "" : title;
            this.icon = icon;
            this.content = content;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title == null ? "" : title;
            changed();
        }

        public Icon getIcon() {
            return icon;
        }

        public void setIcon(Icon icon) {
            this.icon = icon;
            changed();
        }

        public String getTooltip() {
            return tooltip;
        }

        public void setTooltip(String tooltip) {
            this.tooltip = tooltip;
        }

        public boolean isClosable() {
            return closable;
        }

        public void setClosable(boolean closable) {
            this.closable = closable;
            changed();
        }

        public Component getContent() {
            return content;
        }

        /** Free for the application (e.g. the document shown in the tab). */
        public Object getData() {
            return data;
        }

        public void setData(Object data) {
            this.data = data;
        }

        public TabView getTabView() {
            return owner;
        }

        public boolean isSelected() {
            return owner != null && owner.getSelectedTab() == this;
        }

        private void changed() {
            if (owner != null) owner.strip.tabsChanged();
        }
    }

    /** Callbacks for tab changes; override only what you need. */
    public interface TabViewListener extends java.util.EventListener {
        default void selectionChanged(TabView view, Tab oldTab, Tab newTab) {
        }

        /** Return false to keep the tab open. */
        default boolean tabClosing(TabView view, Tab tab) {
            return true;
        }

        default void tabClosed(TabView view, Tab tab) {
        }

        default void tabMoved(TabView view, Tab tab, int fromIndex, int toIndex) {
        }
    }

    static final int STRIP_HEIGHT = 40;
    private static final int TAB_TOP = 6;
    private static final int MIN_TAB_WIDTH = 76;
    private static final int MAX_TAB_WIDTH = 220;
    private static final int RADIUS = 8;

    private final List<Tab> tabs = new ArrayList<>();
    private final Strip strip = new Strip();
    private final Holder holder = new Holder();
    private final CardLayout cards = new CardLayout();
    private final Map<Component, String> cardNames = new IdentityHashMap<>();
    private final EventListenerList listeners = new EventListenerList();
    private Tab selected;
    private boolean reorderable = true;
    private boolean closeLastTabAllowed = true;
    private Runnable addTabHandler;
    private boolean addButtonVisible = true;
    private int cardCounter;

    public TabView() {
        super(null);
        setOpaque(false);
        setLayout(new TabLayout());
        holder.setLayout(cards);
        add(strip);
        add(holder);

        // Switching with the keyboard works from anywhere inside the tab view.
        bind(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, KeyEvent.CTRL_DOWN_MASK), () -> cycle(1));
        bind(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK), () -> cycle(-1));
        bind(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, KeyEvent.CTRL_DOWN_MASK), () -> cycle(1));
        bind(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, KeyEvent.CTRL_DOWN_MASK), () -> cycle(-1));
        bind(KeyStroke.getKeyStroke(KeyEvent.VK_W, KeyEvent.CTRL_DOWN_MASK), () -> {
            if (selected != null) closeTab(selected);
        });
        bind(KeyStroke.getKeyStroke(KeyEvent.VK_F4, KeyEvent.CTRL_DOWN_MASK), () -> {
            if (selected != null) closeTab(selected);
        });
    }

    private void bind(KeyStroke key, Runnable action) {
        String name = "tessel.tabview." + key;
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(key, name);
        getActionMap().put(name, new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                action.run();
            }
        });
    }

    // ------------------------------------------------------------------ tabs

    public Tab addTab(String title, Component content) {
        return insertTab(tabs.size(), title, null, content);
    }

    public Tab addTab(String title, Icon icon, Component content) {
        return insertTab(tabs.size(), title, icon, content);
    }

    /** Adds a tab and selects it. */
    public Tab insertTab(int index, String title, Icon icon, Component content) {
        Tab tab = new Tab(title, icon, content);
        tab.owner = this;
        index = Math.max(0, Math.min(index, tabs.size()));
        tabs.add(index, tab);
        String card = "tab" + (cardCounter++);
        cardNames.put(content, card);
        holder.add(content, card);
        strip.tabsChanged();
        setSelectedTab(tab);
        return tab;
    }

    /** Removes a tab without asking the listeners. */
    public void removeTab(Tab tab) {
        int index = tabs.indexOf(tab);
        if (index < 0) return;
        tabs.remove(index);
        holder.remove(tab.content);
        cardNames.remove(tab.content);
        tab.owner = null;
        if (selected == tab) {
            Tab old = selected;
            selected = null;
            Tab next = tabs.isEmpty() ? null : tabs.get(Math.min(index, tabs.size() - 1));
            if (next != null) {
                selected = next;
                cards.show(holder, cardNames.get(next.content));
            }
            fireSelection(old, selected);
        }
        strip.tabsChanged();
        holder.revalidate();
        holder.repaint();
    }

    public void removeTabAt(int index) {
        removeTab(tabs.get(index));
    }

    /**
     * Closes a tab the way the close button does: the listeners may veto it.
     *
     * @return true when the tab was closed
     */
    public boolean closeTab(Tab tab) {
        if (tab == null || !tab.closable || !tabs.contains(tab)) return false;
        if (tabs.size() == 1 && !closeLastTabAllowed) return false;
        for (TabViewListener l : listeners.getListeners(TabViewListener.class)) {
            if (!l.tabClosing(this, tab)) return false;
        }
        removeTab(tab);
        for (TabViewListener l : listeners.getListeners(TabViewListener.class)) l.tabClosed(this, tab);
        return true;
    }

    public int getTabCount() {
        return tabs.size();
    }

    public Tab getTab(int index) {
        return tabs.get(index);
    }

    public List<Tab> getTabs() {
        return Collections.unmodifiableList(new ArrayList<>(tabs));
    }

    public int indexOf(Tab tab) {
        return tabs.indexOf(tab);
    }

    public Tab getSelectedTab() {
        return selected;
    }

    public int getSelectedIndex() {
        return selected == null ? -1 : tabs.indexOf(selected);
    }

    public void setSelectedIndex(int index) {
        setSelectedTab(index < 0 || index >= tabs.size() ? null : tabs.get(index));
    }

    public void setSelectedTab(Tab tab) {
        if (tab == selected || (tab != null && !tabs.contains(tab))) return;
        Tab old = selected;
        selected = tab;
        if (tab != null) cards.show(holder, cardNames.get(tab.content));
        strip.ensureVisible(tab);
        strip.repaint();
        holder.repaint();
        fireSelection(old, tab);
    }

    /** Moves a tab to a new position. */
    public void moveTab(int from, int to) {
        if (from == to || from < 0 || to < 0 || from >= tabs.size() || to >= tabs.size()) return;
        Tab tab = tabs.remove(from);
        tabs.add(to, tab);
        strip.tabsChanged();
        for (TabViewListener l : listeners.getListeners(TabViewListener.class)) l.tabMoved(this, tab, from, to);
    }

    private void cycle(int delta) {
        if (tabs.size() < 2) return;
        int i = getSelectedIndex();
        setSelectedIndex(((i + delta) % tabs.size() + tabs.size()) % tabs.size());
    }

    // ------------------------------------------------------------------ options & listeners

    public boolean isReorderable() {
        return reorderable;
    }

    /** Allows dragging tabs to reorder them (on by default). */
    public void setReorderable(boolean reorderable) {
        this.reorderable = reorderable;
    }

    public boolean isCloseLastTabAllowed() {
        return closeLastTabAllowed;
    }

    public void setCloseLastTabAllowed(boolean allowed) {
        this.closeLastTabAllowed = allowed;
    }

    public boolean isAddButtonVisible() {
        return addButtonVisible;
    }

    public void setAddButtonVisible(boolean visible) {
        this.addButtonVisible = visible;
        strip.tabsChanged();
    }

    /** Called when the "+" button is pressed. Without a handler the button is hidden. */
    public void setAddTabHandler(Runnable handler) {
        this.addTabHandler = handler;
        strip.tabsChanged();
    }

    /** Convenience for a close handler: return false to veto. */
    public void onTabClosing(Predicate<Tab> handler) {
        addTabViewListener(new TabViewListener() {
            @Override
            public boolean tabClosing(TabView view, Tab tab) {
                return handler.test(tab);
            }
        });
    }

    public void addTabViewListener(TabViewListener l) {
        listeners.add(TabViewListener.class, l);
    }

    public void removeTabViewListener(TabViewListener l) {
        listeners.remove(TabViewListener.class, l);
    }

    private void fireSelection(Tab old, Tab now) {
        for (TabViewListener l : listeners.getListeners(TabViewListener.class)) l.selectionChanged(this, old, now);
    }

    // ------------------------------------------------------------------ layout

    private final class TabLayout implements LayoutManager {
        @Override
        public void addLayoutComponent(String name, Component comp) {
        }

        @Override
        public void removeLayoutComponent(Component comp) {
        }

        @Override
        public Dimension preferredLayoutSize(Container parent) {
            Dimension c = holder.getPreferredSize();
            return new Dimension(Math.max(c.width, 240), c.height + STRIP_HEIGHT - 1);
        }

        @Override
        public Dimension minimumLayoutSize(Container parent) {
            return new Dimension(160, STRIP_HEIGHT + 40);
        }

        @Override
        public void layoutContainer(Container parent) {
            strip.setBounds(0, 0, parent.getWidth(), STRIP_HEIGHT);
            holder.setBounds(0, STRIP_HEIGHT - 1, parent.getWidth(), Math.max(0, parent.getHeight() - STRIP_HEIGHT + 1));
        }
    }

    // ------------------------------------------------------------------ content holder

    /** The surface below the tabs: square top, rounded bottom, no outline under the selected tab. */
    private final class Holder extends JPanel {
        Holder() {
            super(null);
            setOpaque(false);
            setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 2, 1));
        }

        private Shape shape(int w, int h) {
            double r = RADIUS;
            Path2D p = new Path2D.Double();
            p.moveTo(0, 0);
            p.lineTo(w, 0);
            p.lineTo(w, h - r);
            p.quadTo(w, h, w - r, h);
            p.lineTo(r, h);
            p.quadTo(0, h, 0, h - r);
            p.closePath();
            return p;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Palette p = Tessel.palette();
            Graphics2D g2 = UIUtils.create(g);
            try {
                g2.setColor(p.surface);
                g2.fill(shape(getWidth(), getHeight()));
            } finally {
                g2.dispose();
            }
        }

        @Override
        protected void paintChildren(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.clip(shape(getWidth(), getHeight()));
                super.paintChildren(g2);
            } finally {
                g2.dispose();
            }
        }

        @Override
        public void paintBorder(Graphics g) {
            Palette p = Tessel.palette();
            Graphics2D g2 = UIUtils.create(g);
            try {
                g2.setColor(p.border);
                g2.setStroke(new BasicStroke(1f));
                int w = getWidth();
                int h = getHeight();
                double r = RADIUS;
                // Top edge, interrupted below the selected tab.
                Rectangle gap = strip.selectedBounds();
                if (gap == null) {
                    g2.drawLine(0, 0, w - 1, 0);
                } else {
                    if (gap.x > 0) g2.drawLine(0, 0, gap.x, 0);
                    if (gap.x + gap.width < w) g2.drawLine(gap.x + gap.width, 0, w - 1, 0);
                }
                Path2D side = new Path2D.Double();
                side.moveTo(w - 0.5, 0);
                side.lineTo(w - 0.5, h - r - 0.5);
                side.quadTo(w - 0.5, h - 0.5, w - r - 0.5, h - 0.5);
                side.lineTo(r + 0.5, h - 0.5);
                side.quadTo(0.5, h - 0.5, 0.5, h - r - 0.5);
                side.lineTo(0.5, 0);
                g2.draw(side);
            } finally {
                g2.dispose();
            }
        }
    }

    // ------------------------------------------------------------------ tab strip

    private final class Strip extends JComponent {
        private static final int PAD = 12;
        private static final int ADD_WIDTH = 34;
        private static final int ARROW_WIDTH = 24;

        private final List<Rectangle> bounds = new ArrayList<>();
        private int scroll;
        private int hoverTab = -1;
        private int hoverClose = -1;
        private boolean hoverAdd;
        private int hoverArrow;
        private int pressedClose = -1;
        private int dragIndex = -1;
        private int dragStartIndex = -1;
        private int dragOffset;
        private int dragX;
        private boolean dragging;
        private Point pressPoint;
        private int contentWidth;
        private int visibleWidth;
        private Rectangle addBounds = new Rectangle();
        private boolean overflow;

        Strip() {
            setOpaque(false);
            setFocusable(true);
            setFocusTraversalKeysEnabled(true);
            setToolTipText("");
            MouseAdapter mouse = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    handlePress(e);
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    handleRelease(e);
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    handleDrag(e);
                }

                @Override
                public void mouseMoved(MouseEvent e) {
                    updateHover(e.getPoint());
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    if (dragIndex < 0) updateHover(null);
                }

                @Override
                public void mouseWheelMoved(MouseWheelEvent e) {
                    if (!overflow) return;
                    scroll(e.getWheelRotation() * 60);
                }
            };
            addMouseListener(mouse);
            addMouseMotionListener(mouse);
            addMouseWheelListener(mouse);

            getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "prev");
            getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "next");
            getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0), "first");
            getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_END, 0), "last");
            getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "close");
            getActionMap().put("prev", act(() -> setSelectedIndex(Math.max(0, getSelectedIndex() - 1))));
            getActionMap().put("next", act(() -> setSelectedIndex(Math.min(tabs.size() - 1, getSelectedIndex() + 1))));
            getActionMap().put("first", act(() -> setSelectedIndex(0)));
            getActionMap().put("last", act(() -> setSelectedIndex(tabs.size() - 1)));
            getActionMap().put("close", act(() -> {
                if (selected != null) closeTab(selected);
            }));
            // Ctrl+Tab belongs to the tab view, not to focus traversal.
            for (int id : new int[]{java.awt.KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, java.awt.KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS}) {
                java.util.Set<AWTKeyStroke> keys = new java.util.HashSet<>(getFocusTraversalKeys(id));
                keys.removeIf(k -> (k.getModifiers() & KeyEvent.CTRL_DOWN_MASK) != 0);
                setFocusTraversalKeys(id, keys);
            }
        }

        private AbstractAction act(Runnable r) {
            return new AbstractAction() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    r.run();
                }
            };
        }

        // -------------------------------------------------------------- geometry

        void tabsChanged() {
            computeLayout();
            repaint();
            holder.repaint();
        }

        @Override
        public void setBounds(int x, int y, int width, int height) {
            boolean resized = width != getWidth();
            super.setBounds(x, y, width, height);
            if (resized) computeLayout();
        }

        private boolean showAdd() {
            return addButtonVisible && addTabHandler != null;
        }

        private int naturalWidth(Tab tab, FontMetrics fm) {
            int w = PAD * 2 + fm.stringWidth(tab.title);
            if (tab.icon != null) w += tab.icon.getIconWidth() + 8;
            if (tab.closable) w += 8 + 18;
            return Math.max(MIN_TAB_WIDTH, Math.min(MAX_TAB_WIDTH, w));
        }

        private void computeLayout() {
            bounds.clear();
            // The selected tab is painted semibold, the widest variant: size every tab for it.
            FontMetrics fm = getFontMetrics(Fonts.semibold(13));
            int total = getWidth();
            int addW = showAdd() ? ADD_WIDTH : 0;
            int left = 6;
            int available = total - left - addW - 8;
            int[] widths = new int[tabs.size()];
            int sum = 0;
            for (int i = 0; i < widths.length; i++) {
                widths[i] = naturalWidth(tabs.get(i), fm);
                sum += widths[i];
            }
            overflow = false;
            if (sum > available && widths.length > 0) {
                // Shrink evenly, but never below the minimum width.
                int fit = Math.max(MIN_TAB_WIDTH, available / widths.length);
                sum = 0;
                for (int i = 0; i < widths.length; i++) {
                    widths[i] = Math.min(widths[i], fit);
                    sum += widths[i];
                }
                if (sum > available) {
                    overflow = true;
                    available -= 2 * ARROW_WIDTH;
                }
            }
            contentWidth = sum;
            visibleWidth = Math.max(0, available);
            int origin = left + (overflow ? ARROW_WIDTH : 0);
            scroll = overflow ? Math.max(0, Math.min(scroll, contentWidth - visibleWidth)) : 0;
            int x = origin - scroll;
            for (int w : widths) {
                bounds.add(new Rectangle(x, TAB_TOP, w, STRIP_HEIGHT - TAB_TOP));
                x += w;
            }
            int addX = overflow ? total - addW - 4 : Math.min(origin + sum + 2, total - addW - 4);
            addBounds = new Rectangle(addX, TAB_TOP + 3, addW - 4, STRIP_HEIGHT - TAB_TOP - 6);
            if (selected != null && getWidth() > 0) ensureVisible(selected);
        }

        void ensureVisible(Tab tab) {
            if (!overflow || tab == null) return;
            int index = tabs.indexOf(tab);
            if (index < 0 || index >= bounds.size()) return;
            int origin = 6 + ARROW_WIDTH;
            Rectangle r = bounds.get(index);
            int left = r.x;
            int right = r.x + r.width;
            if (left < origin) scroll(left - origin - 8);
            else if (right > origin + visibleWidth) scroll(right - origin - visibleWidth + 8);
        }

        private void scroll(int delta) {
            int max = Math.max(0, contentWidth - visibleWidth);
            int next = Math.max(0, Math.min(max, scroll + delta));
            if (next == scroll) return;
            scroll = next;
            computeLayout();
            repaint();
            holder.repaint();
        }

        /** The selected tab's x-range in the tab view's coordinates (for the content outline gap). */
        Rectangle selectedBounds() {
            int i = selected == null ? -1 : tabs.indexOf(selected);
            if (i < 0 || i >= bounds.size()) return null;
            Rectangle r = bounds.get(i);
            Rectangle clip = visibleTabArea();
            Rectangle shown = r.intersection(clip);
            if (shown.isEmpty()) return null;
            // The tab's own outline overlaps the content border by one pixel on each side.
            return new Rectangle(shown.x + 1, 0, Math.max(0, shown.width - 2), 1);
        }

        private Rectangle visibleTabArea() {
            int origin = 6 + (overflow ? ARROW_WIDTH : 0);
            return new Rectangle(origin, 0, overflow ? visibleWidth : getWidth(), STRIP_HEIGHT);
        }

        private Rectangle closeBounds(Rectangle tab) {
            return new Rectangle(tab.x + tab.width - 8 - 20, tab.y + (tab.height - 20) / 2 - 1, 20, 20);
        }

        private Rectangle leftArrow() {
            return new Rectangle(6, TAB_TOP + 3, ARROW_WIDTH - 2, STRIP_HEIGHT - TAB_TOP - 6);
        }

        private Rectangle rightArrow() {
            int addW = showAdd() ? ADD_WIDTH : 0;
            return new Rectangle(getWidth() - addW - 8 - ARROW_WIDTH, TAB_TOP + 3, ARROW_WIDTH - 2, STRIP_HEIGHT - TAB_TOP - 6);
        }

        private int tabAt(Point pt) {
            if (!visibleTabArea().contains(pt)) return -1;
            for (int i = 0; i < bounds.size(); i++) {
                if (bounds.get(i).contains(pt)) return i;
            }
            return -1;
        }

        // -------------------------------------------------------------- mouse

        private void updateHover(Point pt) {
            int tab = pt == null ? -1 : tabAt(pt);
            int close = -1;
            if (tab >= 0 && tabs.get(tab).closable && closeBounds(bounds.get(tab)).contains(pt)) close = tab;
            boolean add = pt != null && showAdd() && addBounds.contains(pt);
            int arrow = 0;
            if (pt != null && overflow) {
                if (leftArrow().contains(pt)) arrow = -1;
                else if (rightArrow().contains(pt)) arrow = 1;
            }
            if (tab != hoverTab || close != hoverClose || add != hoverAdd || arrow != hoverArrow) {
                hoverTab = tab;
                hoverClose = close;
                hoverAdd = add;
                hoverArrow = arrow;
                repaint();
            }
        }

        private void handlePress(MouseEvent e) {
            requestFocusInWindow();
            Point pt = e.getPoint();
            if (overflow && leftArrow().contains(pt)) {
                scroll(-120);
                return;
            }
            if (overflow && rightArrow().contains(pt)) {
                scroll(120);
                return;
            }
            if (showAdd() && addBounds.contains(pt) && SwingUtilities.isLeftMouseButton(e)) {
                addTabHandler.run();
                return;
            }
            int i = tabAt(pt);
            if (i < 0) return;
            Tab tab = tabs.get(i);
            if (SwingUtilities.isMiddleMouseButton(e)) {
                closeTab(tab);
                return;
            }
            if (!SwingUtilities.isLeftMouseButton(e)) return;
            if (tab.closable && closeBounds(bounds.get(i)).contains(pt)) {
                pressedClose = i;
                return;
            }
            setSelectedTab(tab);
            pressPoint = pt;
            dragStartIndex = i;
            dragIndex = i;
            dragOffset = pt.x - bounds.get(i).x;
            dragging = false;
        }

        private void handleDrag(MouseEvent e) {
            if (!reorderable || dragIndex < 0 || pressPoint == null) return;
            if (!dragging && Math.abs(e.getX() - pressPoint.x) < 5) return;
            dragging = true;
            dragX = e.getX() - dragOffset;
            Rectangle clip = visibleTabArea();
            int width = bounds.get(dragIndex).width;
            dragX = Math.max(clip.x, Math.min(clip.x + clip.width - width, dragX));
            // Swap with a neighbour once the dragged tab's centre passes the neighbour's centre.
            int center = dragX + width / 2;
            while (dragIndex > 0 && center < bounds.get(dragIndex - 1).x + bounds.get(dragIndex - 1).width / 2) {
                swap(dragIndex, dragIndex - 1);
                dragIndex--;
            }
            while (dragIndex < tabs.size() - 1 && center > bounds.get(dragIndex + 1).x + bounds.get(dragIndex + 1).width / 2) {
                swap(dragIndex, dragIndex + 1);
                dragIndex++;
            }
            repaint();
        }

        /** Swaps two neighbouring tabs; their widths travel with them. */
        private void swap(int a, int b) {
            Collections.swap(tabs, a, b);
            int first = Math.min(a, b);
            int last = Math.max(a, b);
            int cursor = bounds.get(first).x;
            Collections.swap(bounds, a, b);
            for (int i = first; i <= last; i++) {
                Rectangle r = bounds.get(i);
                bounds.set(i, new Rectangle(cursor, r.y, r.width, r.height));
                cursor += r.width;
            }
        }

        private void handleRelease(MouseEvent e) {
            if (pressedClose >= 0) {
                int i = pressedClose;
                pressedClose = -1;
                if (i < tabs.size() && closeBounds(bounds.get(i)).contains(e.getPoint())) closeTab(tabs.get(i));
                return;
            }
            if (dragIndex >= 0) {
                boolean moved = dragging && dragIndex != dragStartIndex;
                int from = dragStartIndex;
                int to = dragIndex;
                dragIndex = -1;
                dragStartIndex = -1;
                dragging = false;
                pressPoint = null;
                computeLayout();
                repaint();
                if (moved) {
                    Tab tab = tabs.get(to);
                    for (TabViewListener l : listeners.getListeners(TabViewListener.class)) l.tabMoved(TabView.this, tab, from, to);
                }
            }
            updateHover(e.getPoint());
        }

        @Override
        public String getToolTipText(MouseEvent e) {
            int i = tabAt(e.getPoint());
            if (i < 0) return null;
            Tab t = tabs.get(i);
            if (t.tooltip != null) return t.tooltip;
            FontMetrics fm = getFontMetrics(Fonts.regular(13));
            return fm.stringWidth(t.title) > textWidth(t, bounds.get(i)) ? t.title : null;
        }

        // -------------------------------------------------------------- painting

        private int textWidth(Tab t, Rectangle r) {
            int w = r.width - PAD * 2;
            if (t.icon != null) w -= t.icon.getIconWidth() + 8;
            // Narrow tabs only keep room for the close button while they are selected.
            if (t.closable && (t == selected || r.width >= 130)) w -= 8 + 18;
            return Math.max(0, w);
        }

        private Path2D tabShape(Rectangle r, boolean closedBottom) {
            double x = r.x;
            double y = r.y;
            double w = r.width;
            double h = r.height;
            double rad = RADIUS;
            Path2D p = new Path2D.Double();
            p.moveTo(x, y + h);
            p.lineTo(x, y + rad);
            p.quadTo(x, y, x + rad, y);
            p.lineTo(x + w - rad, y);
            p.quadTo(x + w, y, x + w, y + rad);
            p.lineTo(x + w, y + h);
            if (closedBottom) p.closePath();
            return p;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Palette p = Tessel.palette();
            Graphics2D g2 = UIUtils.create(g);
            try {
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                Rectangle area = visibleTabArea();
                Shape oldClip = g2.getClip();
                g2.clipRect(area.x, 0, area.width, getHeight());

                int selectedIndex = getSelectedIndex();
                // Separators between neighbouring, unhighlighted tabs.
                g2.setColor(p.border);
                for (int i = 0; i < bounds.size() - 1; i++) {
                    if (i == selectedIndex || i + 1 == selectedIndex || i == hoverTab || i + 1 == hoverTab) continue;
                    if (dragging && (i == dragIndex || i + 1 == dragIndex)) continue;
                    Rectangle r = bounds.get(i);
                    g2.fillRect(r.x + r.width - 1, r.y + 10, 1, r.height - 20);
                }
                for (int i = 0; i < bounds.size(); i++) {
                    if (dragging && i == dragIndex) continue;
                    paintTab(g2, p, i, bounds.get(i), false);
                }
                if (dragging && dragIndex >= 0) {
                    Rectangle r = new Rectangle(bounds.get(dragIndex));
                    r.x = dragX;
                    paintTab(g2, p, dragIndex, r, true);
                }
                g2.setClip(oldClip);

                if (overflow) {
                    paintArrow(g2, p, leftArrow(), TesselIcons.Direction.LEFT, hoverArrow == -1, scroll > 0);
                    paintArrow(g2, p, rightArrow(), TesselIcons.Direction.RIGHT, hoverArrow == 1, scroll < contentWidth - visibleWidth);
                }
                if (showAdd()) {
                    if (hoverAdd) {
                        g2.setColor(p.controlHover);
                        UIUtils.fillRound(g2, addBounds.x, addBounds.y, addBounds.width, addBounds.height, 5);
                    }
                    TesselIcons.of(TesselIcons.Kind.PLUS, 16, pal -> hoverAdd ? pal.text : pal.textSecondary)
                            .paintIcon(this, g2, addBounds.x + (addBounds.width - 16) / 2, addBounds.y + (addBounds.height - 16) / 2);
                }
                if (UIUtils.isFocusVisible(this) && selectedIndex >= 0 && selectedIndex < bounds.size()) {
                    Rectangle r = bounds.get(selectedIndex);
                    UIUtils.paintFocusRing(g2, r.x + 1, r.y + 1, r.width - 2, r.height - 2, 7);
                }
            } finally {
                g2.dispose();
            }
        }

        private void paintArrow(Graphics2D g2, Palette p, Rectangle r, TesselIcons.Direction d, boolean hover, boolean enabled) {
            if (hover && enabled) {
                g2.setColor(p.controlHover);
                UIUtils.fillRound(g2, r.x, r.y, r.width, r.height, 5);
            }
            java.awt.Composite old = g2.getComposite();
            if (!enabled) g2.setComposite(java.awt.AlphaComposite.SrcOver.derive(UIUtils.DISABLED_ALPHA));
            TesselIcons.chevron(d, 16).paintIcon(this, g2, r.x + (r.width - 16) / 2, r.y + (r.height - 16) / 2);
            g2.setComposite(old);
        }

        private void paintTab(Graphics2D g2, Palette p, int index, Rectangle r, boolean lifted) {
            Tab tab = tabs.get(index);
            boolean isSelected = tab == selected;
            boolean hover = index == hoverTab && !dragging;
            if (isSelected || lifted) {
                if (lifted) UIUtils.paintShadow(g2, r.x, r.y, r.width, r.height - 6, RADIUS, 5);
                g2.setColor(p.surface);
                g2.fill(tabShape(r, true));
                g2.setColor(p.border);
                g2.setStroke(new BasicStroke(1f));
                Rectangle o = new Rectangle(r.x, r.y, r.width - 1, r.height);
                g2.draw(tabShape(new Rectangle(o.x, o.y, o.width, o.height), false));
                if (lifted) g2.drawLine(r.x, r.y + r.height - 1, r.x + r.width - 1, r.y + r.height - 1);
            } else if (hover) {
                g2.setColor(p.controlHover);
                UIUtils.fillRound(g2, r.x + 2, r.y + 3, r.width - 4, r.height - 8, 6);
            }

            int x = r.x + PAD;
            int centerY = r.y + (r.height - 2) / 2;
            if (tab.icon != null) {
                tab.icon.paintIcon(this, g2, x, centerY - tab.icon.getIconHeight() / 2 + 1);
                x += tab.icon.getIconWidth() + 8;
            }
            g2.setFont(isSelected ? Fonts.semibold(13) : Fonts.regular(13));
            FontMetrics fm = g2.getFontMetrics();
            g2.setColor(isSelected || hover ? p.text : p.textSecondary);
            int avail = textWidth(tab, r);
            String text = clip(tab.title, fm, avail);
            g2.drawString(text, x, centerY - fm.getHeight() / 2 + fm.getAscent() + 1);

            if (tab.closable && (isSelected || hover || lifted)) {
                Rectangle cb = closeBounds(r);
                boolean overClose = index == hoverClose;
                if (!isSelected && !lifted && r.width < 130) {
                    // The close button sits on top of the title of a narrow tab: give it a backdrop.
                    g2.setColor(p.controlHover);
                    UIUtils.fillRound(g2, cb.x - 2, cb.y, cb.width + 4, cb.height, 5);
                }
                if (overClose) {
                    g2.setColor(pressedClose == index ? p.controlPressed : p.controlHover);
                    UIUtils.fillRound(g2, cb.x, cb.y, cb.width, cb.height, 5);
                }
                TesselIcons.of(TesselIcons.Kind.CLOSE, 14, pal -> overClose ? pal.text : pal.textSecondary)
                        .paintIcon(this, g2, cb.x + 3, cb.y + 3);
            }
        }

        private String clip(String s, FontMetrics fm, int width) {
            if (fm.stringWidth(s) <= width) return s;
            String ellipsis = "…";
            int end = s.length();
            while (end > 0 && fm.stringWidth(s.substring(0, end) + ellipsis) > width) end--;
            return s.substring(0, end) + ellipsis;
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(200, STRIP_HEIGHT);
        }

        @Override
        public void updateUI() {
            super.updateUI();
            if (bounds != null) computeLayout();
        }
    }
}
