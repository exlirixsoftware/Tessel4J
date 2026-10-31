package net.tessel.laf.components;

import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.event.EventListenerList;
import net.tessel.laf.Fonts;
import net.tessel.laf.Palette;
import net.tessel.laf.Tessel;
import net.tessel.laf.icons.TesselIcons;
import net.tessel.laf.ui.UIUtils;

/**
 * Shows the path to the current location as a row of clickable items separated by chevrons, such as
 * {@code Home > Documents > Reports}. The last item is the current location (bold, not clickable). When the row
 * is too narrow the first items collapse into a "..." button that lists them in a menu.
 *
 * <pre>{@code
 * BreadcrumbBar bar = new BreadcrumbBar("Home", "Documents", "Reports");
 * bar.setTruncateOnClick(true);                                   // clicking an item makes it the last one
 * bar.addItemClickListener((b, index, item) -> open(index));
 * }</pre>
 *
 * Items can be any objects: the text comes from {@link #setItemTextProvider(Function)} (default
 * {@code String.valueOf}) and an optional icon from {@link #setItemIconProvider(Function)}. The bar is keyboard
 * operable (Left / Right / Home / End move between items, Enter or Space activate, Down opens the "..." menu).
 */
public class BreadcrumbBar extends JComponent {

    /** Called when an item (other than the current one) is clicked. */
    public interface ItemClickListener extends java.util.EventListener {
        void itemClicked(BreadcrumbBar bar, int index, Object item);
    }

    public static final String ITEMS_PROPERTY = "items";

    private static final int HEIGHT = 36;
    private static final int PAD = 10;
    private static final int SEPARATOR = 18;
    private static final int ELLIPSIS_WIDTH = 34;
    private static final int ICON_GAP = 6;

    private final List<Object> items = new ArrayList<>();
    private final EventListenerList listeners = new EventListenerList();
    private Function<Object, String> textProvider = String::valueOf;
    private Function<Object, Icon> iconProvider = o -> null;
    private boolean truncateOnClick;

    /** One thing the bar shows: an item ({@code index >= 0}) or the "..." button ({@code index == ELLIPSIS}). */
    private static final int ELLIPSIS = -1;

    private static final class Slot {
        final int index;
        final Rectangle bounds;

        Slot(int index, Rectangle bounds) {
            this.index = index;
            this.bounds = bounds;
        }
    }

    private List<Slot> slots = new ArrayList<>();
    private List<Rectangle> separators = new ArrayList<>();
    private boolean layoutValid;
    private int hoverSlot = -1;
    private int pressedSlot = -1;
    private int focusSlot;

    public BreadcrumbBar() {
        setOpaque(false);
        setFocusable(true);
        setCursor(java.awt.Cursor.getDefaultCursor());
        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                setHover(slotAt(e.getPoint()));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setHover(-1);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e) || !isEnabled()) return;
                requestFocusInWindow();
                pressedSlot = slotAt(e.getPoint());
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                int slot = slotAt(e.getPoint());
                boolean click = slot >= 0 && slot == pressedSlot && SwingUtilities.isLeftMouseButton(e) && isEnabled();
                pressedSlot = -1;
                repaint();
                if (click) activate(slot);
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                layoutValid = false;
            }
        });
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (focusSlot < 0 || focusSlot >= interactiveCount()) focusSlot = 0;
                repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                repaint();
            }
        });
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int count = interactiveCount();
                if (count == 0) return;
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_LEFT:
                        focusSlot = Math.max(0, focusSlot - 1);
                        break;
                    case KeyEvent.VK_RIGHT:
                        focusSlot = Math.min(count - 1, focusSlot + 1);
                        break;
                    case KeyEvent.VK_HOME:
                        focusSlot = 0;
                        break;
                    case KeyEvent.VK_END:
                        focusSlot = count - 1;
                        break;
                    case KeyEvent.VK_ENTER:
                    case KeyEvent.VK_SPACE:
                    case KeyEvent.VK_DOWN:
                        activate(focusSlot);
                        break;
                    default:
                        return;
                }
                e.consume();
                repaint();
            }
        });
        setToolTipText("");
    }

    public BreadcrumbBar(Object... items) {
        this();
        setItems(java.util.Arrays.asList(items));
    }

    // ------------------------------------------------------------------ items

    /** A copy of the current items, first to last. */
    public List<Object> getItems() {
        return Collections.unmodifiableList(new ArrayList<>(items));
    }

    public void setItems(Collection<?> newItems) {
        List<Object> old = new ArrayList<>(items);
        items.clear();
        items.addAll(newItems);
        itemsChanged(old);
    }

    public void addItem(Object item) {
        List<Object> old = new ArrayList<>(items);
        items.add(item);
        itemsChanged(old);
    }

    /** Removes every item after the one at {@code index}, making it the current item. */
    public void truncateAfter(int index) {
        if (index < -1 || index >= items.size() - 1) return;
        List<Object> old = new ArrayList<>(items);
        while (items.size() > index + 1) items.remove(items.size() - 1);
        itemsChanged(old);
    }

    public void clear() {
        setItems(Collections.emptyList());
    }

    public int getItemCount() {
        return items.size();
    }

    /** The current location: the last item, or null for an empty bar. */
    public Object getCurrentItem() {
        return items.isEmpty() ? null : items.get(items.size() - 1);
    }

    private void itemsChanged(List<Object> old) {
        layoutValid = false;
        hoverSlot = pressedSlot = -1;
        focusSlot = Math.min(focusSlot, Math.max(0, interactiveCount() - 1));
        revalidate();
        repaint();
        firePropertyChange(ITEMS_PROPERTY, old, new ArrayList<>(items));
    }

    /** How the text of an item is obtained (default {@code String.valueOf}). */
    public void setItemTextProvider(Function<Object, String> provider) {
        this.textProvider = provider == null ? String::valueOf : provider;
        layoutValid = false;
        revalidate();
        repaint();
    }

    /** An optional icon for an item (null for none); useful for a first "home" item. */
    public void setItemIconProvider(Function<Object, Icon> provider) {
        this.iconProvider = provider == null ? o -> null : provider;
        layoutValid = false;
        revalidate();
        repaint();
    }

    public boolean isTruncateOnClick() {
        return truncateOnClick;
    }

    /** When true, clicking an item removes everything after it (the usual behavior of a path bar). */
    public void setTruncateOnClick(boolean truncateOnClick) {
        this.truncateOnClick = truncateOnClick;
    }

    public void addItemClickListener(ItemClickListener l) {
        listeners.add(ItemClickListener.class, l);
    }

    public void removeItemClickListener(ItemClickListener l) {
        listeners.remove(ItemClickListener.class, l);
    }

    private void clicked(int index) {
        Object item = items.get(index);
        for (ItemClickListener l : listeners.getListeners(ItemClickListener.class)) l.itemClicked(this, index, item);
        if (truncateOnClick) truncateAfter(index);
    }

    // ------------------------------------------------------------------ layout

    private String text(int i) {
        String s = textProvider.apply(items.get(i));
        return s == null ? "" : s;
    }

    private java.awt.Font itemFont(int i) {
        return i == items.size() - 1 ? Fonts.semibold(14) : Fonts.regular(14);
    }

    private int itemWidth(int i) {
        FontMetrics fm = getFontMetrics(itemFont(i));
        Icon icon = iconProvider.apply(items.get(i));
        int w = PAD * 2 + fm.stringWidth(text(i));
        if (icon != null) w += icon.getIconWidth() + (text(i).isEmpty() ? 0 : ICON_GAP);
        return w;
    }

    @Override
    public Dimension getPreferredSize() {
        if (isPreferredSizeSet()) return super.getPreferredSize();
        int w = 0;
        for (int i = 0; i < items.size(); i++) {
            w += itemWidth(i);
            if (i < items.size() - 1) w += SEPARATOR;
        }
        return new Dimension(Math.max(w, 40), HEIGHT);
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(ELLIPSIS_WIDTH + SEPARATOR + 60, HEIGHT);
    }

    /** Places the items that fit (the end of the path wins) and a "..." button for the rest. */
    private void computeLayout() {
        slots = new ArrayList<>();
        separators = new ArrayList<>();
        layoutValid = true;
        int n = items.size();
        if (n == 0) return;
        int available = Math.max(0, getWidth());
        int y = (getHeight() - HEIGHT) / 2;

        int first = n;
        int used = 0;
        while (first > 0) {
            int w = itemWidth(first - 1) + (first < n ? SEPARATOR : 0);
            boolean last = first == n;
            int reserve = first - 1 > 0 ? ELLIPSIS_WIDTH + SEPARATOR : 0;
            if (!last && used + w + reserve > available) break;
            used += w;
            first--;
        }
        int x = 0;
        if (first > 0) {
            slots.add(new Slot(ELLIPSIS, new Rectangle(x, y, ELLIPSIS_WIDTH, HEIGHT)));
            x += ELLIPSIS_WIDTH;
            separators.add(new Rectangle(x, y, SEPARATOR, HEIGHT));
            x += SEPARATOR;
        }
        for (int i = first; i < n; i++) {
            int w = itemWidth(i);
            // The current item may be squeezed when even it does not fit.
            if (i == n - 1) w = Math.min(w, Math.max(30, available - x));
            slots.add(new Slot(i, new Rectangle(x, y, w, HEIGHT)));
            x += w;
            if (i < n - 1) {
                separators.add(new Rectangle(x, y, SEPARATOR, HEIGHT));
                x += SEPARATOR;
            }
        }
    }

    private List<Slot> slots() {
        if (!layoutValid) computeLayout();
        return slots;
    }

    /** Slots a user can act on: the "..." button and every item except the current one. */
    private int interactiveCount() {
        int count = 0;
        for (Slot s : slots()) {
            if (s.index == ELLIPSIS || s.index < items.size() - 1) count++;
        }
        return count;
    }

    private Slot interactive(int k) {
        int count = 0;
        for (Slot s : slots()) {
            if (s.index == ELLIPSIS || s.index < items.size() - 1) {
                if (count == k) return s;
                count++;
            }
        }
        return null;
    }

    /** Index into the interactive slots under the point, or -1. */
    private int slotAt(Point p) {
        int count = 0;
        for (Slot s : slots()) {
            boolean interactive = s.index == ELLIPSIS || s.index < items.size() - 1;
            if (!interactive) continue;
            if (s.bounds.contains(p)) return count;
            count++;
        }
        return -1;
    }

    private void setHover(int slot) {
        if (slot == hoverSlot) return;
        hoverSlot = slot;
        setCursor(slot >= 0 ? java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR) : java.awt.Cursor.getDefaultCursor());
        repaint();
    }

    private void activate(int interactiveSlot) {
        Slot s = interactive(interactiveSlot);
        if (s == null) return;
        if (s.index != ELLIPSIS) {
            clicked(s.index);
            return;
        }
        int firstVisible = Integer.MAX_VALUE;
        for (Slot other : slots()) {
            if (other.index != ELLIPSIS) firstVisible = Math.min(firstVisible, other.index);
        }
        JPopupMenu menu = new JPopupMenu();
        for (int i = firstVisible - 1; i >= 0; i--) {
            int index = i;
            JMenuItem mi = new JMenuItem(text(i));
            Icon icon = iconProvider.apply(items.get(i));
            if (icon != null) mi.setIcon(icon);
            mi.addActionListener(e -> clicked(index));
            menu.add(mi);
        }
        menu.show(this, s.bounds.x, s.bounds.y + s.bounds.height + 2);
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        for (Slot s : slots()) {
            if (s.bounds.contains(e.getPoint()) && s.index >= 0) {
                String t = text(s.index);
                return getFontMetrics(itemFont(s.index)).stringWidth(t) + PAD * 2 > s.bounds.width ? t : null;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ painting

    @Override
    protected void paintComponent(Graphics g) {
        Palette p = Tessel.palette();
        Graphics2D g2 = UIUtils.create(g);
        try {
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            UIUtils.applyEnabledAlpha(g2, this);
            int last = items.size() - 1;
            int interactiveIndex = 0;
            for (Slot s : slots()) {
                Rectangle r = s.bounds;
                boolean isCurrent = s.index == last;
                boolean interactive = !isCurrent;
                boolean hover = interactive && interactiveIndex == hoverSlot && isEnabled();
                boolean pressed = interactive && interactiveIndex == pressedSlot && hover;
                boolean focus = interactive && interactiveIndex == focusSlot && UIUtils.hasFocusWithin(this) && isEnabled();
                if (hover || pressed) {
                    g2.setColor(pressed ? p.controlPressed : p.controlHover);
                    UIUtils.fillRound(g2, r.x, r.y + 3, r.width, r.height - 6, 5);
                }
                if (focus) UIUtils.paintFocusRing(g2, r.x, r.y + 3, r.width, r.height - 6, 5);

                if (s.index == ELLIPSIS) {
                    TesselIcons.of(TesselIcons.Kind.MORE, 16, pal -> hover ? pal.text : pal.textSecondary)
                            .paintIcon(this, g2, r.x + (r.width - 16) / 2, r.y + (r.height - 16) / 2);
                } else {
                    paintItem(g2, p, s, isCurrent, hover);
                }
                if (interactive) interactiveIndex++;
            }
            for (Rectangle r : separators) {
                TesselIcons.chevron(TesselIcons.Direction.RIGHT, 14, pal -> pal.textTertiary)
                        .paintIcon(this, g2, r.x + (r.width - 14) / 2, r.y + (r.height - 14) / 2);
            }
        } finally {
            g2.dispose();
        }
    }

    private void paintItem(Graphics2D g2, Palette p, Slot s, boolean current, boolean hover) {
        Rectangle r = s.bounds;
        g2.setFont(itemFont(s.index));
        FontMetrics fm = g2.getFontMetrics();
        int x = r.x + PAD;
        Icon icon = iconProvider.apply(items.get(s.index));
        String text = text(s.index);
        if (icon != null) {
            icon.paintIcon(this, g2, x, r.y + (r.height - icon.getIconHeight()) / 2);
            x += icon.getIconWidth() + (text.isEmpty() ? 0 : ICON_GAP);
        }
        g2.setColor(current ? p.text : hover ? p.text : p.textSecondary);
        int room = r.x + r.width - PAD - x;
        g2.drawString(clip(text, fm, room), x, r.y + (r.height - fm.getHeight()) / 2 + fm.getAscent());
    }

    private static String clip(String s, FontMetrics fm, int width) {
        if (fm.stringWidth(s) <= width) return s;
        int end = s.length();
        while (end > 0 && fm.stringWidth(s.substring(0, end) + "…") > width) end--;
        return s.substring(0, end) + "…";
    }

    @Override
    public void doLayout() {
        layoutValid = false;
    }

    @Override
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleJComponent() {
                @Override
                public AccessibleRole getAccessibleRole() {
                    return AccessibleRole.PANEL;
                }
            };
            accessibleContext.setAccessibleName("Breadcrumb");
        }
        return accessibleContext;
    }
}
